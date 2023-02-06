package ru.yandex.market.loyalty.back.controller;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.PaymentSystem;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.cashback.CashbackAmountRequest;
import ru.yandex.market.loyalty.api.model.cashback.CashbackAmountRequests;
import ru.yandex.market.loyalty.api.model.cashback.CashbackAmountResponses;
import ru.yandex.market.loyalty.api.model.cashback.details.BnplStatus;
import ru.yandex.market.loyalty.api.model.cashback.details.CashbackMergeOption;
import ru.yandex.market.loyalty.api.model.cashback.details.ExternalItemCashback;
import ru.yandex.market.loyalty.api.model.cashback.details.StructuredCashbackPromoAccrualStatus;
import ru.yandex.market.loyalty.api.model.cashback.details.StructuredCashbackRequest;
import ru.yandex.market.loyalty.api.model.cashback.details.StructuredCashbackRequests;
import ru.yandex.market.loyalty.api.model.cashback.details.StructuredCashbackResponse;
import ru.yandex.market.loyalty.api.model.cashback.details.StructuredCashbackResponses;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.MultiorderDao;
import ru.yandex.market.loyalty.core.dao.OrderCashbackCalculationDao;
import ru.yandex.market.loyalty.core.dao.OrderCashbackCalculationNoMultistageDao;
import ru.yandex.market.loyalty.core.dao.OrderTerminationDao;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.model.cashback.CashbackDetailsGroupDescriptor;
import ru.yandex.market.loyalty.core.model.multistage.ResolvingState;
import ru.yandex.market.loyalty.core.model.order.entity.MultiorderEntry;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.trigger.event.CoreOrderStatus;
import ru.yandex.market.loyalty.core.model.trigger.event.data.OrderTermination;
import ru.yandex.market.loyalty.core.model.wallet.OrderCashbackCalculation;
import ru.yandex.market.loyalty.core.model.wallet.OrderCashbackCalculationNoMultistage;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletRefundTransactionStatus;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.rule.MinOrderTotalCuttingRule;
import ru.yandex.market.loyalty.core.rule.Rule;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.cashback.CashbackDetailsGroupService;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.SUPPLIER_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.loyaltyProgramPartner;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor({CashbackController.class})
public class CashbackControllerCashbackDetailsTest extends MarketLoyaltyBackMockedDbTestBase {
    public static final long DEFAULT_ORDER_ID = 1L;
    public static final long ANOTHER_ORDER_ID = 2L;
    public static final String DEFAULT_MULTI_ORDER_ID = "multiOrderId";
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private YandexWalletTransactionDao yandexWalletTransactionDao;
    @Autowired
    private MultiorderDao multiorderDao;
    @Autowired
    private OrderTerminationDao orderTerminationDao;
    @Autowired
    private CashbackDetailsGroupService cashbackDetailsGroupService;
    @Autowired
    private OrderCashbackCalculationDao orderCashbackCalculationDao;
    @Autowired
    private OrderCashbackCalculationNoMultistageDao orderCashbackCalculationNoMultistageDao;

    @Before
    public void configure() {
        configurationService.set(ConfigurationService.MULTIORDER_TABLE_PREFERABLE, true);
    }

