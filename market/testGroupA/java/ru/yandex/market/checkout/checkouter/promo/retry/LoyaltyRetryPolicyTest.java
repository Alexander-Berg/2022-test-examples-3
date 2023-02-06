package ru.yandex.market.checkout.checkouter.promo.retry;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by aproskriakov on 11/17/21
 */
public class LoyaltyRetryPolicyTest extends AbstractWebTestBase {

    @Autowired
    private RetryTemplate loyaltyRetryTemplate;

    @Test
    public void testRetryOnPumpkinResponse() {
        testRetry(MarketLoyaltyErrorCode.RESPONSE_FROM_PUMPKIN, 1);
    }

    @Test
    public void testRetryOnOtherError() {
        testRetry(MarketLoyaltyErrorCode.OTHER_ERROR, 3);
    }

    private void testRetry(MarketLoyaltyErrorCode mec, int exectedRetries) {
        AtomicInteger retryCount = new AtomicInteger();
        MarketLoyaltyException mle = new MarketLoyaltyException(mec);
        MarketLoyaltyClient loyaltyClient = Mockito.mock(MarketLoyaltyClient.class);
        when(loyaltyClient.spendDiscount(any(MultiCartWithBundlesDiscountRequest.class), any(HttpHeaders.class)))
                .thenThrow(mle);
        try {
            loyaltyRetryTemplate.execute(ctx -> {
                retryCount.getAndIncrement();
                MultiCartWithBundlesDiscountRequest mockRequest = MultiCartWithBundlesDiscountRequest.builder()
                        .setUseHeldCoins(false)
                        .setBnplSelected(false)
                        .setCalculateOrdersSeparately(false)
                        .setIsOptionalRulesEnabled(false)
                        .build();
                HttpHeaders httpHeaders = new HttpHeaders();
                loyaltyClient.spendDiscount(mockRequest, httpHeaders);
                return null;
            });
        } catch (Exception e) {
            log.info("Catch exception: ", e);
        }

        assertEquals(exectedRetries, retryCount.get());
    }
}
