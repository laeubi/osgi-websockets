# WebSocket Client Module

This module provides a clean room implementation of the Jakarta WebSocket 2.2 Container API for client connections, using Java's built-in HttpClient WebSocket support.

## Overview

The client implementation provides a `WebSocketContainer` that allows client applications to connect to WebSocket servers using annotated endpoint classes (`@ClientEndpoint`). It is built from scratch without relying on external WebSocket implementations like Tyrus.

## Features

- **Jakarta WebSocket 2.2 Compliant**: Implements the `WebSocketContainer` interface from the Jakarta WebSocket specification
- **Annotation-Based Endpoints**: Full support for `@ClientEndpoint` annotated classes
- **Lifecycle Callbacks**: Support for `@OnOpen`, `@OnMessage`, `@OnClose`, and `@OnError` annotations
- **Encoder/Decoder Support**: Custom encoders and decoders for message serialization
- **Text and Binary Messages**: Support for both text and binary WebSocket messages
- **Async and Sync Operations**: Both synchronous and asynchronous message sending
- **Java 11+**: Uses Java's built-in `HttpClient` WebSocket API (no external dependencies)

## Usage

### Basic Client Setup

```java
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;

// Define a client endpoint
@ClientEndpoint
public class MyClientEndpoint {
    
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Connected to server");
    }
    
    @OnMessage
    public void onMessage(String message) {
        System.out.println("Received: " + message);
    }
}

// Connect to a server
JakartaWebSocketContainer container = new JakartaWebSocketContainer();
Session session = container.connectToServer(new MyClientEndpoint(), 
    new URI("ws://localhost:8080/chat"));

// Send messages
session.getBasicRemote().sendText("Hello, Server!");

// Close when done
session.close();
```

### Using Encoders and Decoders

```java
// Custom message class
public class ChatMessage {
    private String user;
    private String content;
    // getters and setters
}

// Text encoder
public class ChatMessageEncoder implements Encoder.Text<ChatMessage> {
    @Override
    public String encode(ChatMessage message) throws EncodeException {
        return message.getUser() + ":" + message.getContent();
    }
    
    @Override
    public void init(EndpointConfig config) {}
    
    @Override
    public void destroy() {}
}

// Text decoder
public class ChatMessageDecoder implements Decoder.Text<ChatMessage> {
    @Override
    public ChatMessage decode(String s) throws DecodeException {
        String[] parts = s.split(":", 2);
        ChatMessage msg = new ChatMessage();
        msg.setUser(parts[0]);
        msg.setContent(parts[1]);
        return msg;
    }
    
    @Override
    public boolean willDecode(String s) {
        return s != null && s.contains(":");
    }
    
    @Override
    public void init(EndpointConfig config) {}
    
    @Override
    public void destroy() {}
}

// Client endpoint with encoders/decoders
@ClientEndpoint(
    encoders = { ChatMessageEncoder.class },
    decoders = { ChatMessageDecoder.class }
)
public class ChatClient {
    
    @OnMessage
    public void onMessage(ChatMessage message) {
        System.out.println(message.getUser() + " says: " + message.getContent());
    }
}
```

### Async Message Sending

```java
@ClientEndpoint
public class AsyncClient {
    
    private Session session;
    
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
    }
    
    public void sendAsync(String message) {
        // Using SendHandler
        session.getAsyncRemote().sendText(message, result -> {
            if (result.getException() != null) {
                System.err.println("Send failed: " + result.getException());
            } else {
                System.out.println("Message sent successfully");
            }
        });
        
        // Or using Future
        Future<Void> future = session.getAsyncRemote().sendText(message);
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Send failed: " + e);
        }
    }
}
```

### Error Handling

```java
@ClientEndpoint
public class ErrorHandlingClient {
    
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Connected");
    }
    
    @OnMessage
    public void onMessage(String message) {
        System.out.println("Received: " + message);
    }
    
    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket error: " + error.getMessage());
        error.printStackTrace();
    }
    
    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("Connection closed: " + reason.getReasonPhrase());
    }
}
```

## API Reference

### JakartaWebSocketContainer

The main entry point for establishing WebSocket connections.

| Method | Description |
|--------|-------------|
| `connectToServer(Object endpoint, URI path)` | Connect to a server using an annotated endpoint instance |
| `connectToServer(Class<?> endpointClass, URI path)` | Connect to a server using an annotated endpoint class |
| `setConnectToServerTimeout(long timeout)` | Set the connection timeout in milliseconds |
| `getActiveSessions()` | Get all active sessions managed by this container |

### Session

The session represents an active WebSocket connection.

| Method | Description |
|--------|-------------|
| `getBasicRemote()` | Get the synchronous remote endpoint for sending messages |
| `getAsyncRemote()` | Get the asynchronous remote endpoint for sending messages |
| `close()` | Close the WebSocket connection |
| `isOpen()` | Check if the connection is open |
| `getId()` | Get the unique session ID |

## Configuration

### Container Settings

```java
JakartaWebSocketContainer container = new JakartaWebSocketContainer();

// Set connection timeout (default: 30 seconds)
container.setConnectToServerTimeout(60000); // 60 seconds

// Set default buffer sizes
container.setDefaultMaxTextMessageBufferSize(32768);
container.setDefaultMaxBinaryMessageBufferSize(32768);

// Set default session idle timeout
container.setDefaultMaxSessionIdleTimeout(300000); // 5 minutes
```

## Supported Annotations

| Annotation | Description |
|------------|-------------|
| `@ClientEndpoint` | Marks a class as a WebSocket client endpoint |
| `@OnOpen` | Method called when connection is established |
| `@OnMessage` | Method called when a message is received |
| `@OnClose` | Method called when connection is closed |
| `@OnError` | Method called when an error occurs |

## Testing

```bash
# Build and run tests
mvn test

# Run specific test
mvn test -Dtest=WebSocketContainerTest
```

## Dependencies

### Runtime Dependencies
- `jakarta.websocket:jakarta.websocket-client-api:2.2.0` - Jakarta WebSocket Client API
- Java 11+ HttpClient (built-in) - WebSocket transport

### Test Dependencies
- `junit-jupiter-api:5.10.2` - Testing framework
- `osgi-websockets-server` (test scope) - For integration testing with a real server

## Jakarta WebSocket Specification Compliance

This implementation aims to comply with the client portion of the Jakarta WebSocket Specification 2.2.

### Supported Features
- ✅ Client endpoint annotations (`@ClientEndpoint`)
- ✅ Lifecycle callbacks (`@OnOpen`, `@OnMessage`, `@OnClose`, `@OnError`)
- ✅ Text and binary message handling
- ✅ Custom encoders and decoders
- ✅ Session API
- ✅ RemoteEndpoint.Basic
- ✅ RemoteEndpoint.Async
- ✅ Ping/Pong support

### Not Yet Implemented
- ❌ Programmatic endpoints (Endpoint class)
- ❌ ClientEndpointConfig.Configurator
- ❌ Subprotocol negotiation
- ❌ Extension support
- ❌ SSL/TLS custom configuration

## License

This project is licensed under the Eclipse Public License 2.0.
