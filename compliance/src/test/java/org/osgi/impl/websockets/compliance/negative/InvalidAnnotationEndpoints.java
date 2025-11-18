package org.osgi.impl.websockets.compliance.negative;

import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.CloseReason;
import jakarta.websocket.server.ServerEndpoint;

/**
 * Collection of invalid endpoint classes for testing @OnOpen, @OnClose, and @OnError validation.
 * 
 * These endpoints are intentionally invalid to test that the server correctly
 * rejects them during endpoint registration.
 * 
 * Adapted from Jakarta WebSocket 2.2 TCK:
 * - com.sun.ts.tests.websocket.negdep.onopen.srv.*
 * - com.sun.ts.tests.websocket.negdep.onclose.srv.*
 * - com.sun.ts.tests.websocket.negdep.onerror.srv.*
 */
public class InvalidAnnotationEndpoints {
    
    /**
     * Invalid: Duplicate @OnOpen methods.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onopen.srv.duplicate
     * Specification: WSC-4.7-3 - Each endpoint may only have one @OnOpen method
     */
    @ServerEndpoint("/invalid/duplicate-onopen")
    public static class DuplicateOnOpenEndpoint {
        private String open;
        
        @OnMessage
        public String echo(String echo) {
            return open + echo;
        }
        
        @OnOpen
        public void onOpen1(Session session) {
            open = "session";
        }
        
        @OnOpen
        public void onOpen2(EndpointConfig config) {
            open = "config";
        }
        
        @OnError
        public void onError(Session session, Throwable thr) {
            // Error handler
        }
    }
    
    /**
     * Invalid: @OnOpen with too many parameters.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onopen.srv.toomanyargs
     * Specification: @OnOpen can only have Session, EndpointConfig, and @PathParam parameters
     */
    @ServerEndpoint("/invalid/onopen-toomanyargs")
    public static class OnOpenTooManyArgsEndpoint {
        @OnMessage
        public String echo(String echo) {
            return echo;
        }
        
        @OnOpen
        public void onOpen(Session session, EndpointConfig config, String invalid1, 
                          String invalid2, String invalid3, String invalid4,
                          String invalid5, String invalid6, String invalid7,
                          String invalid8, String invalid9, String invalid10,
                          String invalid11) {
            // Too many parameters
        }
        
        @OnError
        public void onError(Session session, Throwable thr) {
            // Error handler
        }
    }
    
    /**
     * Invalid: Duplicate @OnClose methods.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onclose.srv.duplicate
     * Specification: WSC-4.7-3 - Each endpoint may only have one @OnClose method
     */
    @ServerEndpoint("/invalid/duplicate-onclose")
    public static class DuplicateOnCloseEndpoint {
        @OnMessage
        public String echo(String echo) {
            return echo;
        }
        
        @OnClose
        public void onClose1(Session session) {
            // First close handler
        }
        
        @OnClose
        public void onClose2(CloseReason reason) {
            // Second close handler - INVALID
        }
        
        @OnError
        public void onError(Session session, Throwable thr) {
            // Error handler
        }
    }
    
    /**
     * Invalid: @OnClose with too many parameters.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onclose.srv.toomanyargs
     * Specification: @OnClose can only have Session, CloseReason, and @PathParam parameters
     */
    @ServerEndpoint("/invalid/onclose-toomanyargs")
    public static class OnCloseTooManyArgsEndpoint {
        @OnMessage
        public String echo(String echo) {
            return echo;
        }
        
        @OnClose
        public void onClose(Session session, CloseReason reason, String invalid1,
                           String invalid2, String invalid3, String invalid4,
                           String invalid5, String invalid6, String invalid7,
                           String invalid8, String invalid9, String invalid10,
                           String invalid11) {
            // Too many parameters
        }
        
        @OnError
        public void onError(Session session, Throwable thr) {
            // Error handler
        }
    }
    
    /**
     * Invalid: Duplicate @OnError methods.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onerror.srv.duplicate
     * Specification: WSC-4.7-3 - Each endpoint may only have one @OnError method
     */
    @ServerEndpoint("/invalid/duplicate-onerror")
    public static class DuplicateOnErrorEndpoint {
        @OnMessage
        public String echo(String echo) {
            return echo;
        }
        
        @OnError
        public void onError1(Session session, Throwable thr) {
            // First error handler
        }
        
        @OnError
        public void onError2(Throwable thr) {
            // Second error handler - INVALID
        }
    }
    
    /**
     * Invalid: @OnError with too many parameters.
     * 
     * TCK Reference: com.sun.ts.tests.websocket.negdep.onerror.srv.toomanyargs
     * Specification: @OnError can only have Session, Throwable, and @PathParam parameters
     */
    @ServerEndpoint("/invalid/onerror-toomanyargs")
    public static class OnErrorTooManyArgsEndpoint {
        @OnMessage
        public String echo(String echo) {
            return echo;
        }
        
        @OnError
        public void onError(Session session, Throwable thr, String invalid1,
                           String invalid2, String invalid3, String invalid4,
                           String invalid5, String invalid6, String invalid7,
                           String invalid8, String invalid9, String invalid10,
                           String invalid11) {
            // Too many parameters
        }
    }
    
    /**
     * Invalid: @OnOpen with invalid parameter type.
     * 
     * Specification: @OnOpen can only have Session, EndpointConfig, and @PathParam parameters
     */
    @ServerEndpoint("/invalid/onopen-invalidparam")
    public static class OnOpenInvalidParamEndpoint {
        @OnMessage
        public String echo(String echo) {
            return echo;
        }
        
        @OnOpen
        public void onOpen(Session session, String invalidParam) {
            // Invalid parameter type
        }
        
        @OnError
        public void onError(Session session, Throwable thr) {
            // Error handler
        }
    }
    
    /**
     * Invalid: @OnClose with invalid parameter type.
     * 
     * Specification: @OnClose can only have Session, CloseReason, and @PathParam parameters
     */
    @ServerEndpoint("/invalid/onclose-invalidparam")
    public static class OnCloseInvalidParamEndpoint {
        @OnMessage
        public String echo(String echo) {
            return echo;
        }
        
        @OnClose
        public void onClose(Session session, String invalidParam) {
            // Invalid parameter type
        }
        
        @OnError
        public void onError(Session session, Throwable thr) {
            // Error handler
        }
    }
    
    /**
     * Invalid: @OnError without Throwable parameter.
     * 
     * Specification: @OnError must have a Throwable parameter
     */
    @ServerEndpoint("/invalid/onerror-nothrowable")
    public static class OnErrorNoThrowableEndpoint {
        @OnMessage
        public String echo(String echo) {
            return echo;
        }
        
        @OnError
        public void onError(Session session) {
            // Missing Throwable parameter
        }
    }
    
    /**
     * Invalid: @OnError with invalid parameter type.
     * 
     * Specification: @OnError can only have Session, Throwable, and @PathParam parameters
     */
    @ServerEndpoint("/invalid/onerror-invalidparam")
    public static class OnErrorInvalidParamEndpoint {
        @OnMessage
        public String echo(String echo) {
            return echo;
        }
        
        @OnError
        public void onError(Session session, Throwable thr, CloseReason invalidParam) {
            // Invalid parameter type
        }
    }
}
