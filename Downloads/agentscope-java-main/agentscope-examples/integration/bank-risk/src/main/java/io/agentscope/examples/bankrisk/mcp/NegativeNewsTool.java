package io.agentscope.examples.bankrisk.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles search_negative_news MCP tool logic.
 * Data sourced from JSON via DataLoader — no hardcoded enterprise data.
 */
public class NegativeNewsTool {

    private static final Logger log = LoggerFactory.getLogger(NegativeNewsTool.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final DataLoader dataLoader;

    public NegativeNewsTool(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    @SuppressWarnings("unchecked")
    public String searchNegativeNews(Map<String, Object> args) {
        String enterpriseName = (String) args.get("enterprise_name");
        if (enterpriseName == null || enterpriseName.isBlank()) {
            return error("enterprise_name is required");
        }

        Object daysObj = args.get("days");
        int days = (daysObj instanceof Number n) ? n.intValue() : 90;

        log.info("Searching negative news for: {} (last {} days)", enterpriseName, days);

        Object raw = dataLoader.getNegativeNews(enterpriseName);
        if (raw == null) {
            return message("No data found for enterprise: " + enterpriseName);
        }
        if (!(raw instanceof List)) {
            return message("Unexpected data format for: " + enterpriseName);
        }

        List<Map<String, Object>> allNews = (List<Map<String, Object>>) raw;
        if (allNews.isEmpty()) {
            return message("No negative news found for: " + enterpriseName);
        }

        try {
            List<Map<String, Object>> filtered;
            if (days < 365) {
                String cutoffDate = java.time.LocalDate.now().minusDays(days).toString();
                filtered =
                        allNews.stream()
                                .filter(
                                        n -> {
                                            String date = (String) n.get("date");
                                            return date != null && date.compareTo(cutoffDate) >= 0;
                                        })
                                .collect(Collectors.toList());
            } else {
                filtered = allNews;
            }

            if (filtered.isEmpty()) {
                return message("No negative news in last " + days + " days for: " + enterpriseName);
            }

            return mapper.writeValueAsString(filtered);
        } catch (Exception e) {
            log.error("Error searching negative news: {}", e.getMessage(), e);
            return error("Internal error searching negative news");
        }
    }

    private static String error(String msg) {
        return "[{\"error\": \"" + msg + "\"}]";
    }

    private static String message(String msg) {
        return "[{\"message\": \"" + msg + "\"}]";
    }
}
