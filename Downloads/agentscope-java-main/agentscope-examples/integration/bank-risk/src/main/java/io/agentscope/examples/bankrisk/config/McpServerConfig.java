package io.agentscope.examples.bankrisk.config;

import io.agentscope.examples.bankrisk.controller.ReportController;
import io.agentscope.examples.bankrisk.mcp.AlertSearchTool;
import io.agentscope.examples.bankrisk.mcp.DataLoader;
import io.agentscope.examples.bankrisk.mcp.EnterpriseDetailTool;
import io.agentscope.examples.bankrisk.mcp.EnterpriseSearchTool;
import io.agentscope.examples.bankrisk.mcp.NegativeNewsTool;
import io.agentscope.examples.bankrisk.mcp.ParseDocumentTool;
import io.agentscope.examples.bankrisk.service.SessionStore;
import io.agentscope.examples.bankrisk.tools.GenerateReportTool;
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
    private final EnterpriseSearchTool enterpriseSearchTool;
    private final EnterpriseDetailTool enterpriseDetailTool;
    private final GenerateReportTool generateReportTool;
    private final AlertSearchTool alertSearchTool;
    private final ParseDocumentTool parseDocumentTool;

    public McpServerConfig(DataLoader dataLoader, SessionStore sessionStore) {
        this.negativeNewsTool = new NegativeNewsTool(dataLoader);
        this.enterpriseSearchTool = new EnterpriseSearchTool(dataLoader);
        this.enterpriseDetailTool = new EnterpriseDetailTool(dataLoader);
        this.generateReportTool = new GenerateReportTool(dataLoader);
        this.alertSearchTool = new AlertSearchTool(dataLoader);
        this.parseDocumentTool = new ParseDocumentTool(sessionStore);
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
                        "generate_risk_report",
                        null,
                        "Generate a comprehensive risk assessment report in Markdown format. Call"
                            + " this AFTER collecting all necessary data. The report includes:"
                            + " executive summary, risk dimension analysis, risk level assessment,"
                            + " and risk mitigation recommendations.",
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

        spec.tool(
                new Tool(
                        "search_alerts",
                        null,
                        "Search risk alerts (预警) for an enterprise. "
                                + "Returns a list of alerts with alert name, category, level "
                                + "(红色/橙色/蓝色), release time, detail, and verification "
                                + "result. Supports optional filtering by alert_category "
                                + "(信用类, 合规类, 操作类, 声誉类) and alert_level.",
                        searchAlertsSchema(),
                        null,
                        null,
                        null),
                (ex, args) -> {
                    String result = alertSearchTool.searchAlerts((Map<String, Object>) args);
                    return new CallToolResult(result, false);
                });

        spec.tool(
                new Tool(
                        "parse_documents",
                        null,
                        "Get all parsed text content for a document upload session. "
                                + "Returns concatenated text from all uploaded files "
                                + "(Word/Excel/OFD) with file markers, plus session metadata "
                                + "(institution name, source file list, total char count). "
                                + "Call this after the user uploads files to get the full "
                                + "text for analysis.",
                        parseDocumentsSchema(),
                        null,
                        null,
                        null),
                (ex, args) -> {
                    String result = parseDocumentTool.parseDocuments((Map<String, Object>) args);
                    return new CallToolResult(result, false);
                });

        spec.tool(
                new Tool(
                        "save_report_html",
                        null,
                        "Save an HTML report and return a URL for viewing. "
                                + "Accepts the complete HTML content of a risk report, "
                                + "saves it, and returns a URL that can be opened in browser.",
                        saveReportHtmlSchema(),
                        null,
                        null,
                        null),
                (ex, args) -> {
                    String html = (String) ((Map<String, Object>) args).get("html_content");
                    String id = ReportController.save(html);
                    String url = "http://localhost:5173/api/reports/" + id;
                    return new CallToolResult(
                            "{\"report_id\":\"" + id + "\",\"url\":\"" + url + "\"}", false);
                });

        log.info(
                "MCP SyncServer registered with 7 tools: search_negative_news, "
                        + "search_enterprises, get_enterprise_detail,"
                        + " generate_risk_report, search_alerts, parse_documents,"
                        + " save_report_html");
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

    private static JsonSchema searchAlertsSchema() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(
                "enterprise_name",
                Map.of("type", "string", "description", "Enterprise name to search alerts for"));
        properties.put(
                "alert_category",
                Map.of(
                        "type",
                        "string",
                        "description",
                        "Optional filter by alert category: 信用类, 合规类, 操作类, 声誉类"));
        properties.put(
                "alert_level",
                Map.of(
                        "type",
                        "string",
                        "description",
                        "Optional filter by alert level: 红色, 橙色, 蓝色"));
        return new JsonSchema("object", properties, List.of("enterprise_name"), null, null, null);
    }

    private static JsonSchema parseDocumentsSchema() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(
                "session_id",
                Map.of("type", "string", "description", "Upload session ID from file upload"));
        return new JsonSchema("object", properties, List.of("session_id"), null, null, null);
    }

    private static JsonSchema saveReportHtmlSchema() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(
                "html_content",
                Map.of(
                        "type",
                        "string",
                        "description",
                        "Complete HTML content of the report to save"));
        return new JsonSchema("object", properties, List.of("html_content"), null, null, null);
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
