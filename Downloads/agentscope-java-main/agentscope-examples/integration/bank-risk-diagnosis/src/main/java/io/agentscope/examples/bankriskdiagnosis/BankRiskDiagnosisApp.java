package io.agentscope.examples.bankriskdiagnosis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BankRiskDiagnosisApp {

    private static final Logger log = LoggerFactory.getLogger(BankRiskDiagnosisApp.class);

    public static void main(String[] args) {
        loadClaudeCodeSettings();
        SpringApplication.run(BankRiskDiagnosisApp.class, args);
    }

    @SuppressWarnings("unchecked")
    private static void loadClaudeCodeSettings() {
        try {
            Path settingsPath = Path.of(System.getProperty("user.home"), ".claude/settings.json");
            if (!Files.exists(settingsPath)) {
                log.warn(
                        "Claude Code settings not found at {}, falling back to env vars",
                        settingsPath);
                return;
            }

            byte[] bytes = Files.readAllBytes(settingsPath);
            Map<String, Object> settings = new ObjectMapper().readValue(bytes, Map.class);
            Map<String, Object> env = (Map<String, Object>) settings.get("env");
            if (env == null) {
                log.warn("No 'env' section in Claude Code settings");
                return;
            }

            setIfPresent(env, "ANTHROPIC_BASE_URL");
            setIfPresent(env, "ANTHROPIC_AUTH_TOKEN");
            setIfPresent(env, "ANTHROPIC_MODEL");

            Object modelObj = env.get("ANTHROPIC_DEFAULT_OPUS_MODEL");
            if (modelObj instanceof String s && !s.isBlank()) {
                System.setProperty("ANTHROPIC_DEFAULT_OPUS_MODEL", s.replace("[1m]", "").trim());
            }

            Object haikuObj = env.get("ANTHROPIC_DEFAULT_HAIKU_MODEL");
            if (haikuObj instanceof String s2 && !s2.isBlank()) {
                System.setProperty("ANTHROPIC_DEFAULT_HAIKU_MODEL", s2.replace("[1m]", "").trim());
            }

            log.info(
                    "Loaded model config from Claude Code settings: baseUrl={}, model={}",
                    env.get("ANTHROPIC_BASE_URL"),
                    env.get("ANTHROPIC_MODEL"));
        } catch (Exception e) {
            log.warn("Failed to read Claude Code settings: {}", e.getMessage());
        }
    }

    private static void setIfPresent(Map<String, Object> env, String key) {
        Object value = env.get(key);
        if (value instanceof String s && !s.isBlank()) {
            System.setProperty(key, s);
        }
    }
}
