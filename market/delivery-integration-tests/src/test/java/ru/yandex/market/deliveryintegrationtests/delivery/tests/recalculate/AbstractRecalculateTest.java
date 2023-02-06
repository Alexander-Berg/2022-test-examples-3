package ru.yandex.market.deliveryintegrationtests.delivery.tests.recalculate;

import java.util.stream.Stream;

import client.DsFfMockClient;
import dto.responses.lgw.LgwTaskFlow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.provider.Arguments;
import ru.qatools.properties.Property;
import step.CapacityStorageSteps;
import step.CheckouterSteps;
import step.LgwSteps;
import step.LmsSteps;
import step.LomOrderSteps;
import step.PartnerApiSteps;
import step.TrackerSteps;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.deliveryintegrationtests.delivery.tests.AbstractTest;

public abstract class AbstractRecalculateTest extends AbstractTest {

    @Property("delivery.pickpoint")
    protected static long pickpointServiceId;
    @Property("delivery.ondemandDS")
    protected static long ondemandDS;

    protected static long successDeliveryDateUpdatePVZLogisticPoint = 10000977915L;
    protected static long failDeliveryDateUpdatePVZLogisticPoint = 10001017767L;

    protected static final DsFfMockClient MOCK_CLIENT = new DsFfMockClient();

    protected static final CapacityStorageSteps CAPACITY_STORAGE_STEPS = new CapacityStorageSteps();
    protected static final TrackerSteps DELIVERY_TRACKER_STEPS = new TrackerSteps();
    protected static final LomOrderSteps LOM_ORDER_STEPS = new LomOrderSteps();
    protected static final CheckouterSteps CHECKOUTER_STEPS = new CheckouterSteps();
    protected static final LgwSteps LGW_STEPS = new LgwSteps();
    protected static final LmsSteps LMS_STEPS = new LmsSteps();
    protected PartnerApiSteps partnerApiSteps;

    protected Long lomOrderId;

    public static Stream<Arguments> getParams() {
        return Stream.of(
            Arguments.arguments(
                successDeliveryDateUpdatePVZLogisticPoint,
                LgwTaskFlow.DS_UPDATE_ORDER_DELIVERY_DATE_SUCCESS,
                "успех в последней миле"
            ),
            Arguments.arguments(
                failDeliveryDateUpdatePVZLogisticPoint,
                LgwTaskFlow.DS_UPDATE_ORDER_DELIVERY_DATE_ERROR,
                "фейл в последней миле"
            )
        );
    }

    protected void prepareSegmentsForUpdate(Long firstMileTrackerId, Long middleMileTrackerId, Long lastMileTrackerId) {
        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            firstMileTrackerId,
            OrderDeliveryCheckpointStatus.SORTING_CENTER_TRANSMITTED
        );

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            middleMileTrackerId,
            OrderDeliveryCheckpointStatus.DELIVERY_AT_START
        );

        DELIVERY_TRACKER_STEPS.addOrderCheckpointToTracker(
            lastMileTrackerId,
            OrderDeliveryCheckpointStatus.DELIVERY_AT_START
        );
    }

    @AfterEach
    public void tearDown() {
        ORDER_STEPS.cancelOrderIfAllowed(order);
    }
}
