package ru.yandex.market.deliveryintegrationtests.delivery.tests.dropship;

import step.LgwSteps;
import step.LmsSteps;
import step.LomOrderSteps;
import step.PartnerApiSteps;
import step.TrackerSteps;
import org.junit.jupiter.api.AfterEach;
import ru.yandex.market.deliveryintegrationtests.delivery.tests.AbstractTest;

public abstract class AbstractDropshipTest extends AbstractTest {

    protected static final TrackerSteps DELIVERY_TRACKER_STEPS = new TrackerSteps();
    protected static final LomOrderSteps LOM_ORDER_STEPS = new LomOrderSteps();
    protected static final LgwSteps LGW_STEPS = new LgwSteps();
    protected static final LmsSteps LMS_STEPS = new LmsSteps();
    protected PartnerApiSteps partnerApiSteps;

    protected Long lomOrderId;

    @AfterEach
    public void tearDown() {
        /*ORDER_STEPS.cancelOrderIfAllowed(order);*/
    }
}
