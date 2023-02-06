package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryCheckpointStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.common.time.TestableClock;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.util.tracker.MockTrackerHelper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType.CARRIER;

public class OrderOptionsAvailabilityTest extends AbstractWebTestBase {

    private static final OptionAvailability CALL_COURIER_AVAILABILITY =
            new OptionAvailability(AvailableOptionType.CALL_COURIER);
    private static final DeliveryCheckpointStatus CALL_COURIER_AVAILABILITY_FROM_CHECKPOINT =
            DeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT;
    private static final DeliveryCheckpointStatus CALL_COURIER_AVAILABILITY_TO_CHECKPOINT =
            DeliveryCheckpointStatus.DELIVERY_COURIER_SEARCH;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private WireMockServer trackerMock;
    @Autowired
    private NotifyTracksHelper notifyTracksHelper;
    @Autowired
    @Qualifier("clock")
    private TestableClock testableClock;
    @Value("${market.checkout.lms.callCourierToDeliveryMinutesThreshold}")
    private long callCourierToDeliveryMinutesThreshold;

    private static Stream<Arguments> falseConjunctionBooleans() {
        return Stream.of(
                new Object[]{false, false},
                new Object[]{false, true},
                new Object[]{true, false}
        ).map(Arguments::of);
    }

    private static Stream<Arguments> regionsWithHourDiff() {
        return Stream.of(
                new Object[]{213, 0}, // Москва
                new Object[]{75, 7}, // Владивосток
                new Object[]{22, -1} // Калининград
        ).map(Arguments::of);
    }

    @Test
    public void forDeferredCourierOrder_betweenStartAndEndCheckpoint_shouldReturnCallCourierOption() throws Exception {
        checkouterProperties.setEnabledCallCourierAvailableOption(true);

        Parameters parameters = BlueParametersProvider.blueOrderWithDeferredCourierDelivery();
        var order = orderCreateHelper.createOrder(parameters);

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        setupTrack(order, BlueParametersProvider.DELIVERY_SERVICE_ID);

        long trackerCheckpointId = 0;
        sendCheckpoint(++trackerCheckpointId, DeliveryCheckpointStatus.DELIVERY_AT_START);
        assertOptionAvailability(order, CALL_COURIER_AVAILABILITY, true);

        sendCheckpoint(++trackerCheckpointId, CALL_COURIER_AVAILABILITY_FROM_CHECKPOINT);
        assertOptionAvailability(order, CALL_COURIER_AVAILABILITY, false);

        sendCheckpoint(++trackerCheckpointId, CALL_COURIER_AVAILABILITY_TO_CHECKPOINT);
        assertOptionAvailability(order, CALL_COURIER_AVAILABILITY, true);
    }

    @Test
    public void forWideSlotDeferredCourierOrder_betweenStartAndEndCheckpoint_shouldNotReturnCallCourierOption()
            throws Exception {
        checkouterProperties.setEnabledCallCourierAvailableOption(true);

        Parameters parameters = BlueParametersProvider.blueOrderWithDeferredCourierDelivery(9, 20);
        var order = orderCreateHelper.createOrder(parameters);

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        setupTrack(order, BlueParametersProvider.DELIVERY_SERVICE_ID);

        long trackerCheckpointId = 0;
        sendCheckpoint(++trackerCheckpointId, DeliveryCheckpointStatus.DELIVERY_AT_START);
        assertOptionAvailability(order, CALL_COURIER_AVAILABILITY, true);

        sendCheckpoint(++trackerCheckpointId, CALL_COURIER_AVAILABILITY_FROM_CHECKPOINT);
        assertOptionAvailability(order, CALL_COURIER_AVAILABILITY, true);

        sendCheckpoint(++trackerCheckpointId, CALL_COURIER_AVAILABILITY_TO_CHECKPOINT);
        assertOptionAvailability(order, CALL_COURIER_AVAILABILITY, true);
    }

