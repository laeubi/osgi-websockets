package org.osgi.impl.websockets.server;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for endpoint lifecycle management.
 */
public class EndpointLifecycleTest {
    
    private JakartaWebSocketServer server;
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 8889;
    
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
    public void testEndpointLifecycle() throws Exception {
        // Register an endpoint
        server.addEndpoint(TestEndpoint.class, null, null);
        
        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();
        
        HttpClient client = HttpClient.newHttpClient();
        
        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                receivedMessage.set(data.toString());
                messageLatch.countDown();
                return CompletableFuture.completedFuture(null);
            }
            
            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                error.printStackTrace();
            }
        };
        
        URI serverUri = URI.create("ws://" + HOSTNAME + ":" + PORT + "/test");
        WebSocket webSocket = client.newWebSocketBuilder()
            .buildAsync(serverUri, listener)
            .join();
        
        // Send a test message
        String testMessage = "Hello Endpoint";
        webSocket.sendText(testMessage, true).join();
        
        // Wait for the response
        boolean received = messageLatch.await(5, TimeUnit.SECONDS);
        assertTrue(received, "Should receive response within timeout");
        
        // Verify the response from the endpoint
        String expectedResponse = "Test: " + testMessage;
        assertEquals(expectedResponse, receivedMessage.get(), "Should receive response from endpoint");
        
        // Close the WebSocket
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
    }
    
    @Test
    public void testEndpointWithCustomPath() throws Exception {
        // Register an endpoint with a custom path
        server.addEndpoint(TestEndpoint.class, "/custom-path", null);
        
        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();
        
        HttpClient client = HttpClient.newHttpClient();
        
        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                receivedMessage.set(data.toString());
                messageLatch.countDown();
                return CompletableFuture.completedFuture(null);
            }
        };
        
        URI serverUri = URI.create("ws://" + HOSTNAME + ":" + PORT + "/custom-path");
        WebSocket webSocket = client.newWebSocketBuilder()
            .buildAsync(serverUri, listener)
            .join();
        
        // Send a test message
        String testMessage = "Custom Path Test";
        webSocket.sendText(testMessage, true).join();
        
        // Wait for the response
        boolean received = messageLatch.await(5, TimeUnit.SECONDS);
        assertTrue(received, "Should receive response within timeout");
        
        // Verify the response
        String expectedResponse = "Test: " + testMessage;
        assertEquals(expectedResponse, receivedMessage.get());
        
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
    }
    
    @Test
    public void testEchoFallbackForUnregisteredPath() throws Exception {
        // Don't register any endpoint - should fall back to echo behavior
        
        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();
        
        HttpClient client = HttpClient.newHttpClient();
        
        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                receivedMessage.set(data.toString());
                messageLatch.countDown();
                return CompletableFuture.completedFuture(null);
            }
        };
        
        URI serverUri = URI.create("ws://" + HOSTNAME + ":" + PORT + "/ws");
        WebSocket webSocket = client.newWebSocketBuilder()
            .buildAsync(serverUri, listener)
            .join();
        
        // Send a test message
        String testMessage = "Echo Test";
        webSocket.sendText(testMessage, true).join();
        
        // Wait for the response
        boolean received = messageLatch.await(5, TimeUnit.SECONDS);
        assertTrue(received, "Should receive echo response within timeout");
        
        // Verify the echo response
        String expectedResponse = "Echo: " + testMessage;
        assertEquals(expectedResponse, receivedMessage.get(), "Should receive echoed message");
        
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
    }
}
