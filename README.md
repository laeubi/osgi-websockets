# OSGi WebSockets

A clean-room implementation of the [Jakarta WebSocket 2.2 specification](https://jakarta.ee/specifications/websocket/2.2/) for OSGi environments, featuring a custom Netty-based WebSocket server and a standalone WebSocket client.

## Overview

This project provides WebSocket support for OSGi applications through:

- **Server Module**: A standalone, Jakarta WebSocket 2.2-compliant server built on Netty, with no OSGi dependencies
- **Client Module**: A standalone WebSocket client implementation using Java's built-in HttpClient, supporting `@ClientEndpoint` annotated classes
- **OSGi Runtime**: Integration layer that exposes WebSocket capabilities as OSGi services
- **Compliance Tests**: Comprehensive test suite derived from the Jakarta WebSocket 2.2 TCK

## Quick Start

### Building the Project

```bash
# Build server and client modules (always works)
mvn clean install -pl server,client

# Build everything (note: runtime/tck modules require additional dependencies)
mvn clean install
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

### Using the WebSocket Client

```java
// Create a client endpoint
@ClientEndpoint
public class MyClient {
    @OnMessage
    public void onMessage(String message) {
        System.out.println("Received: " + message);
    }
}

// Connect to a server
JakartaWebSocketContainer container = new JakartaWebSocketContainer();
Session session = container.connectToServer(new MyClient(), 
    new URI("ws://localhost:8080/chat"));

// Send a message
session.getBasicRemote().sendText("Hello, Server!");

// Close when done
session.close();
```

## Project Structure

- **`server/`** - Netty-based WebSocket server (standalone, Java 11)
- **`client/`** - WebSocket client with `WebSocketContainer` implementation (standalone, Java 11)
- **`runtime/`** - OSGi runtime with Tyrus integration (Java 17, requires additional dependencies)
- **`tck/`** - OSGi integration tests (requires additional dependencies)
- **`compliance/`** - Jakarta WebSocket TCK compliance tests
- **`websocket-tck/`** - TCK artifacts and documentation

## Documentation

- [Server Module README](server/README.md) - Detailed server documentation and usage examples
- [Client Module README](client/README.md) - WebSocket client documentation and usage examples
- [Compliance Testing README](compliance/README.md) - TCK compliance status and testing plan

## Requirements

- Java 11+ (server and client modules)
- Java 17+ (OSGi runtime modules)
- Maven 3.9+

## License

This project is licensed under the [Eclipse Public License 2.0](LICENSE).
