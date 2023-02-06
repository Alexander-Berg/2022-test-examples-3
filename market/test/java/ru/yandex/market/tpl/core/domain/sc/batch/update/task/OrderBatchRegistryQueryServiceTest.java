package ru.yandex.market.tpl.core.domain.sc.batch.update.task;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.IntStreamEx;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.tpl.api.model.outbound.dto.BatchRegistryDto;
import ru.yandex.market.tpl.core.domain.shift.task.projection.PickupPointDeliveryTaskProjection;
import ru.yandex.market.tpl.core.domain.shift.task.projection.UserShiftWLockerOrdersProjection;

import static org.assertj.core.api.Assertions.assertThat;

class OrderBatchRegistryQueryServiceTest {

    @Test
    void createOrderBatchRegistryWhen() {
        var orderBatchUserPropertyQueryService = Mockito.mock(OrderBatchUserPropertyQueryService.class);
        var orderBatchRegistryUserShiftQueryService = Mockito.mock(OrderBatchRegistryUserShiftQueryService.class);
        var orderBatchTaskFilterService = Mockito.mock(OrderBatchTaskFilterService.class);
        var orderBatchRegistryMapper = Mockito.mock(OrderBatchRegistryMapper.class);

        Map<Long, List<Long>> userIdsToOrderIds = Map.of(
                1L, List.of(1L, 2L),
                2L, List.of(3L), //filtered in orderBatchUserPropertyQueryService
                3L, List.of(4L)
        );
        LocalDate shiftDate = LocalDate.now();

        Mockito.when(orderBatchUserPropertyQueryService.filterUserIdsWithBatchesEnabled(userIdsToOrderIds.keySet()))
                .thenReturn(Set.of(1L, 3L));
        UserShiftWLockerOrdersProjection user1ShiftIn = createUserShift(2);
        UserShiftWLockerOrdersProjection user1ShiftOut = createUserShift(1);
        UserShiftWLockerOrdersProjection user3ShiftIn = createUserShift(1);
        UserShiftWLockerOrdersProjection user3ShiftOut = createUserShift(0);
        Mockito.when(orderBatchRegistryUserShiftQueryService.fetchUserShiftWithOrdersForBatches(
                Map.of(
                        1L, List.of(1L, 2L),
                        3L, List.of(4L)
                ),
                shiftDate
        )).thenReturn(List.of(user1ShiftIn));
        Mockito.when(orderBatchTaskFilterService.withEligibleOrdersForBatches(user1ShiftIn))
                .thenReturn(user1ShiftOut);
        Mockito.when(orderBatchTaskFilterService.withEligibleOrdersForBatches(user3ShiftIn))
                .thenReturn(user3ShiftOut);
        Mockito.when(orderBatchRegistryMapper.buildOrderBatchRegistry(user1ShiftOut))
                .thenReturn(BatchRegistryDto.builder()
                        .courierId(1L)
                        .build());


        var orderBatchRegistryQueryService = new OrderBatchRegistryQueryService(
                orderBatchUserPropertyQueryService,
                orderBatchRegistryUserShiftQueryService,
                orderBatchTaskFilterService,
                orderBatchRegistryMapper
        );

        var orderBatchRegistryMap = orderBatchRegistryQueryService.createOrderBatchRegistry(
                userIdsToOrderIds,
                shiftDate
        );

        Mockito.verify(orderBatchRegistryMapper).buildOrderBatchRegistry(user1ShiftOut);
        Mockito.verifyNoMoreInteractions(orderBatchRegistryMapper);

        assertThat(orderBatchRegistryMap).containsKeys(1L);
    }

    private UserShiftWLockerOrdersProjection createUserShift(int taskCount) {
        List<PickupPointDeliveryTaskProjection> tasks = IntStreamEx.range(taskCount)
                .mapToObj(i -> PickupPointDeliveryTaskProjection.builder()
                        .build())
                .toList();
        return UserShiftWLockerOrdersProjection.builder()
                .lockerDeliveryTasks(tasks)
                .build();
    }
}
