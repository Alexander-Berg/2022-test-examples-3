package ru.yandex.direct.core.entity.product.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.math.RandomUtils;
import org.jooq.DSLContext;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.product.model.Product;
import ru.yandex.direct.core.entity.product.model.ProductCalcType;
import ru.yandex.direct.core.entity.product.model.ProductRestriction;
import ru.yandex.direct.core.entity.product.model.ProductType;
import ru.yandex.direct.core.entity.product.model.ProductUnit;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.ProductSteps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppcdict.tables.ProductRestrictions.PRODUCT_RESTRICTIONS;

@RunWith(SpringJUnit4ClassRunner.class)
@CoreTest
public class ProductRepositoryTest {

    @Autowired
    private DslContextProvider databaseWrapperProvider;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductSteps productSteps;

    private List<Long> generatedIds = new ArrayList<>();

    private Product generateProduct() {
        return new Product()
                .withId((long) RandomUtils.nextInt())
                .withType(ProductType.AUTO_IMPORT)
                .withUnitName("Bucks")
                .withRate(1L)
                .withThemeId(0L)
                .withEngineId(7L)
                .withUnitScale(1L)
                .withPrice(BigDecimal.valueOf(1.0).setScale(6, RoundingMode.CEILING))
                .withProductName("Тестовый пакет")
                .withCurrencyCode(CurrencyCode.BYN)
                .withUnit(ProductUnit.SHOWS)
                .withCalcType(ProductCalcType.CPM)
                .withPublicNameKey("")
                .withPublicDescriptionKey("");
    }

    private List<Product> generateProducts(int count) {
        List<Product> products = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            var product = generateProduct();
            products.add(product);
            generatedIds.add(product.getId());
        }

