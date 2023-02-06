package ru.yandex.market.ff4shops.api.xml;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.delivery.stocks.StocksRequestStatusService;
import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "env.single.csv")
class StocksRequestStatusMonitoringControllerTest extends FunctionalTest {

    @Autowired
    StocksRequestStatusService stocksRequestStatusService;

    @Autowired
    LMSClient lmsClient;

    @Autowired
    private StocksRequestStatusMonitoringController controller;

    @Test
    @DbUnitDataSet(before = "StocksRequestStatusMonitoringControllerTest.before.csv")
    @DisplayName("OK, если LGW обновил стоки (партнёр работает и через ЛК)")
    void testOkWhenAllCpaSuppliersUpdatedStocks() {
        stocksRequestStatusService.updateTimeOfRequestStocks(new HashSet<>(Collections.singletonList(1L)));
        String response = controller.checkLastRequestForStock();
        Assertions.assertEquals(MonitoringResult.OK.toString(), response);
    }

    @Test
    @DbUnitDataSet(before = "StocksRequestStatusMonitoringControllerTest.allWithPushStocks.before.csv")
    @DisplayName("OK, если LGW обновил стоки (партнёры работают и через ЛК, и через API)")
    void testOkWhenAllCpaAndPushStocksSuppliersUpdatedStocks() {
        stocksRequestStatusService.updateTimeOfRequestStocks(new HashSet<>(List.of(1L, 2L)));
        String response = controller.checkLastRequestForStock();
        Assertions.assertEquals(MonitoringResult.OK.toString(), response);
    }

    @Test
    @DbUnitDataSet(before = "StocksRequestStatusMonitoringControllerTest.not.cpa.before.csv")
    @DisplayName("CRIT, если LGW не обновил стоки (партнёр работает через API)")
    void testErrorWhenOnlyNotCpaSuppliersDidntUpdateStocks() {
        when(lmsClient.getPartner(10L))
                .thenReturn(
                        Optional.of(PartnerResponse.newBuilder().id(1L)
                                .partnerType(PartnerType.SUPPLIER)
                                .stockSyncEnabled(true)
                                .status(PartnerStatus.ACTIVE)
                                .build()
                        )
                );

        List<Long> expectedSupplierIds = List.of(1L);
        String response = controller.checkLastRequestForStock();
        Assertions.assertEquals(
                MonitoringResult.Status.ERROR.getCode() +
                        ";troubleshooting wiki: https://nda.ya.ru/t/mv77mgtL4rBcLM, not_requested_suppliers_ids" +
                        expectedSupplierIds,
                response
        );
    }

