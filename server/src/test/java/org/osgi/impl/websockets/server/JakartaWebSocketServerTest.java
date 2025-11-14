package org.osgi.impl.websockets.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Tests for the JakartaWebSocketServer using Java 11 HttpClient WebSocket support.
 * These tests verify the server functionality from a client perspective.
 */
public class JakartaWebSocketServerTest {
    
    private JakartaWebSocketServer server;
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 8888;
    
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
    public void testWebSocketConnectionAndEcho() throws Exception {
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
        
        URI serverUri = URI.create("ws://" + HOSTNAME + ":" + PORT + "/ws");
        WebSocket webSocket = client.newWebSocketBuilder()
            .buildAsync(serverUri, listener)
            .join();
        
        // Send a test message
        String testMessage = "Hello WebSocket";
        webSocket.sendText(testMessage, true).join();
        
        // Wait for the echo response
        boolean received = messageLatch.await(5, TimeUnit.SECONDS);
        assertTrue(received, "Should receive echo response within timeout");
        
        // Verify the echo response
        String expectedResponse = "Echo: " + testMessage;
        assertEquals(expectedResponse, receivedMessage.get(), "Should receive echoed message");
        
        // Close the WebSocket
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
    }
    
    @Test
    public void testMultipleMessages() throws Exception {
        CountDownLatch messageLatch = new CountDownLatch(3);
        AtomicReference<String> lastMessage = new AtomicReference<>();
        StringBuilder allMessages = new StringBuilder();
        
        HttpClient client = HttpClient.newHttpClient();
        
        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                String message = data.toString();
                allMessages.append(message).append(";");
                lastMessage.set(message);
                messageLatch.countDown();
                // Request more data
                webSocket.request(1);
                return CompletableFuture.completedFuture(null);
            }
        };
        
        URI serverUri = URI.create("ws://" + HOSTNAME + ":" + PORT + "/ws");
        WebSocket webSocket = client.newWebSocketBuilder()
            .buildAsync(serverUri, listener)
            .join();
        
        // Send multiple messages with delays to ensure they're processed
        webSocket.sendText("Message 1", true).join();
        Thread.sleep(100);
        webSocket.sendText("Message 2", true).join();
        Thread.sleep(100);
        webSocket.sendText("Message 3", true).join();
        
        // Wait for all responses
        boolean received = messageLatch.await(5, TimeUnit.SECONDS);
        assertTrue(received, "Should receive all echo responses within timeout. Received: " + allMessages.toString());
        
        // Verify the last message
        assertEquals("Echo: Message 3", lastMessage.get(), "Last message should be echoed correctly");
        
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
    }
    
}
