package io.agentscope.examples.bankrisk.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

public class GenerateReportTool {

    @Tool(
            name = "generate_risk_report",
            description =
                    "Generate a comprehensive risk assessment report in Markdown format. Call this"
                            + " tool AFTER collecting all necessary data and user preferences. The"
                            + " report includes: executive summary, detailed CAMELS analysis, risk"
                            + " level assessment, and supervisory recommendations.")
    public String generate(
            @ToolParam(name = "enterprise_name", description = "Enterprise name")
                    String enterpriseName,
            @ToolParam(name = "findings", description = "Key findings from risk analysis")
                    String findings,
            @ToolParam(
                            name = "risk_level",
                            description = "Overall risk level: LOW, WATCH, HIGH, CRITICAL")
                    String riskLevel,
            @ToolParam(
                            name = "recommendations",
                            description =
                                    "Risk mitigation recommendations (one per line, separated by"
                                            + " newline)",
                            required = false)
                    String recommendations) {

        MockEnterpriseData.EnterpriseInfo e = MockEnterpriseData.get(enterpriseName);
        if (e == null) {
            return "未找到企业: " + enterpriseName;
        }

        String levelColor;
        String levelLabel;
        switch (riskLevel.toUpperCase()) {
            case "CRITICAL" -> {
                levelColor = "#ef4444";
                levelLabel = "严重";
            }
            case "HIGH" -> {
                levelColor = "#f97316";
                levelLabel = "高风险";
            }
            case "WATCH" -> {
                levelColor = "#eab308";
                levelLabel = "关注";
            }
            default -> {
                levelColor = "#22c55e";
                levelLabel = "低风险";
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# 🏦 ").append(enterpriseName).append(" — 企业客户风险评估报告\n\n");

        sb.append("> 报告日期: 2026-05-28 | 数据截止: 2025Q1 | 评估框架: CAMELS\n\n");

        sb.append("## 一、综合风险评级\n\n");
        sb.append("<div style=\"padding:16px;border-radius:8px;background:")
                .append(levelColor)
                .append("20;border:2px solid ")
                .append(levelColor)
                .append(";\">\n\n");
        sb.append("### 风险等级: <span style=\"color:")
                .append(levelColor)
                .append(";\">**")
                .append(levelLabel)
                .append("**</span>\n\n");
        sb.append("</div>\n\n");

        sb.append("## 二、企业基本信息\n\n");
        sb.append("| 项目 | 内容 |\n");
        sb.append("|------|------|\n");
        sb.append("| 企业名称 | ").append(e.name()).append(" |\n");
        sb.append("| 行业类型 | ").append(e.industry()).append(" |\n");
        sb.append("| 所在地区 | ").append(e.region()).append(" |\n");
        sb.append("| 风险标签 | ").append(String.join(", ", e.riskTags())).append(" |\n\n");

        sb.append("## 三、核心财务指标\n\n");
        sb.append("| 指标 | 当前值 | 监管阈值 | 状态 |\n");
        sb.append("|------|--------|----------|------|\n");
        sb.append("| 资本充足率 (CAR) | ")
                .append(String.format("%.1f%%", e.carRatio()))
                .append(" | ≥8.0% | ")
                .append(e.carRatio() >= 8.0 ? "✓" : "✗")
                .append(" |\n");
        sb.append("| 不良贷款率 (NPL) | ")
                .append(String.format("%.1f%%", e.nplRatio()))
                .append(" | ≤3.0% | ")
                .append(e.nplRatio() <= 3.0 ? "✓" : "✗")
                .append(" |\n");
        sb.append("| 拨备覆盖率 | ")
                .append(String.format("%.1f%%", e.provisionCoverage()))
                .append(" | ≥150% | ")
                .append(e.provisionCoverage() >= 150.0 ? "✓" : "✗")
                .append(" |\n");
        sb.append("| 流动性比率 | ")
                .append(String.format("%.1f%%", e.liquidityRatio()))
                .append(" | ≥25% | ")
                .append(e.liquidityRatio() >= 25.0 ? "✓" : "✗")
                .append(" |\n");
        sb.append("| 资产收益率 (ROA) | ")
                .append(String.format("%.2f%%", e.roa()))
                .append(" | — | ")
                .append(e.roa() >= 0.5 ? "良好" : e.roa() >= 0 ? "一般" : "亏损")
                .append(" |\n\n");

        sb.append("## 四、关键发现\n\n");
        sb.append(findings).append("\n\n");

        sb.append("## 五、近期关键事件\n\n");
        for (String event : e.recentEvents()) {
            sb.append("- ").append(event).append("\n");
        }
        sb.append("\n");

        if (recommendations != null && !recommendations.isBlank()) {
            sb.append("## 六、监管建议\n\n");
            for (String rec : recommendations.split("\n")) {
                String trimmed = rec.trim();
                if (!trimmed.isEmpty()) {
                    sb.append("- ").append(trimmed).append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("---\n");
        sb.append("*本报告由 BankRiskAgent 自动生成，仅供参考。最终决策请结合人工判断。*\n");
        return sb.toString();
    }
}
