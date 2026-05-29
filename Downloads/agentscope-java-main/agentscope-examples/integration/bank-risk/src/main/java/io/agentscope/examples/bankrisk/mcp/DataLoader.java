package io.agentscope.examples.bankrisk.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final ObjectMapper mapper;
    private final List<Map<String, Object>> enterprises;
    private final Map<String, Object> negativeNews;
    private final Map<String, Object> creditReports;

    public DataLoader() {
        this.mapper = new ObjectMapper();
        this.enterprises = loadEnterprises();
        this.negativeNews = loadNegativeNews();
        this.creditReports = loadCreditReports();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadEnterprises() {
        try (InputStream in =
                getClass().getClassLoader().getResourceAsStream("data/enterprises.json")) {
            if (in == null) {
                log.error("enterprises.json not found on classpath");
                return Collections.emptyList();
            }
            List<Map<String, Object>> data =
                    mapper.readValue(in, new TypeReference<List<Map<String, Object>>>() {});
            log.info("Loaded {} enterprises from JSON", data.size());
            return data;
        } catch (Exception e) {
            log.error("Failed to load enterprises.json: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadNegativeNews() {
        try (InputStream in =
                getClass().getClassLoader().getResourceAsStream("data/negative_news.json")) {
            if (in == null) {
                log.error("negative_news.json not found on classpath");
                return Collections.emptyMap();
            }
            Map<String, Object> data =
                    mapper.readValue(in, new TypeReference<Map<String, Object>>() {});
            log.info("Loaded negative news for {} enterprises", data.size());
            return data;
        } catch (Exception e) {
            log.error("Failed to load negative_news.json: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadCreditReports() {
        try (InputStream in =
                getClass().getClassLoader().getResourceAsStream("data/credit_reports.json")) {
            if (in == null) {
                log.error("credit_reports.json not found on classpath");
                return Collections.emptyMap();
            }
            Map<String, Object> data =
                    mapper.readValue(in, new TypeReference<Map<String, Object>>() {});
            log.info("Loaded credit reports for {} enterprises", data.size());
            return data;
        } catch (Exception e) {
            log.error("Failed to load credit_reports.json: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    public List<Map<String, Object>> getEnterprises() {
        return Collections.unmodifiableList(enterprises);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getEnterprise(String name) {
        for (Map<String, Object> e : enterprises) {
            if (name.equals(e.get("name"))) {
                return e;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getNegativeNews(String enterpriseName) {
        return (Map<String, Object>)
                negativeNews.getOrDefault(enterpriseName, Collections.emptyMap());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getCreditReport(String enterpriseName) {
        return (Map<String, Object>)
                creditReports.getOrDefault(enterpriseName, Collections.emptyMap());
    }
}
