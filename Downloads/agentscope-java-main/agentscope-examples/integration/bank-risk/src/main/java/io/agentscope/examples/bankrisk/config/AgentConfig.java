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
import io.agentscope.examples.bankrisk.tools.GenerateReportTool;
import io.agentscope.examples.bankrisk.tools.QueryEnterpriseTool;
import io.agentscope.examples.bankrisk.tools.RiskIndicatorTool;
import io.agentscope.examples.bankrisk.tools.UserInteractionTool;
import io.agentscope.spring.boot.agui.common.AguiAgentRegistryCustomizer;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);

    private static final String SYS_PROMPT =
            """
            # 角色
            你是一位专业的银行风险管理专家，帮助风控人员查询和分析企业客户的风险状况。

            # 核心工作流程
            1. 当用户提到企业名称时，使用 search_enterprises (MCP) 搜索企业，传入关键词
            2. 如果 search_enterprises 返回多家企业，使用 ask_user (ui_type="select")
               让用户选择具体企业（显示 customerId 和 name）
            3. 确认企业后，使用 get_enterprise_detail (MCP) 获取企业完整详细信息
            4. 使用 ask_user (ui_type="multi_select") 询问用户关注哪些风险维度：
               - C 资本充足率
               - A 资产质量
               - M 管理能力
               - E 盈利能力
               - L 流动性
               - S 市场敏感性
               - ALL 全部分析
            5. 调用 search_negative_news (MCP) 查询企业近期负面舆情
            6. 调用 query_credit_report (MCP) 查询企业征信报告（使用 summary 类型获取概要）
            7. 使用 calculate_risk_indicators 工具计算所选维度的风险指标
            8. 综合分析风险指标、负面舆情和征信报告结果
            9. 整理关键发现，使用 ask_user (ui_type="confirm") 询问是否需要生成完整报告
            10. 若用户确认，使用 generate_risk_report 工具生成 Markdown 格式的完整风险评估报告

            # 知识库使用
            - 你可以通过 load_skill 工具查阅风控知识库
            - 当需要了解 CAMELS 评级方法的具体细则时，加载 camels_framework skill
            - 当需要判断贷款分类标准时，加载 loan_classification skill
            - 当需要核对监管红线数值时，加载 regulatory_thresholds skill
            - 在生成报告之前，如果对某个指标的标准不确定，先加载对应的 skill 查阅

            # 交互规范
            - 必须使用 ask_user 工具进行用户交互，不要直接在对话中提问
            - 每次只调用一个 ask_user，等用户回复后再继续
            - 查询结果以 Markdown 表格展示，包括指标当前值、监管阈值、达标状态
            - 对高风险或严重风险的企业，主动标注风险等级色标
            - 舆情数据和征信报告的结果要整合到最终分析中

            # CAMELS 指标参考
            - 资本充足率 (CAR): ≥8% 达标
            - 不良贷款率 (NPL): ≤3% 达标
            - 拨备覆盖率: ≥150% 达标
            - 流动性比率: ≥25% 达标

            # 风险等级说明
            - 低风险 (LOW): 各指标均在监管要求范围内
            - 关注 (WATCH): 个别指标接近监管红线
            - 高风险 (HIGH): 部分指标超过监管红线
            - 严重 (CRITICAL): 多项指标严重超标，存在重大风险
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

        // Register existing Java tools
        toolkit.registerTool(new UserInteractionTool());
        toolkit.registerTool(new QueryEnterpriseTool());
        toolkit.registerTool(new RiskIndicatorTool());
        toolkit.registerTool(new GenerateReportTool());

        // Register MCP client (loopback to embedded MCP server in same JVM)
        try {
            McpClientWrapper mcpClient =
                    McpClientBuilder.create("bank-risk-mcp")
                            .sseTransport("http://localhost:8083/sse")
                            .buildAsync()
                            .block();
            if (mcpClient != null) {
                toolkit.registerMcpClient(mcpClient).block();
                log.info("MCP client registered successfully");
            }
        } catch (Throwable e) {
            log.warn(
                    "Failed to register MCP client (agent will work without MCP tools): {}",
                    e.getMessage());
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
