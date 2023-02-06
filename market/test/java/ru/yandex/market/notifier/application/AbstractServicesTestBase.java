package ru.yandex.market.notifier.application;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.notifier.configuration.TestNotifierConfig;

/**
 * @author kukabara
 */
@ContextConfiguration(classes = TestNotifierConfig.class)
public abstract class AbstractServicesTestBase extends AbstractTestBase {

    @BeforeEach
    public void reset() {
        eventTestUtils.clean();
        notifierPropertiesHolder.resetToDefaults();

        Mockito.reset(shopMetaService, checkouterClient, persNotifyClient, pushClient);
        mockFactory.initShopMetaService(shopMetaService);

        testableClock.clearFixed();
    }
}
