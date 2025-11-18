package org.osgi.impl.websockets.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AttributeKey;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpointConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles WebSocket frames and dispatches to registered Jakarta WebSocket endpoints.
 */
public class EndpointWebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    
    private static final AttributeKey<Object> ENDPOINT_INSTANCE_KEY = AttributeKey.valueOf("endpoint_instance");
    private static final AttributeKey<Session> SESSION_KEY = AttributeKey.valueOf("websocket_session");
    
    private final JakartaWebSocketServer server;
    
    public EndpointWebSocketFrameHandler(JakartaWebSocketServer server) {
        this.server = server;
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // This is called after the WebSocket handshake completes
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            WebSocketServerProtocolHandler.HandshakeComplete handshake = 
                (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            
            // Get the full request URI (including query string) from the channel attribute
            String fullUri = ctx.channel().attr(WebSocketPathHandler.REQUEST_URI_KEY).get();
            if (fullUri == null) {
                // Fallback to handshake URI if attribute not set
                fullUri = handshake.requestUri();
            }
            
            // Create a Session for this connection with the full URI
            java.net.URI requestUri;
            try {
                // Try to parse as complete URI first
                requestUri = new java.net.URI(fullUri);
            } catch (java.net.URISyntaxException e) {
                // Fallback: add ws://localhost prefix if needed
                try {
                    requestUri = new java.net.URI("ws://localhost" + fullUri);
                } catch (java.net.URISyntaxException e2) {
                    // Last resort: use just the path from handshake
                    requestUri = java.net.URI.create("ws://localhost" + handshake.requestUri());
                }
            }
            NettyWebSocketSession session = new NettyWebSocketSession(ctx.channel(), requestUri);
            ctx.channel().attr(SESSION_KEY).set(session);
            
            // Now get the endpoint registration that was set by WebSocketPathHandler
            JakartaWebSocketServer.EndpointRegistration registration = 
                ctx.channel().attr(WebSocketPathHandler.ENDPOINT_REGISTRATION_KEY).get();
            
            if (registration == null) {
                // No endpoint registered for this path - close connection with error
                System.err.println("No endpoint registered for path: " + handshake.requestUri());
                ctx.close();
                return;
            }
            
            // Set the endpoint codecs on the session
            session.setCodecs(registration.codecs);
            
            try {
                // Create endpoint instance using the handler
                Object endpointInstance = registration.handler.createEndpointInstance(registration.endpointClass);
                ctx.channel().attr(ENDPOINT_INSTANCE_KEY).set(endpointInstance);
                
                // Register this channel with the endpoint
                registration.registerChannel(ctx.channel(), endpointInstance);
                
                // Invoke @OnOpen if present
                invokeOnOpen(endpointInstance, session);
            } catch (Exception e) {
                System.err.println("Failed to create endpoint instance: " + e.getMessage());
                e.printStackTrace();
                ctx.close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            String receivedText = textFrame.text();
            
            // Get the endpoint instance for this channel
            Object endpointInstance = ctx.channel().attr(ENDPOINT_INSTANCE_KEY).get();
            if (endpointInstance == null) {
                // No endpoint instance - this is an error
                System.err.println("No endpoint instance found for channel");
                ctx.close();
                return;
            }
            
            // Get the session for this channel
            Session session = ctx.channel().attr(SESSION_KEY).get();
            
            // Find and invoke @OnMessage method
            String response = invokeOnMessage(endpointInstance, receivedText, session);
            if (response != null) {
                ctx.channel().writeAndFlush(new TextWebSocketFrame(response));
            }
        } else if (frame instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame binaryFrame = (BinaryWebSocketFrame) frame;
            
            // Get the endpoint instance for this channel
            Object endpointInstance = ctx.channel().attr(ENDPOINT_INSTANCE_KEY).get();
            if (endpointInstance == null) {
                // No endpoint instance - this is an error
                System.err.println("No endpoint instance found for channel");
                ctx.close();
                return;
            }
            
            // Get the session for this channel
            Session session = ctx.channel().attr(SESSION_KEY).get();
            
            // Convert ByteBuf to ByteBuffer
            java.nio.ByteBuffer receivedData = binaryFrame.content().nioBuffer();
            
            // Find and invoke @OnMessage method for binary data
            invokeOnBinaryMessage(endpointInstance, receivedData, session);
        } else {
            String message = "Unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("WebSocket client connected: " + ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("WebSocket client disconnected: " + ctx.channel().remoteAddress());
        
        // Get the endpoint instance
        Object endpointInstance = ctx.channel().attr(ENDPOINT_INSTANCE_KEY).get();
        if (endpointInstance != null) {
            // Get the session for this channel
            Session session = ctx.channel().attr(SESSION_KEY).get();
            // Invoke @OnClose if present
            invokeOnClose(endpointInstance, session);
            
            // Notify the handler that the session has ended
            JakartaWebSocketServer.EndpointRegistration registration = 
                ctx.channel().attr(WebSocketPathHandler.ENDPOINT_REGISTRATION_KEY).get();
            if (registration != null) {
                try {
                    registration.handler.sessionEnded(endpointInstance);
                } catch (Exception e) {
                    System.err.println("Error in EndpointHandler.sessionEnded: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        // Unregister the channel from the endpoint registration
        JakartaWebSocketServer.EndpointRegistration registration = 
            ctx.channel().attr(WebSocketPathHandler.ENDPOINT_REGISTRATION_KEY).get();
        if (registration != null) {
            registration.unregisterChannel(ctx.channel());
        }
        
        super.channelInactive(ctx);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        
        // Get the endpoint instance
        Object endpointInstance = ctx.channel().attr(ENDPOINT_INSTANCE_KEY).get();
        if (endpointInstance != null) {
            // Get the session for this channel
            Session session = ctx.channel().attr(SESSION_KEY).get();
            // Invoke @OnError if present
            invokeOnError(endpointInstance, cause, session);
        }
        
        ctx.close();
    }
    
    /**
     * Invokes the @OnMessage method on the endpoint instance.
     */
    private String invokeOnMessage(Object endpointInstance, String message, Session session) {
        Method[] methods = endpointInstance.getClass().getDeclaredMethods();
        
        // Get the endpoint registration to access codecs
        JakartaWebSocketServer.EndpointRegistration registration = null;
        if (session instanceof NettyWebSocketSession) {
            NettyWebSocketSession nettySession = (NettyWebSocketSession) session;
            // The codecs are already set on the session
        }
        
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnMessage.class)) {
                try {
                    method.setAccessible(true);
                    Object result;
                    Class<?>[] paramTypes = method.getParameterTypes();
                    
                    // Try different parameter combinations
                    if (paramTypes.length == 1 && paramTypes[0] == String.class) {
                        // Method signature: String onMessage(String message)
                        result = method.invoke(endpointInstance, message);
                    } else if (paramTypes.length == 2 && paramTypes[0] == String.class && 
                               paramTypes[1] == Session.class) {
                        // Method signature: String onMessage(String message, Session session)
                        result = method.invoke(endpointInstance, message, session);
                    } else if (paramTypes.length == 2 && paramTypes[0] == Session.class && 
                               paramTypes[1] == String.class) {
                        // Method signature: String onMessage(Session session, String message)
                        result = method.invoke(endpointInstance, session, message);
                    } else if (paramTypes.length == 1 && paramTypes[0] != String.class) {
                        // Method signature with decoder: void onMessage(CustomType message)
                        // Try to decode the message using the endpoint's decoders
                        Object decodedMessage = tryDecodeTextMessage(message, paramTypes[0], session);
                        if (decodedMessage != null) {
                            result = method.invoke(endpointInstance, decodedMessage);
                        } else {
                            // No decoder found, skip this method
                            continue;
                        }
                    } else if (paramTypes.length == 2 && paramTypes[0] != String.class && 
                               paramTypes[1] == Session.class) {
                        // Method signature with decoder: void onMessage(CustomType message, Session session)
                        Object decodedMessage = tryDecodeTextMessage(message, paramTypes[0], session);
                        if (decodedMessage != null) {
                            result = method.invoke(endpointInstance, decodedMessage, session);
                        } else {
                            // No decoder found, skip this method
                            continue;
                        }
                    } else if (paramTypes.length == 2 && paramTypes[0] == Session.class && 
                               paramTypes[1] != String.class) {
                        // Method signature with decoder: void onMessage(Session session, CustomType message)
                        Object decodedMessage = tryDecodeTextMessage(message, paramTypes[1], session);
                        if (decodedMessage != null) {
                            result = method.invoke(endpointInstance, session, decodedMessage);
                        } else {
                            // No decoder found, skip this method
                            continue;
                        }
                    } else {
                        // Skip methods with unsupported signatures
                        continue;
                    }
                    
                    return result != null ? result.toString() : null;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    System.err.println("Failed to invoke @OnMessage: " + e.getMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    System.err.println("Error processing @OnMessage: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    
    /**
     * Tries to decode a text message to the target type using registered decoders.
     */
    private Object tryDecodeTextMessage(String message, Class<?> targetType, Session session) {
        if (!(session instanceof NettyWebSocketSession)) {
            return null;
        }
        
        NettyWebSocketSession nettySession = (NettyWebSocketSession) session;
        EndpointCodecs codecs = nettySession.getCodecs();
        if (codecs == null) {
            return null;
        }
        
        try {
            return codecs.decodeText(message, targetType);
        } catch (Exception e) {
            System.err.println("Failed to decode message: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Tries to decode a binary message to the target type using registered decoders.
     */
    private Object tryDecodeBinaryMessage(java.nio.ByteBuffer data, Class<?> targetType, Session session) {
        if (!(session instanceof NettyWebSocketSession)) {
            return null;
        }
        
        NettyWebSocketSession nettySession = (NettyWebSocketSession) session;
        EndpointCodecs codecs = nettySession.getCodecs();
        if (codecs == null) {
            return null;
        }
        
        try {
            return codecs.decodeBinary(data, targetType);
        } catch (Exception e) {
            System.err.println("Failed to decode binary message: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Invokes the @OnMessage method on the endpoint instance for binary messages.
     */
    private void invokeOnBinaryMessage(Object endpointInstance, java.nio.ByteBuffer data, Session session) {
        Method[] methods = endpointInstance.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnMessage.class)) {
                try {
                    method.setAccessible(true);
                    Class<?>[] paramTypes = method.getParameterTypes();
                    
                    // Try different parameter combinations for binary messages
                    if (paramTypes.length == 1 && paramTypes[0] == java.nio.ByteBuffer.class) {
                        // Method signature: void onMessage(ByteBuffer data)
                        method.invoke(endpointInstance, data);
                        return;
                    } else if (paramTypes.length == 2 && paramTypes[0] == java.nio.ByteBuffer.class && 
                               paramTypes[1] == Session.class) {
                        // Method signature: void onMessage(ByteBuffer data, Session session)
                        method.invoke(endpointInstance, data, session);
                        return;
                    } else if (paramTypes.length == 2 && paramTypes[0] == Session.class && 
                               paramTypes[1] == java.nio.ByteBuffer.class) {
                        // Method signature: void onMessage(Session session, ByteBuffer data)
                        method.invoke(endpointInstance, session, data);
                        return;
                    } else if (paramTypes.length == 1 && paramTypes[0] == byte[].class) {
                        // Method signature: void onMessage(byte[] data)
                        // Duplicate to avoid modifying the original buffer
                        java.nio.ByteBuffer duplicate = data.duplicate();
                        byte[] byteArray = new byte[duplicate.remaining()];
                        duplicate.get(byteArray);
                        method.invoke(endpointInstance, (Object) byteArray);
                        return;
                    } else if (paramTypes.length == 2 && paramTypes[0] == byte[].class && 
                               paramTypes[1] == Session.class) {
                        // Method signature: void onMessage(byte[] data, Session session)
                        // Duplicate to avoid modifying the original buffer
                        java.nio.ByteBuffer duplicate = data.duplicate();
                        byte[] byteArray = new byte[duplicate.remaining()];
                        duplicate.get(byteArray);
                        method.invoke(endpointInstance, byteArray, session);
                        return;
                    } else if (paramTypes.length == 2 && paramTypes[0] == Session.class && 
                               paramTypes[1] == byte[].class) {
                        // Method signature: void onMessage(Session session, byte[] data)
                        // Duplicate to avoid modifying the original buffer
                        java.nio.ByteBuffer duplicate = data.duplicate();
                        byte[] byteArray = new byte[duplicate.remaining()];
                        duplicate.get(byteArray);
                        method.invoke(endpointInstance, session, byteArray);
                        return;
                    } else if (paramTypes.length == 1 && paramTypes[0] != java.nio.ByteBuffer.class && 
                               paramTypes[0] != byte[].class) {
                        // Method signature with decoder: void onMessage(CustomType message)
                        // Try to decode the binary message using the endpoint's decoders
                        Object decodedMessage = tryDecodeBinaryMessage(data, paramTypes[0], session);
                        if (decodedMessage != null) {
                            method.invoke(endpointInstance, decodedMessage);
                            return;
                        } else {
                            // No decoder found, skip this method
                            continue;
                        }
                    } else if (paramTypes.length == 2 && paramTypes[0] != java.nio.ByteBuffer.class && 
                               paramTypes[0] != byte[].class && paramTypes[0] != Session.class && 
                               paramTypes[1] == Session.class) {
                        // Method signature with decoder: void onMessage(CustomType message, Session session)
                        Object decodedMessage = tryDecodeBinaryMessage(data, paramTypes[0], session);
                        if (decodedMessage != null) {
                            method.invoke(endpointInstance, decodedMessage, session);
                            return;
                        } else {
                            // No decoder found, skip this method
                            continue;
                        }
                    } else if (paramTypes.length == 2 && paramTypes[0] == Session.class && 
                               paramTypes[1] != java.nio.ByteBuffer.class && 
                               paramTypes[1] != byte[].class) {
                        // Method signature with decoder: void onMessage(Session session, CustomType message)
                        Object decodedMessage = tryDecodeBinaryMessage(data, paramTypes[1], session);
                        if (decodedMessage != null) {
                            method.invoke(endpointInstance, session, decodedMessage);
                            return;
                        } else {
                            // No decoder found, skip this method
                            continue;
                        }
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    System.err.println("Failed to invoke @OnMessage for binary: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Invokes the @OnOpen method on the endpoint instance.
     */
    private void invokeOnOpen(Object endpointInstance, Session session) {
        Method[] methods = endpointInstance.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnOpen.class)) {
                try {
                    method.setAccessible(true);
                    Class<?>[] paramTypes = method.getParameterTypes();
                    
                    // Try different parameter combinations
                    if (paramTypes.length == 0) {
                        // Method signature: void onOpen()
                        method.invoke(endpointInstance);
                    } else if (paramTypes.length == 1 && paramTypes[0] == Session.class) {
                        // Method signature: void onOpen(Session session)
                        method.invoke(endpointInstance, session);
                    } else {
                        // Skip methods with unsupported signatures
                        continue;
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    System.err.println("Failed to invoke @OnOpen: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Invokes the @OnClose method on the endpoint instance.
     */
    private void invokeOnClose(Object endpointInstance, Session session) {
        Method[] methods = endpointInstance.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnClose.class)) {
                try {
                    method.setAccessible(true);
                    Class<?>[] paramTypes = method.getParameterTypes();
                    
                    // Try different parameter combinations
                    if (paramTypes.length == 0) {
                        // Method signature: void onClose()
                        method.invoke(endpointInstance);
                    } else if (paramTypes.length == 1 && paramTypes[0] == Session.class) {
                        // Method signature: void onClose(Session session)
                        method.invoke(endpointInstance, session);
                    } else {
                        // Skip methods with unsupported signatures
                        continue;
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    System.err.println("Failed to invoke @OnClose: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Invokes the @OnError method on the endpoint instance.
     */
    private void invokeOnError(Object endpointInstance, Throwable cause, Session session) {
        Method[] methods = endpointInstance.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnError.class)) {
                try {
                    method.setAccessible(true);
                    Class<?>[] paramTypes = method.getParameterTypes();
                    
                    // Try different parameter combinations
                    if (paramTypes.length == 1 && paramTypes[0].isAssignableFrom(Throwable.class)) {
                        // Method signature: void onError(Throwable throwable)
                        method.invoke(endpointInstance, cause);
                    } else if (paramTypes.length == 2 && paramTypes[0] == Session.class && 
                               paramTypes[1].isAssignableFrom(Throwable.class)) {
                        // Method signature: void onError(Session session, Throwable throwable)
                        method.invoke(endpointInstance, session, cause);
                    } else if (paramTypes.length == 2 && paramTypes[0].isAssignableFrom(Throwable.class) && 
                               paramTypes[1] == Session.class) {
                        // Method signature: void onError(Throwable throwable, Session session)
                        method.invoke(endpointInstance, cause, session);
                    } else {
                        // Skip methods with unsupported signatures
                        continue;
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    System.err.println("Failed to invoke @OnError: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}
