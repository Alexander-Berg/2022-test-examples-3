package ru.yandex.market.supplier.summary;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.supplier.summary.SupplierSummaryInfoService;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;

import static org.mockito.Mockito.reset;

/**
 * Тест задачи {@link SupplierMappingSummaryFromMbocExecutor} загрузки данных сводки по маппингу поставщиков из MBOC.
 */

@DbUnitDataSet(before = "SupplierMappingSummaryFromMbocExecutorTest.before.csv")
class SupplierMappingSummaryFromMbocExecutorTest extends FunctionalTest {

    @Autowired
    SupplierSummaryInfoService supplierSummaryInfoService;
    @Autowired
    SupplierMappingSummaryFromMbocExecutor supplierMappingSummaryFromMbocExecutor;
    @Autowired
    private MboMappingsService patientMboMappingsService;

    @DbUnitDataSet(after = "SupplierMappingSummaryFromMbocExecutorTest.after.csv")
    @Test
    void testSupplierMappingSummaryFromMbocExecutor() {
        mockMboMappingService();
        supplierMappingSummaryFromMbocExecutor.doJob(null);
        reset(patientMboMappingsService);
    }

    private void mockMboMappingService() {
        Mockito.when(patientMboMappingsService.searchMappingsByShopId(Mockito.any()))
                .thenReturn(
                        MboMappings.SearchMappingsResponse.newBuilder()
                                .setTotalCount(1)
                                .build());
    }
}
