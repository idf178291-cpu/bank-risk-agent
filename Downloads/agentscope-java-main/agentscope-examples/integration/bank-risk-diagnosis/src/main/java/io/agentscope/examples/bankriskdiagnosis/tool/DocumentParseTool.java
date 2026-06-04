package io.agentscope.examples.bankriskdiagnosis.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.examples.bankriskdiagnosis.model.PipelineContext;
import io.agentscope.examples.bankriskdiagnosis.service.SessionStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentParseTool {

    private static final Logger log = LoggerFactory.getLogger(DocumentParseTool.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final SessionStore sessions;

    public DocumentParseTool(SessionStore sessions) {
        this.sessions = sessions;
    }

    @Tool(
            name = "parse_documents",
            description =
                    "Get all parsed documents from a session. Returns file names and their full"
                            + " text content for analysis.")
    public String parseDocuments(
            @ToolParam(
                            name = "session_id",
                            required = true,
                            description =
                                    "The session ID. Extract it from the user's upload confirmation"
                                            + " message which contains '会话ID: <uuid>'")
                    String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return err(
                    "session_id is required. Extract it from the user's latest upload"
                            + " confirmation message which contains '会话ID: <uuid>'");
        }
        try {
            PipelineContext ctx = sessions.get(sessionId);
            Map<String, String> contents = ctx.getParsedContents();
            if (contents.isEmpty()) {
                return err("No documents found in session " + sessionId);
            }

            List<Map<String, Object>> files = new ArrayList<>();
            StringBuilder allText = new StringBuilder();
            for (Map.Entry<String, String> e : contents.entrySet()) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("fileName", e.getKey());
                info.put("charCount", e.getValue().length());
                info.put("content", e.getValue());
                files.add(info);
                allText.append("=== ")
                        .append(e.getKey())
                        .append(" ===\n")
                        .append(e.getValue())
                        .append("\n\n");
            }

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("sessionId", sessionId);
            resp.put("bankName", ctx.getBankName());
            resp.put("totalFiles", files.size());
            resp.put("files", files);
            resp.put("allContent", allText.toString());
            // Pre-built summary: the agent MUST display this verbatim, then call ask_user ONCE
            StringBuilder summary = new StringBuilder();
            summary.append("| # | 文件名 | 字符数 |\n");
            summary.append("|---|--------|--------|\n");
            int idx = 1;
            for (Map.Entry<String, String> e : contents.entrySet()) {
                summary.append("| ")
                        .append(idx++)
                        .append(" | ")
                        .append(e.getKey())
                        .append(" | ")
                        .append(e.getValue().length())
                        .append(" |\n");
            }
            summary.append("\n**汇总**：共 ")
                    .append(files.size())
                    .append(" 份文档，涵盖银行监管检查、内部审计、合规通报等内容。");
            resp.put("summaryTable", summary.toString());
            // DO-NOT-ITERATE flag: tells the agent to process all files together
            resp.put(
                    "_instruction",
                    "DISPLAY the summaryTable above AS-IS. Do NOT iterate over files."
                            + " Call ask_user ONCE after displaying the table.");
            log.info(
                    "parse_documents: {} files, {} total chars, session {}",
                    files.size(),
                    allText.length(),
                    sessionId);
            return mapper.writeValueAsString(resp);
        } catch (Exception e) {
            log.error("parse_documents failed for session {}", sessionId, e);
            return err(e.getMessage());
        }
    }

    private String err(String msg) {
        return "{\"error\": \"" + msg.replace("\"", "\\\"") + "\"}";
    }
}
