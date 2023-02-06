package ru.yandex.market.loyalty.back.controller;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.welcome.DeliveryWelcomeCoinCheckRequest;
import ru.yandex.market.loyalty.api.model.welcome.DeliveryWelcomeCoinRegistrationRequest;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.loyalty.test.TestFor;

import java.util.Collections;
import java.util.Map;

import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(DeliveryWelcomeCoinController.class)
public class DeliveryWelcomeCoinControllerTest extends MarketLoyaltyBackMockedDbTestBase {
    private static final int DELIVERY_WELCOME_BONUS_TEST_REGION = 213;
    private static final Map<String, String> SOME_DEVICE = Collections.singletonMap("someDevice", "1");
    private static final DeliveryWelcomeCoinCheckRequest TEST_CHECK_REQUEST = new DeliveryWelcomeCoinCheckRequest(
            DELIVERY_WELCOME_BONUS_TEST_REGION,
            DEFAULT_UID,
            SOME_DEVICE
    );

    private static final DeliveryWelcomeCoinRegistrationRequest TEST_REGISTRATION_REQUEST =
            new DeliveryWelcomeCoinRegistrationRequest(
                    DELIVERY_WELCOME_BONUS_TEST_REGION,
                    DEFAULT_UID,
                    SOME_DEVICE
            );

    @Autowired
    private MarketLoyaltyClient marketLoyaltyClient;

    @Test(expected = MarketLoyaltyException.class)
    public void shouldThrowDeprecatedMethodExceptionRegister() {
        marketLoyaltyClient.registerDeliveryWelcomeCoinUser(TEST_REGISTRATION_REQUEST);
    }

    @Test(expected = MarketLoyaltyException.class)
    public void shouldThrowDeprecatedMethodExceptionCheck() {
        marketLoyaltyClient.checkDeliveryWelcomeCoinUser(TEST_CHECK_REQUEST);
    }
}
