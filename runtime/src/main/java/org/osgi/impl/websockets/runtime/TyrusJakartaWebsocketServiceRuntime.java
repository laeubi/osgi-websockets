package org.osgi.impl.websockets.runtime;

import java.io.IOException;
import java.net.URI;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.glassfish.tyrus.container.grizzly.client.GrizzlyContainerProvider;
import org.glassfish.tyrus.core.DefaultComponentProvider;
import org.glassfish.tyrus.core.TyrusServerEndpointConfigurator;
import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Referenced;
import org.osgi.dto.DTO;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.component.AnyService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jakarta.websocket.runtime.JakartaWebsocketServiceRuntime;
import org.osgi.service.jakarta.websocket.runtime.dto.EndpointDTO;
import org.osgi.service.jakarta.websocket.runtime.dto.FailedEndpointDTO;
import org.osgi.service.jakarta.websocket.runtime.dto.RuntimeDTO;
import org.osgi.service.jakarta.websocket.whiteboard.JakartaWebsocketWhiteboardConstants;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

@Component(service = { WebSocketContainer.class }, immediate = true)
@Capability(namespace = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, //
		name = JakartaWebsocketWhiteboardConstants.WEBSOCKET, //
		version = JakartaWebsocketWhiteboardConstants.JAKARTA_WEBSOCKET_WHITEBOARD_SPECIFICATION_VERSION)
@Referenced({ TyrusServerEndpointConfigurator.class, DefaultComponentProvider.class, GrizzlyContainerProvider.class })
public class TyrusJakartaWebsocketServiceRuntime implements JakartaWebsocketServiceRuntime, WebSocketContainer {

	private ComponentContext context;

	private final TyrusWebsocketServer server = new TyrusWebsocketServer();
	private final Map<ServiceReference<?>, ImplementorContainer> implementors = new ConcurrentHashMap<>();
	private final AtomicLong changeCount = new AtomicLong();

	private ServiceRegistration<JakartaWebsocketServiceRuntime> registration;

	@Activate
	public TyrusJakartaWebsocketServiceRuntime(ComponentContext context) {
		this.context = context;
		// WORKAROUND for https://github.com/osgi/osgi/issues/809
		registration = context.getBundleContext().registerService(JakartaWebsocketServiceRuntime.class, this,
				getProperties());
	}

	private Dictionary<String, Long> getProperties() {
		return FrameworkUtil.asDictionary(Map.of(Constants.SERVICE_CHANGECOUNT, changeCount.getAndIncrement()));
	}

	@Deactivate
	public void shutdown() {
		registration.unregister();
		implementors.values().forEach(container -> container.dispose());
		implementors.clear();
		server.stopServer();
	}

	@Reference(service = AnyService.class, target = "("
			+ JakartaWebsocketWhiteboardConstants.WEBSOCKET_ENDPOINT_IMPLEMENTOR
			+ "=true)", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public void addImplementor(ServiceReference<?> implementor) {
		ImplementorContainer container = new ImplementorContainer(implementor, context.getBundleContext(), server);
		implementors.put(implementor, container);
		container.register();
		registration.setProperties(getProperties());
	}

	public void removeImplementor(ServiceReference<?> implementor) {
		ImplementorContainer container = implementors.remove(implementor);
		if (container != null) {
			container.dispose();
			registration.setProperties(getProperties());
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

	@Override
	public RuntimeDTO getRuntimeDTO() {
		RuntimeDTO dto = new RuntimeDTO();
		Map<Boolean, List<DTO>> collect = implementors.values().stream().map(container -> container.toDTO())
				.collect(Collectors.partitioningBy(ep -> ep instanceof FailedEndpointDTO));
		dto.failedEndpoints = collect.getOrDefault(true, List.of()).stream().map(FailedEndpointDTO.class::cast)
				.toArray(FailedEndpointDTO[]::new);
		dto.endpoints = collect.getOrDefault(false, List.of()).stream().map(EndpointDTO.class::cast)
				.toArray(EndpointDTO[]::new);
		dto.serviceReference = context.getServiceReference().adapt(ServiceReferenceDTO.class);
		return dto;
	}

}
