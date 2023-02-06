package ru.yandex.chemodan.app.psbilling.core.products;

import java.util.UUID;

import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Try;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.TrialUsageDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductSetDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PromoTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.UserPromoDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriod;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.TrialDefinitionEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductSetEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPriceEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationArea;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoStatusType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.promos.PromoService;
import ru.yandex.chemodan.app.psbilling.core.promos.PromoTemplate;
import ru.yandex.chemodan.app.psbilling.core.tasks.policies.promo.PromoActivationPreExecutionPolicy;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.core.utils.PromoHelper;
import ru.yandex.chemodan.app.uaas.experiments.ExperimentsManager;
import ru.yandex.chemodan.util.exception.A3ExceptionWithStatus;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class UserProductManagerTest extends AbstractPsBillingCoreTest {
    private PassportUid uid = PassportUid.MAX_VALUE;
    private Option<PassportUid> uidO = Option.of(uid);

    @Autowired
    private ProductLineDao productLineDao;
    @Autowired
    private ProductSetDao productSetDao;
    @Autowired
    private ExperimentsManager experimentsManager;
    @Autowired
    private PromoTemplateDao promoTemplateDao;
    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoActivationPreExecutionPolicy promoActivationPreExecutionPolicy;
    @Autowired
    private UserPromoDao userPromoDao;
    @Autowired
    private PromoHelper promoHelper;
    @Autowired
    private TrialUsageDao trialUsageDao;

    @Test
    public void validatePrice() {
        UserProductEntity availableProduct = psBillingProductsFactory.createUserProduct();
        UserProductEntity unavailableProduct = psBillingProductsFactory.createUserProduct();

        UserProductPriceEntity availablePrice =
                psBillingProductsFactory.createUserProductPrices(availableProduct, CustomPeriodUnit.ONE_DAY);
        UserProductPriceEntity unavailablePrice =
                psBillingProductsFactory.createUserProductPrices(unavailableProduct, CustomPeriodUnit.ONE_DAY);

        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());
        ProductLineEntity availableProductLine = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1).build());

        // the following product line is unavailable because we currently always choose the first available product
        // line in set
        // see ProductsService.selectProductLine
        ProductLineEntity unavailableProductLine = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(2).build());

        productLineDao.bindUserProducts(availableProductLine.getId(), Cf.list(availableProduct.getId()));
        productLineDao.bindUserProducts(unavailableProductLine.getId(), Cf.list(unavailableProduct.getId()));

        userProductManager.validatePrice(uid, userProductManager.findPrice(availablePrice.getId()));

        Assert.failure(Try.tryCatchThrowable(() -> {
            userProductManager.validatePrice(uid, userProductManager.findPrice(unavailablePrice.getId()));
            return true;
        }), A3ExceptionWithStatus.class);
    }

    @Test
    @SuppressWarnings("unused")
    public void testProductLineAvailableSelectors() {
        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());
        ProductLineEntity availableProductLine = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1)
                        .selectorBeanEL("productLineSelectorFactory.availableSelector()").build());
        ProductLineEntity defaultAvailableProductLine = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(2).build());

        Option<AbstractProductManager.LimitedTimeProductLine> limitedTimeProductLine =
                userProductManager.selectProductLine(productSet.getId(), uidO);
        Option<ProductLineEntity> productLine = limitedTimeProductLine
                .map(AbstractProductManager.LimitedTimeProductLine::getProductLine);
        Assert.assertSome(productLine);
        ProductLineEntity lineSelected = productLine.get();
        Assert.equals(availableProductLine.getId(), lineSelected.getId());

        Option<PromoTemplate> promoTemplate =
                limitedTimeProductLine.flatMapO(AbstractProductManager.LimitedTimeProductLine::getPromo);
        Assert.assertEmpty(promoTemplate);
    }

    @Test
    @SuppressWarnings("unused")
    public void testProductLineByAgeSelectors() {
        PassportUid uid = PassportUid.cons(3000185708L);
        DateUtils.freezeTime(Instant.parse("2013-11-23"));

        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());
        ProductLineEntity availableProductLine = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1)
                        .selectorBeanEL(
                                "productLineSelectorFactory.regDateOlderThanSelector(T(org.joda.time.Period).years(1))")
                        .build());
        ProductLineEntity availableByDefaultProductLine = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(2).build());

        Option<ProductLineEntity> productLine = userProductManager.selectProductLine(productSet.getId(), Option.of(uid))
                .map(AbstractProductManager.LimitedTimeProductLine::getProductLine);
        Assert.assertSome(productLine);
        ProductLineEntity lineSelected = productLine.get();
        Assert.equals(availableProductLine.getId(), lineSelected.getId());
    }

    @Test
    @SuppressWarnings("unused")
    public void testProductLineExperimentSelectors() {
        String experiment = "mail_pro_new_year_2021";
        DateTimeUtils.setCurrentMillisFixed(LocalDateTime.parse("2021-01-31").toDate().getTime());

        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());

        ProductLineEntity availableProductLine = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1)
                        .selectorBeanEL(
                                String.format(
                                        "productLineSelectorFactory.availableFromSelector(\"2020-11-20\", " +
                                                "productLineSelectorFactory.availableUntilSelector(\"2021-02-01\", " +
                                                "productLineSelectorFactory.experimentIsActiveSelector(\"%s\")))",
                                        experiment))
                        .build());
        ProductLineEntity availableByDefaultProductLine = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(2).build());


        Mockito.when(experimentsManager.getFlags(Mockito.anyLong())).thenReturn(Cf.list(experiment));

        Option<ProductLineEntity> productLine = userProductManager.selectProductLine(productSet.getId(), uidO)
                .map(AbstractProductManager.LimitedTimeProductLine::getProductLine);
        Assert.assertSome(productLine);
        ProductLineEntity lineSelected = productLine.get();
        Assert.equals(availableProductLine.getId(), lineSelected.getId());
    }

    @Test
    @SuppressWarnings("unused")
    public void testProductLineTrialSelectors() {
        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());
        ProductLineEntity availableProductLine = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1)
                        .selectorBeanEL("productLineSelectorFactory.onlyIfTrialNotUsed(\"mail_pro_b2c_trial_key\", " +
                                "productLineSelectorFactory.availableSelector())")
                        .build());
        TrialDefinitionEntity trial =
                psBillingProductsFactory.createTrialDefinitionUtilDate(DateUtils.farFutureDate(),
                        x -> x.singleUsageComparisonKey(Option.of("mail_pro_b2c_trial_key")));
        UserProductEntity trialProduct =
                psBillingProductsFactory.createUserProduct(x -> x.trialDefinitionId(Option.of(trial.getId())));

        Option<ProductLineEntity> productLine = userProductManager.selectProductLine(productSet.getId(), Option.of(uid))
                .map(AbstractProductManager.LimitedTimeProductLine::getProductLine);
        Assert.assertSome(productLine);

        trialUsageDao.insert(TrialUsageDao.InsertData.builder()
                .activatedByUid(Option.of(uid)).trialDefinitionId(trial.getId()).build());
        productLine = userProductManager.selectProductLine(productSet.getId(), Option.of(uid))
                .map(AbstractProductManager.LimitedTimeProductLine::getProductLine);
        Assert.assertNone(productLine);
    }

    @Test
    @SuppressWarnings("unused")
    public void testProductLines_Promos_Global() {
        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());

        ProductLineEntity availableProductLine = createProductLine(productSet, 1);
        ProductLineEntity defaultAvailableProductLine = createProductLine(productSet, 2);
        ProductLineEntity promoProductLine = createProductLine(productSet, 3);

        UUID promoId = promoHelper.createPromo(PromoApplicationArea.GLOBAL).getId();
        promoTemplateDao.bindProductLines(promoId, promoProductLine.getId());
        Option<AbstractProductManager.LimitedTimeProductLine> limitedTimeProductLine =
                userProductManager.selectProductLine(productSet.getId(), uidO);
        Option<ProductLineEntity> productLine = limitedTimeProductLine
                .map(AbstractProductManager.LimitedTimeProductLine::getProductLine);
        Assert.assertSome(productLine);
        ProductLineEntity lineSelected = productLine.get();
        Assert.equals(promoProductLine.getId(), lineSelected.getId());

        Option<PromoTemplate> promoTemplateO =
                limitedTimeProductLine.flatMapO(AbstractProductManager.LimitedTimeProductLine::getPromo);
        Assert.assertSome(promoTemplateO);
        PromoTemplate promoTemplate = promoTemplateO.get();
        Assert.equals(promoId, promoTemplate.getId());
    }

    @Test
    @SuppressWarnings("unused")
    public void testProductLines_Promos_UsedGlobal() {
        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());

        ProductLineEntity globalPromoProductLine = createProductLine(productSet, 1);
        ProductLineEntity userPromoProductLine = createProductLine(productSet, 3);

        UUID globalPromoId = promoHelper.createPromo(PromoApplicationArea.GLOBAL).getId();
        UUID userPromoId = promoHelper.createPromo(PromoApplicationArea.PER_USER).getId();
        promoTemplateDao.bindProductLines(globalPromoId, globalPromoProductLine.getId());
        promoTemplateDao.bindProductLines(userPromoId, userPromoProductLine.getId());
        promoService.activatePromoForUser(uid, userPromoId);

        AbstractProductManager.LimitedTimeProductLine productLine =
                userProductManager.selectProductLine(productSet.getId(), uidO).get();
        Assert.equals(globalPromoProductLine.getId(), productLine.getProductLine().getId());
        userPromoDao.createOrUpdate(UserPromoDao.InsertData.builder()
                .promoTemplateId(globalPromoId)
                .fromDate(DateUtils.pastDate())
                .uid(uid)
                .promoStatusType(PromoStatusType.USED).build());

        productLine = userProductManager.selectProductLine(productSet.getId(), uidO).get();
        Assert.equals(userPromoProductLine.getId(), productLine.getProductLine().getId());
    }

    @Test
    @SuppressWarnings("unused")
    public void testProductLines_Promos_PerUser() {
        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());

        ProductLineEntity availableProductLine = createProductLine(productSet, 1);
        ProductLineEntity defaultAvailableProductLine = createProductLine(productSet, 2);
        ProductLineEntity promoProductLine = createProductLine(productSet, 3);

        PromoTemplateEntity promo = promoHelper.createPromo(PromoApplicationArea.PER_USER);
        promoTemplateDao.bindProductLines(promo.getId(), promoProductLine.getId());
        promoService.activatePromoForUser(uid, promo.getId());

        Option<AbstractProductManager.LimitedTimeProductLine> limitedTimeProductLine =
                userProductManager.selectProductLine(productSet.getId(), uidO);
        Option<ProductLineEntity> productLine = limitedTimeProductLine
                .map(AbstractProductManager.LimitedTimeProductLine::getProductLine);
        Assert.assertSome(productLine);
        ProductLineEntity lineSelected = productLine.get();
        Assert.equals(promoProductLine.getId(), lineSelected.getId());

        Option<PromoTemplate> promoTemplateO =
                limitedTimeProductLine.flatMapO(AbstractProductManager.LimitedTimeProductLine::getPromo);
        Assert.assertSome(promoTemplateO);
        PromoTemplate promoTemplate = promoTemplateO.get();
        Assert.equals(promo.getId(), promoTemplate.getId());
    }

    @Test
    @SuppressWarnings("unused")
    public void testProductLines_Promos_OrderMatters() {
        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());

        ProductLineEntity promoProductLine1 = createProductLine(productSet, 3);
        ProductLineEntity promoProductLine2 = createProductLine(productSet, 4);
        ProductLineEntity availableProductLine = createProductLine(productSet, 1);
        ProductLineEntity defaultAvailableProductLine = createProductLine(productSet, 2);

        UUID promoId = promoHelper.createPromo(PromoApplicationArea.GLOBAL).getId();
        promoTemplateDao.bindProductLines(promoId, promoProductLine1.getId());
        promoTemplateDao.bindProductLines(promoId, promoProductLine2.getId());

        Option<ProductLineEntity> productLine = userProductManager.selectProductLine(productSet.getId(), uidO)
                .map(AbstractProductManager.LimitedTimeProductLine::getProductLine);
        Assert.assertSome(productLine);
        ProductLineEntity lineSelected = productLine.get();
        Assert.equals(promoProductLine1.getId(), lineSelected.getId());
    }

    @Test
    @SuppressWarnings("unused")
    public void testProductLines_Promos_PromoWithNoProductLine() {
        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());

        ProductLineEntity availableProductLine = createProductLine(productSet, 1);
        ProductLineEntity defaultAvailableProductLine = createProductLine(productSet, 2);
        ProductLineEntity promoProductLine = createProductLine(productSet, 3);

        UUID promoId = promoHelper.createPromo(PromoApplicationArea.GLOBAL).getId();

        Option<ProductLineEntity> productLine = userProductManager.selectProductLine(productSet.getId(), uidO)
                .map(AbstractProductManager.LimitedTimeProductLine::getProductLine);
        Assert.assertSome(productLine);
        ProductLineEntity lineSelected = productLine.get();
        Assert.equals(availableProductLine.getId(), lineSelected.getId());
    }

    @Test
    @SuppressWarnings("unused")
    public void testProductLines_Promos_NotActive() {
        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());

        ProductLineEntity availableProductLine = createProductLine(productSet, 1);
        ProductLineEntity defaultAvailableProductLine = createProductLine(productSet, 2);
        ProductLineEntity globalPromoProductLine = createProductLine(productSet, 3);
        ProductLineEntity userPromoProductLine = createProductLine(productSet, 4);

        PromoTemplateEntity globalPromo = promoHelper.createPromo(PromoApplicationArea.GLOBAL,
                b1 -> b1.fromDate(DateUtils.futureDate()));
        Assert.isFalse(globalPromo.isActive());
        promoTemplateDao.bindProductLines(globalPromo.getId(), globalPromoProductLine.getId());

        PromoTemplateEntity userPromo = promoHelper.createPromo(PromoApplicationArea.PER_USER,
                b -> b.fromDate(DateUtils.futureDate()));
        Assert.isFalse(userPromo.isActive());
        promoTemplateDao.bindProductLines(userPromo.getId(), userPromoProductLine.getId());
        promoService.activatePromoForUser(uid, userPromo.getId());

        Option<ProductLineEntity> productLine = userProductManager.selectProductLine(productSet.getId(), uidO)
                .map(AbstractProductManager.LimitedTimeProductLine::getProductLine);
        Assert.assertSome(productLine);
        ProductLineEntity lineSelected = productLine.get();
        Assert.equals(availableProductLine.getId(), lineSelected.getId());
    }

    @Test
    public void testProductLines_Promos_ToDate_Global() {
        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());
        ProductLineEntity globalPromoProductLine = createProductLine(productSet, 2);
        PromoTemplateEntity globalPromo = promoHelper.createGlobalPromo(x ->
                x.toDate(Option.of(DateUtils.futureDate())));
        promoTemplateDao.bindProductLines(globalPromo.getId(), globalPromoProductLine.getId());

        Option<AbstractProductManager.LimitedTimeProductLine> productLine =
                userProductManager.selectProductLine(productSet.getId(), uidO);
        Assert.assertSome(productLine);
        Assert.equals(globalPromoProductLine.getId(), productLine.get().getProductLine().getId());
        Assert.equals(DateUtils.futureDate(), productLine.get().getAvailableUntil().get());
    }

    @Test
    public void testProductLines_Promos_ToDate_User() {
        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());
        ProductLineEntity userPromoProductLine = createProductLine(productSet, 1);
        PromoTemplateEntity userPromo =
                promoHelper.createUserPromo(x -> x.duration(Option.of(CustomPeriod.fromDays(1))));
        promoTemplateDao.bindProductLines(userPromo.getId(), userPromoProductLine.getId());
        promoService.activatePromoForUser(uid, userPromo.getId());
        Option<Instant> userPromoToDate = userPromoDao.findUserPromo(uid, userPromo.getId()).get().getToDate();

        AbstractProductManager.LimitedTimeProductLine productLine =
                userProductManager.selectProductLine(productSet.getId(), uidO).get();
        Assert.equals(userPromoToDate.get(), productLine.getAvailableUntil().get());
    }

    @Test
    public void testProductLines_Promos_ToDate_SameLines() {
        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());
        ProductLineEntity userPromoProductLine = createProductLine(productSet, 1);

        PromoTemplateEntity userPromo0 =
                promoHelper.createUserPromo(x -> x.duration(Option.empty()));
        PromoTemplateEntity userPromo1 =
                promoHelper.createUserPromo(x -> x.duration(Option.of(CustomPeriod.fromDays(1))));
        PromoTemplateEntity userPromo2 =
                promoHelper.createUserPromo(x -> x.duration(Option.of(CustomPeriod.fromDays(2))));
        Cf.list(userPromo0, userPromo1, userPromo2).forEach(userPromo ->
                promoTemplateDao.bindProductLines(userPromo.getId(), userPromoProductLine.getId()));

        // нет даты у акции без даты конца
        promoService.activatePromoForUser(uid, userPromo0.getId());
        AbstractProductManager.LimitedTimeProductLine productLine
                = userProductManager.selectProductLine(productSet.getId(), uidO).get();
        Assert.assertFalse(productLine.getAvailableUntil().isPresent());

        // появилась акция с датой конца - она будет видна
        promoService.activatePromoForUser(uid, userPromo2.getId());
        productLine = userProductManager.selectProductLine(productSet.getId(), uidO).get();
        Option<Instant> userPromoToDate = userPromoDao.findUserPromo(uid, userPromo2.getId()).get().getToDate();
        Assert.equals(userPromoToDate, productLine.getAvailableUntil());

        // появилась акция с более близкой датой конца - она будет видна
        promoService.activatePromoForUser(uid, userPromo1.getId());
        productLine = userProductManager.selectProductLine(productSet.getId(), uidO).get();
        userPromoToDate = userPromoDao.findUserPromo(uid, userPromo1.getId()).get().getToDate();
        Assert.equals(userPromoToDate, productLine.getAvailableUntil());

    }

    @Test
    @SuppressWarnings("unused")
    public void testProductLines_Promos_ToDate_LineAndPromo() {
        ProductSetEntity productSet = productSetDao.create(ProductSetDao.InsertData.builder().key("111").build());
        ProductLineEntity userPromoProductLine = createProductLine(productSet, 1,
                Option.of(Instant.now().plus(Duration.standardDays(1))));

        PromoTemplateEntity userPromo =
                promoHelper.createUserPromo(x -> x.duration(Option.of(CustomPeriod.fromDays(2))));
        promoTemplateDao.bindProductLines(userPromo.getId(), userPromoProductLine.getId());
        promoService.activatePromoForUser(uid, userPromo.getId());

        AbstractProductManager.LimitedTimeProductLine productLine =
                userProductManager.selectProductLine(productSet.getId(), uidO).get();
        Assert.equals(Instant.now().plus(Duration.standardDays(1)), productLine.getAvailableUntil().get());
    }

    @Test
    @SuppressWarnings("unused")
    public void testProductLines_Promos_activateWebNewbieWhileMobileGlobalActive() {
        PromoTemplateEntity newbiePromo = promoHelper.createUserPromo(x -> x.code("newbie"));
        PromoTemplateEntity globalMobilePromo = promoHelper.createGlobalPromo(x -> x.code("septemer_mobile_30"));

        UserProductEntity defaultWebProduct = psBillingProductsFactory.createUserProduct(x -> x.code("web"));
        UserProductEntity discountWebProduct = psBillingProductsFactory.createUserProduct(x -> x.code("web -20%"));
        UserProductEntity defaultMobileProduct = psBillingProductsFactory.createUserProduct(x -> x.code("mobile"));
        UserProductEntity discountMobileProduct = psBillingProductsFactory.createUserProduct(x -> x.code("mobile-20%"));

        ProductLineEntity defaultWebLine =
                psBillingProductsFactory.createProductLine("web", x -> x, defaultWebProduct);
        ProductLineEntity discountWebLine =
                psBillingProductsFactory.createProductLine("web", x -> x, discountWebProduct);
        ProductLineEntity defaultMobileLine =
                psBillingProductsFactory.createProductLine("mobile", x -> x, defaultMobileProduct);
        ProductLineEntity discountMobileLine =
                psBillingProductsFactory.createProductLine("mobile", x -> x, discountMobileProduct);

        promoTemplateDao.bindProductLines(newbiePromo.getId(), discountWebLine.getId());
        promoTemplateDao.bindProductLines(globalMobilePromo.getId(), discountMobileLine.getId());

        // for web - default web
        LimitedTimeUserProducts userProducts = userProductManager.findProductSet("web", uidO);
        UserProduct product = userProducts.getUserProducts().single();
        Assert.equals(defaultWebProduct.getCode(), product.getCode());

        // for mobile - discount mobile
        userProducts = userProductManager.findProductSet("mobile", uidO);
        product = userProducts.getUserProducts().single();
        Assert.equals(discountMobileProduct.getCode(), product.getCode());

        // can activate promo for web although mobile global promo exist
        boolean shouldActivate = promoActivationPreExecutionPolicy.shouldActivate(uid, "newbie", false);
        Assert.isTrue(shouldActivate);

        boolean activated = promoService.activatePromoForUser(uid, newbiePromo.getId());
        Assert.isTrue(activated);

        userProducts = userProductManager.findProductSet("web", uidO);
        product = userProducts.getUserProducts().single();
        Assert.equals(discountWebProduct.getCode(), product.getCode());
    }

    @Before
    public void Setup() {
        DateTimeZone.setDefault(DateTimeZone.forOffsetHours(0));
        DateUtils.freezeTime(Instant.parse("2020-10-21"));
    }

    private ProductLineEntity createProductLine(ProductSetEntity productSet, Integer orderNum) {
        return createProductLine(productSet, orderNum, Option.empty());
    }

    private ProductLineEntity createProductLine(ProductSetEntity productSet, Integer orderNum,
                                                Option<Instant> availableUntil) {
        String selectorBean = "productLineSelectorFactory.availableSelector()";
        if (availableUntil.isPresent()) {
            selectorBean = "productLineSelectorFactory.availableUntilSelector(\""
                    + availableUntil.get().toString(DateTimeFormat.forPattern("yyyy-MM-dd")) + "\", "
                    + selectorBean + ")";
        }
        ProductLineDao.InsertData data = ProductLineDao.InsertData.builder()
                .productSetId(productSet.getId())
                .orderNum(orderNum)
                .selectorBeanEL(selectorBean).build();

        return productLineDao.create(data);
    }
}
