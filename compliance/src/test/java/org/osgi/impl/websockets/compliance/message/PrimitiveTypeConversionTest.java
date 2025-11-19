package org.osgi.impl.websockets.compliance.message;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.osgi.impl.websockets.server.JakartaWebSocketServer;
import org.osgi.impl.websockets.server.EndpointHandler;

import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.*;

/**
 * Compliance tests for primitive type conversion in @OnMessage handlers.
 * 
 * Adapted from Jakarta WebSocket 2.2 TCK:
 * - com.sun.ts.tests.websocket.ee.jakarta.websocket.websocketmessage.WSPrimitive*Server
 * - Specification: Jakarta WebSocket 2.2, Section 4.7 (Decoding Text and Binary Messages)
 * 
 * Tests primitive type parameters: boolean, byte, char, short, int, long, float, double
 * Also tests combinations with Session parameter.
 */
public class PrimitiveTypeConversionTest {
    
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
    
    // ========== Boolean Tests ==========
    
    /**
     * Test @OnMessage with boolean parameter
     * 
     * TCK Reference: WSPrimitiveBooleanServer.echo(boolean b)
     * Specification: Section 4.7 - Primitive type conversion
     */
    @Test
    public void testBooleanMessage() throws Exception {
        server.createEndpoint(BooleanEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/boolean"), 
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
        
        ws.sendText("true", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("true", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with boolean and Session parameters
     * 
     * TCK Reference: WSPrimitiveBooleanAndSessionServer.echo(boolean b, Session s)
     */
    @Test
    public void testBooleanWithSessionMessage() throws Exception {
        server.createEndpoint(BooleanWithSessionEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/booleansession"), 
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
        
        ws.sendText("false", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("false", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with Session and boolean parameters (reversed order)
     */
    @Test
    public void testSessionAndBooleanMessage() throws Exception {
        server.createEndpoint(SessionAndBooleanEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/sessionboolean"), 
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
        
        ws.sendText("true", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("true", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    // ========== Byte Tests ==========
    
    /**
     * Test @OnMessage with byte parameter
     * 
     * TCK Reference: WSPrimitiveByteServer.echo(byte b)
     */
    @Test
    public void testByteMessage() throws Exception {
        server.createEndpoint(ByteEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/byte"), 
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
        
        ws.sendText("123", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("123", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with byte and Session parameters
     */
    @Test
    public void testByteWithSessionMessage() throws Exception {
        server.createEndpoint(ByteWithSessionEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/bytesession"), 
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
        
        ws.sendText("42", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("42", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with Session and byte parameters
     */
    @Test
    public void testSessionAndByteMessage() throws Exception {
        server.createEndpoint(SessionAndByteEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/sessionbyte"), 
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
        
        ws.sendText("-5", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("-5", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    // ========== Char Tests ==========
    
    /**
     * Test @OnMessage with char parameter
     * 
     * TCK Reference: WSPrimitiveCharServer.echo(char c)
     */
    @Test
    public void testCharMessage() throws Exception {
        server.createEndpoint(CharEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/char"), 
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
        
        ws.sendText("E", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("E", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with char and Session parameters
     */
    @Test
    public void testCharWithSessionMessage() throws Exception {
        server.createEndpoint(CharWithSessionEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/charsession"), 
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
        
        ws.sendText("X", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("X", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with Session and char parameters
     */
    @Test
    public void testSessionAndCharMessage() throws Exception {
        server.createEndpoint(SessionAndCharEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/sessionchar"), 
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
        
        ws.sendText("Z", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("Z", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    // ========== Short Tests ==========
    
    /**
     * Test @OnMessage with short parameter
     * 
     * TCK Reference: WSPrimitiveShortServer.echo(short s)
     */
    @Test
    public void testShortMessage() throws Exception {
        server.createEndpoint(ShortEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/short"), 
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
        
        ws.sendText("32000", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("32000", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with short and Session parameters
     */
    @Test
    public void testShortWithSessionMessage() throws Exception {
        server.createEndpoint(ShortWithSessionEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/shortsession"), 
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
        
        ws.sendText("1234", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("1234", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with Session and short parameters
     */
    @Test
    public void testSessionAndShortMessage() throws Exception {
        server.createEndpoint(SessionAndShortEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/sessionshort"), 
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
        
        ws.sendText("-999", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("-999", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    // ========== Integer Tests ==========
    
    /**
     * Test @OnMessage with int parameter
     * 
     * TCK Reference: WSPrimitiveIntServer.echoInt(int i)
     */
    @Test
    public void testIntMessage() throws Exception {
        server.createEndpoint(IntEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/int"), 
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
        
        ws.sendText(String.valueOf(Integer.MIN_VALUE), true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals(String.valueOf(Integer.MIN_VALUE), response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with int and Session parameters
     */
    @Test
    public void testIntWithSessionMessage() throws Exception {
        server.createEndpoint(IntWithSessionEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/intsession"), 
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
        
        ws.sendText("42", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("42", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with Session and int parameters
     */
    @Test
    public void testSessionAndIntMessage() throws Exception {
        server.createEndpoint(SessionAndIntEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/sessionint"), 
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
        
        ws.sendText("9999", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("9999", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    // ========== Long Tests ==========
    
    /**
     * Test @OnMessage with long parameter
     * 
     * TCK Reference: WSPrimitiveLongServer.echo(long l)
     */
    @Test
    public void testLongMessage() throws Exception {
        server.createEndpoint(LongEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/long"), 
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
        
        ws.sendText(String.valueOf(Long.MAX_VALUE), true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals(String.valueOf(Long.MAX_VALUE), response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with long and Session parameters
     */
    @Test
    public void testLongWithSessionMessage() throws Exception {
        server.createEndpoint(LongWithSessionEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/longsession"), 
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
        
        ws.sendText("123456789", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("123456789", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with Session and long parameters
     */
    @Test
    public void testSessionAndLongMessage() throws Exception {
        server.createEndpoint(SessionAndLongEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/sessionlong"), 
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
        
        ws.sendText("-9876543210", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("-9876543210", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    // ========== Float Tests ==========
    
    /**
     * Test @OnMessage with float parameter
     * 
     * TCK Reference: WSPrimitiveFloatServer.echo(float f)
     */
    @Test
    public void testFloatMessage() throws Exception {
        server.createEndpoint(FloatEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/float"), 
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
        
        ws.sendText("3.14159", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("3.14159", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with float and Session parameters
     */
    @Test
    public void testFloatWithSessionMessage() throws Exception {
        server.createEndpoint(FloatWithSessionEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/floatsession"), 
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
        
        ws.sendText("2.71828", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("2.71828", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with Session and float parameters
     */
    @Test
    public void testSessionAndFloatMessage() throws Exception {
        server.createEndpoint(SessionAndFloatEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/sessionfloat"), 
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
        
        ws.sendText("-1.23456", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("-1.23456", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    // ========== Double Tests ==========
    
    /**
     * Test @OnMessage with double parameter
     * 
     * TCK Reference: WSPrimitiveDoubleServer.echo(double d)
     */
    @Test
    public void testDoubleMessage() throws Exception {
        server.createEndpoint(DoubleEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/double"), 
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
        
        ws.sendText("3.141592653589793", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("3.141592653589793", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with double and Session parameters
     */
    @Test
    public void testDoubleWithSessionMessage() throws Exception {
        server.createEndpoint(DoubleWithSessionEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/doublesession"), 
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
        
        ws.sendText("2.718281828459045", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("2.718281828459045", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    /**
     * Test @OnMessage with Session and double parameters
     */
    @Test
    public void testSessionAndDoubleMessage() throws Exception {
        server.createEndpoint(SessionAndDoubleEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/sessiondouble"), 
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
        
        ws.sendText("-9.87654321", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("-9.87654321", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    // ========== Wrapper Type Test ==========
    
    /**
     * Test @OnMessage with wrapper type (Integer) parameter
     * 
     * Verifies that wrapper types work the same as primitive types
     */
    @Test
    public void testWrapperTypeMessage() throws Exception {
        server.createEndpoint(IntegerWrapperEndpoint.class, null, createHandler());
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/wrapper"), 
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
        
        ws.sendText("12345", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        assertEquals("12345", response);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
    }
    
    // ========== Helper Methods ==========
    
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
    
    // ========== Endpoint Classes ==========
    
    // Boolean Endpoints
    
    @ServerEndpoint("/boolean")
    public static class BooleanEndpoint {
        @OnMessage
        public String echo(boolean b) {
            return String.valueOf(b);
        }
    }
    
    @ServerEndpoint("/booleansession")
    public static class BooleanWithSessionEndpoint {
        @OnMessage
        public String echo(boolean b, Session s) {
            return String.valueOf(b);
        }
    }
    
    @ServerEndpoint("/sessionboolean")
    public static class SessionAndBooleanEndpoint {
        @OnMessage
        public String echo(Session s, boolean b) {
            return String.valueOf(b);
        }
    }
    
    // Byte Endpoints
    
    @ServerEndpoint("/byte")
    public static class ByteEndpoint {
        @OnMessage
        public String echo(byte b) {
            return String.valueOf(b);
        }
    }
    
    @ServerEndpoint("/bytesession")
    public static class ByteWithSessionEndpoint {
        @OnMessage
        public String echo(byte b, Session s) {
            return String.valueOf(b);
        }
    }
    
    @ServerEndpoint("/sessionbyte")
    public static class SessionAndByteEndpoint {
        @OnMessage
        public String echo(Session s, byte b) {
            return String.valueOf(b);
        }
    }
    
    // Char Endpoints
    
    @ServerEndpoint("/char")
    public static class CharEndpoint {
        @OnMessage
        public String echo(char c) {
            return String.valueOf(c);
        }
    }
    
    @ServerEndpoint("/charsession")
    public static class CharWithSessionEndpoint {
        @OnMessage
        public String echo(char c, Session s) {
            return String.valueOf(c);
        }
    }
    
    @ServerEndpoint("/sessionchar")
    public static class SessionAndCharEndpoint {
        @OnMessage
        public String echo(Session s, char c) {
            return String.valueOf(c);
        }
    }
    
    // Short Endpoints
    
    @ServerEndpoint("/short")
    public static class ShortEndpoint {
        @OnMessage
        public String echo(short s) {
            return String.valueOf(s);
        }
    }
    
    @ServerEndpoint("/shortsession")
    public static class ShortWithSessionEndpoint {
        @OnMessage
        public String echo(short sh, Session s) {
            return String.valueOf(sh);
        }
    }
    
    @ServerEndpoint("/sessionshort")
    public static class SessionAndShortEndpoint {
        @OnMessage
        public String echo(Session s, short sh) {
            return String.valueOf(sh);
        }
    }
    
    // Integer Endpoints
    
    @ServerEndpoint("/int")
    public static class IntEndpoint {
        @OnMessage
        public String echo(int i) {
            return String.valueOf(i);
        }
    }
    
    @ServerEndpoint("/intsession")
    public static class IntWithSessionEndpoint {
        @OnMessage
        public String echo(int i, Session s) {
            return String.valueOf(i);
        }
    }
    
    @ServerEndpoint("/sessionint")
    public static class SessionAndIntEndpoint {
        @OnMessage
        public String echo(Session s, int i) {
            return String.valueOf(i);
        }
    }
    
    // Long Endpoints
    
    @ServerEndpoint("/long")
    public static class LongEndpoint {
        @OnMessage
        public String echo(long l) {
            return String.valueOf(l);
        }
    }
    
    @ServerEndpoint("/longsession")
    public static class LongWithSessionEndpoint {
        @OnMessage
        public String echo(long l, Session s) {
            return String.valueOf(l);
        }
    }
    
    @ServerEndpoint("/sessionlong")
    public static class SessionAndLongEndpoint {
        @OnMessage
        public String echo(Session s, long l) {
            return String.valueOf(l);
        }
    }
    
    // Float Endpoints
    
    @ServerEndpoint("/float")
    public static class FloatEndpoint {
        @OnMessage
        public String echo(float f) {
            return String.valueOf(f);
        }
    }
    
    @ServerEndpoint("/floatsession")
    public static class FloatWithSessionEndpoint {
        @OnMessage
        public String echo(float f, Session s) {
            return String.valueOf(f);
        }
    }
    
    @ServerEndpoint("/sessionfloat")
    public static class SessionAndFloatEndpoint {
        @OnMessage
        public String echo(Session s, float f) {
            return String.valueOf(f);
        }
    }
    
    // Double Endpoints
    
    @ServerEndpoint("/double")
    public static class DoubleEndpoint {
        @OnMessage
        public String echo(double d) {
            return String.valueOf(d);
        }
    }
    
    @ServerEndpoint("/doublesession")
    public static class DoubleWithSessionEndpoint {
        @OnMessage
        public String echo(double d, Session s) {
            return String.valueOf(d);
        }
    }
    
    @ServerEndpoint("/sessiondouble")
    public static class SessionAndDoubleEndpoint {
        @OnMessage
        public String echo(Session s, double d) {
            return String.valueOf(d);
        }
    }
    
    // Wrapper Type Endpoint
    
    @ServerEndpoint("/wrapper")
    public static class IntegerWrapperEndpoint {
        @OnMessage
        public String echo(Integer i) {
            return String.valueOf(i);
        }
    }
}
