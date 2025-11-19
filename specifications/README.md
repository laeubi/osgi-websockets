# WebSocket Specifications

This directory contains the authoritative specifications that guide our OSGi WebSocket implementation. These documents define both the low-level protocol and the high-level Java API.

**IMPORTANT: Always trust the specification over any particular implementation!**

## Overview

Our implementation has two layers, each governed by different specifications:

1. **Transport Layer** (RFC 6455) - The WebSocket protocol itself (Netty-based server)
2. **Application Layer** (Jakarta WebSocket 2.2) - The Java API for WebSocket endpoints

## Directory Structure

```
specifications/
├── rfc6455/              # WebSocket Protocol (IETF Standards)
│   ├── rfc6455.md        # Core protocol (148 KB, 3,562 lines)
│   ├── rfc7936.md        # Registry procedures update
│   ├── rfc8307.md        # Well-Known URIs
│   └── rfc8441.md        # HTTP/2 bootstrapping
└── jakarta-websocket/    # Jakarta WebSocket API Specification
    └── jakarta-websocket-spec-2.2.md  # Java API (74 KB, 1,055 lines)
```

---

## 1. The WebSocket Protocol (RFC 6455)

**Location:** `rfc6455/`

The IETF WebSocket Protocol specification defines the transport-level protocol. This applies to our Netty-based server implementation.

### Critical Concept

> The WebSocket Protocol is an independent TCP-based protocol. Its
> only relationship to HTTP is that its handshake is interpreted by
> HTTP servers as an Upgrade request.

**Key Point:** The handshake *looks* like HTTP, but WebSocket itself is **not HTTP**. Don't mix up the concerns!

### Main Specification: RFC 6455

**File:** [rfc6455/rfc6455.md](rfc6455/rfc6455.md) (148 KB)

The core WebSocket protocol specification covering:

#### Key Sections

1. **Introduction** - Background, protocol overview, design philosophy
2. **Conformance Requirements** - Terminology and conventions
3. **WebSocket URIs** - ws:// and wss:// URI schemes
4. **Opening Handshake** - HTTP Upgrade mechanism
   - Client requirements (Sec-WebSocket-Key, headers)
   - Server requirements (handshake validation, acceptance)
5. **Data Framing** - Binary frame structure
   - Base framing protocol (FIN, opcode, mask, payload)
   - Client-to-server masking (required)
   - Fragmentation support
   - Control frames (Close, Ping, Pong)
   - Data frames (Text, Binary)
6. **Sending and Receiving Data** - Message handling
7. **Closing the Connection** - Clean shutdown procedures
8. **Error Handling** - Protocol violations
9. **Extensions** - Extension mechanism
10. **Security Considerations** - Origin model, masking, TLS

**Critical for implementation:**
- Section 4: Opening Handshake validation
- Section 5: Frame structure and masking
- Section 7: Close frame handling
- Section 10: Security requirements

### RFC Updates and Extensions

#### RFC 7936: Registry Procedures

**File:** [rfc6455/rfc7936.md](rfc6455/rfc7936.md) (4 KB)

Clarifies IANA registry procedures for WebSocket subprotocols. Relevant if implementing custom subprotocol registration.

#### RFC 8307: Well-Known URIs

**File:** [rfc6455/rfc8307.md](rfc6455/rfc8307.md) (6 KB)

Defines `.well-known/websocket` URI for WebSocket protocol metadata discovery.

#### RFC 8441: HTTP/2 WebSocket Bootstrapping

**File:** [rfc6455/rfc8441.md](rfc6455/rfc8441.md) (16 KB)

Defines mechanism for running WebSocket over HTTP/2 connections using CONNECT method. Future enhancement for our implementation.

---

## 2. Jakarta WebSocket Specification 2.2

**Location:** `jakarta-websocket/`

**File:** [jakarta-websocket/jakarta-websocket-spec-2.2.md](jakarta-websocket/jakarta-websocket-spec-2.2.md) (74 KB)

The Java API specification for WebSocket applications. This defines the programming model our OSGi implementation must support.

**Release:** March 27, 2024 (Final Release)

### Key Sections

#### 1. Introduction
- Purpose and goals
- Terminology and conventions
- Evolution from JSR-356

#### 2. Applications
- **API Overview:**
  - Endpoint Lifecycle
  - Sessions
  - Receiving Messages
  - Sending Messages
  - Closing Connections
  - Clients and Servers
  - WebSocketContainers

- **Endpoints using WebSocket Annotations:**
  - Annotated endpoints (@ServerEndpoint, @ClientEndpoint)
  - WebSocket lifecycle (@OnOpen, @OnClose)
  - Handling messages (@OnMessage)
  - Handling errors (@OnError)
  - Pings and Pongs

