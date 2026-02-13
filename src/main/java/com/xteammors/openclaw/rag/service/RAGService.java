package com.xteammors.openclaw.rag.service;

import com.xteammors.openclaw.rag.service.vector.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

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
     * Q&A service
     */
    public String answerQuestion(String question) {
        try {
            log.info("handle the problem: {}", question);

            // 1. Vector search for relevant documents (downgraded to empty context if failed)
            List<Document> relevantDocs;
            try {
                relevantDocs = vectorStore.similaritySearch(question);
                log.info("Retrieve {} related documents", relevantDocs.size());
            } catch (Exception searchEx) {
                log.warn("Vector retrieval failed, use keyword rollback retrieval：{}", searchEx.getMessage());
                relevantDocs = vectorStoreService.keywordSearch(question, 5);
                log.info("Keyword rollback retrieved {} related documents", relevantDocs.size());
            }

            // 2. Build context
            String context = buildContext(relevantDocs);

            // 3. Build system prompt words
            String systemPrompt = buildSystemPrompt(context);

            // 4. Call the large model
            return callLLM(systemPrompt, question);
        } catch (Exception e) {
            log.error("RAG service processing failed", e);
            throw new RuntimeException("Problem handling failed: " + e.getMessage(), e);
        }
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
    private String buildSystemPrompt(String context) {
        return """
                你是一个专业的AI助手，基于提供的参考信息回答问题。
                
                特别注意：
                如果你发现用户的意图是请求执行某个具体的操作，且参考信息中包含相关的"技能(Skill)"描述（通常在metadata中有type=skill_doc或内容中包含Skill描述），
                请不要直接回答操作步骤，而是返回特定的指令格式来调用该技能。
                
                指令格式：CALL_SKILL: <技能名称>
                例如：CALL_SKILL: netease-mail-master
                
                如果用户的请求不涉及调用技能，或者没有匹配的技能，请遵循以下规则正常回答：
                1. 仔细阅读并理解提供的参考信息
                2. 基于参考信息回答问题，不要编造信息
                3. 如果参考信息中没有相关答案，请如实说明
                4. 回答要清晰、准确、有条理
                5. 如果有多条相关信息，请进行整合
                
                参考信息：
                %s
                
                请基于以上信息回答用户的问题。
                """.formatted(context);
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
