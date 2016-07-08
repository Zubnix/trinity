package org.westmalle.wayland.html5;


import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.westmalle.wayland.core.Platform;

import javax.inject.Inject;
import java.net.URL;
import java.util.Objects;

public class Html5PlatformFactory {

    private final Platform                    platform;
    private final PrivateHtml5PlatformFactory privateHtml5PlatformFactory;

    @Inject
    Html5PlatformFactory(final Platform platform,
                         final PrivateHtml5PlatformFactory privateHtml5PlatformFactory) {
        this.platform = platform;
        this.privateHtml5PlatformFactory = privateHtml5PlatformFactory;
    }

    public Html5Platform create() {
        //TODO setup factory that properly inits an embedded jetty server with a websocket and pass it in the constructor

        final Server server = new Server(8080);

        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Add websocket servlet
        final ServletHolder wsHolder = new ServletHolder("wayland", new Html5SocketServlet());
        context.addServlet(wsHolder,"/wayland");

        // Add default servlet (to serve the html/css/js)
        // Figure out where the static files are stored.
        //TODO write index.html & js
        final URL urlStatics = Thread.currentThread().getContextClassLoader().getResource("index.html");
        Objects.requireNonNull(urlStatics, "Unable to find index.html in classpath");
        final String        urlBase   = urlStatics.toExternalForm().replaceFirst("/[^/]*$", "/");
        final ServletHolder defHolder = new ServletHolder("default", new DefaultServlet());
        defHolder.setInitParameter("resourceBase",urlBase);
        defHolder.setInitParameter("dirAllowed","true");
        context.addServlet(defHolder,"/");

        try
        {
            server.start();
            server.join();
        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }

        return this.privateHtml5PlatformFactory.create(null);
    }
}