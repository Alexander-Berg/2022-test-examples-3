package ru.yandex.market.deliveryintegrationtests.delivery.tests.manual_orders_doubles;

import client.CheckouterClient;
import client.DsFfMockClient;
import step.CheckouterSteps;
import step.LomOrderSteps;
import step.LrmSteps;
import step.TplPvzSteps;
import step.TrackerSteps;

import ru.yandex.market.deliveryintegrationtests.delivery.tests.AbstractTest;

public class AbstractManualOrderDoubleTest extends AbstractTest {
    protected static final DsFfMockClient MOCK_CLIENT = new DsFfMockClient();
    protected static final TrackerSteps DELIVERY_TRACKER_STEPS = new TrackerSteps();

    protected static final LomOrderSteps LOM_ORDER_STEPS = new LomOrderSteps();
    protected static final CheckouterSteps ORDER_STEPS = new CheckouterSteps();
    protected static final LrmSteps LRM_STEPS = new LrmSteps();
    protected static final TplPvzSteps PVZ_STEPS = new TplPvzSteps();
    protected static final Long userUid = 1626132422L;

    protected Long lomOrderId;
}
