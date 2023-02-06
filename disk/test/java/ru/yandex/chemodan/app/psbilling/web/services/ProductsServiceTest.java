package ru.yandex.chemodan.app.psbilling.web.services;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingProductsFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTextsFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingUsersFactory;
import ru.yandex.chemodan.app.psbilling.core.config.Settings;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductSetDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductBucketDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductPricesDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PromoPayloadDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PromoTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.users.OrderDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriod;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.products.BillingType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductSetEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPeriodEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPriceEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.texts.TankerKeyEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderType;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceBillingStatus;
import ru.yandex.chemodan.app.psbilling.core.mocks.Blackbox2MockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.products.UserProduct;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice;
import ru.yandex.chemodan.app.psbilling.core.promos.PromoService;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.texts.TextsManager;
import ru.yandex.chemodan.app.psbilling.core.users.UserService;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.core.utils.PromoHelper;
import ru.yandex.chemodan.app.psbilling.web.config.PsBillingWebServicesConfiguration;
import ru.yandex.chemodan.app.psbilling.web.model.CustomPeriodApi;
import ru.yandex.chemodan.app.psbilling.web.model.OrderStatusApi;
import ru.yandex.chemodan.app.psbilling.web.model.ProductPricePojo;
import ru.yandex.chemodan.app.psbilling.web.model.ProductSetPojo;
import ru.yandex.chemodan.app.psbilling.web.model.ProductWithPricesPojo;
import ru.yandex.chemodan.app.psbilling.web.model.PromoPojo;
import ru.yandex.chemodan.app.psbilling.web.model.UserServicePojo;
import ru.yandex.chemodan.web.JacksonPojoResultSerializer;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.db.masterSlave.MasterSlaveContextHolder;
import ru.yandex.misc.db.masterSlave.MasterSlavePolicy;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author yashunsky
 */
@ContextConfiguration(classes = {
        PsBillingWebServicesConfiguration.class
})
public class ProductsServiceTest extends AbstractPsBillingCoreTest {
    public static final PassportUid TEST_UID = PassportUid.cons(3000185708L);
    public static final Option<PassportUid> TEST_UID_O = Option.of(TEST_UID);

    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoHelper promoHelper;
    @Autowired
    private PromoTemplateDao promoTemplateDao;
    @Autowired
    private ProductsService productsService;
    @Autowired
    private PsBillingProductsFactory productsFactory;
    @Autowired
    private PsBillingTextsFactory textsFactory;
    @Autowired
    private ProductSetDao productSetDao;
    @Autowired
    private ProductLineDao productLineDao;
    @Autowired
    private PromoPayloadDao promoPayloadDao;
    @Autowired
    private TextsManager textsManager;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Settings settings;
    @Autowired
    private PsBillingUsersFactory usersFactory;

    @Before
    public void setUp() {
        featureFlags.getPreventInappBuyIfTrustExists().setValue("true");
    }

    @Test
    @SneakyThrows
    public void discountProductSetSerializationTest() {
        textsManagerMockConfig.reset();
        String productSetKey = "mail_pro_b2c";
        String productCode1 = UUID.randomUUID().toString();
        String productCode2 = UUID.randomUUID().toString();

        setupDiscountProductSet(productSetKey, productCode1, productCode2, data -> data.price(new BigDecimal(10)));

        ProductSetPojo productSet = productsService.getProductSet(
                TEST_UID_O, productSetKey, Option.empty(), Option.empty(), true, true, Cf.list());

        JacksonPojoResultSerializer resultSerializer = new JacksonPojoResultSerializer(objectMapper);

        OutputStream stream = new ByteArrayOutputStream();

        resultSerializer.serialize(productSet, null, null, stream);

        String result = stream.toString().replace("2121-01-31T21:00:00.000+0000", "2121-02-01T00:00:00.000+0300");
        UserProduct userProduct1 = userProductManager.findByCodeO(productCode1).get();
        UserProduct userProduct2 = userProductManager.findByCodeO(productCode2).get();

        String expectedResult = "{\n" +
                "  \"items\" : [ {\n" +
                "    \"product_id\" : \"" + productCode1 + "\",\n" +
                "    \"product_id_family\" : \"" + userProduct1.getCodeFamily() + "\",\n" +
                "    \"title\" : \"Тестовый ключ\",\n" +
                "    \"best_offer\" : false,\n" +
                "    \"product_type\" : \"subscription\",\n" +
                "    \"features\" : [ {\n" +
                "      \"description\" : \"Тестовый ключ\",\n" +
                "      \"group\" : \"\",\n" +
                "      \"value\" : \"\",\n" +
                "      \"enabled\" : true,\n" +
                "      \"code\" : \"code1\"\n" +
                "    }, {\n" +
                "      \"description\" : \"Тестовый ключ 2\",\n" +
                "      \"group\" : \"\",\n" +
                "      \"value\" : \"\",\n" +
                "      \"enabled\" : false,\n" +
                "      \"code\" : \"code2\"\n" +
                "    } ],\n" +
                "    \"prices\" : [ {\n" +
                "      \"price_id\" : \"" + productCode1 + "_10minutes\",\n" +
                "      \"amount\" : 9,\n" +
                "      \"currency\" : \"RUB\",\n" +
                "      \"period\" : \"month\",\n" +
                "      \"period_count\" : 1,\n" +
                "      \"discount_percent\" : 50,\n" +
                "      \"original_amount\" : 20,\n" +
                "      \"has_active_service\" : false\n" +
                "    } ]\n" +
                "  }, {\n" +
                "    \"product_id\" : \"" + productCode2 + "\",\n" +
                "    \"product_id_family\" : \"" + userProduct2.getCodeFamily() + "\",\n" +
                "    \"title\" : \"Тестовый ключ\",\n" +
                "    \"best_offer\" : false,\n" +
                "    \"product_type\" : \"subscription\",\n" +
                "    \"features\" : [ {\n" +
                "      \"description\" : \"Тестовый ключ\",\n" +
                "      \"group\" : \"\",\n" +
                "      \"value\" : \"\",\n" +
                "      \"enabled\" : false,\n" +
                "      \"code\" : \"code1\"\n" +
                "    }, {\n" +
                "      \"description\" : \"Тестовый ключ 2\",\n" +
                "      \"group\" : \"\",\n" +
                "      \"value\" : \"\",\n" +
                "      \"enabled\" : true,\n" +
                "      \"code\" : \"code2\"\n" +
                "    } ],\n" +
                "    \"prices\" : [ {\n" +
                "      \"price_id\" : \"" + productCode2 + "_month\",\n" +
                "      \"amount\" : 10,\n" +
                "      \"currency\" : \"RUB\",\n" +
                "      \"period\" : \"month\",\n" +
                "      \"period_count\" : 1,\n" +
                "      \"has_active_service\" : false\n" +
                "    } ]\n" +
                "  } ],\n" +
                "  \"available_until_date\" : \"2121-02-01T00:00:00.000+0300\"\n" +
                "}";

        Assert.equals(expectedResult, result);
    }

