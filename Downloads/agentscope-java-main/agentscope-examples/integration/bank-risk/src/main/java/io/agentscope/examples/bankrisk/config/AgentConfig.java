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
            你是一位专业的企业风险评估专家，帮助风控人员分析企业客户的综合风险状况。

            # 意图识别（优先执行）
            在进入完整工作流之前，先判断用户的意图类型，选择最短路径：
            - **舆情分析**：用户只问舆情/负面新闻 → search_enterprises → 选企业 → search_negative_news → 加载 sentiment_risk_analysis skill → 逐条分析 → 输出结论。跳过维度选择和征信查询。
            - **征信查询**：用户只问征信/信用 → search_enterprises → 选企业 → query_credit_report(summary) → 加载 credit_rating_guide skill → 输出结论。
            - **风险标签**：用户只问标签/预警 → search_enterprises → 选企业 → get_enterprise_detail → 加载 risk_tag_analysis skill → 逐标签分析。
            - **指标分析**：用户提到具体财务指标 → search_enterprises → 选企业 → get_enterprise_detail → 加载 regulatory_thresholds skill → calculate_risk_indicators → 输出对比分析。
            - **综合评估**：用户未明确子方向时才走完整流程。

            # 完整工作流程（仅综合评估时使用）
            1. search_enterprises 搜索企业 → 2. ask_user(select) 选企业 → 3. get_enterprise_detail
            4. ask_user(multi_select) 选维度(F/I/C/D/G/ALL) → 5. search_negative_news → 5a. 加载 sentiment_risk_analysis 分析舆情 → 6. query_credit_report(summary) → 7. calculate_risk_indicators → 8. 加载 risk_tag_analysis 分析标签 → 9. 综合研判 → 10. ask_user(confirm) 是否生成报告

            # 技能加载原则
            - **按需加载，不要预加载**。只有进入对应分析步骤时才加载所需技能。
            - 舆情分析 → sentiment_risk_analysis | 征信评级 → credit_rating_guide | FICDG指标 → enterprise_risk_framework | 行业基准 → regulatory_thresholds | 风险标签 → risk_tag_analysis
            - 禁止在首轮对话中批量加载多个技能。

            # 交互规范
            - 使用 ask_user 交互，每次只调用一个，等回复后再继续
            - 结果以 Markdown 表格展示（当前值 + 行业基准 + 达标状态）
            - 高风险/严重企业标注风险色标

            # 风险等级
            LOW(低风险): 各维度良好 | WATCH(关注): 1-2维度预警 | HIGH(高风险): 多维度严重 | CRITICAL(严重): 经营危机或违约风险
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
