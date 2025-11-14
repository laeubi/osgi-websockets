package org.osgi.impl.websockets.server;

/**
 * A test class without @ServerEndpoint annotation for testing validation.
 */
public class InvalidEndpoint {
    
    public String handleMessage(String message) {
        return "Invalid: " + message;
    }
}
