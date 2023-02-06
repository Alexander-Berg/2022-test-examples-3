package ru.yandex.market.loyalty.admin.tms;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.exception.MarketLoyaltyAdminException;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.threshold.UserBlockPromoService;
import ru.yandex.market.loyalty.test.TestFor;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

@TestFor(UserBlockPromoEmptinessProcessor.class)
public class UserBlockPromoEmptinessProcessorTest {
    @Autowired
    private UserBlockPromoEmptinessProcessor userBlockPromoEmptinessProcessor;

    private UserBlockPromoService userBlockPromoService;
    private ConfigurationService configurationService;

    @Before
    public void init() {
        userBlockPromoService = Mockito.mock(UserBlockPromoService.class);
        configurationService = Mockito.mock(ConfigurationService.class);
    }

    @Test
    public void testShouldThrowException() {
        Mockito.when(configurationService.isBlockPromoEnabled()).thenReturn(true);
        Mockito.when(userBlockPromoService.isUserBlockPromoTableEmpty()).thenReturn(true);
        assertThrows(MarketLoyaltyAdminException.class,
                () -> userBlockPromoEmptinessProcessor.checkUserBlockPromoEmpty());
    }
}
