# OSGi WebSockets

A clean-room implementation of the [Jakarta WebSocket 2.2 specification](https://jakarta.ee/specifications/websocket/2.2/) for OSGi environments, featuring a custom Netty-based WebSocket server that provides WebSocket support via the OSGi Whiteboard pattern.

## Overview

This project provides WebSocket support for OSGi applications through:

- **Server Module**: A standalone, Jakarta WebSocket 2.2-compliant server built on Netty, with no OSGi dependencies
- **OSGi Runtime**: Integration layer that exposes WebSocket capabilities as OSGi services
- **Client Module**: OSGi WebSocket client implementation
- **Compliance Tests**: Comprehensive test suite derived from the Jakarta WebSocket 2.2 TCK

## Quick Start

### Building the Project

```bash
# Build everything (note: some modules require missing dependencies)
mvn clean install

# Build and test only the server module (always works)
mvn clean install -pl server
```

### Using the WebSocket Server

```java
// Create and start a WebSocket server
JakartaWebSocketServer server = new JakartaWebSocketServer("localhost", 8080);
server.start();

// Register a WebSocket endpoint
@ServerEndpoint("/chat")
public class ChatEndpoint {
    @OnMessage
    public String onMessage(String message) {
        return "Echo: " + message;
    }
}

WebSocketEndpoint endpoint = server.createEndpoint(ChatEndpoint.class, "/chat", handler);

// Server is now accepting WebSocket connections at ws://localhost:8080/chat
```

## Project Structure

- **`server/`** - Netty-based WebSocket server (standalone, Java 11)
- **`runtime/`** - OSGi runtime with Tyrus integration (Java 17)
- **`client/`** - OSGi WebSocket client (Java 17)
- **`tck/`** - OSGi integration tests
- **`compliance/`** - Jakarta WebSocket TCK compliance tests
- **`websocket-tck/`** - TCK artifacts and documentation

## Documentation

- [Server Module README](server/README.md) - Detailed server documentation and usage examples
- [Compliance Testing README](compliance/README.md) - TCK compliance status and testing plan

## Requirements

- Java 11+ (server module)
- Java 17+ (OSGi modules)
- Maven 3.9+

## License

This project is licensed under the [Eclipse Public License 2.0](LICENSE).
