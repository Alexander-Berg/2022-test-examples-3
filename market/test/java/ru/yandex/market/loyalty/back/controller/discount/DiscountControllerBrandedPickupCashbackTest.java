package ru.yandex.market.loyalty.back.controller.discount;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.CashbackPermision;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.model.multistage.ResolvingState;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.wallet.OrderCashbackCalculation;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.cashback.OrderCashbackCalculationService;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MARKET_BRANDED_PICKUP;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MAX_CASHBACK;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.rule.RuleType.MARKET_BRANDED_PICKUP_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MAX_CASHBACK_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.utils.EventFactory.DEFAULT_MULTI_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.SUPPLIER_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.loyaltyProgramPartner;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.OrderRequestWithBundlesBuilder.DEFAULT_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class DiscountControllerBrandedPickupCashbackTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private YandexWalletTransactionDao yandexWalletTransactionDao;
    @Autowired
    private OrderCashbackCalculationService orderCashbackCalculationService;

    @Test
    public void shouldReturnProfileForMarketBrandedPickup() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN, CashbackLevelType.ORDER)
                        .addCashbackRule(MARKET_BRANDED_PICKUP_FILTER_RULE, MARKET_BRANDED_PICKUP, true)
                        .setUiPromoFlags(List.of("special-pickup-promo"))

        );
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ONE, CashbackLevelType.ITEM));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                        orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        warehouse(SUPPLIER_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(5800),
                                        loyaltyProgramPartner(false)
                                )
                                .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                hasProperty(
                                                        "cashback",
                                                        allOf(
                                                                hasProperty(
                                                                        "emit",
                                                                        allOf(
                                                                                hasProperty(
                                                                                        "amount",
                                                                                        equalTo(BigDecimal.valueOf(638))
                                                                                ),
                                                                                hasProperty(
                                                                                        "promos",
                                                                                        contains(
                                                                                                allOf(
                                                                                                        hasProperty(
                                                                                                                "amount",
                                                                                                                equalTo(BigDecimal.valueOf(580))
                                                                                                        ),
                                                                                                        hasProperty(
                                                                                                                "uiPromoFlags",
                                                                                                                containsInAnyOrder("special-pickup-promo")
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                                ),
                                                                                hasProperty(
                                                                                        "type",
                                                                                        equalTo(CashbackPermision.ALLOWED)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

    }

    @Test
    public void shouldCalculateCashbackForMarketBrandedPickupPromo() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN, CashbackLevelType.ORDER)
                        .addCashbackRule(MARKET_BRANDED_PICKUP_FILTER_RULE, MARKET_BRANDED_PICKUP, true)
                        .setUiPromoFlags(List.of("special-pickup-promo"))

        );
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ONE, CashbackLevelType.ITEM));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                        orderRequestWithBundlesBuilder()
                                .withOrderId(DEFAULT_ORDER_ID)
                                .withOrderItem(
                                        warehouse(SUPPLIER_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(2800),
                                        loyaltyProgramPartner(false)
                                )
                                .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                                .build(),
                        orderRequestWithBundlesBuilder()
                                .withOrderId(ANOTHER_ORDER_ID)
                                .withOrderItem(
                                        warehouse(SUPPLIER_WAREHOUSE_ID),
                                        itemKey(ANOTHER_ITEM_KEY),
                                        price(3000),
                                        loyaltyProgramPartner(false)
                                )
                                .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                                .build())
                        .build()
        );

        assertThat(discountResponse,
                allOf(
                        hasProperty("cashback", hasProperty("emit",
                                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(638)))
                        )),
                        hasProperty("orders", containsInAnyOrder(
                                hasProperty("cashback", hasProperty("emit",
                                        allOf(
                                                hasProperty("amount",
                                                        equalTo(BigDecimal.valueOf(330))
                                                ),
                                                hasProperty("promos", contains(allOf(
                                                        hasProperty("amount",
                                                                equalTo(BigDecimal.valueOf(300))
                                                        ),
                                                        hasProperty("uiPromoFlags",
                                                                containsInAnyOrder("special-pickup-promo")
                                                        )
                                                ))),
                                                hasProperty("type",
                                                        equalTo(CashbackPermision.ALLOWED)
                                                )
                                        )
                                )),
                                hasProperty("cashback", hasProperty("emit",
                                        allOf(
                                                hasProperty("amount",
                                                        equalTo(BigDecimal.valueOf(308))
                                                ),
                                                hasProperty("promos",
                                                        contains(allOf(
                                                                hasProperty("amount",
                                                                        equalTo(BigDecimal.valueOf(280))
                                                                ),
                                                                hasProperty("uiPromoFlags",
                                                                        containsInAnyOrder("special-pickup-promo")
                                                                )
                                                        ))
                                                ),
                                                hasProperty("type",
                                                        equalTo(CashbackPermision.ALLOWED)
                                                )
                                        )
                                ))
                        ))
                )
        );

    }

    @Test
    public void shouldRestrictMaxCashbackForOrderCashbackPromos() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN, CashbackLevelType.ORDER)
                        .addCashbackRule(MAX_CASHBACK_FILTER_RULE, MAX_CASHBACK, BigDecimal.valueOf(100))
                        .setUiPromoFlags(List.of("special-pickup-promo"))

        );

        cashbackCacheService.reloadCashbackPromos();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                        orderRequestWithBundlesBuilder()
                                .withOrderId(DEFAULT_ORDER_ID)
                                .withOrderItem(
                                        warehouse(SUPPLIER_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(2800),
                                        loyaltyProgramPartner(false)
                                )
                                .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                                .build())
                        .build()
        );

        assertThat(discountResponse,
                allOf(
                        hasProperty("cashback", hasProperty("emit",
                                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(100)))
                        )),
                        hasProperty("orders", containsInAnyOrder(
                                hasProperty("cashback", hasProperty("emit",
                                        allOf(
                                                hasProperty("amount",
                                                        equalTo(BigDecimal.valueOf(100))
                                                ),
                                                hasProperty("promos", contains(allOf(
                                                        hasProperty("amount",
                                                                equalTo(BigDecimal.valueOf(100))
                                                        ),
                                                        hasProperty("uiPromoFlags",
                                                                containsInAnyOrder("special-pickup-promo")
                                                        )
                                                ))),
                                                hasProperty("type",
                                                        equalTo(CashbackPermision.ALLOWED)
                                                )
                                        )
                                ))
                        ))
                )
        );

    }

    @Test
    public void shouldFixMarketdiscount7491() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN, CashbackLevelType.ORDER)
                        .addCashbackRule(MARKET_BRANDED_PICKUP_FILTER_RULE, MARKET_BRANDED_PICKUP, true)
                        .setUiPromoFlags(List.of("special-pickup-promo"))

        );
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ONE, CashbackLevelType.ITEM));
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN, CashbackLevelType.ITEM).setPromoBucketName("employee"));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                        orderRequestWithBundlesBuilder()
                                .withOrderId(DEFAULT_ORDER_ID)
                                .withOrderItem(
                                        warehouse(SUPPLIER_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(2800),
                                        loyaltyProgramPartner(false)
                                )
                                .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                                .build(),
                        orderRequestWithBundlesBuilder()
                                .withOrderId(ANOTHER_ORDER_ID)
                                .withOrderItem(
                                        warehouse(SUPPLIER_WAREHOUSE_ID),
                                        itemKey(ANOTHER_ITEM_KEY),
                                        price(3000),
                                        loyaltyProgramPartner(false)
                                )
                                .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                                .build())
                        .build()
        );

        assertThat(
                discountResponse,
                allOf(
                        hasProperty("cashback", allOf(
                                hasProperty("emit", allOf(
                                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(1160)))
                                ))
                        )),
                        hasProperty(
                                "orders",
                                containsInAnyOrder(
                                        allOf(
                                                hasProperty(
                                                        "cashback",
                                                        allOf(
                                                                hasProperty(
                                                                        "emit",
                                                                        allOf(
                                                                                hasProperty(
                                                                                        "amount",
                                                                                        equalTo(BigDecimal.valueOf(600))
                                                                                ),
                                                                                hasProperty(
                                                                                        "promos",
                                                                                        contains(
                                                                                                allOf(
                                                                                                        hasProperty(
                                                                                                                "amount",
                                                                                                                equalTo(BigDecimal.valueOf(300))
                                                                                                        ),
                                                                                                        hasProperty(
                                                                                                                "uiPromoFlags",
                                                                                                                containsInAnyOrder("special-pickup-promo")
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                                ),
                                                                                hasProperty(
                                                                                        "type",
                                                                                        equalTo(CashbackPermision.ALLOWED)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        ),
                                        allOf(
                                                hasProperty(
                                                        "cashback",
                                                        allOf(
                                                                hasProperty(
                                                                        "emit",
                                                                        allOf(
                                                                                hasProperty(
                                                                                        "amount",
                                                                                        equalTo(BigDecimal.valueOf(560))
                                                                                ),
                                                                                hasProperty(
                                                                                        "promos",
                                                                                        contains(
                                                                                                allOf(
                                                                                                        hasProperty(
                                                                                                                "amount",
                                                                                                                equalTo(BigDecimal.valueOf(280))
                                                                                                        ),
                                                                                                        hasProperty(
                                                                                                                "uiPromoFlags",
                                                                                                                containsInAnyOrder("special-pickup-promo")
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                                ),
                                                                                hasProperty(
                                                                                        "type",
                                                                                        equalTo(CashbackPermision.ALLOWED)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

    }

    @Test
    public void shouldAllowCashbackOnSpendForMultiOrder() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN, CashbackLevelType.ORDER)
                        .addCashbackRule(MARKET_BRANDED_PICKUP_FILTER_RULE, MARKET_BRANDED_PICKUP, true)
                        .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(1))
                        .setUiPromoFlags(List.of("special-pickup-promo"))

        );
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.ONE, CashbackLevelType.ITEM)
        );

        cashbackCacheService.reloadCashbackPromos();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                        orderRequestWithBundlesBuilder()
                                .withOrderId(DEFAULT_ORDER_ID)
                                .withOrderItem(
                                        warehouse(SUPPLIER_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(2800),
                                        loyaltyProgramPartner(false)
                                )
                                .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                                .build(),
                        orderRequestWithBundlesBuilder()
                                .withOrderId(ANOTHER_ORDER_ID)
                                .withOrderItem(
                                        warehouse(SUPPLIER_WAREHOUSE_ID),
                                        itemKey(ANOTHER_ITEM_KEY),
                                        price(3000),
                                        loyaltyProgramPartner(false)
                                )
                                .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                                .build())
                        .withMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .build()
        );

        assertThat(yandexWalletTransactionDao.findAllByMultiOrderId(DEFAULT_MULTI_ORDER_ID), empty());
        assertThat(yandexWalletTransactionDao.findAllByOrderId(Long.parseLong(DEFAULT_ORDER_ID)), empty());
        assertThat(yandexWalletTransactionDao.findAllByOrderId(Long.parseLong(ANOTHER_ORDER_ID)), empty());

        List<OrderCashbackCalculation> calculationsAfterTerminate =
                orderCashbackCalculationService.getAllByMultiOrderId(DEFAULT_MULTI_ORDER_ID);

        assertThat(calculationsAfterTerminate, everyItem(allOf(
                hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                hasProperty("orderPaidResult", is(nullValue())),
                hasProperty("orderTerminationResult", is(nullValue())),
                hasProperty("initialCashbackAmount", comparesEqualTo(BigDecimal.valueOf(580))),
                hasProperty("finalCashbackAmount", is(nullValue()))
        )));
    }

    @Test
    public void shouldAllowCashbackOnSpendForSingleOrder() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN, CashbackLevelType.ORDER)
                        .addCashbackRule(MARKET_BRANDED_PICKUP_FILTER_RULE, MARKET_BRANDED_PICKUP, true)
                        .addCashbackRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(1))
                        .setUiPromoFlags(List.of("special-pickup-promo"))

        );
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ONE, CashbackLevelType.ITEM));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        var request = DiscountRequestWithBundlesBuilder.builder(
                orderRequestWithBundlesBuilder()
                        .withOrderItem(
                                warehouse(SUPPLIER_WAREHOUSE_ID),
                                itemKey(DEFAULT_ITEM_KEY),
                                price(5800),
                                loyaltyProgramPartner(false)
                        )
                        .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                        .build())
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(request);

        assertThat(
                discountResponse,
                allOf(
                        hasProperty(
                                "orders",
                                contains(
                                        allOf(
                                                hasProperty(
                                                        "cashback",
                                                        allOf(
                                                                hasProperty(
                                                                        "emit",
                                                                        allOf(
                                                                                hasProperty(
                                                                                        "amount",
                                                                                        equalTo(BigDecimal.valueOf(638))
                                                                                ),
                                                                                hasProperty(
                                                                                        "promos",
                                                                                        contains(
                                                                                                allOf(
                                                                                                        hasProperty(
                                                                                                                "amount",
                                                                                                                equalTo(BigDecimal.valueOf(580))
                                                                                                        ),
                                                                                                        hasProperty(
                                                                                                                "uiPromoFlags",
                                                                                                                containsInAnyOrder("special-pickup-promo")
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                                ),
                                                                                hasProperty(
                                                                                        "type",
                                                                                        equalTo(CashbackPermision.ALLOWED)
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
        assertThat(yandexWalletTransactionDao.findAllByMultiOrderId(DEFAULT_MULTI_ORDER_ID), empty());
        assertThat(yandexWalletTransactionDao.findAllByOrderId(Long.parseLong(DEFAULT_ORDER_ID)), empty());
        assertThat(yandexWalletTransactionDao.findAllByOrderId(Long.parseLong(ANOTHER_ORDER_ID)), empty());

        List<OrderCashbackCalculation> calculationsAfterTerminate =
                orderCashbackCalculationService.getAllByOrderId(Long.parseLong(DEFAULT_ORDER_ID));

        assertThat(calculationsAfterTerminate, everyItem(allOf(
                hasProperty("initialResult", equalTo(ResolvingState.INTERMEDIATE)),
                hasProperty("orderPaidResult", is(nullValue())),
                hasProperty("orderTerminationResult", is(nullValue())),
                hasProperty("initialCashbackAmount", comparesEqualTo(BigDecimal.valueOf(580))),
                hasProperty("finalCashbackAmount", is(nullValue()))
        )));
    }

}
