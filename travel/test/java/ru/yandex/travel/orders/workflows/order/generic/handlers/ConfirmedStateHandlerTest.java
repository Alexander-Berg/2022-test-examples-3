package ru.yandex.travel.orders.workflows.order.generic.handlers;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.orders.commons.proto.EDisplayOrderType;
import ru.yandex.travel.orders.entities.GenericOrder;
import ru.yandex.travel.orders.entities.TrainOrderItem;
import ru.yandex.travel.orders.entities.context.OrderStateContext;
import ru.yandex.travel.orders.repository.OrderRefundRepository;
import ru.yandex.travel.orders.repository.VoucherRepository;
import ru.yandex.travel.orders.services.NotificationHelper;
import ru.yandex.travel.orders.services.buses.BusNotificationHelper;
import ru.yandex.travel.orders.services.promo.PromoCodeApplicationService;
import ru.yandex.travel.orders.services.train.RebookingService;
import ru.yandex.travel.orders.workflow.order.generic.proto.EOrderState;
import ru.yandex.travel.orders.workflow.orderitem.train.proto.TChangeRegistrationStatus;
import ru.yandex.travel.orders.workflow.train.proto.TRegistrationStatusChange;
import ru.yandex.travel.orders.workflow.train.proto.TRegistrationStatusChanged;
import ru.yandex.travel.orders.workflow.train.proto.TTrainTicketsUpdated;
import ru.yandex.travel.orders.workflow.train.proto.TUpdateTrainTickets;
import ru.yandex.travel.orders.workflows.order.generic.GenericWorkflowService;
import ru.yandex.travel.train.model.PassengerCategory;
import ru.yandex.travel.train.model.TrainPassenger;
import ru.yandex.travel.train.model.TrainReservation;
import ru.yandex.travel.train.model.TrainTicket;
import ru.yandex.travel.workflow.StateContext;
import ru.yandex.travel.workflow.entities.Workflow;
import ru.yandex.travel.workflow.repository.WorkflowRepository;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static ru.yandex.travel.orders.entities.context.SimpleMultiItemBatchTaskState.IN_PROGRESS;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.getMessageFor;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

public class ConfirmedStateHandlerTest {
    private final NotificationHelper notificationHelper = mock(NotificationHelper.class);
    private final BusNotificationHelper busNotificationHelper = mock(BusNotificationHelper.class);

    private final ConfirmedStateHandler handler = new ConfirmedStateHandler(
            Mockito.mock(VoucherRepository.class),
            Mockito.mock(WorkflowRepository.class),
            Mockito.mock(OrderRefundRepository.class),
            new GenericWorkflowService(Mockito.mock(PromoCodeApplicationService.class),
                    Mockito.mock(RebookingService.class),
                    notificationHelper,
                    busNotificationHelper,
                    Clock.systemDefaultZone())
    );

    @Test
    public void testRegistrationChangeStart() {
        GenericOrder order = testComplexTrainOrder();
        TRegistrationStatusChange event = TRegistrationStatusChange.newBuilder()
                .addAllBlankIds(List.of(1, 2, 4)).build();
        StateContext<EOrderState, GenericOrder> ctx = testMessagingContext(order);
        assertThat(order.getStateContext().getTrainRegistrationChangeTasks().isEmpty()).isTrue();

        handler.handleRegistrationStatusChange(event, ctx);

        assertThat(ctx.getScheduledEvents()).hasSize(2);
        TChangeRegistrationStatus m1 = getMessageFor(ctx, order.getOrderItems().get(0));
        TChangeRegistrationStatus m2 = getMessageFor(ctx, order.getOrderItems().get(1));
        assertThat(m1.getBlankIdsList()).isEqualTo(List.of(1, 2));
        assertThat(m2.getBlankIdsList()).isEqualTo(List.of(4));
        assertThat(order.getStateContext().getTrainRegistrationChangeTasks().isEmpty()).isFalse();

        assertThatThrownBy(() -> handler.handleRegistrationStatusChange(event, ctx))
                .hasMessageContaining("Batch trainRegistrationChange is not empty");
    }

