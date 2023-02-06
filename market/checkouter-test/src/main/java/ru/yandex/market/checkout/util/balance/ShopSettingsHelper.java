package ru.yandex.market.checkout.util.balance;

import java.util.Collections;
import java.util.List;

import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.pay.legacy.PaymentSubMethod;
import ru.yandex.market.checkout.checkouter.shop.ActualDeliveryRegionalCalculationRule;
import ru.yandex.market.checkout.checkouter.shop.OrderVisibility;
import ru.yandex.market.checkout.checkouter.shop.PaymentArticle;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.checkout.checkouter.shop.ShopActualDeliveryRegionalSettings;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.helpers.BlueCrossborderOrderHelper;

public final class ShopSettingsHelper {

    private static final Integer DEFAULT_AGENCY_COMMISSION = 0;

    private static final ShopMetaData INCORRECT_OLD_PREPAY_META;
    private static final ShopMetaData NEW_PREPAY_META;
    private static final ShopMetaData POSTPAY_META;
    private static final ShopMetaData RED_PREPAY_META;
    private static final ShopMetaData DSBS_SHOP_PREPAY_META;

    private static final String YA_MONEY_ID = "123";
    private static final PaymentArticle[] YA_MONEY_PAYMENT_ARTICLES = {new PaymentArticle("1",
            PaymentSubMethod.YA_MONEY, "1")};
    private static final ShopMetaData NEW_PREPAY_CROSSBORDER_META;

    static {
        INCORRECT_OLD_PREPAY_META = ShopMetaDataBuilder.createTestDefault()
                .withCampaiginId(1L)
                .withClientId(1L)
                .withSandboxClass(PaymentClass.YANDEX)
                .withProdClass(PaymentClass.YANDEX)
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withInn("1234567890")
                .withPhone("+7 495 739-70-00")
                .build();

        NEW_PREPAY_META = ShopMetaDataBuilder.createTestDefault()
                .withBusinessId(98765L)
                .withCampaiginId(2L)
                .withClientId(2)
                .withSandboxClass(PaymentClass.YANDEX)
                .withProdClass(PaymentClass.YANDEX)
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withInn("1234567890")
                .withPhone("+7 495 739-70-00")
                .build();

        NEW_PREPAY_CROSSBORDER_META = createCustomCrossborderMeta(
                (int) BlueCrossborderOrderHelper.RED_MARKET_VIRTUAL_SHOP_ID);

        POSTPAY_META = ShopMetaDataBuilder.createTestDefault()
                .withBusinessId(98765L)
                .withCampaiginId(4L)
                .withClientId(4)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.OFF)
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withInn("9876543210")
                .withPhone("+7 495 739-70-00")
                .build();

        RED_PREPAY_META = ShopMetaDataBuilder.createTestDefault()
                .withCampaiginId(5L)
                .withClientId(5L)
                .withSandboxClass(PaymentClass.GLOBAL)
                .withProdClass(PaymentClass.GLOBAL)
                .withPrepayType(PrepayType.YANDEX_MARKET_AG)
                .build();

