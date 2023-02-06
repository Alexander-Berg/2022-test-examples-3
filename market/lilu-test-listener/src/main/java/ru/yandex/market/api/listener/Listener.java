package ru.yandex.market.api.listener;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import ru.yandex.market.api.listener.expectations.HttpExpectations;
import ru.yandex.market.api.listener.expectations.HttpTestClientConfiguration;
import ru.yandex.market.api.listener.match.MatchServlet;

/**
 * @author dimkarp93
 */
public class Listener {
    private final Server server;

    private Listener(Server server) {
        this.server = server;
    }

    public void start() {
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Listener listener(HttpExpectations httpExpectations, int port) {
        MatchServlet servlet = new MatchServlet(httpExpectations);

        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(
            new ServletHolder(servlet), "/"
        );

        Server server = new Server(port);
        server.setHandler(handler);

        return new Listener(server);
    }

}
