package org.osgi.impl.websockets.server;

import jakarta.websocket.OnMessage;
import jakarta.websocket.server.ServerEndpoint;

/**
 * A test endpoint for testing the JakartaWebSocketServer endpoint registration.
 */
@ServerEndpoint("/test")
public class TestEndpoint {
    
    @OnMessage
    public String handleMessage(String message) {
        return "Test: " + message;
    }
}
