# OSGi WebSockets - Copilot Coding Agent Instructions

## Repository Overview

This is an **OSGi WebSocket Whiteboard Service** implementation prototype that provides WebSocket support for OSGi environments. The repository is a Maven multi-module project (~7MB, 204 files) implementing the Jakarta WebSocket 2.2 specification with custom OSGi integration.

**Key Technologies:**
- **Language:** Java (11 for server, 17 for OSGi modules)
- **Build System:** Maven 3.9+ with BND Maven Plugin 7.0.0
- **Frameworks:** OSGi, Jakarta WebSocket API 2.2.0, Netty 4.2.7, Tyrus 2.2.0
- **Testing:** JUnit 5.10.2, Jakarta WebSocket TCK 2.2.0
- **License:** Eclipse Public License 2.0

## Project Structure

This is a Maven reactor project with 6 modules:

```
/home/runner/work/osgi-websockets/osgi-websockets/
├── pom.xml                    # Root aggregator POM
├── server/                    # Standalone Netty-based WebSocket server (Java 11) that should comply with jakarta specification - will be our new backend instead of tyrus!
├── client/                    # OSGi WebSocket client implementation (Java 17)
├── runtime/                   # OSGi runtime with Tyrus integration (Java 17) - only proof of concept - will be reworked!
├── tck/                       # OSGi integration tests with BND (Java 17) -not yet completed - relate to the POC implementation!
├── compliance/                # Jakarta WebSocket TCK compliance tests (Java 17)
└── websocket-tck/             # TCK artifacts and documentation (external) - used as reference for our own compliance tests!
```

### Module Details