        return products;
    }

    @After
    public void cleanup() {
        productSteps.removeProducts(generatedIds);
        generatedIds.clear();
    }

    @Test
    public void insertNewProductsTest() {
        List<Product> existsProducts = generateProducts(5);
        List<Product> newProducts = generateProducts(3);

        productSteps.addProductsIfNotExists(existsProducts);

        var previos = productRepository.getAllProducts();

        productRepository.insertNewProducts(databaseWrapperProvider.ppcdict(), newProducts);

        var allProducts = productRepository.getAllProducts();

        assertThat(allProducts).hasSize(newProducts.size() + previos.size());
        assertThat(allProducts).containsAll(newProducts);
        assertThat(allProducts).containsAll(previos);
    }

    @Test
    public void insertNewProductsWithIntersectionTest() {
        List<Product> existsProducts = generateProducts(5);
        List<Product> newProducts = generateProducts(3);

        newProducts.add(existsProducts.get(4));

        productSteps.addProductsIfNotExists(existsProducts);

        var previos = productRepository.getAllProducts();

        productRepository.insertNewProducts(databaseWrapperProvider.ppcdict(), newProducts);


        var allProducts = productRepository.getAllProducts();

        assertThat(allProducts).hasSize(newProducts.size() + previos.size() - 1);
        assertThat(allProducts).containsAll(newProducts);
        assertThat(allProducts).containsAll(previos);
    }


    @Test
    public void getAllProductRestrictionsTest() {
        var expectedProductRestriction = insertFullProductRestriction();

        List<ProductRestriction> allProductRestrictions = productRepository.getAllProductRestrictions();
        checkProductRestriction(allProductRestrictions, expectedProductRestriction);
    }

    @Test
    public void getAllProductRestrictionsForUpdateTest() {
        var expectedProductRestriction = insertFullProductRestriction();

        DSLContext ppcdict = databaseWrapperProvider.ppcdict();
        List<ProductRestriction> allProductRestrictions = productRepository.getAllProductRestrictionsForUpdate(ppcdict);
        checkProductRestriction(allProductRestrictions, expectedProductRestriction);
    }

    private ProductRestriction insertFullProductRestriction() {
        long expectedProductId = 85264;
        AdGroupType expectedAdGroupType = AdGroupType.CPM_OUTDOOR;
        String expectedPublicNameKey = "test pub name key " + expectedProductId;
        String expectedPublicDescriptionKey = "test pub description key " + expectedProductId;
        long expectedUnitCountMin = 3453;
        long expectedUnitCountMax = 3475453;
        String expectedConditionJson =
                "[{\"name\":\"correction_traffic\",\"availableAny\":false,\"required\":false,\"values\":[]}]";

        ProductRestriction expectedProductRestriction = new ProductRestriction()
                .withProductId(expectedProductId)
                .withGroupType(expectedAdGroupType)
                .withPublicNameKey(expectedPublicNameKey)
                .withPublicDescriptionKey(expectedPublicDescriptionKey)
                .withUnitCountMin(expectedUnitCountMin)
                .withUnitCountMax(expectedUnitCountMax)
                .withConditionJson(expectedConditionJson);

        var ppcdict = databaseWrapperProvider.ppcdict();
        ppcdict.insertInto(PRODUCT_RESTRICTIONS)
                .set(PRODUCT_RESTRICTIONS.PRODUCT_ID, expectedProductId)
                .set(PRODUCT_RESTRICTIONS.ADGROUP_TYPE, expectedAdGroupType.name())
                .set(PRODUCT_RESTRICTIONS.PUBLIC_NAME_KEY, expectedPublicNameKey)
                .set(PRODUCT_RESTRICTIONS.PUBLIC_DESCRIPTION_KEY, expectedPublicDescriptionKey)
                .set(PRODUCT_RESTRICTIONS.UNIT_COUNT_MIN, expectedUnitCountMin)
                .set(PRODUCT_RESTRICTIONS.UNIT_COUNT_MAX, expectedUnitCountMax)
                .set(PRODUCT_RESTRICTIONS.CONDITION_JSON, expectedConditionJson)
                .execute();
        long newProductRestrictionId = ppcdict.lastID().longValue();
        expectedProductRestriction.setId(newProductRestrictionId);

        return expectedProductRestriction;
    }

    private void checkProductRestriction(
            List<ProductRestriction> allProductRestrictions, ProductRestriction expectedProductRestriction
    ) {
        assertThat(allProductRestrictions).isNotEmpty();
        var actualProductRestriction = allProductRestrictions.stream()
                .filter(pr -> pr.getId().equals(expectedProductRestriction.getId()))
                .findFirst();
        assertThat(actualProductRestriction.isPresent()).isTrue();
        assertThat(actualProductRestriction.get())
                .isEqualToIgnoringGivenFields(expectedProductRestriction, "conditionJson");
        assertThat(JsonUtils.fromJson(actualProductRestriction.get().getConditionJson()))
                .isEqualTo(JsonUtils.fromJson(expectedProductRestriction.getConditionJson()));
    }

    @Test
    public void updateProductRestrictionsTest() {
        var productRestriction = insertFullProductRestriction();
        long expectedProductId = 123145L;
        String expectedPublicDescriptionKey = "test pub description key 2 " + expectedProductId;
        long expectedUnitCountMin = productRestriction.getUnitCountMin() + 123;
        long expectedUnitCountMax = productRestriction.getUnitCountMax() + 123;
        String expectedConditionJson =
                "[{\"name\":\"correction_traffic\",\"availableAny\":false,\"required\":false,\"values\":[]}," +
                        "{\"name\":\"correction_weather\",\"availableAny\":false,\"required\":false,\"values\":[]}]";
        String expectedPublicNameKey = "test pub name key 2 " + expectedProductId;
        AdGroupType expectedGroupType = AdGroupType.CPM_VIDEO;

        var expectedProductRestriction = new ProductRestriction()
                .withId(productRestriction.getId())
                .withGroupType(expectedGroupType)
                .withPublicNameKey(expectedPublicNameKey)
                .withProductId(expectedProductId)
                .withPublicDescriptionKey(expectedPublicDescriptionKey)
                .withUnitCountMin(expectedUnitCountMin)
                .withUnitCountMax(expectedUnitCountMax)
                .withConditionJson(expectedConditionJson);

        var changes = new ModelChanges<>(productRestriction.getId(), ProductRestriction.class)
                .process(expectedGroupType, ProductRestriction.GROUP_TYPE)
                .process(expectedPublicNameKey, ProductRestriction.PUBLIC_NAME_KEY)
                .process(expectedProductId, ProductRestriction.PRODUCT_ID)
                .process(expectedPublicDescriptionKey, ProductRestriction.PUBLIC_DESCRIPTION_KEY)
                .process(expectedUnitCountMin, ProductRestriction.UNIT_COUNT_MIN)
                .process(expectedUnitCountMax, ProductRestriction.UNIT_COUNT_MAX)
                .process(expectedConditionJson, ProductRestriction.CONDITION_JSON)
                .applyTo(productRestriction);
        DSLContext ppcdict = databaseWrapperProvider.ppcdict();
        productRepository.updateProductRestrictions(ppcdict, singletonList(changes));

        List<ProductRestriction> allProductRestrictions = productRepository.getAllProductRestrictions();
        checkProductRestriction(allProductRestrictions, expectedProductRestriction);
    }
}
