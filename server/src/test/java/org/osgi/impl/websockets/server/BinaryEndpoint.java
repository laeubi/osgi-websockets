package org.osgi.impl.websockets.server;

import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A test endpoint for testing binary message support.
 */
@ServerEndpoint("/binary-test")
public class BinaryEndpoint {
    
    @OnMessage
    public void handleBinaryMessage(ByteBuffer data, Session session) throws IOException {
        // Echo the binary data back to the client
        session.getBasicRemote().sendBinary(data);
    }
}
