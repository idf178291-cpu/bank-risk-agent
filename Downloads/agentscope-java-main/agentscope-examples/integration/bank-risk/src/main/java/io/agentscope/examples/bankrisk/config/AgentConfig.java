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

            # 核心工作流程
            1. 当用户提到企业名称时，使用 search_enterprises (MCP) 搜索企业，传入关键词
            2. 如果 search_enterprises 返回多家企业，使用 ask_user (ui_type="select")
               让用户选择具体企业（显示 customerId 和 name）
            3. 确认企业后，使用 get_enterprise_detail (MCP) 获取企业完整详细信息
            4. 使用 ask_user (ui_type="multi_select") 询问用户关注哪些风险维度：
               - F 财务健康 (盈利、现金流、增长率)
               - I 行业地位 (市场份额、技术壁垒、竞争力)
               - C 合规声誉 (处罚、诉讼、舆情)
               - D 债务风险 (负债率、流动性、偿债能力)
               - G 运营治理 (管理层、供应链、公司治理)
               - ALL 全部分析
            5. 调用 search_negative_news (MCP) 查询企业近期负面舆情
            5a. **查询到舆情后，立即加载 sentiment_risk_analysis skill，按技能方法论逐条分析舆情**
            5b. 舆情分析完成后，输出分类结果、影响评估和管理建议
            6. 调用 query_credit_report (MCP) 查询企业征信报告（使用 summary 类型获取概要）
            7. 使用 calculate_risk_indicators (MCP) 计算所选维度的风险指标
            8. **加载 risk_tag_analysis skill，逐项分析企业 riskAssessment 中的风险标签**
            9. 综合分析风险指标、舆情分析结果、风险标签判定和征信报告结果
            10. 整理关键发现，使用 ask_user (ui_type="confirm") 询问是否需要生成完整报告
            11. 若用户确认，使用 generate_risk_report (MCP) 生成 Markdown 格式的完整风险评估报告

            # 知识库使用
            - 你可以通过 load_skill 工具查阅风控知识库
            - 当需要了解 FICDG 评估方法的具体细则时，加载 enterprise_risk_framework skill
            - 当需要了解信用评级标准时，加载 credit_rating_guide skill
            - 当需要核对行业基准数值和指标阈值时，加载 regulatory_thresholds skill
            - **当需要分析企业负面舆情时，必须先加载 sentiment_risk_analysis skill**
              - 该技能提供舆情分类体系（7大类）、影响量化方法、行业风险加权、管理建议框架
              - 查询到负面舆情后，加载此技能，然后按其方法论逐条分析舆情事件
              - 分析输出应包括：舆情类别、严重程度、对偿债能力的影响、客户经理行动建议
            - **当需要分析企业风险标签时，必须先加载 risk_tag_analysis skill**
              - 该技能提供各风险标签的逐个判定方法和管理建议
              - 获取企业详细信息后，加载此技能，然后逐项分析 riskAssessment 中的标签
              - 注意：该技能禁止对风险标签进行量化打分或加权合成
            - 在生成报告之前，如果对某个指标的标准不确定，先加载对应的 skill 查阅

            # 交互规范
            - 必须使用 ask_user 工具进行用户交互，不要直接在对话中提问
            - 每次只调用一个 ask_user，等用户回复后再继续
            - 查询结果以 Markdown 表格展示，包括指标当前值、行业基准、达标状态
            - 对高风险或严重风险的企业，主动标注风险等级色标
            - 舆情数据和征信报告的结果要整合到最终分析中

            # 风险等级说明
            - 低风险 (LOW): 各维度表现良好，无明显风险隐患
            - 关注 (WATCH): 1-2个维度出现预警信号，需跟踪监测
            - 高风险 (HIGH): 多个维度出现严重问题，存在实质性风险
            - 严重 (CRITICAL): 多项指标严重恶化，存在重大经营危机或违约风险
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