**1. server/** - Clean-room WebSocket server implementation
- Main class: `JakartaWebSocketServer.java`
- 8 main source files, fully functional and tested
- Uses Netty for async I/O, implements Jakarta WebSocket annotations
- **No OSGi dependencies** - can be used standalone

**2. runtime/** - OSGi service runtime with Tyrus
- Integrates Tyrus WebSocket implementation with OSGi
- Requires `org.osgi.service.jakarta.websocket:jar:1.0.0-SNAPSHOT` (external dependency)

**3. client/** - OSGi WebSocket client
- 5 source files including example client implementations
- Requires `org.osgi.service.jakarta.websocket` dependency

**4. tck/** - OSGi integration tests
- Uses BND test runner with `test.bndrun` configuration
- Tests OSGi bundle integration

**5. compliance/** - Jakarta WebSocket TCK tests
- Runs official TCK test suite
- Uses Arquillian for test deployment

**6. websocket-tck/** - TCK resources
- Contains TCK artifacts, documentation, and installation script
- Run `websocket-tck/artifacts/artifact-install.sh` to install TCK JARs to local Maven repo

## Critical Build Information

### Missing Dependency Issue

**IMPORTANT:** The repository has a missing dependency that prevents full compilation:
- `org.osgi:org.osgi.service.jakarta.websocket:jar:1.0.0-SNAPSHOT` is not available
- This affects: `runtime/`, `client/`, and `tck/` modules
- **The `server/` module is fully independent and builds successfully**

### Working Build Commands

**To build the entire project (will fail on runtime/client/tck modules):**
```bash
cd /home/runner/work/osgi-websockets/osgi-websockets
mvn clean install -DskipTests
# Expected to fail at runtime module due to missing dependency
```

**To build and test ONLY the working server module:**
```bash
cd /home/runner/work/osgi-websockets/osgi-websockets
mvn clean install -pl server
# This ALWAYS works - use this to validate server changes
```

**To compile server without tests (faster):**
```bash
mvn clean compile -pl server
# Completes in ~10 seconds
```

**To run server tests:**
```bash
mvn test -pl server
# All 23 tests pass, takes ~11 seconds
```

### Installing TCK Artifacts

Before working with compliance tests, install TCK artifacts:
```bash
cd /home/runner/work/osgi-websockets/osgi-websockets/websocket-tck/artifacts
bash artifact-install.sh
# Installs websocket-tck-common-2.2.0.jar and websocket-tck-spec-tests-2.2.0.jar
```

## Build System Architecture

### Maven Configuration

- **Root POM:** `/home/runner/work/osgi-websockets/osgi-websockets/pom.xml`
  - BND Maven Plugin 7.0.0 for OSGi bundle generation
  - BND Testing Maven Plugin for OSGi integration tests
  - Maven Surefire skipped at root level (tests run per module)
  - Modules: tck, client, runtime, server, compliance

- **Module-specific Java versions:**
  - `server/`: Java 11 (maven.compiler.source/target = 11)
  - `runtime/`, `client/`, `tck/`, `compliance/`: Java 17

### BND Configuration

**Key BND files:**
- `tck/test.bndrun` - OSGi test runtime configuration
  - Runtime framework: Eclipse OSGi
  - Execution environment: JavaSE-17
  - Tester: biz.aQute.tester.junit-platform
  - Includes workaround for BND bug #5539 (must include JUnit dependencies)

- `client/bnd.bnd` - Export package configuration
  - Exports: `org.osgi.impl.websockets.*`

## Testing Strategy

### Server Module Tests (ALWAYS RUN THESE)

The server module has comprehensive JUnit 5 tests that can be run independently:

```bash
cd /home/runner/work/osgi-websockets/osgi-websockets
mvn test -pl server
```

**Test classes (23 tests total):**
- `BinarySupportTest` - Binary WebSocket message handling
- `BinaryEndpoint` - Binary endpoint implementation
- `CreateEndpointTest` - Endpoint creation API
- `EndpointHandlerLifecycleTest` - Handler lifecycle management
- `EncoderDecoderTest` - Custom encoder/decoder support
- `MessageEndpoint` - Text message endpoint
- `TestEndpoint` - Basic endpoint functionality

**Test characteristics:**
- Use Java 11's built-in HttpClient for WebSocket client testing
- Server starts on localhost with random ports (e.g., 8892)
- Tests are self-contained and clean up resources
- Netty logging output is visible during test execution

### OSGi Integration Tests (tck module)

**CANNOT be run independently** due to missing `org.osgi.service.jakarta.websocket` dependency.

If dependency is available, run with:
```bash
mvn test -pl tck
# Uses BND test runner with test.bndrun configuration
```

## Key Source Files

### Server Module (`server/src/main/java/org/osgi/impl/websockets/server/`)

1. **JakartaWebSocketServer.java** (main server class)
   - Manages Netty server lifecycle (start/stop)
   - Handles endpoint registration via `createEndpoint()` API
   - Configurable hostname and port

2. **EndpointWebSocketFrameHandler.java** (WebSocket message dispatcher)
   - Processes WebSocket frames
   - Dispatches to Jakarta WebSocket endpoint annotations
   - Supports @OnOpen, @OnMessage, @OnClose, @OnError

3. **NettyWebSocketSession.java** (Session implementation)
   - Implements Jakarta WebSocket Session API
   - Bridges Netty channel to Jakarta Session

4. **EndpointCodecs.java** (Encoder/Decoder management)
   - Manages custom message encoders/decoders
   - Supports text and binary encoders/decoders

5. **EndpointHandler.java** (Endpoint lifecycle interface)
   - Controls endpoint instantiation
   - Provides session lifecycle callbacks

### Configuration Files

- `.gitignore` - Ignores: `target/`, `bin/`, `generated/`
- `.project` - Eclipse project file (OSGi development)
- `LICENSE` - Eclipse Public License 2.0

## Known Issues and Workarounds

### 1. Missing OSGi WebSocket Service Dependency

**Issue:** `org.osgi:org.osgi.service.jakarta.websocket:jar:1.0.0-SNAPSHOT` not available

**Workaround:** Build only the server module:
```bash
mvn clean install -pl server
```

**Affected modules:** runtime, client, tck

### 2. BND Bug #5539

**Issue:** BND tester requires explicit JUnit dependencies in test.bndrun

**Workaround:** Already implemented in `tck/test.bndrun`:
```
-runrequires: \
    bnd.identity;id=junit-jupiter-engine, \
    bnd.identity;id=junit-platform-launcher
```

### 3. Code Annotations Indicate TODOs

From codebase search:
- `NettyWebSocketSession.java`: TODO - SSL support check
- `MyHelloServlet.java`: FIXME - Servlet filters require servlet
- `TyrusJakartaWebsocketServiceRuntime.java`: WORKAROUND for OSGi issue #809
- `TyrusWebSocketContainerFactory.java`: WORKAROUND for OSGi issue #688

**When making changes near these areas, review the comments and linked issues.**

## Making Changes

### If Changing Server Module Code

1. **ALWAYS build and test the server module:**
```bash
cd /home/runner/work/osgi-websockets/osgi-websockets
mvn clean test -pl server
```

2. **Verify all 23 tests pass**

3. **Server module changes do NOT require OSGi testing** (it's standalone)

### If Changing OSGi Modules (runtime/client/tck)

1. **Note:** These modules require the missing `org.osgi.service.jakarta.websocket` dependency

2. **If dependency becomes available, build with:**
```bash
mvn clean install
```

3. **Run OSGi integration tests:**
```bash
mvn verify -pl tck
# Uses BND testing framework
```

### If Changing Compliance Module

1. **Ensure TCK artifacts are installed:**
```bash
cd websocket-tck/artifacts
bash artifact-install.sh
```

2. **Build compliance module:**
```bash
mvn clean test -pl compliance
```

## Environment Setup

**Required Tools:**
- JDK 17 (for most modules) or JDK 11 minimum (for server module only)
- Maven 3.9+
- No additional tools required

**Current environment (verified working):**
- OpenJDK 17.0.17 (Temurin)
- Maven 3.9.11
- Platform: Linux (Ubuntu)

## Validation Checklist

Before committing changes, run these commands:

**For server-only changes:**
```bash
cd /home/runner/work/osgi-websockets/osgi-websockets
mvn clean test -pl server
# All 23 tests must pass
```

**For general changes (if dependencies available):**
```bash
cd /home/runner/work/osgi-websockets/osgi-websockets
mvn clean install
# All modules should compile
```

**Check for compilation warnings:**
```bash
mvn compile -pl server 2>&1 | grep -i warning
# Note: Some deprecation warnings are expected (documented in code)
```

## Documentation

**Key README files to review:**
- `/README.md` - Main project overview (brief, 3 lines)
- `/server/README.md` - Comprehensive server module documentation (313 lines)
- `/compliance/README.md` - TCK compliance status and plans (330 lines)
- `/websocket-tck/README.md` - TCK information

## Important Reminders

1. **Trust these instructions** - They are based on actual build attempts and testing
2. **Server module is the safest place to make changes** - It builds and tests reliably
3. **Missing dependency blocks runtime/client/tck modules** - Don't spend time trying to fix dependency resolution without the external artifact
4. **TCK artifacts must be installed manually** - Run the install script before compliance work
5. **Java version matters** - Server uses Java 11, other modules use Java 17
6. **Tests take ~11 seconds for server module** - Fast feedback loop
7. **All paths referenced here are absolute** - `/home/runner/work/osgi-websockets/osgi-websockets/...`
