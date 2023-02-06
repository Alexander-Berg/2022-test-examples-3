package ru.yandex.market.ff.tms;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.bo.SupplierContentMapping;
import ru.yandex.market.ff.model.bo.SupplierSkuKey;
import ru.yandex.market.ff.repository.RequestItemRepository;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.ff.service.SupplierMappingService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link SyncMarketSkuAndNameExecutor}
 *
 * @author avetokhin 28.06.18.
 */
class SyncMarketSkuAndNameExecutorTest extends IntegrationTest {

    private static final Long SUPPLIER_ID_1 = 1L;
    private static final Long SUPPLIER_ID_2 = 2L;
    private static final String SHOP_SKU_1 = "sku1";
    private static final String SHOP_SKU_2 = "sku2";
    private static final String SHOP_SKU_3 = "sku3";
    private static final long MARKET_SKU_1 = 1L;
    private static final long MARKET_SKU_2 = 2L;
    private static final long MARKET_SKU_3 = 3L;
    private static final String MARKET_NAME_1 = "market name 1";
    private static final String MARKET_NAME_2 = "market name 2";
    private static final String MARKET_NAME_3 = "market name 3";

    @Autowired
    private ShopRequestFetchingService shopRequestFetchingService;

    @Autowired
    private RequestItemRepository requestItemRepository;

    private SyncMarketSkuAndNameExecutor executor;

    @BeforeEach
    void init() {
        final SupplierMappingService mappingService = mock(SupplierMappingService.class);

        final SupplierContentMapping mapping1 = SupplierContentMapping.builder(SHOP_SKU_1, MARKET_SKU_1, null)
            .setMarketName(MARKET_NAME_1)
            .setHasExpirationDate(true)
            .build();
        final SupplierContentMapping mapping2 = SupplierContentMapping.builder(SHOP_SKU_2, MARKET_SKU_2, null)
            .setMarketName(MARKET_NAME_2)
            .build();
        final SupplierContentMapping mapping3 = SupplierContentMapping.builder(SHOP_SKU_3, MARKET_SKU_3, null)
            .setMarketName(MARKET_NAME_3)
            .build();

        final ImmutableMap<SupplierSkuKey, SupplierContentMapping> mappingMap =
            ImmutableMap.<SupplierSkuKey, SupplierContentMapping>builder()
                .put(new SupplierSkuKey(SUPPLIER_ID_1, SHOP_SKU_1), mapping1)
                .put(new SupplierSkuKey(SUPPLIER_ID_1, SHOP_SKU_2), mapping2)
                .put(new SupplierSkuKey(SUPPLIER_ID_2, SHOP_SKU_3), mapping3)
                .build();

        when(mappingService.getMarketSkuMapping(anySet()))
            .thenReturn(mappingMap);

        executor = new SyncMarketSkuAndNameExecutor(mappingService, shopRequestFetchingService, requestItemRepository);
    }

    @Test
    @DatabaseSetup("classpath:tms/sync-market-name-sku/before.xml")
    @ExpectedDatabase(value = "classpath:tms/sync-market-name-sku/after.xml", assertionMode = NON_STRICT)
    void test() {
        executor.doJob(null);
    }
}
