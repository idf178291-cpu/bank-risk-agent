package io.agentscope.examples.bankrisk.config;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.agentscope.examples.bankrisk.tools.UserInteractionTool;
import io.agentscope.spring.boot.agui.common.AguiAgentRegistryCustomizer;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
public class AgentConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);

    // Shared MCP client, initialized eagerly after server is ready.
    // Avoids per-session loopback SSE handshake race on first request.
    private volatile McpClientWrapper sharedMcpClient;

    private static final String SYS_PROMPT =
            """
            # 角色
            你是企业风险评估专家，帮助风控人员分析企业客户的综合风险状况。

            # 路由
            上方 <available_skills> 列出每个技能的 [TRIGGER]/[TOOLS]/[PATH]：
            - [TRIGGER]：匹配用户意图 → 选择技能。只有当用户明确表达了该技能的使用场景时才匹配，模糊表述不算匹配。
            - [TOOLS]：该技能的工具调用链 → 按序执行
            - [PATH]：快捷路径（跳过无关步骤）或完整流程
            当没有任何 [TRIGGER] 匹配时（用户只说"查一下""风险情况"等），用 ask_user 让用户选择方向。可选方向只有 4 种：舆情分析、预警分析、风险标签分析、综合评估。禁止列出这四个方向之外的任何选项。按需加载技能，禁止首轮批量加载。

            # 交互
            所有需要用户选择的操作（选企业、选方向、选维度、确认操作等）必须用 ask_user(ui_type="select") 并传入 options 列表，禁止将选项写在回复文本中让用户手动输入。
            需要用户自由输入时才用 ask_user(ui_type="text")。
            分析结果用 Markdown 表格展示（当前值+基准+达标状态），高风险企业标注色标。
            """;

    @Bean
    public ClasspathSkillRepository skillRepository() throws IOException {
        ClasspathSkillRepository repo = new ClasspathSkillRepository("skills");
        log.info("Loaded {} skills from classpath", repo.getAllSkills().size());
        return repo;
    }

    @Bean
    public SkillBox skillBox(Toolkit toolkit, ClasspathSkillRepository skillRepository) {
        SkillBox skillBox = new SkillBox(toolkit);
        List<AgentSkill> skills = skillRepository.getAllSkills();
        for (AgentSkill skill : skills) {
            skillBox.registration().skill(skill).apply();
            log.info("Registered skill: {}", skill.getName());
        }
        skillBox.registerSkillLoadTool();
        return skillBox;
    }

    /**
     * Eagerly create and warm up the shared MCP client after the embedded web server
     * is ready. This prevents the per-session loopback SSE handshake from racing with
     * the Netty event loop on the very first request.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initSharedMcpClient() {
        try {
            sharedMcpClient =
                    McpClientBuilder.create("bank-risk-mcp")
                            .sseTransport("http://localhost:8083/sse")
                            .buildAsync()
                            .block(Duration.ofSeconds(10));
            log.info("Shared MCP client initialized and warmed up");
        } catch (Throwable e) {
            log.error("Failed to initialize shared MCP client: {}", e.getMessage(), e);
        }
    }

    @Bean
    public AguiAgentRegistryCustomizer aguiAgentRegistryCustomizer(
            SkillBox skillBox, Toolkit toolkit, Model model) {
        return registry -> {
            registry.registerFactory(
                    "bank-risk", () -> createBankRiskAgent(skillBox, toolkit, model));
            registry.registerFactory(
                    "default", () -> createBankRiskAgent(skillBox, toolkit, model));
        };
    }

    private Agent createBankRiskAgent(SkillBox skillBox, Toolkit toolkit, Model model) {

        // Native tool: user interaction (suspend/resume — cannot be MCP)
        toolkit.registerTool(new UserInteractionTool());

        // Register pre-warmed shared MCP client.
        // Use CompletableFuture instead of Reactor's .block() to avoid
        // "block() not supported in event loop thread" on Netty I/O threads.
        if (sharedMcpClient != null) {
            try {
                CompletableFuture<Void> latch = new CompletableFuture<>();
                toolkit.registerMcpClient(sharedMcpClient)
                        .doOnSuccess(v -> latch.complete(null))
                        .doOnError(latch::completeExceptionally)
                        .subscribe();
                latch.get(5, TimeUnit.SECONDS);
                log.debug("MCP tools registered from shared client");
            } catch (Throwable e) {
                log.warn("Failed to register MCP tools: {}", e.getMessage());
            }
        } else {
            log.warn("Shared MCP client not available — agent will run without MCP tools");
        }

        // Bind skillBox to toolkit (Toolkit is prototype-scoped, must rebind)
        skillBox.bindToolkit(toolkit);

        return ReActAgent.builder()
                .name("BankRiskAgent")
                .sysPrompt(SYS_PROMPT)
                .model(model)
                .toolkit(toolkit)
                .skillBox(skillBox)
                .memory(new InMemoryMemory())
                .maxIters(15)
                .enablePendingToolRecovery(true)
                .build();
    }
}
