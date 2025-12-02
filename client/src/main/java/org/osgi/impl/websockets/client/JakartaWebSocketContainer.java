package org.osgi.impl.websockets.client;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of the Jakarta WebSocket Container for client connections.
 * This implementation uses Java's built-in HttpClient WebSocket support internally.
 * 
 * <p>Supported features:</p>
 * <ul>
 *   <li>Connection to WebSocket endpoints using annotated client endpoints</li>
 *   <li>@ClientEndpoint annotation with @OnOpen, @OnMessage, @OnClose, @OnError handlers</li>
 *   <li>Encoders and decoders specified in @ClientEndpoint annotation</li>
 *   <li>Text and binary message support</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * @ClientEndpoint
 * public class MyClient {
 *     @OnMessage
 *     public void onMessage(String message) {
 *         System.out.println("Received: " + message);
 *     }
 * }
 * 
 * WebSocketContainer container = new JakartaWebSocketContainer();
 * Session session = container.connectToServer(new MyClient(), new URI("ws://localhost:8080/echo"));
 * session.getBasicRemote().sendText("Hello!");
 * }</pre>
 *
 * @see <a href="https://jakarta.ee/specifications/websocket/2.2/jakarta-websocket-spec-2.2">Jakarta WebSocket 2.2 Specification</a>
 */
public class JakartaWebSocketContainer implements WebSocketContainer {
    
    private static final long DEFAULT_TIMEOUT = 30_000; // 30 seconds default timeout
    
    private final HttpClient httpClient;
    private final Set<Session> activeSessions;
    private long connectToServerTimeout = DEFAULT_TIMEOUT;
    private long defaultMaxSessionIdleTimeout = 0; // 0 means no timeout
    private int defaultMaxBinaryMessageBufferSize = 8192;
    private int defaultMaxTextMessageBufferSize = 8192;
    
    /**
     * Creates a new WebSocket container with default settings.
     */
    public JakartaWebSocketContainer() {
        this.httpClient = HttpClient.newHttpClient();
        this.activeSessions = ConcurrentHashMap.newKeySet();
    }
    
    @Override
    public long getDefaultAsyncSendTimeout() {
        return 0; // Not implemented - asynchronous send operations do not timeout
    }
    
    @Override
    public void setAsyncSendTimeout(long timeout) {
        // Not implemented - the Java HttpClient doesn't support send timeout
    }
    
    @Override
    public Session connectToServer(Object annotatedEndpointInstance, URI path) 
            throws DeploymentException, IOException {
        if (annotatedEndpointInstance == null) {
            throw new IllegalArgumentException("Endpoint instance cannot be null");
        }
        if (path == null) {
            throw new IllegalArgumentException("URI cannot be null");
        }
        
        Class<?> endpointClass = annotatedEndpointInstance.getClass();
        ClientEndpoint annotation = endpointClass.getAnnotation(ClientEndpoint.class);
        if (annotation == null) {
            throw new DeploymentException("Endpoint instance must be annotated with @ClientEndpoint: " + 
                endpointClass.getName());
        }
        
        // Validate the endpoint
        ClientEndpointValidator.validateEndpoint(endpointClass);
        
        // Create ClientEndpointConfig from annotation
        ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
            .encoders(java.util.Arrays.asList(annotation.encoders()))
            .decoders(java.util.Arrays.asList(annotation.decoders()))
            .preferredSubprotocols(java.util.Arrays.asList(annotation.subprotocols()))
            .build();
        
        return connectInternal(annotatedEndpointInstance, endpointClass, path, config);
    }
    
    @Override
    public Session connectToServer(Class<?> annotatedEndpointClass, URI path) 
            throws DeploymentException, IOException {
        if (annotatedEndpointClass == null) {
            throw new IllegalArgumentException("Endpoint class cannot be null");
        }
        if (path == null) {
            throw new IllegalArgumentException("URI cannot be null");
        }
        
        ClientEndpoint annotation = annotatedEndpointClass.getAnnotation(ClientEndpoint.class);
        if (annotation == null) {
            throw new DeploymentException("Endpoint class must be annotated with @ClientEndpoint: " + 
                annotatedEndpointClass.getName());
        }
        
        // Validate the endpoint
        ClientEndpointValidator.validateEndpoint(annotatedEndpointClass);
        
        // Create instance
        Object endpointInstance;
        try {
            endpointInstance = annotatedEndpointClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new DeploymentException("Failed to create endpoint instance: " + e.getMessage(), e);
        }
        
        // Create ClientEndpointConfig from annotation
        ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
            .encoders(java.util.Arrays.asList(annotation.encoders()))
            .decoders(java.util.Arrays.asList(annotation.decoders()))
            .preferredSubprotocols(java.util.Arrays.asList(annotation.subprotocols()))
            .build();
        
        return connectInternal(endpointInstance, annotatedEndpointClass, path, config);
    }
    
