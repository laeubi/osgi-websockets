package org.osgi.impl.websockets.server;

import jakarta.websocket.Decoder;
import jakarta.websocket.Encoder;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages encoders and decoders for a WebSocket endpoint.
 * This class handles instantiation and lifecycle of encoder/decoder instances.
 */
public class EndpointCodecs {
    
    private final List<Encoder> encoders = new ArrayList<>();
    private final List<Decoder> decoders = new ArrayList<>();
    private final Map<Class<?>, Encoder> encoderByType = new ConcurrentHashMap<>();
    private final Map<Class<?>, Decoder> decoderByType = new ConcurrentHashMap<>();
    
    /**
     * Creates encoder and decoder instances from the endpoint's @ServerEndpoint annotation.
     * 
     * @param endpointClass The endpoint class
     * @param config The endpoint configuration (can be null)
     * @throws InstantiationException if encoder/decoder instantiation fails
     */
    public EndpointCodecs(Class<?> endpointClass, ServerEndpointConfig config) throws InstantiationException {
        ServerEndpoint annotation = endpointClass.getAnnotation(ServerEndpoint.class);
        if (annotation != null) {
            // Initialize encoders from annotation
            for (Class<? extends Encoder> encoderClass : annotation.encoders()) {
                try {
                    Encoder encoder = encoderClass.getDeclaredConstructor().newInstance();
                    encoder.init(config);
                    encoders.add(encoder);
                    // Register encoder by its generic type
                    registerEncoderByType(encoder);
                } catch (Exception e) {
                    throw new InstantiationException("Failed to instantiate encoder " + 
                        encoderClass.getName() + ": " + e.getMessage());
                }
            }
            
            // Initialize decoders from annotation
            for (Class<? extends Decoder> decoderClass : annotation.decoders()) {
                try {
                    Decoder decoder = decoderClass.getDeclaredConstructor().newInstance();
                    decoder.init(config);
                    decoders.add(decoder);
                    // Register decoder by its generic type
                    registerDecoderByType(decoder);
                } catch (Exception e) {
                    throw new InstantiationException("Failed to instantiate decoder " + 
                        decoderClass.getName() + ": " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Registers an encoder in the type map based on its generic type parameter.
     */
    private void registerEncoderByType(Encoder encoder) {
        // Try to find the type parameter T in Encoder<T>
        Class<?> encodedType = findGenericType(encoder.getClass(), Encoder.class);
        if (encodedType != null) {
            encoderByType.put(encodedType, encoder);
        }
    }
    
    /**
     * Registers a decoder in the type map based on its generic type parameter.
     */
    private void registerDecoderByType(Decoder decoder) {
        // Try to find the type parameter T in Decoder<T>
        Class<?> decodedType = findGenericType(decoder.getClass(), Decoder.class);
        if (decodedType != null) {
            decoderByType.put(decodedType, decoder);
        }
    }
    
    /**
     * Attempts to find the generic type parameter of an interface.
     * This is a simplified implementation that looks for the first concrete type.
     */
    private Class<?> findGenericType(Class<?> implClass, Class<?> interfaceClass) {
        try {
            java.lang.reflect.Type[] genericInterfaces = implClass.getGenericInterfaces();
            for (java.lang.reflect.Type genericInterface : genericInterfaces) {
                if (genericInterface instanceof java.lang.reflect.ParameterizedType) {
                    java.lang.reflect.ParameterizedType paramType = 
                        (java.lang.reflect.ParameterizedType) genericInterface;
                    java.lang.reflect.Type rawType = paramType.getRawType();
                    
                    // Check if this is the interface we're looking for or a subinterface
                    if (rawType instanceof Class && 
                        interfaceClass.isAssignableFrom((Class<?>) rawType)) {
                        java.lang.reflect.Type[] typeArgs = paramType.getActualTypeArguments();
                        if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                            return (Class<?>) typeArgs[0];
                        }
                    }
                }
            }
            
            // Check superclass
            Class<?> superClass = implClass.getSuperclass();
            if (superClass != null && superClass != Object.class) {
                return findGenericType(superClass, interfaceClass);
            }
        } catch (Exception e) {
            // Ignore reflection errors
        }
        return null;
    }
    
    /**
     * Encodes an object to a String using a text encoder.
     * 
     * @param obj The object to encode
     * @return The encoded string
     * @throws jakarta.websocket.EncodeException if encoding fails
     */
    public String encodeText(Object obj) throws jakarta.websocket.EncodeException {
        if (obj == null) {
            throw new IllegalArgumentException("Cannot encode null object");
        }
        
        // Try to find encoder for the specific type
        Encoder encoder = encoderByType.get(obj.getClass());
        if (encoder instanceof Encoder.Text) {
            @SuppressWarnings("unchecked")
            Encoder.Text<Object> textEncoder = (Encoder.Text<Object>) encoder;
            return textEncoder.encode(obj);
        }
        
        // Try all text encoders
        for (Encoder enc : encoders) {
            if (enc instanceof Encoder.Text) {
                @SuppressWarnings("unchecked")
                Encoder.Text<Object> textEncoder = (Encoder.Text<Object>) enc;
                try {
                    return textEncoder.encode(obj);
                } catch (Exception e) {
                    // Try next encoder
                }
            }
        }
        
        throw new jakarta.websocket.EncodeException(obj, 
            "No text encoder found for type: " + obj.getClass().getName());
    }
    
    /**
     * Encodes an object to a ByteBuffer using a binary encoder.
     * 
     * @param obj The object to encode
     * @return The encoded binary data
     * @throws jakarta.websocket.EncodeException if encoding fails
     */
    public java.nio.ByteBuffer encodeBinary(Object obj) throws jakarta.websocket.EncodeException {
        if (obj == null) {
            throw new IllegalArgumentException("Cannot encode null object");
        }
        
        // Try to find encoder for the specific type
        Encoder encoder = encoderByType.get(obj.getClass());
        if (encoder instanceof Encoder.Binary) {
            @SuppressWarnings("unchecked")
            Encoder.Binary<Object> binaryEncoder = (Encoder.Binary<Object>) encoder;
            return binaryEncoder.encode(obj);
        }
        
        // Try all binary encoders
        for (Encoder enc : encoders) {
            if (enc instanceof Encoder.Binary) {
                @SuppressWarnings("unchecked")
                Encoder.Binary<Object> binaryEncoder = (Encoder.Binary<Object>) enc;
                try {
                    return binaryEncoder.encode(obj);
                } catch (Exception e) {
                    // Try next encoder
                }
            }
        }
        
        throw new jakarta.websocket.EncodeException(obj, 
            "No binary encoder found for type: " + obj.getClass().getName());
    }
    
    /**
     * Decodes a String message using a text decoder.
     * 
     * @param <T> The expected return type
     * @param message The message to decode
     * @param targetType The target type to decode to
     * @return The decoded object, or null if no decoder can handle it
     * @throws jakarta.websocket.DecodeException if decoding fails
     */
    public <T> T decodeText(String message, Class<T> targetType) throws jakarta.websocket.DecodeException {
        // Try to find decoder for the specific type
        Decoder decoder = decoderByType.get(targetType);
        if (decoder instanceof Decoder.Text) {
            @SuppressWarnings("unchecked")
            Decoder.Text<T> textDecoder = (Decoder.Text<T>) decoder;
            if (textDecoder.willDecode(message)) {
                return textDecoder.decode(message);
            }
        }
        
        // Try all text decoders
        for (Decoder dec : decoders) {
            if (dec instanceof Decoder.Text) {
                @SuppressWarnings("unchecked")
                Decoder.Text<T> textDecoder = (Decoder.Text<T>) dec;
                try {
                    if (textDecoder.willDecode(message)) {
                        return textDecoder.decode(message);
                    }
                } catch (Exception e) {
                    // Try next decoder
                }
            }
        }
        
        return null; // No decoder found
    }
    
    /**
     * Decodes a ByteBuffer message using a binary decoder.
     * 
     * @param <T> The expected return type
     * @param data The data to decode
     * @param targetType The target type to decode to
     * @return The decoded object, or null if no decoder can handle it
     * @throws jakarta.websocket.DecodeException if decoding fails
     */
    public <T> T decodeBinary(java.nio.ByteBuffer data, Class<T> targetType) 
            throws jakarta.websocket.DecodeException {
        // Try to find decoder for the specific type
        Decoder decoder = decoderByType.get(targetType);
        if (decoder instanceof Decoder.Binary) {
            @SuppressWarnings("unchecked")
            Decoder.Binary<T> binaryDecoder = (Decoder.Binary<T>) decoder;
            if (binaryDecoder.willDecode(data)) {
                return binaryDecoder.decode(data);
            }
        }
        
        // Try all binary decoders
        for (Decoder dec : decoders) {
            if (dec instanceof Decoder.Binary) {
                @SuppressWarnings("unchecked")
                Decoder.Binary<T> binaryDecoder = (Decoder.Binary<T>) dec;
                try {
                    // Need to duplicate the buffer for willDecode check
                    java.nio.ByteBuffer duplicate = data.duplicate();
                    if (binaryDecoder.willDecode(duplicate)) {
                        return binaryDecoder.decode(data);
                    }
                } catch (Exception e) {
                    // Try next decoder
                }
            }
        }
        
        return null; // No decoder found
    }
    
    /**
     * Checks if there's a text encoder for the given object type.
     */
    public boolean hasTextEncoder(Class<?> type) {
        Encoder encoder = encoderByType.get(type);
        if (encoder instanceof Encoder.Text) {
            return true;
        }
        // Check all encoders
        for (Encoder enc : encoders) {
            if (enc instanceof Encoder.Text) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if there's a binary encoder for the given object type.
     */
    public boolean hasBinaryEncoder(Class<?> type) {
        Encoder encoder = encoderByType.get(type);
        if (encoder instanceof Encoder.Binary) {
            return true;
        }
        // Check all encoders
        for (Encoder enc : encoders) {
            if (enc instanceof Encoder.Binary) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Destroys all encoder and decoder instances.
     */
    public void destroy() {
        for (Encoder encoder : encoders) {
            try {
                encoder.destroy();
            } catch (Exception e) {
                System.err.println("Error destroying encoder: " + e.getMessage());
            }
        }
        for (Decoder decoder : decoders) {
            try {
                decoder.destroy();
            } catch (Exception e) {
                System.err.println("Error destroying decoder: " + e.getMessage());
            }
        }
        encoders.clear();
        decoders.clear();
        encoderByType.clear();
        decoderByType.clear();
    }
}
