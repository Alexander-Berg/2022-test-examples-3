package ru.yandex.chemodan.app.psbilling.core.promos;

import java.util.UUID;
import java.util.function.BiConsumer;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.billing.users.AbstractPaymentTest;
import ru.yandex.chemodan.app.psbilling.core.billing.users.processors.OrderProcessorFacade;
import ru.yandex.chemodan.app.psbilling.core.config.PsBillingCoreMocksConfig;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductSetDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PromoTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.UserPromoDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.products.BillingType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductSetEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationArea;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoStatusType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.UserPromoEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.mocks.PurchaseReportingServiceMockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.trust.client.responses.PaymentResponse;
import ru.yandex.chemodan.trust.client.responses.PaymentStatus;
import ru.yandex.chemodan.trust.client.responses.SubscriptionResponse;
import ru.yandex.chemodan.util.exception.A3ExceptionWithStatus;
import ru.yandex.misc.test.Assert;

import static org.mockito.ArgumentMatchers.any;

public class BuyWithPromoTests extends AbstractPaymentTest {
    @Autowired
    private OrderProcessorFacade processor;
    @Autowired
    private UserPromoDao userPromoDao;
    @Autowired
    private PromoTemplateDao promoTemplateDao;
    @Autowired
    private PromoService promoService;
    @Autowired
    PsBillingCoreMocksConfig psBillingCoreMocksConfig;

    private UserProductPrice regularProductPrice;
    private UserProductPrice promoProductPrice;
    private ProductLineEntity promoProductLine;
    private ProductSetEntity productSet;
    private UserProductEntity regularProduct;
    private UserProductEntity promoProduct;

