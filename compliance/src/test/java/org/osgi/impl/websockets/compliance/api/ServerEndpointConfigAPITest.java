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
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * Compliance tests for ServerEndpointConfig API
 * 
 * Adapted from Jakarta WebSocket 2.2 TCK:
 * - Original: com.sun.ts.tests.websocket.api.jakarta.websocket.server.serverendpointconfig.WSClientIT
 * - Specification: Jakarta WebSocket 2.2, Section 5.2
 * 
 * These tests verify the ServerEndpointConfig and ServerEndpointConfig.Builder APIs
 * comply with the Jakarta WebSocket specification.
 */
public class ServerEndpointConfigAPITest {

    /**
     * Simple test endpoint for configuration testing
     */
    @ServerEndpoint("/test")
    public static class TestEndpoint {
    }

    /**
     * Simple text encoder for testing
     */
    public static class TestTextEncoder implements Encoder.Text<String> {
        @Override
        public String encode(String object) throws EncodeException {
            return object;
        }

        @Override
        public void init(EndpointConfig config) {
        }

        @Override
        public void destroy() {
        }
    }

    /**
     * Simple binary encoder for testing
     */
    public static class TestBinaryEncoder implements Encoder.Binary<ByteBuffer> {
        @Override
        public ByteBuffer encode(ByteBuffer object) throws EncodeException {
            return object;
        }

        @Override
        public void init(EndpointConfig config) {
        }

        @Override
        public void destroy() {
        }
    }

    /**
     * Simple text decoder for testing
     */
    public static class TestTextDecoder implements Decoder.Text<String> {
        @Override
        public String decode(String s) throws DecodeException {
            return s;
        }

        @Override
        public boolean willDecode(String s) {
            return true;
        }

        @Override
        public void init(EndpointConfig config) {
        }

        @Override
        public void destroy() {
        }
    }

    /**
     * Simple binary decoder for testing
     */
    public static class TestBinaryDecoder implements Decoder.Binary<ByteBuffer> {
        @Override
        public ByteBuffer decode(ByteBuffer bytes) throws DecodeException {
            return bytes;
        }

        @Override
        public boolean willDecode(ByteBuffer bytes) {
            return true;
        }

        @Override
        public void init(EndpointConfig config) {
        }

        @Override
        public void destroy() {
        }
    }

    /**
     * Test ServerEndpointConfig.Builder.create() with basic configuration
     * 
     * TCK Reference: constructortest
     * Specification: WebSocket:JAVADOC:200, WebSocket:JAVADOC:198, 
     *                WebSocket:JAVADOC:70, WebSocket:JAVADOC:71,
     *                WebSocket:JAVADOC:195, WebSocket:JAVADOC:197
     */
    @Test
    public void testBasicBuilder() {
        ServerEndpointConfig config = ServerEndpointConfig.Builder
            .create(TestEndpoint.class, "/test")
            .build();

        assertNotNull(config, "ServerEndpointConfig should be created");

        // Verify default empty lists
        List<?> decoders = config.getDecoders();
        assertNotNull(decoders, "getDecoders() should return non-null list");
        assertTrue(decoders.isEmpty(), "Default decoders should be empty");

        List<?> encoders = config.getEncoders();
        assertNotNull(encoders, "getEncoders() should return non-null list");
        assertTrue(encoders.isEmpty(), "Default encoders should be empty");

        List<?> extensions = config.getExtensions();
        assertNotNull(extensions, "getExtensions() should return non-null list");
        assertTrue(extensions.isEmpty(), "Default extensions should be empty");

        List<String> subprotocols = config.getSubprotocols();
        assertNotNull(subprotocols, "getSubprotocols() should return non-null list");
        assertTrue(subprotocols.isEmpty(), "Default subprotocols should be empty");
    }

    /**
     * Test ServerEndpointConfig.Builder.subprotocols()
     * 
     * TCK Reference: subprotocolsTest
     * Specification: WebSocket:JAVADOC:200, WebSocket:JAVADOC:198,
     *                WebSocket:JAVADOC:197, WebSocket:JAVADOC:204
     */
    @Test
    public void testSubprotocols() {
        List<String> expectedSubprotocols = Arrays.asList(
            "MBWS", "MBLWS", "soap", "WAMP", "v10.stomp", "v11.stomp", "v12.stomp"
        );

        ServerEndpointConfig config = ServerEndpointConfig.Builder
            .create(TestEndpoint.class, "/test")
            .subprotocols(expectedSubprotocols)
            .build();

        List<String> actualSubprotocols = config.getSubprotocols();
        assertNotNull(actualSubprotocols, "getSubprotocols() should return non-null list");
        assertFalse(actualSubprotocols.isEmpty(), "Subprotocols should not be empty");
        assertEquals(expectedSubprotocols.size(), actualSubprotocols.size(),
            "Should have " + expectedSubprotocols.size() + " subprotocols");

        for (String subprotocol : expectedSubprotocols) {
            assertTrue(actualSubprotocols.contains(subprotocol),
                "Subprotocol '" + subprotocol + "' should be present");
        }
    }

