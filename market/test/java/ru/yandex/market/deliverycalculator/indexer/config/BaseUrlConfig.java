package ru.yandex.market.deliverycalculator.indexer.config;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BaseUrlConfig {

    @Autowired
    private Server server;

    @Bean
    public String baseUrl() {
        int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        return "http://localhost:" + port;
    }

}
