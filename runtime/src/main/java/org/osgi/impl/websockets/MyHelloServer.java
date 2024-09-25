package org.osgi.impl.websockets;

import org.osgi.service.jakarta.websocket.whiteboard.propertytypes.WhiteboardEndpoint;

import jakarta.websocket.OnMessage;
import jakarta.websocket.server.ServerEndpoint;

@WhiteboardEndpoint
@ServerEndpoint("/hello")
public class MyHelloServer {
	@OnMessage
	public String handleMessage(String message) {
		return "Got your message (" + message + "). Thanks !";
	}
}