package org.osgi.impl.websockets.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A clean room implementation of a WebSocket server using Netty and Jakarta WebSocket APIs.
 * This server provides basic WebSocket functionality compliant with the Jakarta WebSocket specification.
 * 
 * @see <a href="https://jakarta.ee/specifications/websocket/2.2/jakarta-websocket-spec-2.2">Jakarta WebSocket Specification 2.2</a>
 */
public class JakartaWebSocketServer {
    
    private final String hostname;
    private final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private volatile boolean running = false;
    
    // Map to track registered endpoints: key is effective path
    private final Map<String, EndpointRegistration> endpoints = new ConcurrentHashMap<>();
    
    /**
     * Inner class to hold endpoint registration information
     */
    static class EndpointRegistration {
        final Class<?> endpointClass;
        final String effectivePath;
        final ServerEndpointConfig.Configurator configurator;
        final EndpointHandler handler;
        private final Map<io.netty.channel.Channel, Object> activeChannels = new ConcurrentHashMap<>();
        
        EndpointRegistration(Class<?> endpointClass, String effectivePath, ServerEndpointConfig.Configurator configurator) {
            this.endpointClass = endpointClass;
            this.effectivePath = effectivePath;
            this.configurator = configurator;
            this.handler = null;
        }
        
        EndpointRegistration(Class<?> endpointClass, String effectivePath, EndpointHandler handler) {
            this.endpointClass = endpointClass;
            this.effectivePath = effectivePath;
            this.configurator = null;
            this.handler = handler;
        }
        
        /**
         * Registers an active channel for this endpoint.
         */
        void registerChannel(io.netty.channel.Channel channel, Object endpointInstance) {
            activeChannels.put(channel, endpointInstance);
        }
        
        /**
         * Unregisters a channel from this endpoint.
         */
        void unregisterChannel(io.netty.channel.Channel channel) {
            activeChannels.remove(channel);
        }
        
        /**
         * Closes all active channels for this endpoint.
         */
        void closeAllChannels() {
            for (io.netty.channel.Channel channel : activeChannels.keySet()) {
                if (channel.isActive()) {
                    channel.close();
                }
            }
            activeChannels.clear();
        }
    }
    
    /**
     * Creates a new WebSocket server instance.
     * 
     * @param hostname The hostname to bind the server to
     * @param port The port number to listen on
     */
    public JakartaWebSocketServer(String hostname, int port) {
        if (hostname == null || hostname.trim().isEmpty()) {
            throw new IllegalArgumentException("Hostname cannot be null or empty");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 0 and 65535");
        }
        this.hostname = hostname;
        this.port = port;
    }
    
