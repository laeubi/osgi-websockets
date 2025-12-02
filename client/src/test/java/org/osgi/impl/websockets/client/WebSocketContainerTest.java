package org.osgi.impl.websockets.client;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.osgi.impl.websockets.server.EndpointHandler;
import org.osgi.impl.websockets.server.JakartaWebSocketServer;
import org.osgi.impl.websockets.server.WebSocketEndpoint;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the JakartaWebSocketContainer client implementation.
 */
public class WebSocketContainerTest {
    
    private JakartaWebSocketServer server;
    private JakartaWebSocketContainer container;
    private int port;
    
    @BeforeEach
    public void setUp() throws Exception {
        port = 8080 + (int) (Math.random() * 1000);
        server = new JakartaWebSocketServer("localhost", port);
        server.start();
        container = new JakartaWebSocketContainer();
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
    }
    
    // ============= Server Endpoints =============
    
    @ServerEndpoint("/echo")
    public static class EchoServerEndpoint {
        @OnMessage
        public String onMessage(String message) {
            return "Echo: " + message;
        }
    }
    
    @ServerEndpoint("/reverse")
    public static class ReverseServerEndpoint {
        @OnMessage
        public String onMessage(String message) {
            return new StringBuilder(message).reverse().toString();
        }
    }
    
    // ============= Client Endpoints =============
    
    @ClientEndpoint
    public static class SimpleClientEndpoint {
        public final CompletableFuture<String> messageFuture = new CompletableFuture<>();
        public final CountDownLatch openLatch = new CountDownLatch(1);
        public final CountDownLatch closeLatch = new CountDownLatch(1);
        
        @OnOpen
        public void onOpen(Session session) {
            openLatch.countDown();
        }
        
        @OnMessage
        public void onMessage(String message) {
            messageFuture.complete(message);
        }
        
        @OnClose
        public void onClose(Session session, CloseReason closeReason) {
            closeLatch.countDown();
        }
    }
    
    @ClientEndpoint
    public static class ErrorHandlingClientEndpoint {
        public final AtomicReference<Throwable> errorRef = new AtomicReference<>();
        public final CountDownLatch errorLatch = new CountDownLatch(1);
        
        @OnMessage
        public void onMessage(String message) {
            // Just receive messages
        }
        
        @OnError
        public void onError(Session session, Throwable error) {
            errorRef.set(error);
            errorLatch.countDown();
        }
    }
    
    // ============= Tests =============
    
    @Test
    public void testConnectToServerWithInstance() throws Exception {
        // Register server endpoint
        WebSocketEndpoint endpoint = server.createEndpoint(
            EchoServerEndpoint.class, "/echo", createHandler());
        
        try {
            // Create client and connect
            SimpleClientEndpoint clientEndpoint = new SimpleClientEndpoint();
            Session session = container.connectToServer(
                clientEndpoint, 
                new URI("ws://localhost:" + port + "/echo"));
            
            assertNotNull(session);
            assertTrue(session.isOpen());
            
            // Wait for OnOpen to be called
            assertTrue(clientEndpoint.openLatch.await(5, TimeUnit.SECONDS), "OnOpen not called");
            
            // Send message and wait for response
            session.getBasicRemote().sendText("Hello");
            String response = clientEndpoint.messageFuture.get(5, TimeUnit.SECONDS);
            assertEquals("Echo: Hello", response);
            
            // Close session
            session.close();
            assertTrue(clientEndpoint.closeLatch.await(5, TimeUnit.SECONDS), "OnClose not called");
            
        } finally {
            endpoint.dispose();
        }
    }
    
    @Test
    public void testConnectToServerWithClass() throws Exception {
        // Register server endpoint
        WebSocketEndpoint endpoint = server.createEndpoint(
            EchoServerEndpoint.class, "/echo", createHandler());
        
        try {
            // Connect using class
            Session session = container.connectToServer(
                SimpleClientEndpoint.class, 
                new URI("ws://localhost:" + port + "/echo"));
            
            assertNotNull(session);
            assertTrue(session.isOpen());
            
            // Wait a bit for connection to stabilize
            Thread.sleep(100);
            
            // Close session
            session.close();
            
        } finally {
            endpoint.dispose();
        }
    }
    
