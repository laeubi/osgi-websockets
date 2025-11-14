package org.osgi.impl.websockets.server;

import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.EndpointConfig;

/**
 * Text decoder for Message objects.
 * Decodes messages in format: "content|timestamp"
 */
public class MessageDecoder implements Decoder.Text<Message> {
    
    @Override
    public void init(EndpointConfig config) {
        // No initialization needed
    }
    
    @Override
    public void destroy() {
        // No cleanup needed
    }
    
    @Override
    public Message decode(String s) throws DecodeException {
        if (s == null || s.isEmpty()) {
            throw new DecodeException(s, "String cannot be null or empty");
        }
        
        String[] parts = s.split("\\|");
        if (parts.length != 2) {
            throw new DecodeException(s, "Invalid message format. Expected: content|timestamp");
        }
        
        try {
            Message message = new Message();
            message.setContent(parts[0]);
            message.setTimestamp(Long.parseLong(parts[1]));
            return message;
        } catch (NumberFormatException e) {
            throw new DecodeException(s, "Invalid timestamp format", e);
        }
    }
    
    @Override
    public boolean willDecode(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        // Check if it matches the expected format: content|timestamp
        String[] parts = s.split("\\|");
        if (parts.length != 2) {
            return false;
        }
        // Try to parse the timestamp
        try {
            Long.parseLong(parts[1]);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
