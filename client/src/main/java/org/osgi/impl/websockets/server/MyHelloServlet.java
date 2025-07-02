package org.osgi.impl.websockets.server;

import java.io.IOException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.servlet.whiteboard.propertytypes.HttpWhiteboardServletName;
import org.osgi.service.servlet.whiteboard.propertytypes.HttpWhiteboardServletPattern;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component(service = Servlet.class)
@HttpWhiteboardServletPattern("/hello")
@HttpWhiteboardServletName("Hello Servlet")
public class MyHelloServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// FIXME without a servlet filters do not work!
		System.out.println("MyServlet.doGet()");
		resp.getWriter().write("This is a WS Endpoint, please use HTTP Upgrade to communicate with the Websockets!");
	}

}
