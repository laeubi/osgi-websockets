package org.osgi.impl.websockets.runtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.glassfish.tyrus.core.frame.TyrusFrame.FrameType;
import org.glassfish.tyrus.core.monitoring.ApplicationEventListener;
import org.glassfish.tyrus.core.monitoring.EndpointEventListener;
import org.glassfish.tyrus.core.monitoring.MessageEventListener;
import org.glassfish.tyrus.spi.ServerContainer;
import org.glassfish.tyrus.spi.ServerContainerFactory;
import org.glassfish.tyrus.spi.WebSocketEngine;
import org.osgi.service.component.annotations.Component;

import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerEndpoint;

@Component(service = TyrusWebsocketServer.class)
public class TyrusWebsocketServer {

	private static final int PORT = 3000;
	private static final String ROOT_PATH = "/";
	private List<Object> endpoints = new ArrayList<>();
	private Set<Class<?>> endpointClasses = new LinkedHashSet<>();
	private ServerContainer serverContainer;

	synchronized void addEndpoint(Object service) throws DeploymentException, IOException {
		LinkedHashSet<Class<?>> next = new LinkedHashSet<>(endpointClasses);
		// First check if we have not deployed this endpoint already
		if (next.add(service.getClass())) {
			// now check if we can deploy the server with the new set of endpoints
			try {
				deployServer(next);
				endpoints.add(service);
				endpointClasses = next;
			} catch (DeploymentException e) {
				// if deployment fails, redeploy with the old set and throw...
				deployCurrent();
				throw e;
			}
		} else {
			throw new DeploymentException("Each endpoint can only be deployed once");
		}
	}

	synchronized void removeEndpoint(Object service) {
		if (endpoints.remove(service) && endpointClasses.remove(service.getClass())) {
			try {
				deployCurrent();
			} catch (IOException e) {
				System.err.println("Starting server failed after remove of endpoint: " + e);
			}
		}
	}

	private void deployCurrent() throws IOException {
		try {
			deployServer(endpointClasses);
		} catch (DeploymentException e) {
			// should not happen and even if we can't do much...
			System.err.println("Deploy server failed: " + e);
		}
	}

	private synchronized void deployServer(Set<Class<?>> endpoints) throws DeploymentException, IOException {
		stopServer();
		if (endpoints.isEmpty()) {
			return;
		}
		Thread thread = Thread.currentThread();
		ClassLoader oldccl = thread.getContextClassLoader();
		try {
			thread.setContextClassLoader(TyrusWebsocketServer.class.getClassLoader());
			serverContainer = ServerContainerFactory.createServerContainer(
					Map.of(ApplicationEventListener.APPLICATION_EVENT_LISTENER, new ApplicationEventListener() {

						@Override
						public void onEndpointUnregistered(String endpointPath) {
							System.out.println("Endpoint unregistered: " + endpointPath);
						}

						@Override
						public EndpointEventListener onEndpointRegistered(String endpointPath, Class<?> endpointClass) {
							System.out.println("Endpoint registered: " + endpointPath + " (" + endpointClass + ")");
							return new EndpointEventListener() {

								@Override
								public MessageEventListener onSessionOpened(String sessionId) {
									System.out.println("Session opened: " + sessionId);
									return new MessageEventListener() {

										@Override
										public void onFrameSent(FrameType frameType, long payloadLength) {
											System.out.println("Frame send: " + frameType);
										}

										@Override
										public void onFrameReceived(FrameType frameType, long payloadLength) {
											System.out.println("Frame received: " + frameType);
										}
									};
								}

								@Override
								public void onSessionClosed(String sessionId) {
									System.out.println("Session closed: " + sessionId);
								}

								@Override
								public void onError(String sessionId, Throwable t) {
									System.out.println("Error: " + sessionId + " -> " + t);
								}
							};
						}

						@Override
						public void onApplicationInitialized(String applicationName) {
							System.out.println("Initialized Application");
						}

						@Override
						public void onApplicationDestroyed() {
							System.out.println("Destroyed Application");
						}
					}));
			System.out.println("Server Container is " + serverContainer.getClass());
			for (Class<?> endpoint : endpoints) {
				serverContainer.addEndpoint(endpoint);
			}
			try {
				serverContainer.start(ROOT_PATH, PORT);
			} catch (DeploymentException e) {
				System.out.println("Deployment failed: " + e);
				throw e;
			} catch (IOException e) {
				System.out.println("Start failed: " + e);
				throw e;
			} catch (RuntimeException e) {
				System.out.println("Internal error: " + e);
				throw e;
			}
			System.out.println("Server started!");
			// TyrusServletContainerInitializer servletContainerInitializer = new
			// TyrusServletContainerInitializer();
			// https://docs.osgi.org/specification/osgi.cmpn/8.1.0/service.servlet.html#d0e87922
		} finally {
			thread.setContextClassLoader(oldccl);
		}
	}

	synchronized void stopServer() {
		if (serverContainer != null) {
			System.out.println("Stop server...");
			serverContainer.stop();
			System.out.println("Server stopped!");
			serverContainer = null;
		}
	}

	public synchronized String getAddress(ServerEndpoint serverEndpointAnnotation) {
		return "ws://localhost:" + PORT + (ROOT_PATH + serverEndpointAnnotation.value()).replaceAll("//+", "/");
	}

	public synchronized WebSocketEngine getEngine() {
		if (serverContainer == null) {
			return null;
		}
		return serverContainer.getWebSocketEngine();
	}

}
