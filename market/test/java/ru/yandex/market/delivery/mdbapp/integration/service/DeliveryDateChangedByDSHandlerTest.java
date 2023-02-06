package ru.yandex.market.delivery.mdbapp.integration.service;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import steps.orderSteps.OrderEventSteps;
import steps.orderSteps.OrderSteps;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.date.get.OrderDeliveryDateRequestDto;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.PartnerExternalParamsRepository;
import ru.yandex.market.delivery.mdbapp.util.DeliveryDateUpdateReason;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;

import static java.util.Calendar.DATE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class DeliveryDateChangedByDSHandlerTest {

    private static final Long TEST_PARTNER_ID = 987L;
    private static final Long TEST_POST_PARTNER_ID = 139L;
    private static final String TRACK_CODE = "track_code";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private DeliveryDateChangedByDSHandler deliveryDateChangedByDSHandler;

    @Mock
    private QueueProducer<OrderDeliveryDateRequestDto> producer;

    @Mock
    private PartnerExternalParamsRepository partnerExternalParamsRepository;

    @Captor
    private ArgumentCaptor<EnqueueParams<OrderDeliveryDateRequestDto>> captor;

    @Before
    public void setUp() {
        when(partnerExternalParamsRepository.existsByTypeAndPartnerIdAndActiveTrue(any(), anyLong())).thenReturn(true);
        deliveryDateChangedByDSHandler = new DeliveryDateChangedByDSHandler(producer, partnerExternalParamsRepository);
    }

    @Test
    public void testNoNeededCheckpointsExist() {
        OrderHistoryEvent eventBefore = prepareEvent(Collections.singletonList(130));

        deliveryDateChangedByDSHandler.handle(eventBefore);

        Mockito.verify(producer, Mockito.never()).enqueue(any());
    }

    @Test
    public void testOrderCancelled() {
        OrderHistoryEvent eventBefore = prepareEvent(Collections.singletonList(46));

        eventBefore.getOrderAfter().setStatus(OrderStatus.CANCELLED);

        deliveryDateChangedByDSHandler.handle(eventBefore);

        Mockito.verify(producer, Mockito.never()).enqueue(any());
    }

    @Test
    public void testOrderHasCancellationRequest() {
        OrderHistoryEvent eventBefore = prepareEvent(Collections.singletonList(46));

        eventBefore.getOrderAfter().setCancellationRequest(new CancellationRequest(OrderSubstatus.BROKEN_ITEM, ""));

        deliveryDateChangedByDSHandler.handle(eventBefore);

        Mockito.verify(producer, Mockito.never()).enqueue(any());
    }

    @Test
    public void test44CheckpointsExist() {
        handleEventTwice(44, DeliveryDateUpdateReason.DELAYED_DUE_EXTERNAL_CONDITIONS, null, 2, true);
    }

    @Test
    public void test46CheckpointsExist() {
        handleEventTwice(46, DeliveryDateUpdateReason.USER_MOVED_DELIVERY_DATES_BY_DS, null, 2, true);
    }

    @Test
    public void test47CheckpointsExist() {
        handleEventTwice(47, DeliveryDateUpdateReason.DELIVERY_SERVICE_DELAYED, null, 2, true);
    }

    @Test
    public void testSelectedHighestOfProperStatuses() {
        OrderHistoryEvent eventBefore = prepareEvent(Arrays.asList(1, 2, 3, 44, 46, 47), true);

        OrderHistoryEvent eventAfter = deliveryDateChangedByDSHandler.handle(eventBefore);

        Mockito.verify(producer, Mockito.only()).enqueue(captor.capture());

        OrderDeliveryDateRequestDto payload = captor.getValue().getPayload();

        softly.assertThat(eventAfter).as("Event has not changed").isEqualTo(eventBefore);
        softly.assertThat(payload).as("Proper dto should be put to queue")
            .isEqualTo(new OrderDeliveryDateRequestDto("123", TEST_POST_PARTNER_ID,
                null, TRACK_CODE, DeliveryDateUpdateReason.DELIVERY_SERVICE_DELAYED));
    }

    @Test
    public void test49CheckpointsExist() {
        handleEventTwice(49, DeliveryDateUpdateReason.DELIVERY_SERVICE_HANDED_IN, 12L, 1, false);
    }

    @Test
    public void test50CheckpointsExist() {
        handleEventTwice(50, DeliveryDateUpdateReason.DELIVERY_SERVICE_DELIVERED, 12L, 1, false);
    }

    @Test
    public void testDelayAndDeliverHandledSameTime() {
        OrderHistoryEvent eventBefore = prepareEvent(Arrays.asList(44, 46, 47, 49, 50), true);

        OrderHistoryEvent eventAfter = deliveryDateChangedByDSHandler.handle(eventBefore);


        Mockito.verify(producer, Mockito.times(2)).enqueue(captor.capture());

        List<OrderDeliveryDateRequestDto> payload = captor.getAllValues().stream()
            .map(EnqueueParams::getPayload)
            .collect(Collectors.toList());

        softly.assertThat(eventAfter).as("Event has not changed").isEqualTo(eventBefore);
        softly.assertThat(payload).as("2 times called getOrderDeliveryDate").hasSize(2);
        softly.assertThat(payload).as("Proper dto should be put to queue")
            .containsExactlyInAnyOrder(new OrderDeliveryDateRequestDto("123", TEST_POST_PARTNER_ID,
                    null, TRACK_CODE, DeliveryDateUpdateReason.DELIVERY_SERVICE_DELAYED),
                new OrderDeliveryDateRequestDto("123", TEST_POST_PARTNER_ID,
                    12L, TRACK_CODE, DeliveryDateUpdateReason.DELIVERY_SERVICE_DELIVERED));
    }

    private void handleEventTwice(
        int checkpointId,
        DeliveryDateUpdateReason deliveryDateUpdateReason,
        Long parcelId,
        int producerExecutions,
        boolean postOrder
    ) {
        List<Integer> checkpoints = Arrays.asList(checkpointId, 2, checkpointId);

        OrderHistoryEvent eventBefore = prepareEvent(checkpoints, postOrder);

        deliveryDateChangedByDSHandler.handle(eventBefore);

        eventBefore.getOrderBefore()
            .getDelivery()
            .getParcels().get(0)
            .getTracks().get(0)
            .setCheckpoints(createCheckpoints(Collections.singletonList(checkpointId)));

        OrderHistoryEvent eventAfter = deliveryDateChangedByDSHandler.handle(eventBefore);

        Mockito.verify(producer, Mockito.times(producerExecutions)).enqueue(captor.capture());

        OrderDeliveryDateRequestDto payload = captor.getValue().getPayload();

        softly.assertThat(eventAfter).as("Event has not changed").isEqualTo(eventBefore);
        softly.assertThat(payload).as("Proper dto should be put to queue")
            .isEqualTo(new OrderDeliveryDateRequestDto(
                "123",
                postOrder ? TEST_POST_PARTNER_ID : TEST_PARTNER_ID,
                parcelId,
                TRACK_CODE,
                deliveryDateUpdateReason
            ));
    }

    private OrderHistoryEvent prepareEvent(List<Integer> checkpoints) {
        return prepareEvent(checkpoints, false);
    }

    private OrderHistoryEvent prepareEvent(List<Integer> checkpoints, boolean postOrder) {
        OrderHistoryEvent orderHistoryEvent = OrderEventSteps.getOrderHistoryEvent();
        orderHistoryEvent.setOrderBefore(prepareOrder(postOrder));
        orderHistoryEvent.setOrderAfter(prepareOrder(postOrder));
        orderHistoryEvent.getOrderAfter().getDelivery().setDeliveryDates(prepareDeliveryDates());
        orderHistoryEvent.setType(HistoryEventType.TRACK_CHECKPOINT_CHANGED);
        orderHistoryEvent.getOrderAfter().getDelivery().getParcels().get(0).getTracks().get(0)
            .setCheckpoints(createCheckpoints(checkpoints));
        orderHistoryEvent.getOrderAfter().getDelivery().getParcels().get(0).getTracks().get(0)
            .setTrackCode(TRACK_CODE);

        return orderHistoryEvent;
    }

    private Order prepareOrder(boolean postOrder) {
        Order order = OrderSteps.getFilledOrder();
        order.getDelivery().setDeliveryServiceId(postOrder ? TEST_POST_PARTNER_ID : TEST_PARTNER_ID);
        order.getDelivery().setType(postOrder ? DeliveryType.POST : DeliveryType.DELIVERY);
        return order;

    }

    private DeliveryDates prepareDeliveryDates() {
        return new DeliveryDates(
            getDate(1),
            getDate(2),
            LocalTime.of(12, 0),
            LocalTime.of(15, 0)
        );
    }

    private static Date getDate(int day) {
        Calendar instance = Calendar.getInstance();
        instance.set(YEAR, 2019);
        instance.set(MONTH, 1);
        instance.set(DATE, day);
        return instance.getTime();
    }

    private List<TrackCheckpoint> createCheckpoints(List<Integer> args) {
        return args.stream()
            .map(i -> new TrackCheckpoint(
                null, null, null, null, null, null, null, i)
            )
            .collect(Collectors.toList());
    }
}
