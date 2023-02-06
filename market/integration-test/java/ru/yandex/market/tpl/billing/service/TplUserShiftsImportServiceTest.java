package ru.yandex.market.tpl.billing.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.api.model.transport.RoutingVehicleType;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.checker.QueueTaskChecker;
import ru.yandex.market.tpl.billing.queue.model.DatePayload;
import ru.yandex.market.tpl.billing.queue.model.LongIdPayload;
import ru.yandex.market.tpl.billing.queue.model.QueueType;
import ru.yandex.market.tpl.client.billing.BillingClient;
import ru.yandex.market.tpl.client.billing.dto.BillingShiftType;
import ru.yandex.market.tpl.client.billing.dto.BillingUserShiftContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserShiftDto;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TplUserShiftsImportServiceTest extends AbstractFunctionalTest {

    private static final LocalDate SHIFT_DATE = LocalDate.parse("2021-03-12");

    @Autowired
    private BillingClient billingClient;

    @Autowired
    private TplUserShiftsImportService tplUserShiftsImportService;

    @Autowired
    private QueueTaskChecker queueTaskChecker;

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplusershiftsimport/before/empty.csv",
            after = "/database/service/tplusershiftsimport/after/user_shifts_imported.csv")
    void importTplUserShifts() {
        testImportTplUserShifts();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplusershiftsimport/before/user_shift_1.csv",
            after = "/database/service/tplusershiftsimport/after/user_shifts_imported.csv")
    void importTplUserShiftsIncludingOneWhichAlreadyExists() {
        testImportTplUserShifts();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplusershiftsimport/before/user_shift_6.csv",
            after = "/database/service/tplusershiftsimport/after/user_shifts_imported_6_totally.csv")
    void importTplUserShiftsAdditionallyToOneWhichAlreadyExists() {
        testImportTplUserShifts();

        queueTaskChecker.assertQueueTaskNotCreated(
                QueueType.TPL_SHIFT_PROCESSING, new LongIdPayload(REQUEST_ID, 6L)
        );
    }

    @Test
    @DbUnitDataSet(after = "/database/service/tplusershiftsimport/after/empty.csv")
    void importNoTplUserShifts() {
        testImportNoTplUserShifts();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplusershiftsimport/before/user_shift_1.csv",
            after = "/database/service/tplusershiftsimport/after/user_shift_1.csv")
    void importNoTplUserShiftsWhenAlreadyExists() {
        testImportNoTplUserShifts();
    }

    private void testImportTplUserShifts() {
        when(billingClient.findShifts(eq(SHIFT_DATE))).thenReturn(getBillingUserShiftContainerDto());

        tplUserShiftsImportService.importTplUserShifts(SHIFT_DATE);

        verify(billingClient).findShifts(eq(SHIFT_DATE));

        queueTaskChecker.assertQueueTaskCreated(
                QueueType.TPL_SHIFT_PROCESSING, new DatePayload(REQUEST_ID, SHIFT_DATE)
        );
    }

    private void testImportNoTplUserShifts() {
        when(billingClient.findShifts(eq(SHIFT_DATE)))
                .thenReturn(new BillingUserShiftContainerDto(Collections.emptyList()));

        tplUserShiftsImportService.importTplUserShifts(SHIFT_DATE);

        verify(billingClient).findShifts(eq(SHIFT_DATE));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    private BillingUserShiftContainerDto getBillingUserShiftContainerDto() {
        return new BillingUserShiftContainerDto(List.of(
                new BillingUserShiftDto(
                        1L, BillingShiftType.AVTO, LocalDate.parse("2021-03-12"), 1L,
                        1L, "1", 1L, "1",
                        null, null, null, 0L, 3, 0
                ),
                new BillingUserShiftDto(
                        2L, BillingShiftType.AVTO, LocalDate.parse("2021-03-12"), 1L,
                        2L, "2", 1L, "1",
                        1000, RoutingVehicleType.COMMON, "o001oo164",
                        0L, 3, 0
                ),
                new BillingUserShiftDto(
                        3L, BillingShiftType.AVTO, LocalDate.parse("2021-03-12"), 1L,
                        2L, "2", 1L, "1",
                        1150, RoutingVehicleType.COMMON, null,
                        0L, 3, 0
                ),
                new BillingUserShiftDto(
                        4L, BillingShiftType.AVTO, LocalDate.parse("2021-03-12"), 1L,
                        2L, "2", 1L, "1",
                        65589, RoutingVehicleType.COMMON, null,
                        0L, 3, 0
                ),
                new BillingUserShiftDto(
                        5L,
                        BillingShiftType.VELO,
                        LocalDate.parse("2021-03-12"),
                        1L,
                        2L,
                        "2",
                        1L,
                        "1",
                        67500,
                        RoutingVehicleType.YANDEX_DRIVE,
                        null,
                        0L,
                        3,
                        0
                )
        ));
    }
}