        DSBS_SHOP_PREPAY_META = ShopMetaDataBuilder.createTestDefault()
                .withBusinessId(98765L)
                .withCampaiginId(15L)
                .withClientId(15L)
                .withSandboxClass(PaymentClass.YANDEX)
                .withProdClass(PaymentClass.YANDEX)
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withInn("1234567890")
                .withPhone("+7 495 739-70-00")
                .withActualDeliveryRegionalSettings(new ShopActualDeliveryRegionalSettings[]{
                        new ShopActualDeliveryRegionalSettings(213, false),
                        new ShopActualDeliveryRegionalSettings(39, true)
                })
                .withActualDeliveryRegionalCalculationRule(List.of(
                        new ActualDeliveryRegionalCalculationRule(213,
                                ActualDeliveryRegionalCalculationRule.CalculationRule.DEFAULT),
                        new ActualDeliveryRegionalCalculationRule(39,
                                ActualDeliveryRegionalCalculationRule.CalculationRule.OFFER_WITH_DELIVERY_CALC)
                )).withFreeLiftingEnabled(true)
                .build();
    }

    private ShopSettingsHelper() {
    }

    public static void createShopSettings(ShopService shopService, long shopId) {
        shopService.updateMeta(shopId, getDefaultMeta());
    }

    public static ShopMetaData createShopSettings(ShopService shopService, Long shopId, ShopMetaData other) {
        ShopMetaData metadata = ShopMetaDataBuilder.createTestDefault()
                .withCampaiginId(shopId)
                .withClientId(shopId)
                .withSandboxClass(other.getSandboxClass())
                .withProdClass(other.getProdClass())
                .withYaMoneyId(other.getYaMoneyId())
                .withArticles(other.getArticles())
                .withPrepayType(other.getPrepayType())
                .withInn(other.getInn())
                .withPhone(other.getPhoneNumber())
                .withAgencyCommission(other.getAgencyCommission())
                .withOrderVisibilityMap(other.getOrderVisibilityMap())
                .build();
        shopService.updateMeta(shopId, metadata);
        return metadata;
    }

    public static ShopMetaData getPostpayMeta() {
        return POSTPAY_META;
    }

    public static ShopMetaData getDefaultMeta() {
        return NEW_PREPAY_META;
    }

    public static ShopMetaData getDsbsShopPrepayMeta() {
        return DSBS_SHOP_PREPAY_META;
    }

    public static ShopMetaData getDefaultCrossborderMeta() {
        return NEW_PREPAY_CROSSBORDER_META;
    }

    public static ShopMetaData getIncorrectOldPrepayMeta() {
        return INCORRECT_OLD_PREPAY_META;
    }

    @Deprecated
    public static ShopMetaData getOldPrepayMeta() {
        return getOldPrepayMeta(YA_MONEY_PAYMENT_ARTICLES);
    }

    @Deprecated
    public static ShopMetaData getOldPrepayMeta(PaymentArticle[] articles) {
        return ShopMetaDataBuilder.createTestDefault()
                .withCampaiginId(1L)
                .withClientId(1L)
                .withSandboxClass(PaymentClass.YANDEX)
                .withProdClass(PaymentClass.YANDEX)
                .withYaMoneyId(YA_MONEY_ID)
                .withArticles(articles)
                .withPrepayType(PrepayType.YANDEX_MONEY)
                .build();
    }

    public static ShopMetaData getRedPrepayMeta() {
        return RED_PREPAY_META;
    }

    public static ShopMetaData createCustomNewPrepayMeta(Integer id) {
        return ShopMetaDataBuilder.createTestDefault()
                .withCampaiginId((long) id)
                .withClientId((long) id)
                .withBusinessId((long) id)
                .withSandboxClass(PaymentClass.YANDEX)
                .withProdClass(PaymentClass.YANDEX)
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withInn("1234567890")
                .withPhone("+7 495 739-70-00")
                .withAgencyCommission(DEFAULT_AGENCY_COMMISSION)
                .withOgrn("1397431111806")
                .withSupplierName("prepay_shop")
                .withMedicineLicense("medicine_license_12131415")
                .build();
    }

    public static ShopMetaData createCustomCrossborderMeta(int id) {
        return ShopMetaDataBuilder.createTestDefault()
                .withCampaiginId(id)
                .withClientId(id)
                .withBusinessId(id)
                .withSandboxClass(PaymentClass.YANDEX)
                .withProdClass(PaymentClass.YANDEX)
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withInn("1234567890")
                .withPhone("+7 495 739-70-00")
                .withOgrn("1397431111806")
                .withSupplierName("crossborder_shop")
                .withMedicineLicense("medicine_license_12131415")
                .build();
    }

    public static PaymentArticle[] paymentArticles() {
        return new PaymentArticle[]{
                new PaymentArticle("700", PaymentSubMethod.BANK_CARD, null),
                new PaymentArticle("700", PaymentSubMethod.YA_MONEY, null),
        };
    }

    // TODO Нужно совсем избавиться от YANDEX_MONEY
    @Deprecated
    public static ShopMetaDataBuilder oldPrepayBuilder() {
        return ShopMetaDataBuilder.createTestDefault()
                .withPrepayType(PrepayType.YANDEX_MONEY);
    }

    public static ShopMetaData createCustomNotFulfilmentMeta(int shopId, Long businessId) {
        return ShopMetaDataBuilder.createTestDefault()
                .withBusinessId(businessId)
                .withCampaiginId(shopId)
                .withClientId(shopId)
                .withSandboxClass(PaymentClass.OFF)
                .withProdClass(PaymentClass.OFF)
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withInn("7724053916")
                .withPhone("+7 999 999-99-99")
                .withAgencyCommission(300)
                .withOgrn("1397431111806")
                .withSupplierName("not_fulfilment_shop")
                .withOrderVisibilityMap(Collections.singletonMap(OrderVisibility.BUYER_EMAIL, true))
                .withMedicineLicense("medicine_license_12131415")
                .build();
    }

    public static ShopMetaData metaWithPaymentControlFlag(Integer id) {
        return ShopMetaDataBuilder.createTestDefault()
                .withCampaiginId((long) id)
                .withClientId((long) id)
                .withBusinessId((long) id)
                .withSandboxClass(PaymentClass.YANDEX)
                .withProdClass(PaymentClass.YANDEX)
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withInn("1234567890")
                .withPhone("+7 495 739-70-00")
                .withAgencyCommission(DEFAULT_AGENCY_COMMISSION)
                .withOgrn("1397431111806")
                .withSupplierName("prepay_shop")
                .withPaymentControlEnabled(true)
                .withMedicineLicense("medicine_license_12131415")
                .build();
    }

    public static ShopMetaData metaWithEmptySupplierName(Integer id) {
        return ShopMetaDataBuilder.createTestDefault()
                .withCampaiginId((long) id)
                .withClientId((long) id)
                .withBusinessId((long) id)
                .withSandboxClass(PaymentClass.YANDEX)
                .withProdClass(PaymentClass.YANDEX)
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withInn("1234567890")
                .withPhone("+7 495 739-70-00")
                .withAgencyCommission(DEFAULT_AGENCY_COMMISSION)
                .withOgrn("1397431111806")
                .withSupplierName(null)
                .withPaymentControlEnabled(true)
                .build();
    }
}
