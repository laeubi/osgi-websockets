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

import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * Tests for query string support in WebSocket sessions.
 */
public class QueryStringTest {
    
    private JakartaWebSocketServer server;
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 8895;
    
    private EndpointHandler<QueryStringEndpoint> createSimpleHandler() {
        return new EndpointHandler<QueryStringEndpoint>() {
            @Override
            public QueryStringEndpoint createEndpointInstance(Class<QueryStringEndpoint> endpointClass) throws InstantiationException {
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed to create instance: " + e.getMessage());
                }
            }
            
            @Override
            public void sessionEnded(QueryStringEndpoint endpointInstance) {
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
    public void testQueryStringIsAvailable() throws Exception {
        server.createEndpoint(QueryStringEndpoint.class, null, createSimpleHandler());
        
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
        
        URI serverUri = URI.create("ws://" + HOSTNAME + ":" + PORT + "/query-test?name=value&foo=bar");
        WebSocket webSocket = client.newWebSocketBuilder()
            .buildAsync(serverUri, listener)
            .join();
        
        // Request the query string
        webSocket.sendText("getquery", true).join();
        
        boolean received = messageLatch.await(5, TimeUnit.SECONDS);
        assertTrue(received, "Should receive response within timeout");
        
        String response = receivedMessage.get();
        assertEquals("query:name=value&foo=bar", response, 
            "Query string should be available via Session.getQueryString()");
        
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
    }
    
    @ServerEndpoint("/query-test")
    public static class QueryStringEndpoint {
        @OnMessage
        public String handleMessage(String message, Session session) {
            if (message.equals("getquery")) {
                String queryString = session.getQueryString();
                return "query:" + (queryString != null ? queryString : "null");
            }
            return "Echo: " + message;
        }
    }
}
