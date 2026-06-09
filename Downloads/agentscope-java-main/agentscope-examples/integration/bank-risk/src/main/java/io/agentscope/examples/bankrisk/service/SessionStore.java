package io.agentscope.examples.bankrisk.service;

import io.agentscope.examples.bankrisk.model.InstitutionSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class SessionStore {

    private final Map<String, InstitutionSession> sessions = new ConcurrentHashMap<>();

    public InstitutionSession create() {
        InstitutionSession session = new InstitutionSession();
        sessions.put(session.getSessionId(), session);
        return session;
    }

    public InstitutionSession get(String sessionId) {
        return sessions.get(sessionId);
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }

    public boolean exists(String sessionId) {
        return sessions.containsKey(sessionId);
    }
}
