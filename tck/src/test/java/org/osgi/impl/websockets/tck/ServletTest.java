package org.osgi.impl.websockets.tck;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.impl.websockets.server.MyHelloServer;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.jakarta.websocket.runtime.JakartaWebsocketServiceRuntime;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

import jakarta.websocket.WebSocketContainer;

@ExtendWith(ServiceExtension.class)
@ExtendWith(BundleContextExtension.class)
public class ServletTest {
	@InjectService(timeout = 10000)
	JakartaWebsocketServiceRuntime runtime;

	@InjectService(timeout = 10000)
	WebSocketContainer container;

	@InjectService(timeout = 10000)
	MyHelloServer helloServer;

	@InjectService(timeout = 10000)
	HttpServiceRuntime httpServiceRuntime;

	@Test
	public void testEchoService(@InjectBundleContext BundleContext bundleContext) throws Exception {
		System.out.println("Testing the websocket service with http whiteboard");
		System.out.println("Runtime is:   " + runtime);
		System.out.println("Container is: " + container);
		System.out.println("Server is:    " + helloServer);
		System.out.println("HTTP is:      " + httpServiceRuntime);
		URI url = getURL();
		RuntimeDTO runtimeDTO = httpServiceRuntime.getRuntimeDTO();
		System.out.println(runtimeDTO);
		System.out.println("Server URL is: " + url);
		HttpClient client = HttpClient.newHttpClient();
		URI endpointUri = url.resolve("/hello");
		HttpRequest getrequest = HttpRequest.newBuilder(endpointUri).GET().build();
		String body = client.send(getrequest, BodyHandlers.ofString()).body();
		assertTrue(body.contains("This is a WS Endpoint"));
		System.out.println("=== Send Upgrade ===");
		try {
			HttpURLConnection connection = (HttpURLConnection) endpointUri.toURL().openConnection();
			connection.setRequestProperty("Upgrade", "websocket");
			connection.setRequestProperty("hello", "world");
			connection.setRequestProperty("Connection", "Upgrade");
			connection.setRequestProperty("Sec-WebSocket-Version", "13");
			connection.setRequestProperty("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==");
			connection.connect();
			Map<String, List<String>> headerFields = connection.getHeaderFields();
			System.out.println("-- response headers are:");
			for (Entry<String, List<String>> header : headerFields.entrySet()) {
				System.out.println(header + ": " + header.getValue().get(0));
			}
			InputStream errorStream = connection.getErrorStream();
			if (errorStream != null) {
				System.out.println("=== ERROR ===");
				System.out.println(new String(errorStream.readAllBytes()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		TimeUnit.SECONDS.sleep(60);
	}

	private URI getURL() throws Exception {
		Object endpoints = httpServiceRuntime.getRuntimeDTO().serviceDTO.properties.get("osgi.http.endpoint");
		if (endpoints instanceof String baseUrl) {
			return new URI(baseUrl);
		} else if (endpoints instanceof String[] baseUrls && baseUrls.length > 0) {
			return new URI(baseUrls[0]);
		}
		throw new IllegalStateException("Can't get the URL of the webserver!");
	}
}
