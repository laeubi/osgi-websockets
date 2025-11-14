# Jakarta WebSocket TCK Compliance Module

This module is dedicated to running the official Jakarta WebSocket 2.2 TCK (Technology Compatibility Kit) against the OSGi WebSocket implementation.

## Overview

The Jakarta WebSocket TCK is a comprehensive test suite designed to verify that implementations comply with the [Jakarta WebSocket 2.2 Specification](https://jakarta.ee/specifications/websocket/2.2/). Running the TCK against our implementation ensures compatibility and correctness.

## Current Status

### Completed
- ✅ Created `compliance` module structure
- ✅ Added module to parent POM
- ✅ Installed TCK artifacts to local Maven repository
- ✅ Added TCK dependencies to module POM:
  - `websocket-tck-common-2.2.0.jar`
  - `websocket-tck-spec-tests-2.2.0.jar`
- ✅ Configured JUnit 5 and Arquillian dependencies
- ✅ Basic module builds successfully

### Pending
- ⏳ Implement Arquillian container adapter for our WebSocket server
- ⏳ Create deployment configuration for TCK tests
- ⏳ Implement TCK porting package interfaces (if required)
- ⏳ Configure test execution to run against our implementation
- ⏳ Analyze test results and identify failing tests
- ⏳ Document compliance gaps and create action plan

## TCK Architecture

The Jakarta WebSocket TCK uses:
- **JUnit 5**: Test framework
- **Arquillian**: For deploying and testing in containers
- **Test Classes**: Located in `com.sun.ts.tests.websocket.*` packages

### Test Categories

The TCK includes tests for:
1. **API Tests** (`com.sun.ts.tests.websocket.api.*`)
   - Client endpoint configuration
   - Server endpoint configuration
   - Close reason handling
   - Encoder/Decoder exceptions
   - WebSocket container

2. **EE Tests** (`com.sun.ts.tests.websocket.ee.*`)
   - Client endpoints
   - Server endpoints
   - Message coders (encoders/decoders)
   - Remote endpoints (async/basic)
   - Container provider

## Integration Requirements

### 1. Arquillian Container Adapter

The TCK uses Arquillian to deploy tests to a container. We need to create an Arquillian adapter that:
- Starts our WebSocket server
- Deploys test endpoints
- Manages server lifecycle during tests

**Options:**
- Create a custom Arquillian container adapter
- Use an embedded container approach
- Bridge to an existing servlet container with our implementation

### 2. Test Configuration

Configuration needed:
- `arquillian.xml`: Defines container settings
- Test deployment descriptors
- Server endpoint registration mechanism

### 3. Porting Package

Some TCK implementations require a "porting package" - vendor-specific adapters. Need to investigate if the WebSocket TCK requires this.

## Running the Tests

Currently, the module is set up with basic infrastructure but cannot execute TCK tests until the Arquillian container adapter is implemented.

### Build the Module

```bash
cd compliance
mvn clean install
```

### Run Tests (Future)

Once the integration is complete:

```bash
mvn test
```

Or run specific test classes:

```bash
mvn test -Dtest=WSClientIT
```

## TCK Test Discovery

The TCK contains 37+ test classes (WSClientIT), each with multiple test methods. Test classes follow the pattern:

```
com/sun/ts/tests/websocket/api/jakarta/websocket/[feature]/WSClientIT.class
com/sun/ts/tests/websocket/ee/jakarta/websocket/[feature]/WSClientIT.class
```

### TCK Infrastructure Components

From analysis of the TCK jars, the following key components are included:

1. **Test Cases** (`websocket-tck-spec-tests-2.2.0.jar`)
   - Integration test classes (WSClientIT)
   - Test endpoints and configurations
   - Test scenarios for various features

2. **Common Test Utilities** (`websocket-tck-common-2.2.0.jar`)
   - `TCKExtension`: JUnit 5 extension for TCK tests
   - `WebSocketTestCase`: Base class for test cases
   - String bean encoders/decoders for testing
   - Client endpoint implementations
   - Test utilities and helpers

3. **Test Execution Framework**
   - Uses JUnit 5 as the test framework
   - Requires Arquillian for deployment
   - Tests expect a running WebSocket server
   - Client-side tests that connect to server endpoints

### Sample Test Categories

From the TCK jar contents, tests cover:

**API Tests:**
- Client endpoint configuration (`clientendpointconfig`)
- Server endpoint configuration (`serverendpointconfig`)
- Close reason handling (`closereason`)
- Decode/Encode exceptions (`decodeexception`, `encodeexception`)
- Deployment exceptions (`deploymentException`)
- WebSocket container (`websocketcontainer`)

**EE Tests:**
- Client endpoints (`clientendpoint`)
- Client endpoint configurations (`clientendpointconfig`)
- Client endpoint message handling (`clientendpointonmessage`)
- Client endpoint return types (`clientendpointreturntype`)
- Coders (encoders/decoders) (`coder`)
- Container provider (`containerprovider`)
- Server and client endpoint implementations (`endpoint`)
- Remote endpoints (async/basic) (`remoteendpoint`)
- User-defined coders (`remoteendpoint/usercoder`)

## Dependencies

### Required at Runtime
- Our WebSocket server implementation (`osgi-websockets-server`)
- Jakarta WebSocket APIs (2.2.0)
- TCK test artifacts (2.2.0)
- JUnit 5
- Arquillian

### Maven Artifacts
TCK artifacts are installed from `/websocket-tck/artifacts/`:
- `jakarta.tck:websocket-tck-common:2.2.0`
- `jakarta.tck:websocket-tck-spec-tests:2.2.0`

## Reference Documentation

- TCK Documentation: `/websocket-tck/docs/`
  - HTML User Guide: `/websocket-tck/docs/html-usersguide/`
  - PDF User Guide: `/websocket-tck/docs/pdf-usersguide/Jakarta-WebSocket-TCK-Users-Guide.pdf`
- TCK Exclude List: `/websocket-tck/docs/TCK-Exclude-List.txt`
- Release Notes: `/websocket-tck/docs/WebSocketTCK2.2-ReleaseNotes.html`

## TCK Integration Approaches

There are several possible approaches to run the TCK against our implementation:

### Option 1: Arquillian Container Adapter (Standard Approach)
Create a custom Arquillian container adapter that:
- Starts/stops our WebSocket server
- Deploys test archives
- Manages test lifecycle

**Pros:**
- Standard TCK approach
- Full test coverage
- Official compliance

**Cons:**
- Complex implementation
- Requires understanding Arquillian SPI
- May require significant adapter code

### Option 2: Manual Test Execution (Simplified Approach)
Extract test logic and run tests directly against our server:
- Start server manually in test setup
- Import and run individual test classes
- Adapt tests to work without Arquillian

**Pros:**
- Simpler to implement
- More control over test execution
- Easier debugging

**Cons:**
- Not official TCK execution
- May miss some integration aspects
- Requires test adaptation

### Option 3: Bridge to Servlet Container
Run our implementation inside a servlet container that has Arquillian support:
- Deploy to Tomcat/Jetty with Arquillian adapter
- Use servlet-based WebSocket endpoints
- Leverage existing container adapters

**Pros:**
- Uses existing infrastructure
- Standard deployment model
- Well-supported

**Cons:**
- Requires servlet container integration
- May not test standalone server
- Additional dependencies

## Next Steps

### Phase 1: Investigation (Current)
- [x] Set up compliance module structure
- [x] Install TCK artifacts
- [x] Document TCK architecture
- [ ] Analyze TCK test structure in detail
- [ ] Study one reference implementation (e.g., Tyrus, Tomcat)
- [ ] Decide on integration approach

### Phase 2: Basic Integration
1. **Choose Integration Approach**
   - Evaluate all three options
   - Consider effort vs. benefit
   - Document decision rationale

2. **Implement Initial Test Runner**
   - Create minimal working example
   - Run one simple TCK test
   - Verify test can connect to server

3. **Expand Test Coverage**
   - Add more test classes gradually
   - Document pass/fail results
   - Track implementation gaps

### Phase 3: Full Compliance
1. **Run Complete Test Suite**
   - Execute all TCK tests
   - Generate compliance report
   - Identify all failures

2. **Fix Implementation Issues**
   - Prioritize critical failures
   - Address missing features
   - Fix bugs found by tests

3. **Document Compliance Status**
   - List passing tests
   - Document known limitations
   - Create exclusion list if needed

## Contributing

When working on TCK integration:
1. Run tests frequently to catch regressions
2. Document any spec ambiguities or issues
3. Keep test results and analysis updated in this README
4. Consider creating test exclusion lists for known issues

## Notes

- The TCK is designed for Jakarta EE environments, so adaptation for OSGi may require creative solutions
- Not all tests may be applicable to our use case (e.g., CDI integration tests)
- Some tests may be excluded if they test optional features not implemented
- The goal is maximum compliance while maintaining our architecture
