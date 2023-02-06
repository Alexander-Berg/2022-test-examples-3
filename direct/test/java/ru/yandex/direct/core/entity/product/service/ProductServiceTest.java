package ru.yandex.direct.core.entity.product.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.adgroup.model.ProductRestrictionKey;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.product.model.Product;
import ru.yandex.direct.core.entity.product.model.ProductRestriction;
import ru.yandex.direct.core.entity.product.model.ProductSimple;
import ru.yandex.direct.core.entity.product.model.ProductType;
import ru.yandex.direct.core.entity.product.repository.ProductRepository;
import ru.yandex.direct.core.entity.product.repository.ProductsCache;
import ru.yandex.direct.core.testing.data.TestProducts;
import ru.yandex.direct.currency.CurrencyCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.product.service.ProductService.BUCKS;
import static ru.yandex.direct.core.entity.product.service.ProductService.QUASI_CURRENCY;

public class ProductServiceTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    public static final Product PRODUCT_WITH_QUASI_CURRENCY = new Product()
            .withId(3L)
            .withCurrencyCode(CurrencyCode.KZT)
            .withType(ProductType.TEXT)
            .withUnitName(QUASI_CURRENCY);
    public static final Product PRODUCT_WITHOUT_QUASI_CURRENCY = new Product()
            .withId(4L)
            .withCurrencyCode(CurrencyCode.RUB)
            .withType(ProductType.TEXT)
            .withUnitName(BUCKS);
    private ProductService serviceUnderTest;

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductsCache productsCache;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        serviceUnderTest = new ProductService(productsCache, repository, null);

        when(repository.getAllProducts()).thenReturn(Arrays.asList(
                new Product().withId(1L).withDailyShows(123L),
                new Product().withId(2L).withEngineId(5L),
                PRODUCT_WITH_QUASI_CURRENCY,
                PRODUCT_WITHOUT_QUASI_CURRENCY
        ));
    }

    @Test(expected = NoSuchElementException.class)
    public void testNoElement() {
        serviceUnderTest.getProductById(Long.MAX_VALUE);
    }

    @Test
    public void testGetElementById() {
        ProductSimple product = serviceUnderTest.getProductById(2L);
        assertThat(product).isEqualTo(new Product().withId(2L).withEngineId(5L));
    }

    @Test
    public void testGetFromCache() {
        ProductSimple product = serviceUnderTest.getProductById(2L);
        assertThat(product).isEqualTo(new Product().withId(2L).withEngineId(5L));

        product = serviceUnderTest.getProductById(1L);
        assertThat(product).isEqualTo(new Product().withId(1L).withDailyShows(123L));

        product = serviceUnderTest.getProductById(2L);
        assertThat(product).isEqualTo(new Product().withId(2L).withEngineId(5L));

        // Обращение к базе было только одно
        verify(repository, times(1)).getAllProducts();
    }

    @Test
    public void testInvalidateCache() {
        ProductSimple product = serviceUnderTest.getProductById(2L);
        assertThat(product).isEqualTo((new Product().withId(2L).withEngineId(5L)));

        // Обращение к базе было только одно
        verify(repository, times(1)).getAllProducts();

        productsCache.invalidate();
        when(repository.getAllProducts()).thenReturn(Arrays.asList(
                new Product().withId(1L).withDailyShows(123L),
                new Product().withId(2L).withEngineId(6L)
        ));

        product = serviceUnderTest.getProductById(2L);
        assertThat(product).isEqualTo(new Product().withId(2L).withEngineId(6L));

        // После инвалидации кеша было еще одно обращение
        verify(repository, times(2)).getAllProducts();
    }

    @Test
    public void calculateProductForTextCampaignWithoutQuasiCurrency() {
        ProductSimple productSimple = serviceUnderTest.calculateProductForCampaign(CampaignType.TEXT,
                CurrencyCode.RUB, false);

        assertThat(productSimple).isNotNull();
        assertThat(productSimple).isEqualTo(PRODUCT_WITHOUT_QUASI_CURRENCY);
    }

    @Test
    public void calculateProductForTextCampaignWithQuasiCurrency() {
        ProductSimple productSimple = serviceUnderTest.calculateProductForCampaign(CampaignType.TEXT,
                CurrencyCode.KZT, true);

        assertThat(productSimple)
                .isNotNull()
                .isEqualTo(PRODUCT_WITH_QUASI_CURRENCY);
    }

    @Test()
    public void calculateUnknownProductForCampaign() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Can not find product info by type: " + ProductType.GEO +
                " and currencyCode: " + CurrencyCode.YND_FIXED.name() +
                " and quasiCurrencyFlag: " + true);

        serviceUnderTest.calculateProductForCampaign(CampaignType.GEO,
                CurrencyCode.YND_FIXED, true);
    }


    @Test()
    public void calculateProductForUnknownCampaignType() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Can not calculate product type for campaign type: " + CampaignType.BILLING_AGGREGATE.name());

        serviceUnderTest.calculateProductForCampaign(CampaignType.BILLING_AGGREGATE,
                CurrencyCode.YND_FIXED, true);
    }

    /**
     * Парсит JSON файл с ограничениями продукта.
     */
    @Test
    public void readProductRestrictionFileTest() {
        ProductService.readProductRestrictionFile();
    }

    /**
     * Проверяет, что у всех записей из файла с ограничениями продукта ключ уникален.
     */
    @Test
    public void getUniqueKeyTest() {
        List<ProductRestriction> prList = ProductService.readProductRestrictionFile();

        Set<ProductRestrictionKey> seenKeys = new HashSet<>();
        List<ProductRestrictionKey> duplicates = new ArrayList<>();
        for (var pr : prList) {
            ProductRestrictionKey key = ProductService.calculateUniqueProductRestrictionKey(pr);
            if (!seenKeys.add(key)) {
                duplicates.add(key);
            }
        }

        assertThat("No product restrictions with duplicate keys found in resource file",
                duplicates, is(empty())
        );
    }

    @Test
    public void getProductRestrictionsNoCacheTest() {
        List<ProductRestriction> expectedRestrictions = List.of(TestProducts.defaultProductRestriction());
        when(repository.getAllProductRestrictions()).thenReturn(expectedRestrictions);
        List<ProductRestriction> allRest = serviceUnderTest.getProductRestrictionsNoCache();
        assertThat(allRest, beanDiffer(expectedRestrictions));
    }
}
