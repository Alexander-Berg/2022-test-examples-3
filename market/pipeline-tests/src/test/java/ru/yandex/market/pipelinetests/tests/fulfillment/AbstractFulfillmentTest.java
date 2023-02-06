package ru.yandex.market.pipelinetests.tests.fulfillment;

import client.DsFfMockClient;
import org.junit.jupiter.api.AfterEach;
import step.CheckouterSteps;
import step.LomOrderSteps;
import step.LrmSteps;
import step.TplPvzSteps;
import step.TrackerSteps;

import ru.yandex.market.pipelinetests.tests.AbstractTest;

public abstract class AbstractFulfillmentTest extends AbstractTest {

    protected static final DsFfMockClient MOCK_CLIENT = new DsFfMockClient();
    protected static final TrackerSteps DELIVERY_TRACKER_STEPS = new TrackerSteps();

    protected static final LomOrderSteps LOM_ORDER_STEPS = new LomOrderSteps();
    protected static final CheckouterSteps ORDER_STEPS = new CheckouterSteps();
    protected static final LrmSteps LRM_STEPS = new LrmSteps();
    protected static final TplPvzSteps PVZ_STEPS = new TplPvzSteps();

    protected Long lomOrderId;

    @AfterEach
    public void tearDown() {
        ORDER_STEPS.cancelOrderIfAllowed(order);
    }
}
