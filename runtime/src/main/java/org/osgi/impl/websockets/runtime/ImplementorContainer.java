package org.osgi.impl.websockets.runtime;

import java.io.IOException;

import org.osgi.dto.DTO;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.jakarta.websocket.runtime.dto.EndpointDTO;
import org.osgi.service.jakarta.websocket.runtime.dto.FailedEndpointDTO;

import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerEndpoint;

class ImplementorContainer {

	private ServiceReference<?> reference;
	private Object service;
	private BundleContext context;

	private int errorCode;
	private Exception exception;
	private TyrusWebsocketServer server;
	private boolean registered;
	private ServerEndpoint serverEndpointAnnotation;

	ImplementorContainer(ServiceReference<?> reference, BundleContext context, TyrusWebsocketServer server) {
		this.reference = reference;
		this.context = context;
		this.server = server;
	}

	synchronized void dispose() {
		if (!registered) {
			return;
		}
		registered = false;
		exception = null;
		errorCode = -1;
		if (service != null) {
			server.removeEndpoint(service);
			context.ungetService(reference);
		}
	}

	synchronized void register() {
		if (registered) {
			return;
		}
		registered = true;
		try {
			service = context.getService(reference);
			if (service == null) {
				errorCode = FailedEndpointDTO.FAILURE_REASON_SERVICE_NOT_GETTABLE;
				return;
			}
			Class<?> serviceClass = service.getClass();
			serverEndpointAnnotation = serviceClass.getAnnotation(ServerEndpoint.class);
			if (serverEndpointAnnotation == null) {
				errorCode = FailedEndpointDTO.FAILURE_REASON_INVALID;
				exception = new IllegalArgumentException("Endpoint is missing @ServerEndpoint annotation");
				return;
			}
			try {
				server.addEndpoint(service);
			} catch (DeploymentException e) {
				errorCode = FailedEndpointDTO.FAILURE_REASON_INVALID;
				exception = e;
			} catch (IOException e) {
				errorCode = FailedEndpointDTO.FAILURE_REASON_UNKNOWN;
				exception = e;
			}
		} catch (RuntimeException e) {
			exception = e;
			errorCode = FailedEndpointDTO.FAILURE_REASON_UNKNOWN;
		}
	}

	synchronized DTO toDTO() {
		if (errorCode > 0) {
			FailedEndpointDTO dto = new FailedEndpointDTO();
			dto.failureCode = errorCode;
			if (exception != null) {
				dto.failureMessage = exception.getMessage();
			}
			dto.implementor = reference.adapt(ServiceReferenceDTO.class);
			return dto;
		}
		EndpointDTO dto = new EndpointDTO();
		dto.implementor = reference.adapt(ServiceReferenceDTO.class);
		dto.address = server.getAddress(serverEndpointAnnotation);
		return dto;
	}

}
