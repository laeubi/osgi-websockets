package org.osgi.impl.websockets.client;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.PongMessage;
import jakarta.websocket.Session;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * Validates WebSocket client endpoint classes for compliance with Jakarta WebSocket specification.
 * 
 * This validator checks for:
 * - Duplicate annotation handlers
 * - Invalid parameter types
 * - Missing required parameters
 * 
 * @see <a href="https://jakarta.ee/specifications/websocket/2.2/">Jakarta WebSocket Specification 2.2</a>
 */
public class ClientEndpointValidator {
    
    /**
     * Validates an endpoint class for compliance with the Jakarta WebSocket specification.
     * 
     * @param endpointClass the endpoint class to validate
     * @throws IllegalArgumentException if the endpoint class has invalid annotations or configurations
     */
    public static void validateEndpoint(Class<?> endpointClass) {
        if (endpointClass == null) {
            throw new IllegalArgumentException("Endpoint class cannot be null");
        }
        
        ClientEndpoint annotation = endpointClass.getAnnotation(ClientEndpoint.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Endpoint class must be annotated with @ClientEndpoint");
        }
        
        Method[] methods = endpointClass.getDeclaredMethods();
        
        // Validate annotation uniqueness
        validateAnnotationUniqueness(methods);
        
        // Validate @OnMessage methods
        validateOnMessageMethods(methods);
        
        // Validate @OnOpen methods
        validateOnOpenMethods(methods);
        
        // Validate @OnClose methods
        validateOnCloseMethods(methods);
        
        // Validate @OnError methods
        validateOnErrorMethods(methods);
    }
    
