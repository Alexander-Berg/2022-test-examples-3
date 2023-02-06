package ru.yandex.market.ff4shops.api.xml;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.delivery.stocks.StocksRequestStatusService;
import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "env.batch.csv")
public class StocksRequestStatusMonitoringBatchControllerTest extends FunctionalTest {

    @Autowired
    StocksRequestStatusService stocksRequestStatusService;

    @Autowired
    LMSClient lmsClient;


    @Autowired
    private StocksRequestStatusMonitoringController controller;

    @Test
    @DbUnitDataSet(
            before = "StocksRequestStatusMonitoringControllerTest.cpa.before.csv",
            after = "StocksRequestStatusMonitoringControllerTest.cpa.after.csv"
    )
    @DisplayName("OK батчевый, если LGW не обновил стоки (партнёр работает через ЛК), но синк стоков в LMS отключен")
    void testErrorWhenCpaSupplierDidntUpdateStocksNoStockSyncBatch() {
        when(lmsClient.searchPartners(eq(SearchPartnerFilter.builder().setIds(Set.of(10L, 20L)).build())))
                .thenReturn(
                        List.of(PartnerResponse.newBuilder().id(10L)
                                        .partnerType(PartnerType.SUPPLIER)
                                        .stockSyncEnabled(false)
                                        .status(PartnerStatus.ACTIVE)
                                        .build(),
                                PartnerResponse.newBuilder().id(20L)
                                        .partnerType(PartnerType.SUPPLIER)
                                        .stockSyncEnabled(false)
                                        .status(PartnerStatus.ACTIVE)
                                        .build()
                        )
                );

        String response = controller.checkLastRequestForStock();
        Assertions.assertEquals(MonitoringResult.OK.toString(), response);
    }
    @Test
    @DbUnitDataSet(
            before = "StocksRequestStatusMonitoringControllerTest.cpa2.before.csv",
            after = "StocksRequestStatusMonitoringControllerTest.cpa.after.csv"
    )
    @DisplayName("OK батчевый, если LGW не обновил стоки (партнёр работает через ЛК), но синк стоков в LMS отключен " +
            "и два склада у партнера")
    void testErrorWhenCpaSupplierDidntUpdateStocksNoStockSyncBatch2() {
        when(lmsClient.searchPartners(eq(SearchPartnerFilter.builder().setIds(Set.of(10L, 11L, 20L)).build())))
                .thenReturn(
                        List.of(PartnerResponse.newBuilder().id(10L)
                                        .partnerType(PartnerType.SUPPLIER)
                                        .stockSyncEnabled(false)
                                        .status(PartnerStatus.ACTIVE)
                                        .build(),
                                PartnerResponse.newBuilder().id(11L)
                                        .partnerType(PartnerType.SUPPLIER)
                                        .stockSyncEnabled(false)
                                        .status(PartnerStatus.ACTIVE)
                                        .build(),
                                PartnerResponse.newBuilder().id(20L)
                                        .partnerType(PartnerType.SUPPLIER)
                                        .stockSyncEnabled(false)
                                        .status(PartnerStatus.ACTIVE)
                                        .build()
                        )
                );

        String response = controller.checkLastRequestForStock();
        Assertions.assertEquals(MonitoringResult.OK.toString(), response);
    }


    @Test
    @DbUnitDataSet(
            before = "StocksRequestStatusMonitoringControllerTest.cpaAndPushStocks.before.csv",
            after = "StocksRequestStatusMonitoringControllerTest.cpaAndPushStocks.after.csv"
    )
    @DisplayName("OK батчевый, если LGW не обновил стоки (партнёры работают и через ЛК, и через API), но партнёр в LMS не " +
            "ACTIVE")
    void testErrorWhenCpaAndPushStocksSupplierDidntUpdateStocksNotActiveInLMS() {
        when(lmsClient.searchPartners(eq(SearchPartnerFilter.builder().setIds(Set.of(10L, 20L)).build())))
                .thenReturn(
                        List.of(PartnerResponse.newBuilder().id(10L)
                                        .partnerType(PartnerType.SUPPLIER)
                                        .stockSyncEnabled(true)
                                        .status(PartnerStatus.TESTING)
                                        .build(),
                                PartnerResponse.newBuilder().id(20L)
                                        .partnerType(PartnerType.SUPPLIER)
                                        .stockSyncEnabled(true)
                                        .status(PartnerStatus.INACTIVE)
                                        .build()
                        )
                );

        String response = controller.checkLastRequestForStock();
        Assertions.assertEquals(MonitoringResult.OK.toString(), response);
    }


    @Test
    @DbUnitDataSet(
            before = "StocksRequestStatusMonitoringControllerTest.cpaAndPushStocks.before.csv",
            after = "StocksRequestStatusMonitoringControllerTest.cpaAndPushStocks.after.csv"
    )
    @DisplayName("OK батчевый, если LGW не обновил стоки (партнёры работают и через ЛК, и через API), но синк стоков в LMS " +
            "отключен")
    void testErrorWhenCpaAndPushStocksSupplierDidntUpdateStocksNoStockSync() {
        when(lmsClient.searchPartners(eq(SearchPartnerFilter.builder().setIds(Set.of(10L, 20L)).build())))
                .thenReturn(
                        List.of(PartnerResponse.newBuilder().id(10L)
                                        .partnerType(PartnerType.SUPPLIER)
                                        .stockSyncEnabled(false)
                                        .status(PartnerStatus.ACTIVE)
                                        .build(),
                                PartnerResponse.newBuilder().id(20L)
                                        .partnerType(PartnerType.SUPPLIER)
                                        .stockSyncEnabled(false)
                                        .status(PartnerStatus.ACTIVE)
                                        .build()
                        )
                );

        String response = controller.checkLastRequestForStock();
        Assertions.assertEquals(MonitoringResult.OK.toString(), response);
    }

    @Test
    @DbUnitDataSet(before = "StocksRequestStatusMonitoringControllerTest.not.cpa.before.csv")
    @DisplayName("OK батчевый, если LGW не обновил стоки (партнёр работает через API), но синк стоков в LMS отключен")
    void testErrorWhenOnlyNotCpaSuppliersDidntUpdateStocksNoStockSync() {
        when(lmsClient.searchPartners(eq(SearchPartnerFilter.builder().setIds(Set.of(10L)).build())))
                .thenReturn(
                        List.of(PartnerResponse.newBuilder().id(10L)
                                .partnerType(PartnerType.SUPPLIER)
                                .stockSyncEnabled(false)
                                .status(PartnerStatus.ACTIVE)
                                .build()
                        )
                );

        String response = controller.checkLastRequestForStock();
        Assertions.assertEquals(MonitoringResult.OK.toString(), response);
    }

}
