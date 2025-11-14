package org.osgi.impl.websockets.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
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
    
    private final JakartaWebSocketServer server;
    private final Map<ChannelHandlerContext, Session> sessions = new ConcurrentHashMap<>();
    
    public EndpointWebSocketFrameHandler(JakartaWebSocketServer server) {
        this.server = server;
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // This is called after the WebSocket handshake completes
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            // Now get the endpoint registration that was set by WebSocketPathHandler
            JakartaWebSocketServer.EndpointRegistration registration = 
                ctx.channel().attr(WebSocketPathHandler.ENDPOINT_REGISTRATION_KEY).get();
            
            if (registration != null) {
                try {
                    // Create endpoint instance using the configurator
                    Object endpointInstance = registration.configurator.getEndpointInstance(registration.endpointClass);
                    ctx.channel().attr(ENDPOINT_INSTANCE_KEY).set(endpointInstance);
                    
                    // Invoke @OnOpen if present
                    invokeOnOpen(endpointInstance);
                } catch (Exception e) {
                    System.err.println("Failed to create endpoint instance: " + e.getMessage());
                    e.printStackTrace();
                }
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
                // No endpoint registered, use echo behavior
                String response = "Echo: " + receivedText;
                ctx.channel().writeAndFlush(new TextWebSocketFrame(response));
                return;
            }
            
            // Find and invoke @OnMessage method
            String response = invokeOnMessage(endpointInstance, receivedText);
            if (response != null) {
                ctx.channel().writeAndFlush(new TextWebSocketFrame(response));
            }
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
            // Invoke @OnClose if present
            invokeOnClose(endpointInstance);
        }
        
        super.channelInactive(ctx);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        
        // Get the endpoint instance
        Object endpointInstance = ctx.channel().attr(ENDPOINT_INSTANCE_KEY).get();
        if (endpointInstance != null) {
            // Invoke @OnError if present
            invokeOnError(endpointInstance, cause);
        }
        
        ctx.close();
    }
    
    /**
     * Invokes the @OnMessage method on the endpoint instance.
     */
    private String invokeOnMessage(Object endpointInstance, String message) {
        Method[] methods = endpointInstance.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnMessage.class)) {
                try {
                    method.setAccessible(true);
                    Object result = method.invoke(endpointInstance, message);
                    return result != null ? result.toString() : null;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    System.err.println("Failed to invoke @OnMessage: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    
    /**
     * Invokes the @OnOpen method on the endpoint instance.
     */
    private void invokeOnOpen(Object endpointInstance) {
        Method[] methods = endpointInstance.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnOpen.class)) {
                try {
                    method.setAccessible(true);
                    // For now, invoke without Session parameter
                    // In a full implementation, we'd create a proper Session object
                    if (method.getParameterCount() == 0) {
                        method.invoke(endpointInstance);
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
    private void invokeOnClose(Object endpointInstance) {
        Method[] methods = endpointInstance.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnClose.class)) {
                try {
                    method.setAccessible(true);
                    // For now, invoke without parameters
                    if (method.getParameterCount() == 0) {
                        method.invoke(endpointInstance);
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
    private void invokeOnError(Object endpointInstance, Throwable cause) {
        Method[] methods = endpointInstance.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnError.class)) {
                try {
                    method.setAccessible(true);
                    // Look for method with Throwable parameter
                    if (method.getParameterCount() == 1 && method.getParameterTypes()[0].isAssignableFrom(Throwable.class)) {
                        method.invoke(endpointInstance, cause);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    System.err.println("Failed to invoke @OnError: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}
