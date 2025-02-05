package org.osgi.impl.websockets.tck;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

@ClientEndpoint
public class TestClient {
	
	private Session session;

	private LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<String>();

	public TestClient(WebSocketContainer container, URI endpointAddress)
			throws DeploymentException, IOException, URISyntaxException {
		session = container.connectToServer(this, endpointAddress);
	}

	@OnMessage
	public void processMessage(String message) {
		System.out.println("Received message in client: " + message);
		messages.add(message);
	}

	public void setMessage(String string) throws IOException {
		session.getBasicRemote().sendText(string);
	}

	public String getNextMessage() throws InterruptedException {
		return messages.poll(10, TimeUnit.SECONDS);
	}

}
