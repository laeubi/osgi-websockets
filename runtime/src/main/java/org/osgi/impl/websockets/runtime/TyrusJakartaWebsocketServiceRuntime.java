package org.osgi.impl.websockets.runtime;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.AnyService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jakarta.websocket.runtime.JakartaWebsocketServiceRuntime;
import org.osgi.service.jakarta.websocket.whiteboard.JakartaWebsocketWhiteboardConstants;

@Component(service = JakartaWebsocketServiceRuntime.class, immediate = true)
public class TyrusJakartaWebsocketServiceRuntime implements JakartaWebsocketServiceRuntime {

	private BundleContext context;

	public TyrusJakartaWebsocketServiceRuntime(BundleContext context) {
		this.context = context;
	}

	@Reference(service = AnyService.class, target = "("
			+ JakartaWebsocketWhiteboardConstants.WEBSOCKET_ENDPOINT_IMPLEMENTOR
			+ "=true)", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public void addImplementor(ServiceReference<?> implementor) {
		System.out.println("Got implementor " + implementor);
	}

}
