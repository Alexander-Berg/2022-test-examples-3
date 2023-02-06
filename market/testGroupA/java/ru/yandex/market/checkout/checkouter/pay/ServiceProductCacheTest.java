package ru.yandex.market.checkout.checkouter.pay;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.balance.service.CreateServiceProductCacheDao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkouter.jooq.tables.ServiceProductCache.SERVICE_PRODUCT_CACHE;

public class ServiceProductCacheTest extends AbstractServicesTestBase {

    @Autowired
    private CreateServiceProductCacheDao cacheService;
    @Autowired
    private DSLContext dsl;
    @Autowired
    private Clock clock;

    @Test
    public void checkCacheRecordCreatedWithEmptyFeeAndPartnerId() {
        cacheService.cacheCreateServiceProduct(
                "some-token-1",
                "some-service-product-id-1",
                "some-name-1",
                null,
                null,
                "service-product-response-1"
        );

        Optional<String> cachedCreateServiceProduct = cacheService.getCachedCreateServiceProduct(
                "some-token-1",
                "some-service-product-id-1",
                "some-name-1",
                null,
                null
        );

        assertTrue(cachedCreateServiceProduct.isPresent());
        assertThat(cachedCreateServiceProduct.get(), Matchers.equalTo("service-product-response-1"));
    }

    @Test
    public void checkCacheRecordCreatedWithEmptyFee() {
        cacheService.cacheCreateServiceProduct(
                "some-token-2",
                "some-service-product-id-2",
                "some-name-2",
                null,
                2L,
                "service-product-response-2"
        );

        Optional<String> cachedCreateServiceProduct = cacheService.getCachedCreateServiceProduct(
                "some-token-2",
                "some-service-product-id-2",
                "some-name-2",
                null,
                2L
        );

        assertTrue(cachedCreateServiceProduct.isPresent());
        assertThat(cachedCreateServiceProduct.get(), Matchers.equalTo("service-product-response-2"));
    }

    @Test
    public void checkCacheRecordCreatedWithEmptyPartnerId() {
        cacheService.cacheCreateServiceProduct(
                "some-token-3",
                "some-service-product-id-3",
                "some-name-3",
                3,
                null,
                "service-product-response-3"
        );

        Optional<String> cachedCreateServiceProduct = cacheService.getCachedCreateServiceProduct(
                "some-token-3",
                "some-service-product-id-3",
                "some-name-3",
                3,
                null
        );

        assertTrue(cachedCreateServiceProduct.isPresent());
        assertThat(cachedCreateServiceProduct.get(), Matchers.equalTo("service-product-response-3"));
    }

    @Test
    public void checkNoCacheRecord() {
        Optional<String> cachedCreateServiceProduct = cacheService.getCachedCreateServiceProduct(
                "some-token-4",
                "some-service-product-id-4",
                "some-name-4",
                4,
                4L
        );

        assertFalse(cachedCreateServiceProduct.isPresent());
    }

    @Test
    public void checkCacheRecordCreated() {
        cacheService.cacheCreateServiceProduct(
                "some-token-5",
                "some-service-product-id-5",
                "some-name-5",
                5,
                5L,
                "service-product-response-5"
        );

        Optional<String> cachedCreateServiceProduct = cacheService.getCachedCreateServiceProduct(
                "some-token-5",
                "some-service-product-id-5",
                "some-name-5",
                5,
                5L
        );

        assertTrue(cachedCreateServiceProduct.isPresent());
        assertThat(cachedCreateServiceProduct.get(), Matchers.equalTo("service-product-response-5"));
    }

    @Test
    public void checkReturnsDuplicatedRecordFromCache() {
        cacheService.cacheCreateServiceProduct(
                "some-token-6",
                "some-service-product-id-6",
                "some-name-6",
                null,
                6L,
                "service-product-response-6-1"
        );

        cacheService.cacheCreateServiceProduct(
                "some-token-6",
                "some-service-product-id-6",
                "some-name-6",
                null,
                6L,
                "service-product-response-6-2"
        );

        Optional<String> cachedCreateServiceProduct = cacheService.getCachedCreateServiceProduct(
                "some-token-6",
                "some-service-product-id-6",
                "some-name-6",
                null,
                6L
        );

        assertTrue(cachedCreateServiceProduct.isPresent());
        assertThat(
                cachedCreateServiceProduct.get(),
                Matchers.anyOf(
                        Matchers.equalTo("service-product-response-6-1"),
                        Matchers.equalTo("service-product-response-6-2")
                )
        );
    }

    @Test
    public void checkCacheWithExpiration() {
        final String serviceProductId = "some-service-product-id-7";
        cacheService.cacheCreateServiceProduct(
                "some-token-7",
                serviceProductId,
                "some-name-7",
                10,
                7L,
                "service-product-response-7-1"
        );
        //После инсерта продукт находится.
        Optional<String> cachedProduct = getServiceProductById(serviceProductId);
        assertTrue(cachedProduct.isPresent());
        //Выставляем дату кэширования сильно в прошлое.
        final LocalDateTime expiredDate = LocalDateTime.now(clock).minus(5, ChronoUnit.MONTHS);
        transactionTemplate.execute(txStatus -> {
            dsl.update(SERVICE_PRODUCT_CACHE)
                    .set(SERVICE_PRODUCT_CACHE.CACHED_AT, expiredDate)
                    .where(SERVICE_PRODUCT_CACHE.SERVICE_PRODUCT_ID.eq(serviceProductId))
                    .execute();
            return null;
        });
        cachedProduct = getServiceProductById(serviceProductId);
        assertFalse(cachedProduct.isPresent());

        //Переписываем его снова, продукт снова появляется в выдаче.
        cacheService.cacheCreateServiceProduct(
                "some-token-7",
                serviceProductId,
                "some-name-7",
                10,
                7L,
                "service-product-response-7-1"
        );
        cachedProduct = getServiceProductById(serviceProductId);
        assertTrue(cachedProduct.isPresent());

        int count = dsl.selectCount()
                .from(SERVICE_PRODUCT_CACHE)
                .where(SERVICE_PRODUCT_CACHE.SERVICE_PRODUCT_ID.eq(serviceProductId))
                .execute();
        //Обновился существующий продукт без инсерта нового.
        assertEquals(1, count);
    }

    private Optional<String> getServiceProductById(String serviceProductId) {
        return cacheService.getCachedCreateServiceProduct("some-token-7", serviceProductId,
                "some-name-7", 10, 7L);
    }
}
