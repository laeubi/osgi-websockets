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
 * Tests for Session support in WebSocket endpoints.
 */
public class SessionSupportTest {
    
    private JakartaWebSocketServer server;
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 8891;
    
    private EndpointHandler<SessionEndpoint> createSimpleHandler() {
        return new EndpointHandler<SessionEndpoint>() {
            @Override
            public SessionEndpoint createEndpointInstance(Class<SessionEndpoint> endpointClass) throws InstantiationException {
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed to create instance: " + e.getMessage());
                }
            }
            
            @Override
            public void sessionEnded(SessionEndpoint endpointInstance) {
                // No-op for this test
            }
        };
    }
    
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
    public void testSessionIdIsProvided() throws Exception {
        server.createEndpoint(SessionEndpoint.class, null, createSimpleHandler());
        
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
        
        URI serverUri = URI.create("ws://" + HOSTNAME + ":" + PORT + "/session-test");
        WebSocket webSocket = client.newWebSocketBuilder()
            .buildAsync(serverUri, listener)
            .join();
        
        // Test session ID
        webSocket.sendText("test-session-id", true).join();
        
        boolean received = messageLatch.await(5, TimeUnit.SECONDS);
        assertTrue(received, "Should receive response within timeout");
        
        String response = receivedMessage.get();
        assertTrue(response.startsWith("Session ID: "), "Response should contain session ID");
        assertFalse(response.endsWith("null"), "Session ID should not be null");
        
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
    }
    
    @Test
    public void testSessionRequestURI() throws Exception {
        server.createEndpoint(SessionEndpoint.class, null, createSimpleHandler());
        
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
        
        URI serverUri = URI.create("ws://" + HOSTNAME + ":" + PORT + "/session-test");
        WebSocket webSocket = client.newWebSocketBuilder()
            .buildAsync(serverUri, listener)
            .join();
        
        // Test request URI
        webSocket.sendText("test-session-uri", true).join();
        
        boolean received = messageLatch.await(5, TimeUnit.SECONDS);
        assertTrue(received, "Should receive response within timeout");
        
        String response = receivedMessage.get();
        assertEquals("Request URI: /session-test", response, 
            "Response should contain the correct request URI path");
        
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
    }
    
    @Test
    public void testSessionIsOpen() throws Exception {
        server.createEndpoint(SessionEndpoint.class, null, createSimpleHandler());
        
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
        
        URI serverUri = URI.create("ws://" + HOSTNAME + ":" + PORT + "/session-test");
        WebSocket webSocket = client.newWebSocketBuilder()
            .buildAsync(serverUri, listener)
            .join();
        
        // Test session isOpen
        webSocket.sendText("test-session-open", true).join();
        
        boolean received = messageLatch.await(5, TimeUnit.SECONDS);
        assertTrue(received, "Should receive response within timeout");
        
        String response = receivedMessage.get();
        assertEquals("Session open: true", response, 
            "Session should be open when message is being processed");
        
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
    }
    
    @Test
    public void testBasicRemoteEndpoint() throws Exception {
        server.createEndpoint(SessionEndpoint.class, null, createSimpleHandler());
        
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
        
        URI serverUri = URI.create("ws://" + HOSTNAME + ":" + PORT + "/session-test");
        WebSocket webSocket = client.newWebSocketBuilder()
            .buildAsync(serverUri, listener)
            .join();
        
        // Test basic remote endpoint
        webSocket.sendText("test-basic-remote", true).join();
        
        boolean received = messageLatch.await(5, TimeUnit.SECONDS);
        assertTrue(received, "Should receive response within timeout");
        
        String response = receivedMessage.get();
        assertEquals("Response via BasicRemote", response, 
            "Should receive response sent via BasicRemote");
        
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
    }
    
    @Test
    public void testMultipleSessionsHaveUniqueIds() throws Exception {
        server.createEndpoint(SessionEndpoint.class, null, createSimpleHandler());
        
        // Create two connections
        CountDownLatch messageLatch1 = new CountDownLatch(1);
        CountDownLatch messageLatch2 = new CountDownLatch(1);
        AtomicReference<String> receivedMessage1 = new AtomicReference<>();
        AtomicReference<String> receivedMessage2 = new AtomicReference<>();
        
        HttpClient client = HttpClient.newHttpClient();
        
        WebSocket.Listener listener1 = new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                receivedMessage1.set(data.toString());
                messageLatch1.countDown();
                return CompletableFuture.completedFuture(null);
            }
        };
        
        WebSocket.Listener listener2 = new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                receivedMessage2.set(data.toString());
                messageLatch2.countDown();
                return CompletableFuture.completedFuture(null);
            }
        };
        
        URI serverUri = URI.create("ws://" + HOSTNAME + ":" + PORT + "/session-test");
        WebSocket webSocket1 = client.newWebSocketBuilder()
            .buildAsync(serverUri, listener1)
            .join();
        WebSocket webSocket2 = client.newWebSocketBuilder()
            .buildAsync(serverUri, listener2)
            .join();
        
        // Request session IDs from both connections
        webSocket1.sendText("test-session-id", true).join();
        webSocket2.sendText("test-session-id", true).join();
        
        boolean received1 = messageLatch1.await(5, TimeUnit.SECONDS);
        boolean received2 = messageLatch2.await(5, TimeUnit.SECONDS);
        
        assertTrue(received1, "Should receive response from first connection");
        assertTrue(received2, "Should receive response from second connection");
        
        String sessionId1 = receivedMessage1.get().substring("Session ID: ".length());
        String sessionId2 = receivedMessage2.get().substring("Session ID: ".length());
        
        assertNotEquals(sessionId1, sessionId2, 
            "Different connections should have different session IDs");
        
        webSocket1.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        webSocket2.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
    }
}
