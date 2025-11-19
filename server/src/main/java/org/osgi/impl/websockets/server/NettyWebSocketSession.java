package org.osgi.impl.websockets.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Extension;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of Jakarta WebSocket Session backed by a Netty Channel.
 */
public class NettyWebSocketSession implements Session {
    
    private final String id;
    private final Channel channel;
    private final URI requestUri;
    private final String rawQueryString; // Raw query string from request URI (URL-encoded)
    private final Map<String, String> pathParameters;
    private final Map<String, List<String>> requestParameterMap;
    private final Map<String, Object> userProperties;
    private final Set<MessageHandler> messageHandlers;
    private final BasicRemoteEndpointImpl basicRemote;
    private final AsyncRemoteEndpointImpl asyncRemote;
    private EndpointCodecs codecs; // Encoders and decoders for this session
    
    private long maxIdleTimeout = 0;
    private int maxBinaryMessageBufferSize = 8192;
    private int maxTextMessageBufferSize = 8192;
    
    public NettyWebSocketSession(Channel channel, URI requestUri) {
        this(channel, requestUri, null);
    }
    
    public NettyWebSocketSession(Channel channel, URI requestUri, String rawQueryString) {
        this.id = UUID.randomUUID().toString();
        this.channel = channel;
        this.requestUri = requestUri;
        this.rawQueryString = rawQueryString;
        this.pathParameters = new HashMap<>();
        this.requestParameterMap = new HashMap<>();
        this.userProperties = new ConcurrentHashMap<>();
        this.messageHandlers = new HashSet<>();
        this.basicRemote = new BasicRemoteEndpointImpl(channel, this);
        this.asyncRemote = new AsyncRemoteEndpointImpl(channel, this);
    }
    
    /**
     * Sets the endpoint codecs for this session.
     * This is called when the session is initialized with an endpoint registration.
     */
    void setCodecs(EndpointCodecs codecs) {
        this.codecs = codecs;
    }
    
