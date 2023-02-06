package ru.yandex.market.http.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.IntFunction;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import ru.yandex.market.http.Http;
import ru.yandex.market.http.util.rules.HttpRule;

/**
 * @author dimkarp93
 */
public class TestClientHelper implements AutoCloseable {
    private final IntFunction<HttpConfig> httpConfigGen;
    private final Server server;
    private final int port;
    private final HttpRule httpRule;

    private static final String BASE_URL = "http://localhost:";
    private static final int DEFAULT_PORT = 2000;

    public TestClientHelper(IntFunction<HttpConfig> httpConfigGen, HttpRule httpRule) {
        this(httpConfigGen, httpRule, DEFAULT_PORT);
    }

    public TestClientHelper(IntFunction<HttpConfig> httpConfigGen, HttpRule httpRule, int port) {
        this.httpConfigGen = httpConfigGen;
        this.httpRule = httpRule;
        this.port = port;
        this.server = new Server(port);
    }

    public void start() throws Exception {
        httpRule.add(this);
        MatchServlet servlet = new MatchServlet(httpConfigGen);
        server.setStopTimeout(0L);

        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(
                new ServletHolder(servlet), "/"
        );

        server.setHandler(handler);
        server.start();

    }

    public void stop() throws Exception {
        server.stop();
    }

    public URI getBaseUrl() {
        return getBaseUrl(null);
    }

    public URI getBaseUrl(String path) {
        try {
            return new URI(BASE_URL + port + (null == path ? "" : path));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        stop();
    }
}
