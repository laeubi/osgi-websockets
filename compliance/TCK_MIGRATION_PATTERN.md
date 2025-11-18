# TCK Test Migration Pattern

This document describes the pattern for migrating Jakarta WebSocket TCK tests to our OSGi WebSocket implementation compliance test suite.

## Overview

The Jakarta WebSocket 2.2 TCK contains comprehensive tests designed for Jakarta EE environments using Arquillian for deployment. Our approach adapts these tests to work directly with our lightweight server implementation.

## Migration Steps

### 1. Locate Original TCK Tests

Extract and explore TCK sources:
```bash
cd /tmp && mkdir -p tck-explore && cd tck-explore
unzip -q /path/to/websocket-tck-spec-tests-2.2.0-sources.jar
find . -name "*YourFeature*" -type f
```

### 2. Analyze Test Structure

TCK tests typically have this structure:
- Package: `com.sun.ts.tests.websocket.api.jakarta.websocket.*`
- Uses JUnit 5 (`@Test` annotations)
- Contains test data arrays
- Multiple test methods with descriptive names
- Javadoc with TCK references (e.g., `@testName`, `@assertion_ids`)

### 3. Simplify Test Setup

**TCK Approach (Complex):**
```java
@Deployment
public static Archive<?> createDeployment() {
    return ShrinkWrap.create(WebArchive.class)
        .addClasses(...)
        .addAsWebInfResource(...);
}
```

**Our Approach (Simple):**
```java
// No deployment needed - we test the API directly
// For server-side tests, we use direct server instantiation
```

### 4. Create Compliance Test Class

Template structure:
```java
package org.osgi.impl.websockets.compliance.{category};

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import jakarta.websocket.*;

/**
 * Compliance tests for {Feature} API
 * 
 * Adapted from Jakarta WebSocket 2.2 TCK:
 * - Original: com.sun.ts.tests.websocket.{package}.{OriginalClass}
 * - Specification: Jakarta WebSocket 2.2, Section X.Y
 * 
 * {Description of what these tests verify}
 */
public class {Feature}ComplianceTest {
    
    // Copy test data arrays from TCK
    private static final Type[] TEST_DATA = {...};
    
    /**
     * {Test description}
     * 
     * TCK Reference: {originalTestName}
     * Specification: WebSocket:JAVADOC:{number}
     */
    @Test
    public void test{FeatureName}() {
        // Simplified test logic
        // Keep assertions matching TCK intent
    }
}
```

### 5. Adapt Test Logic

**Key Principles:**
- **Keep test intent**: Same scenarios and assertions as TCK
- **Simplify setup**: Remove Arquillian, deployment descriptors, EE containers
- **Test APIs directly**: For API tests, no server needed
- **Reference original**: Always document the original TCK test name

**Example - API Test:**
```java
// TCK uses complex setup but simple test
@Test
public void getCodeTest() throws Exception {
    boolean passed = true;
    for (int i = 0; i < codes_number.length; i++) {
        if (codes[i].getCode() != codes_number[i]) {
            passed = false;
            logger.log(...);
        }
    }
    if (!passed) throw new Exception("Test failed");
}

// Our version - cleaner but same logic
@Test
public void testGetCode() {
    for (int i = 0; i < CODES_NUMBER.length; i++) {
        assertEquals(CODES_NUMBER[i], CODES[i].getCode(),
            "Expected CloseCodes' number " + CODES_NUMBER[i]);
    }
}
```

**Example - Server-Side Test (future):**
```java
@Test
public void testServerFeature() throws Exception {
    // Start server
    JakartaWebSocketServer server = new JakartaWebSocketServer("localhost", port);
    server.start();
    
    // Register endpoint
    WebSocketEndpoint endpoint = server.createEndpoint(TestEndpoint.class, "/test", handler);
    
    // Use Java HttpClient for WebSocket client
    HttpClient client = HttpClient.newHttpClient();
    WebSocket ws = client.newWebSocketBuilder()
        .buildAsync(URI.create("ws://localhost:" + port + "/test"), listener)
        .join();
    
    // Test scenario
    ws.sendText("test", true);
    
    // Assert results (matching TCK expectations)
    assertEquals("expected", result);
    
    // Cleanup
    ws.sendClose(WebSocket.NORMAL_CLOSURE, "done");
    server.stop();
}
```

