package io.agentscope.examples.bankrisk.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles query_credit_report MCP tool logic.
 * Data sourced from JSON via DataLoader — no hardcoded enterprise data.
 */
public class CreditReportTool {

    private static final Logger log = LoggerFactory.getLogger(CreditReportTool.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final DataLoader dataLoader;

    public CreditReportTool(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    public String queryCreditReport(Map<String, Object> args) {
        String enterpriseName = (String) args.get("enterprise_name");
        if (enterpriseName == null || enterpriseName.isBlank()) {
            return "[{\"error\": \"enterprise_name is required\"}]";
        }

        String reportType = (String) args.getOrDefault("report_type", "full");
        log.info("Querying credit report for: {} (type: {})", enterpriseName, reportType);

        Map<String, Object> report = dataLoader.getCreditReport(enterpriseName);
        if (report.isEmpty()) {
            return "[{\"message\": \"No credit report found for: " + enterpriseName + "\"}]";
        }

        try {
            if ("summary".equalsIgnoreCase(reportType)) {
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("enterpriseName", enterpriseName);
                summary.put("creditScore", report.get("creditScore"));
                summary.put("creditRating", report.get("creditRating"));
                summary.put("reportDate", report.get("reportDate"));
                summary.put("summary", report.get("summary"));
                return mapper.writeValueAsString(summary);
            }
            return mapper.writeValueAsString(report);
        } catch (Exception e) {
            log.error("Error querying credit report: {}", e.getMessage(), e);
            return "[{\"error\": \"Internal error querying credit report\"}]";
        }
    }
}
