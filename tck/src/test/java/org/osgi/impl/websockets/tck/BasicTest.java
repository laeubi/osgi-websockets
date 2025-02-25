package org.osgi.impl.websockets.tck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.impl.websockets.server.MyHelloServer;
import org.osgi.service.jakarta.websocket.runtime.JakartaWebsocketServiceRuntime;
import org.osgi.service.jakarta.websocket.runtime.dto.EndpointDTO;
import org.osgi.service.jakarta.websocket.runtime.dto.RuntimeDTO;
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

	@InjectService(timeout = 10000)
	MyHelloServer helloServer;

	@Test
	public void testEchoService(@InjectBundleContext BundleContext bundleContext) throws Exception {
		System.out.println("Testing the websocket service!");
		System.out.println("Runtime is:   " + runtime);
		System.out.println("Container is: " + container);
		System.out.println("Server is:    " + helloServer);
		URI uri = getURI();
		System.out.println("URI is: " + uri);
		CountDownLatch latch = new CountDownLatch(1);
		HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(uri, new Listener() {
			@Override
			public void onOpen(WebSocket webSocket) {
				System.out.println("Web Socket connection was opened!");
				latch.countDown();
			}
		}).join();
		assertTrue(latch.await(10, TimeUnit.SECONDS), "Wait for server response using http client timed out!");
		TestClient client = new TestClient(container, uri);
		System.out.println("Client connected!");
		client.setMessage("Hello");
		String message = client.getNextMessage();
		assertEquals("Got your message (Hello). Thanks !", message);
	}

	private URI getURI() throws URISyntaxException {
		RuntimeDTO dto = runtime.getRuntimeDTO();
		for (EndpointDTO endpoint : dto.endpoints) {
			if ("org.osgi.impl.websockets.server.MyHelloServer"
					.equals(endpoint.implementor.properties.get("component.name"))) {
				assertNotNull(endpoint.address, "Endpoint address is null");
				return new URI(endpoint.address);
			}
		}
		fail("Endpoint not found!");
		return null;
	}
}
