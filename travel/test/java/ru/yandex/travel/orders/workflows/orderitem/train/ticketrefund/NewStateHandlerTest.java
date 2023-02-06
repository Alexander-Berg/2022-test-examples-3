package ru.yandex.travel.orders.workflows.orderitem.train.ticketrefund;

import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.orders.entities.TrainOrderItem;
import ru.yandex.travel.orders.entities.TrainTicketRefund;
import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflow.orderitem.train.ticketrefund.proto.ETrainTicketRefundState;
import ru.yandex.travel.orders.workflow.orderitem.train.ticketrefund.proto.TRefundAll;
import ru.yandex.travel.orders.workflow.orderitem.train.ticketrefund.proto.TRefundOne;
import ru.yandex.travel.orders.workflow.orderitem.train.ticketrefund.proto.TStartRefund;
import ru.yandex.travel.orders.workflows.orderitem.train.ticketrefund.handlers.NewStateHandler;
import ru.yandex.travel.train.model.refund.PassengerRefundInfo;
import ru.yandex.travel.train.model.refund.TicketRefund;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

public class NewStateHandlerTest {
    private NewStateHandler handler;

    @Before
    public void setUp() {
        handler = new NewStateHandler();
    }

    @Test
    public void testStartRefundOne() {
        TrainTicketRefund refund = createRefund();
        refund.getPayload().setItems(refund.getPayload().getItems().subList(0, 1));
        var ctx = testMessagingContext(refund);

        handler.handleEvent(TStartRefund.newBuilder().build(), ctx);

        assertThat(refund.getState()).isEqualTo(ETrainTicketRefundState.RS_REFUNDING);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TRefundOne.class);
    }

    @Test
    public void testStartRefundAll() {
        TrainTicketRefund refund = createRefund();
        var ctx = testMessagingContext(refund);

        handler.handleEvent(TStartRefund.newBuilder().build(), ctx);

        assertThat(refund.getState()).isEqualTo(ETrainTicketRefundState.RS_REFUNDING);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TRefundAll.class);
    }

    private TrainTicketRefund createRefund() {
        TrainTicketRefund refund = new TrainTicketRefund();
        refund.setId(UUID.randomUUID());
        refund.setState(ETrainTicketRefundState.RS_NEW);
        refund.setPayload(new TicketRefund());
        refund.getPayload().setBuyOperationId(31000001);
        var item1 = new PassengerRefundInfo();
        item1.setBlankId(555111);
        var item2 = new PassengerRefundInfo();
        item2.setBlankId(555222);
        var item3 = new PassengerRefundInfo();
        item3.setBlankId(555333);
        refund.getPayload().setItems(List.of(item1, item2, item3));

        TrainOrderItem orderItem = createTrainOrderItem();
        refund.setOrderItem(orderItem);
        return refund;
    }

    private TrainOrderItem createTrainOrderItem() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_CONFIRMED);
        var trainOrderItem = factory.createTrainOrderItem();

        var passenger1 = factory.createTrainPassenger();
        passenger1.getTicket().setBlankId(555111);
        var passenger2 = factory.createTrainPassenger();
        passenger2.getTicket().setBlankId(555222);
        var passenger3 = factory.createTrainPassenger();
        passenger3.getTicket().setBlankId(555333);

        trainOrderItem.getPayload().setPassengers(List.of(passenger1, passenger2, passenger3));
        return trainOrderItem;
    }
}
