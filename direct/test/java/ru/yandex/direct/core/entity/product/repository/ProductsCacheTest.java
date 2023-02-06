package ru.yandex.direct.core.entity.product.repository;

import java.util.Arrays;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.product.model.Product;
import ru.yandex.direct.core.entity.product.model.ProductType;

import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.product.repository.ProductsCache.YND_FIXED_FOR_TEXT_TYPE_PRODUCT_ID;

public class ProductsCacheTest {

    private static final long SOME_PRODUCT_ID = 123L;

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductsCache productsCache;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        when(repository.getAllProducts()).thenReturn(Arrays.asList(
                new Product().withId(YND_FIXED_FOR_TEXT_TYPE_PRODUCT_ID).withType(ProductType.TEXT),
                new Product().withId(YND_FIXED_FOR_TEXT_TYPE_PRODUCT_ID).withType(ProductType.GEO),
                new Product().withId(SOME_PRODUCT_ID).withType(ProductType.GEO)
        ));
    }


    @Test
    public void checkGetData_expectSkipYndFixedProductForGeoType() {
        Map<Long, Product> data = productsCache.getData();

        SoftAssertions assertions = new SoftAssertions();
        assertions.assertThat(data)
                .containsOnlyKeys(YND_FIXED_FOR_TEXT_TYPE_PRODUCT_ID, SOME_PRODUCT_ID);

        assertions.assertThat(data.get(YND_FIXED_FOR_TEXT_TYPE_PRODUCT_ID))
                .isEqualTo(new Product().withId(YND_FIXED_FOR_TEXT_TYPE_PRODUCT_ID).withType(ProductType.TEXT));
        assertions.assertThat(data.get(SOME_PRODUCT_ID))
                .isEqualTo(new Product().withId(SOME_PRODUCT_ID).withType(ProductType.GEO));
        assertions.assertAll();
    }
}