    /**
     * Test ServerEndpointConfig.Builder.encoders()
     * 
     * TCK Reference: encodersTest
     * Specification: WebSocket:JAVADOC:200, WebSocket:JAVADOC:198,
     *                WebSocket:JAVADOC:71, WebSocket:JAVADOC:202
     */
    @Test
    public void testEncoders() {
        List<Class<? extends Encoder>> expectedEncoders = Arrays.asList(
            TestTextEncoder.class,
            TestBinaryEncoder.class
        );

        ServerEndpointConfig config = ServerEndpointConfig.Builder
            .create(TestEndpoint.class, "/test")
            .encoders(expectedEncoders)
            .build();

        List<Class<? extends Encoder>> actualEncoders = config.getEncoders();
        assertNotNull(actualEncoders, "getEncoders() should return non-null list");
        assertFalse(actualEncoders.isEmpty(), "Encoders should not be empty");
        assertEquals(expectedEncoders.size(), actualEncoders.size(),
            "Should have " + expectedEncoders.size() + " encoders");

        for (Class<? extends Encoder> encoder : expectedEncoders) {
            assertTrue(actualEncoders.contains(encoder),
                "Encoder " + encoder.getSimpleName() + " should be present");
        }
    }

    /**
     * Test ServerEndpointConfig.Builder.decoders()
     * 
     * TCK Reference: decodersTest
     * Specification: WebSocket:JAVADOC:200, WebSocket:JAVADOC:198,
     *                WebSocket:JAVADOC:70, WebSocket:JAVADOC:201
     */
    @Test
    public void testDecoders() {
        List<Class<? extends Decoder>> expectedDecoders = Arrays.asList(
            TestTextDecoder.class,
            TestBinaryDecoder.class
        );

        ServerEndpointConfig config = ServerEndpointConfig.Builder
            .create(TestEndpoint.class, "/test")
            .decoders(expectedDecoders)
            .build();

        List<Class<? extends Decoder>> actualDecoders = config.getDecoders();
        assertNotNull(actualDecoders, "getDecoders() should return non-null list");
        assertFalse(actualDecoders.isEmpty(), "Decoders should not be empty");
        assertEquals(expectedDecoders.size(), actualDecoders.size(),
            "Should have " + expectedDecoders.size() + " decoders");

        for (Class<? extends Decoder> decoder : expectedDecoders) {
            assertTrue(actualDecoders.contains(decoder),
                "Decoder " + decoder.getSimpleName() + " should be present");
        }
    }

    /**
     * Test ServerEndpointConfig.Builder with multiple configurations
     * 
     * TCK Reference: constructorTest1, constructorTest2
     * Specification: WebSocket:JAVADOC:200, WebSocket:JAVADOC:198,
     *                WebSocket:JAVADOC:195, WebSocket:JAVADOC:197,
     *                WebSocket:JAVADOC:203, WebSocket:JAVADOC:204
     */
    @Test
    public void testBuilderWithMultipleConfigurations() {
        List<String> expectedSubprotocols = Arrays.asList("soap", "WAMP");
        List<Class<? extends Encoder>> expectedEncoders = Arrays.asList(TestTextEncoder.class);
        List<Class<? extends Decoder>> expectedDecoders = Arrays.asList(TestTextDecoder.class);

        ServerEndpointConfig config = ServerEndpointConfig.Builder
            .create(TestEndpoint.class, "/test")
            .subprotocols(expectedSubprotocols)
            .encoders(expectedEncoders)
            .decoders(expectedDecoders)
            .build();

        // Verify subprotocols
        List<String> actualSubprotocols = config.getSubprotocols();
        assertEquals(expectedSubprotocols.size(), actualSubprotocols.size());
        assertTrue(actualSubprotocols.containsAll(expectedSubprotocols));

        // Verify encoders
        List<Class<? extends Encoder>> actualEncoders = config.getEncoders();
        assertEquals(expectedEncoders.size(), actualEncoders.size());
        assertTrue(actualEncoders.containsAll(expectedEncoders));

        // Verify decoders
        List<Class<? extends Decoder>> actualDecoders = config.getDecoders();
        assertEquals(expectedDecoders.size(), actualDecoders.size());
        assertTrue(actualDecoders.containsAll(expectedDecoders));
    }

    /**
     * Test ServerEndpointConfig.getEndpointClass()
     * 
     * TCK Reference: getEndpointClassTest
     * Specification: WebSocket:JAVADOC:200, WebSocket:JAVADOC:198, WebSocket:JAVADOC:194
     */
    @Test
    public void testGetEndpointClass() {
        ServerEndpointConfig config = ServerEndpointConfig.Builder
            .create(TestEndpoint.class, "/test")
            .build();

        Class<?> endpointClass = config.getEndpointClass();
        assertEquals(TestEndpoint.class, endpointClass,
            "getEndpointClass() should return TestEndpoint.class");
    }

