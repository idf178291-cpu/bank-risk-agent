package io.agentscope.examples.bankrisk.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);
    private static final Path REPORT_DIR =
            Paths.get(System.getProperty("java.io.tmpdir"), "bank-risk-reports");

    static {
        try {
            Files.createDirectories(REPORT_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create report dir", e);
        }
    }

    public static String save(String html) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        try {
            Path file = REPORT_DIR.resolve(id + ".html");
            Files.writeString(file, html, StandardCharsets.UTF_8);
            log.info("Report saved: {} ({} bytes)", file, html.length());
        } catch (IOException e) {
            log.error("Failed to save report {}: {}", id, e.getMessage());
        }
        return id;
    }

    @GetMapping(value = "/api/reports/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public String getReport(@PathVariable String id) {
        try {
            Path file = REPORT_DIR.resolve(id + ".html");
            if (Files.exists(file)) {
                return Files.readString(file, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("Failed to read report {}: {}", id, e.getMessage());
        }
        return "<!DOCTYPE html><html><body><h1>Report not found</h1>"
                + "<p>ID: "
                + id
                + "</p>"
                + "<p>The report may have expired. Please regenerate the report.</p>"
                + "</body></html>";
    }
}
