package org.osgi.impl.websockets;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

@Component(service = HelloService.class)
public class MyHelloClient implements HelloService {


	private Session session;

	@Activate
	public MyHelloClient(WebSocketContainer container) throws DeploymentException, IOException, URISyntaxException {
		session = container.connectToServer(this, new URI("ws://localhost:3000/hello"));
	}

	@Deactivate
	public void close() throws IOException {
		session.close(new CloseReason(CloseCodes.GOING_AWAY, "service shut down"));
	}

	@Override
	public void sayHello(String message) throws IOException {
		session.getBasicRemote().sendText(message);
	}

	@OnMessage
	public void processMessage(String message) {
		System.out.println("Received message in client: " + message);
	}

}
