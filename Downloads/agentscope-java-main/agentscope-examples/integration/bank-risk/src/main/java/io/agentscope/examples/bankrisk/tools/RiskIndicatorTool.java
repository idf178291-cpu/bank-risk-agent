package io.agentscope.examples.bankrisk.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.util.Arrays;
import java.util.List;

public class RiskIndicatorTool {

    // Industry benchmarks for general enterprise assessment
    private static final double ROA_GOOD = 5.0;
    private static final double ROA_OK = 1.0;
    private static final double GROSS_MARGIN_GOOD = 25.0;
    private static final double GROSS_MARGIN_OK = 10.0;
    private static final double DEBT_RATIO_SAFE = 50.0;
    private static final double DEBT_RATIO_WARN = 70.0;
    private static final double CURRENT_RATIO_SAFE = 2.0;
    private static final double CURRENT_RATIO_WARN = 1.0;

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

        MockEnterpriseData.EnterpriseInfo e = MockEnterpriseData.get(enterpriseName);
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
        sb.append("## ").append(e.name()).append(" — FICDG 风险指标分析\n\n");

        for (String dim : dims) {
            switch (dim) {
                case "F" -> {
                    sb.append("### F — 财务健康 (Financial Health)\n\n");
                    sb.append("| 指标 | 当前值 | 行业基准 | 评估 |\n");
                    sb.append("|------|--------|----------|------|\n");
                    String roaEval = e.roa() >= ROA_GOOD ? "优秀" : e.roa() >= ROA_OK ? "一般" : "预警";
                    sb.append("| ROA (资产收益率) | ")
                            .append(String.format("%.2f%%", e.roa()))
                            .append(" | ≥")
                            .append(String.format("%.0f%%", ROA_OK))
                            .append(" | ")
                            .append(roaEval)
                            .append(" |\n");

                    double margin = e.liquidityRatio();
                    String marginEval =
                            margin >= GROSS_MARGIN_GOOD
                                    ? "优秀"
                                    : margin >= GROSS_MARGIN_OK ? "一般" : "预警";
                    sb.append("| 毛利率 | ")
                            .append(String.format("%.1f%%", margin))
                            .append(" | ≥")
                            .append(String.format("%.0f%%", GROSS_MARGIN_OK))
                            .append(" | ")
                            .append(marginEval)
                            .append(" |\n");

                    String growthEval = e.roa() >= ROA_GOOD ? "良好" : "需关注";
                    sb.append("| 营收增长趋势 | — | — | ").append(growthEval).append(" |\n");
                    sb.append("\n");
                }
                case "I" -> {
                    sb.append("### I — 行业地位 (Industry Position)\n\n");
                    sb.append("| 指标 | 评估 |\n");
                    sb.append("|------|------|\n");
                    sb.append("| 行业分类 | ").append(e.industry()).append(" |\n");
                    sb.append("| 所在地区 | ").append(e.region()).append(" |\n");
                    String pos;
                    if (e.riskLevel().equals("LOW")) {
                        pos = "行业龙头/领先地位";
                    } else if (e.riskLevel().equals("WATCH")) {
                        pos = "中等水平，面临竞争压力";
                    } else {
                        pos = "竞争力较弱，市场份额承压";
                    }
                    sb.append("| 竞争地位 | ").append(pos).append(" |\n");
                    sb.append("\n");
                }
                case "C" -> {
                    sb.append("### C — 合规与声誉 (Compliance & Reputation)\n\n");
                    sb.append("| 指标 | 评估 |\n");
                    sb.append("|------|------|\n");
                    sb.append("| 风险标签 | ").append(String.join(", ", e.riskTags())).append(" |\n");
                    sb.append("| 近期事件 | 见下方详细列表 |\n");
                    sb.append("\n");
                }
                case "D" -> {
                    sb.append("### D — 债务风险 (Debt Risk)\n\n");
                    sb.append("| 指标 | 当前值 | 安全线 | 评估 |\n");
                    sb.append("|------|--------|--------|------|\n");
                    double debtRatio = 100.0 - e.carRatio();
                    String debtEval =
                            debtRatio <= DEBT_RATIO_SAFE
                                    ? "安全"
                                    : debtRatio <= DEBT_RATIO_WARN ? "关注" : "危险";
                    sb.append("| 资产负债率 (估算) | ")
                            .append(String.format("%.1f%%", debtRatio))
                            .append(" | <")
                            .append(String.format("%.0f%%", DEBT_RATIO_SAFE))
                            .append(" | ")
                            .append(debtEval)
                            .append(" |\n");

                    double currentRatio = e.provisionCoverage() / 100.0;
                    String curEval =
                            currentRatio >= CURRENT_RATIO_SAFE
                                    ? "安全"
                                    : currentRatio >= CURRENT_RATIO_WARN ? "关注" : "危险";
                    sb.append("| 流动比率 (估算) | ")
                            .append(String.format("%.2f", currentRatio))
                            .append(" | >")
                            .append(String.format("%.1f", CURRENT_RATIO_SAFE))
                            .append(" | ")
                            .append(curEval)
                            .append(" |\n");

                    String nplEval = e.nplRatio() <= 3.0 ? "安全" : "预警";
                    sb.append("| 资产质量指标 | ")
                            .append(String.format("%.1f%%", e.nplRatio()))
                            .append(" | ≤3.0% | ")
                            .append(nplEval)
                            .append(" |\n");
                    sb.append("\n");
                }
                case "G" -> {
                    sb.append("### G — 运营与治理 (Governance & Operations)\n\n");
                    sb.append("| 指标 | 评估 |\n");
                    sb.append("|------|------|\n");
                    String gov;
                    if (e.riskLevel().equals("LOW")) {
                        gov = "治理结构健全，运营稳定";
                    } else if (e.riskLevel().equals("WATCH")) {
                        gov = "存在个别治理瑕疵，需关注";
                    } else if (e.riskLevel().equals("HIGH")) {
                        gov = "治理存在明显缺陷";
                    } else {
                        gov = "治理严重失序，运营面临中断风险";
                    }
                    sb.append("| 治理评估 | ").append(gov).append(" |\n");
                    sb.append("| 供应链风险 | 基于行业和地区评估 |\n");
                    sb.append("\n");
                }
                default -> sb.append("未知维度: ").append(dim).append("\n\n");
            }
        }

        sb.append("---\n");
        sb.append("**综合风险等级: ").append(e.riskLevelLabel()).append("**  \n");
        sb.append("数据来源: 企业公开财务数据 + 征信报告 + 舆情监测\n");
        return sb.toString();
    }
}