    @Test
    public void gotActivePromo_AllValues() {
        String productSetKey = "key";
        UserProductEntity product1 = psBillingProductsFactory.createUserProduct();
        ProductLineEntity line = psBillingProductsFactory.createProductLine(productSetKey);
        productLineDao.bindUserProducts(line.getId(), Cf.list(product1.getId()));

        UserProductEntity product2 = psBillingProductsFactory.createUserProduct();
        ProductLineEntity promoLine = psBillingProductsFactory.createProductLine(productSetKey);
        productLineDao.bindUserProducts(promoLine.getId(), Cf.list(product2.getId()));

        TankerKeyEntity promoTankerName = psBillingTextsFactory.create();
        PromoTemplateEntity promo = promoHelper.createGlobalPromo(x -> x
                .toDate(Option.of(DateUtils.futureDate()))
                .promoNameTankerKey(Option.of(promoTankerName.getId())));

        promoTemplateDao.bindProductLines(promo.getId(), promoLine.getId());
        promoPayloadDao.create(new PromoPayloadDao.InsertData(promo.getId(), PROMO_PAYLOAD_TYPE_WEB_DISK, "{\"picture" +
                "\":\"url\"}", 0));

        ProductSetPojo productSet =
                productsService.getProductSetForUpgrade(TEST_UID_O, productSetKey, Option.empty(), Option.empty(),
                        Option.empty(), true, false, Option.of(PROMO_PAYLOAD_TYPE_WEB_DISK), Option.of(0), Cf.list());

        Assert.notEmpty(productSet.getActivePromo());
        PromoPojo promoPojo = productSet.getActivePromo().get();
        assertEquals(promo.getCode(), promoPojo.getKey());
        assertEquals("Тестовый ключ", promoPojo.getTitle().get());
        assertEquals(DateUtils.futureDate(), promoPojo.getAvailableUntil().get());
        assertEquals("{\"picture\":\"url\"}", promoPojo.getPayload().get());

        productSet =
                productsService.getProductSetForUpgrade(TEST_UID_O, productSetKey, Option.empty(), Option.empty(),
                        Option.empty(), true, false, Option.of(PROMO_PAYLOAD_TYPE_MOBILE), Option.of(0), Cf.list());
        Assert.isEmpty(productSet.getActivePromo().get().getPayload());
    }

    @Test
    public void gotActivePromo_WithDuration() {
        DateUtils.freezeTime(Instant.parse("2021-07-01T00:00:00+0300"));

        String productSetKey = "key";
        UserProductEntity product1 = psBillingProductsFactory.createUserProduct();
        ProductLineEntity line = psBillingProductsFactory.createProductLine(productSetKey);
        productLineDao.bindUserProducts(line.getId(), Cf.list(product1.getId()));

        UserProductEntity product2 = psBillingProductsFactory.createUserProduct();
        ProductLineEntity promoLine = psBillingProductsFactory.createProductLine(productSetKey);
        productLineDao.bindUserProducts(promoLine.getId(), Cf.list(product2.getId()));

        PromoTemplateEntity promo = promoHelper.createUserPromo(x -> x.duration(Option.of(CustomPeriod.fromDays(5))));

        promoService.activatePromoForUser(TEST_UID_O.get(), promo.getId());
        promoTemplateDao.bindProductLines(promo.getId(), promoLine.getId());

        ProductSetPojo productSet =
                productsService.getProductSetForUpgrade(TEST_UID_O, productSetKey, Option.empty(), Option.empty(),
                        Option.empty(), true, false, Option.empty(), Option.empty(), Cf.list());

        Assert.notEmpty(productSet.getActivePromo());
        PromoPojo promoPojo = productSet.getActivePromo().get();
        assertEquals(promo.getCode(), promoPojo.getKey());
        Assert.notEmpty(promoPojo.getAvailableUntil());
        assertEquals(Instant.now().plus(Duration.standardDays(5)), promoPojo.getAvailableUntil().get());
    }

    @Test
    public void gotActivePromo_NoValues() {
        String productSetKey = "key";
        UserProductEntity product1 = psBillingProductsFactory.createUserProduct();
        ProductLineEntity line = psBillingProductsFactory.createProductLine(productSetKey);
        productLineDao.bindUserProducts(line.getId(), Cf.list(product1.getId()));

        UserProductEntity product2 = psBillingProductsFactory.createUserProduct();
        ProductLineEntity promoLine = psBillingProductsFactory.createProductLine(productSetKey);
        productLineDao.bindUserProducts(promoLine.getId(), Cf.list(product2.getId()));

        PromoTemplateEntity promo = promoHelper.createGlobalPromo();
        promoTemplateDao.bindProductLines(promo.getId(), promoLine.getId());

        ProductSetPojo productSet =
                productsService.getProductSetForUpgrade(TEST_UID_O, productSetKey, Option.empty(), Option.empty(),
                        Option.empty(), true, false, Option.empty(), Option.empty(), Cf.list());

        Assert.notEmpty(productSet.getActivePromo());
        PromoPojo promoPojo = productSet.getActivePromo().get();
        assertEquals(promo.getCode(), promoPojo.getKey());
        Assert.isEmpty(promoPojo.getTitle());
        Assert.isEmpty(promoPojo.getAvailableUntil());
        Assert.isEmpty(promoPojo.getPayload());
    }

