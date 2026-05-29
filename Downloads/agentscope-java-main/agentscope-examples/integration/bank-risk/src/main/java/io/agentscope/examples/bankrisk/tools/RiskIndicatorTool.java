package io.agentscope.examples.bankrisk.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.util.Arrays;
import java.util.List;

public class RiskIndicatorTool {

    /**
     * 监管阈值参考
     */
    private static final double CAR_MIN = 8.0;

    private static final double NPL_MAX = 3.0;
    private static final double PROVISION_MIN = 150.0;
    private static final double LIQUIDITY_MIN = 25.0;

    @Tool(
            name = "calculate_risk_indicators",
            description =
                    "Calculate CAMELS risk indicators for a specific enterprise."
                            + " Returns capital adequacy (C), asset quality (A), management (M),"
                            + " earnings (E), liquidity (L), and market sensitivity (S) indicators"
                            + " with regulatory threshold comparisons.")
    public String calculate(
            @ToolParam(name = "enterprise_name", description = "Exact enterprise name")
                    String enterpriseName,
            @ToolParam(
                            name = "dimensions",
                            description =
                                    "CAMELS dimensions to analyze: C, A, M, E, L, S. Use ALL for"
                                            + " all dimensions.")
                    String dimensions) {

        MockEnterpriseData.EnterpriseInfo e = MockEnterpriseData.get(enterpriseName);
        if (e == null) {
            return "未找到企业: " + enterpriseName + "。请先使用 query_enterprise 查询。";
        }

        List<String> dims;
        if (dimensions == null || dimensions.equalsIgnoreCase("ALL")) {
            dims = List.of("C", "A", "M", "E", "L", "S");
        } else {
            dims =
                    Arrays.stream(dimensions.split(","))
                            .map(String::trim)
                            .map(String::toUpperCase)
                            .toList();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(e.name()).append(" — CAMELS 风险指标分析\n\n");

        for (String dim : dims) {
            switch (dim) {
                case "C" -> {
                    sb.append("### C — 资本充足率 (Capital Adequacy)\n\n");
                    sb.append("| 指标 | 当前值 | 监管要求 | 状态 |\n");
                    sb.append("|------|--------|----------|------|\n");
                    String carStatus = e.carRatio() >= CAR_MIN ? "✓ 达标" : "✗ 不达标";
                    sb.append("| 资本充足率 (CAR) | ")
                            .append(String.format("%.1f%%", e.carRatio()))
                            .append(" | ≥")
                            .append(String.format("%.1f%%", CAR_MIN))
                            .append(" | ")
                            .append(carStatus)
                            .append(" |\n");
                    sb.append("\n");
                }
                case "A" -> {
                    sb.append("### A — 资产质量 (Asset Quality)\n\n");
                    sb.append("| 指标 | 当前值 | 监管要求 | 状态 |\n");
                    sb.append("|------|--------|----------|------|\n");
                    String nplStatus = e.nplRatio() <= NPL_MAX ? "✓ 达标" : "✗ 超标";
                    sb.append("| 不良贷款率 (NPL) | ")
                            .append(String.format("%.1f%%", e.nplRatio()))
                            .append(" | ≤")
                            .append(String.format("%.1f%%", NPL_MAX))
                            .append(" | ")
                            .append(nplStatus)
                            .append(" |\n");
                    String provStatus = e.provisionCoverage() >= PROVISION_MIN ? "✓ 达标" : "✗ 不足";
                    sb.append("| 拨备覆盖率 | ")
                            .append(String.format("%.1f%%", e.provisionCoverage()))
                            .append(" | ≥")
                            .append(String.format("%.1f%%", PROVISION_MIN))
                            .append(" | ")
                            .append(provStatus)
                            .append(" |\n");
                    sb.append("\n");
                }
                case "M" -> {
                    sb.append("### M — 管理能力 (Management)\n\n");
                    sb.append("| 指标 | 评估 |\n");
                    sb.append("|------|------|\n");
                    sb.append("| 风险标签 | ").append(String.join(", ", e.riskTags())).append(" |\n");
                    sb.append("| 监管评级趋势 | 基于近期事件评估 |\n");
                    sb.append("\n");
                }
                case "E" -> {
                    sb.append("### E — 盈利能力 (Earnings)\n\n");
                    sb.append("| 指标 | 当前值 | 评估 |\n");
                    sb.append("|------|--------|------|\n");
                    String roaEval = e.roa() >= 0.5 ? "良好" : e.roa() >= 0 ? "一般" : "亏损";
                    sb.append("| ROA (资产收益率) | ")
                            .append(String.format("%.2f%%", e.roa()))
                            .append(" | ")
                            .append(roaEval)
                            .append(" |\n");
                    sb.append("\n");
                }
                case "L" -> {
                    sb.append("### L — 流动性 (Liquidity)\n\n");
                    sb.append("| 指标 | 当前值 | 监管要求 | 状态 |\n");
                    sb.append("|------|--------|----------|------|\n");
                    String liqStatus = e.liquidityRatio() >= LIQUIDITY_MIN ? "✓ 达标" : "✗ 不足";
                    sb.append("| 流动性比率 | ")
                            .append(String.format("%.1f%%", e.liquidityRatio()))
                            .append(" | ≥")
                            .append(String.format("%.1f%%", LIQUIDITY_MIN))
                            .append(" | ")
                            .append(liqStatus)
                            .append(" |\n");
                    sb.append("\n");
                }
                case "S" -> {
                    sb.append("### S — 市场敏感性 (Market Sensitivity)\n\n");
                    sb.append("| 指标 | 评估 |\n");
                    sb.append("|------|------|\n");
                    String sensitivity;
                    if (e.riskLevel().equals("CRITICAL") || e.riskLevel().equals("HIGH")) {
                        sensitivity = "高度敏感 — 信用评级可能面临下调压力";
                    } else if (e.riskLevel().equals("WATCH")) {
                        sensitivity = "中度敏感 — 需关注利率和信用环境变化";
                    } else {
                        sensitivity = "低敏感 — 市场风险敞口可控";
                    }
                    sb.append("| 市场风险评估 | ").append(sensitivity).append(" |\n");
                    sb.append("\n");
                }
                default -> sb.append("未知维度: ").append(dim).append("\n\n");
            }
        }

        sb.append("---\n");
        sb.append("**综合风险等级: ").append(e.riskLevelLabel()).append("**  \n");
        sb.append("数据来源: 2025Q1 监管报告 + 公开财务数据\n");
        return sb.toString();
    }
}
