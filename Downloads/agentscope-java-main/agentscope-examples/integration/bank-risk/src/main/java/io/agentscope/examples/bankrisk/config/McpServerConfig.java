package io.agentscope.examples.bankrisk.config;

import io.agentscope.examples.bankrisk.mcp.CreditReportTool;
import io.agentscope.examples.bankrisk.mcp.DataLoader;
import io.agentscope.examples.bankrisk.mcp.EnterpriseDetailTool;
import io.agentscope.examples.bankrisk.mcp.EnterpriseSearchTool;
import io.agentscope.examples.bankrisk.mcp.NegativeNewsTool;
import io.agentscope.examples.bankrisk.tools.GenerateReportTool;
import io.agentscope.examples.bankrisk.tools.RiskIndicatorTool;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;

@Configuration
public class McpServerConfig {

    private static final Logger log = LoggerFactory.getLogger(McpServerConfig.class);

    private final NegativeNewsTool negativeNewsTool;
    private final CreditReportTool creditReportTool;
    private final EnterpriseSearchTool enterpriseSearchTool;
    private final EnterpriseDetailTool enterpriseDetailTool;
    private final RiskIndicatorTool riskIndicatorTool;
    private final GenerateReportTool generateReportTool;

    public McpServerConfig(DataLoader dataLoader) {
        this.negativeNewsTool = new NegativeNewsTool(dataLoader);
        this.creditReportTool = new CreditReportTool(dataLoader);
        this.enterpriseSearchTool = new EnterpriseSearchTool(dataLoader);
        this.enterpriseDetailTool = new EnterpriseDetailTool(dataLoader);
        this.riskIndicatorTool = new RiskIndicatorTool(dataLoader);
        this.generateReportTool = new GenerateReportTool(dataLoader);
    }

    @Bean
    public WebFluxSseServerTransportProvider webFluxSseServerTransportProvider() {
        return WebFluxSseServerTransportProvider.builder().messageEndpoint("/mcp/message").build();
    }

    @Bean
    public RouterFunction<?> mcpRouterFunction(
            WebFluxSseServerTransportProvider transportProvider) {
        return transportProvider.getRouterFunction();
    }

    @SuppressWarnings("unchecked")
    @Bean
    public McpSyncServer mcpSyncServer(WebFluxSseServerTransportProvider transportProvider) {
        McpServer.SyncSpecification spec =
                McpServer.sync(transportProvider)
                        .serverInfo("bank-risk-mcp", "1.0.0")
                        .capabilities(ServerCapabilities.builder().tools(true).build());

        spec.tool(
                new Tool(
                        "search_negative_news",
                        null,
                        "Search negative public opinion/news for an enterprise. "
                                + "Returns a list of negative news articles with date, title, "
                                + "source, risk level, and summary.",
                        searchNegativeNewsSchema(),
                        null,
                        null,
                        null),
                (ex, args) -> {
                    String result = negativeNewsTool.searchNegativeNews((Map<String, Object>) args);
                    return new CallToolResult(result, false);
                });

        spec.tool(
                new Tool(
                        "query_credit_report",
                        null,
                        "Query credit report for an enterprise. "
                                + "Returns credit score, rating, loan records, "
                                + "guarantee records, default records, and summary.",
                        queryCreditReportSchema(),
                        null,
                        null,
                        null),
                (ex, args) -> {
                    String result = creditReportTool.queryCreditReport((Map<String, Object>) args);
                    return new CallToolResult(result, false);
                });

        spec.tool(
                new Tool(
                        "search_enterprises",
                        null,
                        "Search for enterprises by keyword. Returns a JSON array of matching"
                            + " enterprises with customerId and name. Supports fuzzy matching on"
                            + " enterprise name. Use this to find the customerId for an"
                            + " enterprise.",
                        searchEnterprisesSchema(),
                        null,
                        null,
                        null),
                (ex, args) -> {
                    String result =
                            enterpriseSearchTool.searchEnterprises((Map<String, Object>) args);
                    return new CallToolResult(result, false);
                });

        spec.tool(
                new Tool(
                        "get_enterprise_detail",
                        null,
                        "Get full enterprise detail by customer ID (e.g. CUST-001). "
                                + "Returns all fields including financial indicators, risk tags, "
                                + "and recent events.",
                        getEnterpriseDetailSchema(),
                        null,
                        null,
                        null),
                (ex, args) -> {
                    String result =
                            enterpriseDetailTool.getEnterpriseDetail((Map<String, Object>) args);
                    return new CallToolResult(result, false);
                });

        spec.tool(
                new Tool(
                        "calculate_risk_indicators",
                        null,
                        "Calculate FICDG risk indicators for an enterprise."
                                + " Financial health (F), Industry position (I), Compliance (C),"
                                + " Debt risk (D), Governance (G). Returns each dimension with"
                                + " industry benchmark comparisons.",
                        calculateRiskIndicatorsSchema(),
                        null,
                        null,
                        null),
                (ex, args) -> {
                    String result =
                            riskIndicatorTool.calculate(
                                    (String) ((Map<String, Object>) args).get("enterprise_name"),
                                    (String) ((Map<String, Object>) args).get("dimensions"));
                    return new CallToolResult(result, false);
                });

        spec.tool(
                new Tool(
                        "generate_risk_report",
                        null,
                        "Generate a comprehensive risk assessment report in Markdown format."
                                + " Call this AFTER collecting all necessary data. The report"
                                + " includes: executive summary, FICDG analysis, risk level"
                                + " assessment, and risk mitigation recommendations.",
                        generateRiskReportSchema(),
                        null,
                        null,
                        null),
                (ex, args) -> {
                    String result =
                            generateReportTool.generate(
                                    (String) ((Map<String, Object>) args).get("enterprise_name"),
                                    (String) ((Map<String, Object>) args).get("findings"),
                                    (String) ((Map<String, Object>) args).get("risk_level"),
                                    (String) ((Map<String, Object>) args).get("recommendations"));
                    return new CallToolResult(result, false);
                });

        log.info(
                "MCP SyncServer registered with 6 tools: search_negative_news, "
                        + "query_credit_report, search_enterprises, get_enterprise_detail,"
                        + " calculate_risk_indicators, generate_risk_report");
        return spec.build();
    }

