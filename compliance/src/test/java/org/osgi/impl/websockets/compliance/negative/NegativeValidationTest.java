package org.osgi.impl.websockets.compliance.negative;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.impl.websockets.server.EndpointHandler;
import org.osgi.impl.websockets.server.JakartaWebSocketServer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Negative validation tests for WebSocket endpoint annotations.
 * 
 * These tests verify that the server correctly rejects invalid endpoint configurations
 * during endpoint registration, as required by the Jakarta WebSocket 2.2 specification.
 * 
 * Adapted from Jakarta WebSocket 2.2 TCK:
 * - com.sun.ts.tests.websocket.negdep.onmessage.srv.*
 * - com.sun.ts.tests.websocket.negdep.onopen.srv.*
 * - com.sun.ts.tests.websocket.negdep.onclose.srv.*
 * - com.sun.ts.tests.websocket.negdep.onerror.srv.*
 * 
 * Specification References:
 * - WSC-4.7-3: Each endpoint may only have one @OnOpen, @OnClose, and @OnError method
 * - WSC-4.7-4: Each endpoint may only have one message handling method for each
 *              message type (text, binary, pong)
 * - WSC-5.2.1-3: Deployment errors must halt deployment and remove endpoints
 */
public class NegativeValidationTest {
    
    private JakartaWebSocketServer server;
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 8098;
    
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
    
    private <T> EndpointHandler<T> createHandler() {
        return new EndpointHandler<T>() {
            @Override
            public T createEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed to create instance: " + e.getMessage());
                }
            }
            
