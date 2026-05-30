package io.agentscope.examples.bankrisk.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnterpriseSearchTool {

    private static final Logger log = LoggerFactory.getLogger(EnterpriseSearchTool.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final DataLoader dataLoader;

    public EnterpriseSearchTool(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    public String searchEnterprises(Map<String, Object> args) {
        String keyword = (String) args.get("keyword");
        if (keyword == null) {
            keyword = "";
        }
        keyword = keyword.trim();

        log.info("Searching enterprises with keyword: '{}'", keyword);

        try {
            List<Map<String, Object>> results = dataLoader.searchEnterprises(keyword);
            List<Map<String, Object>> slim = new ArrayList<>();
            for (Map<String, Object> e : results) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("customerId", e.get("customerId"));
                item.put("name", e.get("name"));
                slim.add(item);
            }
            return mapper.writeValueAsString(slim);
        } catch (Exception e) {
            log.error("Error searching enterprises: {}", e.getMessage(), e);
            return "[{\"error\": \"Internal error searching enterprises\"}]";
        }
    }
}