    private static JsonSchema searchNegativeNewsSchema() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(
                "enterprise_name",
                Map.of("type", "string", "description", "Enterprise name to search for"));
        properties.put(
                "days",
                Map.of(
                        "type",
                        "integer",
                        "description",
                        "Number of days to look back (default 90)"));
        return new JsonSchema("object", properties, List.of("enterprise_name"), null, null, null);
    }

    private static JsonSchema queryCreditReportSchema() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(
                "enterprise_name",
                Map.of("type", "string", "description", "Enterprise name to query"));
        properties.put(
                "report_type",
                Map.of(
                        "type",
                        "string",
                        "description",
                        "Report type: 'full' or 'summary' (default 'full')"));
        return new JsonSchema("object", properties, List.of("enterprise_name"), null, null, null);
    }

    private static JsonSchema searchEnterprisesSchema() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(
                "keyword",
                Map.of(
                        "type",
                        "string",
                        "description",
                        "Enterprise name keyword (fuzzy, case-insensitive). "
                                + "Empty returns all enterprises."));
        return new JsonSchema("object", properties, List.of(), null, null, null);
    }

    private static JsonSchema getEnterpriseDetailSchema() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(
                "customer_id",
                Map.of(
                        "type",
                        "string",
                        "description",
                        "Exact customer ID of the enterprise (e.g. CUST-001)"));
        return new JsonSchema("object", properties, List.of("customer_id"), null, null, null);
    }

    private static JsonSchema calculateRiskIndicatorsSchema() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(
                "enterprise_name",
                Map.of("type", "string", "description", "Exact enterprise name"));
        properties.put(
                "dimensions",
                Map.of(
                        "type",
                        "string",
                        "description",
                        "FICDG dimensions to analyze: F, I, C, D, G. Use ALL for all"
                                + " dimensions."));
        return new JsonSchema(
                "object", properties, List.of("enterprise_name", "dimensions"), null, null, null);
    }

    private static JsonSchema generateRiskReportSchema() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(
                "enterprise_name", Map.of("type", "string", "description", "Enterprise name"));
        properties.put(
                "findings",
                Map.of("type", "string", "description", "Key findings from risk analysis"));
        properties.put(
                "risk_level",
                Map.of(
                        "type",
                        "string",
                        "description",
                        "Overall risk level: LOW, WATCH, HIGH, CRITICAL"));
        properties.put(
                "recommendations",
                Map.of(
                        "type",
                        "string",
                        "description",
                        "Risk mitigation recommendations (one per line)"));
        return new JsonSchema(
                "object",
                properties,
                List.of("enterprise_name", "findings", "risk_level"),
                null,
                null,
                null);
    }
}
