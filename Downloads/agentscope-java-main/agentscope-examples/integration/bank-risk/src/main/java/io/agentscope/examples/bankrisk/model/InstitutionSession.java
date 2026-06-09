package io.agentscope.examples.bankrisk.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InstitutionSession {

    private final String sessionId;
    private String institutionName;
    private boolean institutionNameConfirmed;
    private final Map<String, String> parsedContents;
    private final List<String> sourceFiles;
    private final LocalDateTime createdAt;

    public InstitutionSession() {
        this.sessionId = UUID.randomUUID().toString();
        this.parsedContents = new LinkedHashMap<>();
        this.sourceFiles = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
        this.institutionNameConfirmed = true;
    }

    public boolean isInstitutionNameConfirmed() {
        return institutionNameConfirmed;
    }

    public Map<String, String> getParsedContents() {
        return parsedContents;
    }

    public List<String> getSourceFiles() {
        return sourceFiles;
    }

    public void addDocument(String fileName, String content) {
        parsedContents.put(fileName, content);
        sourceFiles.add(fileName);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getTotalFiles() {
        return sourceFiles.size();
    }

    public int getTotalCharCount() {
        return parsedContents.values().stream().mapToInt(String::length).sum();
    }
}
