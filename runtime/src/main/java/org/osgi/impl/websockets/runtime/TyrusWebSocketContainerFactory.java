package org.osgi.impl.websockets.runtime;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.container.grizzly.client.GrizzlyClientContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import jakarta.websocket.WebSocketContainer;

@Component(immediate = true)
public class TyrusWebSocketContainerFactory implements PrototypeServiceFactory<WebSocketContainer> {

	private TyrusWebsocketServer server;

	@Activate
	public TyrusWebSocketContainerFactory(@Reference TyrusWebsocketServer server, BundleContext bundleContext) {
		this.server = server;
		// WORKAROUND for https://github.com/osgi/osgi/issues/688
		bundleContext.registerService(WebSocketContainer.class, this, null);
	}

	@Override
	public WebSocketContainer getService(Bundle bundle, ServiceRegistration<WebSocketContainer> registration) {
		Thread thread = Thread.currentThread();
		ClassLoader oldccl = thread.getContextClassLoader();
		try {
			thread.setContextClassLoader(TyrusWebsocketServer.class.getClassLoader());
			return ClientManager.createClient(GrizzlyClientContainer.class.getName());
		} finally {
			thread.setContextClassLoader(oldccl);
		}
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<WebSocketContainer> registration,
			WebSocketContainer service) {
		// we don't care here...
	}

}
