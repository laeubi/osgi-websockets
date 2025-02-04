package org.osgi.impl.websockets.tck;

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
	}
}
