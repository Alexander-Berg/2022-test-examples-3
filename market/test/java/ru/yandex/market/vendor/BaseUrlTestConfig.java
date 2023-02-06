package ru.yandex.market.vendor;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BaseUrlTestConfig {

    private final Server jettyServer;

    public BaseUrlTestConfig(Server jettyServer) {
        this.jettyServer = jettyServer;
    }

    @Bean
    public String baseUrl() {
        int port = ((ServerConnector) jettyServer.getConnectors()[0]).getLocalPort();
        return "http://localhost:" + port;
    }

}
