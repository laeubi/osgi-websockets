package org.osgi.impl.websockets.server;

import jakarta.websocket.server.ServerEndpointConfig;

/**
 * A custom configurator for testing.
 */
public class CustomConfigurator extends ServerEndpointConfig.Configurator {
    
    private int instanceCount = 0;
    
    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        instanceCount++;
        try {
            return endpointClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new InstantiationException("Failed to create instance: " + e.getMessage());
        }
    }
    
    public int getInstanceCount() {
        return instanceCount;
    }
}
