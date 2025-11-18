/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.osgi.impl.websockets.compliance.api;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import jakarta.websocket.DecodeException;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.EncodeException;

/**
 * Compliance tests for WebSocket Exception APIs
 * 
 * Adapted from Jakarta WebSocket 2.2 TCK:
 * - DecodeException: com.sun.ts.tests.websocket.api.jakarta.websocket.decodeexception.WSClientIT
 * - EncodeException: com.sun.ts.tests.websocket.api.jakarta.websocket.encodeexception.WSClientIT
 * - DeploymentException: com.sun.ts.tests.websocket.api.jakarta.websocket.deploymentException.WSClientIT
 * - Specification: Jakarta WebSocket 2.2, Section 7
 * 
 * These tests verify the exception APIs (DecodeException, EncodeException, DeploymentException)
 * comply with the Jakarta WebSocket specification.
 */
public class ExceptionAPITest {

    // ============================================================================
    // DecodeException Tests
    // ============================================================================

    /**
     * Test DecodeException(String, String) constructor
     * 
     * TCK Reference: constructorTest (DecodeException)
     * Specification: WebSocket:JAVADOC:32, WebSocket:JAVADOC:33, WebSocket:JAVADOC:34
     */
    @Test
    public void testDecodeExceptionStringConstructor() {
        String reason = "TCK: Cannot decode the message";
        String encodedMessage = "xyz for now";

        DecodeException dex = new DecodeException(encodedMessage, reason);

        assertEquals(encodedMessage, dex.getText(),
            "Expected text message '" + encodedMessage + "'");
        assertNull(dex.getBytes(),
            "Expected ByteBuffer to be null for String constructor");
    }

    /**
     * Test DecodeException(ByteBuffer, String) constructor
     * 
     * TCK Reference: constructorTest1 (DecodeException)
     * Specification: WebSocket:JAVADOC:31, WebSocket:JAVADOC:33, WebSocket:JAVADOC:34
     */
    @Test
    public void testDecodeExceptionByteBufferConstructor() {
        String reason = "TCK: Cannot decode the message";
        ByteBuffer encodedMessage = ByteBuffer.allocate(20);
        encodedMessage.put("xyz for now".getBytes());

        DecodeException dex = new DecodeException(encodedMessage, reason);

        assertNull(dex.getText(),
            "Expected text to be null for ByteBuffer constructor");
        assertEquals(encodedMessage, dex.getBytes(),
            "Expected ByteBuffer " + encodedMessage);
    }

    /**
     * Test DecodeException(String, String, Throwable) constructor
     * 
     * TCK Reference: constructorTest2 (DecodeException)
     * Specification: WebSocket:JAVADOC:30, WebSocket:JAVADOC:33, WebSocket:JAVADOC:34
     */
    @Test
    public void testDecodeExceptionStringWithCauseConstructor() {
        String reason = "TCK: Cannot decode the message";
        String encodedMessage = "xyz for now";
        Throwable cause = new Throwable("ConstructorTest2");

        DecodeException dex = new DecodeException(encodedMessage, reason, cause);

        assertEquals(encodedMessage, dex.getText(),
            "Expected text message '" + encodedMessage + "'");
        assertNull(dex.getBytes(),
            "Expected ByteBuffer to be null for String constructor");
        assertEquals(cause, dex.getCause(),
            "Expected cause to match");
    }

    /**
     * Test DecodeException(ByteBuffer, String, Throwable) constructor
     * 
     * TCK Reference: constructorTest3 (DecodeException)
     * Specification: WebSocket:JAVADOC:29, WebSocket:JAVADOC:33, WebSocket:JAVADOC:34
     */
    @Test
    public void testDecodeExceptionByteBufferWithCauseConstructor() {
        String reason = "TCK: Cannot decode the message";
        ByteBuffer encodedMessage = ByteBuffer.allocate(20);
        encodedMessage.put("xyz for now".getBytes());
        Throwable cause = new Throwable("constructorTest3");

        DecodeException dex = new DecodeException(encodedMessage, reason, cause);

        assertNull(dex.getText(),
            "Expected text to be null for ByteBuffer constructor");
        assertEquals(encodedMessage, dex.getBytes(),
            "Expected ByteBuffer " + encodedMessage);
        assertEquals(cause, dex.getCause(),
            "Expected cause to match");
    }

    // ============================================================================
    // EncodeException Tests
    // ============================================================================

    /**
     * Test EncodeException(Object, String) constructor
     * 
     * TCK Reference: constructorTest (EncodeException)
     * Specification: WebSocket:JAVADOC:51, WebSocket:JAVADOC:53
     */
    @Test
    public void testEncodeExceptionConstructor() {
        String reason = "TCK: Cannot encode the message";
        String encodedMessage = "xyz for now";

        EncodeException eex = new EncodeException(encodedMessage, reason);

        assertEquals(encodedMessage, eex.getObject(),
            "Expected object '" + encodedMessage + "'");
    }

    /**
     * Test EncodeException(Object, String, Throwable) constructor
     * 
     * TCK Reference: constructorTest1 (EncodeException)
     * Specification: WebSocket:JAVADOC:52, WebSocket:JAVADOC:53
     */
    @Test
    public void testEncodeExceptionWithCauseConstructor() {
        String reason = "TCK: Cannot decode the message";
        ByteBuffer encodedMessage = ByteBuffer.allocate(20);
        encodedMessage.put("xyz for now".getBytes());
        Throwable cause = new Throwable("TCK Cannot encode");

        EncodeException eex = new EncodeException(encodedMessage, reason, cause);

        assertEquals(encodedMessage, eex.getObject(),
            "Expected object " + encodedMessage);
        assertEquals(cause, eex.getCause(),
            "Expected cause to match");
    }

    // ============================================================================
    // DeploymentException Tests
    // ============================================================================

    /**
     * Test DeploymentException(String) constructor
     * 
     * TCK Reference: constructorTest (DeploymentException)
     * Specification: WebSocket:JAVADOC:49
     */
    @Test
    public void testDeploymentExceptionConstructor() {
        String reason = "TCK: testing the DeploymentException(String)";

        DeploymentException dex = new DeploymentException(reason);

        assertNotNull(dex, "DeploymentException should be created");
        assertEquals(reason, dex.getMessage(),
            "Expected message '" + reason + "'");
    }

    /**
     * Test DeploymentException(String, Throwable) constructor
     * 
     * TCK Reference: constructorTest1 (DeploymentException)
     * Specification: WebSocket:JAVADOC:50
     */
    @Test
    public void testDeploymentExceptionWithCauseConstructor() {
        String reason = "TCK: testing the DeploymentException(String)";
        Throwable cause = new Throwable("TCK_Test");

        DeploymentException dex = new DeploymentException(reason, cause);

        assertNotNull(dex, "DeploymentException should be created");
        assertEquals(reason, dex.getMessage(),
            "Expected message '" + reason + "'");
        assertEquals(cause, dex.getCause(),
            "Expected cause to match");
    }
}
