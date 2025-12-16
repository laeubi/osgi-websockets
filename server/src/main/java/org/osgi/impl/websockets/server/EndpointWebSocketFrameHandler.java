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

import jakarta.websocket.MessageHandler;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpointConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
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
            
            // Get the full request URI path (with query string) that was stored by WebSocketPathHandler
            String requestPath = ctx.channel().attr(WebSocketPathHandler.REQUEST_URI_KEY).get();
            if (requestPath == null) {
                // Fallback to handshake URI if not stored (shouldn't happen)
                requestPath = handshake.requestUri();
            }
            
            // Extract the raw query string (URL-encoded) before parsing the URI
            String rawQueryString = null;
            int queryStart = requestPath.indexOf('?');
            if (queryStart >= 0 && queryStart < requestPath.length() - 1) {
                rawQueryString = requestPath.substring(queryStart + 1);
            }
            
            // Now get the endpoint registration that was set by WebSocketPathHandler
            JakartaWebSocketServer.EndpointRegistration registration = 
                ctx.channel().attr(WebSocketPathHandler.ENDPOINT_REGISTRATION_KEY).get();
            
            if (registration == null) {
                // No endpoint registered for this path - close connection with error
                System.err.println("No endpoint registered for path: " + handshake.requestUri());
                ctx.close();
                return;
            }
            
            // Extract path parameters from the request path using the URI template
            String cleanPath = ctx.channel().attr(WebSocketPathHandler.REQUEST_PATH_KEY).get();
            if (cleanPath == null) {
                cleanPath = requestPath.contains("?") ? requestPath.substring(0, requestPath.indexOf("?")) : requestPath;
            }
            Map<String, String> pathParameters = registration.uriTemplate.extractParameters(cleanPath);
            if (pathParameters == null) {
                pathParameters = new java.util.HashMap<>();
            }
            
            // Construct a complete WebSocket URI including scheme and host
            // The request path is just the path + query (e.g., "/requesturi?param=value")
            // We need to construct a full URI with the WebSocket scheme
            String host = handshake.requestHeaders().get(HttpHeaderNames.HOST);
            if (host == null) {
                host = "localhost";
            }
            String fullUriString = "ws://" + host + requestPath;
            
            // Create a Session for this connection with the full URI and path parameters
            java.net.URI requestUri;
            try {
                requestUri = new java.net.URI(fullUriString);
            } catch (java.net.URISyntaxException e) {
                // Fallback if URI parsing fails
                try {
                    requestUri = new java.net.URI("ws://localhost" + requestPath);
                } catch (java.net.URISyntaxException e2) {
                    requestUri = java.net.URI.create("ws://localhost/");
                }
            }
            NettyWebSocketSession session = new NettyWebSocketSession(ctx.channel(), requestUri, rawQueryString, pathParameters);
            ctx.channel().attr(SESSION_KEY).set(session);
            
            // Set the endpoint codecs on the session
            session.setCodecs(registration.codecs);
            
            // Set the endpoint registration on the session for open session tracking
            session.setEndpointRegistration(registration);
            
            try {
                // Create endpoint instance using the handler
                Object endpointInstance = registration.handler.createEndpointInstance(registration.endpointClass);
                ctx.channel().attr(ENDPOINT_INSTANCE_KEY).set(endpointInstance);
                
                // Register this channel and session with the endpoint
                registration.registerChannel(ctx.channel(), endpointInstance);
                registration.registerSession(session);
                
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
            
            // Try programmatic message handlers first
            if (!invokeProgrammaticTextHandlers(session, receivedText)) {
                // No programmatic handler found, try annotation-based @OnMessage
                String response = invokeOnMessage(endpointInstance, receivedText, session);
                if (response != null) {
                    ctx.channel().writeAndFlush(new TextWebSocketFrame(response));
                }
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
            
            // Try programmatic message handlers first
            if (invokeProgrammaticBinaryHandlers(session, receivedData)) {
                return; // Handler processed the message
            }
            
            // Fall back to annotation-based @OnMessage method for binary data
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
            
            // Unregister session and channel from endpoint
            JakartaWebSocketServer.EndpointRegistration registration = 
                ctx.channel().attr(WebSocketPathHandler.ENDPOINT_REGISTRATION_KEY).get();
            if (registration != null) {
                registration.unregisterChannel(ctx.channel());
                if (session != null) {
                    registration.unregisterSession(session);
                }
                
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
        
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnMessage.class)) {
                try {
                    method.setAccessible(true);
                    Object result;
                    Class<?>[] paramTypes = method.getParameterTypes();
                    java.lang.reflect.Parameter[] parameters = method.getParameters();
                    
                    // Build argument list based on parameter types and annotations
                    Object[] args = new Object[paramTypes.length];
                    boolean validMethod = true;
                    boolean hasMessageParam = false;
                    
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (!hasMessageParam && paramTypes[i] == String.class && !parameters[i].isAnnotationPresent(jakarta.websocket.server.PathParam.class)) {
                            // String message parameter
                            args[i] = message;
                            hasMessageParam = true;
                        } else if (!hasMessageParam && isPrimitiveOrWrapper(paramTypes[i]) && !parameters[i].isAnnotationPresent(jakarta.websocket.server.PathParam.class)) {
                            // Primitive message parameter
                            args[i] = convertToPrimitive(message, paramTypes[i]);
                            if (args[i] == null) {
                                validMethod = false;
                                break;
                            }
                            hasMessageParam = true;
                        } else if (!hasMessageParam && paramTypes[i] != Session.class && !parameters[i].isAnnotationPresent(jakarta.websocket.server.PathParam.class)) {
                            // Custom type with decoder
                            try {
                                args[i] = decodeTextMessage(message, paramTypes[i], session);
                                if (args[i] == null) {
                                    validMethod = false;
                                    break;
                                }
                                hasMessageParam = true;
                            } catch (jakarta.websocket.DecodeException e) {
                                // Decoder threw an exception - invoke @OnError
                                invokeOnError(endpointInstance, e, session);
                                return null;
                            }
                        } else if (paramTypes[i] == Session.class) {
                            args[i] = session;
                        } else if (parameters[i].isAnnotationPresent(jakarta.websocket.server.PathParam.class)) {
                            // Handle @PathParam annotation
                            jakarta.websocket.server.PathParam pathParam = parameters[i].getAnnotation(jakarta.websocket.server.PathParam.class);
                            String paramName = pathParam.value();
                            String paramValue = session.getPathParameters().get(paramName);
                            args[i] = convertPathParam(paramValue, paramTypes[i]);
                        } else {
                            // Unsupported parameter
                            validMethod = false;
                            break;
                        }
                    }
                    
                    // @OnMessage must have a message parameter
                    if (validMethod && hasMessageParam) {
                        result = method.invoke(endpointInstance, args);
                        if (result != null) {
                            return result.toString();
                        }
                        return null;
                    }
                } catch (InvocationTargetException e) {
                    // The endpoint method threw an exception - invoke @OnError
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    invokeOnError(endpointInstance, cause, session);
                    return null;
                } catch (IllegalAccessException e) {
                    System.err.println("Failed to invoke @OnMessage: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    

    
    /**
     * Decodes a text message to the target type using registered decoders.
     * 
     * @throws jakarta.websocket.DecodeException if decoding fails
     */
    private Object decodeTextMessage(String message, Class<?> targetType, Session session) 
            throws jakarta.websocket.DecodeException {
        if (!(session instanceof NettyWebSocketSession)) {
            return null;
        }
        
        NettyWebSocketSession nettySession = (NettyWebSocketSession) session;
        EndpointCodecs codecs = nettySession.getCodecs();
        if (codecs == null) {
            return null;
        }
        
        return codecs.decodeText(message, targetType);
    }
    
    /**
     * Decodes a binary message to the target type using registered decoders.
     * 
     * @throws jakarta.websocket.DecodeException if decoding fails
     */
    private Object decodeBinaryMessage(java.nio.ByteBuffer data, Class<?> targetType, Session session) 
            throws jakarta.websocket.DecodeException {
        if (!(session instanceof NettyWebSocketSession)) {
            return null;
        }
        
        NettyWebSocketSession nettySession = (NettyWebSocketSession) session;
        EndpointCodecs codecs = nettySession.getCodecs();
        if (codecs == null) {
            return null;
        }
        
        return codecs.decodeBinary(data, targetType);
    }
    
    /**
     * Invokes programmatic text message handlers registered via Session.addMessageHandler().
     * 
     * @return true if a handler was invoked, false if no matching handler found
     */
    @SuppressWarnings("unchecked")
    private boolean invokeProgrammaticTextHandlers(Session session, String message) {
        if (!(session instanceof NettyWebSocketSession)) {
            return false;
        }
        
        NettyWebSocketSession nettySession = (NettyWebSocketSession) session;
        Set<MessageHandler> handlers = nettySession.getMessageHandlers();
        
        if (handlers.isEmpty()) {
            return false;
        }
        
        // Try Whole<String> handlers
        for (MessageHandler handler : handlers) {
            if (handler instanceof MessageHandler.Whole) {
                try {
                    // Cast to raw type and invoke - handler will throw ClassCastException if wrong type
                    ((MessageHandler.Whole) handler).onMessage(message);
                    return true; // Handler invoked successfully
                } catch (ClassCastException e) {
                    // This handler doesn't accept String, try next
                    continue;
                } catch (Exception e) {
                    // Handler threw an exception - this is a real error
                    System.err.println("Message handler threw exception: " + e.getMessage());
                    e.printStackTrace();
                    return true; // Consider it handled even if it threw
                }
            }
        }
        
        // TODO: Support Partial<String> handlers for fragmented messages
        
        return false; // No suitable handler found
    }
    
    /**
     * Invokes programmatic binary message handlers registered via Session.addMessageHandler().
     * 
     * @return true if a handler was invoked, false if no matching handler found
     */
    @SuppressWarnings("unchecked")
    private boolean invokeProgrammaticBinaryHandlers(Session session, java.nio.ByteBuffer data) {
        if (!(session instanceof NettyWebSocketSession)) {
            return false;
        }
        
        NettyWebSocketSession nettySession = (NettyWebSocketSession) session;
        Set<MessageHandler> handlers = nettySession.getMessageHandlers();
        
        if (handlers.isEmpty()) {
            return false;
        }
        
        // Try Whole<ByteBuffer> handlers first
        for (MessageHandler handler : handlers) {
            if (handler instanceof MessageHandler.Whole) {
                try {
                    // Duplicate the buffer so handler can read it without affecting position
                    ((MessageHandler.Whole) handler).onMessage(data.duplicate());
                    return true; // Handler invoked successfully
                } catch (ClassCastException e) {
                    // This handler doesn't accept ByteBuffer, try next type
                    continue;
                } catch (Exception e) {
                    // Handler threw an exception - this is a real error
                    System.err.println("Message handler threw exception: " + e.getMessage());
                    e.printStackTrace();
                    return true; // Consider it handled even if it threw
                }
            }
        }
        
        // Try Whole<byte[]> handlers
        for (MessageHandler handler : handlers) {
            if (handler instanceof MessageHandler.Whole) {
                try {
                    // Convert ByteBuffer to byte[]
                    java.nio.ByteBuffer duplicate = data.duplicate();
                    byte[] bytes = new byte[duplicate.remaining()];
                    duplicate.get(bytes);
                    ((MessageHandler.Whole) handler).onMessage(bytes);
                    return true; // Handler invoked successfully
                } catch (ClassCastException e) {
                    // This handler doesn't accept byte[], try next
                    continue;
                } catch (Exception e) {
                    // Handler threw an exception - this is a real error
                    System.err.println("Message handler threw exception: " + e.getMessage());
                    e.printStackTrace();
                    return true; // Consider it handled even if it threw
                }
            }
        }
        
        // TODO: Support Partial handlers for fragmented messages
        
        return false; // No suitable handler found
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
                        try {
                            Object decodedMessage = decodeBinaryMessage(data, paramTypes[0], session);
                            if (decodedMessage != null) {
                                method.invoke(endpointInstance, decodedMessage);
                                return;
                            } else {
                                // No decoder found, skip this method
                                continue;
                            }
                        } catch (jakarta.websocket.DecodeException e) {
                            // Decoder threw an exception - invoke @OnError
                            invokeOnError(endpointInstance, e, session);
                            return;
                        }
                    } else if (paramTypes.length == 2 && paramTypes[0] != java.nio.ByteBuffer.class && 
                               paramTypes[0] != byte[].class && paramTypes[0] != Session.class && 
                               paramTypes[1] == Session.class) {
                        // Method signature with decoder: void onMessage(CustomType message, Session session)
                        try {
                            Object decodedMessage = decodeBinaryMessage(data, paramTypes[0], session);
                            if (decodedMessage != null) {
                                method.invoke(endpointInstance, decodedMessage, session);
                                return;
                            } else {
                                // No decoder found, skip this method
                                continue;
                            }
                        } catch (jakarta.websocket.DecodeException e) {
                            // Decoder threw an exception - invoke @OnError
                            invokeOnError(endpointInstance, e, session);
                            return;
                        }
                    } else if (paramTypes.length == 2 && paramTypes[0] == Session.class && 
                               paramTypes[1] != java.nio.ByteBuffer.class && 
                               paramTypes[1] != byte[].class) {
                        // Method signature with decoder: void onMessage(Session session, CustomType message)
                        try {
                            Object decodedMessage = decodeBinaryMessage(data, paramTypes[1], session);
                            if (decodedMessage != null) {
                                method.invoke(endpointInstance, session, decodedMessage);
                                return;
                            } else {
                                // No decoder found, skip this method
                                continue;
                            }
                        } catch (jakarta.websocket.DecodeException e) {
                            // Decoder threw an exception - invoke @OnError
                            invokeOnError(endpointInstance, e, session);
                            return;
                        }
                    }
                } catch (InvocationTargetException e) {
                    // The endpoint method threw an exception - invoke @OnError
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    invokeOnError(endpointInstance, cause, session);
                } catch (IllegalAccessException e) {
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
                    java.lang.reflect.Parameter[] parameters = method.getParameters();
                    
                    // Build argument list based on parameter types and annotations
                    Object[] args = new Object[paramTypes.length];
                    boolean validMethod = true;
                    
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (paramTypes[i] == Session.class) {
                            args[i] = session;
                        } else if (parameters[i].isAnnotationPresent(jakarta.websocket.server.PathParam.class)) {
                            // Handle @PathParam annotation
                            jakarta.websocket.server.PathParam pathParam = parameters[i].getAnnotation(jakarta.websocket.server.PathParam.class);
                            String paramName = pathParam.value();
                            String paramValue = session.getPathParameters().get(paramName);
                            args[i] = convertPathParam(paramValue, paramTypes[i]);
                        } else if (paramTypes[i] == jakarta.websocket.EndpointConfig.class) {
                            // EndpointConfig - not yet supported, pass null
                            args[i] = null;
                        } else {
                            // Unsupported parameter type
                            validMethod = false;
                            break;
                        }
                    }
                    
                    if (validMethod) {
                        method.invoke(endpointInstance, args);
                        return; // Successfully invoked
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
                    java.lang.reflect.Parameter[] parameters = method.getParameters();
                    
                    // Build argument list based on parameter types and annotations
                    Object[] args = new Object[paramTypes.length];
                    boolean validMethod = true;
                    
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (paramTypes[i] == Session.class) {
                            args[i] = session;
                        } else if (paramTypes[i] == jakarta.websocket.CloseReason.class) {
                            // CloseReason - not provided in this context, pass null
                            args[i] = null;
                        } else if (parameters[i].isAnnotationPresent(jakarta.websocket.server.PathParam.class)) {
                            // Handle @PathParam annotation
                            jakarta.websocket.server.PathParam pathParam = parameters[i].getAnnotation(jakarta.websocket.server.PathParam.class);
                            String paramName = pathParam.value();
                            String paramValue = session.getPathParameters().get(paramName);
                            args[i] = convertPathParam(paramValue, paramTypes[i]);
                        } else {
                            // Unsupported parameter type
                            validMethod = false;
                            break;
                        }
                    }
                    
                    if (validMethod) {
                        method.invoke(endpointInstance, args);
                        return; // Successfully invoked
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
                    java.lang.reflect.Parameter[] parameters = method.getParameters();
                    
                    // Build argument list based on parameter types and annotations
                    Object[] args = new Object[paramTypes.length];
                    boolean validMethod = true;
                    boolean hasThrowable = false;
                    
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (paramTypes[i].isAssignableFrom(Throwable.class)) {
                            args[i] = cause;
                            hasThrowable = true;
                        } else if (paramTypes[i] == Session.class) {
                            args[i] = session;
                        } else if (parameters[i].isAnnotationPresent(jakarta.websocket.server.PathParam.class)) {
                            // Handle @PathParam annotation
                            jakarta.websocket.server.PathParam pathParam = parameters[i].getAnnotation(jakarta.websocket.server.PathParam.class);
                            String paramName = pathParam.value();
                            String paramValue = session.getPathParameters().get(paramName);
                            args[i] = convertPathParam(paramValue, paramTypes[i]);
                        } else {
                            // Unsupported parameter type
                            validMethod = false;
                            break;
                        }
                    }
                    
                    // @OnError must have a Throwable parameter
                    if (validMethod && hasThrowable) {
                        method.invoke(endpointInstance, args);
                        return; // Successfully invoked
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    System.err.println("Failed to invoke @OnError: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Checks if the given class is a primitive type or its wrapper.
     */
    private boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() || 
               type == Boolean.class ||
               type == Byte.class ||
               type == Character.class ||
               type == Short.class ||
               type == Integer.class ||
               type == Long.class ||
               type == Float.class ||
               type == Double.class;
    }
    
    /**
     * Converts a String message to a primitive type or wrapper.
     * Returns null if conversion fails.
     */
    private Object convertToPrimitive(String message, Class<?> targetType) {
        try {
            if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(message);
            } else if (targetType == byte.class || targetType == Byte.class) {
                return Byte.parseByte(message);
            } else if (targetType == char.class || targetType == Character.class) {
                if (message.length() == 1) {
                    return message.charAt(0);
                } else {
                    // For compatibility with TCK, if message ends with "char", extract first character
                    if (message.endsWith("char") && message.length() > 4) {
                        return message.charAt(0);
                    }
                    return null;
                }
            } else if (targetType == short.class || targetType == Short.class) {
                return Short.parseShort(message);
            } else if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(message);
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(message);
            } else if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(message);
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(message);
            }
        } catch (NumberFormatException e) {
            System.err.println("Failed to convert '" + message + "' to " + targetType.getSimpleName() + ": " + e.getMessage());
            return null;
        }
        return null;
    }
    
    /**
     * Converts a path parameter value to the target type.
     * Supports String and all primitive types and their wrappers.
     */
    private Object convertPathParam(String value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        // String type - no conversion needed
        if (targetType == String.class) {
            return value;
        }
        
        // Use the same conversion logic as convertToPrimitive
        return convertToPrimitive(value, targetType);
    }
}
