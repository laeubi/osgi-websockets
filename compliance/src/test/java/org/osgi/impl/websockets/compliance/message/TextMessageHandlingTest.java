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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.*;

/**
 * Compliance tests for text message handling with various parameter types.
 * 
 * Adapted from Jakarta WebSocket 2.2 TCK:
 * - com.sun.ts.tests.websocket.ee.jakarta.websocket.websocketmessage
 * - Specification: Jakarta WebSocket 2.2, Section 3.5 (Message Handlers)
 * 
 * Note: This test focuses on currently implemented features:
 * - String messages
 * - Messages with Session parameter
 * - Custom object types with Text Decoders
 */
public class TextMessageHandlingTest {
    
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
     * Test @OnMessage with String parameter
     * 
     * TCK Reference: WSStringServer
     * Specification: Section 3.5
     */
    @Test
    public void testStringMessage() throws Exception {
        server.createEndpoint(StringEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/string"), 
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
        
        ws.sendText("Hello", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("Hello", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with String and Session parameters
     * 
     * TCK Reference: WSStringAndSessionServer
     * Specification: Section 3.5
     */
    @Test
    public void testStringWithSessionMessage() throws Exception {
        server.createEndpoint(StringWithSessionEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/stringwithsession"), 
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
        
        ws.sendText("test", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertTrue(response.startsWith("test-"), "Response should start with 'test-' followed by session ID");
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with Session and String parameters (reversed order)
     * 
     * TCK Reference: Various TCK tests
     * Specification: Section 3.5
     */
    @Test
    public void testSessionWithStringMessage() throws Exception {
        server.createEndpoint(SessionWithStringEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/sessionwithstring"), 
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
        
        ws.sendText("hello", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertTrue(response.endsWith("-hello"), "Response should end with '-hello'");
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with custom object using Text decoder
     * 
     * TCK Reference: WSTextDecoderServer
     * Specification: Section 3.5, 4.5 (Decoders)
     */
    @Test
    public void testCustomObjectWithDecoder() throws Exception {
        server.createEndpoint(CustomObjectEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/customobject"), 
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
        
        ws.sendText("Alice:30", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("Person{name='Alice', age=30}", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage return value
     * 
     * TCK Reference: Various return type tests
     * Specification: Section 3.5
     */
    @Test
    public void testMessageReturnValue() throws Exception {
        server.createEndpoint(MessageReturnEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/return"), 
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
        
        ws.sendText("input", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("ECHO: input", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage void return (no response)
     * 
     * TCK Reference: Various void return tests
     * Specification: Section 3.5
     */
    @Test
    public void testVoidReturn() throws Exception {
        server.createEndpoint(VoidReturnEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<Boolean> openFuture = new CompletableFuture<>();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/voidreturn"), 
                new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        openFuture.complete(true);
                        WebSocket.Listener.super.onOpen(webSocket);
                    }
                    
                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, 
                                                     CharSequence data, 
                                                     boolean last) {
                        messageFuture.complete(data.toString());
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        // Wait for connection
        openFuture.get(5, TimeUnit.SECONDS);
        
        // Send message - should not get a response since method returns void
        ws.sendText("test", true);
        
        // Wait a bit to ensure no response is sent
        Thread.sleep(500);
        
        assertFalse(messageFuture.isDone(), "Should not receive a response for void return");
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with empty string
     * 
     * TCK Reference: Various edge case tests
     * Specification: Section 3.5
     */
    @Test
    public void testEmptyStringMessage() throws Exception {
        server.createEndpoint(StringEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/string"), 
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
        
        ws.sendText("", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with large text message
     * 
     * TCK Reference: Various performance/limit tests
     * Specification: Section 3.5
     */
    @Test
    public void testLargeTextMessage() throws Exception {
        server.createEndpoint(StringEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/string"), 
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
        
        // Create a large message (10KB)
        StringBuilder largeMessage = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeMessage.append("0123456789");
        }
        String message = largeMessage.toString();
        
        ws.sendText(message, true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals(message, response);
        assertEquals(10000, response.length());
        
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
    
    @ServerEndpoint("/string")
    public static class StringEndpoint {
        @OnMessage
        public String echo(String message) {
            return message;
        }
    }
    
    @ServerEndpoint("/stringwithsession")
    public static class StringWithSessionEndpoint {
        @OnMessage
        public String echo(String message, Session session) {
            return message + "-" + session.getId();
        }
    }
    
    @ServerEndpoint("/sessionwithstring")
    public static class SessionWithStringEndpoint {
        @OnMessage
        public String echo(Session session, String message) {
            return session.getId() + "-" + message;
        }
    }
    
    @ServerEndpoint(value = "/customobject", decoders = {PersonDecoder.class})
    public static class CustomObjectEndpoint {
        @OnMessage
        public String echo(Person person) {
            return person.toString();
        }
    }
    
    @ServerEndpoint("/return")
    public static class MessageReturnEndpoint {
        @OnMessage
        public String processMessage(String message) {
            return "ECHO: " + message;
        }
    }
    
    @ServerEndpoint("/voidreturn")
    public static class VoidReturnEndpoint {
        @OnMessage
        public void processMessage(String message) {
            // Process but don't return anything
            System.out.println("Received: " + message);
        }
    }
    
    // Helper classes for custom object test
    
    public static class Person {
        private String name;
        private int age;
        
        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
        
        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + "}";
        }
    }
    
    public static class PersonDecoder implements Decoder.Text<Person> {
        @Override
        public Person decode(String s) throws DecodeException {
            String[] parts = s.split(":");
            if (parts.length != 2) {
                throw new DecodeException(s, "Invalid format. Expected: name:age");
            }
            return new Person(parts[0], Integer.parseInt(parts[1]));
        }
        
        @Override
        public boolean willDecode(String s) {
            return s != null && s.contains(":");
        }
        
        @Override
        public void init(EndpointConfig config) {
        }
        
        @Override
        public void destroy() {
        }
    }
}
