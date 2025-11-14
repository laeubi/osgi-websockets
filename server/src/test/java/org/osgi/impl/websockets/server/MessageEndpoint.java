package org.osgi.impl.websockets.server;

import jakarta.websocket.EncodeException;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;

/**
 * Endpoint that uses encoders and decoders.
 */
@ServerEndpoint(
    value = "/message-test",
    encoders = { MessageEncoder.class },
    decoders = { MessageDecoder.class }
)
public class MessageEndpoint {
    
    @OnMessage
    public void handleMessage(Message message, Session session) throws IOException, EncodeException {
        // Echo the message back, but modify the content
        Message response = new Message("Echo: " + message.getContent(), message.getTimestamp());
        session.getBasicRemote().sendObject(response);
    }
}