- **Jakarta WebSocket Client API**

#### 3. Configuration
- **Server Configurations:**
  - URI Mapping
  - Subprotocol Negotiation
  - Extension Modification
  - Origin Check
  - Handshake Modification
  - Custom State or Processing
  - Customizing Endpoint Creation

- **Client Configuration:**
  - Subprotocols
  - Extensions
  - SSLContext
  - Configuration Modification

#### 4. Annotations

Core annotations our implementation must support:

- `@ServerEndpoint` - Marks class as server endpoint
  - `value` - URI mapping
  - `encoders` - Custom message encoders
  - `decoders` - Custom message decoders
  - `subprotocols` - Supported subprotocols
  - `configurator` - Custom configurator

- `@ClientEndpoint` - Marks class as client endpoint

- `@PathParam` - Extracts path parameters from URI template

- `@OnOpen` - Called when connection opens

- `@OnClose` - Called when connection closes

- `@OnError` - Called on errors

- `@OnMessage` - Handles incoming messages
  - `maxMessageSize` - Maximum message size

- **WebSockets and Inheritance** - Annotation inheritance rules

#### 5. Exception Handling and Threading
- Threading considerations
- Deployment errors
- Errors from application code
- Errors from container/connection

#### 6. Packaging and Deployment
- Client deployment on JRE
- Web container deployment
- Standalone WebSocket server deployment
- Programmatic server deployment
- WebSocket server paths
- Platform versions

#### 7. Jakarta EE Environment
- Dependency Injection integration
- HTTP Session relationship
- Authenticated state

#### 8. Server Security
- Authentication of WebSockets
- Authorization of WebSockets
- Transport Guarantee
- Security examples

---

## Implementation Mapping

### Our Architecture

```
┌─────────────────────────────────────────────┐
│   Jakarta WebSocket API (Annotations)      │  ← jakarta-websocket spec
│   @ServerEndpoint, @OnMessage, etc.        │
├─────────────────────────────────────────────┤
│   OSGi WebSocket Service Runtime           │  ← Our integration layer
│   (Endpoint registration, lifecycle)       │
├─────────────────────────────────────────────┤
│   Netty WebSocket Server                   │  ← RFC 6455 compliance
│   (Handshake, framing, protocol)           │
├─────────────────────────────────────────────┤
│   TCP/TLS                                  │
└─────────────────────────────────────────────┘
```

### Specification to Code Mapping

| Specification Section | Implementation Location |
|----------------------|-------------------------|
| RFC 6455 §4: Opening Handshake | `server/` - Netty HTTP upgrade handler |
| RFC 6455 §5: Data Framing | `server/` - Netty WebSocket frame handler |
| RFC 6455 §7: Closing | `server/` - Connection lifecycle |
| Jakarta §2.2: Annotated Endpoints | `server/EndpointWebSocketFrameHandler.java` |
| Jakarta §4: Annotations | `server/` - Annotation processing |
| Jakarta §3: Configuration | `runtime/` - OSGi service runtime |

---

## Compliance Testing

Our compliance tests are organized by specification:

- **RFC 6455 Compliance:** Protocol-level tests (handshake, framing, masking)
- **Jakarta WebSocket 2.2 Compliance:** API-level tests from TCK (Test Compatibility Kit)

See `/compliance/README.md` for detailed test coverage and implementation status.

---

## Quick Reference

### When to Consult Each Spec

**Use RFC 6455 when:**
- Implementing handshake validation
- Working with frame structure (masking, opcodes)
- Handling protocol errors
- Implementing control frames (ping/pong/close)
- Understanding security requirements

**Use Jakarta WebSocket 2.2 when:**
- Implementing annotation handling
- Designing endpoint lifecycle
- Working with encoders/decoders
- Implementing message dispatch
- Understanding error handling requirements
- Configuring server/client endpoints

### External Links

- RFC 6455: https://www.rfc-editor.org/rfc/rfc6455.html
- Jakarta WebSocket 2.2: https://jakarta.ee/specifications/websocket/2.2/
- WebSocket Protocol Registry: https://www.iana.org/assignments/websocket/websocket.xhtml

---

## Format Notes

All specifications have been converted to clean markdown:
- ✅ Linear reading format (no page numbers or headers)
- ✅ Proper section hierarchy with markdown headers
- ✅ ASCII diagrams in code blocks
- ✅ External links preserved
- ✅ Table of contents removed for streamlined reading

**Converted:** 2024-11-19