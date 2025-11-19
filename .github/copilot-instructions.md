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
├── specifications/            # Contains important specifications our implementation must follow.
├── docs/                      # Contains documents about past investigation of problems or limitations - might not always match current implementation (outdated) and is retained fro reference, check git history to understand to what issue it belongs.
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

---

# Compliance Testing Instructions

## Overview for Future Sessions

When working on Jakarta WebSocket 2.2 compliance tests, follow this workflow to adapt TCK tests to our architecture.

## TCK Test Adaptation Workflow

### 1. Extract and Analyze TCK Tests

```bash
# Extract TCK sources to temporary directory
cd /tmp && mkdir -p tck-explore && cd tck-explore
unzip -q /home/runner/work/osgi-websockets/osgi-websockets/websocket-tck/artifacts/websocket-tck-spec-tests-2.2.0-sources.jar

# Find relevant test files for a specific category
find com/sun/ts/tests/websocket -name "*.java" | grep <category>
```

### 2. Understand Test Structure

The TCK contains **737 test methods** in **93 test classes**:

- **API Tests** (47 tests): CloseReason, ServerEndpointConfig, exceptions
- **End-to-End Tests** (545+ tests): Full WebSocket functionality  
- **Negative Tests** (45 files): Error conditions and validation

See `/home/runner/work/osgi-websockets/osgi-websockets/compliance/README.md` for the complete feature matrix and implementation plan.

### 3. Create Adapted Tests

**DON'T**: Copy TCK tests as-is (they use Arquillian, Jakarta EE containers, complex setup)

**DO**: Create simplified tests following our pattern:

```java
package org.osgi.impl.websockets.compliance.<category>;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.osgi.impl.websockets.server.JakartaWebSocketServer;
import org.osgi.impl.websockets.server.WebSocketEndpoint;
import org.osgi.impl.websockets.server.EndpointHandler;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.*;

/**
 * Compliance tests for [Feature Name]
 * 
 * Adapted from Jakarta WebSocket 2.2 TCK:
 * - Original: com.sun.ts.tests.websocket.<path>.<TestClass>
 * - Specification: Jakarta WebSocket 2.2, Section X.Y
 */
public class FeatureComplianceTest {
    
    private JakartaWebSocketServer server;
    private int port;
    
    @BeforeEach
    public void setUp() throws Exception {
        port = 8080 + ThreadLocalRandom.current().nextInt(1000);
        server = new JakartaWebSocketServer("localhost", port);
        server.start();
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
    }
    
    /**
     * Test [description]
     * 
     * TCK Reference: [original test name]
     * Specification: Section [X.Y]
     */
    @Test
    public void testFeature() throws Exception {
        // Register endpoint
        WebSocketEndpoint endpoint = server.createEndpoint(
            TestEndpoint.class, "/test", createHandler());
        
        // Create WebSocket client using Java HttpClient
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<String> messageFuture = new CompletableFuture<>();
        
        WebSocket ws = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://localhost:" + port + "/test"), 
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, 
                                                     CharSequence data, 
                                                     boolean last) {
                        messageFuture.complete(data.toString());
                        return CompletableFuture.completedFuture(null);
                    }
                })
            .join();
        
        // Test scenario
        ws.sendText("test message", true);
        String response = messageFuture.get(5, TimeUnit.SECONDS);
        
        // Assertions (matching TCK test intent)
        assertEquals("expected response", response);
        
        // Cleanup
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete");
        endpoint.dispose();
    }
    
    private EndpointHandler createHandler() {
        return new EndpointHandler() {
            @Override
            public <T> T createEndpointInstance(Class<T> endpointClass) 
                    throws InstantiationException {
                try {
                    return endpointClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new InstantiationException("Failed: " + e.getMessage());
                }
            }
            
            @Override
            public void sessionEnded(Object endpointInstance) {
                // Cleanup if needed
            }
        };
    }
}
```

### 4. Test Organization

