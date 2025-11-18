package org.osgi.impl.websockets.server;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;

/**
 * A test endpoint that uses Session parameter to verify Session support.
 */
@ServerEndpoint("/session-test")
public class SessionEndpoint {
    
    private Session currentSession;
    
    @OnOpen
    public void onOpen(Session session) {
        this.currentSession = session;
        System.out.println("Session opened with ID: " + session.getId());
    }
    
    @OnMessage
    public String handleMessage(String message, Session session) throws IOException {
        // Verify we can use the session to send messages
        if (message.equals("test-session-id")) {
            return "Session ID: " + session.getId();
        } else if (message.equals("test-session-uri")) {
            return "Request URI: " + session.getRequestURI().getPath();
        } else if (message.equals("test-session-open")) {
            return "Session open: " + session.isOpen();
        } else if (message.equals("test-basic-remote")) {
            // Test using basic remote to send
            session.getBasicRemote().sendText("Response via BasicRemote");
            return null; // Already sent via BasicRemote
        } else if (message.equals("test-query-string")) {
            String queryString = session.getQueryString();
            return "Query string: " + (queryString != null ? queryString : "null");
        } else {
            return "Echo: " + message;
        }
    }
    
    @OnClose
    public void onClose(Session session) {
        System.out.println("Session closed with ID: " + session.getId());
        this.currentSession = null;
    }
}
