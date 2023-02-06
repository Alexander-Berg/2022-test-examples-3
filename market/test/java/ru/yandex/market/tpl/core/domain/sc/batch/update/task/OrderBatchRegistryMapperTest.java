package ru.yandex.market.tpl.core.domain.sc.batch.update.task;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.outbound.dto.BatchRegistryDto;
import ru.yandex.market.tpl.api.model.outbound.dto.BoxesBatchRegistryDto;
import ru.yandex.market.tpl.api.model.outbound.dto.IntervalBatchRegistryDto;
import ru.yandex.market.tpl.api.model.outbound.dto.LogisticPointsBatchRegistryDto;
import ru.yandex.market.tpl.core.domain.shift.task.projection.OrderProjection;
import ru.yandex.market.tpl.core.domain.shift.task.projection.PickupPointDeliveryTaskProjection;
import ru.yandex.market.tpl.core.domain.shift.task.projection.PickupPointProjection;
import ru.yandex.market.tpl.core.domain.shift.task.projection.ShiftProjection;
import ru.yandex.market.tpl.core.domain.shift.task.projection.UserShiftWLockerOrdersProjection;
import ru.yandex.market.tpl.core.domain.user.projection.UserProjection;

import static org.assertj.core.api.Assertions.assertThat;

class OrderBatchRegistryMapperTest {

    public static final LocalDate SHIFT_DATE = LocalDate.of(2021, Month.OCTOBER, 22);

    @Test
    void buildOrderBatchRegistry() {
        OrderBatchRegistryMapper mapper = new OrderBatchRegistryMapper();
        UserShiftWLockerOrdersProjection userShift = UserShiftWLockerOrdersProjection.builder()
                .id(1L)
                .user(UserProjection.builder()
                        .id(1L)
                        .uid(123L)
                        .build())
                .shift(ShiftProjection.builder()
                        .id(1L)
                        .shiftDate(SHIFT_DATE)
                        .zoneOffset(ZoneOffset.ofHours(3))
                        .startTime(LocalTime.of(8, 0))
                        .build())
                .shiftStartTime(LocalTime.of(9, 0))
                .lockerDeliveryTasks(List.of(
                        PickupPointDeliveryTaskProjection.builder()
                                .id(1L)
                                .pickupPoint(PickupPointProjection.builder()
                                        .name("ПВЗ на Академической")
                                        .addressString("Академическая, д.1")
                                        .logisticPointId(321L)
                                        .build())
                                .orders(List.of(OrderProjection.builder()
                                        .externalId("123")
                                        .orderPlaceBarcodes(List.of("123-1"))
                                        .build()))
                                .build()
                ))
                .build();

        BatchRegistryDto batchRegistryDto = mapper.buildOrderBatchRegistry(userShift);

        BatchRegistryDto expected = BatchRegistryDto.builder()
                .id("tpl_1")
                .courierId(1L)
                .courierUid(123L)
                .interval(IntervalBatchRegistryDto.builder()
                        .from(OffsetDateTime.of(SHIFT_DATE, LocalTime.of(8, 0), ZoneOffset.ofHours(3)))
                        .to(OffsetDateTime.of(SHIFT_DATE, LocalTime.of(9, 0), ZoneOffset.ofHours(3)))
                        .build())
                .logisticPoints(List.of(LogisticPointsBatchRegistryDto.builder()
                        .id(321L)
                        .name("ПВЗ на Академической")
                        .addressString("Академическая, д.1")
                        .build()))
                .boxes(List.of(BoxesBatchRegistryDto.builder()
                        .destinationLogisticPointId(321L)
                        .orderYandexId("123")
                        .build()
                ))
                .build();
        assertThat(batchRegistryDto).isEqualTo(expected);
    }
}