    @Test
    public void gotActivePromo_MultipleFirst() {
        String productSetKey = "key";
        UserProductEntity product1 = psBillingProductsFactory.createUserProduct();
        ProductLineEntity line = psBillingProductsFactory.createProductLine(productSetKey);
        productLineDao.bindUserProducts(line.getId(), Cf.list(product1.getId()));

        UserProductEntity product2 = psBillingProductsFactory.createUserProduct();
        ProductLineEntity promoLine = psBillingProductsFactory.createProductLine(productSetKey);
        productLineDao.bindUserProducts(promoLine.getId(), Cf.list(product2.getId()));

        PromoTemplateEntity oneTimePromo = promoHelper.createGlobalPromo(x -> x
                .applicationType(PromoApplicationType.ONE_TIME)
                .toDate(Option.of(DateUtils.farFutureDate())));
        PromoTemplateEntity multipleTimePromo = promoHelper.createGlobalPromo(x -> x
                .applicationType(PromoApplicationType.MULTIPLE_TIME)
                .toDate(Option.of(DateUtils.futureDate())));
        promoTemplateDao.bindProductLines(oneTimePromo.getId(), promoLine.getId());
        promoTemplateDao.bindProductLines(multipleTimePromo.getId(), promoLine.getId());

        ProductSetPojo productSet =
                productsService.getProductSetForUpgrade(TEST_UID_O, productSetKey, Option.empty(), Option.empty(),
                        Option.empty(), true, false, Option.empty(), Option.empty(), Cf.list());

        Assert.notEmpty(productSet.getActivePromo());
        PromoPojo promoPojo = productSet.getActivePromo().get();
        assertEquals(multipleTimePromo.getCode(), promoPojo.getKey());
    }

    @Test
    public void gotActivePromo_NearestFinishFirst() {
        String productSetKey = "key";
        UserProductEntity product1 = psBillingProductsFactory.createUserProduct();
        ProductLineEntity line = psBillingProductsFactory.createProductLine(productSetKey);
        productLineDao.bindUserProducts(line.getId(), Cf.list(product1.getId()));

        UserProductEntity product2 = psBillingProductsFactory.createUserProduct();
        ProductLineEntity promoLine = psBillingProductsFactory.createProductLine(productSetKey);
        productLineDao.bindUserProducts(promoLine.getId(), Cf.list(product2.getId()));

        PromoTemplateEntity futurePromo = promoHelper.createGlobalPromo(x -> x
                .applicationType(PromoApplicationType.ONE_TIME)
                .toDate(Option.of(DateUtils.futureDate())));
        PromoTemplateEntity farFuturePromo = promoHelper.createGlobalPromo(x -> x
                .applicationType(PromoApplicationType.ONE_TIME)
                .toDate(Option.of(DateUtils.farFutureDate())));
        promoTemplateDao.bindProductLines(futurePromo.getId(), promoLine.getId());
        promoTemplateDao.bindProductLines(farFuturePromo.getId(), promoLine.getId());

        ProductSetPojo productSet =
                productsService.getProductSetForUpgrade(TEST_UID_O, productSetKey, Option.empty(), Option.empty(),
                        Option.empty(), true, false, Option.empty(), Option.empty(), Cf.list());

        Assert.notEmpty(productSet.getActivePromo());
        PromoPojo promoPojo = productSet.getActivePromo().get();
        assertEquals(futurePromo.getCode(), promoPojo.getKey());
    }

    @Test
    public void testUpgrade() {
        String productSetKey = "mail_pro_b2c";
        String productCode1 = UUID.randomUUID().toString();
        String productCode2 = UUID.randomUUID().toString();

        setupDiscountProductSet(productSetKey, productCode1, productCode2, data -> data.price(new BigDecimal(10)));
        UserProductPrice priceToBuy =
                userProductManager.findPeriodByCode(productCode1 + "_10minutes").getPrices().first();
        userProductBucketDao.addToBucket(UserProductBucketDao.InsertData.builder()
                .code("someCode").productSetId(productSetDao.findByKey(productSetKey).map(ProductSetEntity::getId)).build());

        UserService service = buy(priceToBuy);

        ProductSetPojo productSet =
                productsService.getProductSetForUpgrade(TEST_UID_O, productSetKey, Option.empty(), Option.empty(),
                        Option.empty(), true, false, Option.empty(), Option.empty(), Cf.list());

        Assert.isEmpty(productSet.getActivePromo());
        assertEquals(1, productSet.getItems().size());
        assertEquals(productCode2, productSet.getItems().get(0).getProductId());
        assertEquals(1, productSet.getItems().get(0).getPrices().size());
        assertEquals(productCode2 + "_month", productSet.getItems().get(0).getPrices().get(0).getPriceId());
        assertEquals(service.getId().toString(), productSet.getCurrentSubscription().get().getServiceId());
        assertEquals(OrderStatusApi.PAID, productSet.getOrderStatus().get());

        //    Аналогично комменту к ProductsServiceTest#testProductSetForUpgradeWithDiscountedProduct
        assertFalse(productSet.getAvailableUntil().isPresent());
    }

