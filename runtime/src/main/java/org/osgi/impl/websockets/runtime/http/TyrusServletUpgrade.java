/*
 * Copyright (c) 2012, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.osgi.impl.websockets.runtime.http;

import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.glassfish.tyrus.core.RequestContext;
import org.glassfish.tyrus.core.TyrusUpgradeResponse;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.core.Utils;
import org.glassfish.tyrus.core.wsadl.model.Application;
import org.glassfish.tyrus.spi.UpgradeResponse;
import org.glassfish.tyrus.spi.WebSocketEngine;
import org.glassfish.tyrus.spi.Writer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.WebConnection;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

class TyrusServletUpgrade {
    private static final Logger LOGGER = Logger.getLogger(TyrusServletUpgrade.class.getName());

    // I don't like this map, but it seems like it is necessary. I am forced to handle subscriptions
    // for HttpSessionListener because the listener itself must be registered *before* ServletContext
    // initialization.
    // I could create List of listeners and send a create something like sessionDestroyed(HttpSession s)
    // but that would take more time (statistically higher number of comparisons).
    private final Map<HttpSession, TyrusHttpUpgradeHandler> sessionToHandler = new ConcurrentHashMap<HttpSession,
            TyrusHttpUpgradeHandler>();

    private JAXBContext wsadlJaxbContext;

    private static class TyrusHttpUpgradeHandlerProxy extends TyrusHttpUpgradeHandler {

        private TyrusHttpUpgradeHandler handler;

        @Override
        public void init(WebConnection wc) {
            handler.init(wc);
        }

        @Override
        public void onDataAvailable() {
            handler.onDataAvailable();
        }

        @Override
        public void onAllDataRead() {
            handler.onAllDataRead();
        }

        @Override
        public void onError(Throwable t) {
            handler.onError(t);
        }

        @Override
        public void destroy() {
            handler.destroy();
        }

        @Override
        public void sessionDestroyed() {
            handler.sessionDestroyed();
        }

        @Override
        public void preInit(WebSocketEngine.UpgradeInfo upgradeInfo, Writer writer, boolean authenticated) {
            handler.preInit(upgradeInfo, writer, authenticated);
        }

        @Override
        public void setIncomingBufferSize(int incomingBufferSize) {
            handler.setIncomingBufferSize(incomingBufferSize);
        }

        @Override
        WebConnection getWebConnection() {
            return handler.getWebConnection();
        }

        void setHandler(TyrusHttpUpgradeHandler handler) {
            this.handler = handler;
        }
    }

	private final boolean wsadlEnabled;

	public TyrusServletUpgrade(boolean wsadlEnabled) {
		this.wsadlEnabled = wsadlEnabled;
	}

    /**
     * provide the HTTP upgrade
     * @param httpServletRequest servlet request
     * @param httpServletResponse servlet response
     * @return Return true if response is set, i.e. in filter chain the next filter should not be invoked.
     * @throws IOException
     * @throws ServletException
     */
    boolean upgrade(HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse, WebSocketEngine engine)
			throws IOException, ServletException {
		if (engine == null) {
			// NO engine no upgrade
			return false;
		}
        // check for mandatory websocket header
        final String header = httpServletRequest.getHeader(HandshakeRequest.SEC_WEBSOCKET_KEY);
		System.out.println("TyrusServletUpgrade.upgrade() header=" + header);
        if (header != null) {
            LOGGER.fine("Setting up WebSocket protocol handler");

            final TyrusHttpUpgradeHandlerProxy handler = new TyrusHttpUpgradeHandlerProxy();

            final Map<String, String[]> paramMap = httpServletRequest.getParameterMap();

            final TyrusServletWriter webSocketConnection = new TyrusServletWriter(handler);

            final RequestContext requestContext = RequestContext.Builder
                    .create()
                    .requestURI(URI.create(httpServletRequest.getRequestURI()))
                    .queryString(httpServletRequest.getQueryString())
                    .httpSession(httpServletRequest.getSession(false))
                    .secure(httpServletRequest.isSecure())
                    .userPrincipal(httpServletRequest.getUserPrincipal())
                    .isUserInRoleDelegate(new RequestContext.Builder.IsUserInRoleDelegate() {
                        @Override
                        public boolean isUserInRole(String role) {
                            return httpServletRequest
                                    .isUserInRole(role);
                        }
                    }).parameterMap(paramMap)
                    .remoteAddr(httpServletRequest.getRemoteAddr())
                    .serverAddr(httpServletRequest.getLocalName() == null
                            ? httpServletRequest.getLocalAddr() : httpServletRequest.getLocalName())
                    .serverPort(httpServletRequest.getLocalPort())
                    .tyrusProperties(getInitParams(httpServletRequest.getServletContext()))
                    .build();

            Enumeration<String> headerNames = httpServletRequest.getHeaderNames();

            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();

                Enumeration<String> headerValues = httpServletRequest.getHeaders(name);

                while (headerValues.hasMoreElements()) {

                    final List<String> values = requestContext.getHeaders().get(name);
                    if (values == null) {
                        requestContext.getHeaders().put(name, Utils.parseHeaderValue(
                                headerValues.nextElement().trim()));
                    } else {
                        values.addAll(Utils.parseHeaderValue(headerValues.nextElement().trim()));
                    }
                }
            }

            final TyrusUpgradeResponse tyrusUpgradeResponse = new TyrusUpgradeResponse();
            final WebSocketEngine.UpgradeInfo upgradeInfo = engine.upgrade(requestContext, tyrusUpgradeResponse);
            switch (upgradeInfo.getStatus()) {
                case HANDSHAKE_FAILED:
                    appendAllHeaders(httpServletResponse, tyrusUpgradeResponse);
                    httpServletResponse.sendError(tyrusUpgradeResponse.getStatus());
                    break;
                case NOT_APPLICABLE:
                    appendTraceHeaders(httpServletResponse, tyrusUpgradeResponse);
                    return false;
                case SUCCESS:
                    LOGGER.fine("Upgrading Servlet request");

                    handler.setHandler(httpServletRequest.upgrade(TyrusHttpUpgradeHandler.class));
                    final String frameBufferSize =
                            httpServletRequest.getServletContext().getInitParameter(TyrusHttpUpgradeHandler.FRAME_BUFFER_SIZE);
                    if (frameBufferSize != null) {
                        handler.setIncomingBufferSize(Integer.parseInt(frameBufferSize));
                    }

                    handler.preInit(upgradeInfo, webSocketConnection, httpServletRequest.getUserPrincipal() != null);

                    if (requestContext.getHttpSession() != null) {
                        sessionToHandler.put((HttpSession) requestContext.getHttpSession(), handler);
                    }

                    httpServletResponse.setStatus(tyrusUpgradeResponse.getStatus());
                    appendAllHeaders(httpServletResponse, tyrusUpgradeResponse);

                    httpServletResponse.flushBuffer();
                    LOGGER.fine("Handshake Complete");
                    break;
            }
            return true;
        } else {
			if (wsadlEnabled && engine instanceof TyrusWebSocketEngine wsadl) { // wsadl
                if (httpServletRequest.getMethod().equals("GET")
                        && httpServletRequest.getRequestURI().endsWith("application.wsadl")) {
                    try {
                        getWsadlJaxbContext().createMarshaller().marshal(
								wsadl.getWsadlApplication(), httpServletResponse.getWriter()
                        );
                    } catch (JAXBException e) {
                        throw new ServletException(e);
                    }
                    httpServletResponse.setStatus(200);
                    httpServletResponse.setContentType("application/wsadl+xml");
                    httpServletResponse.flushBuffer();
                    return true;
                }
            }
            return false;
        }
    }

    TyrusHttpUpgradeHandler destroySession(HttpSession session) {
        final TyrusHttpUpgradeHandler upgradeHandler = sessionToHandler.get(session);
        if (upgradeHandler != null) {
            sessionToHandler.remove(session);
            upgradeHandler.sessionDestroyed();
        }
        return upgradeHandler;
    }

    private static void appendTraceHeaders(HttpServletResponse httpServletResponse, TyrusUpgradeResponse
            tyrusUpgradeResponse) {
        for (Map.Entry<String, List<String>> entry : tyrusUpgradeResponse.getHeaders().entrySet()) {
            if (entry.getKey().contains(UpgradeResponse.TRACING_HEADER_PREFIX)) {
                httpServletResponse.addHeader(entry.getKey(), Utils.getHeaderFromList(entry.getValue()));
            }
        }
    }

    private static void appendAllHeaders(HttpServletResponse httpServletResponse, TyrusUpgradeResponse
            tyrusUpgradeResponse) {
        for (Map.Entry<String, List<String>> entry : tyrusUpgradeResponse.getHeaders().entrySet()) {
            httpServletResponse.addHeader(entry.getKey(), Utils.getHeaderFromList(entry.getValue()));
        }
    }

    private synchronized JAXBContext getWsadlJaxbContext() throws JAXBException {
        if (wsadlJaxbContext == null) {
            wsadlJaxbContext = JAXBContext.newInstance(Application.class.getPackage().getName());
        }
        return wsadlJaxbContext;
    }

    private Map<String, Object> getInitParams(ServletContext ctx) {
        Map<String, Object> initParams = new HashMap<>();
        Enumeration<String> enumeration = ctx.getInitParameterNames();
        while (enumeration.hasMoreElements()) {
            String initName = enumeration.nextElement();
            initParams.put(initName, ctx.getInitParameter(initName));
        }
        return initParams;
    }

    public void destroy() {
        sessionToHandler.forEach((session, upgradeHandler) -> upgradeHandler.destroy());
        sessionToHandler.clear();
    }

}
