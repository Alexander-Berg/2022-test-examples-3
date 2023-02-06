package ru.yandex.market.tpl.core.query.usershift.mapper;

import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.order.OrderSummaryDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderSummaryDtoMapperTest {

    public static final long ROUTPOINT_ID = 10L;
    @Mock
    private LocationDetailsDtoMapper locationDetailsDtoMapper;
    @InjectMocks
    private OrderSummaryDtoMapper orderSummaryDtoMapper;

    @Test
    void mapCargoToOrderSummaryDto_Cargo_FAIL() {
        //given
        LockerSubtask subtask = buildSubtask(LockerDeliverySubtaskStatus.FAILED);

        //when
        OrderSummaryDto orderSummaryDto = orderSummaryDtoMapper.mapCargoToOrderSummaryDto(subtask,
                new DropoffCargo(), buildMockedMovement());
        //then
        assertEquals(OrderDeliveryTaskStatus.DELIVERY_FAILED, orderSummaryDto.getTaskStatus());
    }

    @Test
    void mapCargoToOrderSummaryDto_CanByReopen() {
        //given
        LockerSubtask subtask = buildSubtask(LockerDeliverySubtaskStatus.FAILED);

        //when
        OrderSummaryDto orderSummaryDto = orderSummaryDtoMapper.mapCargoToOrderSummaryDto(subtask,
                new DropoffCargo(), buildMockedMovement());
        //then
        Assertions.assertThat(orderSummaryDto.getActions())
                .contains(new LockerDeliveryTaskDto.Action(LockerDeliveryTaskDto.ActionType.REOPEN));
    }

    @Test
    void mapCargoToOrderSummaryDto_Cargo_SUCCESS() {
        //given
        LockerSubtask subtask = buildSubtask(LockerDeliverySubtaskStatus.FINISHED);

        //when
        OrderSummaryDto orderSummaryDto = orderSummaryDtoMapper.mapCargoToOrderSummaryDto(subtask,
                new DropoffCargo(), buildMockedMovement());
        //then
        assertEquals(OrderDeliveryTaskStatus.DELIVERED, orderSummaryDto.getTaskStatus());
    }

    private Movement buildMockedMovement() {
        OrderWarehouse orderWarehouse = mock(OrderWarehouse.class);
        Movement mockedMovement = mock(Movement.class);
        when(mockedMovement.getWarehouseTo()).thenReturn(orderWarehouse);
        return mockedMovement;
    }

    private LockerSubtask buildSubtask(LockerDeliverySubtaskStatus status) {
        LockerDeliveryTask lockerDeliveryTask = buildMockedDropOffTask();
        LockerSubtask lockerSubtask = mock(LockerSubtask.class);

        when(lockerSubtask.getTask()).thenReturn(lockerDeliveryTask);
        when(lockerSubtask.getStatus()).thenReturn(status);
        return lockerSubtask;
    }

    private LockerDeliveryTask buildMockedDropOffTask() {
        RoutePoint mockedRP = mock(RoutePoint.class);
        when(mockedRP.getId()).thenReturn(ROUTPOINT_ID);
        LockerSubtask lockerSubtask = mock(LockerSubtask.class);
        when(lockerSubtask.getFailReason()).thenReturn(new OrderDeliveryFailReason(
                OrderDeliveryTaskFailReasonType.PVZ_CLOSED, "fail Comment"
        ));
        LockerDeliveryTask mockedDropOffTask = mock(LockerDeliveryTask.class);
        when(mockedDropOffTask.getRoutePoint()).thenReturn(mockedRP);
        when(mockedDropOffTask.isInTerminalStatus()).thenReturn(true);
        when(mockedDropOffTask.streamSubtask()).thenReturn(
                StreamEx.of(lockerSubtask), StreamEx.of(lockerSubtask)
        );
        return mockedDropOffTask;
    }
}
