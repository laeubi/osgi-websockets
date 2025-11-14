# WebSocket Server Module

This module provides a clean room implementation of a WebSocket server using Netty and Jakarta WebSocket APIs.

## Overview

The server implementation is built from scratch without relying on Tyrus or other heavyweight WebSocket implementations. This gives us complete control over the server behavior and better integration with OSGi environments.

## Architecture

### Key Components

1. **JakartaWebSocketServer**: The main server class that handles server lifecycle (start/stop)
   - Uses Netty's NIO event loop groups for efficient I/O handling
   - Configurable hostname and port
   - Thread-safe server state management

2. **WebSocketFrameHandler**: Handles WebSocket frame processing
   - Currently implements a simple echo endpoint for testing
   - Can be extended to support Jakarta WebSocket endpoint annotations

### Technology Stack

- **Netty 4.1.107.Final**: High-performance asynchronous I/O framework
  - Note: The issue specified version "4.2.7.Final" but Netty 4.2.x doesn't exist. The 4.1.x series is the stable, production-ready version as of 2024/2025.
- **Jakarta WebSocket API 2.2.0**: Standard API for WebSocket communication
- **Java 11**: Minimum Java version for compatibility

## Usage

### Basic Server Setup

```java
// Create a server instance
JakartaWebSocketServer server = new JakartaWebSocketServer("localhost", 8888);

// Start the server
server.start();

// Server is now ready to accept WebSocket connections at ws://localhost:8888/ws

// Stop the server when done
server.stop();
```

### Current Endpoint

The server currently implements a dummy echo endpoint at `/ws` that:
- Accepts WebSocket connections
- Echoes back any text message received
- Responds with "Echo: " prefix + original message

Example client interaction:
```
Client sends: "Hello"
Server responds: "Echo: Hello"
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
4. Message echo functionality
5. Multiple message handling
6. Server state management (cannot start twice)

## Jakarta WebSocket Specification Compliance

This implementation aims to comply with the [Jakarta WebSocket Specification 2.2](https://jakarta.ee/specifications/websocket/2.2/jakarta-websocket-spec-2.2).

### Current Compliance

- ✅ WebSocket handshake (handled by Netty's WebSocketServerProtocolHandler)
- ✅ Text frame handling
- ⚠️ Binary frames (not yet implemented)
- ⚠️ Endpoint annotations (@ServerEndpoint, @OnMessage, etc.) (planned)
- ⚠️ Session management (planned)
- ⚠️ Encoders/Decoders (planned)

### Future Enhancements

1. **Jakarta Annotations Support**: Implement support for `@ServerEndpoint`, `@OnOpen`, `@OnMessage`, `@OnClose`, `@OnError`
2. **Session Management**: Implement `javax.websocket.Session` interface
3. **Binary Frame Support**: Handle binary WebSocket frames
4. **Encoders/Decoders**: Support for custom message encoding/decoding
5. **Subprotocol Negotiation**: Support WebSocket subprotocol selection
6. **Extension Support**: Implement WebSocket extensions (e.g., compression)
7. **Connection Limits**: Add configurable connection limits and resource management
8. **Security**: Add TLS/SSL support and authentication mechanisms

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
- `io.netty:netty-all:4.1.107.Final` - Async I/O framework
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

- The server currently uses a hardcoded path `/ws` for WebSocket connections
- Log output goes to stdout/stderr
- The implementation is designed to be extended with Jakarta WebSocket annotation support
- Thread safety is ensured through Netty's event loop model

## Contributing

When extending this implementation:
1. Ensure compliance with Jakarta WebSocket 2.2 specification for any implemented features
2. Add corresponding tests using Java HttpClient
3. Update this README with new features and configuration options
4. Test both happy path and error scenarios