## Test Categories

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

## Naming Conventions

- **Package**: `org.osgi.impl.websockets.compliance.{category}`
- **Class**: `{Feature}APITest` or `{Feature}ComplianceTest`
- **Method**: `test{FeatureName}` (convert TCK's camelCase to our convention)

Examples:
- TCK: `getCodeTest` → Our: `testGetCode`
- TCK: `constructorTest` → Our: `testConstructor`
- TCK: `valueOfTest` → Our: `testValueOf`

## Documentation Requirements

Each test class must include:
1. **Class Javadoc**:
   - Original TCK package/class reference
   - Specification section reference
   - Brief description

2. **Method Javadoc**:
   - TCK test name reference
   - Specification JAVADOC number (if available)
   - Brief description

Example:
```java
/**
 * Compliance tests for CloseReason API
 * 
 * Adapted from Jakarta WebSocket 2.2 TCK:
 * - Original: com.sun.ts.tests.websocket.api.jakarta.websocket.closereason.WSClientIT
 * - Specification: Jakarta WebSocket 2.2, Section 2.1.5
 */
public class CloseReasonAPITest {
    
    /**
     * Test method CloseCodes.getCode()
     * 
     * TCK Reference: getCodeTest
     * Specification: WebSocket:JAVADOC:24
     */
    @Test
    public void testGetCode() { ... }
}
```

## Example: CloseReason API Tests

The first migrated tests demonstrate this pattern:

**Original TCK**: `com.sun.ts.tests.websocket.api.jakarta.websocket.closereason.WSClientIT`
**Our Version**: `org.osgi.impl.websockets.compliance.api.CloseReasonAPITest`

Key adaptations:
- Removed logger, simplified error handling (use JUnit assertions)
- Converted test arrays to static final constants (UPPERCASE naming)
- Simplified test logic while preserving test intent
- Added descriptive assertion messages
- Documented TCK references in Javadoc

**Results**: All 5 tests migrated and passing ✅

## Test Execution

### Run compliance tests only:
```bash
cd /home/runner/work/osgi-websockets/osgi-websockets
mvn test -pl compliance
```

### Run specific test class:
```bash
mvn test -pl compliance -Dtest=CloseReasonAPITest
```

### Run all tests (server + compliance):
```bash
mvn test -pl server,compliance
```

## Best Practices

1. **Test in batches**: Migrate 5-10 tests at a time, not entire TCK modules
2. **Verify as you go**: Run tests after each batch to catch issues early
3. **Keep it simple**: If a test seems overly complex, verify it's actually testing our scope
4. **Skip non-applicable tests**: Client-specific, CDI, or Jakarta EE container tests
5. **Update progress**: Mark completed tests in `compliance/README.md`
6. **Preserve test data**: Copy test data arrays verbatim (easier to verify correctness)

## Success Criteria

A successfully migrated test:
- ✅ Compiles without errors
- ✅ Runs and passes
- ✅ Tests the same specification behavior as TCK original
- ✅ Documents TCK references
- ✅ Uses simplified setup (no Arquillian/EE containers)
- ✅ Follows our naming and organization conventions

## Next Steps

After completing API tests, move to more complex categories:
1. API tests (current) ✅
2. Message handling tests (requires server integration)
3. Encoder/decoder tests (server + custom coders)
4. Session API tests (server + session management)
5. Annotation handler tests (server + various annotations)
6. Negative validation tests (error conditions)

Refer to `compliance/README.md` for the complete implementation plan.