    @Test
    @DbUnitDataSet(before = "StocksRequestStatusMonitoringControllerTest.not.cpa.before.csv")
    @DisplayName("OK, если LGW не обновил стоки (партнёр работает через API), но синк стоков в LMS отключен")
    void testErrorWhenOnlyNotCpaSuppliersDidntUpdateStocksNoStockSync() {
        when(lmsClient.getPartner(10L))
                .thenReturn(
                        Optional.of(PartnerResponse.newBuilder().id(1L)
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
    @DbUnitDataSet(before = "StocksRequestStatusMonitoringControllerTest.cpa.before.csv")
    @DisplayName("CRIT, если LGW не обновил стоки (партнёр работает через ЛК)")
    void testErrorWhenCpaSupplierDidntUpdateStocks() {
        List<Long> expectedSupplierIds = List.of(1L, 2L);
        when(lmsClient.getPartner(10L))
                .thenReturn(
                        Optional.of(PartnerResponse.newBuilder().id(1L)
                                .partnerType(PartnerType.SUPPLIER)
                                .stockSyncEnabled(true)
                                .status(PartnerStatus.ACTIVE)
                                .build())
                );
        when(lmsClient.getPartner(20L))
                .thenReturn(
                        Optional.of(PartnerResponse.newBuilder().id(1L)
                                .partnerType(PartnerType.SUPPLIER)
                                .stockSyncEnabled(true)
                                .status(PartnerStatus.ACTIVE)
                                .build()
                        )
                );

        String response = controller.checkLastRequestForStock();
        Assertions.assertEquals(
                MonitoringResult.Status.ERROR.getCode() +
                        ";troubleshooting wiki: https://nda.ya.ru/t/mv77mgtL4rBcLM, not_requested_suppliers_ids" +
                        expectedSupplierIds,
                response
        );
    }

    @Test
    @DbUnitDataSet(
            before = {"StocksRequestStatusMonitoringControllerTest.cpa.before.csv"},
            after = "StocksRequestStatusMonitoringControllerTest.cpa.after.csv"
    )
    @DisplayName("OK, если LGW не обновил стоки (партнёр работает через ЛК), но синк стоков в LMS отключен")
    void testErrorWhenCpaSupplierDidntUpdateStocksNoStockSync() {

        when(lmsClient.getPartner(10L))
                .thenReturn(
                        Optional.of(PartnerResponse.newBuilder().id(1L)
                                .partnerType(PartnerType.SUPPLIER)
                                .stockSyncEnabled(false)
                                .status(PartnerStatus.ACTIVE)
                                .build()
                        )
                );
        when(lmsClient.getPartner(20L))
                .thenReturn(
                        Optional.of(PartnerResponse.newBuilder().id(2L)
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
    @DbUnitDataSet(before = "StocksRequestStatusMonitoringControllerTest.cpaAndPushStocks.before.csv")
    @DisplayName("CRIT, если LGW не обновил стоки (партнёры работают и через ЛК, и через API)")
    void testErrorWhenCpaAndPushStocksSupplierDidntUpdateStocks() {
        List<Long> expectedSupplierIds = List.of(1L, 2L);
        when(lmsClient.getPartner(10L))
                .thenReturn(
                        Optional.of(PartnerResponse.newBuilder().id(1L)
                                .partnerType(PartnerType.SUPPLIER)
                                .stockSyncEnabled(true)
                                .status(PartnerStatus.ACTIVE)
                                .build()
                        )
                );
        when(lmsClient.getPartner(20L))
                .thenReturn(
                        Optional.of(PartnerResponse.newBuilder().id(1L)
                                .partnerType(PartnerType.SUPPLIER)
                                .stockSyncEnabled(true)
                                .status(PartnerStatus.ACTIVE)
                                .build()
                        )
                );

        String response = controller.checkLastRequestForStock();
        Assertions.assertEquals(
                MonitoringResult.Status.ERROR.getCode() +
                        ";troubleshooting wiki: https://nda.ya.ru/t/mv77mgtL4rBcLM, not_requested_suppliers_ids" +
                        expectedSupplierIds,
                response
        );
    }

    @Test
    @DbUnitDataSet(
            before = "StocksRequestStatusMonitoringControllerTest.cpaAndPushStocks.before.csv",
            after = "StocksRequestStatusMonitoringControllerTest.cpaAndPushStocks.after.csv"
    )
    @DisplayName("OK, если LGW не обновил стоки (партнёры работают и через ЛК, и через API), но синк стоков в LMS " +
            "отключен")
    void testErrorWhenCpaAndPushStocksSupplierDidntUpdateStocksNoStockSync() {
        when(lmsClient.getPartner(10L))
                .thenReturn(
                        Optional.of(PartnerResponse.newBuilder().id(1L)
                                .partnerType(PartnerType.SUPPLIER)
                                .stockSyncEnabled(false)
                                .status(PartnerStatus.ACTIVE)
                                .build()
                        )
                );
        when(lmsClient.getPartner(20L))
                .thenReturn(
                        Optional.of(PartnerResponse.newBuilder().id(2L)
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
    @DisplayName("OK, если LGW не обновил стоки (партнёры работают и через ЛК, и через API), но партнёр в LMS не " +
            "ACTIVE")
    void testErrorWhenCpaAndPushStocksSupplierDidntUpdateStocksNotActiveInLMS() {
        when(lmsClient.getPartner(10L))
                .thenReturn(
                        Optional.of(PartnerResponse.newBuilder().id(1L)
                                .partnerType(PartnerType.SUPPLIER)
                                .stockSyncEnabled(true)
                                .status(PartnerStatus.TESTING)
                                .build())
                );
        when(lmsClient.getPartner(20L))
                .thenReturn(
                        Optional.of(PartnerResponse.newBuilder().id(2L)
                                .partnerType(PartnerType.SUPPLIER)
                                .stockSyncEnabled(true)
                                .status(PartnerStatus.INACTIVE)
                                .build())
                );

        String response = controller.checkLastRequestForStock();
        Assertions.assertEquals(MonitoringResult.OK.toString(), response);
    }

    @Test
    @DisplayName("Проверка на то, что по push стокам корректно работает таймаут в 12 часов, а не в 3")
    @DbUnitDataSet(before = "StocksRequestStatusMonitoringControllerTest.cpaPushAndPullStocks.before.csv")
    void testOkWhenCpaPushAndPullStocksSupplierUpdateStocks() {
        String response = controller.checkLastRequestForStock();
        Assertions.assertEquals(MonitoringResult.OK.toString(), response);
    }

    @Test
    @DbUnitDataSet(before = {"StocksRequestStatusMonitoringControllerTest.cpa.before.csv",
            "StocksRequestStatusMonitoringControllerTest.group.before.csv"})
    @DisplayName("CRIT, если LGW не обновил стоки. Ошибка только для главного склада")
    void testErrorWhenCpaSupplierDidntUpdateStocksForGroup() {
        List<Long> expectedSupplierIds = List.of(1L);
        when(lmsClient.getPartner(10L))
                .thenReturn(
                        Optional.of(PartnerResponse.newBuilder().id(1L)
                                .partnerType(PartnerType.SUPPLIER)
                                .stockSyncEnabled(true)
                                .status(PartnerStatus.ACTIVE)
                                .build())
                );
        when(lmsClient.getPartner(20L))
                .thenReturn(
                        Optional.of(PartnerResponse.newBuilder().id(1L)
                                .partnerType(PartnerType.SUPPLIER)
                                .stockSyncEnabled(true)
                                .status(PartnerStatus.ACTIVE)
                                .build()
                        )
                );

        String response = controller.checkLastRequestForStock();
        Assertions.assertEquals(
                MonitoringResult.Status.ERROR.getCode() +
                        ";troubleshooting wiki: https://nda.ya.ru/t/mv77mgtL4rBcLM, not_requested_suppliers_ids" +
                        expectedSupplierIds,
                response
        );
    }
}
