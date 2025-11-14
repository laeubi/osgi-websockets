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

## Next Steps

1. **Study Arquillian Integration**
   - Review Arquillian documentation for custom container adapters
   - Examine existing WebSocket TCK implementations (e.g., Tyrus, Tomcat)
   - Determine the simplest integration approach for our architecture

2. **Create Container Adapter**
   - Implement Arquillian container SPI
   - Integrate with our `JakartaWebSocketServer`
   - Handle test deployment and lifecycle

3. **Configure Test Execution**
   - Create `arquillian.xml` configuration
   - Set up test deployment mechanism
   - Configure server port and endpoints

4. **Run Initial Tests**
   - Execute a subset of TCK tests
   - Document failures and errors
   - Categorize issues (implementation bugs vs. missing features)

5. **Iterative Improvement**
   - Fix failing tests systematically
   - Update implementation to meet spec requirements
   - Track compliance progress

6. **Document Results**
   - Maintain list of passing/failing tests
   - Document known limitations
   - Create compliance report

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
