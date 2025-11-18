package org.osgi.impl.websockets.compliance.negative;

import jakarta.websocket.OnMessage;
import jakarta.websocket.OnError;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;

/**
 * Collection of invalid endpoint classes for testing @OnMessage validation.
 * 
 * These endpoints are intentionally invalid to test that the server correctly
 * rejects them during endpoint registration.
 * 
 * Adapted from Jakarta WebSocket 2.2 TCK:
 * - com.sun.ts.tests.websocket.negdep.onmessage.srv.*
 */
public class InvalidOnMessageEndpoints {
    
    /**
     * Invalid: Duplicate text message handlers.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onmessage.srv.textduplicate
     * Specification: WSC-4.7-4 - Each endpoint may only have one message handling method
     * for each message type (text, binary, pong).
     */
    @ServerEndpoint("/invalid/duplicate-text")
    public static class DuplicateTextMessageEndpoint {
        @OnMessage
        public String echo1(Reader reader) throws IOException {
            return "reader";
        }
        
        @OnMessage
        public String echo2(String echo, boolean finito) {
            return echo;
        }
        
        @OnError
        public void onError(Session session, Throwable thr) {
            // Error handler
        }
    }
    
    /**
     * Invalid: Duplicate binary message handlers.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onmessage.srv.binaryduplicate
     * Specification: WSC-4.7-4
     */
    @ServerEndpoint("/invalid/duplicate-binary")
    public static class DuplicateBinaryMessageEndpoint {
        @OnMessage
        public void onMessage1(byte[] data) {
            // First binary handler
        }
        
        @OnMessage
        public void onMessage2(java.nio.ByteBuffer data) {
            // Second binary handler - INVALID
        }
        
        @OnError
        public void onError(Session session, Throwable thr) {
            // Error handler
        }
    }
    
    /**
     * Invalid: Text message with invalid int parameter.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onmessage.srv.textstringint
     * Specification: int parameter is only valid with specific signatures
     */
    @ServerEndpoint("/invalid/text-string-int")
    public static class TextMessageWithIntEndpoint {
        @OnMessage
        public String echo(String echo, int i) {
            return echo;
        }
        
        @OnError
        public void onError(Session session, Throwable thr) {
            // Error handler
        }
    }
    
    /**
     * Invalid: BigDecimal without decoder.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onmessage.srv.textbigdecimal
     * Specification: Custom object types require a decoder
     */
    @ServerEndpoint("/invalid/bigdecimal-nodecoder")
    public static class BigDecimalWithoutDecoderEndpoint {
        @OnMessage
        public String echo(BigDecimal echo) {
            return echo.toString();
        }
        
        @OnError
        public void onError(Session session, Throwable thr) {
            // Error handler
        }
    }
    
    /**
     * Invalid: boolean parameter not in last position.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onmessage.srv.textreaderboolean
     * Specification: boolean parameter must be the last parameter
     */
    @ServerEndpoint("/invalid/text-reader-boolean")
    public static class TextReaderBooleanEndpoint {
        @OnMessage
        public String echo(Reader reader, boolean last) throws IOException {
            return "invalid";
        }
        
        @OnError
        public void onError(Session session, Throwable thr) {
            // Error handler
        }
    }
    
    /**
     * Invalid: ByteBuffer with invalid int parameter.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onmessage.srv.binarybytebufferint
     * Specification: int parameter is only valid with specific signatures
     */
    @ServerEndpoint("/invalid/binary-bytebuffer-int")
    public static class BinaryByteBufferIntEndpoint {
        @OnMessage
        public void onMessage(java.nio.ByteBuffer data, int i) {
            // Invalid signature
        }
        
        @OnError
        public void onError(Session session, Throwable thr) {
            // Error handler
        }
    }
    
    /**
     * Invalid: InputStream with invalid boolean parameter.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onmessage.srv.binaryinputstreamboolean
     * Specification: boolean parameter must be the last parameter
     */
    @ServerEndpoint("/invalid/binary-inputstream-boolean")
    public static class BinaryInputStreamBooleanEndpoint {
        @OnMessage
        public void onMessage(java.io.InputStream stream, boolean last) {
            // Invalid signature
        }
        
        @OnError
        public void onError(Session session, Throwable thr) {
            // Error handler
        }
    }
    
    /**
     * Invalid: PongMessage with invalid boolean parameter.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onmessage.srv.pongboolean
     * Specification: PongMessage handlers cannot have boolean parameter
     */
    @ServerEndpoint("/invalid/pong-boolean")
    public static class PongBooleanEndpoint {
        @OnMessage
        public void onMessage(jakarta.websocket.PongMessage pong, boolean last) {
            // Invalid signature
        }
        
        @OnError
        public void onError(Session session, Throwable thr) {
            // Error handler
        }
    }
    
    /**
     * Invalid: Duplicate pong message handlers.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onmessage.srv.pongduplicate
     * Specification: WSC-4.7-4
     */
    @ServerEndpoint("/invalid/duplicate-pong")
    public static class DuplicatePongEndpoint {
        @OnMessage
        public void onMessage1(jakarta.websocket.PongMessage pong) {
            // First pong handler
        }
        
        @OnMessage
        public void onMessage2(jakarta.websocket.PongMessage pong, Session session) {
            // Second pong handler - INVALID
        }
        
        @OnError
        public void onError(Session session, Throwable thr) {
            // Error handler
        }
    }
}
