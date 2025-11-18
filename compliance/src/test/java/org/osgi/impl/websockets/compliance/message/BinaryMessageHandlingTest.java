package org.osgi.impl.websockets.compliance.message;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.osgi.impl.websockets.server.JakartaWebSocketServer;
import org.osgi.impl.websockets.server.EndpointHandler;

import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.Decoder;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EncodeException;
import jakarta.websocket.EndpointConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/**
 * Compliance tests for binary message handling.
 * 
 * Adapted from Jakarta WebSocket 2.2 TCK:
 * - com.sun.ts.tests.websocket.ee.jakarta.websocket.websocketmessage
 * - Specification: Jakarta WebSocket 2.2, Section 3.5 (Message Handlers)
 */
public class BinaryMessageHandlingTest {
    
    private JakartaWebSocketServer server;
    private int port;
    
    @BeforeEach
    public void setUp() throws Exception {
        port = 8080 + ThreadLocalRandom.current().nextInt(1000);
        server = new JakartaWebSocketServer("localhost", port);
        server.start();
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }
    
    /**
     * Test @OnMessage with ByteBuffer parameter
     * 
     * TCK Reference: WSByteBufferServer
     * Specification: Section 3.5
     */
    @Test
    public void testByteBufferMessage() throws Exception {
        server.createEndpoint(ByteBufferEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<ByteBuffer> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/bytebuffer"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, 
                                                       ByteBuffer data, 
                                                       boolean last) {
                        messageFuture.complete(data);
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        ByteBuffer testData = ByteBuffer.wrap("Hello Binary".getBytes(StandardCharsets.UTF_8));
        ws.sendBinary(testData, true);
        
        ByteBuffer response = messageFuture.get(5, TimeUnit.SECONDS);
        
        byte[] responseBytes = new byte[response.remaining()];
        response.get(responseBytes);
        String responseText = new String(responseBytes, StandardCharsets.UTF_8);
        assertEquals("Hello Binary", responseText);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with byte[] parameter
     * 
     * TCK Reference: WSByteArrayServer
     * Specification: Section 3.5
     */
    @Test
    public void testByteArrayMessage() throws Exception {
        server.createEndpoint(ByteArrayEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<ByteBuffer> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/bytearray"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, 
                                                       ByteBuffer data, 
                                                       boolean last) {
                        messageFuture.complete(data);
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        byte[] testData = "Binary Data".getBytes(StandardCharsets.UTF_8);
        ws.sendBinary(ByteBuffer.wrap(testData), true);
        
        ByteBuffer response = messageFuture.get(5, TimeUnit.SECONDS);
        
        byte[] responseBytes = new byte[response.remaining()];
        response.get(responseBytes);
        String responseText = new String(responseBytes, StandardCharsets.UTF_8);
        assertEquals("Binary Data", responseText);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with ByteBuffer and Session parameters
     * 
     * TCK Reference: WSByteBufferAndSessionServer
     * Specification: Section 3.5
     */
    @Test
    public void testByteBufferWithSession() throws Exception {
        server.createEndpoint(ByteBufferWithSessionEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<ByteBuffer> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/bytebuffersession"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, 
                                                       ByteBuffer data, 
                                                       boolean last) {
                        messageFuture.complete(data);
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        ByteBuffer testData = ByteBuffer.wrap("Test".getBytes(StandardCharsets.UTF_8));
        ws.sendBinary(testData, true);
        
        ByteBuffer response = messageFuture.get(5, TimeUnit.SECONDS);
        assertNotNull(response);
        assertTrue(response.remaining() > 0);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with byte[] and Session parameters
     * 
     * TCK Reference: WSByteArrayAndSessionServer
     * Specification: Section 3.5
     */
    @Test
    public void testByteArrayWithSession() throws Exception {
        server.createEndpoint(ByteArrayWithSessionEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<ByteBuffer> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/bytearraysession"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, 
                                                       ByteBuffer data, 
                                                       boolean last) {
                        messageFuture.complete(data);
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        byte[] testData = "Session Test".getBytes(StandardCharsets.UTF_8);
        ws.sendBinary(ByteBuffer.wrap(testData), true);
        
        ByteBuffer response = messageFuture.get(5, TimeUnit.SECONDS);
        assertNotNull(response);
        assertTrue(response.remaining() > 0);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with Session and ByteBuffer parameters (reversed order)
     * 
     * TCK Reference: Various TCK tests
     * Specification: Section 3.5
     */
    @Test
    public void testSessionWithByteBuffer() throws Exception {
        server.createEndpoint(SessionWithByteBufferEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<ByteBuffer> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/sessionbytebuffer"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, 
                                                       ByteBuffer data, 
                                                       boolean last) {
                        messageFuture.complete(data);
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        ByteBuffer testData = ByteBuffer.wrap("Reversed".getBytes(StandardCharsets.UTF_8));
        ws.sendBinary(testData, true);
        
        ByteBuffer response = messageFuture.get(5, TimeUnit.SECONDS);
        assertNotNull(response);
        assertTrue(response.remaining() > 0);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with large binary message
     * 
     * TCK Reference: Various performance/limit tests
     * Specification: Section 3.5
     */
    @Test
    public void testLargeBinaryMessage() throws Exception {
        server.createEndpoint(ByteArrayEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<ByteBuffer> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/bytearray"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, 
                                                       ByteBuffer data, 
                                                       boolean last) {
                        messageFuture.complete(data);
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        // Create a large binary message (10KB)
        byte[] largeData = new byte[10240];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        
        ws.sendBinary(ByteBuffer.wrap(largeData), true);
        
        ByteBuffer response = messageFuture.get(5, TimeUnit.SECONDS);
        
        byte[] responseBytes = new byte[response.remaining()];
        response.get(responseBytes);
        assertArrayEquals(largeData, responseBytes);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with empty binary message
     * 
     * TCK Reference: Various edge case tests
     * Specification: Section 3.5
     */
    @Test
    public void testEmptyBinaryMessage() throws Exception {
        server.createEndpoint(ByteArrayEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<ByteBuffer> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/bytearray"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, 
                                                       ByteBuffer data, 
                                                       boolean last) {
                        messageFuture.complete(data);
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        byte[] emptyData = new byte[0];
        ws.sendBinary(ByteBuffer.wrap(emptyData), true);
        
        ByteBuffer response = messageFuture.get(5, TimeUnit.SECONDS);
        
        byte[] responseBytes = new byte[response.remaining()];
        response.get(responseBytes);
        assertEquals(0, responseBytes.length);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    private <T> EndpointHandler<T> createHandler() {
        return new EndpointHandler<T>() {
            @Override
            public T createEndpointInstance(Class<T> endpointClass) 
                    throws InstantiationException {
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed: " + e.getMessage());
                }
            }
            
            @Override
            public void sessionEnded(T endpointInstance) {
                // Cleanup if needed
            }
        };
    }
    
    // Test endpoint classes
    
    @ServerEndpoint("/bytebuffer")
    public static class ByteBufferEndpoint {
        @OnMessage
        public void echo(ByteBuffer data, Session session) throws IOException {
            session.getBasicRemote().sendBinary(data);
        }
    }
    
    @ServerEndpoint("/bytearray")
    public static class ByteArrayEndpoint {
        @OnMessage
        public void echo(byte[] data, Session session) throws IOException {
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(data));
        }
    }
    
    @ServerEndpoint("/bytebuffersession")
    public static class ByteBufferWithSessionEndpoint {
        @OnMessage
        public void echo(ByteBuffer data, Session session) throws IOException {
            // Include session ID in response by appending bytes
            byte[] dataBytes = new byte[data.remaining()];
            data.get(dataBytes);
            String sessionInfo = "-" + session.getId().substring(0, 8);
            byte[] sessionBytes = sessionInfo.getBytes(StandardCharsets.UTF_8);
            
            ByteBuffer result = ByteBuffer.allocate(dataBytes.length + sessionBytes.length);
            result.put(dataBytes);
            result.put(sessionBytes);
            result.flip();
            session.getBasicRemote().sendBinary(result);
        }
    }
    
    @ServerEndpoint("/bytearraysession")
    public static class ByteArrayWithSessionEndpoint {
        @OnMessage
        public void echo(byte[] data, Session session) throws IOException {
            // Include session ID in response
            String sessionInfo = "-" + session.getId().substring(0, 8);
            byte[] sessionBytes = sessionInfo.getBytes(StandardCharsets.UTF_8);
            
            byte[] result = new byte[data.length + sessionBytes.length];
            System.arraycopy(data, 0, result, 0, data.length);
            System.arraycopy(sessionBytes, 0, result, data.length, sessionBytes.length);
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(result));
        }
    }
    
    @ServerEndpoint("/sessionbytebuffer")
    public static class SessionWithByteBufferEndpoint {
        @OnMessage
        public void echo(Session session, ByteBuffer data) throws IOException {
            // Prepend session ID to response
            String sessionInfo = session.getId().substring(0, 8) + "-";
            byte[] sessionBytes = sessionInfo.getBytes(StandardCharsets.UTF_8);
            byte[] dataBytes = new byte[data.remaining()];
            data.get(dataBytes);
            
            ByteBuffer result = ByteBuffer.allocate(sessionBytes.length + dataBytes.length);
            result.put(sessionBytes);
            result.put(dataBytes);
            result.flip();
            session.getBasicRemote().sendBinary(result);
        }
    }
}
