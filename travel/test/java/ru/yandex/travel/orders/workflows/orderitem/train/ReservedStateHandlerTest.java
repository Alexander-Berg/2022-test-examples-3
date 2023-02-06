package ru.yandex.travel.orders.workflows.orderitem.train;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.TCancellationStart;
import ru.yandex.travel.orders.workflows.orderitem.train.handlers.ReservedStateHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

public class ReservedStateHandlerTest {
    private ReservedStateHandler handler;

    @Before
    public void setUp() {
        handler = new ReservedStateHandler();
    }

    @Test
    public void testCancellationStart() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_RESERVED);
        var trainOrderItem = factory.createTrainOrderItem();

        handler.handleEvent(TCancellationStart.getDefaultInstance(), testMessagingContext(trainOrderItem));

        assertThat(trainOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_CANCELLING);
    }
}
