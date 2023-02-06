package ru.yandex.market.pers.shopinfo.test.context;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.common.test.junit.JupiterDbUnitTest;


/**
 * Функциональный контекст для тестов модуля shopinfo.
 */
@SpringJUnitConfig(locations = "classpath:/ru/yandex/market/pers/shopinfo/test/context/functional-test-config.xml")
@PreserveDictionariesDbUnitDataSet
public abstract class FunctionalTest extends JupiterDbUnitTest {
    protected String urlBasePrefix;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    public void init() {
        Server jettyServer = (Server) applicationContext.getBean("jettyServer");
        int port = ((ServerConnector) jettyServer.getConnectors()[0]).getLocalPort();
        String host = ((ServerConnector) jettyServer.getConnectors()[0]).getHost();
        urlBasePrefix = "http://" + host + ":" + port;
    }

}
