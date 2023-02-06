package ru.yandex.market.pipelinetests.tests.lavka;

import org.junit.jupiter.api.AfterEach;
import step.LomOrderSteps;
import step.TrackerSteps;
import step.TristeroOrderSteps;

import ru.yandex.market.pipelinetests.tests.AbstractTest;

public abstract class AbstractLavkaTest extends AbstractTest {
    protected static final TristeroOrderSteps TRISTERO_ORDER_STEPS = new TristeroOrderSteps();
    protected static final TrackerSteps DELIVERY_TRACKER_STEPS = new TrackerSteps();
    protected static final LomOrderSteps LOM_ORDER_STEPS = new LomOrderSteps();

    protected long lomOrderId;
    protected long orderId;

    @AfterEach
    public void tearDown() {
        ORDER_STEPS.cancelOrderIfAllowed(order);
    }

}
