package ru.yandex.market.security.config;


import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import ru.yandex.market.security.CheckStaticAuthorityServantlet;
import ru.yandex.market.security.util.http.LoggingHandlerWrapper;

@Configuration
public class FunctionalTestConfig {

    @Autowired
    private LoggingHandlerWrapper loggingHandlerWrapper;

    @Bean(name = "baseUrl")
    @DependsOn("httpServerInitializer")
    public String baseUrl() {
        final Server jettyServer = loggingHandlerWrapper.getServer();
        final Connector[] connectors = jettyServer.getConnectors();
        final Connector firstConnector = connectors[0];

        final NetworkConnector networkConnector = (NetworkConnector) firstConnector;
        final int actualPort = networkConnector.getLocalPort();

        return "http://localhost:" + actualPort;
    }

    @Bean
    public CheckStaticAuthorityServantlet checkStaticAuthoritySpyServantlet(
            CheckStaticAuthorityServantlet checkStaticAuthorityServantlet
    ) {
        return Mockito.spy(checkStaticAuthorityServantlet);
    }
}