    @ParameterizedTest
    @MethodSource("falseConjunctionBooleans")
    public void forNonDeferredCourierOrDisabledToggle_shouldNotReturnCallCourierOption(boolean isDeferredCourier,
                                                                                       boolean toggleEnabled)
            throws Exception {
        checkouterProperties.setEnabledCallCourierAvailableOption(toggleEnabled);

        Parameters parameters = isDeferredCourier
                ? BlueParametersProvider.blueOrderWithDeferredCourierDelivery()
                : BlueParametersProvider.defaultBlueOrderParameters();
        var order = orderCreateHelper.createOrder(parameters);

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        setupTrack(order, BlueParametersProvider.DELIVERY_SERVICE_ID);

        long trackerCheckpointId = 0;
        sendCheckpoint(++trackerCheckpointId, CALL_COURIER_AVAILABILITY_FROM_CHECKPOINT);
        assertOptionAvailability(order, CALL_COURIER_AVAILABILITY, true);
    }

    @ParameterizedTest
    @MethodSource("regionsWithHourDiff")
    public void forDeferredCourierOrderInRegion_tooCloseToDeliveryDate_shouldNotReturnCallCourierOption(long regionId,
                                                                                                        long hourDiff)
            throws Exception {
        checkouterProperties.setEnabledCallCourierAvailableOption(true);

        final int hourFrom = 13;
        final int hourTo = 14;
        Parameters parameters = BlueParametersProvider.blueOrderWithDeferredCourierDelivery(hourFrom, hourTo);
        parameters.getOrder().getDelivery().setRegionId(regionId);
        parameters.getReportParameters().setRegionId(regionId);
        parameters.getBuiltMultiCart().setBuyerRegionId(regionId);
        var order = orderCreateHelper.createOrder(parameters);

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        setupTrack(order, BlueParametersProvider.DELIVERY_SERVICE_ID);

        long trackerCheckpointId = 0;
        sendCheckpoint(++trackerCheckpointId, CALL_COURIER_AVAILABILITY_FROM_CHECKPOINT);
        assertOptionAvailability(order, CALL_COURIER_AVAILABILITY, false);

        var now = ZonedDateTime.ofInstant(testableClock.instant(), testableClock.getZone());
        testableClock.setFixed(now
                .plusDays(1)
                .withHour(hourFrom).withMinute(0)
                .minusHours(hourDiff) // hour difference between Moscow and other region
                .minusMinutes(callCourierToDeliveryMinutesThreshold + 1).toInstant(), ZoneId.systemDefault());
        assertOptionAvailability(order, CALL_COURIER_AVAILABILITY, false);

        now = ZonedDateTime.ofInstant(testableClock.instant(), testableClock.getZone());
        testableClock.setFixed(now
                .plusMinutes(2)
                .toInstant(), ZoneId.systemDefault());
        assertOptionAvailability(order, CALL_COURIER_AVAILABILITY, true);
    }

    private void setupTrack(Order order, long deliveryServiceId) throws Exception {
        orderDeliveryHelper.addTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                new Track("iddqd", deliveryServiceId) {{
                    setDeliveryServiceType(CARRIER);
                }},
                ClientInfo.SYSTEM);

        MockTrackerHelper.mockGetDeliveryServices(deliveryServiceId, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        tmsTaskHelper.runRegisterDeliveryTrackTaskV2();
    }

    private void sendCheckpoint(long trackerCheckpointId, DeliveryCheckpointStatus checkpoint) throws Exception {
        notifyTracksHelper.notifyTracks(
                DeliveryTrackProvider.getDeliveryTrack(
                        MockTrackerHelper.TRACKER_ID,
                        checkpoint.getId(),
                        trackerCheckpointId
                )
        );
    }

    private void assertOptionAvailability(Order order,
                                          OptionAvailability expectedOption,
                                          boolean reverted) {

        List<OrderOptionAvailability> orderOptionsAvailabilities = client.getOrderOptionsAvailabilities(
                Set.of(order.getId()),
                ClientRole.USER,
                1L
        );

        assertThat(orderOptionsAvailabilities, hasSize(1));
        final OrderOptionAvailability optionAvailability = orderOptionsAvailabilities.get(0);
        assertThat(optionAvailability, hasProperty("orderId", is(order.getId())));
        if (reverted) {
            assertThat(optionAvailability.getAvailableOptions(), not(contains(expectedOption)));
        } else {
            assertThat(optionAvailability.getAvailableOptions(), contains(expectedOption));
        }
    }
}
