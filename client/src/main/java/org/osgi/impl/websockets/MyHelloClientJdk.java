package org.osgi.impl.websockets;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(service = HelloService.class)
public class MyHelloClientJdk implements HelloService, Listener {

	private CompletableFuture<WebSocket> client;

	@Activate
	public void start() {
		client = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create("ws://localhost:3000/hello"),
				this);
	}

	@Deactivate
	public void close() throws IOException {
		client.join().abort();
	}

	@Override
	public void sayHello(String message) throws IOException {
		client.thenAccept(ws -> ws.sendText(message, true));
	}

	@Override
	public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
		System.out.println("Received message in client: " + data);
		return CompletableFuture.completedStage(null);
	}

}
