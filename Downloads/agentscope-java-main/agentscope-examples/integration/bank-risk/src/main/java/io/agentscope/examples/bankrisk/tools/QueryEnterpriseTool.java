package io.agentscope.examples.bankrisk.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.util.List;
import java.util.stream.Collectors;

public class QueryEnterpriseTool {

    @Tool(
            name = "query_enterprise",
            description =
                    "Search enterprise customers by name keyword, industry, or region. Returns"
                        + " basic information including industry, region, and risk tags. Use this"
                        + " tool when the user mentions an enterprise name or wants to find"
                        + " enterprises.")
    public String query(
            @ToolParam(
                            name = "keyword",
                            description = "Enterprise name keyword, industry, or region")
                    String keyword) {

        List<MockEnterpriseData.EnterpriseInfo> results = MockEnterpriseData.search(keyword);

        if (results.isEmpty()) {
            return "未找到匹配的企业客户。请尝试其他关键词。\n\n"
                    + "可查询的企业列表: "
                    + MockEnterpriseData.all().stream()
                            .map(MockEnterpriseData.EnterpriseInfo::name)
                            .collect(Collectors.joining(", "));
        }

        if (results.size() == 1) {
            return formatSingle(results.get(0));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("找到 ").append(results.size()).append(" 家匹配企业:\n\n");
        sb.append("| 企业名称 | 行业 | 地区 | 风险等级 |\n");
        sb.append("|----------|------|------|----------|\n");
        for (var e : results) {
            sb.append("| ")
                    .append(e.name())
                    .append(" | ")
                    .append(e.industry())
                    .append(" | ")
                    .append(e.region())
                    .append(" | ")
                    .append(e.riskLevelLabel())
                    .append(" |\n");
        }
        return sb.toString();
    }

    private String formatSingle(MockEnterpriseData.EnterpriseInfo e) {
        return "## "
                + e.name()
                + "\n\n"
                + "| 项目 | 内容 |\n"
                + "|------|------|\n"
                + "| 行业类型 | "
                + e.industry()
                + " |\n"
                + "| 所在地区 | "
                + e.region()
                + " |\n"
                + "| 风险等级 | **"
                + e.riskLevelLabel()
                + "** |\n"
                + "| 风险标签 | "
                + String.join(", ", e.riskTags())
                + " |\n\n"
                + "### 近期关键事件\n"
                + e.recentEvents().stream().map(ev -> "- " + ev).collect(Collectors.joining("\n"));
    }
}
