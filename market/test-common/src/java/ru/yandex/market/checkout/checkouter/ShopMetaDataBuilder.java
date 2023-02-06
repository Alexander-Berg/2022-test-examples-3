package ru.yandex.market.checkout.checkouter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import ru.yandex.market.checkout.checkouter.shop.ActualDeliveryRegionalCalculationRule;
import ru.yandex.market.checkout.checkouter.shop.DeliveryReceiptNeedType;
import ru.yandex.market.checkout.checkouter.shop.MigrationMapping;
import ru.yandex.market.checkout.checkouter.shop.OrderVisibility;
import ru.yandex.market.checkout.checkouter.shop.PaymentArticle;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.checkout.checkouter.shop.PrescriptionManagementSystem;
import ru.yandex.market.checkout.checkouter.shop.ShopActualDeliveryRegionalSettings;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;

import static ru.yandex.market.checkout.checkouter.shop.DeliveryReceiptNeedType.CREATE_DELIVERY_RECEIPT;

public class ShopMetaDataBuilder {

    private Long businessId;
    private long campaignId;
    private long clientId;
    private PaymentClass sandboxClass;
    private PaymentClass prodClass;
    private String yaMoneyId;
    private PaymentArticle[] articles;
    private PrepayType prepayType;
    private String inn;
    private String phone;
    private Map<OrderVisibility, Boolean> orderVisibilityMap;
    private Integer agencyCommission;
    private String ogrn;
    private String supplierName;
    private boolean isOrderAutoAcceptEnabled;
    private boolean isPushApiActualization;
    private boolean isPushApiActualizationRegional;
    private boolean supplierFastReturnEnabled;
    private ShopActualDeliveryRegionalSettings[] actualDeliveryRegionalSettings;
    private List<ActualDeliveryRegionalCalculationRule> actualDeliveryRegionalCalculationRules;
    private boolean freeLiftingEnabled;
    private boolean cartRequestTurnedOff;
    private DeliveryReceiptNeedType deliveryReceiptNeedType = CREATE_DELIVERY_RECEIPT;
    private boolean paymentControlEnabled;
    private PrescriptionManagementSystem prescriptionManagementSystem;
    private MigrationMapping migrationMapping;
    private String medicineLicense;
    private boolean selfEmployed;

    public static ShopMetaDataBuilder createTestDefault() {
        return new ShopMetaDataBuilder()
                .withBusinessId(2L)
                .withCampaiginId(2L)
                .withClientId(2)
                .withSandboxClass(PaymentClass.YANDEX)
                .withProdClass(PaymentClass.YANDEX)
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withInn("1234567890")
                .withPhone("+7 495 739-70-00");
    }

    public static ShopMetaDataBuilder create() {
        return new ShopMetaDataBuilder();
    }

    public static ShopMetaDataBuilder createCopy(ShopMetaData shopMetaData) {
        return (new ShopMetaDataBuilder())
                .copy(shopMetaData);
    }

    public ShopMetaDataBuilder copy(ShopMetaData shopMetaData) {
        businessId = shopMetaData.getBusinessId();
        campaignId = shopMetaData.getCampaignId();
        clientId = shopMetaData.getClientId();
        sandboxClass = shopMetaData.getSandboxClass();
        prodClass = shopMetaData.getProdClass();
        yaMoneyId = shopMetaData.getYaMoneyId();
        articles = Objects.isNull(shopMetaData.getArticles()) ? null : shopMetaData.getArticles().clone();
        prepayType = shopMetaData.getPrepayType();
        inn = shopMetaData.getInn();
        phone = shopMetaData.getPhoneNumber();
        orderVisibilityMap = shopMetaData.getOrderVisibilityMap();
        isOrderAutoAcceptEnabled = shopMetaData.isOrderAutoAcceptEnabled();
        isPushApiActualization = shopMetaData.isPushApiActualization();
        isPushApiActualizationRegional = shopMetaData.isPushApiActualizationRegional();
        supplierFastReturnEnabled = shopMetaData.isSupplierFastReturnEnabled();
        actualDeliveryRegionalSettings = shopMetaData.getActualDeliveryRegionalSettings();
        actualDeliveryRegionalCalculationRules = shopMetaData.getActualDeliveryRegionalCalculationRules();
        freeLiftingEnabled = shopMetaData.isFreeLiftingEnabled();
        cartRequestTurnedOff = shopMetaData.isCartRequestTurnedOff();
        deliveryReceiptNeedType = shopMetaData.getDeliveryReceiptNeedType();
        paymentControlEnabled = shopMetaData.isPaymentControlEnabled();
        prescriptionManagementSystem = shopMetaData.getPrescriptionManagementSystem();
        migrationMapping = shopMetaData.getMigrationMapping();
        medicineLicense = shopMetaData.getMedicineLicense();
        selfEmployed = shopMetaData.isSelfEmployed();
        return this;
    }

    public ShopMetaDataBuilder withBusinessId(long businessId) {
        this.businessId = businessId;
        return this;
    }

    public ShopMetaDataBuilder withCampaiginId(long campaignId) {
        this.campaignId = campaignId;
        return this;
    }

    public ShopMetaDataBuilder withClientId(long clientId) {
        this.clientId = clientId;
        return this;
    }

