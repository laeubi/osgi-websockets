package org.osgi.impl.websockets.client;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Extension;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import java.io.IOException;
import java.net.URI;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Implementation of Jakarta WebSocket Session for client connections.
 * This session wraps Java's built-in HttpClient WebSocket API.
 */
public class JakartaClientSession implements Session {
    
    private final String id;
    private final WebSocketContainer container;
    private final URI requestUri;
    private final ClientEndpointConfig config;
    private final Object endpointInstance;
    private final ClientEndpointCodecs codecs;
    private final Map<String, Object> userProperties;
    private final Set<MessageHandler> messageHandlers;
    private final BasicRemoteEndpointImpl basicRemote;
    private final AsyncRemoteEndpointImpl asyncRemote;
    
    private WebSocket webSocket;
    private Runnable onCloseCallback;
    private long maxIdleTimeout = 0;
    private int maxBinaryMessageBufferSize = 8192;
    private int maxTextMessageBufferSize = 8192;
    
    public JakartaClientSession(WebSocketContainer container, URI requestUri, 
            ClientEndpointConfig config, Object endpointInstance, ClientEndpointCodecs codecs) {
        this.id = UUID.randomUUID().toString();
        this.container = container;
        this.requestUri = requestUri;
        this.config = config;
        this.endpointInstance = endpointInstance;
        this.codecs = codecs;
        this.userProperties = new ConcurrentHashMap<>();
        this.messageHandlers = new HashSet<>();
        this.basicRemote = new BasicRemoteEndpointImpl(this);
        this.asyncRemote = new AsyncRemoteEndpointImpl(this);
    }
    
