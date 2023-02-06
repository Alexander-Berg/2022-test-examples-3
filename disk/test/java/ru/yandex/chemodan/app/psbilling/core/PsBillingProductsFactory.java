package ru.yandex.chemodan.app.psbilling.core;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Currency;
import java.util.UUID;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.joda.time.Instant;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.BucketContentDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupProductDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.TrialDefinitionDao.InsertData;
import ru.yandex.chemodan.app.psbilling.core.dao.products.FeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductOwnerDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductSetDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductTemplateFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductBucketDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductPeriodDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductPricesDao;
import ru.yandex.chemodan.app.psbilling.core.entities.AbstractEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriod;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupProductType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.TrialDefinitionEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.TrialType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.BillingType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureScope;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductFeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductOwner;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductSetEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductTemplateFeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPeriodEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPriceEntity;
import ru.yandex.chemodan.app.psbilling.core.groups.TrialService;
import ru.yandex.chemodan.app.psbilling.core.products.BucketContentManager;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProductManager;
import ru.yandex.chemodan.app.psbilling.core.synchronization.feature.errorprocessors.DefaultErrorProcessor;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;

import static ru.yandex.chemodan.app.psbilling.core.products.UserProductFeatureRegistry.FAN_MAIL_LIMIT_FEATURE_CODE;
import static ru.yandex.chemodan.app.psbilling.core.products.UserProductFeatureRegistry.MPFS_SPACE_FEATURE_CODE;

@AllArgsConstructor
public class PsBillingProductsFactory {
    public static final String DEFAULT_BALANCE_PRODUCT_NAME = "balance_product";
    public static final Currency DEFAULT_CURRENCY = Currency.getInstance("RUB");
    public static final GroupPaymentType DEFAULT_PAYMENT_TYPE = GroupPaymentType.POSTPAID;
    public static final String DEFAULT_MAINS_SET = "DEFAULT_MAINS_SET";
    public static final String DEFAULT_ADDONS_SET = "DEFAULT_ADDONS_SET";
    private static final String DEFAULT_ADDON_BUCKET = "ADDON_BUCKET";


    private final GroupProductDao groupProductDao;
    private final UserProductDao userProductDao;
    private final ProductFeatureDao productFeatureDao;
    private final FeatureDao featureDao;
    private final ProductOwnerDao productOwnerDao;
    private final UserProductPricesDao userProductPricesDao;
    private final UserProductPeriodDao userProductPeriodDao;
    private final TrialService trialService;
    private final ProductLineDao productLineDao;
    private final ProductSetDao productSetDao;
    private final GroupProductManager groupProductManager;
    private final PsBillingTextsFactory psBillingTextsFactory;
    private final UserProductBucketDao userProductBucketDao;
    private final ProductTemplateFeatureDao productTemplateFeatureDao;
    private final ProductTemplateDao productTemplateDao;
    private final BucketContentDao bucketContentDao;
    private final BucketContentManager bucketContentManager;

    private final JdbcTemplate3 jdbcTemplate3;


    @NotNull
    public GroupProduct createGroupProduct() {
        return createGroupProduct(Function.identityF());
    }

    @NotNull
    public GroupProduct createGroupProduct(
            Function<GroupProductDao.InsertData.InsertDataBuilder, GroupProductDao.InsertData.InsertDataBuilder> customizer) {
        UserProductEntity userProduct = createUserProduct();
        GroupProductDao.InsertData.InsertDataBuilder defaultBuilder = GroupProductDao.InsertData.builder()
                .code(UUID.randomUUID().toString()).userProduct(userProduct.getId())
                .titleTankerKeyId(Option.of(psBillingTextsFactory.create().getId()))
                .pricePerUserInMonth(BigDecimal.TEN).priceCurrency(DEFAULT_CURRENCY)
                .balanceProductName(Option.of(DEFAULT_BALANCE_PRODUCT_NAME))
                .displayDiscountPercent(Option.of(BigDecimal.TEN))
                .displayOriginalPrice(Option.of(BigDecimal.TEN))
                .paymentType(DEFAULT_PAYMENT_TYPE)
                .productType(GroupProductType.MAIN);
        defaultBuilder = customizer.apply(defaultBuilder);
        return groupProductManager.findById(groupProductDao.insert(defaultBuilder.build()).getId());
    }

