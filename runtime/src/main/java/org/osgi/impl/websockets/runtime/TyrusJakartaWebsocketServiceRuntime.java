package org.osgi.impl.websockets.runtime;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.glassfish.tyrus.container.grizzly.client.GrizzlyContainerProvider;
import org.glassfish.tyrus.core.DefaultComponentProvider;
import org.glassfish.tyrus.core.TyrusServerEndpointConfigurator;
import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Referenced;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.component.AnyService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
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
import jakarta.websocket.server.ServerEndpoint;

@Component(service = { JakartaWebsocketServiceRuntime.class, WebSocketContainer.class }, immediate = true)
@Capability(namespace = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, //
		name = JakartaWebsocketWhiteboardConstants.WEBSOCKET, //
		version = JakartaWebsocketWhiteboardConstants.JAKARTA_WEBSOCKET_WHITEBOARD_SPECIFICATION_VERSION)
@Referenced({ TyrusServerEndpointConfigurator.class, DefaultComponentProvider.class, GrizzlyContainerProvider.class })
public class TyrusJakartaWebsocketServiceRuntime implements JakartaWebsocketServiceRuntime, WebSocketContainer {

	private BundleContext context;

	private final TyrusWebsocketServer server = new TyrusWebsocketServer();
	private final Map<ServiceReference<?>, Object> implementors = new ConcurrentHashMap<>();

	@Activate
	public TyrusJakartaWebsocketServiceRuntime(BundleContext context) {
		System.out.println("TyrusJakartaWebsocketServiceRuntime<init>()");
		this.context = context;
	}

	@Deactivate
	public void shutdown() {
		implementors.clear();
		server.stopServer();
	}

	@Reference(service = AnyService.class, target = "("
			+ JakartaWebsocketWhiteboardConstants.WEBSOCKET_ENDPOINT_IMPLEMENTOR
			+ "=true)", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public void addImplementor(ServiceReference<?> implementor) {
		System.out.println("######################################");
		System.out.println();
		System.out.println("Got implementor " + implementor);
		Object service = context.getService(implementor);
		if (service == null) {
			// TODO track error in DTO
			return;
		}
		System.out.println("Implementor service: " + service);
		System.out.println();
		System.out.println("######################################");
		Class<?> serviceClass = service.getClass();
		final ServerEndpoint wseAnnotation = serviceClass.getAnnotation(ServerEndpoint.class);
		if (wseAnnotation == null) {
			// TODO track error in DTO
			return;
		}
		implementors.put(implementor, wseAnnotation);
		try {
			server.addEndpoint(service);
		} catch (Exception e) {
			// TODO track error in DTO
		}
	}

	public void removeImplementor(ServiceReference<?> implementor) {
		Object service = implementors.remove(implementor);
		if (service != null) {
			server.removeEndpoint(service);
		}
	}

	@Override
	public long getDefaultAsyncSendTimeout() {
		return server.getWebSocketContainer().getDefaultAsyncSendTimeout();
	}

	@Override
	public void setAsyncSendTimeout(long timeoutmillis) {
		server.getWebSocketContainer().setAsyncSendTimeout(timeoutmillis);
	}

	@Override
	public Session connectToServer(Object annotatedEndpointInstance, URI path) throws DeploymentException, IOException {
		return server.getWebSocketContainer().connectToServer(annotatedEndpointInstance, path);
	}

	@Override
	public Session connectToServer(Class<?> annotatedEndpointClass, URI path) throws DeploymentException, IOException {
		return server.getWebSocketContainer().connectToServer(annotatedEndpointClass, path);
	}

	@Override
	public Session connectToServer(Endpoint endpointInstance, ClientEndpointConfig cec, URI path)
			throws DeploymentException, IOException {
		return server.getWebSocketContainer().connectToServer(endpointInstance, cec, path);
	}

	@Override
	public Session connectToServer(Class<? extends Endpoint> endpointClass, ClientEndpointConfig cec, URI path)
			throws DeploymentException, IOException {
		return server.getWebSocketContainer().connectToServer(endpointClass, cec, path);
	}

	@Override
	public long getDefaultMaxSessionIdleTimeout() {
		return server.getWebSocketContainer().getDefaultMaxSessionIdleTimeout();
	}

	@Override
	public void setDefaultMaxSessionIdleTimeout(long timeout) {
		server.getWebSocketContainer().setDefaultMaxSessionIdleTimeout(timeout);
	}

	@Override
	public int getDefaultMaxBinaryMessageBufferSize() {
		return server.getWebSocketContainer().getDefaultMaxBinaryMessageBufferSize();
	}

	@Override
	public void setDefaultMaxBinaryMessageBufferSize(int max) {
		server.getWebSocketContainer().setDefaultMaxBinaryMessageBufferSize(max);
	}

	@Override
	public int getDefaultMaxTextMessageBufferSize() {
		return server.getWebSocketContainer().getDefaultMaxTextMessageBufferSize();
	}

	@Override
	public void setDefaultMaxTextMessageBufferSize(int max) {
		server.getWebSocketContainer().setDefaultMaxTextMessageBufferSize(max);
	}

	@Override
	public Set<Extension> getInstalledExtensions() {
		return server.getWebSocketContainer().getInstalledExtensions();
	}

}