    @Test
    public void testUpgrade_mobile() {
        String productSetKey = "inapp_android";
        String lightProduct = "mail_pro_b2c_light_inapp_google";
        String standardProduct = "mail_pro_b2c_standard100_inapp_google";
        Option<String> diskPackageName = Option.of("ru.yandex.disk");
        Option<String> mailPackageName = Option.of("ru.yandex.mail");

        setupDiscountProductSet(productSetKey, lightProduct, standardProduct, data -> data.price(new BigDecimal(10)));
        userProductBucketDao.addToBucket(UserProductBucketDao.InsertData.builder()
                .code("someCode").productSetId(productSetDao.findByKey(productSetKey).map(ProductSetEntity::getId)).build());

        UserProductPrice priceToBuy =
                userProductManager.findPeriodByCode(lightProduct + "_10minutes").getPrices().first();
        UserService service = buy(priceToBuy, diskPackageName);

        ProductSetPojo productSet =
                productsService.getProductSetForUpgrade(TEST_UID_O, productSetKey, diskPackageName, Option.empty(),
                        Option.empty(), true, false, Option.empty(), Option.empty(), Cf.list());

        Assert.isEmpty(productSet.getActivePromo());
        assertEquals(1, productSet.getItems().size());
        assertEquals(standardProduct, productSet.getItems().get(0).getProductId());
        assertEquals(1, productSet.getItems().get(0).getPrices().size());
        assertEquals(standardProduct + "_month", productSet.getItems().get(0).getPrices().get(0).getPriceId());
        assertEquals(service.getId().toString(), productSet.getCurrentSubscription().get().getServiceId());
        assertEquals(OrderStatusApi.PAID, productSet.getOrderStatus().get());
        //    Аналогично комменту к ProductsServiceTest#testProductSetForUpgradeWithDiscountedProduct
        assertFalse(productSet.getAvailableUntil().isPresent());

        productSet = productsService.getProductSetForUpgrade(TEST_UID_O, productSetKey, mailPackageName, Option.empty()
                , Option.empty(), true, false, Option.empty(), Option.empty(), Cf.list());
        assertEquals(0, productSet.getItems().size());
    }

    //    Ожидается, что дата, по которую доступена линейка (при наличии таковой) будет проставляться только если в
    //    списке продуктов есть продукт со скидкой или неиспользованным триалом
    //    Подробности зачем так сделано тут https://st.yandex-team.ru/CHEMODAN-77238
    @Test
    public void testProductSetForUpgradeWithDiscountedProduct() {
        String productSetKey = "mail_pro_b2c";
        String productCode1 = UUID.randomUUID().toString();
        String productCode2 = UUID.randomUUID().toString();

        setupDiscountProductSet(productSetKey, productCode1, productCode2, data -> data
                .price(new BigDecimal(11))
                .displayOriginalPrice(Option.of(new BigDecimal(22)))
                .displayDiscountPercent(Option.of(new BigDecimal(50))));
        UserProductPrice priceToBuy =
                userProductManager.findPeriodByCode(productCode1 + "_10minutes").getPrices().first();
        userProductBucketDao.addToBucket(UserProductBucketDao.InsertData.builder()
                .code("someCode").productSetId(productSetDao.findByKey(productSetKey).map(ProductSetEntity::getId)).build());

        UserService service = buy(priceToBuy);

        ProductSetPojo productSet =
                productsService.getProductSetForUpgrade(TEST_UID_O, productSetKey, Option.empty(), Option.empty(),
                        Option.empty(), true, false, Option.empty(), Option.empty(), Cf.list());

        Assert.isEmpty(productSet.getActivePromo());
        assertEquals(1, productSet.getItems().size());
        assertEquals(productCode2, productSet.getItems().get(0).getProductId());
        assertEquals(1, productSet.getItems().get(0).getPrices().size());
        assertEquals(productCode2 + "_month", productSet.getItems().get(0).getPrices().get(0).getPriceId());
        assertEquals(service.getId().toString(), productSet.getCurrentSubscription().get().getServiceId());
        assertEquals(OrderStatusApi.PAID, productSet.getOrderStatus().get());
        assertTrue(productSet.getAvailableUntil().isPresent());
        assertEquals(productSet.getAvailableUntil().get(), Instant.parse("2121-02-01"));
    }

    @Test
    public void testGetProductsReadonly() {
        MasterSlaveContextHolder.withPolicy(MasterSlavePolicy.R_SYNC_SM, this::discountProductSetSerializationTest);
    }

    @Test
    public void testProductsWithConflictingHold() {
        String productSetKey = "mail_pro_b2c";
        prepareTestProductsWithHold(true, productSetKey);
        // не должно быть доступно продуктов для покупки
        ProductSetPojo productSet = productsService.getProductSetForUpgrade(TEST_UID_O, productSetKey, Option.empty(), Option.empty(),
                Option.empty(), true, false, Option.empty(), Option.empty(), Cf.list());

        assertEquals(0, productSet.getItems().size());
        assertTrue(productSet.getCurrentSubscription().isPresent());
        assertEquals(userServiceManager.find(TEST_UID.toString(), Option.empty()).get(0).getId().toString(), productSet.getCurrentSubscription().get().getServiceId());
        assertEquals(OrderStatusApi.ON_HOLD, productSet.getOrderStatus().get());

        // и в старой ручке
        productSet = productsService.getProductSet(TEST_UID_O, productSetKey, Option.empty(), Option.empty(), false,
                false, Cf.list());

        assertEquals(0, productSet.getItems().size());
        assertFalse(productSet.getCurrentSubscription().isPresent());
    }