    @Test
    public void testSendAndReceiveMultipleMessages() throws Exception {
        // Register server endpoint
        WebSocketEndpoint endpoint = server.createEndpoint(
            ReverseServerEndpoint.class, "/reverse", createHandler());
        
        try {
            // Create client and connect
            @ClientEndpoint
            class MultiMessageClient {
                final CompletableFuture<String> msg1 = new CompletableFuture<>();
                final CompletableFuture<String> msg2 = new CompletableFuture<>();
                final CompletableFuture<String> msg3 = new CompletableFuture<>();
                int messageCount = 0;
                
                @OnMessage
                public void onMessage(String message) {
                    messageCount++;
                    switch (messageCount) {
                        case 1: msg1.complete(message); break;
                        case 2: msg2.complete(message); break;
                        case 3: msg3.complete(message); break;
                    }
                }
            }
            
            MultiMessageClient clientEndpoint = new MultiMessageClient();
            Session session = container.connectToServer(
                clientEndpoint, 
                new URI("ws://localhost:" + port + "/reverse"));
            
            // Send multiple messages
            session.getBasicRemote().sendText("Hello");
            session.getBasicRemote().sendText("World");
            session.getBasicRemote().sendText("Test");
            
            // Verify responses
            assertEquals("olleH", clientEndpoint.msg1.get(5, TimeUnit.SECONDS));
            assertEquals("dlroW", clientEndpoint.msg2.get(5, TimeUnit.SECONDS));
            assertEquals("tseT", clientEndpoint.msg3.get(5, TimeUnit.SECONDS));
            
            session.close();
            
        } finally {
            endpoint.dispose();
        }
    }
    
    @Test
    public void testAsyncSend() throws Exception {
        // Register server endpoint
        WebSocketEndpoint endpoint = server.createEndpoint(
            EchoServerEndpoint.class, "/echo", createHandler());
        
        try {
            // Create client and connect
            SimpleClientEndpoint clientEndpoint = new SimpleClientEndpoint();
            Session session = container.connectToServer(
                clientEndpoint, 
                new URI("ws://localhost:" + port + "/echo"));
            
            // Wait for connection
            assertTrue(clientEndpoint.openLatch.await(5, TimeUnit.SECONDS));
            
            // Send message asynchronously
            session.getAsyncRemote().sendText("Async Hello").get(5, TimeUnit.SECONDS);
            
            // Wait for response
            String response = clientEndpoint.messageFuture.get(5, TimeUnit.SECONDS);
            assertEquals("Echo: Async Hello", response);
            
            session.close();
            
        } finally {
            endpoint.dispose();
        }
    }
    
    @Test
    public void testConnectionTimeout() throws Exception {
        // Set a very short timeout
        container.setConnectToServerTimeout(100);
        
        // Try to connect to a non-existent server
        try {
            container.connectToServer(
                SimpleClientEndpoint.class, 
                new URI("ws://localhost:59999/nonexistent"));
            fail("Should have thrown exception");
        } catch (IOException e) {
            // Expected - connection should timeout or fail
            assertTrue(true);
        }
    }
    
    @Test
    public void testNullEndpointThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            container.connectToServer((Object) null, new URI("ws://localhost:8080/test"));
        });
    }
    
    @Test
    public void testNullUriThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            container.connectToServer(new SimpleClientEndpoint(), null);
        });
    }
    
    @Test
    public void testContainerDefaults() {
        assertEquals(0, container.getDefaultAsyncSendTimeout());
        assertEquals(0, container.getDefaultMaxSessionIdleTimeout());
        assertEquals(8192, container.getDefaultMaxBinaryMessageBufferSize());
        assertEquals(8192, container.getDefaultMaxTextMessageBufferSize());
        assertTrue(container.getInstalledExtensions().isEmpty());
    }
    
    @Test
    public void testContainerSettings() {
        container.setDefaultMaxSessionIdleTimeout(30000);
        container.setDefaultMaxBinaryMessageBufferSize(16384);
        container.setDefaultMaxTextMessageBufferSize(32768);
        
        assertEquals(30000, container.getDefaultMaxSessionIdleTimeout());
        assertEquals(16384, container.getDefaultMaxBinaryMessageBufferSize());
        assertEquals(32768, container.getDefaultMaxTextMessageBufferSize());
    }
    
    @Test
    public void testSessionProperties() throws Exception {
        // Register server endpoint
        WebSocketEndpoint endpoint = server.createEndpoint(
            EchoServerEndpoint.class, "/echo", createHandler());
        
        try {
            SimpleClientEndpoint clientEndpoint = new SimpleClientEndpoint();
            Session session = container.connectToServer(
                clientEndpoint, 
                new URI("ws://localhost:" + port + "/echo?param1=value1&param2=value2"));
            
            // Check session properties
            assertNotNull(session.getId());
            assertEquals("ws://localhost:" + port + "/echo?param1=value1&param2=value2", 
                session.getRequestURI().toString());
            assertEquals("param1=value1&param2=value2", session.getQueryString());
            assertFalse(session.isSecure());
            assertEquals("13", session.getProtocolVersion());
            assertEquals(container, session.getContainer());
            
            // Test user properties
            session.getUserProperties().put("key", "value");
            assertEquals("value", session.getUserProperties().get("key"));
            
            session.close();
            
        } finally {
            endpoint.dispose();
        }
    }
    
    // ============= Helper Methods =============
    
    @SuppressWarnings("unchecked")
    private <T> EndpointHandler<T> createHandler() {
        return new EndpointHandler<T>() {
            @Override
            public T createEndpointInstance(Class<T> endpointClass) throws InstantiationException {
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
}
