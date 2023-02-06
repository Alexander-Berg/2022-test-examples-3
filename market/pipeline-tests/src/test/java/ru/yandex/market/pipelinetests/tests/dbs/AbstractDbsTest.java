package ru.yandex.market.pipelinetests.tests.dbs;

import org.junit.jupiter.api.AfterEach;
import step.LomOrderSteps;
import step.TplPvzSteps;
import step.TrackerSteps;

import ru.yandex.market.pipelinetests.tests.AbstractTest;

public abstract class AbstractDbsTest extends AbstractTest {
    protected static final TrackerSteps DELIVERY_TRACKER_STEPS = new TrackerSteps();
    protected static final LomOrderSteps LOM_ORDER_STEPS = new LomOrderSteps();
    protected static final TplPvzSteps TPL_PVZ_STEPS = new TplPvzSteps();

    @AfterEach
    public void tearDown() {
        ORDER_STEPS.cancelOrderIfAllowed(order);
    }
}
