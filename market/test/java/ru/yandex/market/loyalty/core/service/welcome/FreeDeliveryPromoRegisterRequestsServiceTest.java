package ru.yandex.market.loyalty.core.service.welcome;

import java.util.Objects;
import java.util.Random;
import java.util.stream.IntStream;

import com.vividsolutions.jts.util.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.welcome.DeliveryWelcomeCoinCheckResponse;
import ru.yandex.market.loyalty.api.model.welcome.DeliveryWelcomeCoinPromoStatus;
import ru.yandex.market.loyalty.api.model.welcome.DeliveryWelcomeCoinUserStatus;
import ru.yandex.market.loyalty.api.model.welcome.FreeDeliveryCoinCheckRequest;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.test.TestFor;

import static java.lang.Math.abs;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

@TestFor(FreeDeliveryPromoRegisterRequestsService.class)
public class FreeDeliveryPromoRegisterRequestsServiceTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    private FreeDeliveryPromoRegisterRequestsService freeDeliveryPromoRegisterRequestsService;

    @Autowired
    private ConfigurationService configurationService;

    private static final int MINI_TEST_COUNT = 100;

    private static final Random random = new Random(System.currentTimeMillis());

    private FreeDeliveryCoinCheckRequest testCheckRequest;

    private DeliveryWelcomeCoinCheckResponse response;

    @Test
    public void shouldNotPassCheckIfWelcomeBonusDisabled() {
        configurationService.disable(ConfigurationService.DELIVERY_WELCOME_BONUS_ENABLE);
        IntStream.range(0, MINI_TEST_COUNT).forEach(i -> {
            testCheckRequest = new FreeDeliveryCoinCheckRequest(abs(random.nextInt()), abs(random.nextLong()));
            response = freeDeliveryPromoRegisterRequestsService.processRegistrationCheckRequest(testCheckRequest);
            assertNotNull(response);
            Assert.equals(response.getPromoStatus(), DeliveryWelcomeCoinPromoStatus.PROMO_NOT_ACTIVE);
            Assert.equals(response.getUserStatus(), DeliveryWelcomeCoinUserStatus.UNKNOWN);
        });
    }

    @Test
    public void shouldPassCheckIfWelcomeBonusEnabled() {
        configurationService.enable(ConfigurationService.DELIVERY_WELCOME_BONUS_ENABLE);
        IntStream.range(0, MINI_TEST_COUNT).forEach(i -> {
            testCheckRequest = new FreeDeliveryCoinCheckRequest(abs(random.nextInt()), abs(random.nextLong()));
            response = freeDeliveryPromoRegisterRequestsService.processRegistrationCheckRequest(testCheckRequest);
            assertNotNull(response);
            assertFalse(
                    Objects.equals(response.getPromoStatus(), DeliveryWelcomeCoinPromoStatus.PROMO_NOT_ACTIVE)
                            &&
                            Objects.equals(response.getUserStatus(), DeliveryWelcomeCoinUserStatus.UNKNOWN)
            );
        });
    }
}
