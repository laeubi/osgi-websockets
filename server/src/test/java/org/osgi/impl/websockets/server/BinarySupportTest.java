package org.osgi.impl.websockets.server;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for binary frame support in WebSocket endpoints.
 */
public class BinarySupportTest {
    
    private JakartaWebSocketServer server;
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 8892;
    
    private EndpointHandler<BinaryEndpoint> createSimpleHandler() {
        return new EndpointHandler<BinaryEndpoint>() {
            @Override
            public BinaryEndpoint createEndpointInstance(Class<BinaryEndpoint> endpointClass) throws InstantiationException {
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed to create instance: " + e.getMessage());
                }
            }
            
            @Override
            public void sessionEnded(BinaryEndpoint endpointInstance) {
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
    public void testBinaryEcho() throws Exception {
        server.createEndpoint(BinaryEndpoint.class, null, createSimpleHandler());
        
        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<ByteBuffer> receivedData = new AtomicReference<>();
        
        HttpClient client = HttpClient.newHttpClient();
        
        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                receivedData.set(data);
                messageLatch.countDown();
                return CompletableFuture.completedFuture(null);
            }
        };
        
        URI serverUri = URI.create("ws://" + HOSTNAME + ":" + PORT + "/binary-test");
        WebSocket webSocket = client.newWebSocketBuilder()
            .buildAsync(serverUri, listener)
            .join();
        
        // Send binary data
        byte[] testData = new byte[] {1, 2, 3, 4, 5};
        ByteBuffer sendBuffer = ByteBuffer.wrap(testData);
        webSocket.sendBinary(sendBuffer, true).join();
        
        boolean received = messageLatch.await(5, TimeUnit.SECONDS);
        assertTrue(received, "Should receive binary response within timeout");
        
        ByteBuffer response = receivedData.get();
        assertNotNull(response, "Should receive binary data");
        
        byte[] responseData = new byte[response.remaining()];
        response.get(responseData);
        assertArrayEquals(testData, responseData, "Echoed data should match sent data");
        
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
    }
    
    @Test
    public void testBinaryLargeData() throws Exception {
        server.createEndpoint(BinaryEndpoint.class, null, createSimpleHandler());
        
        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<ByteBuffer> receivedData = new AtomicReference<>();
        
        HttpClient client = HttpClient.newHttpClient();
        
        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                receivedData.set(data);
                messageLatch.countDown();
                return CompletableFuture.completedFuture(null);
            }
        };
        
        URI serverUri = URI.create("ws://" + HOSTNAME + ":" + PORT + "/binary-test");
        WebSocket webSocket = client.newWebSocketBuilder()
            .buildAsync(serverUri, listener)
            .join();
        
        // Send larger binary data (1KB)
        byte[] testData = new byte[1024];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = (byte) (i % 256);
        }
        ByteBuffer sendBuffer = ByteBuffer.wrap(testData);
        webSocket.sendBinary(sendBuffer, true).join();
        
        boolean received = messageLatch.await(5, TimeUnit.SECONDS);
        assertTrue(received, "Should receive binary response within timeout");
        
        ByteBuffer response = receivedData.get();
        assertNotNull(response, "Should receive binary data");
        
        byte[] responseData = new byte[response.remaining()];
        response.get(responseData);
        assertArrayEquals(testData, responseData, "Echoed data should match sent data");
        
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
    }
    
    @Test
    public void testBinaryEmptyData() throws Exception {
        server.createEndpoint(BinaryEndpoint.class, null, createSimpleHandler());
        
        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<ByteBuffer> receivedData = new AtomicReference<>();
        
        HttpClient client = HttpClient.newHttpClient();
        
        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                receivedData.set(data);
                messageLatch.countDown();
                return CompletableFuture.completedFuture(null);
            }
        };
        
        URI serverUri = URI.create("ws://" + HOSTNAME + ":" + PORT + "/binary-test");
        WebSocket webSocket = client.newWebSocketBuilder()
            .buildAsync(serverUri, listener)
            .join();
        
        // Send empty binary data
        byte[] testData = new byte[0];
        ByteBuffer sendBuffer = ByteBuffer.wrap(testData);
        webSocket.sendBinary(sendBuffer, true).join();
        
        boolean received = messageLatch.await(5, TimeUnit.SECONDS);
        assertTrue(received, "Should receive binary response within timeout");
        
        ByteBuffer response = receivedData.get();
        assertNotNull(response, "Should receive binary data");
        
        byte[] responseData = new byte[response.remaining()];
        response.get(responseData);
        assertArrayEquals(testData, responseData, "Echoed empty data should match");
        
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
    }
}
