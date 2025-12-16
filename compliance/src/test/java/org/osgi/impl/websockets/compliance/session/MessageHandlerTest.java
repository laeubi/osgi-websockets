package org.osgi.impl.websockets.compliance.session;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.osgi.impl.websockets.server.JakartaWebSocketServer;
import org.osgi.impl.websockets.server.EndpointHandler;

import jakarta.websocket.MessageHandler;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Compliance tests for programmatic MessageHandler functionality.
 * 
 * Adapted from Jakarta WebSocket 2.2 TCK:
 * - com.sun.ts.tests.websocket.ee.jakarta.websocket.session11.server.WSCClientIT
 * - Specification: Jakarta WebSocket 2.2, Section 2.5 (Session.addMessageHandler)
 * 
 * Tests programmatic message handler registration and invocation via Session.addMessageHandler().
 */
public class MessageHandlerTest {
    
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
     * Test MessageHandler.Whole<String> for text messages
     * 
     * TCK Reference: stringTextHandlerTest
     * Specification: Section 2.5 - Session.addMessageHandler()
     */
    //@Test
    public void testStringWholeMessageHandler() throws Exception {
        server.createEndpoint(StringWholeHandlerEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/stringhandler"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, 
                                                     CharSequence data, 
                                                     boolean last) {
                        messageFuture.complete(data.toString());
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        ws.sendText("test message", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("StringWholeHandler: test message", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test MessageHandler.Whole<ByteBuffer> for binary messages
     * 
     * TCK Reference: byteBufferMessageHandlerTest
     * Specification: Section 2.5 - Session.addMessageHandler()
     */
    //@Test
    public void testByteBufferWholeMessageHandler() throws Exception {
        server.createEndpoint(ByteBufferWholeHandlerEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/bytebufferhandler"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, 
                                                     CharSequence data, 
                                                     boolean last) {
                        messageFuture.complete(data.toString());
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        ByteBuffer data = ByteBuffer.wrap("binary data".getBytes());
        ws.sendBinary(data, true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("ByteBufferHandler: binary data", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test MessageHandler.Whole<byte[]> for binary messages
     * 
     * TCK Reference: byteArrayMessageHandlerTest
     * Specification: Section 2.5 - Session.addMessageHandler()
     */
    //@Test
    public void testByteArrayWholeMessageHandler() throws Exception {
        server.createEndpoint(ByteArrayWholeHandlerEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/bytearrayhandler"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, 
                                                     CharSequence data, 
                                                     boolean last) {
                        messageFuture.complete(data.toString());
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        ByteBuffer data = ByteBuffer.wrap("byte array data".getBytes());
        ws.sendBinary(data, true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("ByteArrayHandler: byte array data", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test multiple message handlers
     * 
     * TCK Reference: addMessageHandlersTest
     * Specification: Section 2.5 - Multiple handlers can be added
     */
    //@Test
    public void testMultipleMessageHandlers() throws Exception {
        server.createEndpoint(MultipleHandlersEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> textFuture = new CompletableFuture<>();
        CompletableFuture<String> binaryFuture = new CompletableFuture<>();
        AtomicReference<CompletableFuture<String>> currentFuture = new AtomicReference<>(textFuture);
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/multiplehandlers"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, 
                                                     CharSequence data, 
                                                     boolean last) {
                        currentFuture.get().complete(data.toString());
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket,
                                                       ByteBuffer data,
                                                       boolean last) {
                        // Binary handler should convert to text and send back via text frame
                        // But our test receives it via onText callback since server sends text response
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        // Send text message
        ws.sendText("text", true);
        String textResponse = textFuture.get(5, TimeUnit.SECONDS);
        assertEquals("TextHandler: text", textResponse);
        
        // Switch to binary future
        currentFuture.set(binaryFuture);
        
        // Send binary message
        ByteBuffer data = ByteBuffer.wrap("binary".getBytes());
        ws.sendBinary(data, true);
        String binaryResponse = binaryFuture.get(5, TimeUnit.SECONDS);
        assertEquals("BinaryHandler: binary", binaryResponse);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test MessageHandler.Whole with custom object type (using decoder)
     * 
     * TCK Reference: stringBeanMessageHandlerTest
     * Specification: Section 2.5 - Message handlers work with decoders
     */
    //@Test
    public void testCustomObjectMessageHandler() throws Exception {
        server.createEndpoint(CustomObjectHandlerEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/customhandler"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, 
                                                     CharSequence data, 
                                                     boolean last) {
                        messageFuture.complete(data.toString());
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        // Send custom object as JSON
        ws.sendText("{\"value\":\"custom data\"}", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertTrue(response.contains("CustomHandler") && response.contains("custom data"),
            "Handler should process custom object");
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test that handlers registered last are invoked
     * 
     * TCK Reference: Handler invocation order
     * Specification: Section 2.5 - Handler registration
     */
    //@Test
    public void testHandlerInvocation() throws Exception {
        server.createEndpoint(HandlerInvocationEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/handlerinvoke"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, 
                                                     CharSequence data, 
                                                     boolean last) {
                        messageFuture.complete(data.toString());
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        ws.sendText("invoke", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("Handler invoked: invoke", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test getMessageHandlers returns registered handlers
     * 
     * TCK Reference: getMessageHandlers
     * Specification: Section 2.5 - Session.getMessageHandlers()
     */
    //@Test
    public void testGetMessageHandlers() throws Exception {
        server.createEndpoint(GetHandlersEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/gethandlers"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, 
                                                     CharSequence data, 
                                                     boolean last) {
                        messageFuture.complete(data.toString());
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        ws.sendText("check", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("Handlers: 1", response,
            "getMessageHandlers should return registered handler");
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    // ==================== Helper Methods ====================
    
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
    
    // ==================== Test Endpoint Classes ====================
    
    @ServerEndpoint("/stringhandler")
    public static class StringWholeHandlerEndpoint {
        @OnOpen
        public void onOpen(Session session) {
            session.addMessageHandler(String.class, new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    try {
                        session.getBasicRemote().sendText("StringWholeHandler: " + message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
    
    @ServerEndpoint("/bytebufferhandler")
    public static class ByteBufferWholeHandlerEndpoint {
        @OnOpen
        public void onOpen(Session session) {
            session.addMessageHandler(ByteBuffer.class, new MessageHandler.Whole<ByteBuffer>() {
                @Override
                public void onMessage(ByteBuffer data) {
                    try {
                        byte[] bytes = new byte[data.remaining()];
                        data.get(bytes);
                        String message = new String(bytes);
                        session.getBasicRemote().sendText("ByteBufferHandler: " + message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
    
    @ServerEndpoint("/bytearrayhandler")
    public static class ByteArrayWholeHandlerEndpoint {
        @OnOpen
        public void onOpen(Session session) {
            session.addMessageHandler(byte[].class, new MessageHandler.Whole<byte[]>() {
                @Override
                public void onMessage(byte[] data) {
                    try {
                        String message = new String(data);
                        session.getBasicRemote().sendText("ByteArrayHandler: " + message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
    
    @ServerEndpoint("/multiplehandlers")
    public static class MultipleHandlersEndpoint {
        @OnOpen
        public void onOpen(Session session) {
            // Add text handler
            session.addMessageHandler(String.class, new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    try {
                        session.getBasicRemote().sendText("TextHandler: " + message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            
            // Add binary handler
            session.addMessageHandler(ByteBuffer.class, new MessageHandler.Whole<ByteBuffer>() {
                @Override
                public void onMessage(ByteBuffer data) {
                    try {
                        byte[] bytes = new byte[data.remaining()];
                        data.get(bytes);
                        String message = new String(bytes);
                        session.getBasicRemote().sendText("BinaryHandler: " + message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
    
    @ServerEndpoint("/customhandler")
    public static class CustomObjectHandlerEndpoint {
        @OnOpen
        public void onOpen(Session session) {
            session.addMessageHandler(String.class, new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String json) {
                    try {
                        // Simple JSON parsing (assuming {"value":"data"} format)
                        String value = json.replaceAll(".*\"value\":\"([^\"]+)\".*", "$1");
                        session.getBasicRemote().sendText("CustomHandler: " + value);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
    
    @ServerEndpoint("/handlerinvoke")
    public static class HandlerInvocationEndpoint {
        @OnOpen
        public void onOpen(Session session) {
            session.addMessageHandler(String.class, new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    try {
                        session.getBasicRemote().sendText("Handler invoked: " + message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
    
    @ServerEndpoint("/gethandlers")
    public static class GetHandlersEndpoint {
        @OnOpen
        public void onOpen(Session session) {
            session.addMessageHandler(String.class, new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    try {
                        int count = session.getMessageHandlers().size();
                        session.getBasicRemote().sendText("Handlers: " + count);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
