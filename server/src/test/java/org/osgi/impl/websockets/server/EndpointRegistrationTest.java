package org.osgi.impl.websockets.server;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.websocket.server.ServerEndpointConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for endpoint registration and removal in JakartaWebSocketServer.
 */
public class EndpointRegistrationTest {
    
    private JakartaWebSocketServer server;
    
    @BeforeEach
    public void setUp() {
        server = new JakartaWebSocketServer("localhost", 9999);
    }
    
    @Test
    public void testAddEndpoint() {
        // Should successfully add an endpoint
        assertDoesNotThrow(() -> server.addEndpoint(TestEndpoint.class, null, null));
    }
    
    @Test
    public void testAddEndpointWithCustomPath() {
        // Should successfully add an endpoint with a custom path
        assertDoesNotThrow(() -> server.addEndpoint(TestEndpoint.class, "/custom", null));
    }
    
    @Test
    public void testAddEndpointWithCustomConfigurator() {
        CustomConfigurator configurator = new CustomConfigurator();
        assertDoesNotThrow(() -> server.addEndpoint(TestEndpoint.class, null, configurator));
        
        // Verify the configurator is stored
        JakartaWebSocketServer.EndpointRegistration registration = server.getEndpointRegistration("/test");
        assertNotNull(registration);
        assertEquals(TestEndpoint.class, registration.endpointClass);
        assertEquals("/test", registration.effectivePath);
        assertSame(configurator, registration.configurator);
    }
    
    @Test
    public void testAddEndpointNullEndpoint() {
        // Should throw exception for null endpoint
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> server.addEndpoint(null, null, null));
        assertTrue(exception.getMessage().contains("cannot be null"));
    }
    
    @Test
    public void testAddEndpointWithoutAnnotation() {
        // Should throw exception for endpoint without @ServerEndpoint annotation
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> server.addEndpoint(InvalidEndpoint.class, null, null));
        assertTrue(exception.getMessage().contains("@ServerEndpoint"));
    }
    
    @Test
    public void testAddDuplicateEndpoint() {
        // Add endpoint first time
        assertDoesNotThrow(() -> server.addEndpoint(TestEndpoint.class, null, null));
        
        // Should throw exception when adding the same endpoint again
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> server.addEndpoint(TestEndpoint.class, null, null));
        assertTrue(exception.getMessage().contains("already registered"));
    }
    
    @Test
    public void testPathNormalization() {
        // Test that paths are normalized to start with /
        assertDoesNotThrow(() -> server.addEndpoint(TestEndpoint.class, "custom", null));
        
        JakartaWebSocketServer.EndpointRegistration registration = server.getEndpointRegistration("/custom");
        assertNotNull(registration);
        assertEquals("/custom", registration.effectivePath);
    }
    
    @Test
    public void testRemoveEndpoint() {
        // Add endpoint first
        assertDoesNotThrow(() -> server.addEndpoint(TestEndpoint.class, null, null));
        
        // Remove it
        assertTrue(server.removeEndpoint(TestEndpoint.class, null));
        
        // Verify it's removed
        assertNull(server.getEndpointRegistration("/test"));
    }
    
    @Test
    public void testRemoveEndpointWithCustomPath() {
        // Add endpoint with custom path
        assertDoesNotThrow(() -> server.addEndpoint(TestEndpoint.class, "/custom", null));
        
        // Remove it with the custom path
        assertTrue(server.removeEndpoint(TestEndpoint.class, "/custom"));
        
        // Verify it's removed
        assertNull(server.getEndpointRegistration("/custom"));
    }
    
    @Test
    public void testRemoveNonExistentEndpoint() {
        // Should return false when removing non-existent endpoint
        assertFalse(server.removeEndpoint(TestEndpoint.class, null));
    }
    
    @Test
    public void testRemoveEndpointNullEndpoint() {
        // Should throw exception for null endpoint
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> server.removeEndpoint(null, null));
        assertTrue(exception.getMessage().contains("cannot be null"));
    }
    
    @Test
    public void testRemoveEndpointWithoutAnnotation() {
        // Should throw exception for endpoint without @ServerEndpoint annotation
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> server.removeEndpoint(InvalidEndpoint.class, null));
        assertTrue(exception.getMessage().contains("@ServerEndpoint"));
    }
    
    @Test
    public void testAddRemoveAddEndpoint() {
        // Add endpoint
        assertDoesNotThrow(() -> server.addEndpoint(TestEndpoint.class, null, null));
        
        // Remove it
        assertTrue(server.removeEndpoint(TestEndpoint.class, null));
        
        // Add it again - should work
        assertDoesNotThrow(() -> server.addEndpoint(TestEndpoint.class, null, null));
        
        // Verify it's registered
        assertNotNull(server.getEndpointRegistration("/test"));
    }
    
    @Test
    public void testEndpointWithAnnotationPath() {
        // TestEndpoint has @ServerEndpoint("/test")
        assertDoesNotThrow(() -> server.addEndpoint(TestEndpoint.class, null, null));
        
        JakartaWebSocketServer.EndpointRegistration registration = server.getEndpointRegistration("/test");
        assertNotNull(registration);
        assertEquals("/test", registration.effectivePath);
    }
    
    @Test
    public void testEndpointPathOverride() {
        // Add endpoint with path override
        assertDoesNotThrow(() -> server.addEndpoint(TestEndpoint.class, "/override", null));
        
        // Verify the override path is used
        JakartaWebSocketServer.EndpointRegistration registration = server.getEndpointRegistration("/override");
        assertNotNull(registration);
        assertEquals("/override", registration.effectivePath);
        
        // Verify original path is not registered
        assertNull(server.getEndpointRegistration("/test"));
    }
}
