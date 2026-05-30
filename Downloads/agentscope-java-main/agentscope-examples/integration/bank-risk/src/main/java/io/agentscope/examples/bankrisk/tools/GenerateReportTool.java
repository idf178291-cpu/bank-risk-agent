package io.agentscope.examples.bankrisk.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

public class GenerateReportTool {

    @Tool(
            name = "generate_risk_report",
            description =
                    "Generate a comprehensive risk assessment report in Markdown format. Call this"
                            + " tool AFTER collecting all necessary data and user preferences. The"
                            + " report includes: executive summary, FICDG analysis, risk level"
                            + " assessment, and risk mitigation recommendations.")
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
        sb.append("# ").append(enterpriseName).append(" — 企业风险评估报告\n\n");

        sb.append("> 报告日期: 2026-05-30 | 评估框架: FICDG | 数据来源: 公开财务 + 征信 + 舆情\n\n");

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
        sb.append("| 客户编号 | ").append(e.customerId()).append(" |\n");
        sb.append("| 行业类型 | ").append(e.industry()).append(" |\n");
        sb.append("| 所在地区 | ").append(e.region()).append(" |\n");
        sb.append("| 风险标签 | ").append(String.join(", ", e.riskTags())).append(" |\n\n");

        sb.append("## 三、核心财务指标\n\n");
        sb.append("| 指标 | 当前值 | 行业基准 | 状态 |\n");
        sb.append("|------|--------|----------|------|\n");
        sb.append("| ROA (资产收益率) | ")
                .append(String.format("%.2f%%", e.roa()))
                .append(" | ≥3% | ")
                .append(e.roa() >= 3.0 ? "良好" : e.roa() >= 1.0 ? "一般" : "预警")
                .append(" |\n");
        sb.append("| 资产质量指标 | ")
                .append(String.format("%.1f%%", e.nplRatio()))
                .append(" | ≤3% | ")
                .append(e.nplRatio() <= 3.0 ? "✓" : "✗")
                .append(" |\n");
        double debtRatio = 100.0 - e.carRatio();
        sb.append("| 资产负债率 (估算) | ")
                .append(String.format("%.1f%%", debtRatio))
                .append(" | <70% | ")
                .append(debtRatio <= 70.0 ? "✓" : "✗")
                .append(" |\n");
        sb.append("| 流动比率 (估算) | ")
                .append(String.format("%.2f", e.provisionCoverage() / 100.0))
                .append(" | >1.5 | ")
                .append(e.provisionCoverage() >= 150.0 ? "✓" : "✗")
                .append(" |\n");
        sb.append("| 毛利率 (估算) | ")
                .append(String.format("%.1f%%", e.liquidityRatio()))
                .append(" | ≥10% | ")
                .append(e.liquidityRatio() >= 10.0 ? "✓" : "✗")
                .append(" |\n\n");

        sb.append("## 四、关键发现\n\n");
        sb.append(findings).append("\n\n");

        sb.append("## 五、近期关键事件\n\n");
        for (String event : e.recentEvents()) {
            sb.append("- ").append(event).append("\n");
        }
        sb.append("\n");

        if (recommendations != null && !recommendations.isBlank()) {
            sb.append("## 六、风险建议\n\n");
            for (String rec : recommendations.split("\n")) {
                String trimmed = rec.trim();
                if (!trimmed.isEmpty()) {
                    sb.append("- ").append(trimmed).append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("---\n");
        sb.append("*本报告由企业风险评估智能体自动生成，仅供参考。最终决策请结合人工判断。*\n");
        return sb.toString();
    }
}