    @Test
    public void testProductsWithNotConflictingHold() {
        String productSetKey = "mail_pro_b2c";
        prepareTestProductsWithHold(false, productSetKey);

        // должны быть доступно продуктов для покупки
        ProductSetPojo productSet = productsService.getProductSetForUpgrade(TEST_UID_O, productSetKey, Option.empty(), Option.empty(),
                Option.empty(), true, false, Option.empty(), Option.empty(), Cf.list());

        assertEquals(1, productSet.getItems().size());
        assertTrue(productSet.getCurrentSubscription().isEmpty());
        assertTrue(productSet.getOrderStatus().isEmpty());

        // и в старой ручке
        productSet = productsService.getProductSet(TEST_UID_O, productSetKey, Option.empty(), Option.empty(), false,
                false, Cf.list());

        assertEquals(0, productSet.getItems().size());
        assertFalse(productSet.getCurrentSubscription().isPresent());
    }

    private void prepareTestProductsWithHold(boolean conflicts, String productSetKey) {
        UUID userProduct1 = productsFactory.createUserProductWithPrice(
                data -> data.billingType(BillingType.INAPP_GOOGLE)).getId();
        UUID userProduct2 = productsFactory.createUserProductWithPrice(
                data -> data.billingType(conflicts ? BillingType.INAPP_GOOGLE: BillingType.TRUST)).getId();

        List<Tuple2<UUID, String>> buckets = new ArrayList<>();
        if(conflicts) {
            buckets.add(new Tuple2<>(userProduct1, "someCode"));
            buckets.add(new Tuple2<>(userProduct2, "someCode"));
            productsFactory.addUserProductToProductSet(productSetKey, userProduct1, userProduct2);
        } else {
            buckets.add(new Tuple2<>(userProduct1, "someCode1"));
            buckets.add(new Tuple2<>(userProduct2, "someCode2"));
            productsFactory.addUserProductToProductSet(productSetKey, userProduct1);
            productsFactory.addUserProductToProductSet("other", userProduct2);
        }
        UserProductPrice priceToBuy =
                userProductManager.findById(userProduct2).getProductPeriods().first().getPrices().first();

        for (Tuple2<UUID, String> bucket : buckets) {
            userProductBucketDao.addToBucket(UserProductBucketDao.InsertData.builder()
                    .code(bucket._2).userProductId(Option.of(bucket._1)).build());
        }

        // покупаем один продукт
        Order order = createOrder(priceToBuy, Option.empty());
        UserService service = createUserService(order);

        // иимитируем OnHold купленной подписки
        userServiceManager.disableService(service.getId());
        orderDao.holdOrder(order.getId());
    }

    @Test
    public void testCurrency_DefaultCurrency() {
        String regionId = "84";
        mockUserRegion(regionId, "us");

        UserProductEntity product = productsFactory.createUserProduct();
        UserProductPeriodEntity period = productsFactory.createUserProductPeriod(product);
        productsFactory.createUserProductPrices(period.getId(),
                x -> x.regionId(UserProductPrice.DEFAULT_REGION).currencyCode("RUB"));
        productsFactory.createUserProductPrices(period.getId(),
                x -> x.regionId(UserProductPrice.DEFAULT_REGION).currencyCode("USD"));
        productsFactory.createUserProductPrices(period.getId(),
                x -> x.regionId(regionId).currencyCode("USD"));
        productsFactory.createProductLine("set", x -> x, product);

        // валюта по умолчанию - RUB
        ProductSetPojo set = productsService.getProductSetForUpgrade(TEST_UID_O, "set", Option.empty(), Option.empty(),
                Option.empty(), true, false, Option.empty(), Option.empty(), Cf.list());
        List<ProductPricePojo> prices = set.getItems().get(0).getPrices();
        Assert.equals(1, prices.size());
        Assert.equals("RUB", prices.get(0).getCurrency().get());
    }

    @Test
    public void testCurrency_UserRegionPrice() {
        String regionId = "84";
        mockUserRegion(regionId, "us");

        BigDecimal defaultRegionPrice = new BigDecimal(10000);
        BigDecimal regionPrice = new BigDecimal(10);

        UserProductEntity product = productsFactory.createUserProduct();
        UserProductPeriodEntity period = productsFactory.createUserProductPeriod(product);
        productsFactory.createUserProductPrices(period.getId(),
                x -> x.regionId(UserProductPrice.DEFAULT_REGION).currencyCode("RUB"));
        productsFactory.createUserProductPrices(period.getId(),
                x -> x.regionId(UserProductPrice.DEFAULT_REGION).currencyCode("USD").price(defaultRegionPrice));
        productsFactory.createUserProductPrices(period.getId(),
                x -> x.regionId(regionId).currencyCode("USD").price(regionPrice));
        productsFactory.createProductLine("set", x -> x, product);

        // нашли цену для региона пользователя в заданной валюте
        ProductSetPojo set = productsService.getProductSetForUpgrade(TEST_UID_O, "set", Option.empty(), Option.empty(),
                Option.of("USD"), true, false, Option.empty(), Option.empty(), Cf.list());
        List<ProductPricePojo> prices = set.getItems().get(0).getPrices();
        Assert.equals(1, prices.size());
        Assert.equals("USD", prices.get(0).getCurrency().get());
        Assert.equals(regionPrice, prices.get(0).getAmount().get());
    }

    @Test
    public void testCurrency_UserRegionPriceNotFound() {
        String regionId = "84";
        mockUserRegion(regionId, "us");

        BigDecimal defaultRegionPrice = new BigDecimal(10000);
        BigDecimal regionPrice = new BigDecimal(10);

        UserProductEntity product = productsFactory.createUserProduct();
        UserProductPeriodEntity period = productsFactory.createUserProductPeriod(product);
        productsFactory.createUserProductPrices(period.getId(),
                x -> x.regionId(UserProductPrice.DEFAULT_REGION).currencyCode("RUB"));
        productsFactory.createUserProductPrices(period.getId(),
                x -> x.regionId(UserProductPrice.DEFAULT_REGION).currencyCode("USD").price(defaultRegionPrice));
        productsFactory.createUserProductPrices(period.getId(),
                x -> x.regionId("99").currencyCode("USD").price(regionPrice));
        productsFactory.createProductLine("set", x -> x, product);

        // не нашли цену для региона пользователя в заданной валюте - возвращаем цену для региона по умолчанию
        ProductSetPojo set = productsService.getProductSetForUpgrade(TEST_UID_O, "set", Option.empty(), Option.empty(),
                Option.of("USD"), true, false, Option.empty(), Option.empty(), Cf.list());
        List<ProductPricePojo> prices = set.getItems().get(0).getPrices();
        Assert.equals(1, prices.size());
        Assert.equals("USD", prices.get(0).getCurrency().get());
        Assert.equals(defaultRegionPrice, prices.get(0).getAmount().get());
    }

