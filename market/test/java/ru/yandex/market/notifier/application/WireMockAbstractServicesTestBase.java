package ru.yandex.market.notifier.application;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.notifier.configuration.NotifierWireMockConfiguration;
import ru.yandex.market.notifier.configuration.TestNotifierConfig;

/**
 * @author kukabara
 */
@ContextConfiguration(classes = {NotifierWireMockConfiguration.class, TestNotifierConfig.class})
public abstract class WireMockAbstractServicesTestBase extends AbstractTestBase {

    @BeforeEach
    public void reset() {
        eventTestUtils.clean();
        notifierPropertiesHolder.resetToDefaults();

        Mockito.reset(shopMetaService, checkouterClient, persNotifyClient);
        mockFactory.initShopMetaService(shopMetaService);

        testableClock.clearFixed();
    }
}
