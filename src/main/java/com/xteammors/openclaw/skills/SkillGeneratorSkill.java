package com.xteammors.openclaw.skills;

import com.xteammors.openclaw.property.SkillsDirProperty;
import com.xteammors.openclaw.skills.base.AgentSkill;
import com.xteammors.openclaw.utils.ShellUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SkillGeneratorSkill implements AgentSkill {
    private static final Logger log = LoggerFactory.getLogger(SkillGeneratorSkill.class);

    private static final int MAX_ITERATIONS = 5;
    private static final int TEST_TIMEOUT_SECONDS = 30;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private SkillsDirProperty skillsDirProperty;

    @Autowired
    private SkillInitializer skillInitializer;

    private final Map<String, SkillRequestState> userStates = new HashMap<>();

    @Override
    public String getName() {
        return "skill-generator";
    }

    @Override
    public String getDescription() {
        return "技能生成与升级器。检测用户需求：若需新技能则创建，若需改进现有技能则升级迭代。均通过AI循环生成+自测。";
    }

    public boolean hasPendingRequest(String chatId) {
        return userStates.containsKey(chatId);
    }

    @Override
    public String execute(String input, String chatId) {
        log.info("Executing SkillGeneratorSkill with input: {}", input);
        try {
            String cleanedInput = cleanInput(input);
            SkillRequestState state = userStates.get(chatId);

            if (state == null) {
                return detectIntent(cleanedInput, chatId);
            } else {
                return handleUserResponse(cleanedInput, chatId, state);
            }
        } catch (Exception e) {
            log.error("Failed to execute SkillGeneratorSkill", e);
            return "执行技能生成器失败: " + e.getMessage();
        }
    }

    private String cleanInput(String input) {
        if (input == null) return "";
        return input.replaceAll("@\\S+", "").trim();
    }

    // ==================== 意图识别：创建 vs 升级 ====================

    private String detectIntent(String input, String chatId) {
        String skillsDir = getSkillsDir();
        String skillList = buildExistingSkillList(skillsDir);

        String prompt = PROMPT_DETECT_INTENT
                .replace("{user_input}", input)
                .replace("{existing_skills}", skillList);

        String response = chatClient.prompt(prompt).call().content();
        log.info("Intent detection AI response: {}", response);

        String action = extractJsonValue(response, "action");
        String skillName = extractJsonValue(response, "skill_name");
        String description = extractJsonValue(response, "description");

        if ("UPGRADE".equalsIgnoreCase(action) && skillName != null && !skillName.isBlank()) {
            // 升级现有技能
            return startUpgradeFlow(input, chatId, skillName.trim(), description);
        } else {
            // 创建新技能
            return startCreateFlow(input, chatId);
        }
    }

    /**
     * 扫描 skills 目录，列出所有已有技能供 AI 匹配
     */
    private String buildExistingSkillList(String skillsDir) {
        StringBuilder sb = new StringBuilder();
        File dir = new File(skillsDir);
        if (dir.exists() && dir.isDirectory()) {
            File[] children = dir.listFiles(File::isDirectory);
            if (children != null) {
                for (File child : children) {
                    // 检查是否有 scripts 目录且包含 .py 文件
                    File scriptsDir = new File(child, "scripts");
                    if (scriptsDir.exists() && scriptsDir.isDirectory()) {
                        File[] pyFiles = scriptsDir.listFiles((d, name) -> name.endsWith(".py"));
                        if (pyFiles != null && pyFiles.length > 0) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(child.getName());
                        }
                    }
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : "无已有技能";
    }

    // ==================== 创建新技能流程 ====================

    private String startCreateFlow(String input, String chatId) {
        userStates.put(chatId, new SkillRequestState(input, Action.CREATE, null, null, null));
        return "检测到您需要创建一个新技能。\n\n"
             + "需求: " + input + "\n\n"
             + "是否确认创建？（回复 是/好/OK 确认）";
    }

    // ==================== 升级现有技能流程 ====================

    private String startUpgradeFlow(String input, String chatId, String skillName, String description) {
        String skillsDir = getSkillsDir();

        // 查找现有脚本
        File skillDir = new File(skillsDir, skillName);
        if (!skillDir.exists() || !skillDir.isDirectory()) {
            return "未找到技能: " + skillName + "，请确认技能名称是否正确。";
        }

        File scriptsDir = new File(skillDir, "scripts");
        if (!scriptsDir.exists()) {
            return "技能 " + skillName + " 没有 scripts 目录，无法升级。";
        }

        File[] pyFiles = scriptsDir.listFiles((d, name) -> name.endsWith(".py"));
        if (pyFiles == null || pyFiles.length == 0) {
            return "技能 " + skillName + " 没有 Python 脚本，无法升级。";
        }

        // 读取现有脚本内容
        String existingCode;
        try {
            existingCode = Files.readString(pyFiles[0].toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read existing script: {}", pyFiles[0], e);
            return "读取技能脚本失败: " + e.getMessage();
        }

        String changeDesc = description != null ? description : input;

        userStates.put(chatId, new SkillRequestState(
                input, Action.UPGRADE, skillName, existingCode, changeDesc));

        return "检测到您需要升级技能 **" + skillName + "** 。\n\n"
             + "改进描述: " + changeDesc + "\n\n"
             + "是否确认升级？（回复 是/好/OK 确认）";
    }

    // ==================== 用户确认处理 ====================

    private String handleUserResponse(String input, String chatId, SkillRequestState state) {
        if (isApproval(input)) {
            userStates.remove(chatId);
            if (state.action == Action.UPGRADE) {
                return upgradeSkillWithAI(state);
            } else {
                return createSkillWithAI(state.originalRequest);
            }
        } else {
            userStates.remove(chatId);
            return "好的，操作已取消。如有其他需求请随时告诉我。";
        }
    }

    private boolean isApproval(String input) {
        String prompt = PROMPT_IS_APPROVAL.replace("{user_input}", input);
        try {
            String response = chatClient.prompt(prompt).call().content();
            log.info("Approval detection result: {}", response);
            return "yes".equalsIgnoreCase(response.trim());
        } catch (Exception e) {
            log.error("Error detecting approval", e);
            return false;
        }
    }

    // ==================== 核心：创建新技能 ====================

    private String createSkillWithAI(String userRequest) {
        log.info("Starting AI skill CREATE for: {}", userRequest);
        String skillsDir = getSkillsDir();

        try {
            SkillMeta meta = parseIntentWithAI(userRequest);
            log.info("Parsed intent: name={}, desc={}", meta.name, meta.description);

            Path skillDir = Paths.get(skillsDir, meta.name);
            Path scriptsDir = skillDir.resolve("scripts");
            Files.createDirectories(scriptsDir);

            String scriptContent = generateScriptLoop(userRequest, meta, null);
            if (scriptContent == null) {
                return "技能创建失败: 经过 " + MAX_ITERATIONS + " 次尝试仍无法生成可用的脚本。";
            }

            Path scriptPath = scriptsDir.resolve(toScriptFileName(meta.name));
            Files.writeString(scriptPath, scriptContent, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            scriptPath.toFile().setExecutable(true);

            String skillMd = generateSkillMdWithAI(userRequest, meta);
            Path skillMdPath = skillDir.resolve("SKILL.md");
            Files.writeString(skillMdPath, skillMd, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            skillInitializer.reinitializeSkills();

            return "技能创建成功！\n\n"
                 + "技能名称: " + meta.name + "\n"
                 + "描述: " + meta.description + "\n"
                 + "创建时间: " + now() + "\n"
                 + "脚本路径: " + scriptPath + "\n\n"
                 + "技能已自动测试通过，已添加到技能库，可以使用了。";

        } catch (Exception e) {
            log.error("Error creating skill", e);
            return "技能创建失败: " + e.getMessage();
        }
    }

    // ==================== 核心：升级现有技能 ====================

    private String upgradeSkillWithAI(SkillRequestState state) {
        String skillName = state.skillName;
        String existingCode = state.existingCode;
        String changeDesc = state.changeDescription;
        String skillsDir = getSkillsDir();

        log.info("Starting AI skill UPGRADE for: {}, change: {}", skillName, changeDesc);

        try {
            Path skillDir = Paths.get(skillsDir, skillName);
            Path scriptsDir = skillDir.resolve("scripts");

            // 找到现有的 .py 脚本文件
            File[] pyFiles = scriptsDir.toFile().listFiles((d, name) -> name.endsWith(".py"));
            if (pyFiles == null || pyFiles.length == 0) {
                return "技能 " + skillName + " 没有找到 Python 脚本文件。";
            }
            Path scriptPath = pyFiles[0].toPath();

            // 用 AI 循环生成升级后的脚本
            SkillMeta meta = new SkillMeta(skillName, "升级: " + changeDesc);
            String newCode = generateScriptLoop(changeDesc, meta, existingCode);

            if (newCode == null) {
                return "技能升级失败: 经过 " + MAX_ITERATIONS + " 次尝试仍无法生成可用的脚本。\n"
                     + "原始脚本未被修改。";
            }

            // 写入升级后的脚本
            Files.writeString(scriptPath, newCode, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            scriptPath.toFile().setExecutable(true);
            log.info("Upgraded script written to: {}", scriptPath);

            // 重新注册
            skillInitializer.reinitializeSkills();

            return "技能升级成功！\n\n"
                 + "技能名称: " + skillName + "\n"
                 + "改进内容: " + changeDesc + "\n"
                 + "升级时间: " + now() + "\n"
                 + "脚本路径: " + scriptPath + "\n\n"
                 + "技能已通过自动测试，已更新到技能库中。";

        } catch (Exception e) {
            log.error("Error upgrading skill", e);
            return "技能升级失败: " + e.getMessage();
        }
    }

    // ==================== AI 意图解析（创建时用） ====================

    private SkillMeta parseIntentWithAI(String userRequest) {
        String prompt = PROMPT_PARSE_INTENT.replace("{user_request}", userRequest);
        String response = chatClient.prompt(prompt).call().content();
        log.info("Intent parse AI response: {}", response);

        String name = extractJsonValue(response, "skill_name");
        String desc = extractJsonValue(response, "skill_description");

        if (name == null || name.isBlank()) {
            name = "custom-" + System.currentTimeMillis() % 100000;
        }
        if (desc == null || desc.isBlank()) {
            desc = "处理用户请求的自定义技能";
        }
        return new SkillMeta(name.trim(), desc.trim());
    }

    // ==================== AI + 自测 循环（创建/升级共用） ====================

    /**
     * 生成脚本的循环入口
     *
     * @param userNeed      用户需求描述（创建时是原始请求，升级时是改进说明）
     * @param meta          技能元数据
     * @param existingCode  若不为 null，表示是升级模式，AI 需要基于此代码改进
     * @return 最终通过测试的 Python 代码，或 null 表示全部迭代失败
     */
    private String generateScriptLoop(String userNeed, SkillMeta meta, String existingCode) {
        String lastError = null;
        String previousCode = null;

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            log.info("Script generation attempt {}/{} for {}", i + 1, MAX_ITERATIONS, meta.name);

            String prompt = buildScriptPrompt(userNeed, meta, existingCode, lastError, previousCode);
            String code = chatClient.prompt(prompt).call().content();
            code = extractPythonCode(code);

            if (code == null || code.isBlank()) {
                lastError = "AI 未返回有效的 Python 代码";
                continue;
            }

            TestResult testResult = testScript(code);
            if (testResult.success) {
                log.info("Script test PASSED on attempt {} for {}", i + 1, meta.name);
                return code;
            }

            lastError = testResult.error;
            previousCode = code;
            log.warn("Script test FAILED on attempt {} for {}: {}", i + 1, meta.name, lastError);
        }

        return null;
    }

    /**
     * 构建脚本生成的 Prompt（区分创建/升级）
     */
    private String buildScriptPrompt(String userNeed, SkillMeta meta,
                                      String existingCode, String lastError, String previousCode) {
        boolean isUpgrade = existingCode != null;

        StringBuilder sb = new StringBuilder();

        if (isUpgrade) {
            sb.append(PROMPT_UPGRADE_SCRIPT
                    .replace("{skill_name}", meta.name)
                    .replace("{change_description}", userNeed)
                    .replace("{existing_code}", existingCode));
        } else {
            sb.append(PROMPT_GENERATE_SCRIPT
                    .replace("{skill_name}", meta.name)
                    .replace("{skill_description}", meta.description)
                    .replace("{user_request}", userNeed));
        }

        // 追加错误反馈（创建/升级共用）
        if (lastError != null) {
            sb.append("\n\n【上一次生成的代码自测失败】\n");
            sb.append("错误信息:\n").append(lastError).append("\n");
            if (previousCode != null && previousCode.length() < 5000) {
                sb.append("\n上一次的代码:\n```python\n").append(previousCode).append("\n```\n");
            }
            sb.append("\n请分析错误原因，修复问题后重新生成。");
        }

        return sb.toString();
    }

    // ==================== 自测 ====================

    private TestResult testScript(String scriptContent) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("skill_test_");
            Path scriptPath = tempDir.resolve("test_script.py");
            Files.writeString(scriptPath, scriptContent, StandardCharsets.UTF_8);

            String pythonCmd = ShellUtils.getPythonCommand();
            ProcessBuilder pb = new ProcessBuilder(pythonCmd, scriptPath.toString(), "test");
            pb.directory(tempDir.toFile());
            pb.redirectErrorStream(false);

            Process process = pb.start();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread stdoutThread = new Thread(() -> {
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.error("Error reading stdout", e);
                }
            });

            Thread stderrThread = new Thread(() -> {
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.error("Error reading stderr", e);
                }
            });

            stdoutThread.start();
            stderrThread.start();

            boolean finished = process.waitFor(TEST_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            stdoutThread.join(2000);
            stderrThread.join(2000);

            if (!finished) {
                process.destroyForcibly();
                return new TestResult(false, "脚本执行超时（" + TEST_TIMEOUT_SECONDS + "秒）");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String error = stderr.toString().trim();
                if (error.isEmpty()) error = stdout.toString().trim();
                if (error.isEmpty()) error = "脚本退出码: " + exitCode;
                return new TestResult(false, error);
            }

            return new TestResult(true, null);

        } catch (Exception e) {
            return new TestResult(false, "测试异常: " + e.getMessage());
        } finally {
            if (tempDir != null) {
                try {
                    Files.walk(tempDir)
                            .sorted(java.util.Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException e) {
                    log.warn("Failed to clean up temp dir: {}", tempDir, e);
                }
            }
        }
    }

    /**
     * AI 生成 SKILL.md 文件
     */
    private String generateSkillMdWithAI(String userRequest, SkillMeta meta) {
        String prompt = PROMPT_GENERATE_SKILL_MD
                .replace("{skill_name}", meta.name)
                .replace("{skill_description}", meta.description)
                .replace("{user_request}", userRequest);

        String response = chatClient.prompt(prompt).call().content();
        return stripMarkdownCodeBlock(response, "markdown");
    }

    // ==================== Prompt 模板 ====================

    private static final String PROMPT_IS_APPROVAL = """
            请判断用户是否同意，只输出 'yes' 或 'no'。

            用户输入: {user_input}

            同意的表述包括：是的、好的、可以、确定、OK、好、行、yes等。
            拒绝的表述包括：不、不用、不需要、算了、别、no等。
            请只输出 'yes' 或 'no'，不要输出其他任何内容。
            """;

    /**
     * 意图识别 Prompt：判断用户是要"创建新技能"还是"升级现有技能"
     */
    private static final String PROMPT_DETECT_INTENT = """
            请分析用户输入，判断用户意图是"创建新技能"还是"升级现有技能"，只输出 JSON。

            用户输入：{user_input}

            已有技能列表：{existing_skills}

            判断规则：
            1. 如果用户提到对某个已有技能的不满、bug、功能缺失、改造、升级、修改等，action 设为 "UPGRADE"
            2. 如果用户请求一个全新的功能，且没有匹配的已有技能，action 设为 "CREATE"
            3. 对于 UPGRADE，skill_name 必须是已有技能列表中的名称
            4. description 提取用户希望改进的具体内容

            输出格式：
            {"action": "CREATE|UPGRADE", "skill_name": "xxx", "description": "xxx"}

            示例：
            - "帮我创建一个查询股票的技能" → {"action": "CREATE", "skill_name": "", "description": "查询股票价格"}
            - "pdf创建成功了但中文不显示，能改一下吗" → {"action": "UPGRADE", "skill_name": "pdf-generator", "description": "支持中文显示，解决中文变黑块的问题"}
            - "天气技能加上湿度显示" → {"action": "UPGRADE", "skill_name": "weather-forecast", "description": "增加湿度信息显示"}
            """;

    private static final String PROMPT_PARSE_INTENT = """
            请分析以下用户请求，提取技能名称和描述，只输出 JSON。

            用户请求：{user_request}

            要求：
            1. skill_name：小写短字符串，用连字符分隔单词，如 "pdf-maker"、"weather-forecast"
            2. skill_description：简洁中文描述，说明技能的核心功能
            3. 只输出 JSON，不要输出其他任何内容

            输出格式：
            {"skill_name": "xxx", "skill_description": "xxx"}
            """;

    private static final String PROMPT_GENERATE_SCRIPT = """
            请为以下技能生成一个完整的 Python 脚本。

            技能名称：{skill_name}
            技能描述：{skill_description}
            用户需求：{user_request}

            严格要求：
            1. 脚本必须是完整可执行的 Python 文件，包含 #!/usr/bin/env python3 和编码声明
            2. 通过 sys.argv 接收命令行参数（sys.argv[1:] 为参数列表）
            3. 所有输出使用 print() 输出到标准输出
            4. 包含 if __name__ == "__main__": 入口
            5. 依赖库如果缺失，使用 pip 自动安装（subprocess.check_call([sys.executable, "-m", "pip", "install", "库名", "--break-system-packages"])）
            6. 适当的异常处理，错误信息输出到标准输出（不要用 sys.exit 非零退出）
            7. 输出内容为用户友好的中文
            8. 不要使用 f-string，使用字符串格式化方法如 format() 或 + 连接
            9. 当参数为 "test" 时，执行一个简单的自测逻辑（如打印 "test ok"）并正常退出
            10. 只输出 Python 代码，不要输出任何解释、markdown 标记或其他内容
            """;

    /**
     * 升级已有脚本的 Prompt
     */
    private static final String PROMPT_UPGRADE_SCRIPT = """
            请根据改进需求，修改以下 Python 脚本。

            技能名称：{skill_name}
            改进需求：{change_description}

            现有代码：
            ```python
            {existing_code}
            ```

            严格要求：
            1. 在现有代码基础上修改，保留原有功能，只做需求中要求的改动
            2. 脚本必须是完整可执行的 Python 文件
            3. 通过 sys.argv 接收命令行参数（sys.argv[1:] 为参数列表）
            4. 包含 if __name__ == "__main__": 入口
            5. 依赖库如果缺失，使用 pip 自动安装（subprocess.check_call([sys.executable, "-m", "pip", "install", "库名", "--break-system-packages"])）
            6. 适当的异常处理
            7. 不要使用 f-string，使用 + 连接字符串或 format()
            8. 当参数为 "test" 时，打印 "test ok" 并正常退出
            9. 只输出完整的 Python 代码，不要输出任何解释或 markdown 标记
            """;

    private static final String PROMPT_GENERATE_SKILL_MD = """
            请为以下技能生成 SKILL.md 文件内容（YAML front matter + Markdown）。

            技能名称：{skill_name}
            技能描述：{skill_description}
            用户需求：{user_request}

            要求：
            1. 使用 YAML front matter 格式，包含 name 和 description 字段
            2. 正文包含：Capabilities、Usage、Parameters、Dependencies 等章节
            3. 使用中文描述
            4. 只输出 SKILL.md 的内容，不要输出其他任何内容

            输出格式：
            ---
            name: "{skill_name}"
            description: "{skill_description}"
            ---

            # {技能标题}

            ## 功能说明
            ...

            ## 使用方式
            ...

            ## 参数说明
            ...

            ## 依赖
            ...
            """;

    // ==================== 工具方法 ====================

    private String getSkillsDir() {
        String dir = skillsDirProperty.getDir();
        if (dir == null || dir.isBlank()) {
            dir = System.getProperty("user.dir") + "/skills";
        }
        return dir;
    }

    private String toScriptFileName(String skillName) {
        return skillName.replace('-', '_') + ".py";
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractPythonCode(String response) {
        if (response == null) return null;

        Pattern pattern = Pattern.compile("```python\\s*\\n([\\s\\S]*?)```", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        pattern = Pattern.compile("```\\s*\\n([\\s\\S]*?)```", Pattern.MULTILINE);
        matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        String trimmed = response.trim();
        if (trimmed.startsWith("#!/usr/bin/env python") || trimmed.startsWith("import ") || trimmed.startsWith("# -*-")) {
            return trimmed;
        }

        return null;
    }

    private String stripMarkdownCodeBlock(String text, String lang) {
        if (text == null) return "";
        String prefix = "```" + lang;
        if (text.startsWith(prefix)) {
            text = text.substring(prefix.length());
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
    }

    // ==================== 内部类 ====================

    private enum Action {
        CREATE, UPGRADE
    }

    private static class SkillRequestState {
        final String originalRequest;
        final Action action;
        final String skillName;        // UPGRADE 时使用
        final String existingCode;     // UPGRADE 时使用
        final String changeDescription; // UPGRADE 时使用

        SkillRequestState(String originalRequest, Action action,
                          String skillName, String existingCode, String changeDescription) {
            this.originalRequest = originalRequest;
            this.action = action;
            this.skillName = skillName;
            this.existingCode = existingCode;
            this.changeDescription = changeDescription;
        }
    }

    private static class SkillMeta {
        final String name;
        final String description;

        SkillMeta(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }

    private static class TestResult {
        final boolean success;
        final String error;

        TestResult(boolean success, String error) {
            this.success = success;
            this.error = error;
        }
    }
}
