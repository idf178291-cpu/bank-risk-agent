package io.agentscope.examples.bankrisk.config;

import io.agentscope.examples.bankrisk.mcp.CreditReportTool;
import io.agentscope.examples.bankrisk.mcp.DataLoader;
import io.agentscope.examples.bankrisk.mcp.NegativeNewsTool;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
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

    public McpServerConfig(DataLoader dataLoader) {
        this.negativeNewsTool = new NegativeNewsTool(dataLoader);
        this.creditReportTool = new CreditReportTool(dataLoader);
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
                (ex, args) -> negativeNewsTool.searchNegativeNews((Map<String, Object>) args));

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
                (ex, args) -> creditReportTool.queryCreditReport((Map<String, Object>) args));

        log.info(
                "MCP SyncServer registered with 2 tools: search_negative_news, "
                        + "query_credit_report");
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
}