    /**
     * Gets the endpoint codecs for this session.
     */
    EndpointCodecs getCodecs() {
        return codecs;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public boolean isOpen() {
        return channel.isActive();
    }
    
    @Override
    public void close() throws IOException {
        close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Normal closure"));
    }
    
    @Override
    public void close(CloseReason closeReason) throws IOException {
        if (channel.isActive()) {
            channel.writeAndFlush(new CloseWebSocketFrame(
                closeReason.getCloseCode().getCode(),
                closeReason.getReasonPhrase()
            )).addListener(future -> channel.close());
        }
    }
    
    @Override
    public WebSocketContainer getContainer() {
        // Not implemented for server-side sessions
        return null;
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
        return "13"; // WebSocket protocol version
    }
    
    @Override
    public String getNegotiatedSubprotocol() {
        return null;
    }
    
    @Override
    public List<Extension> getNegotiatedExtensions() {
        return Collections.emptyList();
    }
    
    @Override
    public boolean isSecure() {
        return false; // TODO: Check if channel uses SSL
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
        return Collections.unmodifiableMap(requestParameterMap);
    }
    
    @Override
    public String getQueryString() {
        // Return the raw query string if available (preserves URL encoding)
        // Otherwise fall back to the decoded version from URI.getQuery()
        return rawQueryString != null ? rawQueryString : requestUri.getQuery();
    }
    
    @Override
    public Map<String, String> getPathParameters() {
        return Collections.unmodifiableMap(pathParameters);
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
        // Not tracking all sessions in this implementation
        return Collections.emptySet();
    }
    
    /**
     * Basic implementation of RemoteEndpoint.Basic
     */
    private static class BasicRemoteEndpointImpl implements RemoteEndpoint.Basic {
        private final Channel channel;
        private final NettyWebSocketSession session;
        
        BasicRemoteEndpointImpl(Channel channel, NettyWebSocketSession session) {
            this.channel = channel;
            this.session = session;
        }
        
        @Override
        public void sendText(String text) throws IOException {
            if (!channel.isActive()) {
                throw new IOException("Channel is not active");
            }
            channel.writeAndFlush(new TextWebSocketFrame(text)).syncUninterruptibly();
        }
        
        @Override
        public void sendBinary(java.nio.ByteBuffer data) throws IOException {
            if (!channel.isActive()) {
                throw new IOException("Channel is not active");
            }
            // Convert ByteBuffer to Netty ByteBuf - copy to avoid issues with buffer reuse
            io.netty.buffer.ByteBuf buf = Unpooled.copiedBuffer(data);
            channel.writeAndFlush(new BinaryWebSocketFrame(buf)).syncUninterruptibly();
        }
        
        @Override
        public void sendText(String partialMessage, boolean isLast) throws IOException {
            throw new UnsupportedOperationException("Partial text messages not yet implemented");
        }
        
        @Override
        public void sendBinary(java.nio.ByteBuffer partialByte, boolean isLast) throws IOException {
            throw new UnsupportedOperationException("Partial binary messages not yet implemented");
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
            if (!channel.isActive()) {
                throw new IOException("Channel is not active");
            }
            
            EndpointCodecs codecs = session.getCodecs();
            if (codecs == null) {
                throw new IOException("No encoders configured for this endpoint");
            }
            
            try {
                // Try text encoder first
                if (codecs.hasTextEncoder(data.getClass())) {
                    String encoded = codecs.encodeText(data);
                    sendText(encoded);
                    return;
                }
                
                // Try binary encoder
                if (codecs.hasBinaryEncoder(data.getClass())) {
                    java.nio.ByteBuffer encoded = codecs.encodeBinary(data);
                    sendBinary(encoded);
                    return;
                }
                
                throw new IOException("No suitable encoder found for type: " + data.getClass().getName());
            } catch (jakarta.websocket.EncodeException e) {
                throw new IOException("Failed to encode object: " + e.getMessage(), e);
            }
        }
        
        @Override
        public void sendPing(java.nio.ByteBuffer applicationData) throws IOException {
            throw new UnsupportedOperationException("Ping messages not yet implemented");
        }
        
        @Override
        public void sendPong(java.nio.ByteBuffer applicationData) throws IOException {
            throw new UnsupportedOperationException("Pong messages not yet implemented");
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
        private final Channel channel;
        private final NettyWebSocketSession session;
        
        AsyncRemoteEndpointImpl(Channel channel, NettyWebSocketSession session) {
            this.channel = channel;
            this.session = session;
        }
        
        @Override
        public long getSendTimeout() {
            return 0;
        }
        
        @Override
        public void setSendTimeout(long timeoutmillis) {
            // Not implemented
        }
        
        @Override
        public void sendText(String text, jakarta.websocket.SendHandler handler) {
            if (!channel.isActive()) {
                handler.onResult(new jakarta.websocket.SendResult(
                    new IOException("Channel is not active")
                ));
                return;
            }
            channel.writeAndFlush(new TextWebSocketFrame(text)).addListener(future -> {
                if (future.isSuccess()) {
                    handler.onResult(new jakarta.websocket.SendResult());
                } else {
                    handler.onResult(new jakarta.websocket.SendResult(future.cause()));
                }
            });
        }
        
        @Override
        public java.util.concurrent.Future<Void> sendText(String text) {
            java.util.concurrent.CompletableFuture<Void> result = new java.util.concurrent.CompletableFuture<>();
            if (!channel.isActive()) {
                result.completeExceptionally(new IOException("Channel is not active"));
                return result;
            }
            channel.writeAndFlush(new TextWebSocketFrame(text)).addListener(future -> {
                if (future.isSuccess()) {
                    result.complete(null);
                } else {
                    result.completeExceptionally(future.cause());
                }
            });
            return result;
        }
        
        @Override
        public void sendBinary(java.nio.ByteBuffer data, jakarta.websocket.SendHandler handler) {
            if (!channel.isActive()) {
                handler.onResult(new jakarta.websocket.SendResult(
                    new IOException("Channel is not active")
                ));
                return;
            }
            // Convert ByteBuffer to Netty ByteBuf - copy to avoid issues with buffer reuse
            io.netty.buffer.ByteBuf buf = Unpooled.copiedBuffer(data);
            channel.writeAndFlush(new BinaryWebSocketFrame(buf)).addListener(future -> {
                if (future.isSuccess()) {
                    handler.onResult(new jakarta.websocket.SendResult());
                } else {
                    handler.onResult(new jakarta.websocket.SendResult(future.cause()));
                }
            });
        }
        
        @Override
        public java.util.concurrent.Future<Void> sendBinary(java.nio.ByteBuffer data) {
            java.util.concurrent.CompletableFuture<Void> result = new java.util.concurrent.CompletableFuture<>();
            if (!channel.isActive()) {
                result.completeExceptionally(new IOException("Channel is not active"));
                return result;
            }
            // Convert ByteBuffer to Netty ByteBuf - copy to avoid issues with buffer reuse
            io.netty.buffer.ByteBuf buf = Unpooled.copiedBuffer(data);
            channel.writeAndFlush(new BinaryWebSocketFrame(buf)).addListener(future -> {
                if (future.isSuccess()) {
                    result.complete(null);
                } else {
                    result.completeExceptionally(future.cause());
                }
            });
            return result;
        }
        
        @Override
        public void sendObject(Object data, jakarta.websocket.SendHandler handler) {
            if (!channel.isActive()) {
                handler.onResult(new jakarta.websocket.SendResult(
                    new IOException("Channel is not active")
                ));
                return;
            }
            
            EndpointCodecs codecs = session.getCodecs();
            if (codecs == null) {
                handler.onResult(new jakarta.websocket.SendResult(
                    new IOException("No encoders configured for this endpoint")
                ));
                return;
            }
            
            try {
                // Try text encoder first
                if (codecs.hasTextEncoder(data.getClass())) {
                    String encoded = codecs.encodeText(data);
                    sendText(encoded, handler);
                    return;
                }
                
                // Try binary encoder
                if (codecs.hasBinaryEncoder(data.getClass())) {
                    java.nio.ByteBuffer encoded = codecs.encodeBinary(data);
                    sendBinary(encoded, handler);
                    return;
                }
                
                handler.onResult(new jakarta.websocket.SendResult(
                    new IOException("No suitable encoder found for type: " + data.getClass().getName())
                ));
            } catch (jakarta.websocket.EncodeException e) {
                handler.onResult(new jakarta.websocket.SendResult(
                    new IOException("Failed to encode object: " + e.getMessage(), e)
                ));
            }
        }
        
        @Override
        public java.util.concurrent.Future<Void> sendObject(Object data) {
            java.util.concurrent.CompletableFuture<Void> result = new java.util.concurrent.CompletableFuture<>();
            
            if (!channel.isActive()) {
                result.completeExceptionally(new IOException("Channel is not active"));
                return result;
            }
            
            EndpointCodecs codecs = session.getCodecs();
            if (codecs == null) {
                result.completeExceptionally(new IOException("No encoders configured for this endpoint"));
                return result;
            }
            
            try {
                // Try text encoder first
                if (codecs.hasTextEncoder(data.getClass())) {
                    String encoded = codecs.encodeText(data);
                    return sendText(encoded);
                }
                
                // Try binary encoder
                if (codecs.hasBinaryEncoder(data.getClass())) {
                    java.nio.ByteBuffer encoded = codecs.encodeBinary(data);
                    return sendBinary(encoded);
                }
                
                result.completeExceptionally(
                    new IOException("No suitable encoder found for type: " + data.getClass().getName())
                );
            } catch (jakarta.websocket.EncodeException e) {
                result.completeExceptionally(
                    new IOException("Failed to encode object: " + e.getMessage(), e)
                );
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
        public void sendPing(java.nio.ByteBuffer applicationData) throws IOException {
            throw new UnsupportedOperationException("Ping messages not yet implemented");
        }
        
        @Override
        public void sendPong(java.nio.ByteBuffer applicationData) throws IOException {
            throw new UnsupportedOperationException("Pong messages not yet implemented");
        }
    }
}
