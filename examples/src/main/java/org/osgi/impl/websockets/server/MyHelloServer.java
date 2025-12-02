package org.osgi.impl.websockets.server;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.jakarta.websocket.whiteboard.propertytypes.WhiteboardEndpoint;

import jakarta.websocket.OnMessage;
import jakarta.websocket.server.ServerEndpoint;

@WhiteboardEndpoint
@ServerEndpoint("/hello")
@Component(service = MyHelloServer.class, immediate = true)
public class MyHelloServer {
	@OnMessage
	public String handleMessage(String message) {
		return "Got your message (" + message + "). Thanks !";
	}
}