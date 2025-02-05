package org.osgi.impl.websockets.runtime;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.container.grizzly.client.GrizzlyClientContainer;
import org.glassfish.tyrus.container.grizzly.client.GrizzlyContainerProvider;
import org.glassfish.tyrus.core.DefaultComponentProvider;
import org.glassfish.tyrus.core.TyrusServerEndpointConfigurator;
import org.glassfish.tyrus.spi.ServerContainer;
import org.glassfish.tyrus.spi.ServerContainerFactory;
import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Referenced;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.component.AnyService;
import org.osgi.service.component.annotations.Activate;
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
import jakarta.websocket.server.ServerEndpoint;

@Component(service = { JakartaWebsocketServiceRuntime.class, WebSocketContainer.class }, immediate = true)
@Capability(namespace = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, //
		name = JakartaWebsocketWhiteboardConstants.WEBSOCKET, //
		version = JakartaWebsocketWhiteboardConstants.JAKARTA_WEBSOCKET_WHITEBOARD_SPECIFICATION_VERSION)
@Referenced({ TyrusServerEndpointConfigurator.class, DefaultComponentProvider.class, GrizzlyContainerProvider.class })
public class TyrusJakartaWebsocketServiceRuntime implements JakartaWebsocketServiceRuntime, WebSocketContainer {

	private BundleContext context;
	private WebSocketContainer websocketContainerDelegate;

	@Activate
	public TyrusJakartaWebsocketServiceRuntime(BundleContext context) {
		System.out.println("TyrusJakartaWebsocketServiceRuntime<init>()");
		this.context = context;
		try {
			Thread thread = Thread.currentThread();
			ClassLoader oldccl = thread.getContextClassLoader();
			try {
				thread.setContextClassLoader(TyrusJakartaWebsocketServiceRuntime.class.getClassLoader());
				this.websocketContainerDelegate = ClientManager.createClient(GrizzlyClientContainer.class.getName());
			} finally {
				thread.setContextClassLoader(oldccl);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Reference(service = AnyService.class, target = "("
			+ JakartaWebsocketWhiteboardConstants.WEBSOCKET_ENDPOINT_IMPLEMENTOR
			+ "=true)", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public void addImplementor(ServiceReference<?> implementor) {
		System.out.println("######################################");
		System.out.println();
		System.out.println("Got implementor " + implementor);
		Object service = context.getService(implementor);
		createServer(service);
		System.out.println("Implementor service: " + service);
		System.out.println();
		System.out.println("######################################");
	}

	private void createServer(Object service) {
		try {
			Thread thread = Thread.currentThread();
			ClassLoader oldccl = thread.getContextClassLoader();
			try {
				Class<?> serviceClass = service.getClass();
				final ServerEndpoint wseAnnotation = serviceClass.getAnnotation(ServerEndpoint.class);
				if (wseAnnotation == null) {
					throw new DeploymentException(
							"Service must be an jakarta.websocket.server.ServerEndpoint annotated class!");
				}
				thread.setContextClassLoader(TyrusJakartaWebsocketServiceRuntime.class.getClassLoader());
				System.out.println("start");
				ServerContainer serverContainer = ServerContainerFactory.createServerContainer();
				serverContainer.addEndpoint(service.getClass());
				serverContainer.start("/", 3000);
			} finally {
				thread.setContextClassLoader(oldccl);
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}

	}

	public void removeImplementor(ServiceReference<?> implementor) {

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
		return websocketContainerDelegate.connectToServer(annotatedEndpointInstance, path);
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
