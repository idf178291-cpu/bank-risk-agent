package io.agentscope.examples.bankriskdiagnosis.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Session state holder — parsed document contents and progress tracking. */
public class PipelineContext {

    private final String sessionId;
    private String bankName;
    private final List<String> uploadedFiles;
    private final Map<String, String> parsedContents;
    private int currentStep;
    private final int totalSteps;
    private PipelineStatus status;
    private String errorMessage;
    private final LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public PipelineContext() {
        this.sessionId = UUID.randomUUID().toString();
        this.uploadedFiles = new ArrayList<>();
        this.parsedContents = new ConcurrentHashMap<>();
        this.totalSteps = 8;
        this.currentStep = 0;
        this.status = PipelineStatus.PENDING;
        this.startedAt = LocalDateTime.now();
    }

    public static PipelineContext create(String bankName) {
        PipelineContext ctx = new PipelineContext();
        ctx.bankName = bankName;
        return ctx;
    }

    public enum PipelineStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public List<String> getUploadedFiles() {
        return uploadedFiles;
    }

    public Map<String, String> getParsedContents() {
        return parsedContents;
    }

    public void addParsedContent(String filename, String content) {
        this.parsedContents.put(filename, content);
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int step) {
        this.currentStep = step;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public PipelineStatus getStatus() {
        return status;
    }

    public void setStatus(PipelineStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String err) {
        this.errorMessage = err;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public double getProgressPercentage() {
        return totalSteps > 0 ? (double) currentStep / totalSteps * 100.0 : 0.0;
    }
}
