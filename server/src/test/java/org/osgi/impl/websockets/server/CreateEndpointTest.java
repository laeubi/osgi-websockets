package org.osgi.impl.websockets.server;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for the new createEndpoint API with EndpointHandler.
 */
public class CreateEndpointTest {
    
    private JakartaWebSocketServer server;
    
    @BeforeEach
    public void setUp() {
        server = new JakartaWebSocketServer("localhost", 9999);
    }
    
    @Test
    public void testCreateEndpoint() {
        // Create a simple handler
        EndpointHandler handler = new EndpointHandler() {
            @Override
            public <T> T createEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed to create instance: " + e.getMessage());
                }
            }
            
            @Override
            public void sessionEnded(Object endpointInstance) {
                // No-op for this test
            }
        };
        
        // Should successfully create an endpoint
        WebSocketEndpoint endpoint = assertDoesNotThrow(() -> server.createEndpoint(TestEndpoint.class, null, handler));
        assertNotNull(endpoint);
        assertEquals(TestEndpoint.class, endpoint.getEndpointClass());
        assertEquals("/test", endpoint.getPath());
    }
    
    @Test
    public void testCreateEndpointWithCustomPath() {
        EndpointHandler handler = new EndpointHandler() {
            @Override
            public <T> T createEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed to create instance: " + e.getMessage());
                }
            }
            
            @Override
            public void sessionEnded(Object endpointInstance) {
                // No-op for this test
            }
        };
        
        // Should successfully create an endpoint with a custom path
        WebSocketEndpoint endpoint = assertDoesNotThrow(() -> server.createEndpoint(TestEndpoint.class, "/custom", handler));
        assertNotNull(endpoint);
        assertEquals("/custom", endpoint.getPath());
    }
    
    @Test
    public void testCreateEndpointNullEndpoint() {
        EndpointHandler handler = new EndpointHandler() {
            @Override
            public <T> T createEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                return null;
            }
            
            @Override
            public void sessionEnded(Object endpointInstance) {
            }
        };
        
        // Should throw exception for null endpoint
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> server.createEndpoint(null, null, handler));
        assertTrue(exception.getMessage().contains("cannot be null"));
    }
    
    @Test
    public void testCreateEndpointNullHandler() {
        // Should throw exception for null handler
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> server.createEndpoint(TestEndpoint.class, null, null));
        assertTrue(exception.getMessage().contains("cannot be null"));
    }
    
    @Test
    public void testCreateEndpointWithoutAnnotation() {
        EndpointHandler handler = new EndpointHandler() {
            @Override
            public <T> T createEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                return null;
            }
            
            @Override
            public void sessionEnded(Object endpointInstance) {
            }
        };
        
        // Should throw exception for endpoint without @ServerEndpoint annotation
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> server.createEndpoint(InvalidEndpoint.class, null, handler));
        assertTrue(exception.getMessage().contains("@ServerEndpoint"));
    }
    
    @Test
    public void testCreateDuplicateEndpoint() {
        EndpointHandler handler = new EndpointHandler() {
            @Override
            public <T> T createEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed to create instance: " + e.getMessage());
                }
            }
            
            @Override
            public void sessionEnded(Object endpointInstance) {
            }
        };
        
        // Create endpoint first time
        WebSocketEndpoint endpoint1 = assertDoesNotThrow(() -> server.createEndpoint(TestEndpoint.class, null, handler));
        assertNotNull(endpoint1);
        
        // Should throw exception when creating the same endpoint again
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> server.createEndpoint(TestEndpoint.class, null, handler));
        assertTrue(exception.getMessage().contains("already registered"));
    }
    
    @Test
    public void testDisposeEndpoint() {
        EndpointHandler handler = new EndpointHandler() {
            @Override
            public <T> T createEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed to create instance: " + e.getMessage());
                }
            }
            
            @Override
            public void sessionEnded(Object endpointInstance) {
            }
        };
        
        // Create endpoint
        WebSocketEndpoint endpoint = assertDoesNotThrow(() -> server.createEndpoint(TestEndpoint.class, null, handler));
        assertNotNull(endpoint);
        
        // Dispose it
        assertDoesNotThrow(() -> endpoint.dispose());
        
        // Verify it's removed
        assertNull(server.getEndpointRegistration("/test"));
    }
    
    @Test
    public void testDisposeEndpointTwice() {
        EndpointHandler handler = new EndpointHandler() {
            @Override
            public <T> T createEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed to create instance: " + e.getMessage());
                }
            }
            
            @Override
            public void sessionEnded(Object endpointInstance) {
            }
        };
        
        // Create endpoint
        WebSocketEndpoint endpoint = assertDoesNotThrow(() -> server.createEndpoint(TestEndpoint.class, null, handler));
        
        // Dispose it twice - should not throw
        assertDoesNotThrow(() -> endpoint.dispose());
        assertDoesNotThrow(() -> endpoint.dispose());
    }
    
    @Test
    public void testCreateDisposeCreate() {
        EndpointHandler handler = new EndpointHandler() {
            @Override
            public <T> T createEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed to create instance: " + e.getMessage());
                }
            }
            
            @Override
            public void sessionEnded(Object endpointInstance) {
            }
        };
        
        // Create endpoint
        WebSocketEndpoint endpoint1 = assertDoesNotThrow(() -> server.createEndpoint(TestEndpoint.class, null, handler));
        
        // Dispose it
        endpoint1.dispose();
        
        // Create it again - should work
        WebSocketEndpoint endpoint2 = assertDoesNotThrow(() -> server.createEndpoint(TestEndpoint.class, null, handler));
        assertNotNull(endpoint2);
        
        // Verify it's registered
        assertNotNull(server.getEndpointRegistration("/test"));
    }
    
    @Test
    public void testHandlerInstanceTracking() {
        // Track instances created and sessions ended
        AtomicInteger instancesCreated = new AtomicInteger(0);
        List<Object> endedInstances = new ArrayList<>();
        
        EndpointHandler handler = new EndpointHandler() {
            @Override
            public <T> T createEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                instancesCreated.incrementAndGet();
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed to create instance: " + e.getMessage());
                }
            }
            
            @Override
            public void sessionEnded(Object endpointInstance) {
                endedInstances.add(endpointInstance);
            }
        };
        
        // Create endpoint
        WebSocketEndpoint endpoint = assertDoesNotThrow(() -> server.createEndpoint(TestEndpoint.class, null, handler));
        assertNotNull(endpoint);
        
        // Verify handler is properly set
        JakartaWebSocketServer.EndpointRegistration registration = server.getEndpointRegistration("/test");
        assertNotNull(registration);
        assertSame(handler, registration.handler);
    }
}
