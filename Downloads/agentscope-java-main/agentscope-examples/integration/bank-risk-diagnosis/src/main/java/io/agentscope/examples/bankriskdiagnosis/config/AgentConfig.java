package io.agentscope.examples.bankriskdiagnosis.config;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.examples.bankriskdiagnosis.service.SessionStore;
import io.agentscope.examples.bankriskdiagnosis.tool.DocumentParseTool;
import io.agentscope.examples.bankriskdiagnosis.tool.UserInteractionTool;
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
            你是银行风险诊断专家。分析文档，给出结论，辅助用户决策。

            # 行为规则（严格按顺序执行）

            ## 步骤 1：解析文档（自主完成，不调 ask_user）
            - 调用 parse_documents(session_id="...")
            - 直接复制返回结果中的 summaryTable 到消息
            - 加载 issue_extract 和 risk_classify 两个 skill
            - 进入步骤 2，不要暂停

            ## 步骤 2：分析并输出（自主完成，不调 ask_user）
            在一条或多条连续消息中输出以下全部内容：
            ### 📋 提取的风险问题（表格：标题 | 来源文件 | 类别 | 等级）
            ### 📊 风险分类分布（表格：类别 | 问题数 | 严重/高）
            ### 🔍 TOP 3 重点风险主题（每项含证据来源和原文摘录）
            ### 💡 初步整改建议（含责任部门和紧急程度）

            ## 步骤 3：用户决策（调 ask_user，仅 1 次）
            使用 ask_user(ui_type="multi_select",
              question="下一步您希望？",
              options=[{label:"深入信用风险",value:"credit"},
                       {label:"深入合规风险",value:"compliance"},
                       {label:"深入操作风险",value:"operation"},
                       {label:"深入安全风险",value:"security"},
                       {label:"补充更多文档",value:"supplement"},
                       {label:"生成完整诊断报告",value:"report"}])

            ## 步骤 4：响应用户选择
            - 选了风险类别 → 深入分析该类别（问题详情+趋势+针对性建议）
            - 选了补充文档 → 告知用户上传后用同一 session_id 重新 parse
            - 选了生成报告 → 加载 report_generate skill，汇总全部分析，输出完整报告

            # ask_user 规则
            - 步骤 1 和 2 绝对不要调 ask_user。信息展示不需要用户确认。
            - 步骤 3 是第一个 ask_user。
            - 步骤 4 仅在发现关键信息缺口且需要用户决定时，可再调 1 次。
            - 单次对话最多 3 次 ask_user。

            # 6大风险分类
            信用风险 | 操作风险 | 安全风险 | 合规风险 | 战略与经营风险 | 声誉风险
            🔴严重 | 🟠高风险 | 🟡关注 | 🟢低风险
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
            SkillBox skillBox, Toolkit toolkit, Model model, SessionStore sessions) {
        return registry -> {
            registry.registerFactory(
                    "bank-risk-diagnosis", () -> createAgent(skillBox, toolkit, model, sessions));
            registry.registerFactory(
                    "default", () -> createAgent(skillBox, toolkit, model, sessions));
        };
    }

    private Agent createAgent(
            SkillBox skillBox, Toolkit toolkit, Model model, SessionStore sessions) {

        toolkit.registerTool(new UserInteractionTool());
        toolkit.registerTool(new DocumentParseTool(sessions));

        skillBox.bindToolkit(toolkit);

        return ReActAgent.builder()
                .name("BankRiskDiagnosisAgent")
                .sysPrompt(SYS_PROMPT)
                .model(model)
                .toolkit(toolkit)
                .skillBox(skillBox)
                .memory(new InMemoryMemory())
                .maxIters(40)
                .enablePendingToolRecovery(true)
                .build();
    }
}