    public GroupProduct createPrepaidProduct(Currency currency) {
        return createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID).priceCurrency(currency));
    }

    public GroupProduct createPostpaidProduct(Currency currency) {
        return createGroupProduct(x -> x.paymentType(GroupPaymentType.POSTPAID).priceCurrency(currency));
    }

    public void addUserProductToProductSet(String key, UserProductEntity product) {
        addUserProductToProductSet(key, product.getId());
    }

    public void addUserProductToProductSet(String key, UUID... productIds) {
        Option<ProductSetEntity> setO = productSetDao.findByKey(key);
        if (!setO.isPresent()) {
            setO = Option.of(productSetDao.create(ProductSetDao.InsertData.builder().key(key).build()));
        }
        ProductLineEntity productLineEntity = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(setO.get().getId()).orderNum(1).build());
        productLineDao.bindUserProducts(productLineEntity.getId(), Cf.list(productIds));
    }

    public void addProductSetToBucket(String bucketCode, String setKey) {
        userProductBucketDao.addToBucket(UserProductBucketDao.InsertData.builder()
                .code(bucketCode).productSetId(productSetDao.findByKey(setKey).map(ProductSetEntity::getId)).build());
    }

    public ProductLineEntity createProductLine(String setKey) {
        return createProductLine(setKey, x -> x);
    }

    public ProductLineEntity createProductLine(String setKey, Function<ProductLineDao.InsertData.InsertDataBuilder,
            ProductLineDao.InsertData.InsertDataBuilder> customizer, UserProductEntity... products) {
        Option<ProductSetEntity> setO = productSetDao.findByKey(setKey);
        if (!setO.isPresent()) {
            setO = Option.of(productSetDao.create(ProductSetDao.InsertData.builder().key(setKey).build()));
            addProductSetToBucket(setKey, setKey);
        }
        ProductLineEntity line = productLineDao
                .create(customizer.apply(ProductLineDao.InsertData.builder()
                        .productSetId(setO.get().getId())
                        .orderNum(1)).build());
        if (products.length > 0) {
            productLineDao.bindUserProducts(line.getId(),
                    Cf.list(Arrays.stream(products).map(AbstractEntity::getId).toArray(UUID[]::new)));
        }
        return line;
    }

    public TrialDefinitionEntity createTrialDefinitionWithPeriod(
            Function<InsertData.InsertDataBuilder, InsertData.InsertDataBuilder> customizer) {
        InsertData.InsertDataBuilder builder = InsertData.builder()
                .duration(Option.of(1))
                .durationMeasurement(Option.of(CustomPeriodUnit.ONE_MONTH))
                .hidden(false)
                .price(BigDecimal.ZERO)
                .singleUsageComparisonKey(Option.of(UUID.randomUUID().toString()))
                .type(TrialType.CONSTANT_PERIOD)
                .endDate(Option.empty());

        return trialService.insert(customizer.apply(builder).build());
    }

    public TrialDefinitionEntity createTrialDefinitionWithPeriod() {
        return createTrialDefinitionWithPeriod(Function.identityF());
    }

    public TrialDefinitionEntity createTrialDefinitionUtilDate(Instant until,
                                                               Function<InsertData.InsertDataBuilder,
                                                                       InsertData.InsertDataBuilder> customizer) {
        InsertData.InsertDataBuilder builder = InsertData.builder()
                .endDate(Option.of(until))
                .hidden(false)
                .price(BigDecimal.ZERO)
                .type(TrialType.UNTIL_DATE)
                .endDate(Option.empty());

        return trialService.insert(customizer.apply(builder).build());
    }

    public TrialDefinitionEntity createTrialDefinitionUtilDate(Instant until) {
        return createTrialDefinitionUtilDate(until, Function.identityF());
    }

    public UserProductEntity createUserProduct() {
        return createUserProduct(Function.identityF());
    }

    public UserProductPeriodEntity createUserProductPeriod(UserProductEntity userProductEntity) {
        return createUserProductPeriod(userProductEntity, CustomPeriodUnit.TEN_MINUTES);
    }

    public UserProductPeriodEntity createUserProductPeriod(UserProductEntity userProductEntity,
                                                           CustomPeriodUnit period) {
        return createUserProductPeriod(userProductEntity, period, Function.identityF());
    }

    public UserProductPeriodEntity createUserProductPeriod(UserProductEntity userProductEntity, CustomPeriodUnit period,
                                                           Function<UserProductPeriodDao.InsertData.InsertDataBuilder,
                                                                   UserProductPeriodDao.InsertData.InsertDataBuilder> customizer) {
        UserProductPeriodDao.InsertData.InsertDataBuilder builder = UserProductPeriodDao.InsertData.builder()
                .code(userProductEntity.getCode() + "_" + period.value())
                .period(new CustomPeriod(period, 1))
                .trustFiscalTitle(Option.of("Fiscal_title"))
                .userProductId(userProductEntity.getId());

        return userProductPeriodDao.create(customizer.apply(builder).build());
    }

    public UserProductEntity createUserProduct(
            Function<UserProductDao.InsertData.InsertDataBuilder, UserProductDao.InsertData.InsertDataBuilder> customizer) {
        UserProductDao.InsertData.InsertDataBuilder builder = UserProductDao.InsertData.builder()
                .code(UUID.randomUUID().toString())
                .codeFamily(UUID.randomUUID().toString())
                .titleTankerKeyId(psBillingTextsFactory.create().getId())
                .productOwnerId(getOrCreateProductOwner().getId())
                .trustServiceId(Option.of(111))
                .billingType(BillingType.TRUST);

        return userProductDao.insert(customizer.apply(builder).build());
    }

    public UserProductEntity createUserProductWithPrice(
            Function<UserProductDao.InsertData.InsertDataBuilder, UserProductDao.InsertData.InsertDataBuilder> customizer) {
        UserProductDao.InsertData.InsertDataBuilder builder = UserProductDao.InsertData.builder()
                .code(UUID.randomUUID().toString())
                .codeFamily(UUID.randomUUID().toString())
                .titleTankerKeyId(psBillingTextsFactory.create().getId())
                .productOwnerId(getOrCreateProductOwner().getId())
                .trustServiceId(Option.of(111))
                .billingType(BillingType.TRUST);

        UserProductEntity product = userProductDao.insert(customizer.apply(builder).build());
        createUserProductPrices(product, CustomPeriodUnit.ONE_MONTH);
        return product;
    }

    public UserProductPriceEntity createUserProductPrices(UserProductPeriodEntity period, BigDecimal price) {
        return createUserProductPrices(period.getId(), p -> p.price(price));
    }

    public UserProductPriceEntity createUserProductPrices(UserProductEntity product, CustomPeriodUnit period,
                                                          BigDecimal price,
                                                          Function<UserProductPeriodDao.InsertData.InsertDataBuilder,
                                                                  UserProductPeriodDao.InsertData.InsertDataBuilder> periodCustomizer) {
        UserProductPeriodEntity periodEntity = createUserProductPeriod(product, period, periodCustomizer);

        return createUserProductPrices(periodEntity.getId(), p -> p.price(price));
    }

    public UserProductPriceEntity createUserProductPrices(UserProductEntity product, CustomPeriodUnit period,
                                                          BigDecimal price) {
        return createUserProductPrices(product, period, price, Function.identityF());
    }

    public UserProductPriceEntity createUserProductPrices(CustomPeriodUnit period,
                                                          BigDecimal price, CustomPeriod startPeriodDuration,
                                                          Integer startPeriodCount,
                                                          BigDecimal startPeriodPrice) {
        return createUserProductPrices(createUserProduct(), period, price, startPeriodDuration, startPeriodCount,
                startPeriodPrice);
    }

    public UserProductPriceEntity createUserProductPrice() {
        return createUserProductPrices(CustomPeriodUnit.ONE_MONTH,
                BigDecimal.TEN, null, null, null);
    }

    public UserProductPriceEntity createUserProductPrices(UserProductEntity product, CustomPeriodUnit period,
                                                          BigDecimal price, CustomPeriod startPeriodDuration,
                                                          Integer startPeriodCount,
                                                          BigDecimal startPeriodPrice) {
        UserProductPeriodEntity periodEntity = createUserProductPeriod(product, period, builder -> {
            builder.startPeriodCount(Option.ofNullable(startPeriodCount));
            builder.startPeriodDurationLength(Option.ofNullable(startPeriodDuration).map(CustomPeriod::getValue));
            builder.startPeriodDurationMeasurement(Option.ofNullable(startPeriodDuration).map(CustomPeriod::getUnit));
            return builder;
        });

        return createUserProductPrices(periodEntity.getId(), p -> {
            p.price(price);
            p.startPeriodPrice(Option.of(startPeriodPrice));
            return p;
        });
    }

    public UserProductPriceEntity createUserProductPrices(UserProductEntity product, CustomPeriodUnit period) {
        return createUserProductPrices(product, period, BigDecimal.TEN);
    }

    public UserProductPriceEntity createUserProductPrices(UUID periodEntityId,
                                                          Function<UserProductPricesDao.InsertData.InsertDataBuilder,
                                                                  UserProductPricesDao.InsertData.InsertDataBuilder> customizer) {

        UserProductPricesDao.InsertData.InsertDataBuilder builder = UserProductPricesDao.InsertData.builder()
                .userProductPeriodId(periodEntityId)
                .regionId("225")
                .price(BigDecimal.TEN)
                .currencyCode("RUB");
        return userProductPricesDao.create(customizer.apply(builder).build());
    }


    public FeatureEntity createFeature(FeatureType type) {
        return createFeature(type, Function.identityF());
    }

    public FeatureEntity createFeature(FeatureType type,
                                       Function<FeatureEntity.FeatureEntityBuilder,
                                               FeatureEntity.FeatureEntityBuilder> customizer) {
        FeatureEntity.FeatureEntityBuilder defaultBuilder = FeatureEntity.builder()
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .code(UUID.randomUUID().toString())
                .activationRequestTemplate(Option.empty())
                .setAmountRequestTemplate(Option.empty())
                .deactivationRequestTemplate(Option.empty())
                .description("description")
                .systemTvmId(Option.empty())
                .errorProcessorName(DefaultErrorProcessor.NAME)
                .type(type)
                .callSetAmountOnActivation(true)
                .callSetAmountOnDeactivation(true);
        defaultBuilder = customizer.apply(defaultBuilder);
        return featureDao.insert(defaultBuilder.build());
    }

    public ProductFeatureEntity createProductFeature(UUID userProductId, FeatureEntity feature) {
        return createProductFeature(userProductId, feature, java.util.function.Function.identity());
    }

    public ProductFeatureEntity createProductFeature(UUID userProductId, FeatureEntity feature,
                                                     java.util.function.Function<ProductFeatureDao.InsertData.InsertDataBuilder, ProductFeatureDao.InsertData.InsertDataBuilder> customizer) {
        ProductFeatureDao.InsertData.InsertDataBuilder defaultBuilder = ProductFeatureDao.InsertData.builder()
                .featureId(Option.of(feature.getId()))
                .amount(feature.getType() == FeatureType.ADDITIVE ? BigDecimal.TEN : BigDecimal.ONE)
                .userProductId(userProductId)
                .scope(FeatureScope.USER)
                .code(UUID.randomUUID().toString());
        defaultBuilder = customizer.apply(defaultBuilder);
        return productFeatureDao.insert(defaultBuilder.build());
    }

    public ProductTemplateEntity createProductTemplate(String templateCode) {
        return productTemplateDao.insert(templateCode);
    }

    public ProductTemplateFeatureEntity createProductTemplateFeature(UUID templateCode, FeatureEntity feature) {
        ProductTemplateFeatureDao.InsertData.InsertDataBuilder defaultBuilder =
                ProductTemplateFeatureDao.InsertData.builder()
                        .featureId(Option.of(feature.getId()))
                        .amount(feature.getType() == FeatureType.ADDITIVE ? BigDecimal.TEN : BigDecimal.ONE)
                        .productTemplateId(templateCode)
                        .code(UUID.randomUUID().toString());
        return productTemplateFeatureDao.insert(defaultBuilder.build());
    }

    public ProductOwner createProductOwner(Option<UUID> defaultGroupProductId) {
        return productOwnerDao.create(UUID.randomUUID().toString(), defaultGroupProductId);
    }

    public ProductOwner getOrCreateProductOwner() {
        return getOrCreateProductOwner("disk");
    }

    public ProductOwner getOrCreateProductOwner(String code) {
        return productOwnerDao.findByCode(code).orElseGet(() -> productOwnerDao.create(code, Option.empty()));
    }

    public void addProductsToBucket(String bucket, UUID... productIds) {
        for (UUID productId : productIds) {
            userProductBucketDao.addToBucket(UserProductBucketDao.InsertData.builder()
                    .code(bucket)
                    .userProductId(Option.of(productId))
                    .build());
        }
    }

    public void addGroupProductsToBucket(String bucket, String... productCodes) {
        for (String productCode : productCodes) {
            bucketContentDao.addRowBucket(bucket, productCode);
        }
    }

    public GroupProduct createProductWithCommonEmailFeatures(BigDecimal price, long diskSpace) {
        FeatureEntity featureStub = createFeature(FeatureType.ADDITIVE);
        GroupProduct groupProduct = createGroupProduct(b -> b
                .paymentType(GroupPaymentType.PREPAID)
                .pricePerUserInMonth(price)
        );
        createProductFeature(groupProduct.getUserProductId(), featureStub,
                pf -> pf.amount(BigDecimal.valueOf(diskSpace))
                        .code(MPFS_SPACE_FEATURE_CODE)
                        .valueTankerKeyId(Option.of(psBillingTextsFactory.create("test_40_Mb").getId())));
        createProductFeature(groupProduct.getUserProductId(), featureStub,
                pf -> pf.code(FAN_MAIL_LIMIT_FEATURE_CODE)
                        .amount(BigDecimal.valueOf(33))
                        .valueTankerKeyId(Option.of(psBillingTextsFactory.create("test_1234").getId())));
        return groupProduct;
    }

    public ProductLineEntity createGroupProductLineWithSet(ListF<GroupProduct> groupProducts) {
        ProductSetEntity productSet = createProductSet("mail_pro_b2b");
        return createGroupProductLineWithSet(productSet.getId(), groupProducts);
    }

    public ProductLineEntity createGroupProductLineWithSet(UUID productSet, ListF<GroupProduct> groupProducts) {
        ProductLineEntity productLine = productLineDao.create(
                ProductLineDao.InsertData.builder().productSetId(productSet).build());
        productLineDao.bindGroupProducts(productLine.getId(), groupProducts.map(GroupProduct::getId));
        return productLine;
    }

    public ProductSetEntity createProductSet(String key) {
        return productSetDao.create(
                ProductSetDao.InsertData.builder().key(key).build());
    }

    public Tuple2<ListF<GroupProduct>, ListF<GroupProduct>> createMainsAndAddons() {
        GroupProduct main1 = createGroupProduct();
        GroupProduct main2 = createGroupProduct();
        GroupProduct addon1 = createGroupProduct(builder -> builder.productType(GroupProductType.ADDON));
        GroupProduct addon2 = createGroupProduct(builder -> builder.productType(GroupProductType.ADDON));

        ListF<GroupProduct> addons = Cf.list(addon1, addon2);
        addons.forEach(addon -> groupProductDao.linkAddon(main1, addon));
        addons.forEach(addon -> groupProductDao.linkAddon(main2, addon));

        createGroupProductLineWithSet(createProductSet(DEFAULT_MAINS_SET).getId(), Cf.list(main1, main2));
        createGroupProductLineWithSet(createProductSet(DEFAULT_ADDONS_SET).getId(), Cf.list(addon1, addon2));
        addGroupProductsToBucket(DEFAULT_ADDON_BUCKET, addon1.getCode(), addon2.getCode());
        bucketContentManager.refresh();
        return Tuple2.tuple(Cf.list(main1, main2), addons);
    }


    public void cleanUpPromoTemplate(UUID promoTemplateId) {
        jdbcTemplate3.update("DELETE FROM promo_product_lines WHERE promo_template_id = ?", promoTemplateId);
        jdbcTemplate3.update("DELETE FROM promo_templates WHERE id = ?", promoTemplateId);
        jdbcTemplate3.update("DELETE FROM group_promos WHERE promo_template_id = ?", promoTemplateId);
        jdbcTemplate3.update("DELETE FROM user_promos WHERE promo_template_id = ?", promoTemplateId);
    }
}
