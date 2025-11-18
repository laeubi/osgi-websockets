package org.osgi.impl.websockets.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AttributeKey;

/**
 * Handler that captures the WebSocket request path before the protocol upgrade.
 * This allows dynamic routing to registered endpoints.
 */
public class WebSocketPathHandler extends ChannelInboundHandlerAdapter {
    
    static final AttributeKey<String> REQUEST_PATH_KEY = AttributeKey.valueOf("request_path");
    static final AttributeKey<JakartaWebSocketServer.EndpointRegistration> ENDPOINT_REGISTRATION_KEY = 
        AttributeKey.valueOf("endpoint_registration");
    
    private final JakartaWebSocketServer server;
    
    public WebSocketPathHandler(JakartaWebSocketServer server) {
        this.server = server;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            // Store the request path for later use
            String uri = request.uri();
            String path = uri.contains("?") ? uri.substring(0, uri.indexOf("?")) : uri;
            ctx.channel().attr(REQUEST_PATH_KEY).set(path);
            
            // Check if we have an endpoint registered for this path
            JakartaWebSocketServer.EndpointRegistration registration = server.getEndpointRegistration(path);
            if (registration != null) {
                ctx.channel().attr(ENDPOINT_REGISTRATION_KEY).set(registration);
            }
            
            // Add WebSocket protocol handler for this specific path
            ctx.pipeline().addAfter(ctx.name(), "wsProtocolHandler", 
                new WebSocketServerProtocolHandler(path, null, true));
        }
        
        // Pass the message forward
        ctx.fireChannelRead(msg);
    }
}