    public ShopMetaDataBuilder withSandboxClass(PaymentClass sandboxClass) {
        this.sandboxClass = sandboxClass;
        return this;
    }

    public ShopMetaDataBuilder withProdClass(PaymentClass prodClass) {
        this.prodClass = prodClass;
        return this;
    }

    public ShopMetaDataBuilder withYaMoneyId(String yaMoneyId) {
        this.yaMoneyId = yaMoneyId;
        return this;
    }

    public ShopMetaDataBuilder withArticles(PaymentArticle[] articles) {
        this.articles = articles;
        return this;
    }

    public ShopMetaDataBuilder withPrepayType(PrepayType prepayType) {
        this.prepayType = prepayType;
        return this;
    }

    public ShopMetaDataBuilder withInn(String inn) {
        this.inn = inn;
        return this;
    }

    public ShopMetaDataBuilder withPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public ShopMetaDataBuilder withOrderVisibilityMap(Map<OrderVisibility, Boolean> orderVisibilityMap) {
        this.orderVisibilityMap = orderVisibilityMap;
        return this;
    }

    public ShopMetaDataBuilder withAgencyCommission(Integer agencyCommission) {
        this.agencyCommission = agencyCommission;
        return this;
    }

    public ShopMetaDataBuilder withOgrn(String ogrn) {
        this.ogrn = ogrn;
        return this;
    }

    public ShopMetaDataBuilder withSupplierName(String supplierName) {
        this.supplierName = supplierName;
        return this;
    }

    public ShopMetaDataBuilder withOrderAutoAcceptEnabled(boolean orderAutoAcceptEnabled) {
        isOrderAutoAcceptEnabled = orderAutoAcceptEnabled;
        return this;
    }

    public ShopMetaDataBuilder withPushApiActualization(boolean pushApiActualization) {
        isPushApiActualization = pushApiActualization;
        return this;
    }

    public ShopMetaDataBuilder withPushApiActualizationRegional(boolean pushApiActualizationRegional) {
        isPushApiActualizationRegional = pushApiActualizationRegional;
        return this;
    }

    public ShopMetaDataBuilder withSupplierFastReturnEnabled(boolean supplierFastReturnEnabled) {
        this.supplierFastReturnEnabled = supplierFastReturnEnabled;
        return this;
    }

    public ShopMetaDataBuilder withActualDeliveryRegionalSettings(
            ShopActualDeliveryRegionalSettings[] actualDeliveryRegionalSettings) {
        this.actualDeliveryRegionalSettings = actualDeliveryRegionalSettings;
        return this;
    }

    public ShopMetaDataBuilder withActualDeliveryRegionalCalculationRule(
            List<ActualDeliveryRegionalCalculationRule> actualDeliveryRegionalCalculationRules
    ) {
        this.actualDeliveryRegionalCalculationRules = actualDeliveryRegionalCalculationRules;
        return this;
    }

    public ShopMetaDataBuilder withFreeLiftingEnabled(boolean freeLiftingEnabled) {
        this.freeLiftingEnabled = freeLiftingEnabled;
        return this;
    }

    public ShopMetaDataBuilder withCartRequestTurnedOff(boolean cartRequestTurnedOff) {
        this.cartRequestTurnedOff = cartRequestTurnedOff;
        return this;
    }

    public ShopMetaDataBuilder withDeliveryReceiptNeedType(DeliveryReceiptNeedType deliveryReceiptNeedType) {
        this.deliveryReceiptNeedType = deliveryReceiptNeedType;
        return this;
    }

    public ShopMetaDataBuilder withPaymentControlEnabled(boolean paymentControlEnabled) {
        this.paymentControlEnabled = paymentControlEnabled;
        return this;
    }

    public ShopMetaDataBuilder withPrescriptionManagementSystem(
            PrescriptionManagementSystem prescriptionManagementSystem) {
        this.prescriptionManagementSystem = prescriptionManagementSystem;
        return this;
    }

    public ShopMetaDataBuilder withMigrationMapping(MigrationMapping migrationMapping) {
        this.migrationMapping = migrationMapping;
        return this;
    }

    public ShopMetaDataBuilder withMedicineLicense(String medicineLicense) {
        this.medicineLicense = medicineLicense;
        return this;
    }

    public ShopMetaDataBuilder withSelfEmployed(boolean selfEmployed) {
        this.selfEmployed = selfEmployed;
        return this;
    }

    public ShopMetaData build() {
        return new ShopMetaData(
                businessId,
                campaignId,
                clientId,
                sandboxClass,
                prodClass,
                yaMoneyId,
                articles,
                prepayType,
                inn,
                phone,
                agencyCommission,
                orderVisibilityMap,
                ogrn,
                supplierName,
                isOrderAutoAcceptEnabled,
                isPushApiActualization,
                isPushApiActualizationRegional,
                supplierFastReturnEnabled,
                actualDeliveryRegionalSettings,
                actualDeliveryRegionalCalculationRules,
                freeLiftingEnabled,
                cartRequestTurnedOff,
                deliveryReceiptNeedType,
                paymentControlEnabled,
                prescriptionManagementSystem,
                migrationMapping,
                medicineLicense,
                selfEmployed);
    }
}