Organize tests by category in `compliance/src/test/java/org/osgi/impl/websockets/compliance/`:

```
compliance/src/test/java/org/osgi/impl/websockets/compliance/
├── api/              # API tests (CloseReason, ServerEndpointConfig, etc.)
├── annotations/      # Annotation handler tests (@OnMessage, @OnOpen, etc.)
├── coder/           # Encoder/Decoder tests
├── session/         # Session API tests
├── remoteendpoint/  # RemoteEndpoint tests (basic and async)
├── server/          # Server-specific tests (PathParam, ServerApplicationConfig)
├── negative/        # Negative validation tests
└── util/            # Test utilities and helpers
```

### 5. Implementation Phases

Follow the priority order defined in `compliance/README.md`:

1. **Phase 1 (HIGH)**: Core compliance tests - verify existing features (~130 tests)
2. **Phase 2 (MEDIUM)**: Negative validation tests - error handling (~30 tests)
3. **Phase 3 (LOW)**: Advanced features requiring implementation (~105 tests)
4. **Phase 4 (OPTIONAL)**: Optional specification features

Start with **Phase 1, Task 1**: CloseReason API tests (6 tests)

### 6. Test Execution

```bash
cd /home/runner/work/osgi-websockets/osgi-websockets

# Test compliance module
mvn test -pl compliance

# Test specific test class
mvn test -pl compliance -Dtest=CloseReasonComplianceTest

# Always verify server module still passes
mvn test -pl server
```

### 7. Update Progress

After completing tests, update `compliance/README.md`:

1. Mark completed tests in the Progress Tracking section
2. Update test counts (e.g., "Task 1: Basic API Tests - 20/20 ✅")
3. Update total progress percentage
4. Commit with descriptive message

## Key Principles for Compliance Testing

### DO ✅
- Always reference the original TCK test in comments
- Keep test scenarios and assertions matching TCK intent
- Use Java 11+ HttpClient for WebSocket client testing
- Follow existing server module test patterns
- Update progress in `compliance/README.md` after each task
- Ensure all existing tests still pass
- Document specification section references

### DON'T ❌
- Copy TCK test infrastructure (Arquillian, deployment descriptors)
- Test client-specific features (unless from client perspective)
- Add complex test dependencies
- Skip documentation of test origins
- Break existing server module functionality
- Implement features not in the specification

## Example Session Prompts

**Starting Phase 1, Task 1:**
```
Implement Phase 1, Task 1 from compliance/README.md: Basic API Tests for CloseReason. 
Extract the 6 tests from the TCK CloseReason test class, adapt them to our test 
structure, and create CloseReasonComplianceTest.java.
```

**Implementing specific feature:**
```
Add @PathParam support to the server module based on the specification, then create 
compliance tests adapted from the 25 TCK tests in 
com.sun.ts.tests.websocket.ee.jakarta.websocket.server.pathparam
```

**Debugging failures:**
```
The SessionApiComplianceTest tests are failing. Analyze the failures, check our 
implementation against the Jakarta WebSocket 2.2 specification Section 3, and 
fix any issues.
```

## Specification References

When working on compliance tests, reference:

- **Specification**: https://jakarta.ee/specifications/websocket/2.2/jakarta-websocket-spec-2.2.pdf
- **TCK User Guide**: `websocket-tck/docs/html-usersguide/`
- **Implementation Plan**: `compliance/README.md`
- **Server Documentation**: `server/README.md`

## Success Criteria

A compliance test task is complete when:

1. ✅ All adapted tests pass
2. ✅ Original TCK test references are documented
3. ✅ Test organization follows defined structure
4. ✅ Server module tests still pass
5. ✅ Progress is updated in `compliance/README.md`
6. ✅ Code is committed with clear message

## Notes

- We're testing the **server implementation**, not creating a full Jakarta EE environment
- Focus on server-side WebSocket functionality
- Client features are out of scope for the server module
- Always test incrementally - port 5-10 tests at a time, not 50 at once
- When in doubt, check existing server module test structure
