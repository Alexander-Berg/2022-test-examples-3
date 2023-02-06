package ru.yandex.market.loyalty.back.controller.discount;

import java.math.BigDecimal;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.CashbackPromoRequest;
import ru.yandex.market.loyalty.api.model.PartnerCashbackRequest;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.controller.CashbackTestBase;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.cashback.BillingSchema;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.OrderRequestUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.api.model.PromoType.CASHBACK;
import static ru.yandex.market.loyalty.core.logbroker.EventType.CASHBACK_EMIT;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.IF_REPORT_PROMO_KNOWN_LOYALTY_PROMO_PREFERRED;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_MSKU;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_SUPPLIER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.categoryId;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.dropship;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.loyaltyProgramPartner;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.msku;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.nullCashbackPromo;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.promoKeys;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.supplier;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_EMAIL;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(DiscountController.class)
public class DiscountControllerReportCashbackTest extends CashbackTestBase {

    @Test
    public void shouldUseReportCashbackPromo() {
        // в лоялтевой акции и в акции от репорта разные номиналы
        // но без флага market.loyalty.config.if.report.promo.known.loyalty.promo.preferred
        // лоялти должен предпочесть акцию от репорта
        final Promo defaultPromo = createKnownToReportPromo(
                "reportPromoKey",
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
        );
        configureReportCashback(false);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(cashbackPromo("reportPromoKey", BigDecimal.ONE),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        supplier(100),
                                        loyaltyProgramPartner(false))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(discountResponse.getCashback().getEmit().getAmount(), comparesEqualTo(BigDecimal.valueOf(1)));
        assertThat(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getAmountByPromoKey().get(defaultPromo.getPromoKey()), comparesEqualTo(BigDecimal.valueOf(1)));
        assertEquals(Long.valueOf(0),
                discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getPromos().get(0).getPartnerId());
        assertThat(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getPromos().get(0).getMarketTariff(), comparesEqualTo(BigDecimal.valueOf(1)));
        assertThat(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getPromos().get(0).getPartnerTariff(), comparesEqualTo(BigDecimal.valueOf(0)));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", is(BigDecimal.ONE)),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(defaultPromo.getPromoKey())),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));
    }

    @Test
    public void shouldUseReportCashbackPromoForSingleUser() {
        // в лоялтевой акции и в акции от репорта разные номиналы
        final Promo defaultPromo = createKnownToReportPromo(
                "reportPromoKey",
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
        );
        // конфигурирует чтобы акции от репорта были включены только для одного uid
        configureReportCashback(DEFAULT_UID);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(cashbackPromo("reportPromoKey", BigDecimal.ONE),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        supplier(100),
                                        loyaltyProgramPartner(false))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withOperationContext(OperationContextFactory.withUidBuilder(DEFAULT_UID).buildOperationContext())
                        .build()
        );

