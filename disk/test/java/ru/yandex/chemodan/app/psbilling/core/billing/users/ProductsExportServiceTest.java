package ru.yandex.chemodan.app.psbilling.core.billing.users;

import java.math.BigDecimal;

import org.joda.time.DateTimeUtils;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriod;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.products.BillingType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPeriodEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPriceEntity;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductManager;
import ru.yandex.chemodan.trust.client.TrustClient;
import ru.yandex.chemodan.trust.client.requests.CreateProductRequest;
import ru.yandex.chemodan.trust.client.requests.LocalName;
import ru.yandex.chemodan.trust.client.requests.Price;
import ru.yandex.devtools.test.annotations.YaExternal;
import ru.yandex.misc.lang.Validate;

import static ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice.DEFAULT_REGION;

@YaExternal
public class ProductsExportServiceTest extends AbstractPsBillingCoreTest {
    @Autowired
    private ProductsExportService productsExportService;

    private static final LocalDate SOME_DATE = LocalDate.parse("2020-01-08");
    private static final Instant FAKE_NOW = new Instant(SOME_DATE.toDate());

    @Autowired
    private TrustClient trustClient;
    @Autowired
    private UserProductManager userProductManager;

    @After
    public void after() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testInappSubscriptionExport() {
        DateTimeUtils.setCurrentMillisFixed(SOME_DATE.toDate().getTime());
        UserProductEntity userProduct = psBillingProductsFactory.createUserProduct(builder -> {
            builder.billingType(BillingType.INAPP_GOOGLE);
            builder.code("TEST");
            builder.allowAutoProlong(true);
            builder.titleTankerKeyId(psBillingTextsFactory.create().getId());
            builder.trustServiceId(Option.of(1111));
            return builder;
        });
        psBillingTextsFactory.loadTranslations();
        UserProductPeriodEntity productPeriod = psBillingProductsFactory.createUserProductPeriod(
                userProduct, CustomPeriodUnit.TEN_MINUTES, b -> b.packageName(Option.of("package")));

        psBillingProductsFactory.createUserProductPrices(productPeriod.getId(), Function.identityF());
        psBillingProductsFactory.createUserProductPrices(productPeriod.getId(),
                b -> b.regionId(DEFAULT_REGION));

        productsExportService.exportProduct(userProductManager.findById(userProduct.getId()));

        validateCallsForInapp();
    }

    @Test
    public void testTrustSubscriptionExport() {
        DateTimeUtils.setCurrentMillisFixed(SOME_DATE.toDate().getTime());
        UserProductEntity userProduct = psBillingProductsFactory.createUserProduct(builder -> {
            builder.billingType(BillingType.TRUST);
            builder.code("TEST");
            builder.allowAutoProlong(true);
            builder.titleTankerKeyId(psBillingTextsFactory.create().getId());
            builder.trustServiceId(Option.of(1111));
            builder.trustSubsChargingRetryLimit(Option.of("4D"));
            builder.trustSubsChargingRetryDelay(Option.of("1D"));
            builder.trustSubsGracePeriod(Option.of("2Y"));
            return builder;
        });
        psBillingTextsFactory.loadTranslations();
        UserProductPriceEntity userProductPrices =
                psBillingProductsFactory.createUserProductPrices(userProduct, CustomPeriodUnit.TEN_MINUTES);
        psBillingProductsFactory.createUserProductPrices(userProductPrices.getUserProductPeriodId(),
                b -> b.regionId(DEFAULT_REGION));

        productsExportService.exportProduct(userProductManager.findById(userProduct.getId()));

        validateCallsForSubscription();
    }

