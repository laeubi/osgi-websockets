package org.osgi.impl.websockets.server;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for session cleanup when endpoints are removed.
 */
public class EndpointRemovalTest {
    
    private JakartaWebSocketServer server;
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 8890;
    
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
    public void testRemoveEndpointClosesActiveSessions() throws Exception {
        // Register an endpoint
        server.addEndpoint(TestEndpoint.class, null, null);
        
        CountDownLatch closeLatch = new CountDownLatch(1);
        AtomicBoolean connectionClosed = new AtomicBoolean(false);
        
        HttpClient client = HttpClient.newHttpClient();
        
        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                connectionClosed.set(true);
                closeLatch.countDown();
                return CompletableFuture.completedFuture(null);
            }
            
            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                // Connection might also close with an error
                connectionClosed.set(true);
                closeLatch.countDown();
            }
        };
        
        URI serverUri = URI.create("ws://" + HOSTNAME + ":" + PORT + "/test");
        WebSocket webSocket = client.newWebSocketBuilder()
            .buildAsync(serverUri, listener)
            .join();
        
        // Give the connection time to establish
        Thread.sleep(100);
        
        // Remove the endpoint - this should close the active connection
        assertTrue(server.removeEndpoint(TestEndpoint.class, null));
        
        // Wait for the connection to close
        boolean closed = closeLatch.await(5, TimeUnit.SECONDS);
        assertTrue(closed, "Connection should be closed when endpoint is removed");
        assertTrue(connectionClosed.get(), "Connection closed flag should be set");
    }
    
    @Test
    public void testRemoveNonExistentEndpointDoesNothing() {
        // Try to remove an endpoint that was never registered
        assertFalse(server.removeEndpoint(TestEndpoint.class, null));
    }
    
    @Test
    public void testAddRemoveAdd() throws Exception {
        // Add an endpoint
        server.addEndpoint(TestEndpoint.class, null, null);
        
        // Remove it
        assertTrue(server.removeEndpoint(TestEndpoint.class, null));
        
        // Add it again
        assertDoesNotThrow(() -> server.addEndpoint(TestEndpoint.class, null, null));
        
        // Verify it works
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
        
        webSocket.sendText("test", true).join();
        
        boolean received = messageLatch.await(5, TimeUnit.SECONDS);
        assertTrue(received, "Should receive response from re-registered endpoint");
        
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
    }
}
