package ru.yandex.market.tpl.billing.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.service.checker.CourierIntakeCheckerService;
import ru.yandex.market.tpl.billing.service.checker.CourierOrdersCheckerService;
import ru.yandex.market.tpl.billing.service.checker.CourierShiftCheckerService;
import ru.yandex.market.tpl.billing.util.DateTimeUtil;
import ru.yandex.market.tpl.client.billing.BillingClient;
import ru.yandex.market.tpl.client.billing.dto.BillingIntakeContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingIntakeDto;
import ru.yandex.market.tpl.client.billing.dto.BillingOrderContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingOrderDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserShiftContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserShiftDto;

import static org.mockito.Mockito.when;

public class CheckerServiceTest extends AbstractFunctionalTest {

    private static final LocalDate DATE_2022_01_01 = LocalDate.of(2022, 1, 1);
    private static final LocalDate DATE_2022_01_02 = LocalDate.of(2022, 1, 2);

    @Autowired
    private BillingClient billingClient;

    @Autowired
    private CourierShiftCheckerService courierShiftCheckerService;
    @Autowired
    private CourierOrdersCheckerService courierOrdersCheckerService;
    @Autowired
    private CourierIntakeCheckerService courierIntakeCheckerService;

    @Autowired
    TestableClock clock;

    @BeforeEach
    void mockBillingClient() {
        mockUserShifts();
        mockOrders();
        mockIntakes();
    }

    @DbUnitDataSet(
            before = "/database/service/checkerservice/before/ok_mon.csv",
            after = "/database/service/checkerservice/after/ok_mon.csv")
    @Test
    void testCheckOk() {
        clock.setFixed(Instant.parse("2022-01-03T03:00:00.00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
        courierShiftCheckerService.checkTillToday();
        courierOrdersCheckerService.checkTillToday();
        courierIntakeCheckerService.checkTillToday();
    }

    @DbUnitDataSet(
            before = "/database/service/checkerservice/before/fired_mon.csv",
            after = "/database/service/checkerservice/after/fired_mon.csv")
    @Test
    void testCheckMonFired() {
        clock.setFixed(Instant.parse("2022-01-03T03:00:00.00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
        courierShiftCheckerService.checkTillToday();
        courierOrdersCheckerService.checkTillToday();
        courierIntakeCheckerService.checkTillToday();
    }

    private void mockUserShifts() {
        when(billingClient.findShifts(DATE_2022_01_01)).thenReturn(
                BillingUserShiftContainerDto.builder()
                        .userShifts(
                                List.of(
                                        BillingUserShiftDto.builder()
                                                .shiftDate(DATE_2022_01_01)
                                                .id(1L)
                                                .build(),
                                        BillingUserShiftDto.builder()
                                                .shiftDate(DATE_2022_01_01)
                                                .id(2L)
                                                .build(),
                                        BillingUserShiftDto.builder()
                                                .shiftDate(DATE_2022_01_01)
                                                .id(3L)
                                                .build()
                                )
                        )
                        .build()
        );

        when(billingClient.findShifts(DATE_2022_01_02)).thenReturn(
                BillingUserShiftContainerDto.builder()
                        .userShifts(
                                List.of(
                                        BillingUserShiftDto.builder()
                                                .shiftDate(DATE_2022_01_02)
                                                .id(4L)
                                                .build(),
                                        BillingUserShiftDto.builder()
                                                .shiftDate(DATE_2022_01_02)
                                                .id(5L)
                                                .build()
                                )
                        )
                        .build()
        );
    }

    private void mockOrders() {
        when(billingClient.findOrders(Set.of(1L, 2L))).thenReturn(
                BillingOrderContainerDto.builder()
                        .orders(
                                List.of(
                                        BillingOrderDto.builder()
                                                .userShiftId(1L)
                                                .id(1L)
                                                .deliveryTaskStatus("DELIVERED")
                                                .build(),
                                        BillingOrderDto.builder()
                                                .userShiftId(2L)
                                                .deliveryTaskStatus("DELIVERED")
                                                .id(2L)
                                                .build()
                                )
                        )
                        .build()
        );

        when(billingClient.findOrders(Set.of(3L))).thenReturn(
                BillingOrderContainerDto.builder()
                        .orders(
                                List.of(
                                        BillingOrderDto.builder()
                                                .userShiftId(3L)
                                                .id(3L)
                                                .deliveryTaskStatus("DELIVERED")
                                                .build()
                                )
                        )
                        .build()
        );

        when(billingClient.findOrders(Set.of(4L, 5L))).thenReturn(
                BillingOrderContainerDto.builder()
                        .orders(
                                List.of(
                                        BillingOrderDto.builder()
                                                .userShiftId(4L)
                                                .id(4L)
                                                .deliveryTaskStatus("DELIVERED")
                                                .build(),
                                        BillingOrderDto.builder()
                                                .userShiftId(5L)
                                                .id(5L)
                                                .deliveryTaskStatus("DELIVERED")
                                                .build()
                                )
                        )
                        .build()
        );

        when(billingClient.findOrders(Set.of(4L))).thenReturn(
                BillingOrderContainerDto.builder()
                        .orders(
                                List.of(
                                        BillingOrderDto.builder()
                                                .userShiftId(4L)
                                                .id(4L)
                                                .deliveryTaskStatus("DELIVERED")
                                                .build()
                                )
                        )
                        .build()
        );
    }

    private void mockIntakes() {
        when(billingClient.findIntakes(Set.of(1L, 2L))).thenReturn(
                BillingIntakeContainerDto.builder()
                        .intakes(
                                List.of(
                                        BillingIntakeDto.builder()
                                                .userShiftId(1L)
                                                .taskId(1L)
                                                .collectDropshipTaskStatus("FINISHED")
                                                .build()
                                )
                        )
                        .build()
        );

        when(billingClient.findIntakes(Set.of(3L))).thenReturn(
                BillingIntakeContainerDto.builder()
                        .intakes(
                                List.of()
                        )
                        .build()
        );

        when(billingClient.findIntakes(Set.of(4L, 5L))).thenReturn(
                BillingIntakeContainerDto.builder()
                        .intakes(
                                List.of()
                        )
                        .build()
        );

        when(billingClient.findIntakes(Set.of(4L))).thenReturn(
                BillingIntakeContainerDto.builder()
                        .intakes(
                                List.of()
                        )
                        .build()
        );
    }
}
