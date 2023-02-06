package ru.yandex.travel.orders.workflows.orderitem.bus;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.bus.model.BusReservation;
import ru.yandex.travel.bus.model.BusRide;
import ru.yandex.travel.bus.model.BusTicketStatus;
import ru.yandex.travel.bus.model.BusesOrder;
import ru.yandex.travel.bus.model.BusesTicket;
import ru.yandex.travel.bus.service.BusesService;
import ru.yandex.travel.bus.service.BusesServiceException;
import ru.yandex.travel.bus.service.BusesServiceRetryableException;
import ru.yandex.travel.buses.backend.proto.worker.EOrderStatus;
import ru.yandex.travel.buses.backend.proto.worker.ETicketStatus;
import ru.yandex.travel.buses.backend.proto.worker.TConfirmResponse;
import ru.yandex.travel.buses.backend.proto.worker.TOrder;
import ru.yandex.travel.buses.backend.proto.worker.TTicket;
import ru.yandex.travel.commons.proto.EErrorCode;
import ru.yandex.travel.orders.entities.BusOrderItem;
import ru.yandex.travel.orders.workflow.order.proto.TServiceCancelled;
import ru.yandex.travel.orders.workflow.order.proto.TServiceConfirmed;
import ru.yandex.travel.orders.workflow.orderitem.bus.proto.TConfirmationCommit;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflows.orderitem.bus.handlers.ConfirmingStateHandler;
import ru.yandex.travel.workflow.exceptions.RetryableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

public class ConfirmingStateHandlerTest {

    private BusesService busesService;
    private ConfirmingStateHandler handler;

    @Before
    public void setUp() {
        busesService = mock(BusesService.class);
        var properties = new BusProperties();
        properties.setConfirmationMaxTries(1);
        properties.setConfirmationRetryDelay(Duration.ZERO);
        handler = new ConfirmingStateHandler(new SingletonBusesServiceProvider(busesService), properties);
    }

    @Test
    public void testOrderConfirmed() {
        var busOrderItem = createItem(EOrderItemState.IS_CONFIRMING);
        when(busesService.confirm(any())).thenReturn(TConfirmResponse
                .newBuilder()
                .setOrder(TOrder.newBuilder()
                        .setId(busOrderItem.getPayload().getOrder().getId())
                        .addTickets(TTicket.newBuilder()
                                .setId("ticketId1")
                                .setStatus(ETicketStatus.TS_SOLD)
                                .build())
                        .setStatus(EOrderStatus.OS_SOLD))
                .build());

        var ctx = testMessagingContext(busOrderItem);
        handler.handleEvent(TConfirmationCommit.newBuilder().build(), ctx);

        assertThat(busOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_CONFIRMED);
        assertThat(busOrderItem.getPayload().getOrder().getTickets().get(0).getStatus()).isEqualTo(BusTicketStatus.SOLD);
        verify(busesService).confirm(any());
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TServiceConfirmed.class);
    }

    @Test
    public void testConfirmingRetryableError() {
        var exception = new BusesServiceRetryableException(new Exception());
        var busOrderItem = createItem(EOrderItemState.IS_CONFIRMING);
        when(busesService.confirm(any())).thenThrow(exception);

        var ctx = testMessagingContext(busOrderItem);

        assertThatThrownBy(() -> handler.handleEvent(TConfirmationCommit.getDefaultInstance(), ctx))
                .isInstanceOf(RetryableException.class);
        assertThat(busOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_CONFIRMING);
        verify(busesService).confirm(any());
    }

    @Test
    public void testConfirmingCancelled() {
        var exception = new BusesServiceException(EErrorCode.EC_GENERAL_ERROR,
                EErrorCode.EC_GENERAL_ERROR.getValueDescriptor().getFullName());
        var busOrderItem = createItem(EOrderItemState.IS_CONFIRMING);
        when(busesService.confirm(any())).thenThrow(exception);

        var ctx = testMessagingContext(busOrderItem);
        handler.handleEvent(TConfirmationCommit.getDefaultInstance(), ctx);

        assertThat(busOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_CANCELLED);
        verify(busesService).confirm(any());
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
        BusesTicket ticket = new BusesTicket();
        ticket.setId("ticketId1");
        order.setTickets(List.of(ticket));
        reservation.setOrder(order);
        item.setReservation(reservation);
        return item;
    }
}
