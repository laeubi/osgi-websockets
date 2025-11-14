package org.osgi.impl.websockets.server;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration test for the new createEndpoint API with full lifecycle.
 */
public class EndpointHandlerLifecycleTest {
    
    private JakartaWebSocketServer server;
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 8891;
    
    @BeforeEach
    public void setUp() throws Exception {
        server = new JakartaWebSocketServer(HOSTNAME, PORT);
        server.start();
    }
    
    @AfterEach
    public void tearDown() {
        if (server != null) {
            server.stop();
        }
    }
    
    @Test
    public void testEndpointHandlerLifecycle() throws Exception {
        // Track lifecycle events
        AtomicInteger instancesCreated = new AtomicInteger(0);
        List<TestEndpoint> sessionEndedInstances = new ArrayList<>();
        CountDownLatch sessionEndedLatch = new CountDownLatch(1);
        
        EndpointHandler<TestEndpoint> handler = new EndpointHandler<TestEndpoint>() {
            @Override
            public TestEndpoint createEndpointInstance(Class<TestEndpoint> endpointClass) throws InstantiationException {
                instancesCreated.incrementAndGet();
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed to create instance: " + e.getMessage());
                }
            }
            
            @Override
            public void sessionEnded(TestEndpoint endpointInstance) {
                sessionEndedInstances.add(endpointInstance);
                sessionEndedLatch.countDown();
            }
        };
        
        // Register the endpoint
        WebSocketEndpoint<TestEndpoint> endpoint = server.createEndpoint(TestEndpoint.class, null, handler);
        assertNotNull(endpoint);
        assertEquals("/test", endpoint.getPath());
        
        // Verify no instances created yet
        assertEquals(0, instancesCreated.get());
        assertEquals(0, sessionEndedInstances.size());
        
        // Connect a client
        CountDownLatch messageLatch = new CountDownLatch(1);
        HttpClient client = HttpClient.newHttpClient();
        
        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                messageLatch.countDown();
                return CompletableFuture.completedFuture(null);
            }
        };
        
        URI serverUri = URI.create("ws://" + HOSTNAME + ":" + PORT + "/test");
        WebSocket webSocket = client.newWebSocketBuilder()
            .buildAsync(serverUri, listener)
            .join();
        
        // Send a message
        webSocket.sendText("test", true).join();
        
        // Wait for response
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Should receive response");
        
        // Verify an instance was created
        assertEquals(1, instancesCreated.get(), "One instance should be created");
        
        // Close the connection
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        
        // Wait for sessionEnded callback
        assertTrue(sessionEndedLatch.await(5, TimeUnit.SECONDS), "sessionEnded should be called");
        assertEquals(1, sessionEndedInstances.size(), "sessionEnded should be called once");
        
        // Dispose the endpoint
        endpoint.dispose();
    }
    
    @Test
    public void testMultipleConnections() throws Exception {
        // Track instances
        AtomicInteger instancesCreated = new AtomicInteger(0);
        AtomicInteger sessionsEnded = new AtomicInteger(0);
        
        EndpointHandler<TestEndpoint> handler = new EndpointHandler<TestEndpoint>() {
            @Override
            public TestEndpoint createEndpointInstance(Class<TestEndpoint> endpointClass) throws InstantiationException {
                instancesCreated.incrementAndGet();
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed to create instance: " + e.getMessage());
                }
            }
            
            @Override
            public void sessionEnded(TestEndpoint endpointInstance) {
                sessionsEnded.incrementAndGet();
            }
        };
        
        // Register the endpoint
        WebSocketEndpoint<TestEndpoint> endpoint = server.createEndpoint(TestEndpoint.class, null, handler);
        
        // Connect multiple clients
        HttpClient client = HttpClient.newHttpClient();
        URI serverUri = URI.create("ws://" + HOSTNAME + ":" + PORT + "/test");
        
        WebSocket ws1 = client.newWebSocketBuilder()
            .buildAsync(serverUri, new WebSocket.Listener() {})
            .join();
        
        WebSocket ws2 = client.newWebSocketBuilder()
            .buildAsync(serverUri, new WebSocket.Listener() {})
            .join();
        
        WebSocket ws3 = client.newWebSocketBuilder()
            .buildAsync(serverUri, new WebSocket.Listener() {})
            .join();
        
        // Give connections time to establish
        Thread.sleep(100);
        
        // Verify instances were created
        assertEquals(3, instancesCreated.get(), "Three instances should be created");
        
        // Close all connections
        ws1.sendClose(WebSocket.NORMAL_CLOSURE, "Done").join();
        ws2.sendClose(WebSocket.NORMAL_CLOSURE, "Done").join();
        ws3.sendClose(WebSocket.NORMAL_CLOSURE, "Done").join();
        
        // Give time for sessionEnded callbacks
        Thread.sleep(500);
        
        // Verify sessionEnded was called for all
        assertEquals(3, sessionsEnded.get(), "sessionEnded should be called three times");
        
        // Dispose the endpoint
        endpoint.dispose();
    }
    
    @Test
    public void testDisposeClosesActiveSessions() throws Exception {
        // Track sessions
        AtomicInteger sessionsEnded = new AtomicInteger(0);
        
        EndpointHandler<TestEndpoint> handler = new EndpointHandler<TestEndpoint>() {
            @Override
            public TestEndpoint createEndpointInstance(Class<TestEndpoint> endpointClass) throws InstantiationException {
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed to create instance: " + e.getMessage());
                }
            }
            
            @Override
            public void sessionEnded(TestEndpoint endpointInstance) {
                sessionsEnded.incrementAndGet();
            }
        };
        
        // Register the endpoint
        WebSocketEndpoint<TestEndpoint> endpoint = server.createEndpoint(TestEndpoint.class, null, handler);
        
        // Connect a client
        CountDownLatch closeLatch = new CountDownLatch(1);
        HttpClient client = HttpClient.newHttpClient();
        
        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                closeLatch.countDown();
                return CompletableFuture.completedFuture(null);
            }
            
            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                closeLatch.countDown();
            }
        };
        
        URI serverUri = URI.create("ws://" + HOSTNAME + ":" + PORT + "/test");
        WebSocket webSocket = client.newWebSocketBuilder()
            .buildAsync(serverUri, listener)
            .join();
        
        // Give connection time to establish
        Thread.sleep(100);
        
        // Dispose the endpoint - this should close the active connection
        endpoint.dispose();
        
        // Wait for the connection to close
        assertTrue(closeLatch.await(5, TimeUnit.SECONDS), "Connection should be closed when endpoint is disposed");
        
        // Give time for sessionEnded callback
        Thread.sleep(500);
        
        // Verify sessionEnded was called
        assertEquals(1, sessionsEnded.get(), "sessionEnded should be called when connection is closed");
    }
}
