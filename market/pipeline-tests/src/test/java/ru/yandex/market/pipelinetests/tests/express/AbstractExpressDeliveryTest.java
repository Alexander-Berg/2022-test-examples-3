package ru.yandex.market.pipelinetests.tests.express;

import step.LgwSteps;
import step.LomOrderSteps;
import step.PartnerApiSteps;
import step.TrackerSteps;

import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.pipelinetests.tests.AbstractTest;

public abstract class AbstractExpressDeliveryTest extends AbstractTest {

    protected static final TrackerSteps DELIVERY_TRACKER_STEPS = new TrackerSteps();
    protected static final LomOrderSteps LOM_ORDER_STEPS = new LomOrderSteps();
    protected static final LgwSteps LGW_STEPS = new LgwSteps();

    protected PartnerApiSteps partnerApiSteps;
    protected long lomOrderId;
    protected WaybillSegmentDto lastMileWaybillSegment;
}
