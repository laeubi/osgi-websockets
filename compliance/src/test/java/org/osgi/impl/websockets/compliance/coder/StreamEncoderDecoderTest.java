package org.osgi.impl.websockets.compliance.coder;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.osgi.impl.websockets.server.JakartaWebSocketServer;
import org.osgi.impl.websockets.server.EndpointHandler;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.*;

/**
 * Compliance tests for Stream Encoder and Decoder functionality.
 * 
 * Adapted from Jakarta WebSocket 2.2 TCK:
 * - com.sun.ts.tests.websocket.ee.jakarta.websocket.remoteendpoint.usercoder.basic
 * - Specification: Jakarta WebSocket 2.2, Section 4.5 (Encoders and Decoders)
 * 
 * Tests TextStream and BinaryStream encoder/decoder variants.
 */
public class StreamEncoderDecoderTest {
    
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
     * Test TextStream encoder with Boolean type
     * 
     * TCK Reference: WSCTextStreamServer with TextStreamCoderBool
     * Specification: Section 4.5.3 - TextStream encoders
     */
    @Test
    public void testTextStreamEncoderBoolean() throws Exception {
        server.createEndpoint(TextStreamBooleanEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/textstream/bool"), 
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
        
        // Send request for boolean
        ws.sendText("BOOL", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("ENCODED:false", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test TextStream encoder with Integer type
     * 
     * TCK Reference: WSCTextStreamServer with TextStreamCoderInt
     * Specification: Section 4.5.3 - TextStream encoders
     */
    @Test
    public void testTextStreamEncoderInteger() throws Exception {
        server.createEndpoint(TextStreamIntegerEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/textstream/int"), 
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
        
        // Send request for integer
        ws.sendText("INT", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("ENCODED:100", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test TextStream encoder with Long type
     * 
     * TCK Reference: WSCTextStreamServer with TextStreamCoderLong
     * Specification: Section 4.5.3 - TextStream encoders
     */
    @Test
    public void testTextStreamEncoderLong() throws Exception {
        server.createEndpoint(TextStreamLongEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/textstream/long"), 
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
        
        // Send request for long
        ws.sendText("LONG", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("ENCODED:100", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test TextStream decoder with custom object
     * 
     * TCK Reference: WSCTextStreamServer
     * Specification: Section 4.5.3 - TextStream decoders
     */
    @Test
    public void testTextStreamDecoder() throws Exception {
        server.createEndpoint(TextStreamDecoderEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/textstream/decoder"), 
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
        
        // Send message to be decoded
        ws.sendText("Hello Stream", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("DECODED:Hello Stream", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test BinaryStream encoder with Boolean type
     * 
     * TCK Reference: WSCBinaryStreamServer with BinaryStreamCoderBool
     * Specification: Section 4.5.4 - BinaryStream encoders
     */
    @Test
    public void testBinaryStreamEncoderBoolean() throws Exception {
        server.createEndpoint(BinaryStreamBooleanEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<ByteBuffer> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/binarystream/bool"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, 
                                                       ByteBuffer data, 
                                                       boolean last) {
                        messageFuture.complete(data);
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        // Send request for boolean
        ws.sendText("BOOL", true);
        ByteBuffer response = messageFuture.get(5, TimeUnit.SECONDS);
        
        byte[] bytes = new byte[response.remaining()];
        response.get(bytes);
        String responseStr = new String(bytes);
        assertEquals("ENCODED:false", responseStr);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test BinaryStream encoder with Integer type
     * 
     * TCK Reference: WSCBinaryStreamServer with BinaryStreamCoderInt
     * Specification: Section 4.5.4 - BinaryStream encoders
     */
    @Test
    public void testBinaryStreamEncoderInteger() throws Exception {
        server.createEndpoint(BinaryStreamIntegerEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<ByteBuffer> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/binarystream/int"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, 
                                                       ByteBuffer data, 
                                                       boolean last) {
                        messageFuture.complete(data);
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        // Send request for integer
        ws.sendText("INT", true);
        ByteBuffer response = messageFuture.get(5, TimeUnit.SECONDS);
        
        byte[] bytes = new byte[response.remaining()];
        response.get(bytes);
        String responseStr = new String(bytes);
        assertEquals("ENCODED:100", responseStr);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test BinaryStream encoder with Long type
     * 
     * TCK Reference: WSCBinaryStreamServer with BinaryStreamCoderLong
     * Specification: Section 4.5.4 - BinaryStream encoders
     */
    @Test
    public void testBinaryStreamEncoderLong() throws Exception {
        server.createEndpoint(BinaryStreamLongEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<ByteBuffer> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/binarystream/long"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, 
                                                       ByteBuffer data, 
                                                       boolean last) {
                        messageFuture.complete(data);
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        // Send request for long
        ws.sendText("LONG", true);
        ByteBuffer response = messageFuture.get(5, TimeUnit.SECONDS);
        
        byte[] bytes = new byte[response.remaining()];
        response.get(bytes);
        String responseStr = new String(bytes);
        assertEquals("ENCODED:100", responseStr);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test BinaryStream decoder with custom object
     * 
     * TCK Reference: WSCBinaryStreamServer
     * Specification: Section 4.5.4 - BinaryStream decoders
     */
    @Test
    public void testBinaryStreamDecoder() throws Exception {
        server.createEndpoint(BinaryStreamDecoderEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<ByteBuffer> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/binarystream/decoder"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, 
                                                       ByteBuffer data, 
                                                       boolean last) {
                        messageFuture.complete(data);
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        // Send binary message to be decoded
        ByteBuffer data = ByteBuffer.wrap("Binary Stream".getBytes());
        ws.sendBinary(data, true);
        ByteBuffer response = messageFuture.get(5, TimeUnit.SECONDS);
        
        byte[] bytes = new byte[response.remaining()];
        response.get(bytes);
        String responseStr = new String(bytes);
        assertEquals("DECODED:Binary Stream", responseStr);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test combined TextStream encoder and decoder
     * 
     * TCK Reference: Multiple stream coder tests
     * Specification: Section 4.5.3 - TextStream encoders/decoders
     */
    @Test
    public void testTextStreamEncoderDecoder() throws Exception {
        server.createEndpoint(TextStreamBidirectionalEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/textstream/bidir"), 
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
        
        // Send custom message
        ws.sendText("TEST:message", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertTrue(response.contains("PROCESSED"));
        
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
    
    @ServerEndpoint(value = "/textstream/bool", encoders = {TextStreamBooleanEncoder.class})
    public static class TextStreamBooleanEndpoint {
        @OnMessage
        public void processMessage(String msg, Session session) throws IOException, EncodeException {
            if ("BOOL".equals(msg)) {
                session.getBasicRemote().sendObject(false);
            }
        }
    }
    
    @ServerEndpoint(value = "/textstream/int", encoders = {TextStreamIntegerEncoder.class})
    public static class TextStreamIntegerEndpoint {
        @OnMessage
        public void processMessage(String msg, Session session) throws IOException, EncodeException {
            if ("INT".equals(msg)) {
                session.getBasicRemote().sendObject(100);
            }
        }
    }
    
    @ServerEndpoint(value = "/textstream/long", encoders = {TextStreamLongEncoder.class})
    public static class TextStreamLongEndpoint {
        @OnMessage
        public void processMessage(String msg, Session session) throws IOException, EncodeException {
            if ("LONG".equals(msg)) {
                session.getBasicRemote().sendObject(100L);
            }
        }
    }
    
    @ServerEndpoint(value = "/textstream/decoder", 
                    decoders = {TextStreamMessageDecoder.class},
                    encoders = {TextStreamMessageEncoder.class})
    public static class TextStreamDecoderEndpoint {
        @OnMessage
        public void processMessage(StreamMessage message, Session session) throws IOException, EncodeException {
            session.getBasicRemote().sendObject(message);
        }
    }
    
    @ServerEndpoint(value = "/binarystream/bool", encoders = {BinaryStreamBooleanEncoder.class})
    public static class BinaryStreamBooleanEndpoint {
        @OnMessage
        public void processMessage(String msg, Session session) throws IOException, EncodeException {
            if ("BOOL".equals(msg)) {
                session.getBasicRemote().sendObject(false);
            }
        }
    }
    
    @ServerEndpoint(value = "/binarystream/int", encoders = {BinaryStreamIntegerEncoder.class})
    public static class BinaryStreamIntegerEndpoint {
        @OnMessage
        public void processMessage(String msg, Session session) throws IOException, EncodeException {
            if ("INT".equals(msg)) {
                session.getBasicRemote().sendObject(100);
            }
        }
    }
    
    @ServerEndpoint(value = "/binarystream/long", encoders = {BinaryStreamLongEncoder.class})
    public static class BinaryStreamLongEndpoint {
        @OnMessage
        public void processMessage(String msg, Session session) throws IOException, EncodeException {
            if ("LONG".equals(msg)) {
                session.getBasicRemote().sendObject(100L);
            }
        }
    }
    
    @ServerEndpoint(value = "/binarystream/decoder", 
                    decoders = {BinaryStreamMessageDecoder.class},
                    encoders = {BinaryStreamMessageEncoder.class})
    public static class BinaryStreamDecoderEndpoint {
        @OnMessage
        public void processMessage(StreamMessage message, Session session) throws IOException, EncodeException {
            session.getBasicRemote().sendObject(message);
        }
    }
    
    @ServerEndpoint(value = "/textstream/bidir", 
                    decoders = {TextStreamMessageDecoder.class},
                    encoders = {TextStreamMessageEncoder.class})
    public static class TextStreamBidirectionalEndpoint {
        @OnMessage
        public void processMessage(StreamMessage message, Session session) throws IOException, EncodeException {
            message.content = "PROCESSED:" + message.content;
            session.getBasicRemote().sendObject(message);
        }
    }
    
    // Helper class for stream messages
    public static class StreamMessage {
        String content;
        
        public StreamMessage() {}
        
        public StreamMessage(String content) {
            this.content = content;
        }
    }
    
    // TextStream Encoders
    
    public static class TextStreamBooleanEncoder implements Encoder.TextStream<Boolean> {
        @Override
        public void encode(Boolean object, Writer writer) throws EncodeException, IOException {
            writer.write("ENCODED:" + object);
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    public static class TextStreamIntegerEncoder implements Encoder.TextStream<Integer> {
        @Override
        public void encode(Integer object, Writer writer) throws EncodeException, IOException {
            writer.write("ENCODED:" + object);
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    public static class TextStreamLongEncoder implements Encoder.TextStream<Long> {
        @Override
        public void encode(Long object, Writer writer) throws EncodeException, IOException {
            writer.write("ENCODED:" + object);
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    public static class TextStreamMessageEncoder implements Encoder.TextStream<StreamMessage> {
        @Override
        public void encode(StreamMessage object, Writer writer) throws EncodeException, IOException {
            writer.write("DECODED:" + object.content);
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    // TextStream Decoders
    
    public static class TextStreamMessageDecoder implements Decoder.TextStream<StreamMessage> {
        @Override
        public StreamMessage decode(Reader reader) throws DecodeException, IOException {
            BufferedReader br = new BufferedReader(reader);
            String line = br.readLine();
            return new StreamMessage(line);
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    // BinaryStream Encoders
    
    public static class BinaryStreamBooleanEncoder implements Encoder.BinaryStream<Boolean> {
        @Override
        public void encode(Boolean object, OutputStream os) throws EncodeException, IOException {
            os.write(("ENCODED:" + object).getBytes());
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    public static class BinaryStreamIntegerEncoder implements Encoder.BinaryStream<Integer> {
        @Override
        public void encode(Integer object, OutputStream os) throws EncodeException, IOException {
            os.write(("ENCODED:" + object).getBytes());
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    public static class BinaryStreamLongEncoder implements Encoder.BinaryStream<Long> {
        @Override
        public void encode(Long object, OutputStream os) throws EncodeException, IOException {
            os.write(("ENCODED:" + object).getBytes());
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    public static class BinaryStreamMessageEncoder implements Encoder.BinaryStream<StreamMessage> {
        @Override
        public void encode(StreamMessage object, OutputStream os) throws EncodeException, IOException {
            os.write(("DECODED:" + object.content).getBytes());
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    // BinaryStream Decoders
    
    public static class BinaryStreamMessageDecoder implements Decoder.BinaryStream<StreamMessage> {
        @Override
        public StreamMessage decode(InputStream is) throws DecodeException, IOException {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = br.readLine();
            return new StreamMessage(line);
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
}
