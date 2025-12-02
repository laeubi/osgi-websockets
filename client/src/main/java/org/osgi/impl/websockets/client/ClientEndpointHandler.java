package org.osgi.impl.websockets.client;

import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Bridges Java's WebSocket.Listener to Jakarta WebSocket annotated endpoint methods.
 * Handles @OnOpen, @OnMessage, @OnClose, and @OnError callbacks.
 */
public class ClientEndpointHandler implements WebSocket.Listener {
    
    private final JakartaClientSession session;
    private final Object endpointInstance;
    private final ClientEndpointCodecs codecs;
    
    private StringBuilder textMessageBuffer = new StringBuilder();
    private ByteBuffer binaryMessageBuffer = null;
    
    public ClientEndpointHandler(JakartaClientSession session, Object endpointInstance, 
            ClientEndpointCodecs codecs) {
        this.session = session;
        this.endpointInstance = endpointInstance;
        this.codecs = codecs;
    }
    
    /**
     * Invokes the @OnOpen method on the endpoint instance.
     * Called after successful connection.
     */
    public void invokeOnOpen() {
        Method[] methods = endpointInstance.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnOpen.class)) {
                try {
                    method.setAccessible(true);
                    Class<?>[] paramTypes = method.getParameterTypes();
                    Object[] args = new Object[paramTypes.length];
                    
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (paramTypes[i] == Session.class) {
                            args[i] = session;
                        } else if (jakarta.websocket.EndpointConfig.class.isAssignableFrom(paramTypes[i])) {
                            args[i] = null; // EndpointConfig not available in this context
                        }
                    }
                    
                    method.invoke(endpointInstance, args);
                    return;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    invokeOnError(e.getCause() != null ? e.getCause() : e);
                }
            }
        }
    }
    
    @Override
    public void onOpen(WebSocket webSocket) {
        // Request more messages
        webSocket.request(1);
    }
    
    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        textMessageBuffer.append(data);
        
        if (last) {
            String message = textMessageBuffer.toString();
            textMessageBuffer = new StringBuilder();
            
            try {
                invokeOnTextMessage(message);
            } catch (Exception e) {
                invokeOnError(e);
            }
        }
        
        // Request more messages
        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        int dataSize = data.remaining();
        if (binaryMessageBuffer == null) {
            // Allocate initial buffer with some extra capacity
            binaryMessageBuffer = ByteBuffer.allocate(Math.max(dataSize, 1024));
        }
        
        // Check if we need to expand the buffer
        if (binaryMessageBuffer.remaining() < dataSize) {
            // Expand buffer to accommodate new data
            int newSize = binaryMessageBuffer.position() + dataSize;
            ByteBuffer newBuffer = ByteBuffer.allocate(newSize);
            binaryMessageBuffer.flip();
            newBuffer.put(binaryMessageBuffer);
            binaryMessageBuffer = newBuffer;
        }
        
        binaryMessageBuffer.put(data);
        
        if (last) {
            binaryMessageBuffer.flip();
            ByteBuffer message = binaryMessageBuffer;
            binaryMessageBuffer = null;
            
            try {
                invokeOnBinaryMessage(message);
            } catch (Exception e) {
                invokeOnError(e);
            }
        }
        
        // Request more messages
        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        CloseReason.CloseCode closeCode = CloseReason.CloseCodes.getCloseCode(statusCode);
        CloseReason closeReason = new CloseReason(closeCode, reason);
        
        invokeOnClose(closeReason);
        session.onConnectionClosed();
        
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        invokeOnError(error);
    }
    
    /**
     * Invokes the @OnMessage method for text messages.
     */
    private void invokeOnTextMessage(String message) {
        Method[] methods = endpointInstance.getClass().getDeclaredMethods();
        
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnMessage.class)) {
                try {
                    method.setAccessible(true);
                    Class<?>[] paramTypes = method.getParameterTypes();
                    
                    // Check if this method handles text messages
                    boolean isTextHandler = false;
                    boolean isBinaryHandler = false;
                    
                    for (Class<?> paramType : paramTypes) {
                        if (paramType == String.class || 
                            paramType == java.io.Reader.class ||
                            isPrimitiveOrWrapper(paramType)) {
                            isTextHandler = true;
                        }
                        if (paramType == ByteBuffer.class || 
                            paramType == byte[].class ||
                            paramType == java.io.InputStream.class) {
                            isBinaryHandler = true;
                        }
                    }
                    
                    // Skip binary handlers
                    if (isBinaryHandler) {
                        continue; // Skip to next method
                    }
                    
                    // If no explicit text parameters but also not binary, assume custom type decoder - treat as text
                    if (!isTextHandler && !isBinaryHandler) {
                        isTextHandler = true;
                    }
                    
                    if (!isTextHandler) {
                        continue;
                    }
                    
                    Object[] args = new Object[paramTypes.length];
                    boolean hasMessageParam = false;
                    
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (!hasMessageParam && paramTypes[i] == String.class) {
                            args[i] = message;
                            hasMessageParam = true;
                        } else if (!hasMessageParam && isPrimitiveOrWrapper(paramTypes[i])) {
                            args[i] = convertToPrimitive(message, paramTypes[i]);
                            if (args[i] == null) {
                                break; // Conversion failed
                            }
                            hasMessageParam = true;
                        } else if (!hasMessageParam && paramTypes[i] != Session.class) {
                            // Custom type - try to decode
                            args[i] = tryDecodeTextMessage(message, paramTypes[i]);
                            if (args[i] == null) {
                                break; // Decoding failed
                            }
                            hasMessageParam = true;
                        } else if (paramTypes[i] == Session.class) {
                            args[i] = session;
                        }
                    }
                    
                    if (hasMessageParam) {
                        Object result = method.invoke(endpointInstance, args);
                        // If method returns a value, send it back
                        if (result != null && method.getReturnType() != void.class) {
                            session.getBasicRemote().sendText(result.toString());
                        }
                        return;
                    }
                } catch (IllegalAccessException | InvocationTargetException | java.io.IOException e) {
                    invokeOnError(e.getCause() != null ? e.getCause() : e);
                }
            }
        }
    }
    
    /**
     * Invokes the @OnMessage method for binary messages.
     */
    private void invokeOnBinaryMessage(ByteBuffer data) {
        Method[] methods = endpointInstance.getClass().getDeclaredMethods();
        
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnMessage.class)) {
                try {
                    method.setAccessible(true);
                    Class<?>[] paramTypes = method.getParameterTypes();
                    
                    // Check if this method handles binary messages
                    boolean isBinaryHandler = false;
                    for (Class<?> paramType : paramTypes) {
                        if (paramType == ByteBuffer.class || 
                            paramType == byte[].class ||
                            paramType == java.io.InputStream.class) {
                            isBinaryHandler = true;
                            break;
                        }
                    }
                    
                    if (!isBinaryHandler) {
                        continue;
                    }
                    
                    Object[] args = new Object[paramTypes.length];
                    boolean hasMessageParam = false;
                    
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (!hasMessageParam && paramTypes[i] == ByteBuffer.class) {
                            args[i] = data;
                            hasMessageParam = true;
                        } else if (!hasMessageParam && paramTypes[i] == byte[].class) {
                            byte[] bytes = new byte[data.remaining()];
                            data.get(bytes);
                            data.rewind();
                            args[i] = bytes;
                            hasMessageParam = true;
                        } else if (paramTypes[i] == Session.class) {
                            args[i] = session;
                        }
                    }
                    
                    if (hasMessageParam) {
                        Object result = method.invoke(endpointInstance, args);
                        // If method returns a value, send it back as binary
                        if (result != null && result instanceof ByteBuffer) {
                            session.getBasicRemote().sendBinary((ByteBuffer) result);
                        } else if (result != null && result instanceof byte[]) {
                            session.getBasicRemote().sendBinary(ByteBuffer.wrap((byte[]) result));
                        }
                        return;
                    }
                } catch (IllegalAccessException | InvocationTargetException | java.io.IOException e) {
                    invokeOnError(e.getCause() != null ? e.getCause() : e);
                }
            }
        }
    }
    
    /**
     * Invokes the @OnClose method.
     */
    private void invokeOnClose(CloseReason closeReason) {
        Method[] methods = endpointInstance.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnClose.class)) {
                try {
                    method.setAccessible(true);
                    Class<?>[] paramTypes = method.getParameterTypes();
                    Object[] args = new Object[paramTypes.length];
                    
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (paramTypes[i] == Session.class) {
                            args[i] = session;
                        } else if (paramTypes[i] == CloseReason.class) {
                            args[i] = closeReason;
                        }
                    }
                    
                    method.invoke(endpointInstance, args);
                    return;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    System.err.println("Failed to invoke @OnClose: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Invokes the @OnError method.
     */
    private void invokeOnError(Throwable error) {
        Method[] methods = endpointInstance.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(OnError.class)) {
                try {
                    method.setAccessible(true);
                    Class<?>[] paramTypes = method.getParameterTypes();
                    Object[] args = new Object[paramTypes.length];
                    
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (paramTypes[i] == Session.class) {
                            args[i] = session;
                        } else if (Throwable.class.isAssignableFrom(paramTypes[i])) {
                            args[i] = error;
                        }
                    }
                    
                    method.invoke(endpointInstance, args);
                    return;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    System.err.println("Failed to invoke @OnError: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        // If no @OnError handler, print the error
        System.err.println("WebSocket error: " + error.getMessage());
        error.printStackTrace();
    }
    
    /**
     * Tries to decode a text message using registered decoders.
     */
    private Object tryDecodeTextMessage(String message, Class<?> targetType) {
        if (codecs == null) {
            return null;
        }
        try {
            return codecs.decodeText(message, targetType);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Checks if the given class is a primitive type or its wrapper.
     */
    private boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() || 
               type == Boolean.class ||
               type == Byte.class ||
               type == Character.class ||
               type == Short.class ||
               type == Integer.class ||
               type == Long.class ||
               type == Float.class ||
               type == Double.class;
    }
    
    /**
     * Converts a String message to a primitive type or wrapper.
     */
    private Object convertToPrimitive(String message, Class<?> targetType) {
        try {
            if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(message);
            } else if (targetType == byte.class || targetType == Byte.class) {
                return Byte.parseByte(message);
            } else if (targetType == char.class || targetType == Character.class) {
                if (message.length() == 1) {
                    return message.charAt(0);
                }
                return null;
            } else if (targetType == short.class || targetType == Short.class) {
                return Short.parseShort(message);
            } else if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(message);
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(message);
            } else if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(message);
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(message);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }
}
