package org.osgi.impl.websockets.compliance.pathparam;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.impl.websockets.server.EndpointHandler;
import org.osgi.impl.websockets.server.JakartaWebSocketServer;
import org.osgi.impl.websockets.server.WebSocketEndpoint;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Compliance tests for @PathParam support in Jakarta WebSocket server.
 * 
 * Adapted from Jakarta WebSocket 2.2 TCK:
 * - Original: com.sun.ts.tests.websocket.ee.jakarta.websocket.server.pathparam.WSClientIT
 * - Specification: Jakarta WebSocket 2.2, Section 4.3 (URI Template Matching)
 * 
 * Tests @PathParam annotation on @OnOpen, @OnMessage, @OnError, and @OnClose methods
 * with various parameter types: String, primitives (boolean, char, int, long, float, double),
 * and their wrapper classes.
 */
public class PathParamComplianceTest {
    
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
    
    // ==================== Single String PathParam Tests ====================
    
    /**
     * Test @PathParam with single String parameter in @OnMessage
     * 
     * TCK Reference: directStringParamOnMessageTest
     * Specification: Section 4.3-3, 4.3-4
     */
    @Test
    public void testSingleStringPathParamOnMessage() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            SingleStringParamEndpoint.class, "/param/{param1}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/param/testvalue123"), 
                createListener(response))
            .join();
        
        ws.sendText("MESSAGE", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("testvalue123", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with single String parameter in @OnOpen
     * 
     * TCK Reference: directStringParamOnOpenTest
     * Specification: Section 4.3-3, 4.3-4
     */
    @Test
    public void testSingleStringPathParamOnOpen() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            SingleStringParamEndpoint.class, "/param/{param1}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/param/openvalue456"), 
                createListener(response))
            .join();
        
        ws.sendText("OPEN", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("openvalue456", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with single String parameter in @OnError
     * 
     * TCK Reference: directStringParamOnIOETest
     * Specification: Section 4.3-3, 4.3-4
     */
    @Test
    public void testSingleStringPathParamOnError() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            SingleStringParamEndpoint.class, "/param/{param1}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/param/errorvalue789"), 
                createListener(response))
            .join();
        
        ws.sendText("IOEXCEPTION", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("errorvalue789", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    // ==================== Multiple String PathParam Tests ====================
    
    /**
     * Test @PathParam with multiple String parameters in @OnMessage
     * 
     * TCK Reference: multipleStringParamsOnMessageTest
     * Specification: Section 4.3-3, 4.3-4
     */
    @Test
    public void testMultipleStringPathParamsOnMessage() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            MultipleStringParamEndpoint.class, "/multi/{param1}/{param2}/{param3}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/multi/value1/value2/value3"), 
                createListener(response))
            .join();
        
        ws.sendText("MESSAGE", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("value1value2value3", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with multiple String parameters in @OnOpen
     * 
     * TCK Reference: multipleStringParamsOnOpenTest
     * Specification: Section 4.3-3, 4.3-4
     */
    @Test
    public void testMultipleStringPathParamsOnOpen() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            MultipleStringParamEndpoint.class, "/multi/{param1}/{param2}/{param3}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/multi/open1/open2/open3"), 
                createListener(response))
            .join();
        
        ws.sendText("OPEN", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("open1open2open3", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    // ==================== Non-matching PathParam Tests ====================
    
    /**
     * Test @PathParam with non-matching parameter name returns null
     * 
     * TCK Reference: noStringParamsOnMessageTest
     * Specification: Section 4.3-3
     * "If the name does not match a path variable in the URI-template, 
     * the value of the method parameter this annotation annotates is null."
     */
    @Test
    public void testNonMatchingPathParamOnMessage() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            NonMatchingParamEndpoint.class, "/param/{actualParam}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/param/somevalue"), 
                createListener(response))
            .join();
        
        ws.sendText("MESSAGE", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("null", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    // ==================== Primitive Type PathParam Tests ====================
    
    /**
     * Test @PathParam with boolean and char primitive parameters in @OnMessage
     * 
     * TCK Reference: primitiveBooleanAndCharParamsOnMessageTest
     * Specification: Section 4.3-3, 4.3-5
     */
    @Test
    public void testBooleanAndCharPathParamsOnMessage() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            BooleanCharParamEndpoint.class, "/different/{param1}/{param2}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/different/true/X"), 
                createListener(response))
            .join();
        
        ws.sendText("MESSAGE", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("trueX", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with boolean and char primitive parameters in @OnOpen
     * 
     * TCK Reference: primitiveBooleanAndCharParamsOnOpenTest
     * Specification: Section 4.3-3, 4.3-5
     */
    @Test
    public void testBooleanAndCharPathParamsOnOpen() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            BooleanCharParamEndpoint.class, "/different/{param1}/{param2}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/different/false/Y"), 
                createListener(response))
            .join();
        
        ws.sendText("OPEN", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("falseY", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with Double and Float wrapper parameters in @OnMessage
     * 
     * TCK Reference: fullDoubleAndFloatParamsOnMessageTest
     * Specification: Section 4.3-3, 4.3-5
     */
    @Test
    public void testDoubleAndFloatPathParamsOnMessage() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            DoubleFloatParamEndpoint.class, "/numeric/{param1}/{param2}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/numeric/123.45/67.89"), 
                createListener(response))
            .join();
        
        ws.sendText("MESSAGE", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("123.4567.89", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with Double and Float wrapper parameters in @OnOpen
     * 
     * TCK Reference: fullDoubleAndFloatParamsOnOpenTest
     * Specification: Section 4.3-3, 4.3-5
     */
    @Test
    public void testDoubleAndFloatPathParamsOnOpen() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            DoubleFloatParamEndpoint.class, "/numeric/{param1}/{param2}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/numeric/99.99/1.23"), 
                createListener(response))
            .join();
        
        ws.sendText("OPEN", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("99.991.23", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with Long wrapper parameter
     * 
     * TCK Reference: WSDirectLongPathParamServer
     * Specification: Section 4.3-3, 4.3-5
     */
    @Test
    public void testLongPathParamOnMessage() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            LongParamEndpoint.class, "/{param1}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/9876543210"), 
                createListener(response))
            .join();
        
        ws.sendText("MESSAGE", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("9876543210", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with Long wrapper parameter in @OnOpen
     * 
     * TCK Reference: WSDirectLongPathParamServer
     * Specification: Section 4.3-3, 4.3-5
     */
    @Test
    public void testLongPathParamOnOpen() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            LongParamEndpoint.class, "/{param1}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/1234567890"), 
                createListener(response))
            .join();
        
        ws.sendText("OPEN", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("1234567890", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    // ==================== RuntimeException in @OnError Tests ====================
    
    /**
     * Test @PathParam with single String parameter in @OnError (RuntimeException)
     * 
     * TCK Reference: directStringParamOnRETest
     * Specification: Section 4.3-3, 4.3-4
     */
    @Test
    public void testSingleStringPathParamOnRuntimeException() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            SingleStringParamEndpoint.class, "/param/{param1}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/param/runtimevalue"), 
                createListener(response))
            .join();
        
        ws.sendText("RUNTIMEEXCEPTION", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("runtimevalue", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with multiple String parameters in @OnError (IOException)
     * 
     * TCK Reference: multipleStringParamsOnIOETest
     * Specification: Section 4.3-3, 4.3-4
     */
    @Test
    public void testMultipleStringPathParamsOnIOException() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            MultipleStringParamEndpoint.class, "/multi/{param1}/{param2}/{param3}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/multi/io1/io2/io3"), 
                createListener(response))
            .join();
        
        ws.sendText("IOEXCEPTION", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("io1io2io3", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with multiple String parameters in @OnError (RuntimeException)
     * 
     * TCK Reference: multipleStringParamsOnRETest
     * Specification: Section 4.3-3, 4.3-4
     */
    @Test
    public void testMultipleStringPathParamsOnRuntimeException() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            MultipleStringParamEndpoint.class, "/multi/{param1}/{param2}/{param3}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/multi/rt1/rt2/rt3"), 
                createListener(response))
            .join();
        
        ws.sendText("RUNTIMEEXCEPTION", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("rt1rt2rt3", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with non-matching parameter name in @OnOpen
     * 
     * TCK Reference: noStringParamsOnOpenTest
     * Specification: Section 4.3-3
     */
    @Test
    public void testNonMatchingPathParamOnOpen() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            NonMatchingParamEndpoint.class, "/param/{actualParam}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/param/testvalue"), 
                createListener(response))
            .join();
        
        ws.sendText("OPEN", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("null", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with non-matching parameter name in @OnError (IOException)
     * 
     * TCK Reference: noStringParamsOnIOETest
     * Specification: Section 4.3-3
     */
    @Test
    public void testNonMatchingPathParamOnIOException() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            NonMatchingParamEndpoint.class, "/param/{actualParam}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/param/errorval"), 
                createListener(response))
            .join();
        
        ws.sendText("IOEXCEPTION", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("null", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with non-matching parameter name in @OnError (RuntimeException)
     * 
     * TCK Reference: noStringParamsOnRETest
     * Specification: Section 4.3-3
     */
    @Test
    public void testNonMatchingPathParamOnRuntimeException() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            NonMatchingParamEndpoint.class, "/param/{actualParam}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/param/rtval"), 
                createListener(response))
            .join();
        
        ws.sendText("RUNTIMEEXCEPTION", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("null", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with boolean and char parameters in @OnError (IOException)
     * 
     * TCK Reference: primitiveBooleanAndCharParamsOnIOETest
     * Specification: Section 4.3-3, 4.3-5
     */
    @Test
    public void testBooleanAndCharPathParamsOnIOException() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            BooleanCharParamEndpoint.class, "/different/{param1}/{param2}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/different/false/Z"), 
                createListener(response))
            .join();
        
        ws.sendText("IOEXCEPTION", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("falseZ", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with boolean and char parameters in @OnError (RuntimeException)
     * 
     * TCK Reference: primitiveBooleanAndCharParamsOnRETest
     * Specification: Section 4.3-3, 4.3-5
     */
    @Test
    public void testBooleanAndCharPathParamsOnRuntimeException() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            BooleanCharParamEndpoint.class, "/different/{param1}/{param2}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/different/true/A"), 
                createListener(response))
            .join();
        
        ws.sendText("RUNTIMEEXCEPTION", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("trueA", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with Double and Float parameters in @OnError (IOException)
     * 
     * TCK Reference: fullDoubleAndFloatParamsOnIOTest
     * Specification: Section 4.3-3, 4.3-5
     */
    @Test
    public void testDoubleAndFloatPathParamsOnIOException() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            DoubleFloatParamEndpoint.class, "/numeric/{param1}/{param2}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/numeric/11.22/33.44"), 
                createListener(response))
            .join();
        
        ws.sendText("IOEXCEPTION", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("11.2233.44", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with Double and Float parameters in @OnError (RuntimeException)
     * 
     * TCK Reference: fullDoubleAndFloatParamsOnRETest
     * Specification: Section 4.3-3, 4.3-5
     */
    @Test
    public void testDoubleAndFloatPathParamsOnRuntimeException() throws Exception {
        WebSocketEndpoint endpoint = server.createEndpoint(
            DoubleFloatParamEndpoint.class, "/numeric/{param1}/{param2}", createHandler());
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/numeric/55.66/77.88"), 
                createListener(response))
            .join();
        
        ws.sendText("RUNTIMEEXCEPTION", true);
        String result = response.get(5, TimeUnit.SECONDS);
        
        assertEquals("55.6677.88", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        endpoint.dispose();
    }
    
    // ==================== @OnClose with @PathParam Tests ====================
    
    /**
     * Test @PathParam with single String parameter in @OnClose
     * 
     * TCK Reference: directStringParamOnCloseTest
     * Specification: Section 4.3-3, 4.3-4
     */
    @Test
    public void testSingleStringPathParamOnClose() throws Exception {
        OnCloseTestHelper helper = new OnCloseTestHelper();
        WebSocketEndpoint endpoint = server.createEndpoint(
            SingleStringParamWithCloseEndpoint.class, "/param/{param1}", createHandlerWithHelper(helper));
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/param/closevalue"), 
                createListener(response))
            .join();
        
        ws.sendText("MESSAGE", true);
        String result = response.get(5, TimeUnit.SECONDS);
        assertEquals("closevalue", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        
        // Wait a bit for @OnClose to be called
        Thread.sleep(200);
        
        assertEquals("closevalue", helper.getClosedParam());
        
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with multiple String parameters in @OnClose
     * 
     * TCK Reference: multipleStringParamsOnCloseTest
     * Specification: Section 4.3-3, 4.3-4
     */
    @Test
    public void testMultipleStringPathParamsOnClose() throws Exception {
        OnCloseTestHelper helper = new OnCloseTestHelper();
        WebSocketEndpoint endpoint = server.createEndpoint(
            MultipleStringParamWithCloseEndpoint.class, "/multi/{param1}/{param2}/{param3}", 
            createHandlerWithHelper(helper));
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/multi/c1/c2/c3"), 
                createListener(response))
            .join();
        
        ws.sendText("MESSAGE", true);
        String result = response.get(5, TimeUnit.SECONDS);
        assertEquals("c1c2c3", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        
        // Wait a bit for @OnClose to be called
        Thread.sleep(200);
        
        assertEquals("c1c2c3", helper.getClosedParam());
        
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with non-matching parameter name in @OnClose
     * 
     * TCK Reference: noStringParamsOnCloseTest
     * Specification: Section 4.3-3
     */
    @Test
    public void testNonMatchingPathParamOnClose() throws Exception {
        OnCloseTestHelper helper = new OnCloseTestHelper();
        WebSocketEndpoint endpoint = server.createEndpoint(
            NonMatchingParamWithCloseEndpoint.class, "/param/{actualParam}", 
            createHandlerWithHelper(helper));
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/param/anyvalue"), 
                createListener(response))
            .join();
        
        ws.sendText("MESSAGE", true);
        String result = response.get(5, TimeUnit.SECONDS);
        assertEquals("null", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        
        // Wait a bit for @OnClose to be called
        Thread.sleep(200);
        
        assertEquals("null", helper.getClosedParam());
        
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with boolean and char parameters in @OnClose
     * 
     * TCK Reference: primitiveBooleanAndCharParamsOnCloseTest
     * Specification: Section 4.3-3, 4.3-5
     */
    @Test
    public void testBooleanAndCharPathParamsOnClose() throws Exception {
        OnCloseTestHelper helper = new OnCloseTestHelper();
        WebSocketEndpoint endpoint = server.createEndpoint(
            BooleanCharParamWithCloseEndpoint.class, "/different/{param1}/{param2}", 
            createHandlerWithHelper(helper));
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/different/true/B"), 
                createListener(response))
            .join();
        
        ws.sendText("MESSAGE", true);
        String result = response.get(5, TimeUnit.SECONDS);
        assertEquals("trueB", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        
        // Wait a bit for @OnClose to be called
        Thread.sleep(200);
        
        assertEquals("trueB", helper.getClosedParam());
        
        endpoint.dispose();
    }
    
    /**
     * Test @PathParam with Double and Float parameters in @OnClose
     * 
     * TCK Reference: fullDoubleAndFloatParamsOnCloseTest
     * Specification: Section 4.3-3, 4.3-5
     */
    @Test
    public void testDoubleAndFloatPathParamsOnClose() throws Exception {
        OnCloseTestHelper helper = new OnCloseTestHelper();
        WebSocketEndpoint endpoint = server.createEndpoint(
            DoubleFloatParamWithCloseEndpoint.class, "/numeric/{param1}/{param2}", 
            createHandlerWithHelper(helper));
        
        CompletableFuture<String> response = new CompletableFuture<>();
        
        WebSocket ws = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/numeric/88.99/11.22"), 
                createListener(response))
            .join();
        
        ws.sendText("MESSAGE", true);
        String result = response.get(5, TimeUnit.SECONDS);
        assertEquals("88.9911.22", result);
        
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
        
        // Wait a bit for @OnClose to be called
        Thread.sleep(200);
        
        assertEquals("88.9911.22", helper.getClosedParam());
        
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
    
    private <T> EndpointHandler<T> createHandlerWithHelper(OnCloseTestHelper helper) {
        return new EndpointHandler<T>() {
            @Override
            public T createEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                try {
                    T instance = endpointClass.getDeclaredConstructor().newInstance();
                    // Set the helper on the endpoint
                    if (instance instanceof OnCloseAware) {
                        ((OnCloseAware) instance).setHelper(helper);
                    }
                    return instance;
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
    
    private static class OnCloseTestHelper {
        private volatile String closedParam;
        
        public void setClosedParam(String param) {
            this.closedParam = param;
        }
        
        public String getClosedParam() {
            return closedParam;
        }
    }
    
    private interface OnCloseAware {
        void setHelper(OnCloseTestHelper helper);
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
    
    // ==================== Test Endpoint Classes ====================
    
    @ServerEndpoint(value = "/param/{param1}")
    public static class SingleStringParamEndpoint {
        private String savedParam;
        
        @OnOpen
        public void onOpen(@PathParam("param1") String param1) {
            this.savedParam = param1;
        }
        
        @OnMessage
        public String onMessage(@PathParam("param1") String param1, String message) throws IOException {
            if ("MESSAGE".equals(message)) {
                return param1;
            } else if ("OPEN".equals(message)) {
                return savedParam;
            } else if ("IOEXCEPTION".equals(message)) {
                throw new IOException("TCK INTENDED ERROR");
            } else if ("RUNTIMEEXCEPTION".equals(message)) {
                throw new RuntimeException("TCK INTENDED ERROR");
            }
            return "unknown";
        }
        
        @OnError
        public void onError(@PathParam("param1") String param1, Session session, Throwable t) throws IOException {
            if ("TCK INTENDED ERROR".equals(t.getMessage())) {
                session.getBasicRemote().sendText(param1);
            }
        }
    }
    
    @ServerEndpoint(value = "/multi/{param1}/{param2}/{param3}")
    public static class MultipleStringParamEndpoint {
        private String[] savedParams = new String[3];
        
        @OnOpen
        public void onOpen(@PathParam("param1") String p1, @PathParam("param2") String p2, @PathParam("param3") String p3) {
            savedParams[0] = p1;
            savedParams[1] = p2;
            savedParams[2] = p3;
        }
        
        @OnMessage
        public String onMessage(@PathParam("param1") String p1, @PathParam("param2") String p2, 
                                @PathParam("param3") String p3, String message) throws IOException {
            if ("MESSAGE".equals(message)) {
                return p1 + p2 + p3;
            } else if ("OPEN".equals(message)) {
                return savedParams[0] + savedParams[1] + savedParams[2];
            } else if ("IOEXCEPTION".equals(message)) {
                throw new IOException("TCK INTENDED ERROR");
            } else if ("RUNTIMEEXCEPTION".equals(message)) {
                throw new RuntimeException("TCK INTENDED ERROR");
            }
            return "unknown";
        }
        
        @OnError
        public void onError(@PathParam("param1") String p1, @PathParam("param2") String p2, 
                           @PathParam("param3") String p3, Session session, Throwable t) throws IOException {
            if ("TCK INTENDED ERROR".equals(t.getMessage())) {
                session.getBasicRemote().sendText(p1 + p2 + p3);
            }
        }
    }
    
    @ServerEndpoint(value = "/param/{actualParam}")
    public static class NonMatchingParamEndpoint {
        private String savedParam;
        
        @OnOpen
        public void onOpen(@PathParam("nonExistentParam") String param) {
            this.savedParam = String.valueOf(param);
        }
        
        @OnMessage
        public String onMessage(@PathParam("nonExistentParam") String param, String message) throws IOException {
            if ("MESSAGE".equals(message)) {
                return String.valueOf(param);
            } else if ("OPEN".equals(message)) {
                return savedParam;
            } else if ("IOEXCEPTION".equals(message)) {
                throw new IOException("TCK INTENDED ERROR");
            } else if ("RUNTIMEEXCEPTION".equals(message)) {
                throw new RuntimeException("TCK INTENDED ERROR");
            }
            return "unknown";
        }
        
        @OnError
        public void onError(@PathParam("nonExistentParam") String param, Session session, Throwable t) throws IOException {
            if ("TCK INTENDED ERROR".equals(t.getMessage())) {
                session.getBasicRemote().sendText(String.valueOf(param));
            }
        }
    }
    
    @ServerEndpoint(value = "/different/{param1}/{param2}")
    public static class BooleanCharParamEndpoint {
        private String[] savedParams = new String[2];
        
        @OnOpen
        public void onOpen(@PathParam("param1") boolean p1, @PathParam("param2") char p2) {
            savedParams[0] = String.valueOf(p1);
            savedParams[1] = String.valueOf(p2);
        }
        
        @OnMessage
        public String onMessage(@PathParam("param1") boolean p1, @PathParam("param2") char p2, String message) throws IOException {
            if ("MESSAGE".equals(message)) {
                return String.valueOf(p1) + String.valueOf(p2);
            } else if ("OPEN".equals(message)) {
                return savedParams[0] + savedParams[1];
            } else if ("IOEXCEPTION".equals(message)) {
                throw new IOException("TCK INTENDED ERROR");
            } else if ("RUNTIMEEXCEPTION".equals(message)) {
                throw new RuntimeException("TCK INTENDED ERROR");
            }
            return "unknown";
        }
        
        @OnError
        public void onError(@PathParam("param1") boolean p1, @PathParam("param2") char p2, 
                           Session session, Throwable t) throws IOException {
            if ("TCK INTENDED ERROR".equals(t.getMessage())) {
                session.getBasicRemote().sendText(String.valueOf(p1) + String.valueOf(p2));
            }
        }
    }
    
    @ServerEndpoint(value = "/numeric/{param1}/{param2}")
    public static class DoubleFloatParamEndpoint {
        private String[] savedParams = new String[2];
        
        @OnOpen
        public void onOpen(@PathParam("param1") Double p1, @PathParam("param2") Float p2) {
            savedParams[0] = String.valueOf(p1);
            savedParams[1] = String.valueOf(p2);
        }
        
        @OnMessage
        public String onMessage(@PathParam("param1") Double p1, @PathParam("param2") Float p2, String message) throws IOException {
            if ("MESSAGE".equals(message)) {
                return String.valueOf(p1) + String.valueOf(p2);
            } else if ("OPEN".equals(message)) {
                return savedParams[0] + savedParams[1];
            } else if ("IOEXCEPTION".equals(message)) {
                throw new IOException("TCK INTENDED ERROR");
            } else if ("RUNTIMEEXCEPTION".equals(message)) {
                throw new RuntimeException("TCK INTENDED ERROR");
            }
            return "unknown";
        }
        
        @OnError
        public void onError(@PathParam("param1") Double p1, @PathParam("param2") Float p2, 
                           Session session, Throwable t) throws IOException {
            if ("TCK INTENDED ERROR".equals(t.getMessage())) {
                session.getBasicRemote().sendText(String.valueOf(p1) + String.valueOf(p2));
            }
        }
    }
    
    @ServerEndpoint(value = "/{param1}")
    public static class LongParamEndpoint {
        private String savedParam;
        
        @OnOpen
        public void onOpen(@PathParam("param1") Long param1) {
            this.savedParam = param1.toString();
        }
        
        @OnMessage
        public String onMessage(@PathParam("param1") Long param1, String message) {
            if ("MESSAGE".equals(message)) {
                return param1.toString();
            } else if ("OPEN".equals(message)) {
                return savedParam;
            }
            return "unknown";
        }
    }
    
    // ==================== Endpoints with @OnClose ====================
    
    @ServerEndpoint(value = "/param/{param1}")
    public static class SingleStringParamWithCloseEndpoint implements OnCloseAware {
        private String savedParam;
        private OnCloseTestHelper helper;
        
        @Override
        public void setHelper(OnCloseTestHelper helper) {
            this.helper = helper;
        }
        
        @OnOpen
        public void onOpen(@PathParam("param1") String param1) {
            this.savedParam = param1;
        }
        
        @OnMessage
        public String onMessage(@PathParam("param1") String param1, String message) {
            if ("MESSAGE".equals(message)) {
                return param1;
            } else if ("OPEN".equals(message)) {
                return savedParam;
            }
            return "unknown";
        }
        
        @OnClose
        public void onClose(@PathParam("param1") String param1) {
            if (helper != null) {
                helper.setClosedParam(param1);
            }
        }
    }
    
    @ServerEndpoint(value = "/multi/{param1}/{param2}/{param3}")
    public static class MultipleStringParamWithCloseEndpoint implements OnCloseAware {
        private String[] savedParams = new String[3];
        private OnCloseTestHelper helper;
        
        @Override
        public void setHelper(OnCloseTestHelper helper) {
            this.helper = helper;
        }
        
        @OnOpen
        public void onOpen(@PathParam("param1") String p1, @PathParam("param2") String p2, @PathParam("param3") String p3) {
            savedParams[0] = p1;
            savedParams[1] = p2;
            savedParams[2] = p3;
        }
        
        @OnMessage
        public String onMessage(@PathParam("param1") String p1, @PathParam("param2") String p2, 
                                @PathParam("param3") String p3, String message) {
            if ("MESSAGE".equals(message)) {
                return p1 + p2 + p3;
            } else if ("OPEN".equals(message)) {
                return savedParams[0] + savedParams[1] + savedParams[2];
            }
            return "unknown";
        }
        
        @OnClose
        public void onClose(@PathParam("param1") String p1, @PathParam("param2") String p2, @PathParam("param3") String p3) {
            if (helper != null) {
                helper.setClosedParam(p1 + p2 + p3);
            }
        }
    }
    
    @ServerEndpoint(value = "/param/{actualParam}")
    public static class NonMatchingParamWithCloseEndpoint implements OnCloseAware {
        private String savedParam;
        private OnCloseTestHelper helper;
        
        @Override
        public void setHelper(OnCloseTestHelper helper) {
            this.helper = helper;
        }
        
        @OnOpen
        public void onOpen(@PathParam("nonExistentParam") String param) {
            this.savedParam = String.valueOf(param);
        }
        
        @OnMessage
        public String onMessage(@PathParam("nonExistentParam") String param, String message) {
            if ("MESSAGE".equals(message)) {
                return String.valueOf(param);
            } else if ("OPEN".equals(message)) {
                return savedParam;
            }
            return "unknown";
        }
        
        @OnClose
        public void onClose(@PathParam("nonExistentParam") String param) {
            if (helper != null) {
                helper.setClosedParam(String.valueOf(param));
            }
        }
    }
    
    @ServerEndpoint(value = "/different/{param1}/{param2}")
    public static class BooleanCharParamWithCloseEndpoint implements OnCloseAware {
        private String[] savedParams = new String[2];
        private OnCloseTestHelper helper;
        
        @Override
        public void setHelper(OnCloseTestHelper helper) {
            this.helper = helper;
        }
        
        @OnOpen
        public void onOpen(@PathParam("param1") boolean p1, @PathParam("param2") char p2) {
            savedParams[0] = String.valueOf(p1);
            savedParams[1] = String.valueOf(p2);
        }
        
        @OnMessage
        public String onMessage(@PathParam("param1") boolean p1, @PathParam("param2") char p2, String message) {
            if ("MESSAGE".equals(message)) {
                return String.valueOf(p1) + String.valueOf(p2);
            } else if ("OPEN".equals(message)) {
                return savedParams[0] + savedParams[1];
            }
            return "unknown";
        }
        
        @OnClose
        public void onClose(@PathParam("param1") boolean p1, @PathParam("param2") char p2) {
            if (helper != null) {
                helper.setClosedParam(String.valueOf(p1) + String.valueOf(p2));
            }
        }
    }
    
    @ServerEndpoint(value = "/numeric/{param1}/{param2}")
    public static class DoubleFloatParamWithCloseEndpoint implements OnCloseAware {
        private String[] savedParams = new String[2];
        private OnCloseTestHelper helper;
        
        @Override
        public void setHelper(OnCloseTestHelper helper) {
            this.helper = helper;
        }
        
        @OnOpen
        public void onOpen(@PathParam("param1") Double p1, @PathParam("param2") Float p2) {
            savedParams[0] = String.valueOf(p1);
            savedParams[1] = String.valueOf(p2);
        }
        
        @OnMessage
        public String onMessage(@PathParam("param1") Double p1, @PathParam("param2") Float p2, String message) {
            if ("MESSAGE".equals(message)) {
                return String.valueOf(p1) + String.valueOf(p2);
            } else if ("OPEN".equals(message)) {
                return savedParams[0] + savedParams[1];
            }
            return "unknown";
        }
        
        @OnClose
        public void onClose(@PathParam("param1") Double p1, @PathParam("param2") Float p2) {
            if (helper != null) {
                helper.setClosedParam(String.valueOf(p1) + String.valueOf(p2));
            }
        }
    }
}
