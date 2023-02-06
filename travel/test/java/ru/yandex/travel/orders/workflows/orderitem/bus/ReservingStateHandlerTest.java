package ru.yandex.travel.orders.workflows.orderitem.bus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.bus.model.BusReservation;
import ru.yandex.travel.bus.model.BusRide;
import ru.yandex.travel.bus.model.BusesPassenger;
import ru.yandex.travel.bus.service.BusesService;
import ru.yandex.travel.bus.service.BusesServiceException;
import ru.yandex.travel.bus.service.BusesServiceRetryableException;
import ru.yandex.travel.buses.backend.proto.worker.EOrderStatus;
import ru.yandex.travel.buses.backend.proto.worker.TBookResponse;
import ru.yandex.travel.buses.backend.proto.worker.TOrder;
import ru.yandex.travel.commons.proto.EErrorCode;
import ru.yandex.travel.orders.entities.BusOrderItem;
import ru.yandex.travel.orders.workflow.order.proto.TServiceCancelled;
import ru.yandex.travel.orders.workflow.orderitem.bus.proto.TFeeCalculationCommit;
import ru.yandex.travel.orders.workflow.orderitem.bus.proto.TReservationCommit;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflows.orderitem.bus.handlers.ReservingStateHandler;
import ru.yandex.travel.workflow.exceptions.RetryableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

public class ReservingStateHandlerTest {

    private BusesService busesService;
    private ReservingStateHandler handler;

    @Before
    public void setUp() {
        busesService = mock(BusesService.class);
        BusProperties busProperties = new BusProperties();
        busProperties.setReservationMaxTries(3);
        busProperties.setReservationRetryDelay(Duration.ofMillis(10));
        handler = new ReservingStateHandler(new SingletonBusesServiceProvider(busesService), busProperties);
    }

    @Test
    public void testOrderReserved() {
        var busOrderItem = createItem(EOrderItemState.IS_RESERVING);
        when(busesService.book(any(), any())).thenReturn(TBookResponse
                .newBuilder()
                .setOrder(TOrder.newBuilder().setStatus(EOrderStatus.OS_BOOKED))
                .build());

        var ctx = testMessagingContext(busOrderItem);
        handler.handleEvent(TReservationCommit.newBuilder().build(), ctx);

        assertThat(busOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_CALCULATING_FEE_BUS);
        verify(busesService).book(any(), any());
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TFeeCalculationCommit.class);
    }

    @Test
    public void testReservationRetryableError() {
        var exception = new BusesServiceRetryableException(new Exception());
        var busOrderItem = createItem(EOrderItemState.IS_RESERVING);
        when(busesService.book(any(), any())).thenThrow(exception);

        var ctx = testMessagingContext(busOrderItem);

        assertThatThrownBy(() -> handler.handleEvent(TReservationCommit.getDefaultInstance(), ctx))
                .isInstanceOf(RetryableException.class);
        assertThat(busOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_RESERVING);
        verify(busesService).book(any(), any());
    }

    @Test
    public void testReservationCancelled() {
        var exception = new BusesServiceException(EErrorCode.EC_GENERAL_ERROR,
                EErrorCode.EC_GENERAL_ERROR.getValueDescriptor().getFullName());
        var busOrderItem = createItem(EOrderItemState.IS_RESERVING);
        when(busesService.book(any(), any())).thenThrow(exception);

        var ctx = testMessagingContext(busOrderItem);
        handler.handleEvent(TReservationCommit.getDefaultInstance(), ctx);

        assertThat(busOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_CANCELLED);
        verify(busesService).book(any(), any());
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TServiceCancelled.class);
    }

    @SuppressWarnings("SameParameterValue")
    private BusOrderItem createItem(EOrderItemState state) {
        var item = new BusOrderItem();
        item.setId(UUID.randomUUID());
        item.setState(state);
        BusReservation reservation = new BusReservation();
        var ride = new BusRide();
        reservation.setRide(ride);
        reservation.getRide().setSupplierId(0);
        reservation.getRide().setRideId("rideId");
        reservation.setEmail("test@test.ru");
        reservation.setPhone("+79876543210");
        reservation.setRequestPassengers(new ArrayList<BusesPassenger>());
        item.setReservation(reservation);
        return item;
    }
}
