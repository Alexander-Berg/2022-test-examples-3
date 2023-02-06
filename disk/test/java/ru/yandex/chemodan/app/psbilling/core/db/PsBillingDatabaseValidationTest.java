package ru.yandex.chemodan.app.psbilling.core.db;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingDBTest;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.BucketContentDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupProductDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.FeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductPeriodDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductPricesDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PromoPayloadDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriod;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.BillingType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductFeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPeriodEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPriceEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoPayloadEntity;
import ru.yandex.chemodan.app.psbilling.core.mocks.TextsManagerMockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.products.SpringExpressionEvaluator;
import ru.yandex.chemodan.app.psbilling.core.products.UserProduct;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductFeature;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductManager;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPeriod;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice;
import ru.yandex.chemodan.app.psbilling.core.products.selectors.ProductLineSelector;
import ru.yandex.chemodan.app.psbilling.core.promos.PromoPayloadParser;
import ru.yandex.chemodan.app.psbilling.core.texts.TextsManager;
import ru.yandex.chemodan.test.DatabaseValidationUtils;
import ru.yandex.inside.utils.Language;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.assertNotNull;
import static ru.yandex.chemodan.app.psbilling.core.products.UserProductFeatureRegistry.MPFS_SPACE_FEATURE_CODE;
import static ru.yandex.misc.test.Assert.assertNotEmpty;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class PsBillingDatabaseValidationTest extends AbstractPsBillingDBTest {
    @Autowired
    private JdbcTemplate3 jdbcTemplate;
    @Autowired
    private ProductLineDao productLineDao;
    @Autowired
    private PromoPayloadDao promoPayloadDao;
    @Autowired
    private SpringExpressionEvaluator springExpressionEvaluator;
    @Autowired
    private PromoPayloadParser promoPayloadParser;
    @Autowired
    private UserProductPricesDao userProductPricesDao;
    @Autowired
    private UserProductPeriodDao userProductPeriodDao;
    @Autowired
    private UserProductManager userProductManager;
    @Autowired
    private UserProductDao userProductDao;
    @Autowired
    private GroupProductDao groupProductDao;
    @Autowired
    private BucketContentDao bucketContentDao;
    @Autowired
    private FeatureDao featureDao;
    @Autowired
    private ProductFeatureDao productFeatureDao;
    @Autowired
    TextsManagerMockConfiguration textsManagerMockConfiguration;
    @Autowired
    TextsManager textsManager;

    @Test
    public void everyForeignKeyShouldHaveIndex() {
        DatabaseValidationUtils.checkIndexesExistsForAllFK(jdbcTemplate, Cf.set());
    }

    @Test
    public void testSelectors() {
        ListF<ProductLineEntity> productLines = productLineDao.findAll();
        assertNotEmpty(productLines);

        for (ProductLineEntity productLine : productLines) {
            String expression = productLine.getSelectorBeanEL();
            ProductLineSelector result = springExpressionEvaluator.evaluateExpression(expression,
                    ProductLineSelector.class);
            assertNotNull("null evaluation result for expression " + expression, result);
        }
    }

    @Test
    public void testPricesDisplayDiscount() {
        ListF<UserProductPriceEntity> prices = userProductPricesDao.findAll();
        assertNotEmpty(prices);
        SetF<String> ignore = Cf.set(
                "mail_pro_b2c_premium1000_discount50_v20210610_month_subs_USD",
                "mail_pro_b2c_standard100_discount50_v20210610_month_subs_USD",//https://st.yandex-team.ru/CHEMODAN-80531#618d1b6b52718e2e7f18de89
                "mail_pro_b2c_premium1000_discount50_v20210610_beauty_address_month_subs_USD",
                "mail_pro_b2c_standard100_discount50_v20210610_beauty_address_month_subs_USD",

                //https://st.yandex-team.ru/CHEMODAN-83203#6261346dd0d31b36d85f0cec
                "_discount20_v20220414_"
        );

        for (UserProductPriceEntity price : prices) {
            String currencyCode = price.getCurrencyCode();
            String code = userProductManager.findPrice(price.getId()).getPeriod().getCode() + "_" + currencyCode;
            if (price.getDisplayDiscountPercent().isPresent()) {
                Assert.ge(price.getDisplayDiscountPercent().get(), BigDecimal.ZERO);
                Assert.notEmpty(price.getDisplayOriginalPrice());

                BigDecimal realDiscount = price.getDisplayOriginalPrice().get().subtract(price.getPrice());
                Assert.ge(realDiscount, BigDecimal.ZERO, "discount price should be less when original for " + code);

                BigDecimal realDiscountPercent =
                        realDiscount.multiply(BigDecimal.valueOf(100))
                                .divide(price.getDisplayOriginalPrice().get(), RoundingMode.CEILING);
                if (ignore.filter(code::contains).isNotEmpty()) {
                    continue;
                }

                double delta = currencyCode.equals("USD") ? 7 : 1;
                Assert.assertEquals("price discount seems to be unfair for price " + code,
                        price.getDisplayDiscountPercent().get().doubleValue(),
                        realDiscountPercent.doubleValue(),
                        delta);
            } else {
                Assert.none(price.getDisplayOriginalPrice(), "has display original price for " + code);
            }
        }
    }

    @Test
    public void testStartPeriodPrices() {
        userProductPeriodDao.findAll()
                .map(UserProductPeriodEntity::getCode)
                .map(userProductManager::findPeriodByCode)
                .forEach(period -> {
                    Option<UserProductPrice> usdPrice =
                            period.getPrices().filter(price -> price.getCurrencyCode().equals("USD")).firstO();
                    Option<UserProductPrice> rubPrice =
                            period.getPrices().filter(price -> price.getCurrencyCode().equals("RUB")).firstO();
                    if (usdPrice.isPresent() && rubPrice.isPresent()) {
                        Assert.gt(rubPrice.get().getPrice().doubleValue(), usdPrice.get().getPrice().doubleValue(),
                                "RUB price must be bigger than USD for period" + period.getCode());
                    }
                    for (UserProductPrice price : period.getPrices()) {
                        price.getStartPeriodPrice().ifPresent(startPrice -> {
                            Option<CustomPeriod> discountPeriod = period.getStartPeriodDuration();
                            Assert.some(discountPeriod, "Start price should have start period duration for period " + period.getCode());
                            Instant now = Instant.now();
                            double discountPeriodInSeconds = discountPeriod.get().toDurationFrom(now).toStandardSeconds().getSeconds();
                            double pricePeriodInSeconds = period.getPeriod().toDurationFrom(now).toStandardSeconds().getSeconds();
                            double discountPeriodCoeff = discountPeriodInSeconds / pricePeriodInSeconds;
                            double normalizedDiscountPrice = startPrice.doubleValue() / discountPeriodCoeff;
                            double fullPrice = price.getPrice().doubleValue();

                            Assert.gt(
                                    fullPrice, normalizedDiscountPrice,
                                    String.format("Start period price should be lower than full price for period %s: " +
                                            "full price %s for period %s, discount price %s for period %s, " +
                                            "normalized discount price %s", period.getCode(),
                                            fullPrice, period.getPeriod(),
                                            startPrice.doubleValue(), discountPeriod.get(),
                                            normalizedDiscountPrice
                                            ));
                            }
                        );
                    }
                });
    }

    @Test
    public void testPeriodPrices() {
        userProductDao.findAll()
                .map(UserProductEntity::getCode)
                .map(userProductCode -> userProductManager.findByCodeO(userProductCode).get())
                .forEach(product -> {
                    ListF<UserProductPrice> allPrices = product.getProductPeriods()
                            .flatMap(UserProductPeriod::getPrices);
                    ListF<String> currencies = allPrices
                            .map(UserProductPrice::getCurrencyCode)
                            .stableUnique();
                    for (String currency : currencies) {
                        ListF<UserProductPrice> currentPrices = allPrices
                                .filter(price -> price.getCurrencyCode().equals(currency));
                        ListF<Double> yearPrices = pricesByPeriod(currentPrices, CustomPeriod.fromYears(1))
                                .map(BigDecimal::doubleValue);
                        ListF<Double> monthPrices = pricesByPeriod(currentPrices, CustomPeriod.fromMonths(1))
                                .map(BigDecimal::doubleValue);
                        for (Double yearPrice : yearPrices) {
                            for (Double monthPrice : monthPrices) {
                                double priceDiff = yearPrice / monthPrice;
                                Assert.gt(priceDiff, 5d,
                                        "To high year/month difference in " + currency + " price for product " + product.getCode());
                                Assert.lt(priceDiff, 12d,
                                        "To low year/month difference in " + currency + " price for product " + product.getCode());
                            }
                        }
                    }
                });
    }

    private ListF<BigDecimal> pricesByPeriod(ListF<UserProductPrice> currentPrices, CustomPeriod period) {
        return currentPrices.filter(price -> price.getPeriod().getPeriod().equals(period))
                .map(UserProductPrice::getPrice);
    }


    @Test
    public void testPromoPayload() {
        textsManagerMockConfiguration.reset();
        textsManager.updateTranslations();
        ListF<PromoPayloadEntity> promoPayloads = promoPayloadDao.findAll();
        assertNotEmpty(promoPayloads);

        for (PromoPayloadEntity promoPayload : promoPayloads) {
            String payload = promoPayload.getContent();
            String parsedPayload = promoPayloadParser.processPayload(payload, Language.RUSSIAN.value(), false);
            Assert.notEmpty(parsedPayload);
            Assert.isFalse(parsedPayload.toLowerCase().contains("tanker"));
        }
    }

    @Test
    // может быть ошибка при копировании продуктов - можно забыть поправить code family
    public void codeFamilyIsPartOfCode() {
        ListF<UserProductEntity> userProducts = userProductDao.findAll();
        assertNotEmpty(userProducts);
        MapF<String, ListF<UserProduct>> productsByFamily =
                userProducts.map(up -> userProductManager.findById(up.getId())).groupBy(UserProduct::getCodeFamily);

        for (Tuple2<String, ListF<UserProduct>> tuple : productsByFamily.entries()) {
            BigDecimal amount = getAmount(tuple._2.first());
            String firstProductCode = tuple._2.first().getCode();
            for (UserProduct up : tuple._2) {
                Assert.equals(getAmount(up), amount,
                        String.format("product %s space amount differs from %s within code family %s",
                                up.getCode(), firstProductCode, tuple._1));
            }
        }
    }

    @Test
    // может быть ошибка при копировании продуктов - можно забыть поправить trust fiscal title
    public void trustFiscalTitleIsCorrect() {
        ListF<UserProductEntity> userProducts = userProductDao.findAll();
        assertNotEmpty(userProducts);
        SetF<String> skipProducts = Cf.set("b2c_mail_disk_pro_v1", "b2c_mail_disk_pro_v1", "b2c_mail_pro_v1",
                "b2c_mail_pro_v1", "b2c_test_minute", "b2c_test_minute", "mail_pro_b2c_light", "mail_pro_b2c_light",
                "mail_pro_b2c_light_30days_trial_v2", "mail_pro_b2c_light_30days_trial_v2",
                "mail_pro_b2c_light_30days_trial_v3", "mail_pro_b2c_light_30days_trial_v3",
                "mail_pro_b2c_light_60days_trial_v2", "mail_pro_b2c_light_60days_trial_v2",
                "mail_pro_b2c_light_60days_trial_v3", "mail_pro_b2c_light_60days_trial_v3",
                "mail_pro_b2c_light_90days_trial_v2", "mail_pro_b2c_light_90days_trial_v2",
                "mail_pro_b2c_light_90days_trial_v3", "mail_pro_b2c_light_90days_trial_v3",
                "mail_pro_b2c_light_discount20_v2", "mail_pro_b2c_light_discount20_v2",
                "mail_pro_b2c_light_inapp_apple", "mail_pro_b2c_light_inapp_apple",
                "mail_pro_b2c_light_inapp_apple_for_disk", "mail_pro_b2c_light_inapp_apple_for_disk",
                "mail_pro_b2c_light_inapp_google", "mail_pro_b2c_light_inapp_google", "mail_pro_b2c_light_v2",
                "mail_pro_b2c_light_v2", "mail_pro_b2c_light_v2_inapp_apple", "mail_pro_b2c_light_v2_inapp_apple",
                "mail_pro_b2c_test_trial_v2", "mail_pro_b2c_test_trial_v2");

        for (UserProduct userProduct :
                userProducts.filter(x -> !skipProducts.containsTs(x.getCode())).map(x -> userProductManager.findById(x.getId()))) {
            BigDecimal amount = getAmount(userProduct);
            BigDecimal amountGb = amount.divide(BigDecimal.valueOf(1024L * 1024 * 1024), RoundingMode.FLOOR);
            BigDecimal amountTb = amount.divide(BigDecimal.valueOf(1024L * 1024 * 1024 * 1024), RoundingMode.FLOOR);
            for (UserProductPeriod period :
                    userProduct.getProductPeriods().filter(x -> x.getTrustFiscalTitle().isPresent())) {
                Assert.isTrue(period.getTrustFiscalTitle().get().contains(amountGb + " ГБ")
                                || period.getTrustFiscalTitle().get().contains(amountTb + " ТБ"),
                        String.format("product %s trust fiscal title '%s' for period %s should contain space info",
                                userProduct.getCode(), period.getTrustFiscalTitle(), period.getCode()));

                if (period.getPeriod().getUnit().equals(CustomPeriodUnit.ONE_MONTH)) {
                    Assert.assertContains(period.getTrustFiscalTitle().get(), "мес");
                }
                if (period.getPeriod().getUnit().equals(CustomPeriodUnit.ONE_YEAR)) {
                    Assert.assertContains(period.getTrustFiscalTitle().get(), "год");
                }

                //периоды, тикающие раз в X>1 интервалов времени и ходящие в траст должны отображать это в своем
                // названии
                if (period.getPeriod().getValue() > 1 && period.getTrustFiscalTitle().isPresent()) {
                    Assert.assertContains(period.getTrustFiscalTitle().get(),
                            Integer.toString(period.getPeriod().getValue()));
                }

                for (UserProductPrice price : period.getPrices()) {
                    if (price.getDisplayDiscountPercent().isPresent()) {
                        Assert.assertContains(period.getTrustFiscalTitle().get(), "скидка");
                        Assert.assertContains(period.getTrustFiscalTitle().get(),
                                price.getDisplayDiscountPercent().get() + "%");
                    } else {
                        Assert.isFalse(period.getTrustFiscalTitle().get().contains("скидка"),
                                "trust fiscal title contains discount info although price is not discount: " + userProduct.getCode());
                    }
                }
            }
        }
    }

    @Test
    public void featureCodeConsistencyTest() {
        for (ProductFeatureEntity productFeature : productFeatureDao.findAll()) {
            productFeature.getFeatureId().ifPresent(featureId -> {
                FeatureEntity feature = featureDao.findById(featureId);
                Assert.equals(feature.getCode(), productFeature.getCode(), "Code and id is inconsistent for feature " +
                        "and productFeature");
            });
        }

    }

    @Test
    public void featureOrderIsUnique() {
        MapF<UUID, ListF<ProductFeatureEntity>> featuresByProduct =
                productFeatureDao.findAll().groupBy(ProductFeatureEntity::getUserProductId);
        SetF<String> skipProducts = Cf.set(
                "b2c_mail_pro_v1",
                "b2c_mail_disk_pro_v1",
                "b2b_disk_2tb_v1",
                "b2b_disk_200gb_v1",
                "b2b_mail_pro_v1"
        );

        for (UUID userProductId : featuresByProduct.keys()) {
            String productCode = userProductDao.findById(userProductId).getCode();
            if (skipProducts.containsTs(productCode)) {
                continue;
            }

            ListF<ProductFeatureEntity> features =
                    featuresByProduct.getTs(userProductId).filter(pf -> pf.getOrderNum() < 999 && pf.isEnabled()
                            && pf.getDescriptionTankerKeyId().isPresent() // костыль. в базе непорядок
                            && !pf.getCode().equals("mail_pro_b2c_add_disk_space")); // костыль. в базе непорядок
            ListF<Integer> duplicateOrderNums =
                    features.groupBy(ProductFeatureEntity::getOrderNum).filter((k, v) -> v.size() > 1).keys();

            Assert.equals(0, duplicateOrderNums.size(),
                    "found duplicate order nums " + String.join(", ", duplicateOrderNums.map(Object::toString)) +
                            " for product " + productCode);
        }
    }

    @Test
    public void startPeriodFieldsConsistency() {
        for (UserProductPeriodEntity period : userProductPeriodDao.findAll()) {
            Assert.equals(period.getStartPeriodDuration().isPresent(), period.getStartPeriodCount().isPresent(),
                    "Start period parameters are inconsistent. Duration and count is "
                            + period.getStartPeriodDuration() + " and " + period.getStartPeriodCount());
            ListF<UserProductPriceEntity> prices = userProductPricesDao.findByPeriodId(period.getId());
            boolean durationPresents = period.getStartPeriodDuration().isPresent();
            prices.forEach(price -> Assert.equals(price.getStartPeriodPrice().isPresent(), durationPresents,
                    "Period " + period.getCode() + (durationPresents ? " has " : "has no") +
                            " duration but has startPeriodPrice with value " + price.getStartPeriodPrice()));
        }
    }

    @Test
    public void prepaidIsPrepaid() {
        ListF<GroupProductEntity> groupProducts = groupProductDao.findAll();
        ListF<GroupProductEntity> prepaidProducts = groupProducts.filter(x -> x.getCode().matches(".*prepaid.*"));
        for (GroupProductEntity product : prepaidProducts) {
            Assert.equals(GroupPaymentType.PREPAID, product.getPaymentType(),
                    "product " + product + " is not prepaid as it should be");
        }
    }

    @Test
    public void groupProductIsInBucket() {
        ListF<GroupProductEntity> groupProducts = groupProductDao.findAll().filter(x -> !x.isHidden());
        ListF<SetF<String>> buckets = bucketContentDao.getBuckets();
        for (GroupProductEntity product : groupProducts) {
            Assert.isTrue(buckets.find(b -> b.find(x -> x.equals(product.getCode())).isPresent()).isPresent(),
                    "no bucket with product " + product.getCode());
        }
    }

    @Test
    public void singlePriceForInapp() {
        ListF<UserProduct> inappProducts = userProductManager.findByBillingTypes(BillingType.INAPP_APPLE,
                BillingType.INAPP_GOOGLE);
        for (UserProduct inappProduct : inappProducts) {
            for (UserProductPeriod period : inappProduct.getProductPeriods()) {
                Assert.assertEquals("Only one price allowed for inapp period",1, period.getPrices().size());
            }
        }

    }

    private BigDecimal getAmount(UserProduct userProduct) {
        return userProduct.getFeatures()
                .filter(x -> x.getCode() != null &&
                        (x.getCode().equals(MPFS_SPACE_FEATURE_CODE) || x.getCode().equals("mail_pro_b2c_add_disk_space")))
                .firstO()
                .map(UserProductFeature::getAmount).orElse(BigDecimal.ZERO);
    }

    @Test
    public void assertTrustPeriodShorterThan100Symbols() {
        ListF<UserProductPeriodEntity> pes = userProductPeriodDao.findAll();
        for(UserProductPeriodEntity pe: pes) {
            UserProductPeriod p = new UserProductPeriod(pe, userProductManager, userProductPricesDao);
            String withAutoProlong = p.getTrustProductId(true);
            String withoutAutoProlong = p.getTrustProductId(false);

            try {
                Assert.lt(withAutoProlong.length(), 100);
            } catch (AssertionError e) {
                throw new AssertionError("Code won't do: " + withAutoProlong + " because " + e.getMessage(), e);
            }
            try {
                Assert.lt(withoutAutoProlong.length(), 100);
            } catch (AssertionError e) {
                throw new AssertionError("Code won't do: " + withoutAutoProlong + " because " + e.getMessage(), e);
            }
        }
    }
}