    @Before
    public void init() {
        DateUtils.freezeTime();
        regularProduct = createProduct("REGULAR");
        promoProduct = createProduct("PROMO");

        productSet = productSetDao.create(ProductSetDao.InsertData.builder().key(UUID.randomUUID().toString()).build());

        ProductLineEntity availableProductLine = productLineDao.create(
                ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1).build());
        promoProductLine = productLineDao.create(
                ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1).build());

        productLineDao.bindUserProducts(availableProductLine.getId(),
                Cf.list(regularProduct.getId()));
        productLineDao.bindUserProducts(promoProductLine.getId(),
                Cf.list(regularProduct.getId(), promoProduct.getId()));

        psBillingTextsFactory.loadTranslations();
        regularProductPrice = userProductManager.findPrice(
                psBillingProductsFactory.createUserProductPrices(regularProduct, CustomPeriodUnit.TEN_MINUTES).getId());
        promoProductPrice = userProductManager.findPrice(
                psBillingProductsFactory.createUserProductPrices(promoProduct, CustomPeriodUnit.TEN_MINUTES).getId());

        initMocks();
    }

    @Test
    public void buyWithNoPromo() {
        // promos exist but product not bound to any. No used promo should occur

        UUID promoId;
        createActivePromo(PromoApplicationArea.GLOBAL, PromoApplicationType.ONE_TIME);
        createActivePromo(PromoApplicationArea.GLOBAL, PromoApplicationType.MULTIPLE_TIME);

        promoId = createActivePromo(PromoApplicationArea.PER_USER, PromoApplicationType.ONE_TIME).getId();
        promoService.activatePromoForUser(userId, promoId);

        promoId = createActivePromo(PromoApplicationArea.PER_USER, PromoApplicationType.MULTIPLE_TIME).getId();
        promoService.activatePromoForUser(userId, promoId);

        Assert.assertThrows(() -> buyProduct(promoProductPrice), A3ExceptionWithStatus.class);
        buyProduct(regularProductPrice);

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(2, userPromos.length());
        Assert.equals(2, userPromos.filter(UserPromoEntity::isActive).length());
    }

    @Test
    public void buyNotActivatedUserPromo() {
        UUID promoId = createActivePromo(PromoApplicationArea.PER_USER, PromoApplicationType.ONE_TIME).getId();
        promoTemplateDao.bindProductLines(promoId, promoProductLine.getId());

        Assert.assertThrows(() -> buyProduct(promoProductPrice), A3ExceptionWithStatus.class);
        buyProduct(regularProductPrice);

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(0, userPromos.length());
    }

    @Test
    public void buyWithGlobalPromo_OneTime() {
        UUID promoId = createActivePromo(PromoApplicationArea.GLOBAL, PromoApplicationType.ONE_TIME).getId();
        promoTemplateDao.bindProductLines(promoId, promoProductLine.getId());

        buyProduct(promoProductPrice);
        PurchaseReportingServiceMockConfiguration.assertLogLike(
                "action: buy_new, status: success, order_id: %uuid%, uid: %uid%, product_code: PROMO, period: 600S, " +
                        "price: 10, is_start_period: false, currency: RUB, new_expiration_date: %date%, " +
                        "active_promo: %uuid%"
        );

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(1, userPromos.length());
        Assert.assertFalse(userPromos.single().isActive());
        Assert.equals(PromoStatusType.USED, userPromos.single().getStatus());
    }

    @Test
    public void buyWithGlobalPromo_MultipleTime() {
        UUID promoId = createActivePromo(PromoApplicationArea.GLOBAL, PromoApplicationType.MULTIPLE_TIME).getId();
        promoTemplateDao.bindProductLines(promoId, promoProductLine.getId());

        buyProduct(promoProductPrice);
        PurchaseReportingServiceMockConfiguration.assertLogLike(
                "action: buy_new, status: success, order_id: %uuid%, uid: %uid%, product_code: PROMO, period: 600S, " +
                        "price: 10, is_start_period: false, currency: RUB, new_expiration_date: %date%, " +
                        "active_promo: %uuid%"
        );

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(0, userPromos.length());
    }

    @Test
    public void buyWithSeveralGlobalPromos() {
        UUID promoId1 = createPromo(DateUtils.pastDate(), DateUtils.futureDate(),
                PromoApplicationArea.GLOBAL, PromoApplicationType.ONE_TIME).getId();
        UUID promoId2 = createPromo(DateUtils.pastDate(), DateUtils.farFutureDate(),
                PromoApplicationArea.GLOBAL, PromoApplicationType.ONE_TIME).getId();
        promoTemplateDao.bindProductLines(promoId1, promoProductLine.getId());
        promoTemplateDao.bindProductLines(promoId2, promoProductLine.getId());

        buyProduct(promoProductPrice);

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(1, userPromos.length());
        Assert.assertFalse(userPromos.single().isActive());
        Assert.equals(promoId1, userPromos.single().getPromoTemplateId());
        Assert.equals(PromoStatusType.USED, userPromos.single().getStatus());
    }

    @Test
    public void buyWithUserPromo_OneTime() {
        UUID promoId = createActivePromo(PromoApplicationArea.PER_USER, PromoApplicationType.ONE_TIME).getId();
        promoTemplateDao.bindProductLines(promoId, promoProductLine.getId());
        promoService.activatePromoForUser(userId, promoId);

        buyProduct(promoProductPrice);
        PurchaseReportingServiceMockConfiguration.assertLogLike(
                "action: buy_new, status: success, order_id: %uuid%, uid: %uid%, product_code: PROMO, period: 600S, " +
                        "price: 10, is_start_period: false, currency: RUB, new_expiration_date: %date%, " +
                        "active_promo: %uuid%"
        );

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(1, userPromos.length());
        Assert.assertFalse(userPromos.single().isActive());
        Assert.equals(PromoStatusType.USED, userPromos.single().getStatus());
    }

    @Test
    public void buyWithUserPromo_MultipleTime() {
        UUID promoId = createActivePromo(PromoApplicationArea.PER_USER, PromoApplicationType.MULTIPLE_TIME).getId();
        promoTemplateDao.bindProductLines(promoId, promoProductLine.getId());
        promoService.activatePromoForUser(userId, promoId);

        buyProduct(promoProductPrice);
        PurchaseReportingServiceMockConfiguration.assertLogLike(
                "action: buy_new, status: success, order_id: %uuid%, uid: %uid%, product_code: PROMO, period: 600S, " +
                        "price: 10, is_start_period: false, currency: RUB, new_expiration_date: %date%, " +
                        "active_promo: %uuid%"
        );

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(1, userPromos.length());
        Assert.assertTrue(userPromos.single().isActive());
    }

    @Test
    public void buyWithUserPromo_MultipleTime_ignoresPromoOrder() {
        UUID promoId = createPromo(DateUtils.pastDate(), DateUtils.farFutureDate(), PromoApplicationArea.GLOBAL,
                PromoApplicationType.MULTIPLE_TIME).getId();
        promoTemplateDao.bindProductLines(promoId, promoProductLine.getId());
        promoId = createPromo(DateUtils.pastDate(), DateUtils.futureDate(), PromoApplicationArea.GLOBAL,
                PromoApplicationType.ONE_TIME).getId();
        promoTemplateDao.bindProductLines(promoId, promoProductLine.getId());

        buyProduct(promoProductPrice);

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(0, userPromos.length());
    }

    @Test
    public void buyWithSeveralUserPromos() {
        UUID promoId1 = createPromo(DateUtils.pastDate(), DateUtils.futureDate(),
                PromoApplicationArea.PER_USER, PromoApplicationType.ONE_TIME).getId();
        UUID promoId2 = createPromo(DateUtils.pastDate(), null,
                PromoApplicationArea.PER_USER, PromoApplicationType.ONE_TIME).getId();
        promoTemplateDao.bindProductLines(promoId1, promoProductLine.getId());
        promoTemplateDao.bindProductLines(promoId2, promoProductLine.getId());
        promoService.activatePromoForUser(userId, promoId1);
        promoService.activatePromoForUser(userId, promoId2);

        buyProduct(promoProductPrice);

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(2, userPromos.length());
        UserPromoEntity activeUserPromo = userPromos.filter(UserPromoEntity::isActive).single();
        UserPromoEntity usedUserPromo = userPromos.filter(p -> !p.isActive()).single();

        Assert.equals(promoId1, usedUserPromo.getPromoTemplateId());
        Assert.equals(PromoStatusType.USED, usedUserPromo.getStatus());
        Assert.equals(promoId2, activeUserPromo.getPromoTemplateId());
    }

    @Test
    public void buySeveralTimes() {
        UUID promoId1 = createPromo(DateUtils.pastDate(), DateUtils.futureDate(),
                PromoApplicationArea.GLOBAL, PromoApplicationType.ONE_TIME).getId();
        UUID promoId2 = createPromo(DateUtils.pastDate(), DateUtils.farFutureDate(),
                PromoApplicationArea.PER_USER, PromoApplicationType.ONE_TIME).getId();
        UUID promoId3 = createPromo(DateUtils.pastDate(), null,
                PromoApplicationArea.PER_USER, PromoApplicationType.ONE_TIME).getId();
        promoTemplateDao.bindProductLines(promoId1, promoProductLine.getId());
        promoTemplateDao.bindProductLines(promoId2, promoProductLine.getId());
        promoTemplateDao.bindProductLines(promoId3, promoProductLine.getId());
        promoService.activatePromoForUser(userId, promoId2);
        promoService.activatePromoForUser(userId, promoId3);

        BiConsumer<Integer, Integer> checkUserPromos = (Integer userPromosCount, Integer activeUserPromosCount) ->
        {
            ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
            Assert.equals(userPromosCount, userPromos.length());
            Assert.equals(activeUserPromosCount, userPromos.filter(UserPromoEntity::isActive).length());
        };

        checkUserPromos.accept(2, 2); // 2 active user promos

        buyProduct(promoProductPrice);
        checkUserPromos.accept(3, 2); // + used global promo

        buyProduct(promoProductPrice);
        checkUserPromos.accept(3, 1); // - used user promo

        buyProduct(promoProductPrice);
        checkUserPromos.accept(3, 0); // - used user promo

        Assert.assertThrows(() -> buyProduct(promoProductPrice), A3ExceptionWithStatus.class);
    }

    @Test
    public void buyWithExpiredPromos() {
        UUID promoId1 = createPromo(DateUtils.pastDate(), DateUtils.futureDate(),
                PromoApplicationArea.GLOBAL, PromoApplicationType.ONE_TIME).getId();
        UUID promoId2 = createPromo(DateUtils.pastDate(), DateUtils.futureDate(),
                PromoApplicationArea.PER_USER, PromoApplicationType.ONE_TIME).getId();
        promoTemplateDao.bindProductLines(promoId1, promoProductLine.getId());
        promoTemplateDao.bindProductLines(promoId2, promoProductLine.getId());
        promoService.activatePromoForUser(userId, promoId2);

        DateUtils.freezeTime(DateUtils.farFutureDate());
        Assert.assertThrows(() -> buyProduct(promoProductPrice), A3ExceptionWithStatus.class);
        buyProduct(regularProductPrice);

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(1, userPromos.length());
        Assert.assertFalse(userPromos.single().isActive());
        Assert.equals(PromoStatusType.ACTIVE, userPromos.single().getStatus());
    }

    @Test
    public void productLineOrderMatters() {
        ProductLineEntity promoProductLineZeroOrder = productLineDao.create(
                ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(0).build());
        productLineDao.bindUserProducts(promoProductLineZeroOrder.getId(), Cf.list(promoProduct.getId()));

        UUID promoId1 = createPromo(DateUtils.pastDate(), DateUtils.farFutureDate(),
                PromoApplicationArea.GLOBAL, PromoApplicationType.ONE_TIME).getId();
        promoTemplateDao.bindProductLines(promoId1, promoProductLineZeroOrder.getId());

        UUID promoId2 = createPromo(DateUtils.pastDate(), DateUtils.futureDate(),
                PromoApplicationArea.GLOBAL, PromoApplicationType.ONE_TIME).getId();
        promoTemplateDao.bindProductLines(promoId2, promoProductLine.getId());

        buyProduct(promoProductPrice);

        UserPromoEntity userPromo = userPromoDao.findUserPromos(userId).single();
        Assert.assertFalse(userPromo.isActive());
        Assert.equals(PromoStatusType.USED, userPromo.getStatus());
        Assert.equals(promoId1, userPromo.getPromoTemplateId());
    }

    @Test
    public void productLineSelectorMatters() {
        ProductLineEntity notAvailableProductLine = productLineDao.create(
                ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(0)
                        .selectorBeanEL("productLineSelectorFactory.unavailableSelector()").build());
        productLineDao.bindUserProducts(notAvailableProductLine.getId(), Cf.list(regularProduct.getId()));

        UUID promoId1 = createPromo(DateUtils.pastDate(), DateUtils.futureDate(),
                PromoApplicationArea.GLOBAL, PromoApplicationType.ONE_TIME).getId();
        promoTemplateDao.bindProductLines(promoId1, notAvailableProductLine.getId());

        UUID promoId2 = createPromo(DateUtils.pastDate(), DateUtils.farFutureDate(),
                PromoApplicationArea.GLOBAL, PromoApplicationType.ONE_TIME).getId();
        promoTemplateDao.bindProductLines(promoId2, promoProductLine.getId());

        buyProduct(promoProductPrice);

        UserPromoEntity userPromo = userPromoDao.findUserPromos(userId).single();
        Assert.assertFalse(userPromo.isActive());
        Assert.equals(PromoStatusType.USED, userPromo.getStatus());
        Assert.equals(promoId2, userPromo.getPromoTemplateId());
    }

    @Test
    public void buyingCommonProductDoNotUsesPromo() {
        UUID promoId = createActivePromo(PromoApplicationArea.PER_USER, PromoApplicationType.ONE_TIME).getId();
        promoTemplateDao.bindProductLines(promoId, promoProductLine.getId());
        promoService.activatePromoForUser(userId, promoId);

        buyProduct(regularProductPrice);

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(1, userPromos.length());
        Assert.assertTrue(userPromos.single().isActive());
        Assert.equals(PromoStatusType.ACTIVE, userPromos.single().getStatus());
    }

    private void buyProduct(UserProductPrice price) {
        initStubs(price);
        initMocks();
        Order order = createOrder(price);
        processor.processByOrderId(order.getId());
    }

    private void initMocks() {
        Mockito.when(trustClient.getSubscription(any())).thenAnswer(invocation ->
                SubscriptionResponse.builder()
                        .subscriptionUntil(Instant.now().plus(Duration.standardDays(30)))
                        .paymentIds(Cf.list(UUID.randomUUID().toString()))
                        .subscriptionPeriodCount(1)
                        .subscriptionState(3)
                        .currentAmount(new String[][]{{"10", "RUB"}})
                        .build());
        Mockito.when(trustClient.getPayment(any())).thenAnswer(invocation ->
                PaymentResponse.builder()
                        .paymentStatus(PaymentStatus.cleared)
                        .build());
        textsManagerMockConfig.turnMockOn();
    }

    private PromoTemplateEntity createActivePromo(PromoApplicationArea area, PromoApplicationType type) {
        return createPromo(DateUtils.pastDate(), null, area, type);
    }

    private PromoTemplateEntity createPromo(Instant from, Instant to, PromoApplicationArea area,
                                            PromoApplicationType type) {
        PromoTemplateDao.InsertData.InsertDataBuilder promoTemplateData
                = PromoTemplateDao.InsertData.builder()
                .fromDate(from == null ? Instant.now() : from)
                .toDate(Option.ofNullable(to))
                .description("description")
                .code(UUID.randomUUID().toString())
                .applicationArea(area)
                .applicationType(type);
        return promoTemplateDao.create(promoTemplateData.build());
    }

    private UserProductEntity createProduct(String billingCode) {
        return psBillingProductsFactory.createUserProduct(builder -> {
            builder.billingType(BillingType.TRUST);
            builder.code(billingCode);
            builder.allowAutoProlong(true);
            builder.titleTankerKeyId(psBillingTextsFactory.create(UUID.randomUUID().toString()).getId());
            builder.singleton(false);
            builder.trustServiceId(Option.of(111));
            builder.trustSubsChargingRetryDelay(Option.of("1D"));
            builder.trustSubsChargingRetryLimit(Option.of("2D"));
            builder.trustSubsGracePeriod(Option.of("2D"));

            return builder;
        });
    }
}
