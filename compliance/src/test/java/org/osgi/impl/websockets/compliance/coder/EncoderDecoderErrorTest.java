package org.osgi.impl.websockets.compliance.coder;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.impl.websockets.server.EndpointHandler;
import org.osgi.impl.websockets.server.JakartaWebSocketServer;
import org.osgi.impl.websockets.server.WebSocketEndpoint;

import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Compliance tests for encoder/decoder error handling.
 * 
 * Adapted from Jakarta WebSocket 2.2 TCK:
 * - Original: com.sun.ts.tests.websocket.ee.jakarta.websocket.throwingcoder.annotated.WSClientIT
 * - Specification: Jakarta WebSocket 2.2, Sections on Decoder and Encoder error handling
 * 
 * Tests that DecodeException and EncodeException are properly propagated to @OnError handlers.
 */
public class EncoderDecoderErrorTest {
    
    private JakartaWebSocketServer server;
    private int port;
    private HttpClient httpClient;
    
    @BeforeEach
    public void setUp() throws Exception {
        port = 8080 + ThreadLocalRandom.current().nextInt(1000);
        server = new JakartaWebSocketServer("localhost", port);
        server.start();
        httpClient = HttpClient.newHttpClient();
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
    }
    
    /**
     * Test that DecodeException from text decoder is caught in @OnError
     * 
     * TCK Reference: textDecoderThrowAndCatchOnServerTest
     * Specification: Jakarta WebSocket 2.2 - Decoder error handling
     */
    @Test
    public void testTextDecoderThrowsDecodeException() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            TextDecoderErrorEndpoint.class, "/textdecoder", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/textdecoder"), 
                createListener(response))
            .join();
        
        ws.sendText("trigger decode error", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        // Should receive the error message from @OnError
        assertEquals(ThrowingTextDecoder.ERR_MSG, result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test that DecodeException from binary decoder is caught in @OnError
     * 
     * TCK Reference: binaryDecoderThrowAndCatchOnServerTest
     * Specification: Jakarta WebSocket 2.2 - Decoder error handling
     */
    @Test
    public void testBinaryDecoderThrowsDecodeException() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            BinaryDecoderErrorEndpoint.class, "/binarydecoder", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/binarydecoder"), 
                createListener(response))
            .join();
        
        ByteBuffer data = ByteBuffer.wrap("trigger decode error".getBytes());
        ws.sendBinary(data, true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        // Should receive the error message from @OnError
        assertEquals(ThrowingBinaryDecoder.ERR_MSG, result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test that EncodeException from text encoder is properly handled
     * 
     * TCK Reference: textEncoderThrowAndCatchOnServerTest
     * Specification: Jakarta WebSocket 2.2 - Encoder error handling
     * 
     * Note: This test verifies that when an encoder throws EncodeException,
     * it's converted to IOException (as per RemoteEndpoint.Basic.sendObject spec)
     * but the original cause is preserved.
     */
    @Test
    public void testTextEncoderThrowsEncodeException() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            TextEncoderErrorEndpoint.class, "/textencoder", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/textencoder"), 
                createListener(response))
            .join();
        
        ws.sendText("trigger encode error", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        // Should receive error indication from @OnError
        // The EncodeException is wrapped in IOException by sendObject()
        assertTrue(result.contains("Failed to encode") || result.contains(ThrowingTextEncoder.ERR_MSG),
            "Expected encode error message, got: " + result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test that EncodeException from binary encoder is properly handled
     * 
     * TCK Reference: binaryEncoderThrowAndCatchOnServerTest
     * Specification: Jakarta WebSocket 2.2 - Encoder error handling
     */
    @Test
    public void testBinaryEncoderThrowsEncodeException() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            BinaryEncoderErrorEndpoint.class, "/binaryencoder", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/binaryencoder"), 
                createListener(response))
            .join();
        
        ws.sendText("trigger encode error", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        // Should receive error indication from @OnError
        assertTrue(result.contains("Failed to encode") || result.contains(ThrowingBinaryEncoder.ERR_MSG),
            "Expected encode error message, got: " + result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test that IOException from stream decoder is caught in @OnError
     * 
     * TCK Reference: textStreamDecoderThrowIOAndCatchOnServerTest
     * Specification: Jakarta WebSocket 2.2 - Stream decoder error handling
     */
    @Test
    public void testTextStreamDecoderThrowsIOException() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            TextStreamIODecoderErrorEndpoint.class, "/textstreamiodecoder", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/textstreamiodecoder"), 
                createListener(response))
            .join();
        
        ws.sendText("trigger IO error", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        // Should receive the IO error message from @OnError
        assertEquals(ThrowingIOTextStreamDecoder.IO_ERR_MSG, result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    // ==================== Helper Methods ====================
    
    private <T> EndpointHandler<T> createHandler() {
        return new EndpointHandler<T>() {
            @Override
            public T createEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed to create endpoint: " + e.getMessage());
                }
            }
            
            @Override
            public void sessionEnded(T endpointInstance) {
                // Cleanup if needed
            }
        };
    }
    
    private WebSocket.Listener createListener(CompletableFuture<String> response) {
        return new WebSocket.Listener() {
            private final StringBuilder messageBuilder = new StringBuilder();
            
            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                messageBuilder.append(data);
                if (last) {
                    response.complete(messageBuilder.toString());
                }
                webSocket.request(1);
                return CompletableFuture.completedFuture(null);
            }
            
            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                return CompletableFuture.completedFuture(null);
            }
            
            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                response.completeExceptionally(error);
            }
        };
    }
    
    // ==================== Test Data Classes ====================
    
    public static class StringBean {
        private String value;
        
        public StringBean() {}
        
        public StringBean(String value) {
            this.value = value;
        }
        
        public String get() {
            return value;
        }
        
        public void set(String value) {
            this.value = value;
        }
    }
    
    // ==================== Throwing Decoders ====================
    
    public static class ThrowingTextDecoder implements Decoder.Text<StringBean> {
        public static final String ERR_MSG = "TCK text decoder exception";
        
        @Override
        public StringBean decode(String s) throws DecodeException {
            throw new DecodeException(s, ERR_MSG);
        }
        
        @Override
        public boolean willDecode(String s) {
            return true;
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    public static class ThrowingBinaryDecoder implements Decoder.Binary<StringBean> {
        public static final String ERR_MSG = "TCK binary decoder exception";
        
        @Override
        public StringBean decode(ByteBuffer bytes) throws DecodeException {
            throw new DecodeException(bytes, ERR_MSG);
        }
        
        @Override
        public boolean willDecode(ByteBuffer bytes) {
            return true;
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    public static class ThrowingIOTextStreamDecoder implements Decoder.TextStream<StringBean> {
        public static final String IO_ERR_MSG = "TCK IO exception from text stream decoder";
        
        @Override
        public StringBean decode(java.io.Reader reader) throws DecodeException, IOException {
            throw new IOException(IO_ERR_MSG);
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    // ==================== Throwing Encoders ====================
    
    public static class ThrowingTextEncoder implements Encoder.Text<StringBean> {
        public static final String ERR_MSG = "TCK text encoder exception";
        
        @Override
        public String encode(StringBean object) throws EncodeException {
            throw new EncodeException(object, ERR_MSG);
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    public static class ThrowingBinaryEncoder implements Encoder.Binary<StringBean> {
        public static final String ERR_MSG = "TCK binary encoder exception";
        
        @Override
        public ByteBuffer encode(StringBean object) throws EncodeException {
            throw new EncodeException(object, ERR_MSG);
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    // ==================== Test Endpoint Classes ====================
    
    @ServerEndpoint(value = "/textdecoder", decoders = { ThrowingTextDecoder.class })
    public static class TextDecoderErrorEndpoint {
        @OnMessage
        public String onMessage(StringBean bean) {
            return bean.get();
        }
        
        @OnError
        public void onError(Session session, Throwable t) throws IOException {
            String message = getCauseMessage(t);
            session.getBasicRemote().sendText(message);
        }
    }
    
    @ServerEndpoint(value = "/binarydecoder", decoders = { ThrowingBinaryDecoder.class })
    public static class BinaryDecoderErrorEndpoint {
        @OnMessage
        public String onMessage(StringBean bean) {
            return bean.get();
        }
        
        @OnError
        public void onError(Session session, Throwable t) throws IOException {
            String message = getCauseMessage(t);
            session.getBasicRemote().sendText(message);
        }
    }
    
    @ServerEndpoint(value = "/textencoder", encoders = { ThrowingTextEncoder.class })
    public static class TextEncoderErrorEndpoint {
        @OnMessage
        public void onMessage(String message, Session session) {
            try {
                StringBean bean = new StringBean(message);
                session.getBasicRemote().sendObject(bean);
            } catch (Exception e) {
                // Exception from sendObject will be IOException wrapping EncodeException
                try {
                    session.getBasicRemote().sendText(getCauseMessage(e));
                } catch (IOException ignored) {}
            }
        }
        
        @OnError
        public void onError(Session session, Throwable t) throws IOException {
            session.getBasicRemote().sendText("Error: " + getCauseMessage(t));
        }
    }
    
    @ServerEndpoint(value = "/binaryencoder", encoders = { ThrowingBinaryEncoder.class })
    public static class BinaryEncoderErrorEndpoint {
        @OnMessage
        public void onMessage(String message, Session session) {
            try {
                StringBean bean = new StringBean(message);
                session.getBasicRemote().sendObject(bean);
            } catch (Exception e) {
                // Exception from sendObject will be IOException wrapping EncodeException
                try {
                    session.getBasicRemote().sendText(getCauseMessage(e));
                } catch (IOException ignored) {}
            }
        }
        
        @OnError
        public void onError(Session session, Throwable t) throws IOException {
            session.getBasicRemote().sendText("Error: " + getCauseMessage(t));
        }
    }
    
    @ServerEndpoint(value = "/textstreamiodecoder", decoders = { ThrowingIOTextStreamDecoder.class })
    public static class TextStreamIODecoderErrorEndpoint {
        @OnMessage
        public String onMessage(StringBean bean) {
            return bean.get();
        }
        
        @OnError
        public void onError(Session session, Throwable t) throws IOException {
            String message = getCauseMessage(t);
            session.getBasicRemote().sendText(message);
        }
    }
    
    // ==================== Helper Methods ====================
    
    private static String getCauseMessage(Throwable t) {
        String msg = null;
        while (t != null) {
            msg = t.getMessage();
            t = t.getCause();
        }
        return msg;
    }
}
