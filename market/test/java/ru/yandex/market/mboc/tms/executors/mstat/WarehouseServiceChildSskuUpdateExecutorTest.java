package ru.yandex.market.mboc.tms.executors.mstat;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.dict.ChildSskuResult;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.WarehouseService;
import ru.yandex.market.mboc.common.dict.WarehouseServiceAuditRecorder;
import ru.yandex.market.mboc.common.dict.WarehouseServiceRepository;
import ru.yandex.market.mboc.common.dict.WarehouseServiceRepositoryImpl;
import ru.yandex.market.mboc.common.dict.WarehouseServiceService;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

public class WarehouseServiceChildSskuUpdateExecutorTest extends BaseDbTestClass {

    private WarehouseServiceService warehouseServiceService;
    private WarehouseServiceChildSskuUpdateExecutor executor;
    private WarehouseServiceRepository warehouseServiceRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private MskuRepository mskuRepository;

    private static final String STORAGE_KEY = "warehouse_service_exporter_batch_size";

    @Before
    public void setup() {
        warehouseServiceRepository = new WarehouseServiceRepositoryImpl(
            namedParameterJdbcTemplate,
            transactionTemplate
        );
        warehouseServiceService = Mockito.spy(new WarehouseServiceService(
                warehouseServiceRepository,
                offerRepository,
                supplierRepository,
                mskuRepository,
            Mockito.mock(WarehouseServiceAuditRecorder.class)
            )
        );

        executor = new WarehouseServiceChildSskuUpdateExecutor(
            warehouseServiceService,
            storageKeyValueService,
            transactionHelper
        );
    }

    @Test
    public void shouldUpdateChildSskus() {
        storageKeyValueService.putValue(STORAGE_KEY, 1);

        var warehouseService1 = WarehouseService.builder()
            .supplierId(1)
            .shopSku("shopsku1")
            .needSort(true)
            .childSskus(Set.of("childSku1"))
            .build();

        var warehouseService2 = WarehouseService.builder()
            .supplierId(2)
            .shopSku("shopsku2")
            .needSort(false)
            .childSskus(Set.of("childSku2"))
            .build();

        var warehouseService3 = WarehouseService.builder()
            .supplierId(3)
            .shopSku("shopsku3")
            .needSort(false)
            .childSskus(Set.of())
            .build();

        supplierRepository.insertBatch(
            OfferTestUtils.simpleSupplier(warehouseService1.getSupplierId()),
            OfferTestUtils.simpleSupplier(warehouseService2.getSupplierId()),
            OfferTestUtils.simpleSupplier(warehouseService3.getSupplierId())
        );

        warehouseServiceRepository.saveOrUpdateAll(
            List.of(warehouseService1, warehouseService2, warehouseService3));

        var freshChildSskus1 = Set.of("childSku1, childSku11");
        var freshChildSskus2 = new HashSet<String>();
        var freshChildSskus3 = Set.of("childSku3");

        when(warehouseServiceService.getAssortmentChildSskus(anyList(), anyBoolean()))
            .thenReturn(
                Map.of(
                    warehouseService1.getShopSkuKey(), new ChildSskuResult(warehouseService1.getShopSkuKey(), null,
                        freshChildSskus1),
                    warehouseService2.getShopSkuKey(), new ChildSskuResult(warehouseService2.getShopSkuKey(), null,
                        freshChildSskus2),
                    warehouseService3.getShopSkuKey(), new ChildSskuResult(warehouseService3.getShopSkuKey(), null,
                        freshChildSskus3)
                )
            );

        executor.execute();

        var resultWarehouseService1 = warehouseServiceService.getWarehouseService(
            List.of(warehouseService1.getShopSkuKey())
        ).get(0);

        var resultWarehouseService2 = warehouseServiceService.getWarehouseService(
            List.of(warehouseService2.getShopSkuKey())
        ).get(0);

        var resultWarehouseService3 = warehouseServiceService.getWarehouseService(
            List.of(warehouseService3.getShopSkuKey())
        ).get(0);

        assertThat(resultWarehouseService1.getChildSskus())
            .containsExactlyInAnyOrderElementsOf(freshChildSskus1);
        assertThat(resultWarehouseService2.getChildSskus())
            .containsExactlyInAnyOrderElementsOf(freshChildSskus2);
        assertThat(resultWarehouseService3.getChildSskus())
            .containsExactlyInAnyOrderElementsOf(freshChildSskus3);
    }

    @Test
    public void shouldRemoveWsWithUnknownOfferOrWithoutApprovedMapping() {
        storageKeyValueService.putValue(STORAGE_KEY, 1);

        var warehouseService1 = WarehouseService.builder()
            .supplierId(1)
            .shopSku("shopsku1")
            .needSort(true)
            .childSskus(Set.of("childSku1"))
            .build();

        supplierRepository.insertBatch(
            OfferTestUtils.simpleSupplier(warehouseService1.getSupplierId())
        );

        warehouseServiceRepository.saveOrUpdateAll(List.of(warehouseService1));

        assertThat(warehouseServiceRepository.findAll())
            .hasSize(1);

        executor.execute();

        assertThat(warehouseServiceRepository.findAll())
            .hasSize(0);
    }
}
