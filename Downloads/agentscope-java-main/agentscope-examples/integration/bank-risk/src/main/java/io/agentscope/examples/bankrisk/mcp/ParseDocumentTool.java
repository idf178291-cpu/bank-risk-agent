package io.agentscope.examples.bankrisk.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.examples.bankrisk.model.InstitutionSession;
import io.agentscope.examples.bankrisk.service.SessionStore;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParseDocumentTool {

    private static final Logger log = LoggerFactory.getLogger(ParseDocumentTool.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final SessionStore sessions;

    public ParseDocumentTool(SessionStore sessions) {
        this.sessions = sessions;
    }

    public String parseDocuments(Map<String, Object> args) {
        String sessionId = (String) args.get("session_id");
        if (sessionId == null || sessionId.isBlank()) {
            return error("session_id is required");
        }

        InstitutionSession session = sessions.get(sessionId);
        if (session == null) {
            return error("Session not found: " + sessionId);
        }

        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("sessionId", session.getSessionId());
            result.put(
                    "institutionName",
                    session.getInstitutionName() != null ? session.getInstitutionName() : "");
            result.put("institutionNameConfirmed", session.isInstitutionNameConfirmed());
            result.put("totalFiles", session.getTotalFiles());
            result.put("totalCharCount", session.getTotalCharCount());
            result.put("sourceFiles", session.getSourceFiles());

            // Build concatenated content with file markers
            StringBuilder allContent = new StringBuilder();
            for (String fileName : session.getSourceFiles()) {
                allContent.append("\n=== ").append(fileName).append(" ===\n\n");
                allContent.append(session.getParsedContents().get(fileName));
                allContent.append("\n");
            }
            result.put("allContent", allContent.toString());

            return mapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("Error parsing documents: {}", e.getMessage(), e);
            return error("Internal error parsing documents");
        }
    }

    private static String error(String msg) {
        return "{\"error\": \"" + msg + "\"}";
    }
}
