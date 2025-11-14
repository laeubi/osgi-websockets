package org.osgi.impl.websockets.server;

/**
 * Represents a registered WebSocket endpoint.
 * This interface encapsulates the registration and provides a method to dispose of the endpoint,
 * which will unregister it and close all active sessions.
 */
public interface WebSocketEndpoint {
    
    /**
     * Disposes of this endpoint registration.
     * This will:
     * <ul>
     *   <li>Unregister the endpoint from the server</li>
     *   <li>Close all active WebSocket sessions for this endpoint</li>
     *   <li>Clean up any associated resources</li>
     * </ul>
     * 
     * After calling this method, the endpoint can no longer accept new connections.
     */
    void dispose();
    
    /**
     * Gets the endpoint class that was registered.
     * 
     * @return The endpoint class
     */
    Class<?> getEndpointClass();
    
    /**
     * Gets the path where this endpoint is registered.
     * 
     * @return The endpoint path
     */
    String getPath();
}
