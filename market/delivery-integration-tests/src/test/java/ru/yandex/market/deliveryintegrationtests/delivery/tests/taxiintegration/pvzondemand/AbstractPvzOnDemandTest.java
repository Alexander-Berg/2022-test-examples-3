package ru.yandex.market.deliveryintegrationtests.delivery.tests.taxiintegration.pvzondemand;

import client.DsFfMockClient;
import step.BlueFApiSteps;
import step.LgwSteps;
import step.LomOrderSteps;
import step.TaxiSteps;
import step.TrackerSteps;
import ru.qatools.properties.Property;
import ru.yandex.market.deliveryintegrationtests.delivery.tests.AbstractTest;

public abstract class AbstractPvzOnDemandTest extends AbstractTest {

    @Property("delivery.ondemandDS")
    protected static long ondemandDS;
    @Property("delivery.partnerPVZKurakin")
    protected static Long pickupPartner;

    protected static final DsFfMockClient MOCK_CLIENT = new DsFfMockClient();

    protected static final TrackerSteps DELIVERY_TRACKER_STEPS = new TrackerSteps();
    protected static final BlueFApiSteps BLUE_F_API_STEPS = new BlueFApiSteps();
    protected static final LomOrderSteps LOM_ORDER_STEPS = new LomOrderSteps();
    protected static final TaxiSteps TAXI_STEPS = new TaxiSteps();
    protected static final LgwSteps LGW_STEPS = new LgwSteps();


    protected Long lomOrderId;

}