    @Test
    public void testCurrency_CurrencyNotFound() {
        String regionId = "84";
        mockUserRegion(regionId, "us");

        UserProductEntity product = productsFactory.createUserProduct();
        UserProductPeriodEntity period = productsFactory.createUserProductPeriod(product);
        productsFactory.createUserProductPrices(period.getId(),
                x -> x.regionId(UserProductPrice.DEFAULT_REGION).currencyCode("RUB"));
        productsFactory.createUserProductPrices(period.getId(),
                x -> x.regionId(UserProductPrice.DEFAULT_REGION).currencyCode("USD"));
        productsFactory.createUserProductPrices(period.getId(),
                x -> x.regionId(regionId).currencyCode("USD"));
        productsFactory.createProductLine("set", x -> x, product);

        // не нашли цену в заданной валюте - возвращаем пустой список
        ProductSetPojo set = productsService.getProductSetForUpgrade(TEST_UID_O, "set", Option.empty(), Option.empty(),
                Option.of("CHF"), true, false, Option.empty(), Option.empty(), Cf.list());
        Assert.isTrue(set.getItems().isEmpty());
    }

    @Test
    public void testCurrency_UserRegionDisabled() {
        String regionId = "84";
        mockUserRegion(regionId, "us");

        BigDecimal defaultRegionPrice = new BigDecimal(10000);
        BigDecimal regionPrice = new BigDecimal(10);

        UserProductEntity product = productsFactory.createUserProduct();
        UserProductPeriodEntity period = productsFactory.createUserProductPeriod(product);
        productsFactory.createUserProductPrices(period.getId(),
                x -> x.regionId(UserProductPrice.DEFAULT_REGION).currencyCode("RUB"));
        productsFactory.createUserProductPrices(period.getId(),
                x -> x.regionId(UserProductPrice.DEFAULT_REGION).currencyCode("USD").price(defaultRegionPrice));
        productsFactory.createUserProductPrices(period.getId(),
                x -> x.regionId(regionId).currencyCode("USD").price(regionPrice));
        productsFactory.createProductLine("set", x -> x, product);

        Mockito.when(settings.getDisabledPriceRegions()).thenReturn(Cf.hashSet(regionId, "100500"));

        // регион пользователя задизейблен для цен - возвращаем цену для региона по умолчанию
        ProductSetPojo set = productsService.getProductSetForUpgrade(TEST_UID_O, "set", Option.empty(), Option.empty(),
                Option.of("USD"), true, false, Option.empty(), Option.empty(), Cf.list());
        List<ProductPricePojo> prices = set.getItems().get(0).getPrices();
        Assert.equals(1, prices.size());
        Assert.equals("USD", prices.get(0).getCurrency().get());
        Assert.equals(defaultRegionPrice, prices.get(0).getAmount().get());
    }

    @Test
    public void testCurrency_UserHasConflictingService() {
        UserProductEntity currentProduct = productsFactory.createUserProduct();
        UserProductPrice price = userProductManager.findPrice(
                productsFactory.createUserProductPrices(currentProduct, CustomPeriodUnit.TEN_MINUTES).getId());
        UserProductPrice usdPrice = userProductManager.findPrice(
                productsFactory.createUserProductPrices(price.getPeriod().getId(),
                        x -> x.regionId(UserProductPrice.DEFAULT_REGION).currencyCode("USD")).getId());
        Order order = psBillingOrdersFactory.createOrUpdateOrder(TEST_UID_O.get(), usdPrice.getId(), "test_order");
        productsFactory.addProductsToBucket("bucket", currentProduct.getId());
        userServiceManager.createUserService(order, Instant.now(), UserServiceBillingStatus.PAID);

        UserProductEntity upgradeProduct = productsFactory.createUserProduct();
        UserProductPeriodEntity period = productsFactory.createUserProductPeriod(upgradeProduct, CustomPeriodUnit.ONE_MONTH);
        productsFactory.createUserProductPrices(period.getId(),
                x -> x.regionId(UserProductPrice.DEFAULT_REGION).currencyCode("RUB").price(BigDecimal.valueOf(100)));
        productsFactory.createUserProductPrices(period.getId(),
                x -> x.regionId(UserProductPrice.DEFAULT_REGION).currencyCode("USD").price(BigDecimal.valueOf(100)));
        productsFactory.createProductLine("set", x -> x, upgradeProduct);
        productsFactory.addProductsToBucket("bucket", upgradeProduct.getId());

        // у пользователя есть покупка в USD, возвращаем ему USD цену
        ProductSetPojo set = productsService.getProductSetForUpgrade(TEST_UID_O, "set", Option.empty(), Option.empty(),
                Option.of("RUB"), true, false, Option.empty(), Option.empty(), Cf.list());
        List<ProductPricePojo> prices = set.getItems().get(0).getPrices();
        Assert.equals(1, prices.size());
        Assert.equals("USD", prices.get(0).getCurrency().get());
    }

