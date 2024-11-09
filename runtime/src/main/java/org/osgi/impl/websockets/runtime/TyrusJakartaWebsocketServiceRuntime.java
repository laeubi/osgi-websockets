package org.osgi.impl.websockets.runtime;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.container.grizzly.client.GrizzlyClientContainer;
import org.glassfish.tyrus.container.grizzly.client.GrizzlyContainerProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.AnyService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jakarta.websocket.runtime.JakartaWebsocketServiceRuntime;
import org.osgi.service.jakarta.websocket.whiteboard.JakartaWebsocketWhiteboardConstants;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

@Component(service = { JakartaWebsocketServiceRuntime.class, WebSocketContainer.class }, immediate = true)
public class TyrusJakartaWebsocketServiceRuntime implements JakartaWebsocketServiceRuntime, WebSocketContainer {

	private BundleContext context;
	private WebSocketContainer websocketContainerDelegate;

	public TyrusJakartaWebsocketServiceRuntime(BundleContext context) {
		this.context = context;
		Thread thread = Thread.currentThread();
		ClassLoader oldccl = thread.getContextClassLoader();
		try {
			thread.setContextClassLoader(GrizzlyContainerProvider.class.getClassLoader());
			this.websocketContainerDelegate = ClientManager.createClient(GrizzlyClientContainer.class.getName());
		} finally {
			thread.setContextClassLoader(oldccl);
		}
	}

	@Reference(service = AnyService.class, target = "("
			+ JakartaWebsocketWhiteboardConstants.WEBSOCKET_ENDPOINT_IMPLEMENTOR
			+ "=true)", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public void addImplementor(ServiceReference<?> implementor) {
		System.out.println("Got implementor " + implementor);
	}

	@Override
	public long getDefaultAsyncSendTimeout() {
		return websocketContainerDelegate.getDefaultAsyncSendTimeout();
	}

	@Override
	public void setAsyncSendTimeout(long timeoutmillis) {
		websocketContainerDelegate.setAsyncSendTimeout(timeoutmillis);
	}

	@Override
	public Session connectToServer(Object annotatedEndpointInstance, URI path) throws DeploymentException, IOException {
		return websocketContainerDelegate.connectToServer(getClass(), path);
	}

	@Override
	public Session connectToServer(Class<?> annotatedEndpointClass, URI path) throws DeploymentException, IOException {
		return websocketContainerDelegate.connectToServer(annotatedEndpointClass, path);
	}

	@Override
	public Session connectToServer(Endpoint endpointInstance, ClientEndpointConfig cec, URI path)
			throws DeploymentException, IOException {
		return websocketContainerDelegate.connectToServer(endpointInstance, cec, path);
	}

	@Override
	public Session connectToServer(Class<? extends Endpoint> endpointClass, ClientEndpointConfig cec, URI path)
			throws DeploymentException, IOException {
		return websocketContainerDelegate.connectToServer(endpointClass, cec, path);
	}

	@Override
	public long getDefaultMaxSessionIdleTimeout() {
		return websocketContainerDelegate.getDefaultMaxSessionIdleTimeout();
	}

	@Override
	public void setDefaultMaxSessionIdleTimeout(long timeout) {
		websocketContainerDelegate.setDefaultMaxSessionIdleTimeout(timeout);
	}

	@Override
	public int getDefaultMaxBinaryMessageBufferSize() {
		return websocketContainerDelegate.getDefaultMaxBinaryMessageBufferSize();
	}

	@Override
	public void setDefaultMaxBinaryMessageBufferSize(int max) {
		websocketContainerDelegate.setDefaultMaxBinaryMessageBufferSize(max);
	}

	@Override
	public int getDefaultMaxTextMessageBufferSize() {
		return websocketContainerDelegate.getDefaultMaxTextMessageBufferSize();
	}

	@Override
	public void setDefaultMaxTextMessageBufferSize(int max) {
		websocketContainerDelegate.setDefaultMaxTextMessageBufferSize(max);
	}

	@Override
	public Set<Extension> getInstalledExtensions() {
		return websocketContainerDelegate.getInstalledExtensions();
	}

}
