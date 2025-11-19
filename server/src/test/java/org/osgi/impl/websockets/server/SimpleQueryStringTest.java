package org.osgi.impl.websockets.server;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.*;

/**
 * Simple test to verify query string handling
 */
public class SimpleQueryStringTest {
    
    @Test
    public void testQueryStringDebug() throws Exception {
        int port = 8080 + ThreadLocalRandom.current().nextInt(1000);
        JakartaWebSocketServer server = new JakartaWebSocketServer("localhost", port);
        server.start();
        
        try {
            server.createEndpoint(DebugEndpoint.class, "/debug", new EndpointHandler<DebugEndpoint>() {
                @Override
                public DebugEndpoint createEndpointInstance(Class<DebugEndpoint> endpointClass) 
                        throws InstantiationException {
                    try {
                        return new DebugEndpoint();
                    } catch (Exception e) {
                        throw new InstantiationException("Failed: " + e.getMessage());
                    }
                }
                
                @Override
                public void sessionEnded(DebugEndpoint endpointInstance) {
                }
            });
            
            HttpClient client = HttpClient.newHttpClient();
            CompletableFuture<String> messageFuture = new CompletableFuture<>();
            CountDownLatch connectedLatch = new CountDownLatch(1);
            
            WebSocket ws = client.newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:" + port + "/debug?test=value123"), 
                    new WebSocket.Listener() {
                        @Override
                        public void onOpen(WebSocket webSocket) {
                            System.out.println("Client WebSocket opened");
                            connectedLatch.countDown();
                            WebSocket.Listener.super.onOpen(webSocket);
                        }
                        
                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, 
                                                         CharSequence data, 
                                                         boolean last) {
                            System.out.println("Client received: " + data);
                            messageFuture.complete(data.toString());
                            return CompletableFuture.completedFuture(null);
                        }
                    })
                .join();
            
            // Wait for connection
            assertTrue(connectedLatch.await(5, TimeUnit.SECONDS), "WebSocket didn't connect");
            
            System.out.println("Sending test message");
            ws.sendText("ping", true);
            
            String response = messageFuture.get(5, TimeUnit.SECONDS);
            System.out.println("Got response: " + response);
            
            // Verify the query string was captured
            assertTrue(response.contains("test=value123"), 
                "Response should contain query string: " + response);
            
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        } finally {
            server.stop();
        }
    }
    
    @ServerEndpoint("/debug")
    public static class DebugEndpoint {
        @OnMessage
        public String onMessage(String message, Session session) {
            System.out.println("Server endpoint received: " + message);
            String queryString = session.getQueryString();
            System.out.println("Query string: " + queryString);
            URI requestURI = session.getRequestURI();
            System.out.println("Request URI: " + requestURI);
            return "query=" + queryString + "|uri=" + requestURI;
        }
    }
}