    @Test
    public void testProductWithStartPeriod() {
        UserProductEntity product = productsFactory.createUserProduct();
        int startPeriodCount = 1;
        BigDecimal fullPrice = BigDecimal.TEN;
        BigDecimal startPrice = BigDecimal.ONE;
        CustomPeriod startPeriodDuration = new CustomPeriod(CustomPeriodUnit.ONE_MONTH, 1);
        productsFactory.createUserProductPrices(product, CustomPeriodUnit.ONE_MONTH, fullPrice, startPeriodDuration,
                startPeriodCount, startPrice);
        productsFactory.createProductLine("set", x -> x, product);
        ProductSetPojo set = productsService.getProductSet(TEST_UID_O, "set",
                Option.empty(), Option.empty(), true, true, Cf.list());
        List<ProductWithPricesPojo> products = set.getItems();
        Assert.assertEquals(1, products.size());
        List<ProductPricePojo> prices = products.get(0).getPrices();
        Assert.assertEquals(1, prices.size());
        ProductPricePojo productPricePojo = prices.get(0);
        Assert.equals(fullPrice, productPricePojo.getAmount().get());
        ProductPricePojo.StartDiscount startDiscount = productPricePojo.getStartDiscount().get();
        Assert.equals(CustomPeriodApi.fromCoreEnum(startPeriodDuration.getUnit()),
                startDiscount.getPeriod().getPeriod());
        Assert.equals(startPeriodDuration.getValue(), startDiscount.getPeriod().getPeriodLength());
        Assert.equals(startPrice, startDiscount.getPrice());
        Assert.equals(startPeriodCount, startDiscount.getPeriodsCount());
    }

    @Test
    public void noAvailableInappIfWebBought() {
        usersFactory.createUserServiceWithOrder(Target.ENABLED, TEST_UID);
        //mockUserRegion("225", "ru");

        String productSetKey = "inapp_android";
        String lightProduct = "mail_pro_b2c_light_inapp_google";
        String standardProduct = "mail_pro_b2c_standard100_inapp_google";
        Option<String> diskPackageName = Option.of("ru.yandex.disk");

        setupDiscountProductSet(productSetKey, lightProduct,Function.identityF(), standardProduct, Function.identityF(), BillingType.INAPP_APPLE);
        userProductBucketDao.addToBucket(UserProductBucketDao.InsertData.builder()
                .code("someCode").productSetId(productSetDao.findByKey(productSetKey).map(ProductSetEntity::getId)).build());

        ProductSetPojo productSet =
                productsService.getProductSetForUpgrade(Option.of(TEST_UID), productSetKey, diskPackageName, Option.empty(),
                        Option.empty(), true, false, Option.empty(), Option.empty(), Cf.list());
        Assert.assertTrue(productSet.getItems().isEmpty());
    }

    @Test
    public void groupProductDoNotBlockInapp() {
        UserProductEntity userProduct = productsFactory.createUserProductWithPrice(product -> product.billingType(BillingType.GROUP));
        UUID priceId = userProductManager.findById(userProduct.getId()).getProductPeriods().first().getPrices().first().getId();
        usersFactory.createUserService(Target.ENABLED, psBillingOrdersFactory.createOrUpdateOrder(TEST_UID, priceId, ""+(int)(Math.random()*10000)));
        //mockUserRegion("225", "ru");

        String productSetKey = "inapp_android";
        String lightProduct = "mail_pro_b2c_light_inapp_google";
        String standardProduct = "mail_pro_b2c_standard100_inapp_google";
        Option<String> diskPackageName = Option.of("ru.yandex.disk");

        setupDiscountProductSet(productSetKey, lightProduct,Function.identityF(), standardProduct, Function.identityF(), BillingType.INAPP_APPLE);
        userProductBucketDao.addToBucket(UserProductBucketDao.InsertData.builder()
                .code("someCode").productSetId(productSetDao.findByKey(productSetKey).map(ProductSetEntity::getId)).build());

        ProductSetPojo productSet =
                productsService.getProductSetForUpgrade(Option.of(TEST_UID), productSetKey, diskPackageName, Option.empty(),
                        Option.empty(), true, false, Option.empty(), Option.empty(), Cf.list());
        Assert.assertFalse(productSet.getItems().isEmpty());
    }

    @Test
    public void testMapNextPayment_inStartPeriod() {
        testMapNextPaymentImpl(3, 1, true, BillingType.TRUST);
    }

    @Test
    public void testMapNextPayment_outOfStartPeriod() {
        testMapNextPaymentImpl(3, 3, false, BillingType.TRUST);
    }

    @Test
    public void testMapNextPayment_noStartPeriod() {
        testMapNextPaymentImpl(0, 3, false, BillingType.TRUST);
    }

    @Test
    public void testMapNextPayment_nonTrust() {
        testMapNextPaymentImpl(0, 3, false, BillingType.INAPP_APPLE);
    }

    private void testMapNextPaymentImpl(int startPeriodCount, int subscriptionsCount,
                                        boolean startPeriodPriceExpected, BillingType billingType) {
        UserProductPriceEntity priceEntity;
        if (startPeriodCount > 0 && billingType == BillingType.TRUST)
            priceEntity = productsFactory.createUserProductPrices(CustomPeriodUnit.ONE_MONTH,
                    BigDecimal.TEN, CustomPeriod.fromMonths(1), startPeriodCount, BigDecimal.ONE);
        else
            priceEntity = productsFactory.createUserProductPrices(
                    productsFactory.createUserProduct(builder -> builder.billingType(billingType)),
                    CustomPeriodUnit.ONE_MONTH, BigDecimal.TEN);
        UserProductPrice price = userProductManager.findPrice(priceEntity.getId());
        Order order = createOrder(price, Option.of("package"));
        UserService userService = createUserService(order);
        orderDao.updateSubscriptionsCount(order.getId(), subscriptionsCount);
        Option<UserServicePojo.PaymentPojo> paymentPojos = productsService.mapNextPayment(userService);
        boolean paymentPojoExptect = billingType == BillingType.TRUST;
        Assert.equals(paymentPojoExptect, paymentPojos.isPresent());
        if (paymentPojoExptect) {
            Assert.equals(price.getCurrencyCode(), paymentPojos.get().getCurrency());
            BigDecimal expectedPrice = startPeriodPriceExpected ? price.getStartPeriodPrice().get() : price.getPrice();
            Assert.equals(expectedPrice, paymentPojos.get().getAmount());
        }
    }

