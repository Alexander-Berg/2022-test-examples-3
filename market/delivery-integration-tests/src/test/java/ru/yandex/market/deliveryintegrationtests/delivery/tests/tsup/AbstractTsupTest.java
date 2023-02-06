package ru.yandex.market.deliveryintegrationtests.delivery.tests.tsup;

import client.DsFfMockClient;
import step.FfwfApiSteps;
import step.LgwSteps;
import step.LmsSteps;
import step.LomOrderSteps;
import step.ScApiSteps;
import step.ScIntSteps;
import step.TMSteps;
import step.TsupSteps;
import step.TrackerSteps;
import ru.yandex.market.deliveryintegrationtests.delivery.tests.AbstractTest;

public abstract class AbstractTsupTest extends AbstractTest {

    protected static final TrackerSteps DELIVERY_TRACKER_STEPS = new TrackerSteps();
    protected static final DsFfMockClient DS_FF_MOCK_CLIENT = new DsFfMockClient();
    protected static final FfwfApiSteps FFWF_API_STEPS = new FfwfApiSteps();
    protected static final LomOrderSteps LOM_ORDER_STEPS = new LomOrderSteps();
    protected static final LmsSteps LMS_STEPS = new LmsSteps();
    protected static final LgwSteps LGW_STEPS = new LgwSteps();
    protected static final ScApiSteps SC_STEPS = new ScApiSteps();
    protected static final ScIntSteps SCINT_STEPS = new ScIntSteps();
    protected static final TMSteps TM_STEPS = new TMSteps();
    protected static final TsupSteps TSUP_STEPS = new TsupSteps();
}
