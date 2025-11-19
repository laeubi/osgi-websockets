package org.osgi.impl.websockets.server;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.*;

/**
 * Test query string handling in WebSocket URIs.
 * 
 * According to RFC 6455 Section 3:
 * - ws-URI = "ws:" "//" host [ ":" port ] path [ "?" query ]
 * - The "resource-name" includes both path and query components
 * 
 * According to Jakarta WebSocket 2.2 Specification Issue 228:
 * - Session.getRequestURI() should return the full URI including query string
 * - Session.getQueryString() should extract the query component
 */
public class QueryStringTest {
    
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
     * Test that Session.getQueryString() returns the query part of the URI
     */
    @Test
    public void testGetQueryString() throws Exception {
        server.createEndpoint(QueryStringEndpoint.class, "/test", createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/test?param1=value1&param2=value2"), 
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
        
        ws.sendText("getQueryString", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        System.out.println("Query string response: " + response);
        
        // According to the spec, getQueryString() should return the query component
        assertEquals("param1=value1&param2=value2", response, 
            "Query string should be 'param1=value1&param2=value2'");
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test that Session.getRequestURI() returns the full URI including query string
     */
    @Test
    public void testGetRequestURIWithQuery() throws Exception {
        server.createEndpoint(RequestURIEndpoint.class, "/test", createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/test?foo=bar&baz=qux"), 
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
        
        ws.sendText("getRequestURI", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        System.out.println("Request URI response: " + response);
        
        // According to Jakarta WebSocket 2.2 Issue 228, the full URI should be returned
        assertTrue(response.contains("?foo=bar&baz=qux"), 
            "Request URI should include query string '?foo=bar&baz=qux'");
        assertTrue(response.contains("/test"), 
            "Request URI should include path '/test'");
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test query string with no parameters (empty query string)
     */
    @Test
    public void testEmptyQueryString() throws Exception {
        server.createEndpoint(QueryStringEndpoint.class, "/test", createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/test"), 
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
        
        ws.sendText("getQueryString", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        System.out.println("Empty query string response: " + response);
        
        // When there's no query string, getQueryString() should return null
        assertEquals("null", response, 
            "Query string should be null when not present");
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test special characters in query string
     */
    @Test
    public void testQueryStringWithSpecialCharacters() throws Exception {
        server.createEndpoint(QueryStringEndpoint.class, "/test", createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        // URL-encoded query string with special characters
        String queryString = "name=John%20Doe&email=john%40example.com";
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/test?" + queryString), 
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
        
        ws.sendText("getQueryString", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        System.out.println("Special chars query string response: " + response);
        
        // The query string should be returned as-is (URL-encoded)
        assertEquals(queryString, response, 
            "Query string should preserve URL encoding");
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    // ===== Test Endpoint Implementations =====
    
    @ServerEndpoint("/test")
    public static class QueryStringEndpoint {
        @OnMessage
        public String onMessage(String message, Session session) {
            String queryString = session.getQueryString();
            return queryString == null ? "null" : queryString;
        }
    }
    
    @ServerEndpoint("/test")
    public static class RequestURIEndpoint {
        @OnMessage
        public String onMessage(String message, Session session) {
            URI uri = session.getRequestURI();
            return uri.toString();
        }
    }
}
