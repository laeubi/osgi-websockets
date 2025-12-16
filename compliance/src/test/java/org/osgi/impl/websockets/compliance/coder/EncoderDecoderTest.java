package org.osgi.impl.websockets.compliance.coder;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.osgi.impl.websockets.server.JakartaWebSocketServer;
import org.osgi.impl.websockets.server.EndpointHandler;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/**
 * Compliance tests for Encoder and Decoder functionality.
 * 
 * Adapted from Jakarta WebSocket 2.2 TCK:
 * - com.sun.ts.tests.websocket.ee.jakarta.websocket.coder
 * - Specification: Jakarta WebSocket 2.2, Section 4.5 (Encoders and Decoders)
 */
public class EncoderDecoderTest {
    
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
     * Test text encoder/decoder with simple object
     * 
     * TCK Reference: WSCEndpointWithTextDecoder
     * Specification: Section 4.5.1
     */
    @Test
    public void testTextEncoderDecoder() throws Exception {
        server.createEndpoint(TextEncoderDecoderEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/textcoder"), 
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
        
        // Send message in format: count:42
        ws.sendText("count:42", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("COUNT:42", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test binary encoder/decoder with simple object
     * 
     * TCK Reference: WSCEndpointWithBinaryDecoder
     * Specification: Section 4.5.2
     */
    @Test  
    public void testBinaryEncoderDecoder() throws Exception {
        server.createEndpoint(BinaryEncoderDecoderEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<ByteBuffer> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/binarycoder"), 
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
        
        // Send binary data: 4 bytes for an integer
        ByteBuffer data = ByteBuffer.allocate(4);
        data.putInt(100);
        data.flip();
        
        ws.sendBinary(data, true);
        ByteBuffer response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals(200, response.getInt()); // Server doubles the value
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test binary willDecode() method
     * 
     * TCK Reference: binaryDecoderWillDecodeTest
     * Specification: Section 4.5.2
     */
    @Test
    public void testBinaryWillDecode() throws Exception {
        server.createEndpoint(BinaryWillDecodeEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<ByteBuffer> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/binarywilldecode"), 
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
        
        // Send message that should be accepted by willDecode
        ByteBuffer data = ByteBuffer.wrap("test:message".getBytes());
        ws.sendBinary(data, true);
        
        ByteBuffer response = messageFuture.get(5, TimeUnit.SECONDS);
        byte[] responseBytes = new byte[response.remaining()];
        response.get(responseBytes);
        String responseStr = new String(responseBytes);
        
        // Only the decoder with willDecode=true should process it
        assertTrue(responseStr.contains("PROCESSED"), 
            "Message should be processed by decoder with willDecode=true");
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test multiple binary decoders selection
     * 
     * TCK Reference: WSCEndpointWithBinaryDecoders
     * Specification: Section 4.5.2
     */
    @Test
    public void testMultipleBinaryDecoders() throws Exception {
        server.createEndpoint(MultipleBinaryDecodersEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<ByteBuffer> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/multibinarydecode"), 
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
        
        // Send message that matches the first decoder (starts with "TYPE1:")
        ByteBuffer data1 = ByteBuffer.wrap("TYPE1:hello".getBytes());
        ws.sendBinary(data1, true);
        
        ByteBuffer response1 = messageFuture.get(5, TimeUnit.SECONDS);
        byte[] responseBytes1 = new byte[response1.remaining()];
        response1.get(responseBytes1);
        String responseStr1 = new String(responseBytes1);
        assertEquals("DECODED_TYPE1:hello", responseStr1);
        
        // Send message that matches the second decoder (starts with "TYPE2:")
        CompletableFuture<ByteBuffer> messageFuture2 = new CompletableFuture<>();
        WebSocket ws2 = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/multibinarydecode"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, 
                                                       ByteBuffer data, 
                                                       boolean last) {
                        messageFuture2.complete(data);
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        ByteBuffer data2 = ByteBuffer.wrap("TYPE2:world".getBytes());
        ws2.sendBinary(data2, true);
        
        ByteBuffer response2 = messageFuture2.get(5, TimeUnit.SECONDS);
        byte[] responseBytes2 = new byte[response2.remaining()];
        response2.get(responseBytes2);
        String responseStr2 = new String(responseBytes2);
        assertEquals("DECODED_TYPE2:world", responseStr2);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
        ws2.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test willDecode() method
     * 
     * TCK Reference: Various willDecode tests
     * Specification: Section 4.5.1
     */
    @Test
    public void testWillDecode() throws Exception {
        server.createEndpoint(WillDecodeEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/willdecode"), 
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
        
        // Send valid message (contains :)
        ws.sendText("valid:message", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertTrue(response.contains("VALID"));
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test multiple decoders
     * 
     * TCK Reference: WSCEndpointWithTextDecoders
     * Specification: Section 4.5.1
     */
    @Test
    public void testMultipleDecoders() throws Exception {
        server.createEndpoint(MultipleDecodersEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/multidecode"), 
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
        
        // Send message that matches first decoder
        ws.sendText("count:10", true);
        String response1 = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("COUNT:10", response1);
        
        // Create new connection for second test
        CompletableFuture<String> messageFuture2 = new CompletableFuture<>();
        WebSocket ws2 = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/multidecode"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, 
                                                     CharSequence data, 
                                                     boolean last) {
                        messageFuture2.complete(data.toString());
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        ws2.sendText("item:apple", true);
        String response2 = messageFuture2.get(5, TimeUnit.SECONDS);
        assertEquals("ITEM:apple", response2);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
        ws2.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test encoder/decoder lifecycle (init/destroy)
     * 
     * TCK Reference: InitDestroyTextDecoder/InitDestroyTextEncoder
     * Specification: Section 4.5.1
     */
    @Test
    public void testEncoderDecoderLifecycle() throws Exception {
        // Reset counters
        LifecycleTextDecoder.initCount = 0;
        LifecycleTextDecoder.destroyCount = 0;
        LifecycleTextEncoder.initCount = 0;
        LifecycleTextEncoder.destroyCount = 0;
        
        server.createEndpoint(LifecycleEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/lifecycle"), 
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
        
        ws.sendText("test:value", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertNotNull(response);
        
        // Verify init was called at least once
        assertTrue(LifecycleTextDecoder.initCount > 0, 
            "Decoder init() should have been called");
        assertTrue(LifecycleTextEncoder.initCount > 0, 
            "Encoder init() should have been called");
        
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
    
    @ServerEndpoint(value = "/textcoder", 
                    decoders = {CounterDecoder.class}, 
                    encoders = {CounterEncoder.class})
    public static class TextEncoderDecoderEndpoint {
        @OnMessage
        public void processMessage(Counter counter, Session session) throws IOException, EncodeException {
            session.getBasicRemote().sendObject(counter);
        }
    }
    
    @ServerEndpoint(value = "/binarycoder", 
                    decoders = {IntegerBinaryDecoder.class}, 
                    encoders = {IntegerBinaryEncoder.class})
    public static class BinaryEncoderDecoderEndpoint {
        @OnMessage
        public void processMessage(Integer value, Session session) throws IOException, EncodeException {
            session.getBasicRemote().sendObject(value * 2); // Double the value
        }
    }
    
    @ServerEndpoint(value = "/willdecode", 
                    decoders = {WillDecodeTestDecoder.class}, 
                    encoders = {CounterEncoder.class})
    public static class WillDecodeEndpoint {
        @OnMessage
        public void processMessage(Counter counter, Session session) throws IOException, EncodeException {
            session.getBasicRemote().sendObject(counter);
        }
    }
    
    @ServerEndpoint(value = "/multidecode", 
                    decoders = {CounterDecoder.class, ItemDecoder.class}, 
                    encoders = {CounterEncoder.class, ItemEncoder.class})
    public static class MultipleDecodersEndpoint {
        @OnMessage
        public void processMessage(Object obj, Session session) throws IOException, EncodeException {
            session.getBasicRemote().sendObject(obj);
        }
    }
    
    @ServerEndpoint(value = "/lifecycle", 
                    decoders = {LifecycleTextDecoder.class}, 
                    encoders = {LifecycleTextEncoder.class})
    public static class LifecycleEndpoint {
        @OnMessage
        public void processMessage(Counter counter, Session session) throws IOException, EncodeException {
            session.getBasicRemote().sendObject(counter);
        }
    }
    
    // Helper classes
    
    public static class Counter {
        private String name;
        private int value;
        
        public Counter(String name, int value) {
            this.name = name;
            this.value = value;
        }
        
        public String getName() { return name; }
        public int getValue() { return value; }
    }
    
    public static class Item {
        private String name;
        
        public Item(String name) {
            this.name = name;
        }
        
        public String getName() { return name; }
    }
    
    // Text Decoders
    
    public static class CounterDecoder implements Decoder.Text<Counter> {
        @Override
        public Counter decode(String s) throws DecodeException {
            String[] parts = s.split(":");
            if (parts.length != 2) {
                throw new DecodeException(s, "Invalid format");
            }
            return new Counter(parts[0], Integer.parseInt(parts[1]));
        }
        
        @Override
        public boolean willDecode(String s) {
            return s != null && s.contains(":") && s.startsWith("count");
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    public static class ItemDecoder implements Decoder.Text<Item> {
        @Override
        public Item decode(String s) throws DecodeException {
            String[] parts = s.split(":");
            if (parts.length != 2) {
                throw new DecodeException(s, "Invalid format");
            }
            return new Item(parts[1]);
        }
        
        @Override
        public boolean willDecode(String s) {
            return s != null && s.contains(":") && s.startsWith("item");
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    public static class WillDecodeTestDecoder implements Decoder.Text<Counter> {
        @Override
        public Counter decode(String s) throws DecodeException {
            String[] parts = s.split(":");
            if (parts.length != 2) {
                throw new DecodeException(s, "Invalid format");
            }
            return new Counter(parts[0].toUpperCase(), 0);
        }
        
        @Override
        public boolean willDecode(String s) {
            // Only decode messages containing ":"
            return s != null && s.contains(":");
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    public static class LifecycleTextDecoder implements Decoder.Text<Counter> {
        static int initCount = 0;
        static int destroyCount = 0;
        
        @Override
        public Counter decode(String s) throws DecodeException {
            String[] parts = s.split(":");
            int value = 0;
            if (parts.length > 1) {
                try {
                    value = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    // If not a number, use 0 as default
                    value = 0;
                }
            }
            return new Counter(parts[0], value);
        }
        
        @Override
        public boolean willDecode(String s) {
            return s != null && s.contains(":");
        }
        
        @Override
        public void init(EndpointConfig config) {
            initCount++;
        }
        
        @Override
        public void destroy() {
            destroyCount++;
        }
    }
    
    // Text Encoders
    
    public static class CounterEncoder implements Encoder.Text<Counter> {
        @Override
        public String encode(Counter counter) throws EncodeException {
            return counter.getName().toUpperCase() + ":" + counter.getValue();
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    public static class ItemEncoder implements Encoder.Text<Item> {
        @Override
        public String encode(Item item) throws EncodeException {
            return "ITEM:" + item.getName();
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    public static class LifecycleTextEncoder implements Encoder.Text<Counter> {
        static int initCount = 0;
        static int destroyCount = 0;
        
        @Override
        public String encode(Counter counter) throws EncodeException {
            return counter.getName() + ":" + counter.getValue();
        }
        
        @Override
        public void init(EndpointConfig config) {
            initCount++;
        }
        
        @Override
        public void destroy() {
            destroyCount++;
        }
    }
    
    // Binary Decoders
    
    public static class IntegerBinaryDecoder implements Decoder.Binary<Integer> {
        @Override
        public Integer decode(ByteBuffer bytes) throws DecodeException {
            return bytes.getInt();
        }
        
        @Override
        public boolean willDecode(ByteBuffer bytes) {
            return bytes.remaining() >= 4;
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    // Binary Encoders
    
    public static class IntegerBinaryEncoder implements Encoder.Binary<Integer> {
        @Override
        public ByteBuffer encode(Integer value) throws EncodeException {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt(value);
            buffer.flip();
            return buffer;
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    // Additional test endpoint classes
    
    @ServerEndpoint(value = "/binarywilldecode", 
                    decoders = {BinaryWillDecodeDecoder.class}, 
                    encoders = {BinaryStringEncoder.class})
    public static class BinaryWillDecodeEndpoint {
        @OnMessage
        public void processMessage(BinaryString message, Session session) throws IOException, EncodeException {
            // Append "PROCESSED" to indicate successful decoding
            message.value = "PROCESSED:" + message.value;
            session.getBasicRemote().sendObject(message);
        }
    }
    
    @ServerEndpoint(value = "/multibinarydecode", 
                    decoders = {BinaryType1Decoder.class, BinaryType2Decoder.class}, 
                    encoders = {BinaryStringEncoder.class})
    public static class MultipleBinaryDecodersEndpoint {
        @OnMessage
        public void processMessage(BinaryString message, Session session) throws IOException, EncodeException {
            session.getBasicRemote().sendObject(message);
        }
    }
    
    // Helper class for binary string messages
    public static class BinaryString {
        String value;
        
        public BinaryString(String value) {
            this.value = value;
        }
    }
    
    // Binary decoders for willDecode test
    public static class BinaryWillDecodeDecoder implements Decoder.Binary<BinaryString> {
        @Override
        public BinaryString decode(ByteBuffer bytes) throws DecodeException {
            byte[] array = new byte[bytes.remaining()];
            bytes.get(array);
            return new BinaryString(new String(array));
        }
        
        @Override
        public boolean willDecode(ByteBuffer bytes) {
            // Only decode if message contains ":"
            byte[] array = new byte[bytes.remaining()];
            bytes.get(array);
            bytes.rewind(); // Reset position for decode()
            String content = new String(array);
            return content.contains(":");
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    // Binary decoders for multiple decoder test
    public static class BinaryType1Decoder implements Decoder.Binary<BinaryString> {
        @Override
        public BinaryString decode(ByteBuffer bytes) throws DecodeException {
            byte[] array = new byte[bytes.remaining()];
            bytes.get(array);
            String content = new String(array);
            // Remove "TYPE1:" prefix and add decode marker
            return new BinaryString("DECODED_" + content);
        }
        
        @Override
        public boolean willDecode(ByteBuffer bytes) {
            byte[] array = new byte[bytes.remaining()];
            bytes.get(array);
            bytes.rewind();
            String content = new String(array);
            return content.startsWith("TYPE1:");
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    public static class BinaryType2Decoder implements Decoder.Binary<BinaryString> {
        @Override
        public BinaryString decode(ByteBuffer bytes) throws DecodeException {
            byte[] array = new byte[bytes.remaining()];
            bytes.get(array);
            String content = new String(array);
            // Remove "TYPE2:" prefix and add decode marker
            return new BinaryString("DECODED_" + content);
        }
        
        @Override
        public boolean willDecode(ByteBuffer bytes) {
            byte[] array = new byte[bytes.remaining()];
            bytes.get(array);
            bytes.rewind();
            String content = new String(array);
            return content.startsWith("TYPE2:");
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    // Binary encoder for BinaryString
    public static class BinaryStringEncoder implements Encoder.Binary<BinaryString> {
        @Override
        public ByteBuffer encode(BinaryString obj) throws EncodeException {
            return ByteBuffer.wrap(obj.value.getBytes());
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    // ==================== Advanced Decoder Selection Tests ====================
    
    /**
     * Test decoder selection order with willDecode
     * 
     * TCK Reference: textDecoderWillDecodeTest
     * Specification: Section 4.5 - First decoder whose willDecode returns true is used
     */
    @Test
    public void testDecoderSelectionOrder() throws Exception {
        server.createEndpoint(DecoderSelectionOrderEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/decoderorder"), 
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
        
        // Send message that first decoder will accept
        ws.sendText("FIRST:value", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("FIRST:value:FIRST", response,
            "First decoder should be used when its willDecode returns true");
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test decoder selection with second decoder
     * 
     * TCK Reference: textDecoderWillDecodeTest
     * Specification: Section 4.5 - Second decoder used when first returns false
     */
    @Test
    public void testSecondDecoderSelection() throws Exception {
        server.createEndpoint(DecoderSelectionOrderEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/decoderorder"), 
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
        
        // Send message that second decoder will accept
        ws.sendText("SECOND:value", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("SECOND:value:SECOND", response,
            "Second decoder should be used when first decoder's willDecode returns false");
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test multiple text encoders
     * 
     * TCK Reference: Multiple encoder configuration
     * Specification: Section 4.5 - Multiple encoders can be registered
     */
    @Test
    public void testMultipleTextEncoders() throws Exception {
        server.createEndpoint(MultipleEncodersEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/multiencoders"), 
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
        
        assertTrue(response.startsWith("TYPE1:") || response.startsWith("TYPE2:"),
            "One of the encoders should be used");
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    // ==================== Additional Endpoint Classes ====================
    
    @ServerEndpoint(value = "/decoderorder", 
                    decoders = {FirstTextDecoder.class, SecondTextDecoder.class},
                    encoders = {StringBeanTextEncoder.class})
    public static class DecoderSelectionOrderEndpoint {
        @OnMessage
        public void processMessage(StringBean message, Session session) throws IOException, EncodeException {
            session.getBasicRemote().sendObject(message);
        }
    }
    
    @ServerEndpoint(value = "/multiencoders",
                    encoders = {Type1Encoder.class, Type2Encoder.class})
    public static class MultipleEncodersEndpoint {
        @OnMessage
        public void processMessage(String message, Session session) throws IOException, EncodeException {
            if ("test".equals(message)) {
                // Send Type1 object
                session.getBasicRemote().sendObject(new Type1("test"));
            }
        }
    }
    
    // ==================== Common Data Classes ====================
    
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
    
    // ==================== Additional Decoder Classes ====================
    
    public static class FirstTextDecoder implements Decoder.Text<StringBean> {
        @Override
        public StringBean decode(String s) throws DecodeException {
            StringBean bean = new StringBean();
            bean.set(s + ":FIRST");
            return bean;
        }
        
        @Override
        public boolean willDecode(String s) {
            return s != null && s.startsWith("FIRST:");
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    public static class SecondTextDecoder implements Decoder.Text<StringBean> {
        @Override
        public StringBean decode(String s) throws DecodeException {
            StringBean bean = new StringBean();
            bean.set(s + ":SECOND");
            return bean;
        }
        
        @Override
        public boolean willDecode(String s) {
            return s != null && s.startsWith("SECOND:");
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    // ==================== Additional Encoder Classes ====================
    
    public static class StringBeanTextEncoder implements Encoder.Text<StringBean> {
        @Override
        public String encode(StringBean object) throws EncodeException {
            return object.get();
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    public static class Type1 {
        String value;
        public Type1(String value) { this.value = value; }
        public String getValue() { return value; }
    }
    
    public static class Type2 {
        String value;
        public Type2(String value) { this.value = value; }
        public String getValue() { return value; }
    }
    
    public static class Type1Encoder implements Encoder.Text<Type1> {
        @Override
        public String encode(Type1 object) throws EncodeException {
            return "TYPE1:" + object.getValue();
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
    
    public static class Type2Encoder implements Encoder.Text<Type2> {
        @Override
        public String encode(Type2 object) throws EncodeException {
            return "TYPE2:" + object.getValue();
        }
        
        @Override
        public void init(EndpointConfig config) {}
        
        @Override
        public void destroy() {}
    }
}
