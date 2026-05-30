package io.agentscope.examples.bankrisk.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnterpriseDetailTool {

    private static final Logger log = LoggerFactory.getLogger(EnterpriseDetailTool.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final DataLoader dataLoader;

    public EnterpriseDetailTool(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    public String getEnterpriseDetail(Map<String, Object> args) {
        String customerId = (String) args.get("customer_id");
        if (customerId == null || customerId.isBlank()) {
            return "[{\"error\": \"customer_id is required\"}]";
        }

        log.info("Getting enterprise detail for customerId: {}", customerId);

        Map<String, Object> enterprise = dataLoader.getEnterpriseByCustomerId(customerId);
        if (enterprise == null) {
            return "[{\"message\": \"No enterprise found for customerId: " + customerId + "\"}]";
        }

        try {
            return mapper.writeValueAsString(enterprise);
        } catch (Exception e) {
            log.error("Error serializing enterprise detail: {}", e.getMessage(), e);
            return "[{\"error\": \"Internal error retrieving enterprise detail\"}]";
        }
    }
}
