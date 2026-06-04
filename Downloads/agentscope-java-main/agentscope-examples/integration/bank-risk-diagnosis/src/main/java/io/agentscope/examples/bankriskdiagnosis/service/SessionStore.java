package io.agentscope.examples.bankriskdiagnosis.service;

import io.agentscope.examples.bankriskdiagnosis.model.PipelineContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SessionStore {

    private static final Logger log = LoggerFactory.getLogger(SessionStore.class);
    private final Map<String, PipelineContext> sessions = new ConcurrentHashMap<>();

    public String create(String bankName) {
        PipelineContext ctx = PipelineContext.create(bankName);
        sessions.put(ctx.getSessionId(), ctx);
        log.info("Session {} created for '{}'", ctx.getSessionId(), bankName);
        return ctx.getSessionId();
    }

    public PipelineContext get(String sessionId) {
        PipelineContext ctx = sessions.get(sessionId);
        if (ctx == null) throw new IllegalArgumentException("Session not found: " + sessionId);
        return ctx;
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }
}
