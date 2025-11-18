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

import org.junit.jupiter.api.Test;

import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCode;
import jakarta.websocket.CloseReason.CloseCodes;

/**
 * Compliance tests for CloseReason API
 * 
 * Adapted from Jakarta WebSocket 2.2 TCK:
 * - Original: com.sun.ts.tests.websocket.api.jakarta.websocket.closereason.WSClientIT
 * - Specification: Jakarta WebSocket 2.2, Section 2.1.5
 * 
 * These tests verify the CloseReason, CloseCode, and CloseCodes API implementations
 * comply with the Jakarta WebSocket specification.
 */
public class CloseReasonAPITest {

    // Test data arrays matching TCK structure
    private static final CloseCodes[] CODES = {
        CloseReason.CloseCodes.CANNOT_ACCEPT,
        CloseReason.CloseCodes.CLOSED_ABNORMALLY,
        CloseReason.CloseCodes.GOING_AWAY,
        CloseReason.CloseCodes.NORMAL_CLOSURE,
        CloseReason.CloseCodes.NOT_CONSISTENT,
        CloseReason.CloseCodes.NO_EXTENSION,
        CloseReason.CloseCodes.NO_STATUS_CODE,
        CloseReason.CloseCodes.PROTOCOL_ERROR,
        CloseReason.CloseCodes.RESERVED,
        CloseReason.CloseCodes.SERVICE_RESTART,
        CloseReason.CloseCodes.TLS_HANDSHAKE_FAILURE,
        CloseReason.CloseCodes.TOO_BIG,
        CloseReason.CloseCodes.TRY_AGAIN_LATER,
        CloseReason.CloseCodes.UNEXPECTED_CONDITION,
        CloseReason.CloseCodes.VIOLATED_POLICY
    };

    private static final String[] CODES_STRING = {
        "CANNOT_ACCEPT", "CLOSED_ABNORMALLY", "GOING_AWAY", "NORMAL_CLOSURE",
        "NOT_CONSISTENT", "NO_EXTENSION", "NO_STATUS_CODE", "PROTOCOL_ERROR",
        "RESERVED", "SERVICE_RESTART", "TLS_HANDSHAKE_FAILURE", "TOO_BIG",
        "TRY_AGAIN_LATER", "UNEXPECTED_CONDITION", "VIOLATED_POLICY"
    };

    private static final int[] CODES_NUMBER = {
        1003, 1006, 1001, 1000, 1007, 1010, 1005, 1002, 1004, 1012, 1015, 1009, 1013, 1011, 1008
    };

    private static final String[] TCK_CODES_REASON = {
        "TCK_CANNOT_ACCEPT", "TCK_CLOSED_ABNORMALLY", "TCK_GOING_AWAY",
        "TCK_NORMAL_CLOSURE", "TCK_NOT_CONSISTENT", "TCK_NO_EXTENSION",
        "TCK_NO_STATUS_CODE", "TCK_PROTOCOL_ERROR", "TCK_RESERVED",
        "TCK_SERVICE_RESTART", "TCK_TLS_HANDSHAKE_FAILURE", "TCK_TOO_BIG",
        "TCK_TRY_AGAIN_LATER", "TCK_UNEXPECTED_CONDITION", "TCK_VIOLATED_POLICY"
    };

    /**
     * Test method CloseCodes.getCode()
     * 
     * TCK Reference: getCodeTest
     * Specification: WebSocket:JAVADOC:24
     */
    @Test
    public void testGetCode() {
        for (int i = 0; i < CODES_NUMBER.length; i++) {
            assertEquals(CODES_NUMBER[i], CODES[i].getCode(),
                "Expected CloseCodes' number " + CODES_NUMBER[i] + " for " + CODES_STRING[i]);
        }
    }

    /**
     * Test method CloseCode.getCode() (interface)
     * 
     * TCK Reference: getCodeTest1
     * Specification: WebSocket:JAVADOC:22
     */
    @Test
    public void testCloseCodeGetCode() {
        for (int i = 0; i < CODES_NUMBER.length; i++) {
            CloseCode closeCode = CODES[i];
            assertEquals(CODES_NUMBER[i], closeCode.getCode(),
                "Expected CloseCode' number " + CODES_NUMBER[i] + " for " + CODES_STRING[i]);
        }
    }

    /**
     * Test method CloseCodes.valueOf(String)
     * 
     * TCK Reference: valueOfTest
     * Specification: WebSocket:JAVADOC:25
     */
    @Test
    public void testValueOf() {
        for (int i = 0; i < CODES_NUMBER.length; i++) {
            CloseCodes result = CloseReason.CloseCodes.valueOf(CODES_STRING[i]);
            assertEquals(CODES[i], result,
                "Expected CloseCodes " + CODES[i] + " for valueOf('" + CODES_STRING[i] + "')");
        }
    }

    /**
     * Test CloseReason constructor and getter methods
     * 
     * TCK Reference: constructorTest
     * Specification: WebSocket:JAVADOC:18, WebSocket:JAVADOC:19, WebSocket:JAVADOC:20
     */
    @Test
    public void testConstructor() {
        for (int i = 0; i < CODES_NUMBER.length; i++) {
            CloseReason closeReason = new CloseReason(CODES[i], TCK_CODES_REASON[i]);
            
            assertEquals(CODES[i], closeReason.getCloseCode(),
                "Expected CloseCodes " + CODES[i] + " from getCloseCode()");
            
            assertEquals(TCK_CODES_REASON[i], closeReason.getReasonPhrase(),
                "Expected reason phrase '" + TCK_CODES_REASON[i] + "' from getReasonPhrase()");
        }
    }

    /**
     * Test CloseCodes.getCloseCode(int) static method
     * 
     * TCK Reference: getCloseCodeTest
     * Specification: WebSocket:JAVADOC:23
     */
    @Test
    public void testGetCloseCode() {
        for (int i = 0; i < CODES_NUMBER.length; i++) {
            CloseCode result = CloseReason.CloseCodes.getCloseCode(CODES_NUMBER[i]);
            assertEquals(CODES[i], result,
                "Expected CloseCode " + CODES[i] + " for getCloseCode(" + CODES_NUMBER[i] + ")");
        }
    }
}
