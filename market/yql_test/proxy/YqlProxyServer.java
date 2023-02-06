package ru.yandex.market.yql_test.proxy;

import java.util.concurrent.ThreadLocalRandom;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YqlProxyServer {

    // Порты <= 1025 требуют рутовых прав для бинда
    private static final int MIN_ALLOWED_PORT_NUMBER = 1025;
    private static final int MAX_ALLOWED_PORT_NUMBER = 65535;

    private static final Logger log = LoggerFactory.getLogger(YqlProxyServer.class);

    private final String yqlUrl;
    private final int maxRetries;

    private int proxyPort;

    private Server server;
    private YqlCachingServlet cachingServlet;

    public YqlProxyServer(String yqlUrl, int proxyPort, int maxRetries) {
        this.yqlUrl = yqlUrl;
        this.proxyPort = proxyPort;
        this.maxRetries = maxRetries;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setResponseStorage(YqlResponseStorage storage) {
        this.cachingServlet.setResponseStorage(storage);
    }

    public void setRunBeforeSendingRequestToYqlServer(Runnable runBeforeSendingRequestToYqlServer) {
        this.cachingServlet.setRunBeforeSendingRequestToYqlServer(runBeforeSendingRequestToYqlServer);
    }

    public void addListener(YqlCachingServletListener listener) {
        this.cachingServlet.addListener(listener);
    }

    public synchronized void start() throws Exception {
        if (server != null) {
            return;
        }

        if (proxyPort < MIN_ALLOWED_PORT_NUMBER) {
            log.warn("Requested proxy port {} is lower than minimum allowed, resetting to 1025", proxyPort);
            proxyPort = MIN_ALLOWED_PORT_NUMBER;
        }

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                startInternal();
                break;
            } catch (Exception err) {
                stop();
                String message = String.format(
                        "Failed to start YQL proxy server on port %d, attempt #%d: %s",
                        proxyPort,
                        attempt,
                        err.getMessage()
                );

                if (attempt == maxRetries) {
                    throw new IllegalStateException(message, err);
                } else {
                    log.warn(message, err);
                }

                proxyPort += ThreadLocalRandom.current().nextInt(1, 11);
                if (proxyPort > MAX_ALLOWED_PORT_NUMBER) {
                    throw new IllegalArgumentException(String.format("Post %d is outside valid port range", proxyPort));
                }
            }
        }
    }

    private synchronized void startInternal() throws Exception {
        log.info("Starting YQL proxy server localhost:{} -> {}", proxyPort, yqlUrl);
        server = new Server(proxyPort);
        ServerConnector connector = new ServerConnector(server);
        server.addConnector(connector);

        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        cachingServlet = new YqlCachingServlet(yqlUrl);
        ServletHolder holder = new ServletHolder(cachingServlet);
        handler.addServletWithMapping(holder, "/");

        server.start();
    }

    public synchronized void stop() throws Exception {
        if (server != null) {
            if (server.isRunning()) {
                log.info("Stopping YQL proxy server localhost:{} -> {}", proxyPort, yqlUrl);
                server.stop();
            }

            server = null;
        }
    }

    public synchronized boolean isStarted() {
        return server != null;
    }

}
