package org.osgi.impl.websockets.compliance.session;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.osgi.impl.websockets.server.JakartaWebSocketServer;
import org.osgi.impl.websockets.server.EndpointHandler;

import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Compliance tests for Session API.
 * 
 * Adapted from Jakarta WebSocket 2.2 TCK:
 * - com.sun.ts.tests.websocket.ee.jakarta.websocket.session.WSTestServer
 * - Specification: Jakarta WebSocket 2.2, Section 2.5 (Session)
 * 
 * Tests the following Session API methods:
 * - getId() - Unique session identifier
 * - isOpen() - Session state checking
 * - close() - Session closure
 * - close(CloseReason) - Session closure with reason
 * - getBasicRemote() - Basic remote endpoint access
 * - getRequestURI() - Request URI access
 * - getProtocolVersion() - WebSocket protocol version
 * - getMaxIdleTimeout() / setMaxIdleTimeout() - Timeout configuration
 * - getMaxBinaryMessageBufferSize() / setMaxBinaryMessageBufferSize() - Buffer size
 * - getMaxTextMessageBufferSize() / setMaxTextMessageBufferSize() - Buffer size
 */
public class SessionAPITest {
    
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
    
    /**
     * Test Session.isOpen() returns true during active connection
     * 
     * TCK Reference: isOpenTest
     * Specification: Section 2.5
     */
    @Test
    public void testIsOpenDuringConnection() throws Exception {
        server.createEndpoint(IsOpenEndpoint.class, "/isopen", createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/isopen"), 
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
        
        assertEquals("open:true", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test Session.getId() returns a non-null unique identifier
     * 
     * TCK Reference: getId1Test
     * Specification: Section 2.5
     */
    @Test
    public void testGetId() throws Exception {
        server.createEndpoint(GetIdEndpoint.class, "/getid", createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/getid"), 
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
        
        ws.sendText("getid", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertTrue(response.startsWith("id:"));
        String id = response.substring(3);
        assertNotNull(id);
        assertFalse(id.isEmpty());
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test Session.close() closes the session
     * 
     * TCK Reference: close1Test
     * Specification: Section 2.5
     */
    @Test
    public void testClose() throws Exception {
        server.createEndpoint(CloseEndpoint.class, "/close", createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<Boolean> closeFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/close"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, 
                                                      int statusCode, 
                                                      String reason) {
                        closeFuture.complete(true);
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        ws.sendText("close", true);
        Boolean closed = closeFuture.get(5, TimeUnit.SECONDS);
        
        assertTrue(closed);
    }
    
    /**
     * Test Session.close(CloseReason) closes with specified reason
     * 
     * TCK Reference: close2Test
     * Specification: Section 2.5
     */
    @Test
    public void testCloseWithReason() throws Exception {
        server.createEndpoint(CloseWithReasonEndpoint.class, "/closereason", createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<Integer> codeCapture = new CompletableFuture<>();
        CompletableFuture<String> reasonCapture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/closereason"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, 
                                                      int statusCode, 
                                                      String reason) {
                        codeCapture.complete(statusCode);
                        reasonCapture.complete(reason);
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        ws.sendText("closereason", true);
        Integer code = codeCapture.get(5, TimeUnit.SECONDS);
        String reason = reasonCapture.get(5, TimeUnit.SECONDS);
        
        assertEquals(1009, code); // TOO_BIG
        assertEquals("Test reason", reason);
    }
    
    /**
     * Test Session.getRequestURI() returns the request URI
     * 
     * TCK Reference: getRequestURITest
     * Specification: Section 2.5
     */
    @Test
    public void testGetRequestURI() throws Exception {
        server.createEndpoint(GetRequestURIEndpoint.class, "/requesturi", createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/requesturi?param=value"), 
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
        
        ws.sendText("geturi", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertTrue(response.contains("/requesturi"));
        assertTrue(response.contains("ws://"));
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test Session.getProtocolVersion() returns WebSocket protocol version
     * 
     * TCK Reference: getProtocolVersionTest
     * Specification: Section 2.5
     */
    @Test
    public void testGetProtocolVersion() throws Exception {
        server.createEndpoint(GetProtocolVersionEndpoint.class, "/protocol", createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/protocol"), 
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
        
        ws.sendText("version", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("version:13", response); // RFC 6455 uses version 13
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test Session.getMaxIdleTimeout() and setMaxIdleTimeout()
     * 
     * TCK Reference: setTimeout1Test, setTimeout2Test
     * Specification: Section 2.5
     */
    @Test
    public void testMaxIdleTimeout() throws Exception {
        server.createEndpoint(MaxIdleTimeoutEndpoint.class, "/timeout", createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/timeout"), 
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
        
        ws.sendText("timeout", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("timeout:5000", response); // Should be set to 5000ms
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test Session.getMaxBinaryMessageBufferSize() and setMaxBinaryMessageBufferSize()
     * 
     * TCK Reference: setMaxBinaryMessageBufferSizeTest
     * Specification: Section 2.5
     */
    @Test
    public void testMaxBinaryMessageBufferSize() throws Exception {
        server.createEndpoint(MaxBinaryBufferEndpoint.class, "/binarybuffer", createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/binarybuffer"), 
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
        
        ws.sendText("binarybuffer", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("binarybuffer:16384", response); // Should be set to 16384
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test Session.getMaxTextMessageBufferSize() and setMaxTextMessageBufferSize()
     * 
     * TCK Reference: setMaxTextMessageBufferSizeTest
     * Specification: Section 2.5
     */
    @Test
    public void testMaxTextMessageBufferSize() throws Exception {
        server.createEndpoint(MaxTextBufferEndpoint.class, "/textbuffer", createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/textbuffer"), 
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
        
        ws.sendText("textbuffer", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("textbuffer:32768", response); // Should be set to 32768
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test Session.getBasicRemote() for sending messages
     * 
     * TCK Reference: Multiple tests using getBasicRemote()
     * Specification: Section 2.5
     */
    @Test
    public void testGetBasicRemote() throws Exception {
        server.createEndpoint(GetBasicRemoteEndpoint.class, "/basicremote", createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/basicremote"), 
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
        
        ws.sendText("echo", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("echo:received", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    // ===== Test Endpoint Implementations =====
    
    @ServerEndpoint("/isopen")
    public static class IsOpenEndpoint {
        @OnMessage
        public String onMessage(String message, Session session) {
            return "open:" + session.isOpen();
        }
    }
    
    @ServerEndpoint("/getid")
    public static class GetIdEndpoint {
        @OnMessage
        public String onMessage(String message, Session session) {
            return "id:" + session.getId();
        }
    }
    
    @ServerEndpoint("/close")
    public static class CloseEndpoint {
        @OnMessage
        public void onMessage(String message, Session session) {
            try {
                session.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @ServerEndpoint("/closereason")
    public static class CloseWithReasonEndpoint {
        @OnMessage
        public void onMessage(String message, Session session) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.TOO_BIG, "Test reason"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @ServerEndpoint("/requesturi")
    public static class GetRequestURIEndpoint {
        @OnMessage
        public String onMessage(String message, Session session) {
            URI uri = session.getRequestURI();
            return uri.toString();
        }
    }
    
    @ServerEndpoint("/protocol")
    public static class GetProtocolVersionEndpoint {
        @OnMessage
        public String onMessage(String message, Session session) {
            return "version:" + session.getProtocolVersion();
        }
    }
    
    @ServerEndpoint("/timeout")
    public static class MaxIdleTimeoutEndpoint {
        @OnMessage
        public String onMessage(String message, Session session) {
            session.setMaxIdleTimeout(5000);
            return "timeout:" + session.getMaxIdleTimeout();
        }
    }
    
    @ServerEndpoint("/binarybuffer")
    public static class MaxBinaryBufferEndpoint {
        @OnMessage
        public String onMessage(String message, Session session) {
            session.setMaxBinaryMessageBufferSize(16384);
            return "binarybuffer:" + session.getMaxBinaryMessageBufferSize();
        }
    }
    
    @ServerEndpoint("/textbuffer")
    public static class MaxTextBufferEndpoint {
        @OnMessage
        public String onMessage(String message, Session session) {
            session.setMaxTextMessageBufferSize(32768);
            return "textbuffer:" + session.getMaxTextMessageBufferSize();
        }
    }
    
    @ServerEndpoint("/basicremote")
    public static class GetBasicRemoteEndpoint {
        @OnMessage
        public void onMessage(String message, Session session) {
            try {
                session.getBasicRemote().sendText("echo:received");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
