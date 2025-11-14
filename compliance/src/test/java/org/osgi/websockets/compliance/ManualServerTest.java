package org.osgi.websockets.compliance;

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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Manual test demonstrating running WebSocket tests against our server.
 * This serves as a proof of concept for TCK integration.
 */
public class ManualServerTest {

    private JakartaWebSocketServer server;
    private static final int TEST_PORT = 9876;
    private static final String TEST_HOST = "localhost";

    @ServerEndpoint("/echo")
    public static class EchoEndpoint {
        @OnOpen
        public void onOpen(Session session) {
            System.out.println("Session opened: " + session.getId());
        }

        @OnMessage
        public String onMessage(String message, Session session) {
            return "Echo: " + message;
        }
    }

    @BeforeEach
    public void startServer() throws Exception {
        server = new JakartaWebSocketServer(TEST_HOST, TEST_PORT);
        server.start();
        
        // Register test endpoint
        EndpointHandler<EchoEndpoint> handler = new EndpointHandler<EchoEndpoint>() {
            @Override
            public EchoEndpoint createEndpointInstance(Class<EchoEndpoint> endpointClass) throws InstantiationException {
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed to create endpoint: " + e.getMessage());
                }
            }

            @Override
            public void sessionEnded(EchoEndpoint endpointInstance) {
                // Cleanup if needed
            }
        };
        
        server.createEndpoint(EchoEndpoint.class, "/echo", handler);
    }

    @AfterEach
    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testBasicConnection() throws Exception {
        CountDownLatch messageLatch = new CountDownLatch(1);
        CompletableFuture<String> messageReceived = new CompletableFuture<>();

        HttpClient client = HttpClient.newHttpClient();
        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                messageReceived.complete(data.toString());
                messageLatch.countDown();
                return WebSocket.Listener.super.onText(webSocket, data, last);
            }
        };

        WebSocket ws = client.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .buildAsync(URI.create("ws://" + TEST_HOST + ":" + TEST_PORT + "/echo"), listener)
                .join();

        ws.sendText("Hello", true);

        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Message not received in time");
        String response = messageReceived.get(5, TimeUnit.SECONDS);
        assertEquals("Echo: Hello", response);

        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }

    @Test
    public void testMultipleMessages() throws Exception {
        CountDownLatch messageLatch = new CountDownLatch(3);
        CompletableFuture<String> message1 = new CompletableFuture<>();
        CompletableFuture<String> message2 = new CompletableFuture<>();
        CompletableFuture<String> message3 = new CompletableFuture<>();

        HttpClient client = HttpClient.newHttpClient();
        WebSocket.Listener listener = new WebSocket.Listener() {
            private int count = 0;

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                count++;
                if (count == 1) message1.complete(data.toString());
                if (count == 2) message2.complete(data.toString());
                if (count == 3) message3.complete(data.toString());
                messageLatch.countDown();
                return WebSocket.Listener.super.onText(webSocket, data, last);
            }
        };

        WebSocket ws = client.newWebSocketBuilder()
                .buildAsync(URI.create("ws://" + TEST_HOST + ":" + TEST_PORT + "/echo"), listener)
                .join();

        ws.sendText("First", true);
        ws.sendText("Second", true);
        ws.sendText("Third", true);

        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Messages not received in time");
        assertEquals("Echo: First", message1.get());
        assertEquals("Echo: Second", message2.get());
        assertEquals("Echo: Third", message3.get());

        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
}