    /**
     * Sets the underlying WebSocket connection.
     */
    void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
    }
    
    /**
     * Gets the underlying WebSocket connection.
     */
    WebSocket getWebSocket() {
        return webSocket;
    }
    
    /**
     * Sets the callback to invoke when the session is closed.
     */
    void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }
    
    /**
     * Gets the codec handler for this session.
     */
    ClientEndpointCodecs getCodecs() {
        return codecs;
    }
    
    /**
     * Called when the underlying connection is closed.
     */
    void onConnectionClosed() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
        if (codecs != null) {
            codecs.destroy();
        }
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public boolean isOpen() {
        return webSocket != null && !webSocket.isInputClosed() && !webSocket.isOutputClosed();
    }
    
    @Override
    public void close() throws IOException {
        close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Normal closure"));
    }
    
    @Override
    public void close(CloseReason closeReason) throws IOException {
        if (webSocket != null && isOpen()) {
            try {
                webSocket.sendClose(closeReason.getCloseCode().getCode(), closeReason.getReasonPhrase())
                    .join();
            } catch (Exception e) {
                throw new IOException("Failed to close WebSocket: " + e.getMessage(), e);
            }
        }
        onConnectionClosed();
    }
    
    @Override
    public WebSocketContainer getContainer() {
        return container;
    }
    
    @Override
    public void addMessageHandler(MessageHandler handler) throws IllegalStateException {
        messageHandlers.add(handler);
    }
    
    @Override
    public <T> void addMessageHandler(Class<T> clazz, MessageHandler.Partial<T> handler) {
        messageHandlers.add(handler);
    }
    
    @Override
    public <T> void addMessageHandler(Class<T> clazz, MessageHandler.Whole<T> handler) {
        messageHandlers.add(handler);
    }
    
    @Override
    public Set<MessageHandler> getMessageHandlers() {
        return Collections.unmodifiableSet(messageHandlers);
    }
    
    @Override
    public void removeMessageHandler(MessageHandler handler) {
        messageHandlers.remove(handler);
    }
    
    @Override
    public String getProtocolVersion() {
        return "13";
    }
    
    @Override
    public String getNegotiatedSubprotocol() {
        return webSocket != null ? webSocket.getSubprotocol() : null;
    }
    
    @Override
    public List<Extension> getNegotiatedExtensions() {
        return Collections.emptyList();
    }
    
    @Override
    public boolean isSecure() {
        return requestUri != null && "wss".equalsIgnoreCase(requestUri.getScheme());
    }
    
    @Override
    public long getMaxIdleTimeout() {
        return maxIdleTimeout;
    }
    
    @Override
    public void setMaxIdleTimeout(long timeout) {
        this.maxIdleTimeout = timeout;
    }
    
    @Override
    public void setMaxBinaryMessageBufferSize(int size) {
        this.maxBinaryMessageBufferSize = size;
    }
    
    @Override
    public int getMaxBinaryMessageBufferSize() {
        return maxBinaryMessageBufferSize;
    }
    
    @Override
    public void setMaxTextMessageBufferSize(int size) {
        this.maxTextMessageBufferSize = size;
    }
    
    @Override
    public int getMaxTextMessageBufferSize() {
        return maxTextMessageBufferSize;
    }
    
    @Override
    public RemoteEndpoint.Async getAsyncRemote() {
        return asyncRemote;
    }
    
    @Override
    public RemoteEndpoint.Basic getBasicRemote() {
        return basicRemote;
    }
    
    @Override
    public URI getRequestURI() {
        return requestUri;
    }
    
    @Override
    public Map<String, List<String>> getRequestParameterMap() {
        // Parse query parameters from URI
        Map<String, List<String>> params = new HashMap<>();
        String query = requestUri.getQuery();
        if (query != null && !query.isEmpty()) {
            for (String pair : query.split("&")) {
                int idx = pair.indexOf('=');
                String key = idx > 0 ? pair.substring(0, idx) : pair;
                String value = idx > 0 && idx < pair.length() - 1 ? pair.substring(idx + 1) : "";
                params.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(value);
            }
        }
        return Collections.unmodifiableMap(params);
    }
    
    @Override
    public String getQueryString() {
        return requestUri.getQuery();
    }
    
    @Override
    public Map<String, String> getPathParameters() {
        // Client endpoints don't typically have path parameters
        return Collections.emptyMap();
    }
    
    @Override
    public Map<String, Object> getUserProperties() {
        return userProperties;
    }
    
    @Override
    public Principal getUserPrincipal() {
        return null;
    }
    
    @Override
    public Set<Session> getOpenSessions() {
        // Return all sessions from the container
        if (container instanceof JakartaWebSocketContainer) {
            return ((JakartaWebSocketContainer) container).getActiveSessions();
        }
        return Collections.emptySet();
    }
    
    /**
     * Basic implementation of RemoteEndpoint.Basic
     */
    private static class BasicRemoteEndpointImpl implements RemoteEndpoint.Basic {
        private final JakartaClientSession session;
        
        BasicRemoteEndpointImpl(JakartaClientSession session) {
            this.session = session;
        }
        
        @Override
        public void sendText(String text) throws IOException {
            if (!session.isOpen()) {
                throw new IOException("WebSocket is not open");
            }
            try {
                session.webSocket.sendText(text, true).join();
            } catch (Exception e) {
                throw new IOException("Failed to send text message: " + e.getMessage(), e);
            }
        }
        
        @Override
        public void sendBinary(ByteBuffer data) throws IOException {
            if (!session.isOpen()) {
                throw new IOException("WebSocket is not open");
            }
            try {
                session.webSocket.sendBinary(data, true).join();
            } catch (Exception e) {
                throw new IOException("Failed to send binary message: " + e.getMessage(), e);
            }
        }
        
        @Override
        public void sendText(String partialMessage, boolean isLast) throws IOException {
            if (!session.isOpen()) {
                throw new IOException("WebSocket is not open");
            }
            try {
                session.webSocket.sendText(partialMessage, isLast).join();
            } catch (Exception e) {
                throw new IOException("Failed to send partial text message: " + e.getMessage(), e);
            }
        }
        
        @Override
        public void sendBinary(ByteBuffer partialByte, boolean isLast) throws IOException {
            if (!session.isOpen()) {
                throw new IOException("WebSocket is not open");
            }
            try {
                session.webSocket.sendBinary(partialByte, isLast).join();
            } catch (Exception e) {
                throw new IOException("Failed to send partial binary message: " + e.getMessage(), e);
            }
        }
        
        @Override
        public java.io.OutputStream getSendStream() throws IOException {
            throw new UnsupportedOperationException("Stream output not yet implemented");
        }
        
        @Override
        public java.io.Writer getSendWriter() throws IOException {
            throw new UnsupportedOperationException("Writer output not yet implemented");
        }
        
        @Override
        public void sendObject(Object data) throws IOException {
            if (!session.isOpen()) {
                throw new IOException("WebSocket is not open");
            }
            
            ClientEndpointCodecs codecs = session.getCodecs();
            if (codecs == null) {
                throw new IOException("No encoders configured for this endpoint");
            }
            
            try {
                if (codecs.hasTextEncoder(data.getClass())) {
                    String encoded = codecs.encodeText(data);
                    sendText(encoded);
                    return;
                }
                
                if (codecs.hasBinaryEncoder(data.getClass())) {
                    ByteBuffer encoded = codecs.encodeBinary(data);
                    sendBinary(encoded);
                    return;
                }
                
                throw new IOException("No suitable encoder found for type: " + data.getClass().getName());
            } catch (jakarta.websocket.EncodeException e) {
                throw new IOException("Failed to encode object: " + e.getMessage(), e);
            }
        }
        
        @Override
        public void sendPing(ByteBuffer applicationData) throws IOException {
            if (!session.isOpen()) {
                throw new IOException("WebSocket is not open");
            }
            try {
                session.webSocket.sendPing(applicationData).join();
            } catch (Exception e) {
                throw new IOException("Failed to send ping: " + e.getMessage(), e);
            }
        }
        
        @Override
        public void sendPong(ByteBuffer applicationData) throws IOException {
            if (!session.isOpen()) {
                throw new IOException("WebSocket is not open");
            }
            try {
                session.webSocket.sendPong(applicationData).join();
            } catch (Exception e) {
                throw new IOException("Failed to send pong: " + e.getMessage(), e);
            }
        }
        
        @Override
        public void setBatchingAllowed(boolean batchingAllowed) throws IOException {
            // Not implemented
        }
        
        @Override
        public boolean getBatchingAllowed() {
            return false;
        }
        
        @Override
        public void flushBatch() throws IOException {
            // Not implemented
        }
    }
    
    /**
     * Async implementation of RemoteEndpoint.Async
     */
    private static class AsyncRemoteEndpointImpl implements RemoteEndpoint.Async {
        private final JakartaClientSession session;
        private long sendTimeout = 0;
        
        AsyncRemoteEndpointImpl(JakartaClientSession session) {
            this.session = session;
        }
        
        @Override
        public long getSendTimeout() {
            return sendTimeout;
        }
        
        @Override
        public void setSendTimeout(long timeout) {
            this.sendTimeout = timeout;
        }
        
        @Override
        public void sendText(String text, jakarta.websocket.SendHandler handler) {
            if (!session.isOpen()) {
                handler.onResult(new jakarta.websocket.SendResult(
                    new IOException("WebSocket is not open")));
                return;
            }
            session.webSocket.sendText(text, true).whenComplete((ws, ex) -> {
                if (ex != null) {
                    handler.onResult(new jakarta.websocket.SendResult(ex));
                } else {
                    handler.onResult(new jakarta.websocket.SendResult());
                }
            });
        }
        
        @Override
        public Future<Void> sendText(String text) {
            CompletableFuture<Void> result = new CompletableFuture<>();
            if (!session.isOpen()) {
                result.completeExceptionally(new IOException("WebSocket is not open"));
                return result;
            }
            session.webSocket.sendText(text, true).whenComplete((ws, ex) -> {
                if (ex != null) {
                    result.completeExceptionally(ex);
                } else {
                    result.complete(null);
                }
            });
            return result;
        }
        
        @Override
        public void sendBinary(ByteBuffer data, jakarta.websocket.SendHandler handler) {
            if (!session.isOpen()) {
                handler.onResult(new jakarta.websocket.SendResult(
                    new IOException("WebSocket is not open")));
                return;
            }
            session.webSocket.sendBinary(data, true).whenComplete((ws, ex) -> {
                if (ex != null) {
                    handler.onResult(new jakarta.websocket.SendResult(ex));
                } else {
                    handler.onResult(new jakarta.websocket.SendResult());
                }
            });
        }
        
        @Override
        public Future<Void> sendBinary(ByteBuffer data) {
            CompletableFuture<Void> result = new CompletableFuture<>();
            if (!session.isOpen()) {
                result.completeExceptionally(new IOException("WebSocket is not open"));
                return result;
            }
            session.webSocket.sendBinary(data, true).whenComplete((ws, ex) -> {
                if (ex != null) {
                    result.completeExceptionally(ex);
                } else {
                    result.complete(null);
                }
            });
            return result;
        }
        
        @Override
        public void sendObject(Object data, jakarta.websocket.SendHandler handler) {
            if (!session.isOpen()) {
                handler.onResult(new jakarta.websocket.SendResult(
                    new IOException("WebSocket is not open")));
                return;
            }
            
            ClientEndpointCodecs codecs = session.getCodecs();
            if (codecs == null) {
                handler.onResult(new jakarta.websocket.SendResult(
                    new IOException("No encoders configured for this endpoint")));
                return;
            }
            
            try {
                if (codecs.hasTextEncoder(data.getClass())) {
                    String encoded = codecs.encodeText(data);
                    sendText(encoded, handler);
                    return;
                }
                
                if (codecs.hasBinaryEncoder(data.getClass())) {
                    ByteBuffer encoded = codecs.encodeBinary(data);
                    sendBinary(encoded, handler);
                    return;
                }
                
                handler.onResult(new jakarta.websocket.SendResult(
                    new IOException("No suitable encoder found for type: " + data.getClass().getName())));
            } catch (jakarta.websocket.EncodeException e) {
                handler.onResult(new jakarta.websocket.SendResult(
                    new IOException("Failed to encode object: " + e.getMessage(), e)));
            }
        }
        
        @Override
        public Future<Void> sendObject(Object data) {
            CompletableFuture<Void> result = new CompletableFuture<>();
            
            if (!session.isOpen()) {
                result.completeExceptionally(new IOException("WebSocket is not open"));
                return result;
            }
            
            ClientEndpointCodecs codecs = session.getCodecs();
            if (codecs == null) {
                result.completeExceptionally(new IOException("No encoders configured for this endpoint"));
                return result;
            }
            
            try {
                if (codecs.hasTextEncoder(data.getClass())) {
                    String encoded = codecs.encodeText(data);
                    return sendText(encoded);
                }
                
                if (codecs.hasBinaryEncoder(data.getClass())) {
                    ByteBuffer encoded = codecs.encodeBinary(data);
                    return sendBinary(encoded);
                }
                
                result.completeExceptionally(
                    new IOException("No suitable encoder found for type: " + data.getClass().getName()));
            } catch (jakarta.websocket.EncodeException e) {
                result.completeExceptionally(
                    new IOException("Failed to encode object: " + e.getMessage(), e));
            }
            
            return result;
        }
        
        @Override
        public void setBatchingAllowed(boolean batchingAllowed) throws IOException {
            // Not implemented
        }
        
        @Override
        public boolean getBatchingAllowed() {
            return false;
        }
        
        @Override
        public void flushBatch() throws IOException {
            // Not implemented
        }
        
        @Override
        public void sendPing(ByteBuffer applicationData) throws IOException {
            if (!session.isOpen()) {
                throw new IOException("WebSocket is not open");
            }
            try {
                session.webSocket.sendPing(applicationData).join();
            } catch (Exception e) {
                throw new IOException("Failed to send ping: " + e.getMessage(), e);
            }
        }
        
        @Override
        public void sendPong(ByteBuffer applicationData) throws IOException {
            if (!session.isOpen()) {
                throw new IOException("WebSocket is not open");
            }
            try {
                session.webSocket.sendPong(applicationData).join();
            } catch (Exception e) {
                throw new IOException("Failed to send pong: " + e.getMessage(), e);
            }
        }
    }
}