    /**
     * Starts the WebSocket server.
     * 
     * @throws Exception if the server fails to start
     */
    public void start() throws Exception {
        if (running) {
            throw new IllegalStateException("Server is already running");
        }
        
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        // Add custom path handler to capture the request path
                        pipeline.addLast(new WebSocketPathHandler(JakartaWebSocketServer.this));
                        // The WebSocketServerProtocolHandler will be added dynamically by WebSocketPathHandler
                        pipeline.addLast(new EndpointWebSocketFrameHandler(JakartaWebSocketServer.this));
                    }
                });
            
            serverChannel = bootstrap.bind(hostname, port).sync().channel();
            running = true;
            
            System.out.println("WebSocket server started on " + hostname + ":" + port);
        } catch (Exception e) {
            shutdown();
            throw e;
        }
    }
    
    /**
     * Stops the WebSocket server.
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        try {
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            shutdown();
            running = false;
            System.out.println("WebSocket server stopped");
        }
    }
    
    /**
     * Checks if the server is currently running.
     * 
     * @return true if the server is running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Gets the hostname the server is bound to.
     * 
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }
    
    /**
     * Gets the port number the server is listening on.
     * 
     * @return the port number
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Creates and registers a WebSocket endpoint with the server.
     * This is the new API that replaces addEndpoint/removeEndpoint.
     * 
     * @param endpointClass The endpoint class annotated with @ServerEndpoint
     * @param path The path to register the endpoint at, or null to use the annotation's value
     * @param handler The handler for creating endpoint instances and managing lifecycle (must not be null)
     * @return A WebSocketEndpoint that can be disposed to unregister the endpoint
     * @throws IllegalArgumentException if the endpoint class is not annotated with @ServerEndpoint,
     *         if handler is null, or if an endpoint with the same effective path is already registered
     */
    public WebSocketEndpoint createEndpoint(Class<?> endpointClass, String path, EndpointHandler handler) {
        if (endpointClass == null) {
            throw new IllegalArgumentException("Endpoint class cannot be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("EndpointHandler cannot be null");
        }
        
        // Check if the class has @ServerEndpoint annotation
        ServerEndpoint annotation = endpointClass.getAnnotation(ServerEndpoint.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Endpoint class must be annotated with @ServerEndpoint: " + endpointClass.getName());
        }
        
        // Determine the effective path
        String effectivePath = (path != null) ? path : annotation.value();
        if (effectivePath == null || effectivePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Endpoint path cannot be null or empty");
        }
        
        // Normalize path to ensure it starts with /
        if (!effectivePath.startsWith("/")) {
            effectivePath = "/" + effectivePath;
        }
        
        // Check for duplicate registration
        EndpointRegistration existing = endpoints.get(effectivePath);
        if (existing != null && existing.endpointClass.equals(endpointClass)) {
            throw new IllegalArgumentException("Endpoint class " + endpointClass.getName() + 
                " is already registered at path " + effectivePath);
        }
        
        // Register the endpoint
        EndpointRegistration registration = new EndpointRegistration(endpointClass, effectivePath, handler);
        endpoints.put(effectivePath, registration);
        
        System.out.println("Registered endpoint " + endpointClass.getName() + " at path " + effectivePath);
        
        // Return a WebSocketEndpoint implementation
        return new WebSocketEndpointImpl(registration, effectivePath);
    }
    
    /**
     * Implementation of WebSocketEndpoint that wraps an EndpointRegistration.
     */
    private class WebSocketEndpointImpl implements WebSocketEndpoint {
        private final EndpointRegistration registration;
        private final String path;
        private volatile boolean disposed = false;
        
        WebSocketEndpointImpl(EndpointRegistration registration, String path) {
            this.registration = registration;
            this.path = path;
        }
        
        @Override
        public void dispose() {
            if (disposed) {
                return;
            }
            disposed = true;
            
            // Close all active sessions for this endpoint before removing it
            registration.closeAllChannels();
            
            // Remove the endpoint
            endpoints.remove(path);
            
            System.out.println("Disposed endpoint " + registration.endpointClass.getName() + " from path " + path);
        }
        
        @Override
        public Class<?> getEndpointClass() {
            return registration.endpointClass;
        }
        
        @Override
        public String getPath() {
            return path;
        }
    }
    
    /**
     * Adds a WebSocket endpoint to the server.
     * 
     * @param endpoint The endpoint class annotated with @ServerEndpoint
     * @param path The path to register the endpoint at, or null to use the annotation's value
     * @param configurator The configurator to use for endpoint instance creation, or null to use the annotation's configurator
     * @throws IllegalArgumentException if the endpoint class is not annotated with @ServerEndpoint, 
     *         or if an endpoint with the same effective path is already registered
     */
    public void addEndpoint(Class<?> endpoint, String path, ServerEndpointConfig.Configurator configurator) {
        if (endpoint == null) {
            throw new IllegalArgumentException("Endpoint class cannot be null");
        }
        
        // Check if the class has @ServerEndpoint annotation
        ServerEndpoint annotation = endpoint.getAnnotation(ServerEndpoint.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Endpoint class must be annotated with @ServerEndpoint: " + endpoint.getName());
        }
        
        // Determine the effective path
        String effectivePath = (path != null) ? path : annotation.value();
        if (effectivePath == null || effectivePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Endpoint path cannot be null or empty");
        }
        
        // Normalize path to ensure it starts with /
        if (!effectivePath.startsWith("/")) {
            effectivePath = "/" + effectivePath;
        }
        
        // Determine the configurator to use
        ServerEndpointConfig.Configurator effectiveConfigurator = configurator;
        if (effectiveConfigurator == null) {
            // Check if annotation specifies a custom configurator
            Class<? extends ServerEndpointConfig.Configurator> annotationConfigurator = annotation.configurator();
            if (annotationConfigurator != null && annotationConfigurator != ServerEndpointConfig.Configurator.class) {
                // Annotation has a custom configurator, try to instantiate it
                try {
                    effectiveConfigurator = annotationConfigurator.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalArgumentException("Failed to instantiate configurator from annotation: " + annotationConfigurator.getName(), e);
                }
            }
        }
        
        // Use default configurator if none specified
        if (effectiveConfigurator == null) {
            effectiveConfigurator = new ServerEndpointConfig.Configurator() {
                @Override
                public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                    try {
                        return endpointClass.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new InstantiationException("Failed to create endpoint instance: " + e.getMessage());
                    }
                }
            };
        }
        
        // Check for duplicate registration
        EndpointRegistration existing = endpoints.get(effectivePath);
        if (existing != null && existing.endpointClass.equals(endpoint)) {
            throw new IllegalArgumentException("Endpoint class " + endpoint.getName() + 
                " is already registered at path " + effectivePath);
        }
        
        // Register the endpoint
        EndpointRegistration registration = new EndpointRegistration(endpoint, effectivePath, effectiveConfigurator);
        endpoints.put(effectivePath, registration);
        
        System.out.println("Registered endpoint " + endpoint.getName() + " at path " + effectivePath);
    }
    
    /**
     * Removes a WebSocket endpoint from the server.
     * 
     * @param endpoint The endpoint class to remove
     * @param path The path the endpoint was registered at, or null to use the annotation's value
     * @return true if the endpoint was removed, false if it wasn't registered
     */
    public boolean removeEndpoint(Class<?> endpoint, String path) {
        if (endpoint == null) {
            throw new IllegalArgumentException("Endpoint class cannot be null");
        }
        
        // Check if the class has @ServerEndpoint annotation
        ServerEndpoint annotation = endpoint.getAnnotation(ServerEndpoint.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Endpoint class must be annotated with @ServerEndpoint: " + endpoint.getName());
        }
        
        // Determine the effective path
        String effectivePath = (path != null) ? path : annotation.value();
        if (effectivePath == null || effectivePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Endpoint path cannot be null or empty");
        }
        
        // Normalize path to ensure it starts with /
        if (!effectivePath.startsWith("/")) {
            effectivePath = "/" + effectivePath;
        }
        
        // Check if the endpoint is registered
        EndpointRegistration registration = endpoints.get(effectivePath);
        if (registration == null || !registration.endpointClass.equals(endpoint)) {
            return false;
        }
        
        // Close all active sessions for this endpoint before removing it
        registration.closeAllChannels();
        
        // Remove the endpoint
        endpoints.remove(effectivePath);
        
        System.out.println("Removed endpoint " + endpoint.getName() + " from path " + effectivePath);
        return true;
    }
    
    /**
     * Gets the endpoint registration for a given path.
     * 
     * @param path The path to look up
     * @return The endpoint registration, or null if no endpoint is registered at the path
     */
    EndpointRegistration getEndpointRegistration(String path) {
        return endpoints.get(path);
    }
    
    /**
     * Shuts down the event loop groups.
     */
    private void shutdown() {
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }
}