    @Test
    public void testRegistrationChangeStart_babyBlank() {
        GenericOrder order = testComplexTrainOrder();
        TRegistrationStatusChange event = TRegistrationStatusChange.newBuilder()
                .addAllBlankIds(List.of(1)).build();
        StateContext<EOrderState, GenericOrder> ctx = testMessagingContext(order);
        assertThat(order.getStateContext().getTrainRegistrationChangeTasks().isEmpty()).isTrue();

        TrainOrderItem segment1 = (TrainOrderItem) order.getOrderItems().get(0);
        TrainPassenger passenger2 = segment1.getReservation().getPassengers().get(1);
        passenger2.setCategory(PassengerCategory.BABY);
        passenger2.getTicket().setBlankId(1); // the same as the first's passenger

        handler.handleRegistrationStatusChange(event, ctx);

        assertThat(ctx.getScheduledEvents()).hasSize(1);
        TChangeRegistrationStatus m1 = getMessageFor(ctx, order.getOrderItems().get(0));
        assertThat(m1.getBlankIdsList()).isEqualTo(List.of(1));
    }

    @Test
    public void testRegistrationChangeEnd() {
        GenericOrder order = testComplexTrainOrder();
        TRegistrationStatusChanged e1 = TRegistrationStatusChanged.newBuilder().setServiceId("0-0-0-0-1").build();
        TRegistrationStatusChanged e2 = TRegistrationStatusChanged.newBuilder().setServiceId("0-0-0-0-2").build();
        StateContext<EOrderState, GenericOrder> ctx = testMessagingContext(order);

        order.getStateContext().getTrainRegistrationChangeTasks().addTask(UUID.fromString("0-0-0-0-1"), IN_PROGRESS);
        order.getStateContext().getTrainRegistrationChangeTasks().addTask(UUID.fromString("0-0-0-0-2"), IN_PROGRESS);
        handler.handleRegistrationStatusChanged(e1, ctx);
        assertThat(order.isUserActionScheduled()).isTrue();
        assertThat(order.getStateContext().getTrainRegistrationChangeTasks().isEmpty()).isFalse();

        handler.handleRegistrationStatusChanged(e2, ctx);
        assertThat(order.isUserActionScheduled()).isFalse();
        assertThat(order.getStateContext().getTrainRegistrationChangeTasks().isEmpty()).isTrue();
    }

    @Test
    public void testTicketsUpdateEvents() {
        GenericOrder order = testComplexTrainOrder();
        StateContext<EOrderState, GenericOrder> ctx = testMessagingContext(order);
        assertThat(order.getStateContext().getUpdateTicketsTasks().isEmpty()).isTrue();

        handler.handle(TUpdateTrainTickets.newBuilder().build(), ctx);

        assertThat(ctx.getScheduledEvents()).hasSize(2);
        assertThat((Object) getMessageFor(ctx, order.getOrderItems().get(0))).isNotNull();
        assertThat((Object) getMessageFor(ctx, order.getOrderItems().get(1))).isNotNull();
        assertThat(order.getStateContext().getUpdateTicketsTasks().isEmpty()).isFalse();

        assertThatThrownBy(() -> handler.handle(TUpdateTrainTickets.newBuilder().build(), ctx))
                .hasMessageContaining("Batch updateTickets is not empty");

        handler.handleTicketsUpdated(TTrainTicketsUpdated.newBuilder()
                .setServiceId(order.getOrderItems().get(0).getId().toString()).build(), ctx);
        assertThat(order.isUserActionScheduled()).isTrue();
        assertThat(order.getStateContext().getUpdateTicketsTasks().isEmpty()).isFalse();

        handler.handleTicketsUpdated(TTrainTicketsUpdated.newBuilder()
                .setServiceId(order.getOrderItems().get(1).getId().toString()).build(), ctx);
        assertThat(order.isUserActionScheduled()).isFalse();
        assertThat(order.getStateContext().getUpdateTicketsTasks().isEmpty()).isTrue();
    }

    private GenericOrder testComplexTrainOrder() {
        GenericOrder order = new GenericOrder();
        order.addOrderItem(testTrainOrderItem(List.of(1, 2, 3)));
        order.addOrderItem(testTrainOrderItem(List.of(4)));
        OrderStateContext stateContext = new OrderStateContext();
        stateContext.init(EDisplayOrderType.DT_TRAIN);
        order.setStateContext(stateContext);
        order.setUserActionScheduled(true);
        return order;
    }

    private TrainOrderItem testTrainOrderItem(List<Integer> ticketBlackIds) {
        TrainOrderItem orderItem = new TrainOrderItem();
        orderItem.setReservation(TrainReservation.builder()
                .passengers(ticketBlackIds.stream()
                        .map(blankId -> TrainPassenger.builder()
                                .ticket(TrainTicket.builder()
                                        .blankId(blankId)
                                        .build())
                                .build())
                        .collect(toList()))
                .build());
        // workflow-related properties
        orderItem.setId(UUID.randomUUID());
        orderItem.setWorkflow(Workflow.createWorkflowForEntity(orderItem));
        return orderItem;
    }
}
