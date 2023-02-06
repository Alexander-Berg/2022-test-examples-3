package ru.yandex.travel.orders.workflows.orderitem.bus;

import java.time.Duration;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.bus.model.BusReservation;
import ru.yandex.travel.bus.model.BusRide;
import ru.yandex.travel.bus.model.BusesOrder;
import ru.yandex.travel.bus.service.BusesService;
import ru.yandex.travel.bus.service.BusesServiceException;
import ru.yandex.travel.bus.service.BusesServiceRetryableException;
import ru.yandex.travel.buses.backend.proto.worker.TCancelBookingResponse;
import ru.yandex.travel.commons.proto.EErrorCode;
import ru.yandex.travel.orders.entities.BusOrderItem;
import ru.yandex.travel.orders.workflow.order.proto.TServiceCancelled;
import ru.yandex.travel.orders.workflow.orderitem.bus.proto.TCancellationCommit;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflows.orderitem.bus.handlers.CancellingStateHandler;
import ru.yandex.travel.workflow.exceptions.RetryableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

public class CancellingStateHandlerTest {

    private BusesService busesService;
    private CancellingStateHandler handler;

    @Before
    public void setUp() {
        busesService = mock(BusesService.class);
        var properties = new BusProperties();
        properties.setCancellationMaxTries(1);
        properties.setCancellationRetryDelay(Duration.ZERO);
        handler = new CancellingStateHandler(new SingletonBusesServiceProvider(busesService), properties);
    }

    @Test
    public void testOrderCancelled() {
        var busOrderItem = createItem(EOrderItemState.IS_CONFIRMED);
        when(busesService.cancelBooking(any())).thenReturn(TCancelBookingResponse.newBuilder().build());

        var ctx = testMessagingContext(busOrderItem);
        handler.handleEvent(TCancellationCommit.newBuilder().build(), ctx);

        assertThat(busOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_CANCELLED);
        verify(busesService).cancelBooking(any());
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TServiceCancelled.class);
    }

    @Test
    public void testRetryableError() {
        var exception = new BusesServiceRetryableException(new Exception());
        var busOrderItem = createItem(EOrderItemState.IS_CONFIRMED);
        when(busesService.cancelBooking(any())).thenThrow(exception);

        var ctx = testMessagingContext(busOrderItem);

        assertThatThrownBy(() -> handler.handleEvent(TCancellationCommit.getDefaultInstance(), ctx))
                .isInstanceOf(RetryableException.class);
        assertThat(busOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_CONFIRMED);
        verify(busesService).cancelBooking(any());
    }

    @Test
    public void testCancellationError() {
        var exception = new BusesServiceException(EErrorCode.EC_GENERAL_ERROR,
                EErrorCode.EC_GENERAL_ERROR.getValueDescriptor().getFullName());
        var busOrderItem = createItem(EOrderItemState.IS_CONFIRMED);
        when(busesService.cancelBooking(any())).thenThrow(exception);

        var ctx = testMessagingContext(busOrderItem);
        handler.handleEvent(TCancellationCommit.getDefaultInstance(), ctx);

        assertThat(busOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_CANCELLED);
        verify(busesService).cancelBooking(any());
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
        BusesOrder order = new BusesOrder();
        order.setId("orderId");
        reservation.setOrder(order);
        item.setReservation(reservation);
        return item;
    }
}
