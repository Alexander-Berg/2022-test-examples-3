package ru.yandex.market.pipelinetests.tests.dropship;

import org.junit.jupiter.api.AfterEach;
import step.LomOrderSteps;
import step.PartnerApiSteps;
import step.TrackerSteps;

import ru.yandex.market.pipelinetests.tests.AbstractTest;

public abstract class AbstractDropshipTest extends AbstractTest {

    protected static final LomOrderSteps LOM_ORDER_STEPS = new LomOrderSteps();
    protected static final TrackerSteps DELIVERY_TRACKER_STEPS = new TrackerSteps();
    protected PartnerApiSteps partnerApiSteps;

    protected Long lomOrderId;

    @AfterEach
    public void tearDown() {
        ORDER_STEPS.cancelOrderIfAllowed(order);
    }
}
