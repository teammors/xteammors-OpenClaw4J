package com.xteammors.openclaw.rag.service;

import com.xteammors.openclaw.context.ChatContextManager;
import com.xteammors.openclaw.rag.service.vector.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.json.JSONObject;
import java.util.Iterator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RAGService {

    private static final Logger log = LoggerFactory.getLogger(RAGService.class);

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final VectorStoreService vectorStoreService;

    public RAGService(ChatClient chatClient, VectorStore vectorStore, VectorStoreService vectorStoreService) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * Q&A service（带聊天上下文）
     */
    public String answerQuestion(String question) {
        return answerQuestion(question, null);
    }

    public String answerQuestion(String question, String chatId) {
        try {
            log.info("handle the problem: {}", question);

            // 1. 获取聊天上下文
            String chatContext = "";
            if (chatId != null && !chatId.isBlank()) {
                chatContext = ChatContextManager.getInstance().getContext(chatId);
            }

            // 1. Vector search for relevant documents (downgraded to empty context if failed)
            List<Document> relevantDocs;
            try {
                // 增加搜索结果数量，确保能够找到相关技能
                List<Document> searchResults = vectorStoreService.similaritySearch(question, 10);
                // 确保使用可变列表，以便后续可以添加更多文档
                relevantDocs = new ArrayList<>(searchResults);
                log.info("Retrieve {} related documents", relevantDocs.size());
                // 打印找到的文档信息
                for (int i = 0; i < relevantDocs.size(); i++) {
                    Document doc = relevantDocs.get(i);
                    Map<String, Object> metadata = doc.getMetadata();
                    log.info("Document {}: skill_name={}, filename={}", i+1, metadata.get("skill_name"), metadata.get("filename"));
                }
                
                // 如果向量搜索结果较少，尝试关键词搜索
                if (relevantDocs.size() < 5) {
                    log.info("Vector search results are limited, trying keyword search");
                    List<Document> keywordDocs = vectorStoreService.keywordSearch(question, 10);
                    log.info("Keyword search retrieved {} related documents", keywordDocs.size());
                    // 打印找到的文档信息
                    for (int i = 0; i < keywordDocs.size(); i++) {
                        Document doc = keywordDocs.get(i);
                        Map<String, Object> metadata = doc.getMetadata();
                        log.info("Keyword Document {}: skill_name={}, filename={}", i+1, metadata.get("skill_name"), metadata.get("filename"));
                    }
                    
                    // 将关键词搜索结果添加到向量搜索结果中，避免重复
                    for (Document doc : keywordDocs) {
                        boolean exists = false;
                        for (Document existingDoc : relevantDocs) {
                            if (doc.getText().equals(existingDoc.getText())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            relevantDocs.add(doc);
                        }
                    }
                }
                
                // 如果仍然没有找到足够的相关文档，尝试从 vector_store.json 文件中加载所有技能
                // 修改：总是加载所有技能，确保 DeepSeek 能看到完整的技能列表
                log.info("Loading all skills from vector_store.json to ensure complete skill list for DeepSeek");
                try {
                    File vectorStoreFile = new File("vector_store.json");
                    if (vectorStoreFile.exists()) {
                        String content = new String(Files.readAllBytes(vectorStoreFile.toPath()), StandardCharsets.UTF_8);
                        JSONObject json = new JSONObject(content);
                        Iterator<String> keys = json.keys();
                        int loadedCount = 0;
                        while (keys.hasNext()) {
                            String key = keys.next();
                            JSONObject docJson = json.getJSONObject(key);
                            String text = docJson.getString("text");
                                            
                            // 提取技能名称
                            String skillName = extractSkillName(text);
                            if (skillName != null) {
                                Map<String, Object> metadata = new HashMap<>();
                                metadata.put("skill_name", skillName);
                                metadata.put("filename", "SKILL.md");
                                Document skillDoc = new Document(text, metadata);
                                                
                                // 检查是否已经存在
                                boolean exists = false;
                                for (Document existingDoc : relevantDocs) {
                                    if (existingDoc.getText().equals(skillDoc.getText())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    relevantDocs.add(skillDoc);
                                    loadedCount++;
                                    log.info("Loaded skill from vector_store.json: {}", skillName);
                                }
                            }
                        }
                        log.info("Successfully loaded {} skills from vector_store.json", loadedCount);
                    } else {
                        log.warn("vector_store.json file does not exist");
                    }
                } catch (Exception e) {
                    log.error("Failed to load skills from vector_store.json", e);
                }
            } catch (Exception searchEx) {
                log.warn("Vector retrieval failed, use keyword rollback retrieval:{}", searchEx.getMessage());
                List<Document> keywordResults = vectorStoreService.keywordSearch(question, 10);
                // 确保使用可变列表
                relevantDocs = new ArrayList<>(keywordResults);
                log.info("Keyword rollback retrieved {} related documents", relevantDocs.size());
                // 打印找到的文档信息
                for (int i = 0; i < relevantDocs.size(); i++) {
                    Document doc = relevantDocs.get(i);
                    Map<String, Object> metadata = doc.getMetadata();
                    log.info("Keyword Document {}: skill_name={}, filename={}", i+1, metadata.get("skill_name"), metadata.get("filename"));
                }
                
                // 如果关键词搜索结果较少，尝试从 vector_store.json 文件中加载所有技能
                if (relevantDocs.size() < 5) {
                    log.info("Keyword search results are limited, trying to load all skills from vector_store.json");
                    try {
                        File vectorStoreFile = new File("vector_store.json");
                        if (vectorStoreFile.exists()) {
                            String content = new String(Files.readAllBytes(vectorStoreFile.toPath()), StandardCharsets.UTF_8);
                            JSONObject json = new JSONObject(content);
                            Iterator<String> keys = json.keys();
                            int loadedCount = 0;
                            while (keys.hasNext()) {
                                String key = keys.next();
                                JSONObject docJson = json.getJSONObject(key);
                                String text = docJson.getString("text");
                                                
                                // 提取技能名称
                                String skillName = extractSkillName(text);
                                if (skillName != null) {
                                    Map<String, Object> metadata = new HashMap<>();
                                    metadata.put("skill_name", skillName);
                                    metadata.put("filename", "SKILL.md");
                                    Document skillDoc = new Document(text, metadata);
                                                    
                                    // 检查是否已经存在
                                    boolean exists = false;
                                    for (Document existingDoc : relevantDocs) {
                                        if (existingDoc.getText().equals(skillDoc.getText())) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                    if (!exists) {
                                        relevantDocs.add(skillDoc);
                                        loadedCount++;
                                        log.info("Loaded skill from vector_store.json: {}", skillName);
                                    }
                                }
                            }
                            log.info("Successfully loaded {} skills from vector_store.json", loadedCount);
                        } else {
                            log.warn("vector_store.json file does not exist");
                        }
                    } catch (Exception e) {
                        log.error("Failed to load skills from vector_store.json", e);
                    }
                }
            }

            // 2. Build context
            String context = buildContext(relevantDocs);

            // 3. Build system prompt words
            String systemPrompt = buildSystemPrompt(context, chatContext);

            // 4. Call the large model
            String answer = callLLM(systemPrompt, question);

            // 注意：不在这里记录上下文，由调用方（TeammorsMessageAdapter）在最终响应后记录
            // 因为 RAG 返回的可能是 CALL_SKILL 指令，用户实际看到的是技能执行结果

            return answer;
        } catch (Exception e) {
            log.error("RAG service processing failed", e);
            throw new RuntimeException("Problem handling failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从技能文档中提取技能名称
     */
    private String extractSkillName(String text) {
        // 尝试从 YAML front matter 中提取技能名称
        if (text.startsWith("---")) {
            int endIndex = text.indexOf("---", 3);
            if (endIndex != -1) {
                String frontMatter = text.substring(3, endIndex);
                String[] lines = frontMatter.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("name:")) {
                        String name = line.substring(5).trim();
                        // 移除引号
                        if (name.startsWith("\"") || name.startsWith("'")) {
                            name = name.substring(1, name.length() - 1);
                        }
                        return name;
                    }
                }
            }
        }
            
        // 如果没有 front matter，尝试从内容中提取
        // 查找 # 开头的标题行
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#") && !line.startsWith("##")) {
                // 提取标题作为技能名称（转换为小写并替换空格为连字符）
                String title = line.replace("#", "").trim();
                return title.toLowerCase().replaceAll("\\s+", "-").replaceAll("[^a-z0-9-]", "");
            }
        }
            
        return null;
    }

    /**
     * 构建检索到的上下文
     */
    private String buildContext(List<Document> documents) {
        if (documents.isEmpty()) {
            return "没有找到相关的文档信息。";
        }

        StringBuilder context = new StringBuilder("根据以下参考信息回答问题：\n\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            context.append("参考").append(i + 1).append(":\n")
                    .append(doc.getText())
                    .append("\n\n");

            // 添加元数据信息
            Map<String, Object> metadata = doc.getMetadata();
            if (metadata.containsKey("filename")) {
                context.append("来源：").append(metadata.get("filename")).append("\n");
            }
            context.append("---\n");
        }

        return context.toString();
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(String context, String chatContext) {
        String historyBlock = "";
        if (chatContext != null && !chatContext.isBlank()) {
            historyBlock = "\n以下是最近的对话记录，帮助你理解用户的上下文意图：\n" + chatContext + "\n\n";
        }

        return """
                你是一个专业的AI助手，基于提供的参考信息回答问题。
                
                %s
                特别注意：
                如果你发现用户的意图是请求执行某个具体的操作，且参考信息中包含相关的"技能(Skill)"描述（通常在metadata中有type=skill_doc或内容中包含Skill描述），
                请不要直接回答操作步骤，而是返回特定的指令格式来调用该技能。
                
                指令格式：CALL_SKILL: <技能名称>
                例如：CALL_SKILL: netease-mail-master
                
                多技能链式调用格式：如果用户的请求需要多个技能协作完成，请使用以下格式:
                CALL_SKILL_CHAIN: <技能 1 名称> -> <技能 2 名称> -> <技能 3 名称>
                例如：CALL_SKILL_CHAIN: system-status -> send-email
                
                技能链执行说明:
                1. 技能按照从左到右的顺序依次执行
                2. 前一个技能的输出会作为后一个技能的输入
                3. 最后一个技能的输出作为最终结果返回给用户
                4. 确保技能链中的每个技能都是必要的，且顺序合理
                
                示例场景:
                - 用户说"帮我获取当前系统状态，并发送邮件给 apple@isumaster.com"
                  应该返回:CALL_SKILL_CHAIN: system-status -> send-email
                - 用户说"查询天气然后发邮件给我"
                  应该返回:CALL_SKILL_CHAIN: weather-forecast -> send-email
                
                如果用户的请求不涉及调用技能，或者没有匹配的技能，请遵循以下规则正常回答：
                1. 仔细阅读并理解提供的参考信息
                2. 基于参考信息回答问题，不要编造信息
                3. 如果参考信息中没有相关答案，请如实说明
                4. 回答要清晰、准确、有条理
                5. 如果有多条相关信息，请进行整合
                
                参考信息：
                %s
                
                请基于以上信息回答用户的问题。
                """.formatted(historyBlock, context);
    }

    /**
     * 调用大模型
     */
    private String callLLM(String systemPrompt, String question) {
        try {
            log.info("调用DeepSeek API，问题长度: {}", question.length());

            ChatResponse response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(question)
                    .call()
                    .chatResponse();

            String answer = response.getResult().getOutput().getText();
            log.info("收到DeepSeek响应，答案长度: {}", answer.length());

            return answer;
        } catch (Exception e) {
            log.error("调用DeepSeek API失败", e);
            throw new RuntimeException("调用AI模型失败: " + e.getMessage(), e);
        }
    }

    /**
     * 带对话历史的问答
     */
    public String answerQuestionWithHistory(String question, List<Map<String, String>> history) {
        try {
            // 构建历史对话上下文
            String historyContext = buildHistoryContext(history);

            // 向量搜索（失败则降级为空上下文）
            List<Document> relevantDocs;
            try {
                relevantDocs = vectorStore.similaritySearch(question);
            } catch (Exception searchEx) {
                log.warn("向量检索失败，使用关键字回退检索：{}", searchEx.getMessage());
                relevantDocs = vectorStoreService.keywordSearch(question, 5);
            }

            String context = buildContext(relevantDocs);

            // 构建包含历史的系统提示词
            String systemPrompt = """
                    你是一个专业的AI助手，基于提供的参考信息和对话历史回答问题。
                    
                    对话历史：
                    %s
                    
                    参考信息：
                    %s
                    
                    请基于以上信息回答用户的问题。
                    """.formatted(historyContext, context);

            return callLLM(systemPrompt, question);
        } catch (Exception e) {
            log.error("带历史对话的RAG服务处理失败", e);
            throw new RuntimeException("处理对话失败: " + e.getMessage(), e);
        }
    }

    private String buildHistoryContext(List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) {
            return "无对话历史";
        }

        return history.stream()
                .map(entry -> entry.get("role") + ": " + entry.get("content"))
                .collect(Collectors.joining("\n"));
    }
}
