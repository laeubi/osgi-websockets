# Jakarta WebSocket TCK Compliance Module

This module is dedicated to replicate the official Jakarta WebSocket 2.2 TCK (Technology Compatibility Kit) against the OSGi WebSocket implementation.

## Overview

The Jakarta WebSocket TCK is a comprehensive test suite designed to verify that implementations comply with the [Jakarta WebSocket 2.2 Specification](https://jakarta.ee/specifications/websocket/2.2/). Running the TCK against our implementation ensures compatibility and correctness.

## Goals

- The original TCK and its sources can be found in `/websocket-tck`
- The TCK is designed for Jakarta EE environments, so adaptation for OSGi may require creative solutions
- Not all tests may be applicable to our use case (e.g., CDI integration tests)
- Some tests may be excluded if they test optional features not implemented
- The goal is maximum compliance while maintaining our architecture

## TCK Test Suite Overview

The Jakarta WebSocket 2.2 TCK contains approximately **737 test methods** across **93 test classes**, organized into three main categories:

### 1. API Tests (47 tests)
Tests for API classes, exceptions, and basic functionality:
- `CloseReason` API (6 tests)
- `ClientEndpointConfig` API (11 tests)
- `ServerEndpointConfig` API (13 tests)
- `WebSocketContainer` API (9 tests)
- Exception handling: `DecodeException` (4), `EncodeException` (2), `DeploymentException` (2)

### 2. End-to-End Tests (545+ tests)
Comprehensive functional tests covering:
- **Endpoint annotations**: @ServerEndpoint, @OnOpen, @OnMessage, @OnClose, @OnError
- **Message handling**: Text (61 tests), Binary (61 tests), Return types (8 tests)
- **Encoders/Decoders**: 
  - User coders (36 tests for annotated, 36 for programmatic)
  - Async with handlers (8 tests)
  - Throwing coders (36 tests)
- **Remote endpoint**: Async (66 tests), Basic (52 tests), with user coders (28 tests)
- **Session API**: Session management (31 tests), Session 1.1 features (41 tests)
- **Server features**: PathParam (25 tests), ServerApplicationConfig (10 tests), Configurator (16 tests)
- **Advanced**: Handshake (12 tests), Container provider (3 tests)

### 3. Negative Deployment Tests (45 test files)
Tests for error conditions and validation:
- Invalid annotation parameters
- Duplicate annotations
- Invalid parameter types
- Missing decoders
- Too many arguments

## Feature Coverage Matrix

Based on our current implementation (server module with 23 passing tests):

### ✅ Fully Supported Features
- Server lifecycle management
- `@ServerEndpoint` annotation
- `@OnOpen`, `@OnMessage`, `@OnClose`, `@OnError` handlers
- Text and binary message handling
- Custom encoders/decoders (Text and Binary types)
- Basic Session API (getId, getBasicRemote, etc.)
- CloseReason API
- Exception handling (DecodeException, EncodeException)

### 🔶 Partially Supported Features
- ServerEndpointConfig and Configurator (basic support, needs testing)
- Async remote endpoint (partial implementation)
- Session properties (getUserProperties implemented, needs full testing)

### ❌ Not Yet Implemented
- `@PathParam` for URI template variables (25 TCK tests)
- Streaming encoders/decoders (TextStream, BinaryStream)
- Session timeout and message size limits
- Subprotocol and extension negotiation
- Handshake customization (HandshakeRequest/Response)
- ServerApplicationConfig for programmatic endpoint discovery
- Open sessions tracking (getOpenSessions)
- Pong frame handling
- SSL/TLS support

### 🚫 Out of Scope (Client Features)
- `@ClientEndpoint` annotation
- ClientEndpointConfig
- WebSocketContainer.connectToServer() method

## Implementation Plan

### Phase 1: Core Compliance Tests (Priority: HIGH)
**Goal**: Verify existing functionality with tests adapted from TCK

1. **Basic API Tests** (~20 tests)
   - Port CloseReason API tests (6 tests)
   - Port ServerEndpointConfig API tests (13 tests)
   - Port exception handling tests (DecodeException, EncodeException)
   - **TCK Sources**: `com/sun/ts/tests/websocket/api/jakarta/websocket/`

2. **Message Handling Tests** (~40 tests)
   - Text message variations (String, primitives, objects with decoders)
   - Binary message variations (byte[], ByteBuffer, objects with decoders)
   - Message return types
   - **TCK Sources**: `com/sun/ts/tests/websocket/ee/jakarta/websocket/clientendpointonmessage/`

3. **Encoder/Decoder Tests** (~30 tests)
   - Text encoder/decoder pairs
   - Binary encoder/decoder pairs
   - Error handling in coders
   - **TCK Sources**: `com/sun/ts/tests/websocket/ee/jakarta/websocket/coder/`

4. **Session API Tests** (~25 tests)
   - Basic remote endpoint send methods
   - Session properties
   - Session lifecycle
   - **TCK Sources**: `com/sun/ts/tests/websocket/ee/jakarta/websocket/session/`

5. **Annotation Handler Tests** (~15 tests)
   - @OnOpen with various parameter combinations
   - @OnClose with CloseReason
   - @OnError with Throwable
   - **TCK Sources**: Multiple endpoint test packages

### Phase 2: Negative Validation Tests (Priority: MEDIUM)
**Goal**: Ensure proper error handling and validation

6. **Invalid Annotation Tests** (~20 tests)
   - Invalid @OnMessage parameters
   - Duplicate annotation handlers
   - Invalid parameter types
   - **TCK Sources**: `com/sun/ts/tests/websocket/negdep/onmessage/`

7. **Deployment Validation Tests** (~10 tests)
   - Invalid endpoint configurations
   - Missing required parameters
   - **TCK Sources**: `com/sun/ts/tests/websocket/negdep/`

### Phase 3: Advanced Features (Priority: LOW)
**Goal**: Implement missing features with corresponding tests

8. **@PathParam Support** (~25 tests from TCK)
   - Implement URI template parameter extraction
   - Add tests for various parameter types (String, primitives, wrappers)
   - **TCK Sources**: `com/sun/ts/tests/websocket/ee/jakarta/websocket/server/pathparam/`

9. **Async Remote Endpoint** (~40 tests)
   - Complete async send implementation
   - SendHandler callbacks
   - Future-based async operations
   - **TCK Sources**: `com/sun/ts/tests/websocket/ee/jakarta/websocket/remoteendpoint/async/`

10. **Session Management** (~20 tests)
    - Implement timeout settings
    - Implement message size limits
    - Add open sessions tracking
    - **TCK Sources**: `com/sun/ts/tests/websocket/ee/jakarta/websocket/session11/`

11. **Streaming Encoders/Decoders** (~20 tests)
    - Implement TextStream and BinaryStream variants
    - Add tests for streaming operations
    - **TCK Sources**: `com/sun/ts/tests/websocket/ee/jakarta/websocket/coder/`

### Phase 4: Optional Features (Priority: OPTIONAL)
**Goal**: Add features for complete specification coverage

12. **Handshake Customization** (~10 tests)
    - HandshakeRequest/Response API
    - Custom headers
    - **TCK Sources**: `com/sun/ts/tests/websocket/ee/jakarta/websocket/handshakeresponse/`

13. **Subprotocol Support**
    - Subprotocol negotiation
    - Sec-WebSocket-Protocol header handling

14. **Extension Support**
    - WebSocket extensions framework
    - Compression extension example

## Test Adaptation Guidelines

When porting TCK tests to our compliance module, follow these principles:

### 1. Simplify Test Setup
- **TCK Approach**: Uses Arquillian, complex deployment descriptors, Jakarta EE containers
- **Our Approach**: Direct server instantiation, simple JUnit 5 tests, Java HttpClient for WebSocket client

### 2. Focus on Server-Side Testing
- Skip client-specific TCK tests (we're testing the server)
- Use Java 11+ HttpClient WebSocket API as test client
- Test only server-side behavior and API compliance

### 3. Adapt Test Structure
```java
// TCK test structure (complex):
@Deployment
public static Archive<?> createDeployment() { ... }

@Test
public void testFeature() throws Exception {
    // Uses Arquillian containers and deployment
}

// Our test structure (simple):
@Test
public void testFeature() throws Exception {
    JakartaWebSocketServer server = new JakartaWebSocketServer("localhost", port);
    server.start();
    
    WebSocketEndpoint endpoint = server.createEndpoint(MyEndpoint.class, "/path", handler);
    
    // Test using Java HttpClient
    HttpClient client = HttpClient.newHttpClient();
    WebSocket ws = client.newWebSocketBuilder()
        .buildAsync(URI.create("ws://localhost:" + port + "/path"), listener)
        .join();
    
    // Perform test assertions
    
    server.stop();
}
```

### 4. Maintain Test Intent
- Keep the same test scenarios and assertions
- Preserve test names when possible (e.g., `testBasicTextMessage` → `testBasicTextMessage`)
- Reference original TCK test in comments

### 5. Exclude Non-Applicable Tests
- CDI-specific tests
- Jakarta EE container-specific features
- Client-only features (unless testing from client perspective)
- Optional features we choose not to implement

### 6. Test Organization
```
compliance/src/test/java/org/osgi/impl/websockets/compliance/
├── api/              # API tests (CloseReason, ServerEndpointConfig, etc.)
├── annotations/      # Annotation handler tests (@OnMessage, @OnOpen, etc.)
├── coder/           # Encoder/Decoder tests
├── session/         # Session API tests
├── remoteendpoint/  # RemoteEndpoint tests
├── server/          # Server-specific tests (PathParam, ServerApplicationConfig)
├── negative/        # Negative validation tests
└── util/            # Test utilities and helpers
```

## Current Status

### Completed
- ✅ TCK test suite analysis complete (737 tests identified)
- ✅ Feature coverage matrix created
- ✅ Implementation plan defined
- ✅ Test adaptation guidelines established

### Next Steps
1. **Start with Phase 1, Task 1**: Port CloseReason API tests
   - Extract relevant tests from `websocket-tck/artifacts/websocket-tck-spec-tests-2.2.0-sources.jar`
   - Adapt to our test structure
   - Verify all 6 tests pass
   
2. **Continue with Phase 1 tasks sequentially**
   - Each task should result in a set of passing tests
   - Update this README with progress after each task
   
3. **Track progress** using the checklist below

## Progress Tracking

### Phase 1: Core Compliance Tests
- [ ] Task 1: Basic API Tests (CloseReason, ServerEndpointConfig, Exceptions) - 0/20
- [ ] Task 2: Message Handling Tests - 0/40
- [ ] Task 3: Encoder/Decoder Tests - 0/30
- [ ] Task 4: Session API Tests - 0/25
- [ ] Task 5: Annotation Handler Tests - 0/15

### Phase 2: Negative Validation Tests
- [ ] Task 6: Invalid Annotation Tests - 0/20
- [ ] Task 7: Deployment Validation Tests - 0/10

### Phase 3: Advanced Features
- [ ] Task 8: @PathParam Support - 0/25 (requires implementation)
- [ ] Task 9: Async Remote Endpoint - 0/40
- [ ] Task 10: Session Management - 0/20 (requires implementation)
- [ ] Task 11: Streaming Encoders/Decoders - 0/20 (requires implementation)

### Phase 4: Optional Features
- [ ] Task 12: Handshake Customization - 0/10 (requires implementation)
- [ ] Task 13: Subprotocol Support (requires implementation)
- [ ] Task 14: Extension Support (requires implementation)

**Total Progress: 0/280 tests (0%)**

## References

- [Jakarta WebSocket 2.2 Specification](https://jakarta.ee/specifications/websocket/2.2/jakarta-websocket-spec-2.2)
- [TCK User Guide](../websocket-tck/docs/html-usersguide/)
- [TCK Release Notes](../websocket-tck/docs/WebSocketTCK2.2-ReleaseNotes.html)
- [Server Module README](../server/README.md) - Current implementation details