    @Test
    public void shouldDrawDetailsForOneSingleOrder() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN,
                CashbackLevelType.MULTI_ORDER));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        saveYandexWalletTransaction(promo,
                DEFAULT_UID,
                null,
                DEFAULT_ORDER_ID,
                55,
                YandexWalletTransactionStatus.PENDING
        );

        StructuredCashbackResponses structuredCashbackResponses = requestCashbackDetails(
                StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey",
                                        BigDecimal.valueOf(100),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .setUiPromoFlags(List.of("extra-cashback"))
                        .build()
        );

        assertThat(structuredCashbackResponses.getOrders(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getOrderId(), equalTo(1L));
        assertThat(structuredCashbackResponses.getOrders().get(0).getAmount(),
                comparesEqualTo(BigDecimal.valueOf(155)));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getUiPromoFlags(),
                contains("extra-cashback"));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getSuperGroups(), empty());
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups().get(0), allOf(
                hasProperty("key", equalTo("default")),
                hasProperty("name", equalTo("Стандартный кешбэк")),
                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(155))),
                hasProperty("promoKeys", containsInAnyOrder("promoKey", promo.getPromoKey())),
                hasProperty("uiPromoFlags", empty()),
                hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.PENDING))
        ));
    }

    @Test
    public void shouldDrawDetailsForOneSingleOrderWithCancelledMultistagePromo() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        savePendOrderCashbackCalculation(promo, DEFAULT_UID, null, DEFAULT_ORDER_ID, 55, RuleType.MIN_ORDER_TOTAL_CUTTING_RULE);
        saveFailOrderCashbackCalculation(promo, DEFAULT_UID, null, DEFAULT_ORDER_ID, 55, RuleType.ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE);

        StructuredCashbackResponses structuredCashbackResponses = requestCashbackDetails(
                StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey",
                                        BigDecimal.valueOf(100),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .build()
        );

        assertThat(structuredCashbackResponses.getOrders(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getOrderId(), equalTo(1L));
        assertThat(structuredCashbackResponses.getOrders().get(0).getAmount(),
                comparesEqualTo(BigDecimal.valueOf(100)));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getSuperGroups(), empty());
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups().get(0), allOf(
                hasProperty("key", equalTo("default")),
                hasProperty("name", equalTo("Стандартный кешбэк")),
                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(100))),
                hasProperty("promoKeys", containsInAnyOrder("promoKey")),
                hasProperty("uiPromoFlags", empty()),
                hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.PENDING))
        ));
    }

    private <T extends Rule> void saveFailOrderCashbackCalculation(Promo promo, long uid, String multiOrderId,
                                                                   long orderId,
                                                                   int amount, RuleType<T> ruleType) {
        saveOrderCashbackCalculation(
                promo,
                uid,
                multiOrderId,
                orderId,
                amount,
                ResolvingState.CANCELLED,
                ResolvingState.INTERMEDIATE,
                ResolvingState.CANCELLED,
                null,
                ruleType
        );
    }

    @Test
    public void shouldReturnFallbackForOldOrders() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN,
                CashbackLevelType.MULTI_ORDER));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        saveYandexWalletTransaction(promo, DEFAULT_UID, null, DEFAULT_ORDER_ID, 55,
                YandexWalletTransactionStatus.PENDING);

        StructuredCashbackResponses structuredCashbackResponses = requestCashbackDetails(
                StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey",
                                        BigDecimal.valueOf(100),
                                        null,
                                        null,
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .setUiPromoFlags(List.of("extra-cashback"))
                        .build()
        );

        assertThat(structuredCashbackResponses.getOrders(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getOrderId(), equalTo(1L));
        assertThat(structuredCashbackResponses.getOrders().get(0).getAmount(),
                comparesEqualTo(BigDecimal.valueOf(155)));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getSuperGroups(), empty());
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups().get(0), allOf(
                hasProperty("key", equalTo("default")),
                hasProperty("name", equalTo("Стандартный кешбэк")),
                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(155))),
                hasProperty("promoKeys", containsInAnyOrder("promoKey", promo.getPromoKey())),
                hasProperty("uiPromoFlags", empty()),
                hasProperty("cmsSemanticId", equalTo("default-cashback")),
                hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.PENDING))
        ));
    }

    @Test
    public void shouldDrawDetailsForTwoSingleOrders() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        saveYandexWalletTransaction(promo, DEFAULT_UID, null, DEFAULT_ORDER_ID, 55,
                YandexWalletTransactionStatus.CANCELLED);

        StructuredCashbackResponses structuredCashbackResponses = requestCashbackDetails(
                StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey",
                                        BigDecimal.valueOf(100),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .setUiPromoFlags(List.of("extra-cashback"))
                        .build()
                ,
                StructuredCashbackRequest.builder()
                        .setOrderId(ANOTHER_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey2",
                                        BigDecimal.valueOf(200),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .setUiPromoFlags(List.of("extra-cashback"))
                        .build()
        );

        assertThat(structuredCashbackResponses.getOrders(), hasSize(2));
        assertThat(structuredCashbackResponses.getOrders(), containsInAnyOrder(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("orderId", equalTo(ANOTHER_ORDER_ID))
                )
        );
    }

    @Test
    public void shouldDrawDetailsForMultiOrder() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN,
                        CashbackLevelType.MULTI_ORDER)
                .setCashbackDetailsCartGroupName("payment_system"));
        createCasbackDetailsGroup("payment_system", "Платежная система");

        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        saveMultiorderEntry(DEFAULT_ORDER_ID, DEFAULT_MULTI_ORDER_ID);
        saveMultiorderEntry(ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID);

        saveYandexWalletTransaction(promo, DEFAULT_UID, DEFAULT_MULTI_ORDER_ID, null, 55,
                YandexWalletTransactionStatus.PENDING);

        StructuredCashbackResponses structuredCashbackResponses = requestCashbackDetails(
                StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey",
                                        BigDecimal.valueOf(100),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .setUiPromoFlags(List.of("extra-cashback"))
                        .build(),
                StructuredCashbackRequest.builder()
                        .setOrderId(ANOTHER_ORDER_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey2",
                                        BigDecimal.valueOf(200),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .build()
        );

        final List<StructuredCashbackResponse> orders = structuredCashbackResponses.getOrders();
        assertThat(orders, hasSize(2));
        assertThat(orders, containsInAnyOrder(
                allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(127)))
                ),
                allOf(
                        hasProperty("orderId", equalTo(ANOTHER_ORDER_ID)),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(228)))
                )
        ));
    }

    @Test
    public void shouldDrawDetailsForMultiOrderWithCancelledTransaction() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                .setCashbackDetailsCartGroupName("payment_system"));
        createCasbackDetailsGroup("payment_system", "Платежная система");

        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        saveMultiorderEntry(DEFAULT_ORDER_ID, DEFAULT_MULTI_ORDER_ID);
        saveMultiorderEntry(ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID);

        saveYandexWalletTransaction(promo, DEFAULT_UID, DEFAULT_MULTI_ORDER_ID, null, 55,
                YandexWalletTransactionStatus.CANCELLED);

        StructuredCashbackResponses structuredCashbackResponses = requestCashbackDetails(
                StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey",
                                        BigDecimal.valueOf(100),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .setUiPromoFlags(List.of("extra-cashback"))
                        .build(),
                StructuredCashbackRequest.builder()
                        .setOrderId(ANOTHER_ORDER_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey2",
                                        BigDecimal.valueOf(200),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .build()
        );

        final List<StructuredCashbackResponse> orders = structuredCashbackResponses.getOrders();
        assertThat(orders, hasSize(2));
        assertThat(orders, containsInAnyOrder(
                allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(100)))
                ),
                allOf(
                        hasProperty("orderId", equalTo(ANOTHER_ORDER_ID)),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(200)))
                )
        ));
        // no description for cancelled transaction
        assertThat(orders.get(0).getDetails().getSuperGroups(), everyItem(
                hasProperty("description", nullValue())
        ));
        assertThat(orders.get(1).getDetails().getSuperGroups(), everyItem(
                hasProperty("description", nullValue())
        ));
    }

    @Test
    public void shouldFixMarketdiscount8622() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN,
                        CashbackLevelType.MULTI_ORDER)
                .setCashbackDetailsCartGroupName("payment_system"));
        createCasbackDetailsGroup("payment_system", "Платежная система");

        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        saveMultiorderEntry(DEFAULT_ORDER_ID, DEFAULT_MULTI_ORDER_ID);
        saveMultiorderEntry(ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID);
        saveOrderTermination(ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID, CoreOrderStatus.CANCELLED);

        saveYandexWalletTransaction(promo, DEFAULT_UID, DEFAULT_MULTI_ORDER_ID, null, 100,
                YandexWalletTransactionStatus.PENDING);

        StructuredCashbackResponses structuredCashbackResponses = requestCashbackDetails(
                StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .build(),
                StructuredCashbackRequest.builder()
                        .setOrderId(ANOTHER_ORDER_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .build()
        );

        final List<StructuredCashbackResponse> orders = structuredCashbackResponses.getOrders();
        assertThat(orders, hasSize(2));
        assertThat(orders, containsInAnyOrder(
                allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(100)))
                ),
                allOf(
                        hasProperty("orderId", equalTo(ANOTHER_ORDER_ID)),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(0)))
                )
        ));
    }

    @Test
    public void shouldDrawDetailsForOneOrderFromMultiOrder() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN,
                        CashbackLevelType.MULTI_ORDER)
                .setCashbackDetailsCartGroupName("payment_system"));
        createCasbackDetailsGroup("payment_system", "Платежная система");

        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        saveMultiorderEntry(DEFAULT_ORDER_ID, DEFAULT_MULTI_ORDER_ID);
        saveMultiorderEntry(ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID);

        saveYandexWalletTransaction(promo, DEFAULT_UID, DEFAULT_MULTI_ORDER_ID, null, 55,
                YandexWalletTransactionStatus.PENDING);

        StructuredCashbackResponses structuredCashbackResponses = requestCashbackDetails(
                StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey",
                                        BigDecimal.valueOf(100),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .setUiPromoFlags(List.of("extra-cashback"))
                        .build()
        );

        assertThat(structuredCashbackResponses.getOrders(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders(), containsInAnyOrder(
                allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(127)))
                )
        ));
    }

    @Test
    public void shouldSumInputCashback() {
        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        StructuredCashbackResponses structuredCashbackResponses = requestCashbackDetails(
                StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey",
                                        BigDecimal.valueOf(50),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                ),
                                new ExternalItemCashback(
                                        "promoKey",
                                        BigDecimal.valueOf(50),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )))
                        .setUiPromoFlags(List.of("extra-cashback"))
                        .build()
        );

        assertThat(structuredCashbackResponses.getOrders(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders(), containsInAnyOrder(
                allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(100)))
                )
        ));
    }

    @Test
    public void shouldDrawAmountForOneOrderFromMultiOrder() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN, CashbackLevelType.MULTI_ORDER)
                .setCashbackDetailsCartGroupName("payment_system"));
        createCasbackDetailsGroup("payment_system", "Платежная система");

        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        saveMultiorderEntry(DEFAULT_ORDER_ID, DEFAULT_MULTI_ORDER_ID);
        saveMultiorderEntry(ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID);

        saveYandexWalletTransaction(promo, DEFAULT_UID, DEFAULT_MULTI_ORDER_ID, null, 55,
                YandexWalletTransactionStatus.PENDING);

        CashbackAmountResponses cashbackAmountResponses = getCashbackAmount(
                new CashbackAmountRequest(
                        DEFAULT_ORDER_ID,
                        DEFAULT_MULTI_ORDER_ID
                )
        );

        assertThat(cashbackAmountResponses.getResponses(), hasSize(1));
        assertThat(cashbackAmountResponses.getResponses(), containsInAnyOrder(
                allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("multiOrderId", equalTo(DEFAULT_MULTI_ORDER_ID)),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(27))),
                        hasProperty("uiPromoFlags", is(empty()))
                )
        ));
    }

    @Test
    public void shouldDrawAmountForOneOrderFromMultiOrderWithCancelledOrder() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN, CashbackLevelType.MULTI_ORDER)
                .setCashbackDetailsCartGroupName("payment_system"));
        createCasbackDetailsGroup("payment_system", "Платежная система");

        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        saveMultiorderEntry(DEFAULT_ORDER_ID, DEFAULT_MULTI_ORDER_ID);
        saveMultiorderEntry(ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID);
        saveOrderTermination(ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID, CoreOrderStatus.CANCELLED);

        saveYandexWalletTransaction(promo, DEFAULT_UID, DEFAULT_MULTI_ORDER_ID, null, 55,
                YandexWalletTransactionStatus.PENDING);

        CashbackAmountResponses cashbackAmountResponses = getCashbackAmount(
                new CashbackAmountRequest(
                        DEFAULT_ORDER_ID,
                        DEFAULT_MULTI_ORDER_ID
                )
        );

        assertThat(cashbackAmountResponses.getResponses(), hasSize(1));
        assertThat(cashbackAmountResponses.getResponses(), containsInAnyOrder(
                allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("multiOrderId", equalTo(DEFAULT_MULTI_ORDER_ID)),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(55))),
                        hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.PENDING)),
                        hasProperty("uiPromoFlags", is(empty()))
                )
        ));
    }

    @Test
    public void shouldDrawAmountForOneOrderFromMultiOrderWithCancelledOrder2() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                .setCashbackDetailsCartGroupName("payment_system"));
        createCasbackDetailsGroup("payment_system", "Платежная система");

        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        saveMultiorderEntry(DEFAULT_ORDER_ID, DEFAULT_MULTI_ORDER_ID);
        saveMultiorderEntry(ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID);
        saveOrderTermination(ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID, CoreOrderStatus.CANCELLED);

        saveYandexWalletTransaction(promo, DEFAULT_UID, DEFAULT_MULTI_ORDER_ID, null, 55,
                YandexWalletTransactionStatus.CANCELLED);

        CashbackAmountResponses cashbackAmountResponses = getCashbackAmount(
                new CashbackAmountRequest(
                        DEFAULT_ORDER_ID,
                        DEFAULT_MULTI_ORDER_ID
                )
        );

        assertThat(cashbackAmountResponses.getResponses(), hasSize(1));
        assertThat(cashbackAmountResponses.getResponses(), containsInAnyOrder(
                allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("multiOrderId", equalTo(DEFAULT_MULTI_ORDER_ID)),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(0))),
                        hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.CANCELLED)),
                        hasProperty("uiPromoFlags", is(empty()))
                )
        ));
    }

    @Test
    public void shouldDrawAmountForOneOrderFromMultiOrderWithAllCancelledOrders() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
                .setCashbackDetailsCartGroupName("payment_system"));
        createCasbackDetailsGroup("payment_system", "Платежная система");

        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        saveMultiorderEntry(DEFAULT_ORDER_ID, DEFAULT_MULTI_ORDER_ID);
        saveMultiorderEntry(ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID);
        saveOrderTermination(DEFAULT_ORDER_ID, DEFAULT_MULTI_ORDER_ID, CoreOrderStatus.CANCELLED);
        saveOrderTermination(ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID, CoreOrderStatus.CANCELLED);

        saveYandexWalletTransaction(promo, DEFAULT_UID, DEFAULT_MULTI_ORDER_ID, null, 55,
                YandexWalletTransactionStatus.CANCELLED);

        CashbackAmountResponses cashbackAmountResponses = getCashbackAmount(
                new CashbackAmountRequest(
                        DEFAULT_ORDER_ID,
                        DEFAULT_MULTI_ORDER_ID
                )
        );

        assertThat(cashbackAmountResponses.getResponses(), hasSize(1));
        assertThat(cashbackAmountResponses.getResponses(), containsInAnyOrder(
                allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("multiOrderId", equalTo(DEFAULT_MULTI_ORDER_ID)),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(0))),
                        hasProperty("uiPromoFlags", is(empty()))
                )
        ));
    }

    @Test
    public void shouldDrawMergedDetailsForMultiOrder() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN, CashbackLevelType.MULTI_ORDER)
                .setCashbackDetailsCartGroupName("payment_system"));
        createCasbackDetailsGroup("payment_system", "Платежная система");

        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        saveMultiorderEntry(DEFAULT_ORDER_ID, DEFAULT_MULTI_ORDER_ID);
        saveMultiorderEntry(ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID);

        saveYandexWalletTransaction(promo, DEFAULT_UID, DEFAULT_MULTI_ORDER_ID, null, 55,
                YandexWalletTransactionStatus.PENDING);

        StructuredCashbackResponses structuredCashbackResponses = requestCashbackDetails(
                CashbackMergeOption.MERGED,
                StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey",
                                        BigDecimal.valueOf(100),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .setUiPromoFlags(List.of("extra-cashback"))
                        .build(),
                StructuredCashbackRequest.builder()
                        .setOrderId(ANOTHER_ORDER_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey2",
                                        BigDecimal.valueOf(200),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .build()
        );

        assertThat(structuredCashbackResponses.getOrders(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getOrderId(), equalTo(-1L));
        assertThat(structuredCashbackResponses.getOrders().get(0).getAmount(),
                comparesEqualTo(BigDecimal.valueOf(355)));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getUiPromoFlags(),
                contains("extra-cashback"));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getSuperGroups(), hasSize(2));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getSuperGroups(),
                containsInRelativeOrder(
                        allOf(
                                hasProperty("key", equalTo("offer")),
                                hasProperty("description", nullValue())
                        ),
                        allOf(
                                hasProperty("key", equalTo("order")),
                                hasProperty("description", endsWith(": " + DEFAULT_ORDER_ID + "-" + ANOTHER_ORDER_ID))
                        )
                ));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups(), hasSize(2));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups(), containsInRelativeOrder(
                allOf(
                        hasProperty("key", equalTo("default")),
                        hasProperty("name", equalTo("Стандартный кешбэк")),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(300))),
                        hasProperty("promoKeys", containsInAnyOrder("promoKey", "promoKey2")),
                        hasProperty("uiPromoFlags", empty()),
                        hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.PENDING))
                ),
                allOf(
                        hasProperty("key", equalTo("payment_system")),
                        hasProperty("name", equalTo("Платежная система")),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(55))),
                        hasProperty("promoKeys", containsInAnyOrder(promo.getPromoKey())),
                        hasProperty("uiPromoFlags", empty()),
                        hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.PENDING))
                )
        ));
    }

    @Test
    public void shouldDrawFullDetailsForMultiOrder() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN, CashbackLevelType.MULTI_ORDER)
                .setCashbackDetailsCartGroupName("payment_system"));
        createCasbackDetailsGroup("payment_system", "Платежная система");

        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        saveMultiorderEntry(DEFAULT_ORDER_ID, DEFAULT_MULTI_ORDER_ID);
        saveMultiorderEntry(ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID);

        saveYandexWalletTransaction(promo, DEFAULT_UID, DEFAULT_MULTI_ORDER_ID, null, 55,
                YandexWalletTransactionStatus.PENDING);

        StructuredCashbackResponses structuredCashbackResponses = requestCashbackDetails(
                CashbackMergeOption.FULL,
                StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey",
                                        BigDecimal.valueOf(100),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .setUiPromoFlags(List.of("extra-cashback"))
                        .build(),
                StructuredCashbackRequest.builder()
                        .setOrderId(ANOTHER_ORDER_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey2",
                                        BigDecimal.valueOf(200),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .build()
        );

        assertThat(structuredCashbackResponses.getMerge(), is(notNullValue()));
        assertThat(structuredCashbackResponses.getMerge().getOrderId(), equalTo(-1L));
        assertThat(structuredCashbackResponses.getMerge().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(355)));
        assertThat(structuredCashbackResponses.getMerge().getDetails().getUiPromoFlags(), contains("extra-cashback"));
        assertThat(structuredCashbackResponses.getMerge().getDetails().getSuperGroups(), hasSize(2));
        assertThat(structuredCashbackResponses.getMerge().getDetails().getSuperGroups(), containsInRelativeOrder(
                allOf(
                        hasProperty("key", equalTo("offer")),
                        hasProperty("description", nullValue())
                ),
                allOf(
                        hasProperty("key", equalTo("order")),
                        hasProperty("description", endsWith(": " + DEFAULT_ORDER_ID + "-" + ANOTHER_ORDER_ID))
                )
        ));
        assertThat(structuredCashbackResponses.getMerge().getDetails().getGroups(), hasSize(2));
        assertThat(structuredCashbackResponses.getMerge().getDetails().getGroups(), containsInRelativeOrder(
                allOf(
                        hasProperty("key", equalTo("default")),
                        hasProperty("name", equalTo("Стандартный кешбэк")),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(300))),
                        hasProperty("promoKeys", containsInAnyOrder("promoKey", "promoKey2")),
                        hasProperty("uiPromoFlags", empty()),
                        hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.PENDING))
                ),
                allOf(
                        hasProperty("key", equalTo("payment_system")),
                        hasProperty("name", equalTo("Платежная система")),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(55))),
                        hasProperty("promoKeys", containsInAnyOrder(promo.getPromoKey())),
                        hasProperty("uiPromoFlags", empty()),
                        hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.PENDING))
                )
        ));
        assertThat(structuredCashbackResponses.getOrders(), hasSize(2));
        assertThat(structuredCashbackResponses.getOrders(), containsInAnyOrder(
                allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(127)))
                ),
                allOf(
                        hasProperty("orderId", equalTo(ANOTHER_ORDER_ID)),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(228)))
                )
        ));
    }

    @Test
    public void shouldDrawDetailsForOldOrderWithMissingFields() {
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();

        StructuredCashbackResponses structuredCashbackResponses = requestCashbackDetails(
                CashbackMergeOption.MERGED,
                StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        null,
                                        BigDecimal.valueOf(100),
                                        null,
                                        null,
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .build(),
                StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        null,
                                        BigDecimal.valueOf(200),
                                        null,
                                        null,
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .build()
        );

        assertThat(structuredCashbackResponses.getOrders(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getOrderId(), equalTo(-1L));
        assertThat(structuredCashbackResponses.getOrders().get(0).getAmount(),
                comparesEqualTo(BigDecimal.valueOf(300)));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getUiPromoFlags(), is(empty()));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getSuperGroups(), is(empty()));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups(), containsInAnyOrder(
                allOf(
                        hasProperty("key", equalTo("default")),
                        hasProperty("name", equalTo("Стандартный кешбэк")),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(300))),
                        hasProperty("promoKeys", containsInAnyOrder("fallback_promo_key_for_old_promos")),
                        hasProperty("uiPromoFlags", empty()),
                        hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.PENDING))
                )
        ));
    }


    @Test
    public void shouldDrawMergedDetailsForMultiOrderImmediatellyAfterSpend() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN,
                        CashbackLevelType.MULTI_ORDER)
                .addCashbackRule(RuleType.ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE,
                        RuleParameterName.CLIENT_ONLINE_CARD_PAYMENT_SYSTEM,
                        PaymentSystem.MASTERCARD)
                .setCashbackDetailsCartGroupName("payment_system"));
        createCasbackDetailsGroup("payment_system", "Платежная система");

        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderId(Long.toString(DEFAULT_ORDER_ID))
                                        .withOrderItem(
                                                warehouse(SUPPLIER_WAREHOUSE_ID),
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(2800),
                                                loyaltyProgramPartner(false)
                                        )
                                        .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                                        .withPaymentType(PaymentType.BANK_CARD)
                                        .withPaymentSystem(PaymentSystem.MASTERCARD)
                                        .build(),
                                orderRequestWithBundlesBuilder()
                                        .withOrderId(Long.toString(ANOTHER_ORDER_ID))
                                        .withOrderItem(
                                                warehouse(SUPPLIER_WAREHOUSE_ID),
                                                itemKey(ANOTHER_ITEM_KEY),
                                                price(3000),
                                                loyaltyProgramPartner(false)
                                        )
                                        .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                                        .withPaymentType(PaymentType.BANK_CARD)
                                        .withPaymentSystem(PaymentSystem.MASTERCARD)
                                        .build())
                        .withMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .build()
        );

        StructuredCashbackResponses structuredCashbackResponses = requestCashbackDetails(
                CashbackMergeOption.MERGED,
                StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .build(),
                StructuredCashbackRequest.builder()
                        .setOrderId(ANOTHER_ORDER_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .build()
        );

        assertThat(structuredCashbackResponses.getOrders(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getOrderId(), equalTo(-1L));
        assertThat(structuredCashbackResponses.getOrders().get(0).getAmount(),
                comparesEqualTo(BigDecimal.valueOf(580)));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getUiPromoFlags(), is(empty()));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getSuperGroups(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getSuperGroups(), contains(
                allOf(
                        hasProperty("key", equalTo("order")),
                        hasProperty("description", endsWith(": " + DEFAULT_ORDER_ID + "-" + ANOTHER_ORDER_ID))
                )
        ));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups(), contains(
                allOf(
                        hasProperty("key", equalTo("payment_system")),
                        hasProperty("name", equalTo("Платежная система")),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(580))),
                        hasProperty("promoKeys", containsInAnyOrder(promo.getPromoKey())),
                        hasProperty("uiPromoFlags", empty()),
                        hasProperty("cmsSemanticId", is(nullValue())),
                        hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.PENDING))
                )
        ));
    }

    @Test
    public void shouldNotDrawZeroMergedCashback() {
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN,
                        CashbackLevelType.MULTI_ORDER)
                .addCashbackRule(RuleType.ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE,
                        RuleParameterName.CLIENT_ONLINE_CARD_PAYMENT_SYSTEM,
                        PaymentSystem.MASTERCARD)
                .setCashbackDetailsCartGroupName("payment_system"));
        createCasbackDetailsGroup("payment_system", "Платежная система");

        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);


        marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderId(Long.toString(DEFAULT_ORDER_ID))
                                        .withOrderItem(
                                                warehouse(SUPPLIER_WAREHOUSE_ID),
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(2800),
                                                loyaltyProgramPartner(false)
                                        )
                                        .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                                        .withPaymentType(PaymentType.BANK_CARD)
                                        .withPaymentSystem(PaymentSystem.MAESTRO)
                                        .build())
                        .build()
        );

        StructuredCashbackResponses structuredCashbackResponses = requestCashbackDetails(
                CashbackMergeOption.MERGED,
                StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .build()
        );

        assertThat(structuredCashbackResponses.getOrders(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getOrderId(), equalTo(-1L));
        assertThat(structuredCashbackResponses.getOrders().get(0).getAmount(),
                comparesEqualTo(BigDecimal.valueOf(0)));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getUiPromoFlags(), is(empty()));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getSuperGroups(), is(empty()));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups(), is(empty()));
    }


    @Test
    public void shouldNotDrawReferralCashbackInDetails() {
        Promo promo1 = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN,
                CashbackLevelType.MULTI_ORDER));
        Promo promo2 = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN,
                CashbackLevelType.MULTI_ORDER));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.REFERRAL_PROGRAM_PROMO_KEY_ACCRUAL, promo2.getPromoKey());

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        saveYandexWalletTransaction(promo1, DEFAULT_UID, null, DEFAULT_ORDER_ID, 100,
                YandexWalletTransactionStatus.PENDING);
        saveYandexWalletTransaction(promo2, ANOTHER_UID, null, DEFAULT_ORDER_ID, 200,
                YandexWalletTransactionStatus.PENDING);

        StructuredCashbackResponses structuredCashbackResponses =
                requestCashbackDetails(StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .build());

        assertThat(structuredCashbackResponses.getOrders(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getOrderId(), equalTo(DEFAULT_ORDER_ID));
        assertThat(structuredCashbackResponses.getOrders().get(0).getAmount(),
                comparesEqualTo(BigDecimal.valueOf(100)));
    }

    @Test
    public void shouldDrawDetailsForOneSingleBnplOrderOneSupergroup() {
        StructuredCashbackResponses structuredCashbackResponses = requestCashbackDetails(
                StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey",
                                        BigDecimal.valueOf(100),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .setUiPromoFlags(List.of("extra-cashback"))
                        .setBnplStatus(BnplStatus.ENABLED)
                        .build()
        );

        assertThat(structuredCashbackResponses.getOrders(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getOrderId(), equalTo(DEFAULT_ORDER_ID));
        assertThat(structuredCashbackResponses.getOrders().get(0).getAmount(),
                comparesEqualTo(BigDecimal.valueOf(100)));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getUiPromoFlags(),
                contains("extra-cashback"));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getSuperGroups(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getSuperGroups().get(0), allOf(
                hasProperty("key", equalTo("split")),
                hasProperty("name", equalTo("Придёт после последнего платежа")),
                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(100))),
                hasProperty("groupKeys", containsInAnyOrder("default")),
                hasProperty("uiPromoFlags", empty())
        ));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups().get(0), allOf(
                hasProperty("key", equalTo("default")),
                hasProperty("name", equalTo("Стандартный кешбэк")),
                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(100))),
                hasProperty("promoKeys", containsInAnyOrder("promoKey")),
                hasProperty("uiPromoFlags", empty()),
                hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.PENDING))
        ));
    }

    @Test
    public void shouldDrawDetailsForOneSingleBnplOrderTwoSuperGroups() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN, CashbackLevelType.MULTI_ORDER)
                .setCashbackDetailsCartGroupName("payment_system"));
        createCasbackDetailsGroup("payment_system", "Платежная система");

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        saveYandexWalletTransaction(promo,
                DEFAULT_UID,
                null,
                DEFAULT_ORDER_ID,
                55,
                YandexWalletTransactionStatus.PENDING
        );

        StructuredCashbackResponses structuredCashbackResponses = requestCashbackDetails(
                StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey",
                                        BigDecimal.valueOf(100),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .setBnplStatus(BnplStatus.ENABLED)
                        .setUiPromoFlags(List.of("extra-cashback"))
                        .build()
        );

        assertThat(structuredCashbackResponses.getOrders(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getOrderId(), equalTo(DEFAULT_ORDER_ID));
        assertThat(structuredCashbackResponses.getOrders().get(0).getAmount(),
                comparesEqualTo(BigDecimal.valueOf(155)));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getUiPromoFlags(),
                contains("extra-cashback"));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getSuperGroups(), hasSize(2));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getSuperGroups().get(0), allOf(
                hasProperty("key", equalTo("order")),
                hasProperty("name", equalTo("Придёт после доставки последнего заказа")),
                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(55))),
                hasProperty("groupKeys", containsInAnyOrder("payment_system")),
                hasProperty("uiPromoFlags", empty()))
        );
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getSuperGroups().get(1), allOf(
                hasProperty("key", equalTo("split")),
                hasProperty("name", equalTo("Придёт после последнего платежа")),
                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(100))),
                hasProperty("groupKeys", containsInAnyOrder("default")),
                hasProperty("uiPromoFlags", empty())
        ));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups(), hasSize(2));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups(), containsInAnyOrder(
                allOf(
                        hasProperty("key", equalTo("payment_system")),
                        hasProperty("name", equalTo("Платежная система")),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(55))),
                        hasProperty("promoKeys", containsInAnyOrder(promo.getPromoKey())),
                        hasProperty("uiPromoFlags", empty()),
                        hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.PENDING))
                ),
                allOf(
                        hasProperty("key", equalTo("default")),
                        hasProperty("name", equalTo("Стандартный кешбэк")),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(100))),
                        hasProperty("promoKeys", containsInAnyOrder("promoKey")),
                        hasProperty("uiPromoFlags", empty()),
                        hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.PENDING))
                )));
    }


    @Test
    public void shouldDrawDetailsForNoMultistagePromo() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN, CashbackLevelType.MULTI_ORDER)
                .setCalculateOnDeliveryOnly(true));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        saveOrderCashbackCalculationNoMultistage(
                promo,
                DEFAULT_UID,
                null,
                DEFAULT_ORDER_ID,
                55
        );

        StructuredCashbackResponses structuredCashbackResponses =
                requestCashbackDetails(StructuredCashbackRequest.builder().setOrderId(
                                DEFAULT_ORDER_ID).
                        setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey",
                                        BigDecimal.valueOf(100),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .setUiPromoFlags(List.of("extra-cashback"))
                        .build()
                );

        assertThat(structuredCashbackResponses.getOrders(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getOrderId(), equalTo(1L));
        assertThat(structuredCashbackResponses.getOrders().get(0).getAmount(),
                comparesEqualTo(BigDecimal.valueOf(155)));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getUiPromoFlags(),
                contains("extra-cashback"));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getSuperGroups(), empty());
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups().get(0), allOf(
                hasProperty("key", equalTo("default")),
                hasProperty("name", equalTo("Стандартный кешбэк")),
                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(155))),
                hasProperty("promoKeys", containsInAnyOrder("promoKey", promo.getPromoKey())),
                hasProperty("uiPromoFlags", empty()),
                hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.PENDING))
        ));
    }

    @Test
    public void shouldDrawDetailsForMultistagePromo() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN, CashbackLevelType.MULTI_ORDER));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        savePendOrderCashbackCalculation(
                promo,
                DEFAULT_UID,
                null,
                DEFAULT_ORDER_ID,
                55, RuleType.MIN_ORDER_TOTAL_CUTTING_RULE
        );

        StructuredCashbackResponses structuredCashbackResponses =
                requestCashbackDetails(StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey",
                                        BigDecimal.valueOf(100),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .setUiPromoFlags(List.of("extra-cashback"))
                        .build()
                );

        assertThat(structuredCashbackResponses.getOrders(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getOrderId(), equalTo(1L));
        assertThat(structuredCashbackResponses.getOrders().get(0).getAmount(),
                comparesEqualTo(BigDecimal.valueOf(155)));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getUiPromoFlags(),
                contains("extra-cashback"));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getSuperGroups(), empty());
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups().get(0), allOf(
                hasProperty("key", equalTo("default")),
                hasProperty("name", equalTo("Стандартный кешбэк")),
                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(155))),
                hasProperty("promoKeys", containsInAnyOrder("promoKey", promo.getPromoKey())),
                hasProperty("uiPromoFlags", empty()),
                hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.PENDING))
        ));
    }

    @Test
    public void shouldDrawFullDetailsForMultiOrderWithExternalCashback() {
        Promo externalCashback = promoManager.createExternalCashbackPromo(PromoUtils.ExternalCashback.defaultBank()
                .setCashbackDetailsCartGroupName("external"));

        createCasbackDetailsGroup("external", "Внешний кешбэк");

        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        saveMultiorderEntry(DEFAULT_ORDER_ID, DEFAULT_MULTI_ORDER_ID);
        saveMultiorderEntry(ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID);

        saveYandexWalletTransaction(externalCashback, DEFAULT_UID, DEFAULT_MULTI_ORDER_ID, null, 55,
                YandexWalletTransactionStatus.FAKE_PENDING);

        StructuredCashbackResponses structuredCashbackResponses = requestCashbackDetails(
                CashbackMergeOption.FULL,
                StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey",
                                        BigDecimal.valueOf(100),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .setUiPromoFlags(List.of("extra-cashback"))
                        .build(),
                StructuredCashbackRequest.builder()
                        .setOrderId(ANOTHER_ORDER_ID)
                        .setMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey2",
                                        BigDecimal.valueOf(200),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .build()
        );

        assertThat(structuredCashbackResponses.getMerge(), is(notNullValue()));
        assertThat(structuredCashbackResponses.getMerge().getOrderId(), equalTo(-1L));
        assertThat(structuredCashbackResponses.getMerge().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(355)));
        assertThat(structuredCashbackResponses.getMerge().getDetails().getUiPromoFlags(), contains("extra-cashback"));
        assertThat(structuredCashbackResponses.getMerge().getDetails().getSuperGroups(), hasSize(2));
        assertThat(structuredCashbackResponses.getMerge().getDetails().getSuperGroups(), containsInRelativeOrder(
                allOf(
                        hasProperty("key", equalTo("offer")),
                        hasProperty("description", nullValue())
                ),
                allOf(
                        hasProperty("key", equalTo("order")),
                        hasProperty("description", endsWith(": " + DEFAULT_ORDER_ID + "-" + ANOTHER_ORDER_ID))
                )
        ));
        assertThat(structuredCashbackResponses.getMerge().getDetails().getGroups(), hasSize(2));
        assertThat(structuredCashbackResponses.getMerge().getDetails().getGroups(), containsInRelativeOrder(
                allOf(
                        hasProperty("key", equalTo("default")),
                        hasProperty("name", equalTo("Стандартный кешбэк")),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(300))),
                        hasProperty("promoKeys", containsInAnyOrder("promoKey", "promoKey2")),
                        hasProperty("uiPromoFlags", empty()),
                        hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.PENDING))
                ),
                allOf(
                        hasProperty("key", equalTo("external")),
                        hasProperty("name", equalTo("Внешний кешбэк")),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(55))),
                        hasProperty("promoKeys", containsInAnyOrder(externalCashback.getPromoKey())),
                        hasProperty("uiPromoFlags", empty()),
                        hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.PENDING))
                )
        ));
        assertThat(structuredCashbackResponses.getOrders(), hasSize(2));
        assertThat(structuredCashbackResponses.getOrders(), containsInAnyOrder(
                allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(127)))
                ),
                allOf(
                        hasProperty("orderId", equalTo(ANOTHER_ORDER_ID)),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(228)))
                )
        ));
    }

    @Test
    public void shouldDrawAmountForMultiOrderWithExternalCashback() {
        Promo externalCashback = promoManager.createExternalCashbackPromo(PromoUtils.ExternalCashback.defaultBank()
                .setCashbackDetailsCartGroupName("external"));

        createCasbackDetailsGroup("external", "Внешний кешбэк");

        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        saveMultiorderEntry(DEFAULT_ORDER_ID, DEFAULT_MULTI_ORDER_ID);
        saveMultiorderEntry(ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID);

        saveYandexWalletTransaction(externalCashback, DEFAULT_UID, DEFAULT_MULTI_ORDER_ID, null, 55,
                YandexWalletTransactionStatus.FAKE_CONFIRMED);

        CashbackAmountResponses cashbackAmountResponses = getCashbackAmount(
                new CashbackAmountRequest(
                        DEFAULT_ORDER_ID,
                        DEFAULT_MULTI_ORDER_ID
                )
        );

        assertThat(cashbackAmountResponses.getResponses(), hasSize(1));
        assertThat(cashbackAmountResponses.getResponses(), containsInAnyOrder(
                allOf(
                        hasProperty("orderId", equalTo(DEFAULT_ORDER_ID)),
                        hasProperty("multiOrderId", equalTo(DEFAULT_MULTI_ORDER_ID)),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(27))),
                        hasProperty("uiPromoFlags", is(empty())),
                        hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.SUCCESS))
                )
        ));
    }

    @Test
    public void shouldShowOnlyNotFakedTransactions() {
        Promo orderPromo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN,
                CashbackLevelType.MULTI_ORDER));
        Promo itemPromo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN,
                CashbackLevelType.ITEM));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        saveYandexWalletTransaction(orderPromo,
                DEFAULT_UID,
                null,
                DEFAULT_ORDER_ID,
                55,
                YandexWalletTransactionStatus.PENDING
        );
        saveYandexWalletTransaction(itemPromo,
                DEFAULT_UID,
                null,
                DEFAULT_ORDER_ID,
                100,
                YandexWalletTransactionStatus.FAKE_PENDING
        );

        StructuredCashbackResponses structuredCashbackResponses = requestCashbackDetails(
                StructuredCashbackRequest.builder()
                        .setOrderId(DEFAULT_ORDER_ID)
                        .setItemCashback(List.of(
                                new ExternalItemCashback(
                                        "promoKey",
                                        BigDecimal.valueOf(100),
                                        "default-cashback",
                                        "default",
                                        null,
                                        StructuredCashbackPromoAccrualStatus.PENDING
                                )
                        ))
                        .setUiPromoFlags(List.of("extra-cashback"))
                        .build()
        );

        assertThat(structuredCashbackResponses.getOrders(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getOrderId(), equalTo(1L));
        assertThat(structuredCashbackResponses.getOrders().get(0).getAmount(),
                comparesEqualTo(BigDecimal.valueOf(155)));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getUiPromoFlags(),
                contains("extra-cashback"));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getSuperGroups(), empty());
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups(), hasSize(1));
        assertThat(structuredCashbackResponses.getOrders().get(0).getDetails().getGroups().get(0), allOf(
                hasProperty("key", equalTo("default")),
                hasProperty("name", equalTo("Стандартный кешбэк")),
                hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(155))),
                hasProperty("promoKeys", containsInAnyOrder("promoKey", orderPromo.getPromoKey())),
                hasProperty("uiPromoFlags", empty()),
                hasProperty("status", equalTo(StructuredCashbackPromoAccrualStatus.PENDING))
        ));
    }

    private StructuredCashbackResponses requestCashbackDetails(StructuredCashbackRequest... requests) {
        return marketLoyaltyClient.cashbackDetails(
                new StructuredCashbackRequests(
                        List.of(
                                requests
                        )
                ),
                null
        );
    }

    private StructuredCashbackResponses requestCashbackDetails(CashbackMergeOption mergeOption,
                                                               StructuredCashbackRequest... requests) {
        return marketLoyaltyClient.cashbackDetails(
                new StructuredCashbackRequests(
                        List.of(
                                requests
                        )
                ),
                null,
                mergeOption
        );
    }

    @NotNull
    private YandexWalletTransaction saveYandexWalletTransaction(Promo promo, long uid, String multiOrderId,
                                                                Long orderId, int amount,
                                                                YandexWalletTransactionStatus status) {
        return yandexWalletTransactionDao.save(
                YandexWalletTransaction.builder()
                        .setUid(uid)
                        .setAmount(BigDecimal.valueOf(amount))
                        .setStatus(status)
                        .setPromoId(promo.getPromoId().getId())
                        .setProductId("productId")
                        .setUniqueKey("uniqueKey" + firstNonNull(orderId, multiOrderId) + promo.getPromoKey())
                        .setOrderId(orderId)
                        .setMultiOrderId(multiOrderId)
                        .setTryCount(0)
                        .setRefundStatus(YandexWalletRefundTransactionStatus.NOT_QUEUED)
                        .setCreationTime(Timestamp.from(clock.instant()))
                        .build()
        );
    }

    @NotNull
    private <T extends Rule> OrderCashbackCalculation saveOrderCashbackCalculation(Promo promo, long uid, String multiOrderId,
                                                                  Long orderId, int amount, ResolvingState result,
                                                                  ResolvingState initialResult,
                                                                  ResolvingState orderPaidResult,
                                                                  ResolvingState orderTerminationResult,
                                                                  RuleType<T> ruleType) {
        return orderCashbackCalculationDao.save(
                OrderCashbackCalculation.builder()
                        .setUid(uid)
                        .setInitialCashbackAmount(BigDecimal.valueOf(amount))
                        .setPromoId(promo.getPromoId().getId())
                        .setCashbackPropsId(promo.getCashbackPropsId())
                        .setOrderId(orderId)
                        .setMultiOrderId(multiOrderId)
                        .setInitialResult(initialResult)
                        .setOrderPaidResult(orderPaidResult)
                        .setOrderTerminationResult(orderTerminationResult)
                        .setResult(result)
                        .setRuleBeanName(ruleType.getBeanName())
                        .build()
        );
    }


    @NotNull
    private <T extends Rule> OrderCashbackCalculation savePendOrderCashbackCalculation(Promo promo, long uid,
                                                                                       String multiOrderId,
                                                                                       Long orderId,
                                                                                       int amount,
                                                                                       RuleType<MinOrderTotalCuttingRule> ruleType) {
        return saveOrderCashbackCalculation(promo,
                uid,
                multiOrderId,
                orderId,
                amount,
                ResolvingState.INTERMEDIATE,
                ResolvingState.INTERMEDIATE,
                ResolvingState.INTERMEDIATE,
                null, ruleType
        );
    }

    @NotNull
    private OrderCashbackCalculationNoMultistage saveOrderCashbackCalculationNoMultistage(Promo promo, long uid,
                                                                                          String multiOrderId,
                                                                                          Long orderId, int amount) {
        return orderCashbackCalculationNoMultistageDao.save(
                OrderCashbackCalculationNoMultistage.builder()
                        .setUid(uid)
                        .setInitialCashbackAmount(BigDecimal.valueOf(amount))
                        .setPromoId(promo.getPromoId().getId())
                        .setCashbackPropsId(promo.getCashbackPropsId())
                        .setOrderId(orderId)
                        .setMultiOrderId(multiOrderId)
                        .build()
        );
    }

    private void createCasbackDetailsGroup(String name, String title) {
        cashbackDetailsGroupService.createOrUpdateGroup(new CashbackDetailsGroupDescriptor(name, title));
    }

    private void saveMultiorderEntry(long orderId, String multiOrderId) {
        multiorderDao.save(new MultiorderEntry(null, orderId, multiOrderId, null));
    }

    private void saveOrderTermination(long orderId, String multiOrderId, CoreOrderStatus status) {
        orderTerminationDao.savePersistentData(new OrderTermination(null, orderId, multiOrderId, status, false),
                true);
    }

    private CashbackAmountResponses getCashbackAmount(CashbackAmountRequest cashbackAmountRequest) {
        return marketLoyaltyClient.getCashbackAmount(new CashbackAmountRequests(List.of(cashbackAmountRequest)));
    }
}
