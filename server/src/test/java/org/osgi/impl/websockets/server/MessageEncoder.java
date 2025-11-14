package org.osgi.impl.websockets.server;

import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

/**
 * Text encoder for Message objects.
 * Encodes messages in format: "content|timestamp"
 */
public class MessageEncoder implements Encoder.Text<Message> {
    
    @Override
    public void init(EndpointConfig config) {
        // No initialization needed
    }
    
    @Override
    public void destroy() {
        // No cleanup needed
    }
    
    @Override
    public String encode(Message message) throws EncodeException {
        if (message == null) {
            throw new EncodeException(message, "Message cannot be null");
        }
        return message.getContent() + "|" + message.getTimestamp();
    }
}