    /**
     * Test ServerEndpointConfig.getPath()
     * 
     * TCK Reference: getPathTest
     * Specification: WebSocket:JAVADOC:200, WebSocket:JAVADOC:198, WebSocket:JAVADOC:196
     */
    @Test
    public void testGetPath() {
        String expectedPath = "/test/path";

        ServerEndpointConfig config = ServerEndpointConfig.Builder
            .create(TestEndpoint.class, expectedPath)
            .build();

        String actualPath = config.getPath();
        assertEquals(expectedPath, actualPath,
            "getPath() should return '" + expectedPath + "'");
    }

    /**
     * Test ServerEndpointConfig.getConfigurator()
     * 
     * TCK Reference: configuratorTest
     * Specification: WebSocket:JAVADOC:200, WebSocket:JAVADOC:198,
     *                WebSocket:JAVADOC:199, WebSocket:JAVADOC:193
     */
    @Test
    public void testGetConfigurator() {
        ServerEndpointConfig config = ServerEndpointConfig.Builder
            .create(TestEndpoint.class, "/test")
            .build();

        ServerEndpointConfig.Configurator configurator = config.getConfigurator();
        assertNotNull(configurator, "getConfigurator() should return non-null configurator");
    }

    /**
     * Test ServerEndpointConfig.Builder.configurator()
     * 
     * TCK Reference: configuratorTest, constructorTest4
     * Specification: WebSocket:JAVADOC:200, WebSocket:JAVADOC:198,
     *                WebSocket:JAVADOC:199, WebSocket:JAVADOC:206
     */
    @Test
    public void testCustomConfigurator() {
        ServerEndpointConfig.Configurator customConfigurator = 
            new ServerEndpointConfig.Configurator() {
            };

        ServerEndpointConfig config = ServerEndpointConfig.Builder
            .create(TestEndpoint.class, "/test")
            .configurator(customConfigurator)
            .build();

        ServerEndpointConfig.Configurator actualConfigurator = config.getConfigurator();
        assertSame(customConfigurator, actualConfigurator,
            "getConfigurator() should return the custom configurator");
    }

    /**
     * Test ServerEndpointConfig with custom configurator and empty lists
     * 
     * TCK Reference: constructorTest4, constructorTest5
     * Specification: WebSocket:JAVADOC:200, WebSocket:JAVADOC:198,
     *                WebSocket:JAVADOC:199
     */
    @Test
    public void testCustomConfiguratorWithDefaults() {
        ServerEndpointConfig.Configurator customConfigurator = 
            new ServerEndpointConfig.Configurator() {
            };

        ServerEndpointConfig config = ServerEndpointConfig.Builder
            .create(TestEndpoint.class, "/test")
            .configurator(customConfigurator)
            .build();

        // Even with custom configurator, default lists should be empty
        assertTrue(config.getDecoders().isEmpty(), "Decoders should be empty");
        assertTrue(config.getEncoders().isEmpty(), "Encoders should be empty");
        assertTrue(config.getExtensions().isEmpty(), "Extensions should be empty");
        assertTrue(config.getSubprotocols().isEmpty(), "Subprotocols should be empty");
    }

    /**
     * Test ServerEndpointConfig with all options combined
     * 
     * TCK Reference: constructorTest3, constructorTest5
     * Specification: WebSocket:JAVADOC:200, WebSocket:JAVADOC:198
     */
    @Test
    public void testFullConfiguration() {
        List<String> expectedSubprotocols = Arrays.asList("soap", "WAMP");
        List<Class<? extends Encoder>> expectedEncoders = Arrays.asList(TestTextEncoder.class);
        List<Class<? extends Decoder>> expectedDecoders = Arrays.asList(TestTextDecoder.class);
        ServerEndpointConfig.Configurator customConfigurator = 
            new ServerEndpointConfig.Configurator() {
            };
        String expectedPath = "/fulltest";

        ServerEndpointConfig config = ServerEndpointConfig.Builder
            .create(TestEndpoint.class, expectedPath)
            .subprotocols(expectedSubprotocols)
            .encoders(expectedEncoders)
            .decoders(expectedDecoders)
            .configurator(customConfigurator)
            .build();

        // Verify all configurations
        assertEquals(TestEndpoint.class, config.getEndpointClass());
        assertEquals(expectedPath, config.getPath());
        assertTrue(config.getSubprotocols().containsAll(expectedSubprotocols));
        assertTrue(config.getEncoders().containsAll(expectedEncoders));
        assertTrue(config.getDecoders().containsAll(expectedDecoders));
        assertSame(customConfigurator, config.getConfigurator());
    }
}
