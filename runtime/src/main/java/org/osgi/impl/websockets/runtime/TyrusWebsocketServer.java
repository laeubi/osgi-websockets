package org.osgi.impl.websockets.runtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.container.grizzly.client.GrizzlyClientContainer;
import org.glassfish.tyrus.spi.ServerContainer;
import org.glassfish.tyrus.spi.ServerContainerFactory;

import jakarta.websocket.DeploymentException;
import jakarta.websocket.WebSocketContainer;
import jakarta.websocket.server.ServerEndpoint;

class TyrusWebsocketServer {

	private static final int PORT = 3000;
	private static final String ROOT_PATH = "/";
	private WebSocketContainer websocketContainerDelegate;
	private List<Object> endpoints = new ArrayList<>();
	private Set<Class<?>> endpointClasses = new LinkedHashSet<>();
	private ServerContainer serverContainer;

	synchronized WebSocketContainer getWebSocketContainer() {
		if (websocketContainerDelegate == null) {
			Thread thread = Thread.currentThread();
			ClassLoader oldccl = thread.getContextClassLoader();
			try {
				thread.setContextClassLoader(TyrusWebsocketServer.class.getClassLoader());
				websocketContainerDelegate = ClientManager.createClient(GrizzlyClientContainer.class.getName());
			} finally {
				thread.setContextClassLoader(oldccl);
			}
		}
		return websocketContainerDelegate;
	}

	public synchronized void addEndpoint(Object service) throws DeploymentException, IOException {
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

	public synchronized void removeEndpoint(Object service) {
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
			ServerContainer serverContainer = ServerContainerFactory.createServerContainer();
			for (Class<?> endpoint : endpoints) {
				serverContainer.addEndpoint(endpoint);
			}
			System.out.println("Start server...");
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
		return "ws://localhost:" + PORT + ROOT_PATH + serverEndpointAnnotation.value();
	}

}
