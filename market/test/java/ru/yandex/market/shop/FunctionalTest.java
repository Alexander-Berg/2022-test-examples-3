package ru.yandex.market.shop;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.market.common.test.guava.ForgetfulSuppliersInitializer;
import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.core.database.PreserveDictionariesDbUnitDataSet;
import ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest;
import ru.yandex.market.core.test.context.TestPropertiesInitializer;

@SpringJUnitConfig(
        locations = "classpath:/ru/yandex/market/shop/functional-test-config.xml",
        initializers = {
                TestPropertiesInitializer.class,
                ForgetfulSuppliersInitializer.class
        }
)
@ActiveProfiles(profiles = {
        "functionalTest",
        "development"
})
@PreserveDictionariesDbUnitDataSet
public abstract class FunctionalTest extends JupiterDbUnitTest {
    @Autowired
    Server jettyServer;

    private String baseUrl;

    protected String getBaseUrl() {
        if (baseUrl == null) {
            Connector[] connectors = jettyServer.getConnectors();
            Connector firstConnector = connectors[0];
            NetworkConnector networkConnector = (NetworkConnector) firstConnector;
            int actualPort = networkConnector.getLocalPort();
            baseUrl = "http://localhost:" + actualPort;
        }
        return baseUrl;
    }

    @Autowired
    protected PartnerNotificationClient partnerNotificationClient;

    @BeforeEach
    void commonSetUp() {
        PartnerNotificationApiServiceTest.setUpClient(partnerNotificationClient);
    }
}
