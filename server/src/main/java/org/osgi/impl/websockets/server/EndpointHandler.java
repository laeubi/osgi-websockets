package org.osgi.impl.websockets.server;

/**
 * Interface for controlling endpoint instance creation and lifecycle.
 * This interface acts as the bridge between the server implementation and external
 * components that need to control endpoint behavior.
 */
public interface EndpointHandler {
    
    /**
     * Creates a new instance of the endpoint class.
     * This method is called when a new WebSocket connection is established.
     * 
     * @param <T> The endpoint class type
     * @param endpointClass The endpoint class to instantiate
     * @return A new instance of the endpoint
     * @throws InstantiationException if the endpoint cannot be instantiated
     */
    <T> T createEndpointInstance(Class<T> endpointClass) throws InstantiationException;
    
    /**
     * Called when an endpoint instance is no longer needed because the session has ended.
     * This allows the handler to perform cleanup or track the endpoint lifecycle.
     * 
     * @param endpointInstance The endpoint instance that is no longer in use
     */
    void sessionEnded(Object endpointInstance);
}
