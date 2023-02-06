package ru.yandex.market.core.feed.supplier;

import java.io.IOException;
import java.time.Instant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.common.framework.core.RemoteFile;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feed.mds.FeedFileStorage;
import ru.yandex.market.core.feed.mds.StoreInfo;
import ru.yandex.market.core.feed.supplier.model.SupplierFeed;
import ru.yandex.market.core.misc.resource.RemoteResource;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Тесты на {@link SupplierFeedService}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "FeedSupplierServiceTest.csv")
class FeedSupplierServiceTest extends FunctionalTest {

    private static final long SUPPLIER_ID = 774L;

    @Autowired
    private SupplierFeedService supplierFeedService;

    @Autowired
    private FeedFileStorage feedFileStorage;

    private static void assertSupplierFeedEquals(SupplierFeed expectedFeed, SupplierFeed actualFeed) {
        ReflectionAssert.assertReflectionEquals(expectedFeed.withUpdatedAt(actualFeed.getUpdatedAt()), actualFeed);

    }

    @BeforeEach
    void initMocks() {
        try {
            when(feedFileStorage.upload(any(RemoteFile.class), anyLong()))
                    .thenReturn(new StoreInfo(100500, "mds.url"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Проверяем, что для отсутствующего ассортимента поставщика возвращается {@code null}.
     */
    @Test
    @DbUnitDataSet
    void testGetFeedNotFound() {
        SupplierFeed supplierFeed = supplierFeedService.getSupplierFeed(SUPPLIER_ID).orElse(null);
        Assertions.assertNull(supplierFeed);
    }

    /**
     * Тест на {@link SupplierFeedService#getSupplierFeed(long) получение фида поставщика}.
     */
    @Test
    @DbUnitDataSet(before = "testGetFeed.csv")
    void testGetFeed() {
        SupplierFeed expectedFeed = new SupplierFeed.Builder()
                .setId(10L)
                .setResource(RemoteResource.of("http://mds.url"))
                .setUploadId(50L)
                .setSupplierId(SUPPLIER_ID)
                .setBusinessId(666L)
                .setUpdatedAt(Instant.now())
                .setOriginalFileName("file.name")
                .build();
        SupplierFeed supplierFeed = supplierFeedService.getSupplierFeed(SUPPLIER_ID).orElse(null);
        assertSupplierFeedEquals(expectedFeed, supplierFeed);
    }
}
