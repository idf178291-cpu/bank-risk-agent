package io.agentscope.examples.bankriskdiagnosis.model;

public enum SourceFileType {
    OFD("OFD电子文档"),
    WORD("Word文档"),
    EXCEL("Excel表格");

    private final String displayName;

    SourceFileType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static SourceFileType fromExtension(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".ofd")) return OFD;
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return WORD;
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) return EXCEL;
        throw new IllegalArgumentException("Unsupported file type: " + filename);
    }
}
