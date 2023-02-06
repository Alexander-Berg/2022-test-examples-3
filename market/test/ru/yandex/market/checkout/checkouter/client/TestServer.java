package ru.yandex.market.checkout.checkouter.client;

import java.net.URI;

import javax.servlet.Servlet;
import javax.servlet.annotation.WebServlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Тестовый сервер, для интеграционного тестирования. Поднимается обычный и https-порт. Навешиваются сервлеты.
 *
 * @author Denis Chernyshov <zoom@yandex-team.ru>
 */
public class TestServer {

    private final Server server;
    private final URI plainUri;
    private final URI securedUri;

    public TestServer(Servlet... servlets) throws Exception {
        server = new Server();

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(getClass().getResource("test.keystore").toExternalForm());
        sslContextFactory.setKeyStorePassword("123456");

        sslContextFactory.setKeyManagerPassword("123456");

        ServerConnector connector = new ServerConnector(server, sslContextFactory);
        connector.setPort(0);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        server.setHandler(context);

        for (Servlet servlet : servlets) {
            WebServlet annotation = servlet.getClass().getAnnotation(WebServlet.class);
            if (annotation != null) {
                if (annotation.urlPatterns() != null && annotation.urlPatterns().length > 0) {
                    String pathPattern = annotation.urlPatterns()[0];
                    addServlet(pathPattern, servlet);
                }
            }
        }

        server.start();

        String host = connector.getHost();
        if (host == null) {
            host = "localhost";
        }

        int port = connector.getLocalPort();
        plainUri = new URI(String.format("http://%s:%d/", host, port));
        securedUri = new URI(String.format("https://%s:%d/", host, connector.getLocalPort()));
    }

    public static void main(String[] args) throws Exception {
        TestServer server = new TestServer();
        System.out.println("SSL Port: " + server.getSecuredPort());
    }

    public void join() throws InterruptedException {
        server.join();
    }

    public URI getPlainUri(String str) {
        return plainUri.resolve(str);
    }

    public URI getSecuredUri(String str) {
        return securedUri.resolve(str);
    }

    public void stop() throws Exception {
        server.stop();
    }

    public int getPlainPort() {
        return plainUri.getPort();
    }

    public int getSecuredPort() {
        return securedUri.getPort();
    }

    public void addServlet(String pathPattern, Servlet servlet) {
        ((ServletContextHandler) server.getHandler()).addServlet(new ServletHolder(servlet), pathPattern);
    }
}
