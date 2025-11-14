package org.osgi.impl.websockets.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for encoder/decoder functionality in the WebSocket server.
 */
public class EncoderDecoderTest {
    
    private JakartaWebSocketServer server;
    
    @AfterEach
    public void cleanup() {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }
    
    @Test
    public void testTextEncoderDecoder() throws Exception {
        // Start server
        server = new JakartaWebSocketServer("localhost", 8893);
        server.start();
        
        // Register endpoint with encoder/decoder
        EndpointHandler<MessageEndpoint> handler = new EndpointHandler<MessageEndpoint>() {
            @Override
            public MessageEndpoint createEndpointInstance(Class<MessageEndpoint> endpointClass) 
                    throws InstantiationException {
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed to create instance: " + e.getMessage());
                }
            }
            
            @Override
            public void sessionEnded(MessageEndpoint endpointInstance) {
                // No cleanup needed
            }
        };
        
        server.createEndpoint(MessageEndpoint.class, null, handler);
        
        // Create WebSocket client
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> receivedMessage = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:8893/message-test"), new WebSocket.Listener() {
                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    receivedMessage.complete(data.toString());
                    return CompletableFuture.completedFuture(null);
                }
                
                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    receivedMessage.completeExceptionally(error);
                }
            })
            .get(5, TimeUnit.SECONDS);
        
        // Wait for connection to be established
        Thread.sleep(100);
        
        // Send a message in the format expected by MessageDecoder: "content|timestamp"
        long timestamp = System.currentTimeMillis();
        String testMessage = "Hello World|" + timestamp;
        ws.sendText(testMessage, true).get(5, TimeUnit.SECONDS);
        
        // Wait for response
        String response = receivedMessage.get(5, TimeUnit.SECONDS);
        
        // Verify the response
        assertNotNull(response);
        assertTrue(response.startsWith("Echo: Hello World|"), "Response should start with 'Echo: Hello World|'");
        assertTrue(response.contains(String.valueOf(timestamp)), "Response should contain the same timestamp");
        
        // Parse the response to verify it's in the correct format
        String[] parts = response.split("\\|");
        assertEquals(2, parts.length, "Response should have two parts separated by |");
        assertEquals("Echo: Hello World", parts[0]);
        assertEquals(timestamp, Long.parseLong(parts[1]));
        
        // Close WebSocket
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").get(5, TimeUnit.SECONDS);
        
        // Stop server
        server.stop();
    }
    
    @Test
    public void testEncoderDecoderWithMultipleMessages() throws Exception {
        // Start server
        server = new JakartaWebSocketServer("localhost", 8894);
        server.start();
        
        // Register endpoint
        EndpointHandler<MessageEndpoint> handler = new EndpointHandler<MessageEndpoint>() {
            @Override
            public MessageEndpoint createEndpointInstance(Class<MessageEndpoint> endpointClass) 
                    throws InstantiationException {
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed to create instance: " + e.getMessage());
                }
            }
            
            @Override
            public void sessionEnded(MessageEndpoint endpointInstance) {
                // No cleanup needed
            }
        };
        
        server.createEndpoint(MessageEndpoint.class, null, handler);
        
        // Create WebSocket client
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> firstMessage = new CompletableFuture<>();
        CompletableFuture<String> secondMessage = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:8894/message-test"), new WebSocket.Listener() {
                private int messageCount = 0;
                
                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    messageCount++;
                    if (messageCount == 1) {
                        firstMessage.complete(data.toString());
                    } else if (messageCount == 2) {
                        secondMessage.complete(data.toString());
                    }
                    return CompletableFuture.completedFuture(null);
                }
                
                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    firstMessage.completeExceptionally(error);
                    secondMessage.completeExceptionally(error);
                }
            })
            .get(5, TimeUnit.SECONDS);
        
        // Wait for connection
        Thread.sleep(100);
        
        // Send first message
        long timestamp1 = System.currentTimeMillis();
        ws.sendText("First|" + timestamp1, true).get(5, TimeUnit.SECONDS);
        String response1 = firstMessage.get(5, TimeUnit.SECONDS);
        assertEquals("Echo: First|" + timestamp1, response1);
        
        // Send second message
        Thread.sleep(50);
        long timestamp2 = System.currentTimeMillis();
        ws.sendText("Second|" + timestamp2, true).get(5, TimeUnit.SECONDS);
        String response2 = secondMessage.get(5, TimeUnit.SECONDS);
        assertEquals("Echo: Second|" + timestamp2, response2);
        
        // Close
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").get(5, TimeUnit.SECONDS);
        server.stop();
    }
}
