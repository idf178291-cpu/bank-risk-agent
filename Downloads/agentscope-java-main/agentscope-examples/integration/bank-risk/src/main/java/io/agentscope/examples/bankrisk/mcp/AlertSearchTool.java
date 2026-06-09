package io.agentscope.examples.bankrisk.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlertSearchTool {

    private static final Logger log = LoggerFactory.getLogger(AlertSearchTool.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final DataLoader dataLoader;

    public AlertSearchTool(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    @SuppressWarnings("unchecked")
    public String searchAlerts(Map<String, Object> args) {
        String enterpriseName = (String) args.get("enterprise_name");
        if (enterpriseName == null || enterpriseName.isBlank()) {
            return error("enterprise_name is required");
        }

        String alertCategory = (String) args.get("alert_category");
        String alertLevel = (String) args.get("alert_level");

        log.info(
                "Searching alerts for: {} (category={}, level={})",
                enterpriseName,
                alertCategory != null ? alertCategory : "all",
                alertLevel != null ? alertLevel : "all");

        Object raw = dataLoader.getAlerts(enterpriseName);
        if (raw == null) {
            return message("No alerts found for enterprise: " + enterpriseName);
        }
        if (!(raw instanceof List)) {
            return message("Unexpected data format for: " + enterpriseName);
        }

        List<Map<String, Object>> allAlerts = (List<Map<String, Object>>) raw;
        if (allAlerts.isEmpty()) {
            return message("No alerts found for: " + enterpriseName);
        }

        List<Map<String, Object>> filtered = allAlerts;
        if (alertCategory != null && !alertCategory.isBlank()) {
            filtered =
                    filtered.stream()
                            .filter(
                                    a -> {
                                        String c = (String) a.get("alertCategory");
                                        return c != null && c.contains(alertCategory.trim());
                                    })
                            .collect(Collectors.toList());
        }
        if (alertLevel != null && !alertLevel.isBlank()) {
            filtered =
                    filtered.stream()
                            .filter(
                                    a -> {
                                        String l = (String) a.get("alertLevel");
                                        return l != null && l.equalsIgnoreCase(alertLevel.trim());
                                    })
                            .collect(Collectors.toList());
        }

        if (filtered.isEmpty()) {
            return message(
                    "No matching alerts for: "
                            + enterpriseName
                            + " (category="
                            + alertCategory
                            + ", level="
                            + alertLevel
                            + ")");
        }

        try {
            return mapper.writeValueAsString(filtered);
        } catch (Exception e) {
            log.error("Error searching alerts: {}", e.getMessage(), e);
            return error("Internal error searching alerts");
        }
    }

    private static String error(String msg) {
        return "[{\"error\": \"" + msg + "\"}]";
    }

    private static String message(String msg) {
        return "[{\"message\": \"" + msg + "\"}]";
    }
}
