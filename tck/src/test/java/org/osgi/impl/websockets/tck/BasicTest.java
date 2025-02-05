package org.osgi.impl.websockets.tck;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.service.jakarta.websocket.runtime.JakartaWebsocketServiceRuntime;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

import jakarta.websocket.WebSocketContainer;

@ExtendWith(ServiceExtension.class)
@ExtendWith(BundleContextExtension.class)
public class BasicTest {

	@InjectService(timeout = 10000)
	JakartaWebsocketServiceRuntime runtime;

	@InjectService(timeout = 10000)
	WebSocketContainer container;

	@Test
	public void testEchoService(@InjectBundleContext BundleContext bundleContext) throws Exception {
		System.out.println("Testing the websocket service!");
		System.out.println(runtime);
		System.out.println(container);
		CountDownLatch latch = new CountDownLatch(1);
		URI uri = URI.create("ws://localhost:3000/hello");
		HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(uri,
				new Listener() {
					@Override
					public void onOpen(WebSocket webSocket) {
						System.out.println("Web Socket connection was opened!");
						latch.countDown();
					}
				});
		latch.await();
		TestClient client = new TestClient(container, uri);
		System.out.println("Client connected!");
		client.setMessage("Hello");
		String message = client.getNextMessage();
		assertEquals("Got your message (Hello). Thanks !", message);
	}
}
