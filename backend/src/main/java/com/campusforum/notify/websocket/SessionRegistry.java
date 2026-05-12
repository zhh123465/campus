package com.campusforum.notify.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class SessionRegistry {

    private final ConcurrentHashMap<Long, List<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void add(Long userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(session);
    }

    public void remove(Long userId, WebSocketSession session) {
        List<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            userSessions.remove(session);
            if (userSessions.isEmpty()) {
                sessions.remove(userId);
            }
        }
    }

    public void sendToUser(Long userId, String message) {
        List<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) return;
        TextMessage tm = new TextMessage(message);
        for (WebSocketSession session : userSessions) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(tm);
                    }
                }
            } catch (IOException e) {
                log.debug("Failed to send WebSocket message to userId={}: {}", userId, e.getMessage());
            }
        }
    }
}
