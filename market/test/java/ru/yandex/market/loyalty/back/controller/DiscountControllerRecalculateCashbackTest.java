package ru.yandex.market.loyalty.back.controller;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.OperationContextDto;
import ru.yandex.market.loyalty.api.model.PaymentSystem;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.discount.RecalculateCashbackItemRequest;
import ru.yandex.market.loyalty.api.model.discount.RecalculateCashbackOrderRequest;
import ru.yandex.market.loyalty.api.model.discount.RecalculateCashbackRequest;
import ru.yandex.market.loyalty.api.model.discount.RecalculateCashbackResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.OrderCashbackCalculationDao;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.wallet.OrderCashbackCalculation;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.YandexWalletTransactionService;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.DEFAULT_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.courierDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withPrice;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.atSupplierWarehouse;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class DiscountControllerRecalculateCashbackTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private YandexWalletTransactionService yandexWalletTransactionService;
    @Autowired
    private OrderCashbackCalculationDao orderCashbackCalculationDao;

    @Test
    public void shouldRecalcCashback() {
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        RecalculateCashbackResponse response = marketLoyaltyClient.recalcCashback(RecalculateCashbackRequest.builder()
                .setPlatform(MarketPlatform.BLUE)
                .setOperationContext(OperationContextDto.builderDto()
                        .setUid(DEFAULT_UID)
                        .build())
                .addOrder(RecalculateCashbackOrderRequest.builder()
                        .setCartId("cartId")
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setPlatform(MarketPlatform.BLUE)
                        .addItem(RecalculateCashbackItemRequest.builder()
                                .withFeedId(1L)
                                .withOfferId("1")
                                .withAtSupplierWarehouse(true)
                                .withQuantity(BigDecimal.ONE)
                                .withPrice(BigDecimal.valueOf(100))
                                .withHyperCategoryId(DEFAULT_CATEGORY_ID)
                                .build())
                        .build())
                .build(), null);

        assertThat(response.getCashback().getEmit().getAmount(), comparesEqualTo(BigDecimal.valueOf(10)));
        assertThat(response.getOrders().get(0).getOrderId(), equalTo(DEFAULT_ORDER_ID));
    }

    @Test
    public void shouldRespendCashback() {
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                .setEmissionBudget(BigDecimal.valueOf(1000))
                .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.valueOf(1)));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        spendDiscounts();

        respendDiscounts(2, CashbackType.EMIT);

        List<YandexWalletTransaction> pendingTransactions =
                yandexWalletTransactionService.findByOrderId(Long.parseLong(DEFAULT_ORDER_ID),
                        YandexWalletTransactionStatus.FAKE_PENDING);

        List<YandexWalletTransaction> deletedTransactions =
                yandexWalletTransactionService.findAllDeleted();

        assertThat(pendingTransactions, hasSize(1));
        assertThat(deletedTransactions, hasSize(2));

        List<OrderCashbackCalculation> pendingCalculations =
                orderCashbackCalculationDao.findAllByOrderId(Long.parseLong(DEFAULT_ORDER_ID));
        List<OrderCashbackCalculation> deletedCalculations =
                orderCashbackCalculationDao.findAllDeleted();

        assertThat(pendingCalculations, hasSize(1));
        assertThat(deletedCalculations, hasSize(2));
    }

    @Test
    public void shouldEraseCashback() {
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                .setEmissionBudget(BigDecimal.valueOf(1000))
                .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE, RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.valueOf(1)));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        spendDiscounts();

        respendDiscounts(1, CashbackType.SPEND);

        List<YandexWalletTransaction> pendingTransactions =
                yandexWalletTransactionService.findByOrderId(Long.parseLong(DEFAULT_ORDER_ID),
                        YandexWalletTransactionStatus.FAKE_PENDING);

        List<YandexWalletTransaction> deletedTransactions =
                yandexWalletTransactionService.findAllDeleted();

        assertThat(pendingTransactions, hasSize(0));
        assertThat(deletedTransactions, hasSize(1));

        List<OrderCashbackCalculation> pendingCalculations =
                orderCashbackCalculationDao.findAllByOrderId(Long.parseLong(DEFAULT_ORDER_ID));
        List<OrderCashbackCalculation> deletedCalculations =
                orderCashbackCalculationDao.findAllDeleted();

        assertThat(pendingCalculations, hasSize(0));
        assertThat(deletedCalculations, hasSize(1));
    }

    private void respendDiscounts(int repeat, CashbackType cashbackOptionType) {
        for (int i = 0; i < repeat; i++) {
            RecalculateCashbackResponse respendResponse = marketLoyaltyClient.respendCashback(
                    RecalculateCashbackRequest.builder()
                            .setPlatform(MarketPlatform.BLUE)
                            .setCashbackOptionType(cashbackOptionType)
                            .setOperationContext(OperationContextDto.builderDto()
                                    .setUid(DEFAULT_UID)
                                    .build())
                            .addOrder(RecalculateCashbackOrderRequest.builder()
                                    .setOrderId(DEFAULT_ORDER_ID)
                                    .setCartId("cartId")
                                    .setPlatform(MarketPlatform.BLUE)
                                    .addItem(RecalculateCashbackItemRequest.builder()
                                            .withFeedId(1L)
                                            .withOfferId("1")
                                            .withAtSupplierWarehouse(true)
                                            .withQuantity(BigDecimal.ONE)
                                            .withPrice(BigDecimal.valueOf(100))
                                            .withHyperCategoryId(DEFAULT_CATEGORY_ID)
                                            .build())
                                    .build())
                            .build(),
                    null
            );

            assertThat(respendResponse.getCashback().getEmit().getAmount(), comparesEqualTo(BigDecimal.valueOf(10)));
        }
    }

    private void spendDiscounts() {
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        itemKey(1, "1"),
                                        atSupplierWarehouse(true),
                                        quantity(1),
                                        price(100)
                                )
                                .withPaymentType(PaymentType.BANK_CARD)
                                .withPaymentSystem(PaymentSystem.MASTERCARD)
                                .withDeliveries(courierDelivery(
                                        withPrice(BigDecimal.valueOf(350)),
                                        builder -> builder.setSelected(true)
                                ))
                                .withOrderId(DEFAULT_ORDER_ID)
                                .build())
                        .withOperationContext(OperationContextDto.builderDto()
                                .setUid(DEFAULT_UID)
                                .build())
                        .withPlatform(MarketPlatform.BLUE)
                        .withCashbackOptionType(CashbackType.EMIT)
                        .build(),
                null
        );
        assertThat(discountResponse.getCashback().getEmit().getAmount(), comparesEqualTo(BigDecimal.valueOf(10)));
    }
}


