package ru.yandex.market.loyalty.back.controller.discount;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.back.controller.CashbackTestBase;
import ru.yandex.market.loyalty.core.model.cashback.CashbackDetailsGroupDescriptor;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.cashback.CashbackDetailsGroupService;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.SUPPLIER_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.categoryId;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.loyaltyProgramPartner;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.OrderRequestWithBundlesBuilder.DEFAULT_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.Cashback.defaultPercent;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class DiscountControllerCashbackDetailsTest extends CashbackTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private CashbackDetailsGroupService cashbackDetailsGroupService;

    @Test
    public void shouldCalculateDefaultCashbackDetailsCartGroup() {
        Promo promo1 = promoManager.createCashbackPromo(defaultPercent(BigDecimal.TEN, CashbackLevelType.ORDER)
                .setCmsDescriptionSemanticId("test-id-1"));
        Promo promo2 = promoManager.createCashbackPromo(defaultPercent(BigDecimal.ONE, CashbackLevelType.ITEM)
                .setCmsDescriptionSemanticId("test-id-2"));

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
                        hasProperty("cashback",
                                hasProperty("emit",
                                        hasProperty("details",
                                                allOf(
                                                        hasProperty("cmsDescription", allOf(
                                                                hasEntry(
                                                                        equalTo(promo1.getPromoKey()),
                                                                        equalTo(promo1.getPromoParam(PromoParameterName.CMS_DESCRIPTION_SEMANTIC_ID).orElse(null))
                                                                ),
                                                                hasEntry(
                                                                        equalTo(promo2.getPromoKey()),
                                                                        equalTo(promo2.getPromoParam(PromoParameterName.CMS_DESCRIPTION_SEMANTIC_ID).orElse(null))
                                                                )
                                                        )),
                                                        hasProperty("groups",
                                                                contains(
                                                                        allOf(
                                                                                hasProperty("name", equalTo("Стандартный кешбэк")),
                                                                                hasProperty("key", equalTo("default")),
                                                                                hasProperty("amount",
                                                                                        comparesEqualTo(BigDecimal.valueOf(638)))
                                                                        )
                                                                )
                                                        )
                                                )
                                        ))
                        )
                ));

    }

    @Test
    public void shouldCalculateCashbackDetailsForEachOrder() {
        promoManager.createCashbackPromo(defaultPercent(BigDecimal.TEN, CashbackLevelType.ORDER)
                .setCmsDescriptionSemanticId("test-id-1"));
        promoManager.createCashbackPromo(defaultPercent(BigDecimal.ONE, CashbackLevelType.ITEM)
                .setCmsDescriptionSemanticId("test-id-2"));

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

        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                allOf(
                        hasProperty("cashback",
                                hasProperty("emit",
                                        hasProperty("details",
                                                allOf(
                                                        hasProperty("groups",
                                                                contains(
                                                                        allOf(
                                                                                hasProperty("name", equalTo(
                                                                                        "Стандартный кешбэк")),
                                                                                        hasProperty("key", equalTo(
                                                                                                "default")),
                                                                                        hasProperty("amount",
                                                                                                comparesEqualTo(BigDecimal.valueOf(308)))
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                ))
                                )
                        ), allOf(
                                hasProperty("cashback",
                                        hasProperty("emit",
                                                hasProperty("details",
                                                        allOf(
                                                                hasProperty("groups",
                                                                        contains(
                                                                                allOf(
                                                                                        hasProperty("name", equalTo(
                                                                                                "Стандартный кешбэк")),
                                                                                        hasProperty("key", equalTo(
                                                                                                "default")),
                                                                                        hasProperty("amount",
                                                                                                comparesEqualTo(BigDecimal.valueOf(330)))
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                ))
                                )
                        )
                )
        );
    }

    @Test
    public void shouldReturnNewFieldsInCashbackPromoResponse() {
        promoManager.createCashbackPromo(defaultPercent(BigDecimal.ONE, CashbackLevelType.ITEM)
                .setCmsDescriptionSemanticId("test-id-1"));

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

        assertThat(discountResponse.getOrders().get(0).getItems().get(0),
                allOf(
                        hasProperty("cashback",
                                hasProperty("emit", hasProperty("promos",
                                        contains(
                                                allOf(
                                                        hasProperty("amount",
                                                                equalTo(BigDecimal.valueOf(28))
                                                        ),
                                                        hasProperty("cmsSemanticId",
                                                                equalTo("test-id-1")
                                                        ),
                                                        hasProperty("detailsGroupName",
                                                                equalTo("default")
                                                        )
                                                )
                                        )))
                        )
                ));
    }

    @Test
    public void shouldCalculateCashbackDetailsForDifferentGroups() {
        promoManager.createCashbackPromo(defaultPercent(BigDecimal.TEN, CashbackLevelType.MULTI_ORDER).setCashbackDetailsCartGroupName("payment_system"));
        promoManager.createCashbackPromo(defaultPercent(BigDecimal.ONE, CashbackLevelType.ITEM).setCashbackDetailsCartGroupName("default"));

        cashbackDetailsGroupService.createOrUpdateGroup(new CashbackDetailsGroupDescriptor("payment_system",
                "Платежная система"));
        cashbackCacheService.reloadCashbackPromos();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();

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
                hasProperty("cashback",
                        hasProperty("emit",
                                hasProperty("details",
                                        hasProperty("groups",
                                                containsInAnyOrder(
                                                        allOf(
                                                                hasProperty("name", equalTo("Стандартный кешбэк")),
                                                                hasProperty("key", equalTo("default")),
                                                                hasProperty("amount",
                                                                        comparesEqualTo(BigDecimal.valueOf(58)))
                                                        ),
                                                        allOf(
                                                                hasProperty("name", equalTo("Платежная система")),
                                                                hasProperty("key", equalTo("payment_system")),
                                                                hasProperty("amount",
                                                                        comparesEqualTo(BigDecimal.valueOf(580)))
                                                        )
                                                ))
                                )
                        ))
        );

    }

    @Test
    public void shouldCalculateCashbackDetailsSuperGroupsForOnlyMultiLevelCashbackPromo() {
        promoManager.createCashbackPromo(defaultPercent(BigDecimal.TEN, CashbackLevelType.MULTI_ORDER).setCashbackDetailsCartGroupName("payment_system"));

        cashbackDetailsGroupService.createOrUpdateGroup(new CashbackDetailsGroupDescriptor("payment_system",
                "Платежная " +
                        "система"));
        cashbackCacheService.reloadCashbackPromos();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();

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

        // Матчер проверяет что супергруппы упорядочены в ответе. Если начал флапать то значит сломали код
        assertThat(discountResponse,
                allOf(
                        hasProperty("cashback",
                                hasProperty("emit",
                                        hasProperty("details",
                                                allOf(
                                                        hasProperty("groups",
                                                                contains(
                                                                        allOf(
                                                                                hasProperty("name", equalTo(
                                                                                        "Платежная система")),
                                                                                hasProperty("key", equalTo(
                                                                                        "payment_system")),
                                                                                hasProperty("amount",
                                                                                        comparesEqualTo(BigDecimal.valueOf(580)))
                                                                        )
                                                                )
                                                        ),
                                                        hasProperty("superGroups",
                                                                contains(
                                                                        allOf(
                                                                                hasProperty("name", equalTo("Придёт " +
                                                                                        "после доставки последнего " +
                                                                                        "заказа")),
                                                                                hasProperty("key", equalTo("order")),
                                                                                hasProperty("groupsKeys",
                                                                                        contains(equalTo(
                                                                                                "payment_system")))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                ))));
    }

    @Test
    public void shouldReturnSuperGroupsOrdered() {
        promoManager.createCashbackPromo(defaultPercent(BigDecimal.TEN, CashbackLevelType.MULTI_ORDER).setCashbackDetailsCartGroupName("group1"));
        promoManager.createCashbackPromo(defaultPercent(BigDecimal.ONE, CashbackLevelType.ITEM).setCashbackDetailsCartGroupName("group2"));

        cashbackDetailsGroupService.createOrUpdateGroup(new CashbackDetailsGroupDescriptor("group1", "Группа 1"));
        cashbackDetailsGroupService.createOrUpdateGroup(new CashbackDetailsGroupDescriptor("group2", "Группа 2"));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderId(DEFAULT_ORDER_ID)
                                        .withOrderItem(
                                                warehouse(SUPPLIER_WAREHOUSE_ID),
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(3000),
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
                        hasProperty("cashback",
                                hasProperty("emit",
                                        hasProperty("details",
                                                hasProperty("superGroups",
                                                        containsInRelativeOrder(
                                                                allOf(
                                                                        hasProperty("name", equalTo("Придёт с " +
                                                                                "товаром")),
                                                                        hasProperty("key", equalTo("offer")),
                                                                        hasProperty("groupsKeys", contains(equalTo(
                                                                                "group2")))
                                                                ),
                                                                allOf(
                                                                        hasProperty("name", equalTo("Придёт после " +
                                                                                "доставки последнего заказа")),
                                                                        hasProperty("key", equalTo("order")),
                                                                        hasProperty("groupsKeys", contains(equalTo(
                                                                                "group1")))
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
    public void shouldCalculateCashbackDetailsSuperGroupsForOnlyItemLevelCashbackPromo() {
        promoManager.createCashbackPromo(defaultPercent(BigDecimal.TEN, CashbackLevelType.ITEM));

        cashbackCacheService.reloadCashbackPromos();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();

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
                        hasProperty("cashback",
                                hasProperty("emit",
                                        hasProperty("details",
                                                allOf(
                                                        hasProperty("groups",
                                                                contains(
                                                                        allOf(
                                                                                hasProperty("name", equalTo(
                                                                                        "Стандартный кешбэк")),
                                                                                hasProperty("key", equalTo("default")),
                                                                                hasProperty("amount",
                                                                                        comparesEqualTo(BigDecimal.valueOf(580)))
                                                                        )
                                                                )
                                                        ),
                                                        hasProperty("superGroups", empty())
                                                )
                                        )
                                ))));
    }


    @Test
    public void shouldCalculateCashbackDetailsSuperGroupsForSingleOrder() {
        promoManager.createCashbackPromo(defaultPercent(BigDecimal.TEN, CashbackLevelType.MULTI_ORDER).setCashbackDetailsCartGroupName("payment_system"));

        cashbackDetailsGroupService.createOrUpdateGroup(new CashbackDetailsGroupDescriptor("payment_system",
                "Платежная " +
                        "система"));
        cashbackCacheService.reloadCashbackPromos();
        configurationService.set(ConfigurationService.YANDEX_CASHBACK_ENABLED, true);
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();

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
                        hasProperty("cashback",
                                hasProperty("emit",
                                        hasProperty("details",
                                                allOf(
                                                        hasProperty("groups",
                                                                contains(
                                                                        allOf(
                                                                                hasProperty("name", equalTo(
                                                                                        "Платежная система")),
                                                                                hasProperty("key", equalTo(
                                                                                        "payment_system")),
                                                                                hasProperty("amount",
                                                                                        comparesEqualTo(BigDecimal.valueOf(280)))
                                                                        )
                                                                )
                                                        ),
                                                        hasProperty("superGroups", empty())
                                                )
                                        )
                                ))
                )
        );
    }

    @Test
    public void shouldFixMarketdiscount7672() {
        promoManager.createCashbackPromo(defaultPercent(BigDecimal.TEN, CashbackLevelType.ITEM)
                .setPromoBucketName("extra")
                .setCashbackDetailsCartGroupName("extra"));

        configureReportCashback(true);
        registerTariffs();

        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderId(DEFAULT_ORDER_ID)
                                        .withOrderItem(
                                                cashbackPromo("reportPromoKey", BigDecimal.valueOf(2)),
                                                warehouse(SUPPLIER_WAREHOUSE_ID),
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(2800),
                                                loyaltyProgramPartner(false),
                                                categoryId(HID_WITH_TARIFF)
                                        )
                                        .withDeliveries(DeliveryRequestUtils.marketBrandedPickupDelivery())
                                        .build())
                        .build()
        );

        assertThat(discountResponse,
                allOf(
                        hasProperty("cashback",
                                hasProperty("emit",
                                        hasProperty("details",
                                                hasProperty("groups",
                                                        containsInAnyOrder(
                                                                allOf(
                                                                        hasProperty("name", equalTo("Стандартный " +
                                                                                "кешбэк")),
                                                                        hasProperty("key", equalTo("default")),
                                                                        hasProperty("amount",
                                                                                comparesEqualTo(BigDecimal.valueOf(56)))
                                                                ),
                                                                allOf(
                                                                        hasProperty("name", equalTo("Повышенный " +
                                                                                "кешбэк")),
                                                                        hasProperty("key", equalTo("extra")),
                                                                        hasProperty("amount",
                                                                                comparesEqualTo(BigDecimal.valueOf(280)))
                                                                )
                                                        )
                                                ))
                                )
                        )));

    }

    @Test
    public void shouldFallbackDefaultCashbackGroup() {
        promoManager.createCashbackPromo(defaultPercent(BigDecimal.TEN, CashbackLevelType.ITEM)
                .setPromoBucketName("extra")
                .setCashbackDetailsCartGroupName("extra"));

        configureReportCashback(true);

        cashbackDetailsGroupService.inactivateGroup("extra");

        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
        registerTariffs();

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
                                        .build())
                        .build()
        );

        assertThat(discountResponse,
                allOf(
                        hasProperty("cashback",
                                hasProperty("emit",
                                        hasProperty("details",
                                                hasProperty("groups",
                                                        containsInAnyOrder(
                                                                allOf(
                                                                        hasProperty("name", equalTo("Стандартный " +
                                                                                "кешбэк")),
                                                                        hasProperty("key", equalTo("default")),
                                                                        hasProperty("amount",
                                                                                comparesEqualTo(BigDecimal.valueOf(280)))
                                                                )
                                                        )
                                                ))
                                )
                        )));
    }

    @Test
    public void shouldDrawUiPromoFlags() {
        promoManager.createCashbackPromo(defaultPercent(BigDecimal.valueOf(1), CashbackLevelType.ITEM)
                .setUiPromoFlags(List.of("extra-cashback")));

        configureReportCashback(true);

        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
        registerTariffs();

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
                                        .build())
                        .build()
        );

        assertThat(discountResponse,
                allOf(
                        hasProperty("cashback",
                                hasProperty("emit",
                                        hasProperty("details",
                                                hasProperty("groups",
                                                        containsInAnyOrder(
                                                                allOf(
                                                                        hasProperty("name", equalTo("Стандартный " +
                                                                                "кешбэк")),
                                                                        hasProperty("key", equalTo("default")),
                                                                        hasProperty("uiPromoFlags", contains("extra" +
                                                                                "-cashback"))
                                                                )
                                                        )
                                                ))
                                )
                        )
                )
        );
    }

    @Test
    public void shouldAppendExtraCashbackPromoFlag() {
        // создаем акцию которая выше трешхолда повышенного кешбэка в категории
        // для этой акции не указывается флаг extra-cashback но он должен добавиться автоматически
        promoManager.createCashbackPromo(defaultPercent(BigDecimal.TEN, CashbackLevelType.ITEM));

        configureReportCashback(true);

        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();
        registerTariffs();

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderId(DEFAULT_ORDER_ID)
                                        .withOrderItem(
                                                warehouse(SUPPLIER_WAREHOUSE_ID),
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(2800),
                                                loyaltyProgramPartner(false),
                                                categoryId(HID_WITH_TARIFF)
                                        )
                                        .build())
                        .build()
        );

        assertThat(discountResponse.getOrders().get(0).getItems().get(0).getCashback(),
                hasProperty("emit",
                        hasProperty("uiPromoFlags",
                                contains("extra-cashback")
                        )

                )
        );
        assertThat(discountResponse.getCashback().getEmit().getDetails(),
                hasProperty("uiPromoFlags", contains("extra-cashback"))
        );
        assertThat(discountResponse.getCashback().getEmit().getDetails(),
                hasProperty("groups", contains(
                        allOf(
                                hasProperty("name", equalTo("Стандартный кешбэк")),
                                hasProperty("key", equalTo("default")),
                                hasProperty("uiPromoFlags", is(empty()))
                        )
                ))
        );
    }

    @Test
    public void shouldPlacePartnerX2CashbackToPartnerExtraCashbackGroup() {
        promoManager.createCashbackPromo(defaultPercent(BigDecimal.TEN, CashbackLevelType.ITEM));

        configureReportCashback(true);
        registerTariffs();

        cashbackCacheService.reloadCashbackPromos();
        cashbackDetailsGroupService.reloadCashbackDetailsGroupCache();

        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderId(DEFAULT_ORDER_ID)
                                        .withOrderItem(
                                                cashbackPromo("reportPromoKey", BigDecimal.valueOf(4)),
                                                warehouse(SUPPLIER_WAREHOUSE_ID),
                                                itemKey(DEFAULT_ITEM_KEY),
                                                price(2800),
                                                loyaltyProgramPartner(false),
                                                categoryId(HID_WITH_TARIFF)
                                        )
                                        .build())
                        .build()
        );

        assertThat(discountResponse.getCashback().getEmit().getDetails(),
                hasProperty(
                        "groups",
                        contains(
                                allOf(
                                        hasProperty("uiPromoFlags", contains("extra-cashback")),
                                        hasProperty("name", equalTo("Повышенный кешбэк от продавца")),
                                        hasProperty("key", equalTo("partner_extra")),
                                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(112)))
                                )
                        )
                )
        );
    }

}