    /**
     * Validates that there are no duplicate annotation handlers.
     */
    private static void validateAnnotationUniqueness(Method[] methods) {
        int onOpenCount = 0;
        int onCloseCount = 0;
        int onErrorCount = 0;
        int textMessageCount = 0;
        int binaryMessageCount = 0;
        int pongMessageCount = 0;
        
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnOpen.class)) {
                onOpenCount++;
            }
            if (method.isAnnotationPresent(OnClose.class)) {
                onCloseCount++;
            }
            if (method.isAnnotationPresent(OnError.class)) {
                onErrorCount++;
            }
            if (method.isAnnotationPresent(OnMessage.class)) {
                MessageType msgType = determineMessageType(method);
                switch (msgType) {
                    case TEXT:
                        textMessageCount++;
                        break;
                    case BINARY:
                        binaryMessageCount++;
                        break;
                    case PONG:
                        pongMessageCount++;
                        break;
                }
            }
        }
        
        if (onOpenCount > 1) {
            throw new IllegalArgumentException(
                "Each websocket endpoint may only have one message handling method for @OnOpen (found " + onOpenCount + ")");
        }
        if (onCloseCount > 1) {
            throw new IllegalArgumentException(
                "Each websocket endpoint may only have one message handling method for @OnClose (found " + onCloseCount + ")");
        }
        if (onErrorCount > 1) {
            throw new IllegalArgumentException(
                "Each websocket endpoint may only have one message handling method for @OnError (found " + onErrorCount + ")");
        }
        if (textMessageCount > 1) {
            throw new IllegalArgumentException(
                "Each websocket endpoint may only have one message handling method for text messages (found " + textMessageCount + ")");
        }
        if (binaryMessageCount > 1) {
            throw new IllegalArgumentException(
                "Each websocket endpoint may only have one message handling method for binary messages (found " + binaryMessageCount + ")");
        }
        if (pongMessageCount > 1) {
            throw new IllegalArgumentException(
                "Each websocket endpoint may only have one message handling method for pong messages (found " + pongMessageCount + ")");
        }
    }
    
    /**
     * Determines the message type handled by an @OnMessage method based on its parameters.
     */
    private static MessageType determineMessageType(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        
        for (Class<?> paramType : paramTypes) {
            // Check for text message types (String, Reader, or primitives)
            if (paramType == String.class || paramType == Reader.class || isPrimitiveOrWrapper(paramType)) {
                return MessageType.TEXT;
            }
            // Check for binary message types
            if (paramType == ByteBuffer.class || paramType == byte[].class || paramType == InputStream.class) {
                return MessageType.BINARY;
            }
            // Check for pong message type
            if (paramType == PongMessage.class) {
                return MessageType.PONG;
            }
        }
        
        // If no specific message type is found, assume text (could be custom object with decoder)
        return MessageType.TEXT;
    }
    
    /**
     * Validates @OnMessage methods for correct parameter types.
     */
    private static void validateOnMessageMethods(Method[] methods) {
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnMessage.class)) {
                validateOnMessageMethod(method);
            }
        }
    }
    
    /**
     * Validates a single @OnMessage method.
     */
    private static void validateOnMessageMethod(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        
        if (paramTypes.length == 0) {
            throw new IllegalArgumentException(
                "@OnMessage method must have at least one parameter: " + method.getName());
        }
        
        boolean hasMessageParam = false;
        boolean hasPong = false;
        boolean hasReader = false;
        boolean hasInputStream = false;
        
        // First pass: Check for message type parameters
        for (Class<?> paramType : paramTypes) {
            if (paramType == PongMessage.class) {
                hasPong = true;
            }
            if (paramType == Reader.class) {
                hasReader = true;
            }
            if (paramType == InputStream.class) {
                hasInputStream = true;
            }
        }
        
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> paramType = paramTypes[i];
            boolean isLastParam = (i == paramTypes.length - 1);
            
            // Check for Session parameter
            if (paramType == Session.class) {
                continue;
            }
            
            // Check for boolean parameter (partial message indicator)
            if (paramType == boolean.class || paramType == Boolean.class) {
                if (hasMessageParam) {
                    if (!isLastParam) {
                        throw new IllegalArgumentException(
                            "boolean parameter in @OnMessage must be the last parameter when used with another message type: " + method.getName());
                    }
                    
                    if (hasPong) {
                        throw new IllegalArgumentException(
                            "@OnMessage method for PongMessage cannot have boolean parameter: " + method.getName());
                    }
                    
                    if (hasReader || hasInputStream) {
                        throw new IllegalArgumentException(
                            "boolean parameter cannot be used with Reader or InputStream: " + method.getName());
                    }
                    continue;
                } else {
                    // Boolean is being used as a primitive message type
                    hasMessageParam = true;
                    continue;
                }
            }
            
            // Check for message parameter types
            if (isValidMessageParameter(paramType)) {
                if (hasMessageParam) {
                    throw new IllegalArgumentException(
                        "@OnMessage method can only have one message parameter: " + method.getName());
                }
                hasMessageParam = true;
                continue;
            }
            
            // If it's not a recognized type, check if it's a valid custom type with decoder
            if (!hasMessageParam) {
                hasMessageParam = true;
                continue;
            }
            
            // If we reach here, it's an invalid parameter type
            throw new IllegalArgumentException(
                "Invalid parameter type in @OnMessage method: " + paramType.getName() + " in " + method.getName());
        }
        
        if (!hasMessageParam) {
            throw new IllegalArgumentException(
                "@OnMessage method must have a message parameter: " + method.getName());
        }
    }
    
    /**
     * Checks if a parameter type is a valid message parameter for @OnMessage.
     */
    private static boolean isValidMessageParameter(Class<?> paramType) {
        return paramType == String.class ||
               paramType == ByteBuffer.class ||
               paramType == byte[].class ||
               paramType == Reader.class ||
               paramType == InputStream.class ||
               paramType == PongMessage.class ||
               isPrimitiveOrWrapper(paramType);
    }
    
    /**
     * Checks if a parameter type is a primitive or its wrapper class.
     */
    private static boolean isPrimitiveOrWrapper(Class<?> paramType) {
        return paramType.isPrimitive() ||
               paramType == Boolean.class ||
               paramType == Byte.class ||
               paramType == Character.class ||
               paramType == Short.class ||
               paramType == Integer.class ||
               paramType == Long.class ||
               paramType == Float.class ||
               paramType == Double.class;
    }
    
    /**
     * Validates @OnOpen methods for correct parameter types.
     */
    private static void validateOnOpenMethods(Method[] methods) {
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnOpen.class)) {
                validateOnOpenMethod(method);
            }
        }
    }
    
    /**
     * Validates a single @OnOpen method.
     */
    private static void validateOnOpenMethod(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        
        boolean hasSession = false;
        boolean hasEndpointConfig = false;
        
        for (Class<?> paramType : paramTypes) {
            if (paramType == Session.class) {
                if (hasSession) {
                    throw new IllegalArgumentException(
                        "@OnOpen method can only have one Session parameter: " + method.getName());
                }
                hasSession = true;
                continue;
            }
            
            if (EndpointConfig.class.isAssignableFrom(paramType)) {
                if (hasEndpointConfig) {
                    throw new IllegalArgumentException(
                        "@OnOpen method can only have one EndpointConfig parameter: " + method.getName());
                }
                hasEndpointConfig = true;
                continue;
            }
            
            // If it's not a recognized parameter, it's invalid
            throw new IllegalArgumentException(
                "Invalid parameter type in @OnOpen method: " + paramType.getName() + " in " + method.getName());
        }
    }
    
    /**
     * Validates @OnClose methods for correct parameter types.
     */
    private static void validateOnCloseMethods(Method[] methods) {
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnClose.class)) {
                validateOnCloseMethod(method);
            }
        }
    }
    
    /**
     * Validates a single @OnClose method.
     */
    private static void validateOnCloseMethod(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        
        boolean hasSession = false;
        boolean hasCloseReason = false;
        
        for (Class<?> paramType : paramTypes) {
            if (paramType == Session.class) {
                if (hasSession) {
                    throw new IllegalArgumentException(
                        "@OnClose method can only have one Session parameter: " + method.getName());
                }
                hasSession = true;
                continue;
            }
            
            if (paramType == CloseReason.class) {
                if (hasCloseReason) {
                    throw new IllegalArgumentException(
                        "@OnClose method can only have one CloseReason parameter: " + method.getName());
                }
                hasCloseReason = true;
                continue;
            }
            
            // If it's not a recognized parameter, it's invalid
            throw new IllegalArgumentException(
                "Invalid parameter type in @OnClose method: " + paramType.getName() + " in " + method.getName());
        }
    }
    
    /**
     * Validates @OnError methods for correct parameter types.
     */
    private static void validateOnErrorMethods(Method[] methods) {
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnError.class)) {
                validateOnErrorMethod(method);
            }
        }
    }
    
    /**
     * Validates a single @OnError method.
     */
    private static void validateOnErrorMethod(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        
        boolean hasSession = false;
        boolean hasThrowable = false;
        
        for (Class<?> paramType : paramTypes) {
            if (paramType == Session.class) {
                if (hasSession) {
                    throw new IllegalArgumentException(
                        "@OnError method can only have one Session parameter: " + method.getName());
                }
                hasSession = true;
                continue;
            }
            
            if (Throwable.class.isAssignableFrom(paramType)) {
                if (hasThrowable) {
                    throw new IllegalArgumentException(
                        "@OnError method can only have one Throwable parameter: " + method.getName());
                }
                hasThrowable = true;
                continue;
            }
            
            // If it's not a recognized parameter, it's invalid
            throw new IllegalArgumentException(
                "Invalid parameter type in @OnError method: " + paramType.getName() + " in " + method.getName());
        }
        
        // @OnError must have a Throwable parameter
        if (!hasThrowable) {
            throw new IllegalArgumentException(
                "@OnError method must have a Throwable parameter: " + method.getName());
        }
    }
    
    /**
     * Enum for message types.
     */
    private enum MessageType {
        TEXT,
        BINARY,
        PONG
    }
}
