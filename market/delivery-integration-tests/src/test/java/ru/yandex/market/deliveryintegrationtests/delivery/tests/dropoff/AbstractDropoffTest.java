package ru.yandex.market.deliveryintegrationtests.delivery.tests.dropoff;

import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import step.LmsSteps;
import step.NesuBackOfficeSteps;

public abstract class AbstractDropoffTest {
    protected static final NesuBackOfficeSteps NESU_STEPS = new NesuBackOfficeSteps();
    protected static final LmsSteps LMS_STEPS = new LmsSteps();

    @Property("dropoff.pickup-point-type")
    protected static String PICKUP_POINT_TYPE;

    @Property("dropoff.logistic-point-id")
    protected static long DROPOFF_LOGISTIC_POINT_ID;
    @Property("dropoff.partner-id")
    protected static long DROPOFF_PARTNER_ID;
    @Property("dropoff.return-sorting-center-partner-id")
    protected static long DROPOFF_RETURN_SORTING_CENTER_PARTNER_ID;
    @Property("dropoff.logistic-segment-id")
    protected static long DROPOFF_LOGISTIC_SEGMENT_ID;
    @Property("dropoff.logistic-service-id")
    protected static long DROPOFF_LOGISTIC_SERVICE_ID;

    @Property("dropoff.shop-id")
    protected static long SHOP_ID;
    @Property("dropoff.shop-partner-id")
    protected static long SHOP_PARTNER_ID;
    @Property("dropoff.user-id")
    protected static long USER_ID;

    AbstractDropoffTest() {
        PropertyLoader.newInstance().populate(this);
    }
}
