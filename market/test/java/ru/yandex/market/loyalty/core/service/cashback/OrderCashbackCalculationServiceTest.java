package ru.yandex.market.loyalty.core.service.cashback;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.cashback.BillingSchema;
import ru.yandex.market.loyalty.core.model.multistage.ResolvingState;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.wallet.OrderCashbackCalculation;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.DEFAULT_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.Cashback.defaultFixed;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class OrderCashbackCalculationServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private OrderCashbackCalculationService calculationService;
    @Autowired
    private PromoManager promoManager;

    @Test
    public void shouldSaveCalculationIdempotently() {
        Promo promo = promoManager.createCashbackPromo(defaultFixed(BigDecimal.TEN)
                .setBillingSchema(BillingSchema.SOLID));
        OrderCashbackCalculation expected = OrderCashbackCalculation.builder()
                .setOrderId(DEFAULT_ORDER_ID)
                .setPromoId(promo.getPromoId().getId())
                .setUid(DEFAULT_UID)
                .setCashbackPropsId(promo.getCashbackPropsId())
                .setResult(ResolvingState.INTERMEDIATE)
                .setInitialResult(ResolvingState.INTERMEDIATE)
                .setOrderPaidResult(ResolvingState.FINAL)
                .setOrderTerminationResult(ResolvingState.FINAL)
                .setInitialCashbackAmount(BigDecimal.valueOf(100))
                .setFinalCashbackAmount(BigDecimal.valueOf(100))
                .setEmissionTransactionId(1_000L)
                .setEmissionCorrectionTransactionId(1_100L)
                .setYandexWalletTransactionId(1_000_000L)
                .setRuleBeanName(MIN_ORDER_TOTAL_CUTTING_RULE.getBeanName())
                .build();
        calculationService.save(expected);
        List<OrderCashbackCalculation> actual = calculationService.getAllByOrderId(DEFAULT_ORDER_ID);
        checkEquals(expected, actual.get(0));

        // Idempotent save check

        calculationService.save(expected);
        expected = actual.get(0); // set last response as expected
        actual = calculationService.getAllByOrderId(DEFAULT_ORDER_ID);
        assertThat(actual, hasSize(1));
        assertThat(actual.get(0), hasProperty("id", equalTo(expected.getId())));
        checkEquals(expected, actual.get(0));
    }

    // private

    /**
     * Check for equals of all properties except <i>id</i>
     */
    private void checkEquals(OrderCashbackCalculation expected, OrderCashbackCalculation actual) {
        assertThat(actual, allOf(
                hasProperty("orderId", equalTo(expected.getOrderId())),
                hasProperty("multiOrderId", equalTo(expected.getMultiOrderId())),
                hasProperty("promoId", equalTo(expected.getPromoId())),
                hasProperty("uid", equalTo(expected.getUid())),
                hasProperty("cashbackPropsId", equalTo(expected.getCashbackPropsId())),
                hasProperty("result", equalTo(expected.getResult())),
                hasProperty("initialResult", equalTo(expected.getInitialResult())),
                hasProperty("orderPaidResult", equalTo(expected.getOrderPaidResult())),
                hasProperty("orderTerminationResult", equalTo(expected.getOrderTerminationResult())),
                hasProperty("initialCashbackAmount", equalTo(expected.getInitialCashbackAmount())),
                hasProperty("emissionTransactionId", equalTo(expected.getEmissionTransactionId())),
                hasProperty("emissionCorrectionTransactionId", equalTo(expected.getEmissionCorrectionTransactionId())),
                hasProperty("yandexWalletTransactionId", equalTo(expected.getYandexWalletTransactionId())),
                hasProperty("ruleBeanName", equalTo(expected.getRuleBeanName()))
        ));
    }
}