        assertThat(BigDecimal.valueOf(1), comparesEqualTo(discountResponse.getCashback().getEmit().getAmount()));
        assertThat(BigDecimal.valueOf(1),
                comparesEqualTo(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getAmountByPromoKey().get(defaultPromo.getPromoKey())));
        assertEquals(Long.valueOf(0),
                discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getPromos().get(0).getPartnerId());
        assertThat(BigDecimal.valueOf(1),
                comparesEqualTo(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getPromos().get(0).getMarketTariff()));
        assertThat(BigDecimal.valueOf(0),
                comparesEqualTo(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getPromos().get(0).getPartnerTariff()));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", is(BigDecimal.ONE)),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(defaultPromo.getPromoKey())),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));
    }

    @Test
    public void shouldNotUseReportCashbackPromoForNotAllowedUid() {
        configurationService.disable(ConfigurationService.CASHBACK_PROMOS_FROM_REPORT_ENABLED);

        // в лоялтевой акции и в акции от репорта разные номиналы
        final Promo defaultPromo = createKnownToReportPromo(
                "reportPromoKey",
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
        );
        // конфигурирует чтобы акции от репорта были включены только для одного uid
        configureReportCashback(ANOTHER_UID);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(cashbackPromo("reportPromoKey", BigDecimal.ONE),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        supplier(100),
                                        loyaltyProgramPartner(false))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withOperationContext(OperationContextFactory.withUidBuilder(DEFAULT_UID).buildOperationContext())
                        .build()
        );

        assertThat(BigDecimal.valueOf(10), comparesEqualTo(discountResponse.getCashback().getEmit().getAmount()));
        assertThat(BigDecimal.valueOf(10),
                comparesEqualTo(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getAmountByPromoKey().get(defaultPromo.getPromoKey())));
        assertNull(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getPromos().get(0).getPartnerId());

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", is(BigDecimal.TEN)),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(defaultPromo.getPromoKey())),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));
    }

    @Test
    public void shouldNotUseReportCashbackIfPromoIsNotValid() {
        // в лоялтевой акции и в акции от репорта разные номиналы
        final Promo defaultPromo = createKnownToReportPromo(
                "reportPromoKey",
                PromoUtils.Cashback.defaultPercent(BigDecimal.TEN)
        );
        configureReportCashback(DEFAULT_UID);

        final BigDecimal wrongNominal = null;
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(cashbackPromo("reportPromoKey", wrongNominal),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        supplier(100),
                                        loyaltyProgramPartner(false))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .withOperationContext(OperationContextFactory.withUidBuilder(DEFAULT_UID).buildOperationContext())
                        .build()
        );

        assertThat(BigDecimal.valueOf(0), comparesEqualTo(discountResponse.getCashback().getEmit().getAmount()));

        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));
    }

    @Test
    public void shouldUseLoyaltyCashbackPromoBecauseReportPromoKnown() {
        // в лоялтевой акции и в акции от репорта разные номиналы
        final Promo defaultPromo = createKnownToReportPromo("reportPromoKey",
                PromoUtils.Cashback.defaultPercent(BigDecimal.ONE));
        configureReportCashback(true);
        // предпочитаем условия из лоялти если они известны лоялти
        configurationService.set(IF_REPORT_PROMO_KNOWN_LOYALTY_PROMO_PREFERRED, true);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(cashbackPromo("reportPromoKey", BigDecimal.TEN),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        supplier(100),
                                        loyaltyProgramPartner(false))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(BigDecimal.valueOf(1), comparesEqualTo(discountResponse.getCashback().getEmit().getAmount()));
        assertThat(BigDecimal.valueOf(1),
                comparesEqualTo(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getAmountByPromoKey().get(defaultPromo.getPromoKey())));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", is(BigDecimal.ONE)),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(defaultPromo.getPromoKey())),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));
    }

    @Test
    public void shouldUseLoyaltyCashbackPromoFromReportIfPromoInactive() {
        // в лоялтевой акции и в акции от репорта разные номиналы
        final Promo defaultPromo = createKnownToReportPromo("reportPromoKey",
                PromoUtils.Cashback.defaultPercent(BigDecimal.ONE));
        promoService.updateStatus(defaultPromo, PromoStatus.INACTIVE);
        configureReportCashback(true);

        // неактивной акции нет в индексе
        assertTrue(cashbackCacheService.getCashbackPropsOrEmpty(defaultPromo.getPromoKey(), false).isEmpty());
        // предпочитаем условия из лоялти если они известны лоялти
        configurationService.set(IF_REPORT_PROMO_KNOWN_LOYALTY_PROMO_PREFERRED, true);

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(cashbackPromo("reportPromoKey", BigDecimal.TEN),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        supplier(100),
                                        loyaltyProgramPartner(false))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(BigDecimal.valueOf(1), comparesEqualTo(discountResponse.getCashback().getEmit().getAmount()));
        assertThat(BigDecimal.valueOf(1),
                comparesEqualTo(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getAmountByPromoKey().get(defaultPromo.getPromoKey())));
    }

    @Test
    public void shouldSendCashbackEmitDiscountEvent() {
        configureReportCashback(true);
        registerTariffs();

        marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(cashbackPromo("reportPromoKey", BigDecimal.TEN, null),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        supplier(100),
                                        categoryId(HID_WITH_TARIFF),
                                        loyaltyProgramPartner(true))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        discountService.awaitLogBroker();

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("eventType", equalTo(CASHBACK_EMIT)),
                hasProperty("promoKey", equalTo("reportPromoKey")),
                hasProperty("httpMethod", equalTo("calc")),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(10))),
                hasProperty("cashbackPartnerAmount", comparesEqualTo(BigDecimal.valueOf(8.7))),
                hasProperty("cashbackMarketAmount", comparesEqualTo(BigDecimal.valueOf(1.3))),
                hasProperty("promoBucketName", equalTo("default")),
                hasProperty("partnerId", equalTo(100L)
                )
        )));
    }

    @Test
    public void shouldUseReportCashbackPromoAndPartnerBillingInfo() {
        configureReportCashback(true);
        registerTariffs();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(cashbackPromo(
                                        "reportPromoKey",
                                        BigDecimal.valueOf(5),
                                        new PartnerCashbackRequest(partnerCashbackService.getPartnerCashbackCurrentVersionId(), 1)
                                        ),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        categoryId(1000),
                                        supplier(100),
                                        price(100),
                                        loyaltyProgramPartner(false))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(BigDecimal.valueOf(5), comparesEqualTo(discountResponse.getCashback().getEmit().getAmount()));
        assertThat(BigDecimal.valueOf(5),
                comparesEqualTo(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getAmountByPromoKey().get("reportPromoKey")));
        assertEquals(Long.valueOf(1),
                discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getPromos().get(0).getPartnerId());
        assertThat(BigDecimal.valueOf(1.3),
                comparesEqualTo(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getPromos().get(0).getMarketTariff()));
        assertThat(BigDecimal.valueOf(3.7),
                comparesEqualTo(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getPromos().get(0).getPartnerTariff()));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", is(BigDecimal.valueOf(5L))),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is("reportPromoKey")),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));
    }

    @Test
    public void shouldSumExtraCashbackAndDefaultCashback() {
        configureReportCashback(true);
        registerTariffs();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        OrderRequestUtils.cashbackPromo(
                                                new CashbackPromoRequest(
                                                        "defaultPromoKey",
                                                        BigDecimal.valueOf(5),
                                                        DirectScale.asReportPriority(10),
                                                        "default",
                                                        null,
                                                        null,
                                                        null,
                                                        null,
                                                        null,
                                                        null),
                                                new CashbackPromoRequest(
                                                        "extraPromoKey",
                                                        BigDecimal.valueOf(10),
                                                        DirectScale.asReportPriority(10),
                                                        "extra",
                                                        null,
                                                        null,
                                                        null,
                                                        null,
                                                        null,
                                                        null)
                                        ),
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(100),
                                        dropship(false),
                                        supplier(100),
                                        loyaltyProgramPartner(false))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );
        MatcherAssert.assertThat(discountResponse.getCashback().getSpend().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(99)));
        MatcherAssert.assertThat(discountResponse.getCashback().getEmit().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(15)));
        MatcherAssert.assertThat(discountResponse.getOrders().get(0).getCashback().getSpend().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(99)));
        MatcherAssert.assertThat(discountResponse.getOrders().get(0).getCashback().getEmit().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(15)));
        MatcherAssert.assertThat(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getSpend().getAmount(), comparesEqualTo(BigDecimal.valueOf(99)));
        MatcherAssert.assertThat(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getAmount(), comparesEqualTo(BigDecimal.valueOf(15)));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", is(BigDecimal.valueOf(5L))),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is("defaultPromoKey")),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", is(BigDecimal.TEN)),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is("extraPromoKey")),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));
    }

    @Test
    public void shouldNotAllowExtraCashbackIfDefaultCashbackEmpty() {
        createNotKnownToReportPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10))
                        .setPriority(DirectScale.asLoyaltyPriority(10))
                        .setPromoBucketName("extra")
                        .setRequiredPromoBucket("default")
        );

        configureReportCashback(true);
        registerTariffs();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        nullCashbackPromo(),
                                        msku(ANOTHER_MSKU),
                                        categoryId(HID_WITH_TARIFF),
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        supplier(DEFAULT_SUPPLIER_ID),
                                        price(100),
                                        dropship(false),
                                        loyaltyProgramPartner(false))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );
        MatcherAssert.assertThat(discountResponse.getCashback().getSpend().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(99)));
        MatcherAssert.assertThat(discountResponse.getCashback().getEmit().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(0)));
        MatcherAssert.assertThat(discountResponse.getOrders().get(0).getCashback().getSpend().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(99)));
        MatcherAssert.assertThat(discountResponse.getOrders().get(0).getCashback().getEmit().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(0)));
        MatcherAssert.assertThat(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getSpend().getAmount(), comparesEqualTo(BigDecimal.valueOf(99)));
        MatcherAssert.assertThat(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getAmount(), comparesEqualTo(BigDecimal.valueOf(0)));

        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));
    }

    @Test
    public void shouldAllowExtraCashbackIfDefaultCashbackPresent() {
        Promo promo = createNotKnownToReportPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10))
                        .setPriority(DirectScale.asLoyaltyPriority(10))
                        .setPromoBucketName("extra")
                        .setRequiredPromoBucket("default")
                        .setBillingSchema(BillingSchema.SOLID)
        );

        configureReportCashback(true);
        registerTariffs();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        OrderRequestUtils.cashbackPromo(
                                                new CashbackPromoRequest(
                                                        "defaultPromoKey",
                                                        BigDecimal.valueOf(1),
                                                        DirectScale.asReportPriority(10),
                                                        "default",
                                                        null,
                                                        null,
                                                        null,
                                                        null,
                                                        null,
                                                        null)
                                        ),
                                        categoryId(HID_WITH_TARIFF),
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        supplier(DEFAULT_SUPPLIER_ID),
                                        price(100),
                                        dropship(false),
                                        loyaltyProgramPartner(false))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );
        MatcherAssert.assertThat(discountResponse.getCashback().getSpend().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(99)));
        MatcherAssert.assertThat(discountResponse.getCashback().getEmit().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(11)));
        MatcherAssert.assertThat(discountResponse.getOrders().get(0).getCashback().getSpend().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(99)));
        MatcherAssert.assertThat(discountResponse.getOrders().get(0).getCashback().getEmit().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(11)));
        MatcherAssert.assertThat(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getSpend().getAmount(), comparesEqualTo(BigDecimal.valueOf(99)));
        MatcherAssert.assertThat(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getAmount(), comparesEqualTo(BigDecimal.valueOf(11)));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", is(BigDecimal.TEN)),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(promo.getPromoKey())),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("calc")),
                hasProperty("eventType", is(CASHBACK_EMIT)),
                hasProperty("discount", is(BigDecimal.ONE)),
                hasProperty("uid", is(100L)),
                hasProperty("email", is(DEFAULT_EMAIL)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is("defaultPromoKey")),
                hasProperty("clientDeviceType", is(UsageClientDeviceType.DESKTOP))
        )));
    }

    @Test
    public void shouldFixMarketdiscount7008() {
        Promo promo1 = createNotKnownToReportPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(5), CashbackLevelType.MULTI_ORDER)
                        .setPriority(DirectScale.asLoyaltyPriority(-10))
                        .setPromoBucketName("payment_system")
        );
        Promo promo2 = createKnownToReportPromo(
                "knownToReportPromoKey1", PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10),
                        CashbackLevelType.MULTI_ORDER)
                        .setPriority(DirectScale.asLoyaltyPriority(-11))
                        .setPromoBucketName("payment_system")
        );
        promoService.updateStatus(promo2, PromoStatus.INACTIVE);

        configureReportCashback(true);
        registerTariffs();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        OrderRequestUtils.cashbackPromo(
                                                new CashbackPromoRequest(
                                                        "knownToReportPromoKey1",
                                                        BigDecimal.valueOf(10),
                                                        DirectScale.asReportPriority(11),
                                                        "payment_system",
                                                        null,
                                                        null,
                                                        null,
                                                        null,
                                                        null,
                                                        null)
                                        ),
                                        categoryId(HID_WITH_TARIFF),
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        supplier(DEFAULT_SUPPLIER_ID),
                                        price(100),
                                        dropship(false),
                                        loyaltyProgramPartner(false))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );
        MatcherAssert.assertThat(discountResponse.getCashback().getSpend().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(99)));
        MatcherAssert.assertThat(discountResponse.getCashback().getEmit().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(10)));
        MatcherAssert.assertThat(discountResponse.getOrders().get(0).getCashback().getSpend().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(99)));
        MatcherAssert.assertThat(discountResponse.getOrders().get(0).getCashback().getEmit().getAmount(),
                comparesEqualTo(BigDecimal.valueOf(0)));
        MatcherAssert.assertThat(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getSpend().getAmount(), comparesEqualTo(BigDecimal.valueOf(99)));
        MatcherAssert.assertThat(discountResponse.getOrders().get(0).getItems().get(0).getCashback().getEmit().getAmount(), comparesEqualTo(BigDecimal.valueOf(0)));
    }
}