            @Override
            public void sessionEnded(T endpointInstance) {
                // Cleanup if needed
            }
        };
    }
    
    // ========== @OnMessage Validation Tests ==========
    
    /**
     * Test that duplicate text message handlers are rejected.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onmessage.srv.textduplicate
     * Specification: WSC-4.7-4
     */
    @Test
    public void testDuplicateTextMessageHandlers() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            server.createEndpoint(
                InvalidOnMessageEndpoints.DuplicateTextMessageEndpoint.class,
                null,
                createHandler()
            );
        });
        
        assertTrue(exception.getMessage().contains("text message") || 
                   exception.getMessage().contains("only have one"),
                   "Expected error about duplicate text message handlers, got: " + exception.getMessage());
    }
    
    /**
     * Test that duplicate binary message handlers are rejected.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onmessage.srv.binaryduplicate
     * Specification: WSC-4.7-4
     */
    @Test
    public void testDuplicateBinaryMessageHandlers() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            server.createEndpoint(
                InvalidOnMessageEndpoints.DuplicateBinaryMessageEndpoint.class,
                null,
                createHandler()
            );
        });
        
        assertTrue(exception.getMessage().contains("binary message") || 
                   exception.getMessage().contains("only have one"),
                   "Expected error about duplicate binary message handlers, got: " + exception.getMessage());
    }
    
    /**
     * Test that duplicate pong message handlers are rejected.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onmessage.srv.pongduplicate
     * Specification: WSC-4.7-4
     */
    @Test
    public void testDuplicatePongMessageHandlers() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            server.createEndpoint(
                InvalidOnMessageEndpoints.DuplicatePongEndpoint.class,
                null,
                createHandler()
            );
        });
        
        assertTrue(exception.getMessage().contains("pong message") || 
                   exception.getMessage().contains("only have one"),
                   "Expected error about duplicate pong message handlers, got: " + exception.getMessage());
    }
    
    /**
     * Test that invalid int parameter in text message handler is rejected.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onmessage.srv.textstringint
     * Specification: int parameter is only valid with specific signatures
     */
    @Test
    public void testTextMessageWithInvalidIntParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            server.createEndpoint(
                InvalidOnMessageEndpoints.TextMessageWithIntEndpoint.class,
                null,
                createHandler()
            );
        });
        
        assertTrue(exception.getMessage().contains("int") || 
                   exception.getMessage().contains("message parameter") ||
                   exception.getMessage().contains("Invalid parameter"),
                   "Expected error about invalid int parameter, got: " + exception.getMessage());
    }
    
    /**
     * Test that boolean parameter not in last position is rejected.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onmessage.srv.textreaderboolean
     * Specification: boolean parameter must be the last parameter
     */
    @Test
    public void testBooleanNotInLastPosition() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            server.createEndpoint(
                InvalidOnMessageEndpoints.TextReaderBooleanEndpoint.class,
                null,
                createHandler()
            );
        });
        
        assertTrue(exception.getMessage().contains("boolean") && 
                   (exception.getMessage().contains("last") || exception.getMessage().contains("Reader or InputStream")),
                   "Expected error about boolean parameter position, got: " + exception.getMessage());
    }
    
    /**
     * Test that ByteBuffer with invalid int parameter is rejected.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onmessage.srv.binarybytebufferint
     * Specification: int parameter is only valid with specific signatures
     */
    @Test
    public void testBinaryByteBufferWithInvalidInt() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            server.createEndpoint(
                InvalidOnMessageEndpoints.BinaryByteBufferIntEndpoint.class,
                null,
                createHandler()
            );
        });
        
        assertTrue(exception.getMessage().contains("int") || 
                   exception.getMessage().contains("message parameter") ||
                   exception.getMessage().contains("Invalid parameter"),
                   "Expected error about invalid int parameter, got: " + exception.getMessage());
    }
    
    /**
     * Test that InputStream with invalid boolean parameter is rejected.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onmessage.srv.binaryinputstreamboolean
     * Specification: boolean parameter must be the last parameter
     */
    @Test
    public void testBinaryInputStreamWithInvalidBoolean() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            server.createEndpoint(
                InvalidOnMessageEndpoints.BinaryInputStreamBooleanEndpoint.class,
                null,
                createHandler()
            );
        });
        
        assertTrue(exception.getMessage().contains("boolean") && 
                   (exception.getMessage().contains("last") || exception.getMessage().contains("Reader or InputStream")),
                   "Expected error about boolean parameter position, got: " + exception.getMessage());
    }
    
    /**
     * Test that PongMessage with boolean parameter is rejected.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onmessage.srv.pongboolean
     * Specification: PongMessage handlers cannot have boolean parameter
     */
    @Test
    public void testPongMessageWithBoolean() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            server.createEndpoint(
                InvalidOnMessageEndpoints.PongBooleanEndpoint.class,
                null,
                createHandler()
            );
        });
        
        assertTrue(exception.getMessage().contains("PongMessage") && 
                   exception.getMessage().contains("boolean"),
                   "Expected error about PongMessage with boolean, got: " + exception.getMessage());
    }
    
    // ========== @OnOpen Validation Tests ==========
    
    /**
     * Test that duplicate @OnOpen methods are rejected.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onopen.srv.duplicate
     * Specification: WSC-4.7-3
     */
    @Test
    public void testDuplicateOnOpen() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            server.createEndpoint(
                InvalidAnnotationEndpoints.DuplicateOnOpenEndpoint.class,
                null,
                createHandler()
            );
        });
        
        assertTrue(exception.getMessage().contains("@OnOpen") || 
                   exception.getMessage().contains("only have one"),
                   "Expected error about duplicate @OnOpen, got: " + exception.getMessage());
    }
    
    /**
     * Test that @OnOpen with too many parameters is rejected.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onopen.srv.toomanyargs
     * Specification: @OnOpen has parameter limits
     */
    @Test
    public void testOnOpenTooManyArgs() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            server.createEndpoint(
                InvalidAnnotationEndpoints.OnOpenTooManyArgsEndpoint.class,
                null,
                createHandler()
            );
        });
        
        assertTrue(exception.getMessage().contains("too many") || 
                   exception.getMessage().contains("Invalid parameter"),
                   "Expected error about too many parameters, got: " + exception.getMessage());
    }
    
    /**
     * Test that @OnOpen with invalid parameter type is rejected.
     * 
     * Specification: @OnOpen can only have Session, EndpointConfig, and @PathParam parameters
     */
    @Test
    public void testOnOpenInvalidParam() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            server.createEndpoint(
                InvalidAnnotationEndpoints.OnOpenInvalidParamEndpoint.class,
                null,
                createHandler()
            );
        });
        
        assertTrue(exception.getMessage().contains("Invalid parameter") || 
                   exception.getMessage().contains("@OnOpen"),
                   "Expected error about invalid parameter type, got: " + exception.getMessage());
    }
    
    // ========== @OnClose Validation Tests ==========
    
    /**
     * Test that duplicate @OnClose methods are rejected.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onclose.srv.duplicate
     * Specification: WSC-4.7-3
     */
    @Test
    public void testDuplicateOnClose() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            server.createEndpoint(
                InvalidAnnotationEndpoints.DuplicateOnCloseEndpoint.class,
                null,
                createHandler()
            );
        });
        
        assertTrue(exception.getMessage().contains("@OnClose") || 
                   exception.getMessage().contains("only have one"),
                   "Expected error about duplicate @OnClose, got: " + exception.getMessage());
    }
    
    /**
     * Test that @OnClose with too many parameters is rejected.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onclose.srv.toomanyargs
     * Specification: @OnClose has parameter limits
     */
    @Test
    public void testOnCloseTooManyArgs() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            server.createEndpoint(
                InvalidAnnotationEndpoints.OnCloseTooManyArgsEndpoint.class,
                null,
                createHandler()
            );
        });
        
        assertTrue(exception.getMessage().contains("too many") || 
                   exception.getMessage().contains("Invalid parameter"),
                   "Expected error about too many parameters, got: " + exception.getMessage());
    }
    
    /**
     * Test that @OnClose with invalid parameter type is rejected.
     * 
     * Specification: @OnClose can only have Session, CloseReason, and @PathParam parameters
     */
    @Test
    public void testOnCloseInvalidParam() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            server.createEndpoint(
                InvalidAnnotationEndpoints.OnCloseInvalidParamEndpoint.class,
                null,
                createHandler()
            );
        });
        
        assertTrue(exception.getMessage().contains("Invalid parameter") || 
                   exception.getMessage().contains("@OnClose"),
                   "Expected error about invalid parameter type, got: " + exception.getMessage());
    }
    
    // ========== @OnError Validation Tests ==========
    
    /**
     * Test that duplicate @OnError methods are rejected.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onerror.srv.duplicate
     * Specification: WSC-4.7-3
     */
    @Test
    public void testDuplicateOnError() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            server.createEndpoint(
                InvalidAnnotationEndpoints.DuplicateOnErrorEndpoint.class,
                null,
                createHandler()
            );
        });
        
        assertTrue(exception.getMessage().contains("@OnError") || 
                   exception.getMessage().contains("only have one"),
                   "Expected error about duplicate @OnError, got: " + exception.getMessage());
    }
    
    /**
     * Test that @OnError with too many parameters is rejected.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onerror.srv.toomanyargs
     * Specification: @OnError has parameter limits
     */
    @Test
    public void testOnErrorTooManyArgs() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            server.createEndpoint(
                InvalidAnnotationEndpoints.OnErrorTooManyArgsEndpoint.class,
                null,
                createHandler()
            );
        });
        
        assertTrue(exception.getMessage().contains("too many") || 
                   exception.getMessage().contains("Invalid parameter"),
                   "Expected error about too many parameters, got: " + exception.getMessage());
    }
    
    /**
     * Test that @OnError without Throwable parameter is rejected.
     * 
     * Specification: @OnError must have a Throwable parameter
     */
    @Test
    public void testOnErrorNoThrowable() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            server.createEndpoint(
                InvalidAnnotationEndpoints.OnErrorNoThrowableEndpoint.class,
                null,
                createHandler()
            );
        });
        
        assertTrue(exception.getMessage().contains("Throwable") || 
                   exception.getMessage().contains("must have"),
                   "Expected error about missing Throwable parameter, got: " + exception.getMessage());
    }
    
    /**
     * Test that @OnError with invalid parameter type is rejected.
     * 
     * Specification: @OnError can only have Session, Throwable, and @PathParam parameters
     */
    @Test
    public void testOnErrorInvalidParam() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            server.createEndpoint(
                InvalidAnnotationEndpoints.OnErrorInvalidParamEndpoint.class,
                null,
                createHandler()
            );
        });
        
        assertTrue(exception.getMessage().contains("Invalid parameter") || 
                   exception.getMessage().contains("@OnError"),
                   "Expected error about invalid parameter type, got: " + exception.getMessage());
    }
}
