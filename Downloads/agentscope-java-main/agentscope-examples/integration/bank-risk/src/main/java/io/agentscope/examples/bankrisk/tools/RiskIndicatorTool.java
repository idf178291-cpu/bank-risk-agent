package io.agentscope.examples.bankrisk.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.examples.bankrisk.mcp.DataLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RiskIndicatorTool {

    private static final double ROA_GOOD = 5.0;
    private static final double ROA_OK = 1.0;
    private static final double GROSS_MARGIN_GOOD = 25.0;
    private static final double GROSS_MARGIN_OK = 10.0;
    private static final double DEBT_RATIO_SAFE = 50.0;
    private static final double DEBT_RATIO_WARN = 70.0;
    private static final double CURRENT_RATIO_SAFE = 2.0;
    private static final double CURRENT_RATIO_WARN = 1.0;

    private final DataLoader dataLoader;

    public RiskIndicatorTool(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    @Tool(
            name = "calculate_risk_indicators",
            description =
                    "Calculate FICDG risk indicators for an enterprise. Financial health (F),"
                        + " Industry position (I), Compliance (C), Debt risk (D), Governance (G)."
                        + " Returns each dimension with industry benchmark comparisons.")
    public String calculate(
            @ToolParam(name = "enterprise_name", description = "Exact enterprise name")
                    String enterpriseName,
            @ToolParam(
                            name = "dimensions",
                            description =
                                    "FICDG dimensions to analyze: F, I, C, D, G. Use ALL for all"
                                            + " dimensions.")
                    String dimensions) {

        Map<String, Object> e = dataLoader.getEnterprise(enterpriseName);
        if (e == null) {
            return "未找到企业: " + enterpriseName + "。请先使用 search_enterprises 查询。";
        }

        List<String> dims;
        if (dimensions == null || dimensions.equalsIgnoreCase("ALL")) {
            dims = List.of("F", "I", "C", "D", "G");
        } else {
            dims =
                    Arrays.stream(dimensions.split(","))
                            .map(String::trim)
                            .map(String::toUpperCase)
                            .toList();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(e.get("name")).append(" — FICDG 风险指标分析\n\n");

        for (String dim : dims) {
            switch (dim) {
                case "F" -> appendFinancial(sb, e);
                case "I" -> appendIndustry(sb, e);
                case "C" -> appendCompliance(sb, e);
                case "D" -> appendDebt(sb, e);
                case "G" -> appendGovernance(sb, e);
                default -> sb.append("未知维度: ").append(dim).append("\n\n");
            }
        }

        sb.append("---\n");
        sb.append("**综合风险等级: ").append(e.get("riskLevelLabel")).append("**  \n");
        sb.append("数据来源: 企业公开财务数据 + 征信报告 + 舆情监测\n");
        return sb.toString();
    }

    private void appendFinancial(StringBuilder sb, Map<String, Object> e) {
        double roa = toDouble(e.get("roa"));
        double margin = toDouble(e.get("liquidityRatio"));

        sb.append("### F — 财务健康 (Financial Health)\n\n");
        sb.append("| 指标 | 当前值 | 行业基准 | 评估 |\n");
        sb.append("|------|--------|----------|------|\n");
        sb.append("| ROA (资产收益率) | ")
                .append(String.format("%.2f%%", roa))
                .append(" | ≥")
                .append(String.format("%.0f%%", ROA_OK))
                .append(" | ")
                .append(roa >= ROA_GOOD ? "优秀" : roa >= ROA_OK ? "一般" : "预警")
                .append(" |\n");
        sb.append("| 毛利率 | ")
                .append(String.format("%.1f%%", margin))
                .append(" | ≥")
                .append(String.format("%.0f%%", GROSS_MARGIN_OK))
                .append(" | ")
                .append(
                        margin >= GROSS_MARGIN_GOOD
                                ? "优秀"
                                : margin >= GROSS_MARGIN_OK ? "一般" : "预警")
                .append(" |\n");
        sb.append("| 营收增长趋势 | — | — | ").append(roa >= ROA_GOOD ? "良好" : "需关注").append(" |\n\n");
    }

    private void appendIndustry(StringBuilder sb, Map<String, Object> e) {
        String riskLevel = (String) e.get("riskLevel");
        String pos;
        if ("LOW".equals(riskLevel)) {
            pos = "行业龙头/领先地位";
        } else if ("WATCH".equals(riskLevel)) {
            pos = "中等水平，面临竞争压力";
        } else {
            pos = "竞争力较弱，市场份额承压";
        }

        sb.append("### I — 行业地位 (Industry Position)\n\n");
        sb.append("| 指标 | 评估 |\n");
        sb.append("|------|------|\n");
        sb.append("| 行业分类 | ").append(e.get("industry")).append(" |\n");
        sb.append("| 所在地区 | ").append(e.get("region")).append(" |\n");
        sb.append("| 竞争地位 | ").append(pos).append(" |\n\n");
    }

    @SuppressWarnings("unchecked")
    private void appendCompliance(StringBuilder sb, Map<String, Object> e) {
        List<String> tags = (List<String>) e.get("riskTags");
        sb.append("### C — 合规与声誉 (Compliance & Reputation)\n\n");
        sb.append("| 指标 | 评估 |\n");
        sb.append("|------|------|\n");
        sb.append("| 风险标签 | ").append(String.join(", ", tags)).append(" |\n");
        sb.append("| 近期事件 | 见下方详细列表 |\n\n");
    }

    private void appendDebt(StringBuilder sb, Map<String, Object> e) {
        double carRatio = toDouble(e.get("carRatio"));
        double provisionCoverage = toDouble(e.get("provisionCoverage"));
        double nplRatio = toDouble(e.get("nplRatio"));
        double debtRatio = 100.0 - carRatio;
        double currentRatio = provisionCoverage / 100.0;

        sb.append("### D — 债务风险 (Debt Risk)\n\n");
        sb.append("| 指标 | 当前值 | 安全线 | 评估 |\n");
        sb.append("|------|--------|--------|------|\n");
        sb.append("| 资产负债率 (估算) | ")
                .append(String.format("%.1f%%", debtRatio))
                .append(" | <")
                .append(String.format("%.0f%%", DEBT_RATIO_SAFE))
                .append(" | ")
                .append(
                        debtRatio <= DEBT_RATIO_SAFE
                                ? "安全"
                                : debtRatio <= DEBT_RATIO_WARN ? "关注" : "危险")
                .append(" |\n");
        sb.append("| 流动比率 (估算) | ")
                .append(String.format("%.2f", currentRatio))
                .append(" | >")
                .append(String.format("%.1f", CURRENT_RATIO_SAFE))
                .append(" | ")
                .append(
                        currentRatio >= CURRENT_RATIO_SAFE
                                ? "安全"
                                : currentRatio >= CURRENT_RATIO_WARN ? "关注" : "危险")
                .append(" |\n");
        sb.append("| 资产质量指标 | ")
                .append(String.format("%.1f%%", nplRatio))
                .append(" | ≤3.0% | ")
                .append(nplRatio <= 3.0 ? "安全" : "预警")
                .append(" |\n\n");
    }

    private void appendGovernance(StringBuilder sb, Map<String, Object> e) {
        String riskLevel = (String) e.get("riskLevel");
        String gov;
        if ("LOW".equals(riskLevel)) {
            gov = "治理结构健全，运营稳定";
        } else if ("WATCH".equals(riskLevel)) {
            gov = "存在个别治理瑕疵，需关注";
        } else if ("HIGH".equals(riskLevel)) {
            gov = "治理存在明显缺陷";
        } else {
            gov = "治理严重失序，运营面临中断风险";
        }

        sb.append("### G — 运营与治理 (Governance & Operations)\n\n");
        sb.append("| 指标 | 评估 |\n");
        sb.append("|------|------|\n");
        sb.append("| 治理评估 | ").append(gov).append(" |\n");
        sb.append("| 供应链风险 | 基于行业和地区评估 |\n\n");
    }

    private static double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return 0.0;
    }
}