    @Test
    public void testTrustSubscriptionWithStartPeriod() {
        UserProductEntity userProduct = psBillingProductsFactory.createUserProduct(builder -> {
            builder.billingType(BillingType.TRUST);
            builder.code("START_PERIOD_TEST");
            builder.allowAutoProlong(true);
            builder.titleTankerKeyId(psBillingTextsFactory.create().getId());
            builder.trustServiceId(Option.of(1111));
            builder.trustSubsChargingRetryLimit(Option.of("4D"));
            builder.trustSubsChargingRetryDelay(Option.of("1D"));
            builder.trustSubsGracePeriod(Option.of("2Y"));
            return builder;
        });
        CustomPeriod startPeriodDuration = new CustomPeriod(CustomPeriodUnit.ONE_MONTH, 1);
        psBillingProductsFactory.createUserProductPrices(userProduct, CustomPeriodUnit.ONE_MONTH,
                BigDecimal.TEN, startPeriodDuration, 1, BigDecimal.ONE);

        productsExportService.exportProduct(userProductManager.findById(userProduct.getId()));

        ArgumentCaptor<CreateProductRequest> captor = ArgumentCaptor.forClass(CreateProductRequest.class);
        Mockito.verify(trustClient, Mockito.times(2)).createProduct(captor.capture());
        captor.getAllValues().stream()
                .filter(request -> request.getProductType().equals("subs"))
                .forEach(request -> {
                    BigDecimal startPrice = request.getStartPeriodPrices().get(0).getPrice();
                    Validate.equals(startPrice, BigDecimal.ONE);
                    Validate.equals(request.getSubscriptionStartPeriodCount().longValue(), 1L);
                    Validate.equals(request.getSubscriptionStartPeriod(), startPeriodDuration.toTrustPeriod());
                });
    }


    private void validateCallsForSubscription() {
        Mockito.verify(trustClient).createProduct(CreateProductRequest.builder()
                .trustServiceId(1111)
                .prices(Cf.list(
                        new Price("225", FAKE_NOW, BigDecimal.TEN, "RUB"),
                        new Price("10000", FAKE_NOW, BigDecimal.TEN, "RUB")))
                .productId("PS_BILLING_TEST_10minutes_app")
                .name("PS_BILLING_TEST_10minutes_app")
                .productType("app")
                .fiscalNds("nds_20_120")
                .fiscalTitle("Fiscal_title")
                .localNames(Cf.list(
                        new LocalName("ru", "Тестовый ключ"),
                        new LocalName("uk", "тестовий ключ"),
                        new LocalName("en", "test key"),
                        new LocalName("tr", "Test anahtarı")))
                .build());
        Mockito.verify(trustClient).createProduct(CreateProductRequest.builder()
                .trustServiceId(1111)
                .prices(Cf.list(
                        new Price("225", FAKE_NOW, BigDecimal.TEN, "RUB"),
                        new Price("10000", FAKE_NOW, BigDecimal.TEN, "RUB")))
                .productId("TEST_10minutes")
                .name("TEST_10minutes")
                .productType("subs")
                .fiscalNds("nds_20_120")
                .fiscalTitle("Fiscal_title")
                .localNames(Cf.list(
                        new LocalName("ru", "Тестовый ключ"),
                        new LocalName("uk", "тестовий ключ"),
                        new LocalName("en", "test key"),
                        new LocalName("tr", "Test anahtarı")))
                .subscriptionPeriod("600S")
                .parentProductId("PS_BILLING_TEST_10minutes_app")
                .subscriptionGracePeriod("2Y")
                .subscriptionRetryChargingDelay("1D")
                .subscriptionRetryChargingLimit("4D")
                .build()
        );

        Mockito.verifyNoMoreInteractions(trustClient);
    }

    private void validateCallsForInapp() {
        Mockito.verify(trustClient).createProduct(CreateProductRequest.builder()
                .trustServiceId(1111)
                .prices(Cf.list(
                        new Price("225", FAKE_NOW, BigDecimal.TEN, "RUB"),
                        new Price("10000", FAKE_NOW, BigDecimal.TEN, "RUB")))
                .productId("TEST_10minutes")
                .name("TEST_10minutes")
                .productType("inapp_subs")
                .packageName("package")
                .localNames(Cf.list(
                        new LocalName("ru", "Тестовый ключ"),
                        new LocalName("uk", "тестовий ключ"),
                        new LocalName("en", "test key"),
                        new LocalName("tr", "Test anahtarı")
                ))
                .build()
        );

        Mockito.verifyNoMoreInteractions(trustClient);
    }

    @Before
    public void initialize() {
        super.initialize();
        textsManagerMockConfig.reset();
    }
}
