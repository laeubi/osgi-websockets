# WebSocket Server Module

This module provides a clean room implementation of a WebSocket server using Netty and Jakarta WebSocket APIs.

## Overview

The server implementation is built from scratch without relying on Tyrus or other heavyweight WebSocket implementations. This gives us complete control over the server behavior and better integration with OSGi environments.

## Architecture

### Key Components

1. **JakartaWebSocketServer**: The main server class that handles server lifecycle (start/stop) and endpoint registration
   - Uses Netty's NIO event loop groups for efficient I/O handling
   - Configurable hostname and port
   - Thread-safe server state management
   - Dynamic endpoint registration via the `createEndpoint` API

2. **EndpointHandler**: Interface for controlling endpoint instance creation and lifecycle
   - Allows custom control over endpoint instantiation
   - Provides callbacks for session lifecycle events

3. **WebSocketEndpoint**: Represents a registered endpoint
   - Encapsulates endpoint registration
   - Provides `dispose()` method to unregister and clean up

4. **EndpointWebSocketFrameHandler**: Handles WebSocket frame processing
   - Dispatches messages to Jakarta WebSocket endpoints
   - Supports `@ServerEndpoint`, `@OnOpen`, `@OnMessage`, `@OnClose`, and `@OnError` annotations

### Technology Stack

- **Netty 4.2.7.Final**: High-performance asynchronous I/O framework
- **Jakarta WebSocket API 2.2.0**: Standard API for WebSocket communication
- **Java 11**: Minimum Java version for compatibility

## Usage

### Basic Server Setup

```java
// Create a server instance
JakartaWebSocketServer server = new JakartaWebSocketServer("localhost", 8888);

// Start the server
server.start();

// Server is now ready to accept WebSocket connections

// Stop the server when done
server.stop();
```

### Registering Endpoints

The server supports dynamic endpoint registration using the `createEndpoint` API:

```java
// Define an endpoint handler
EndpointHandler handler = new EndpointHandler() {
    @Override
    public <T> T createEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        // Create and return an instance of the endpoint
        try {
            return endpointClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new InstantiationException("Failed to create instance: " + e.getMessage());
        }
    }
    
    @Override
    public void sessionEnded(Object endpointInstance) {
        // Perform cleanup when a session ends
        // This is called after @OnClose
    }
};

// Register the endpoint
WebSocketEndpoint endpoint = server.createEndpoint(MyEndpoint.class, "/mypath", handler);

// The endpoint is now registered and accepting connections at ws://localhost:8888/mypath

// When done, dispose of the endpoint
endpoint.dispose(); // Closes all active sessions and unregisters the endpoint
```

### Creating Jakarta WebSocket Endpoints

Define your endpoint using Jakarta WebSocket annotations:

```java
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.OnClose;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/chat")
public class ChatEndpoint {
    
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("New connection: " + session.getId());
    }
    
    @OnMessage
    public String onMessage(String message, Session session) {
        return "Echo: " + message;
    }
    
    @OnClose
    public void onClose(Session session) {
        System.out.println("Connection closed: " + session.getId());
    }
}
```

## Testing

Tests use Java 11's built-in `HttpClient` WebSocket support to verify server functionality. This approach ensures we only test the external behavior rather than implementation details.

### Running Tests

```bash
mvn test
```

### Test Coverage

Current tests verify:
1. Server starts and stops correctly
2. Constructor parameter validation
3. WebSocket connection establishment
4. Message handling with Jakarta WebSocket annotations
5. Endpoint registration and removal
6. Session lifecycle management
7. Custom configurators
8. The new `createEndpoint` API with `EndpointHandler`

## Jakarta WebSocket Specification Compliance

This implementation aims to comply with the [Jakarta WebSocket Specification 2.2](https://jakarta.ee/specifications/websocket/2.2/jakarta-websocket-spec-2.2).

### Current Compliance

- ✅ WebSocket handshake (handled by Netty's WebSocketServerProtocolHandler)
- ✅ Text frame handling
- ✅ Endpoint annotations (@ServerEndpoint, @OnMessage, @OnOpen, @OnClose, @OnError)
- ✅ Session management with Jakarta WebSocket Session API
- ✅ Dynamic endpoint registration and removal
- ⚠️ Binary frames (not yet implemented)
- ⚠️ Encoders/Decoders (not yet implemented)

### Future Enhancements

1. **Binary Frame Support**: Handle binary WebSocket frames
2. **Encoders/Decoders**: Support for custom message encoding/decoding
3. **Subprotocol Negotiation**: Support WebSocket subprotocol selection
4. **Extension Support**: Implement WebSocket extensions (e.g., compression)
5. **Connection Limits**: Add configurable connection limits and resource management
6. **Security**: Add TLS/SSL support and authentication mechanisms

## Configuration

Current configuration options:
- **Hostname**: Network interface to bind (e.g., "localhost", "0.0.0.0")
- **Port**: TCP port number (1-65535)

Future configuration options (planned):
- Max frame size
- Idle timeout
- Max connections
- SSL/TLS settings
- Subprotocol list

## Dependencies

### Runtime Dependencies
- `io.netty:netty-all:4.2.7.Final` - Async I/O framework
- `jakarta.websocket:jakarta.websocket-api:2.2.0` - WebSocket API

### Test Dependencies
- `junit-jupiter-api:5.10.2` - Testing framework
- Java 11 HttpClient (built-in) - WebSocket client for testing

## Build

```bash
# Build the module
mvn clean install

# Skip tests
mvn clean install -DskipTests
```

## Notes

- Endpoints are dynamically registered using the `createEndpoint` API
- Log output goes to stdout/stderr
- The implementation supports Jakarta WebSocket annotations
- Thread safety is ensured through Netty's event loop model
- The old `addEndpoint`/`removeEndpoint` methods are still available for backward compatibility but new code should use `createEndpoint`

## Contributing

When extending this implementation:
1. Ensure compliance with Jakarta WebSocket 2.2 specification for any implemented features
2. Add corresponding tests using Java HttpClient
3. Update this README with new features and configuration options
4. Test both happy path and error scenarios