    @Override
    public Session connectToServer(Endpoint endpoint, ClientEndpointConfig config, URI path) 
            throws DeploymentException, IOException {
        throw new DeploymentException("Programmatic endpoints are not yet supported. " +
            "Please use annotated @ClientEndpoint classes.");
    }
    
    @Override
    public Session connectToServer(Class<? extends Endpoint> endpointClass, 
            ClientEndpointConfig config, URI path) throws DeploymentException, IOException {
        throw new DeploymentException("Programmatic endpoints are not yet supported. " +
            "Please use annotated @ClientEndpoint classes.");
    }
    
    /**
     * Internal method to establish WebSocket connection.
     */
    private Session connectInternal(Object endpointInstance, Class<?> endpointClass, 
            URI path, ClientEndpointConfig config) throws DeploymentException, IOException {
        
        // Create codec handler for encoders/decoders
        ClientEndpointCodecs codecs;
        try {
            codecs = new ClientEndpointCodecs(endpointClass, config);
        } catch (InstantiationException e) {
            throw new DeploymentException("Failed to initialize encoders/decoders: " + e.getMessage(), e);
        }
        
        // Create the session wrapper that will be returned
        JakartaClientSession jakartaSession = new JakartaClientSession(
            this, path, config, endpointInstance, codecs);
        
        // Create WebSocket listener that bridges to the endpoint
        ClientEndpointHandler handler = new ClientEndpointHandler(
            jakartaSession, endpointInstance, codecs);
        
        try {
            // Build and connect the WebSocket using Java's HttpClient
            CompletableFuture<WebSocket> webSocketFuture = httpClient.newWebSocketBuilder()
                .buildAsync(path, handler);
            
            // Wait for connection with timeout
            WebSocket webSocket = webSocketFuture.get(connectToServerTimeout, TimeUnit.MILLISECONDS);
            
            // Set the underlying WebSocket in the session
            jakartaSession.setWebSocket(webSocket);
            
            // Track the session
            activeSessions.add(jakartaSession);
            jakartaSession.setOnCloseCallback(() -> activeSessions.remove(jakartaSession));
            
            // Invoke @OnOpen handler
            handler.invokeOnOpen();
            
            return jakartaSession;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Connection interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new DeploymentException("Failed to connect to server: " + cause.getMessage(), cause);
        } catch (TimeoutException e) {
            throw new IOException("Connection timeout after " + connectToServerTimeout + " ms", e);
        }
    }
    
    @Override
    public long getDefaultMaxSessionIdleTimeout() {
        return defaultMaxSessionIdleTimeout;
    }
    
    @Override
    public void setDefaultMaxSessionIdleTimeout(long timeout) {
        this.defaultMaxSessionIdleTimeout = timeout;
    }
    
    @Override
    public int getDefaultMaxBinaryMessageBufferSize() {
        return defaultMaxBinaryMessageBufferSize;
    }
    
    @Override
    public void setDefaultMaxBinaryMessageBufferSize(int size) {
        this.defaultMaxBinaryMessageBufferSize = size;
    }
    
    @Override
    public int getDefaultMaxTextMessageBufferSize() {
        return defaultMaxTextMessageBufferSize;
    }
    
    @Override
    public void setDefaultMaxTextMessageBufferSize(int size) {
        this.defaultMaxTextMessageBufferSize = size;
    }
    
    @Override
    public Set<Extension> getInstalledExtensions() {
        return Collections.emptySet();
    }
    
    /**
     * Sets the timeout for connecting to a server.
     * 
     * @param timeout the timeout in milliseconds
     */
    public void setConnectToServerTimeout(long timeout) {
        this.connectToServerTimeout = timeout;
    }
    
    /**
     * Gets the timeout for connecting to a server.
     * 
     * @return the timeout in milliseconds
     */
    public long getConnectToServerTimeout() {
        return connectToServerTimeout;
    }
    
    /**
     * Gets all active sessions managed by this container.
     * 
     * @return an unmodifiable set of active sessions
     */
    public Set<Session> getActiveSessions() {
        return Collections.unmodifiableSet(activeSessions);
    }
}
