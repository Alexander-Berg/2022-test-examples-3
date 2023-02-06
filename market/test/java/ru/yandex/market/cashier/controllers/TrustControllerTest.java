package ru.yandex.market.cashier.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.cashier.AbstractApplicationTest;
import ru.yandex.market.cashier.mocks.trust.TrustMockConfigurer;
import ru.yandex.market.cashier.trust.TrustBasketId;
import ru.yandex.market.cashier.trust.api.TrustBasketResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.cashier.mocks.trust.checkers.CheckBasketParams.buildPostAuth;
import static ru.yandex.market.cashier.mocks.trust.checkers.TrustCallsChecker.BLUE_PAY_TOKEN_ID;
import static ru.yandex.market.cashier.mocks.trust.checkers.TrustCallsChecker.checkCheckBasketCall;

public class TrustControllerTest extends AbstractApplicationTest {

    @Autowired
    private TrustController trustController;

    @Test
    public void testGetBasket() {
        String purchaseToken = TrustMockConfigurer.generateRandomTrustId();
        trustMockConfigurer.mockCheckBasket(buildPostAuth().withPurchaseToken(purchaseToken));



        TrustBasketResponse basket = trustController.getBasket(purchaseToken, BLUE_PAY_TOKEN_ID);
        assertNotNull(basket);
        assertEquals(purchaseToken, basket.getPurchaseToken());

        checkCheckBasketCall(trustMockConfigurer.eventsIterator(),new TrustBasketId(purchaseToken,BLUE_PAY_TOKEN_ID));
    }
}
