package org.osgi.impl.websockets.runtime.http;

import java.io.IOException;
import java.util.Enumeration;

import org.osgi.impl.websockets.runtime.TyrusWebsocketServer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.servlet.whiteboard.propertytypes.HttpWhiteboardFilterAsyncSupported;
import org.osgi.service.servlet.whiteboard.propertytypes.HttpWhiteboardFilterName;
import org.osgi.service.servlet.whiteboard.propertytypes.HttpWhiteboardFilterPattern;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

/**
 * Connects with the servlet whiteboard see
 * https://docs.osgi.org/specification/osgi.cmpn/8.1.0/service.servlet.html
 */
@Component(service = { HttpSessionListener.class, Filter.class })
@HttpWhiteboardFilterAsyncSupported
@HttpWhiteboardFilterPattern("/*")
@HttpWhiteboardFilterName("Tyrus Websocket Filter")
public class TyrusServletWhiteboardFilter implements Filter, HttpSessionListener {

	private final TyrusServletUpgrade tyrusServletUpgrade;
	private final TyrusWebsocketServer server;

	@Activate
	public TyrusServletWhiteboardFilter(@Reference TyrusWebsocketServer server) {
		this.server = server;
		this.tyrusServletUpgrade = new TyrusServletUpgrade(true);
	}

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
		System.out.println("TyrusServletWhiteboardFilter.init()");
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
		System.out.println("TyrusServletWhiteboardFilter.sessionCreated()");
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
		System.out.println("TyrusServletWhiteboardFilter.sessionDestroyed()");
        tyrusServletUpgrade.destroySession(se.getSession());
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
		if (request instanceof HttpServletRequest http) {
			System.out.println("------ [" + http.getRequestURI() + "] ------");
			Enumeration<String> headerNames = http.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String name = headerNames.nextElement();
				System.out.println(name + "=" + http.getHeader(name));
			}
		}
		boolean upgraded;
		try {
			upgraded = tyrusServletUpgrade.upgrade((HttpServletRequest) request, (HttpServletResponse) response,
				server.getEngine());
		} catch (Exception e) {
			e.printStackTrace();
			upgraded = false;
		}
		System.out.println("TyrusServletWhiteboardFilter.doFilter() upgraded=" + upgraded);
        if (!upgraded && filterChain != null) {
            filterChain.doFilter(request, response);
        }
    }

    @Override
	@Deactivate
    public void destroy() {
		System.out.println("TyrusServletWhiteboardFilter.destroy()");
        tyrusServletUpgrade.destroy();
    }
}
