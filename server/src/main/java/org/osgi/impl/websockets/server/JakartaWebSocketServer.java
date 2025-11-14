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
                        pipeline.addLast(new WebSocketServerProtocolHandler("/ws", null, true));
                        pipeline.addLast(new WebSocketFrameHandler());
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