    private void setupDiscountProductSet(String productSetKey, String product1Code,
                                         Function<UserProductPricesDao.InsertData.InsertDataBuilder,
                                                 UserProductPricesDao.InsertData.InsertDataBuilder> product1Customizer,
                                         String product2Code,
                                         Function<UserProductPricesDao.InsertData.InsertDataBuilder,
                                                 UserProductPricesDao.InsertData.InsertDataBuilder> product2Customizer) {
        setupDiscountProductSet(productSetKey, product1Code, product1Customizer, product2Code, product2Customizer,
                BillingType.TRUST);
    }

    private void setupDiscountProductSet(String productSetKey, String product1Code,
                                         Function<UserProductPricesDao.InsertData.InsertDataBuilder,
                                                 UserProductPricesDao.InsertData.InsertDataBuilder> product1Customizer,
                                         String product2Code,
                                         Function<UserProductPricesDao.InsertData.InsertDataBuilder,
                                                 UserProductPricesDao.InsertData.InsertDataBuilder> product2Customizer, BillingType billingType) {
        TankerKeyEntity tankerKey = textsFactory.create();
        TankerKeyEntity tankerKey2 = textsFactory.create2();
        textsManager.updateTranslations();

        UserProductEntity product1 =
                productsFactory.createUserProduct(data -> data.titleTankerKeyId(tankerKey.getId()).code(product1Code).billingType(billingType));
        UserProductPeriodEntity periodEntity1 = productsFactory.createUserProductPeriod(product1);
        productsFactory.createUserProductPrices(periodEntity1.getId(), product1Customizer);

        UserProductEntity product2 =
                productsFactory.createUserProduct(data -> data.titleTankerKeyId(tankerKey.getId()).code(product2Code).billingType(billingType));
        UserProductPeriodEntity periodEntity2 = productsFactory.createUserProductPeriod(product2,
                CustomPeriodUnit.ONE_MONTH);
        productsFactory.createUserProductPrices(periodEntity2.getId(), product2Customizer);


        ProductSetEntity productSet =
                productSetDao.create(ProductSetDao.InsertData.builder().key(productSetKey).build());
        ProductLineEntity availableProductLine = productLineDao
                .create(ProductLineDao.InsertData.builder().productSetId(productSet.getId()).orderNum(1)
                        .selectorBeanEL("productLineSelectorFactory.availableUntilSelector(\"2121-02-01\", " +
                                "productLineSelectorFactory.availableSelector())").build());

        productLineDao.bindUserProducts(availableProductLine.getId(), Cf.list(product1.getId(), product2.getId()));


        FeatureEntity feature1 = productsFactory.createFeature(FeatureType.TOGGLEABLE);
        FeatureEntity feature2 = productsFactory.createFeature(FeatureType.TOGGLEABLE);

        productsFactory.createProductFeature(product1.getId(), feature1,
                data -> data.descriptionTankerKeyId(Option.of(tankerKey.getId())).code("code1").orderNum(1));
        productsFactory.createProductFeature(product2.getId(), feature2,
                data -> data.descriptionTankerKeyId(Option.of(tankerKey2.getId())).code("code2").orderNum(2));

    }

    private void setupDiscountProductSet(String productSetKey, UUID product1Code, UUID product2Code,
                                         Function<UserProductPricesDao.InsertData.InsertDataBuilder,
                                                 UserProductPricesDao.InsertData.InsertDataBuilder> product2Customizer) {
        setupDiscountProductSet(productSetKey, product1Code.toString(), product2Code.toString(), product2Customizer);
    }

    private void setupDiscountProductSet(String productSetKey, String product1Code, String product2Code,
                                         Function<UserProductPricesDao.InsertData.InsertDataBuilder,
                                                 UserProductPricesDao.InsertData.InsertDataBuilder> product2Customizer) {
        setupDiscountProductSet(productSetKey, product1Code,
                data -> data
                        .price(new BigDecimal(9))
                        .displayOriginalPrice(Option.of(new BigDecimal(20)))
                        .displayDiscountPercent(Option.of(new BigDecimal(50))),
                product2Code, product2Customizer);
    }

    private UserService buy(UserProductPrice price) {
        return buy(price, Option.empty());
    }

    private UserService buy(UserProductPrice price, Option<String> packageName) {
        Order order = createOrder(price, packageName);
        return createUserService(order);
    }

    private Order createOrder(UserProductPrice price, Option<String> packageName) {
        return orderDao.createOrUpdate(OrderDao.InsertData.builder()
                .uid(TEST_UID.toString())
                .trustServiceId(116)
                .trustOrderId("111")
                .userProductPriceId(price.getId())
                .type(price.getPeriod().getUserProduct().getBillingType() == BillingType.TRUST ? OrderType.SUBSCRIPTION
                        : OrderType.INAPP_SUBSCRIPTION)
                .packageName(packageName)
                .build());
    }

    private UserService createUserService(Order order) {
        UserService userService = userServiceManager.createUserService(order,
                Instant.now().plus(Duration.standardDays(5)),
                UserServiceBillingStatus.PAID);
        orderDao.onSuccessfulOrderPurchase(order.getId(), Option.of(userService.getId()), 1);

        return userService;
    }

    private void mockUserRegion(String regionId, String countryCode) {
        blackbox2MockConfig.mockUserInfo(TEST_UID_O.get(), Blackbox2MockConfiguration.getBlackboxResponse("login",
                "firstName", Option.of("displayName"), Option.empty(), Option.empty(), Option.empty(),
                Option.of(countryCode)));
        userInfoService.addRegionCode(regionId, countryCode);
    }
}
