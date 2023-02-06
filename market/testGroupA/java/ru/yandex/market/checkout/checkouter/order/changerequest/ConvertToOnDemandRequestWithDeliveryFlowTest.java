package ru.yandex.market.checkout.checkouter.order.changerequest;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.OnDemandType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryCheckpointStatus;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.lavka.YandexLavkaDeliveryFlowTest;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.OrderTypeUtils;
import ru.yandex.market.checkout.checkouter.order.changerequest.ondemand.ConvertToOnDemandReason;
import ru.yandex.market.checkout.checkouter.order.changerequest.ondemand.ConvertToOnDemandRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ondemand.ConvertToOnDemandRequestPayload;
import ru.yandex.market.checkout.checkouter.trace.OrderEditContextHolder;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT;
import static ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryCheckpointStatus.DELIVERY_AT_START_2;
import static ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryCheckpointStatus.DELIVERY_DELIVERED;
import static ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT;
import static ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT;
import static ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryCheckpointStatus.ON_DEMAND_DELIVERY_REQUESTED;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_DEFERRED_COURIER_NEW_DELIVERY_SUBSTATUSES;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;

public class ConvertToOnDemandRequestWithDeliveryFlowTest extends YandexLavkaDeliveryFlowTest {

    @Autowired
    private EventsGetHelper eventsGetHelper;

    @Test
    void fallbackToOnDemandAfter45Checkpoint_shouldSetReadyForLastMileStatusWithEventReason()
            throws Exception {
        checkouterFeatureWriter.writeValue(ENABLE_DEFERRED_COURIER_NEW_DELIVERY_SUBSTATUSES, false);
        Order order = createOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);

        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        order = convertToOnDemand(order);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        assertSubstatusUpdatedWithReason(order.getId(), HistoryEventReason.CONVERT_TO_ON_DEMAND);


        // Пользователь вызывает курьера
        order = callCourier(order);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        // Заказ отправляется курьером пользователю
        order = notifyTracks(order, 20, 3);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        order = notifyTracks(order, 48, 4);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        // Пользователь получает заказ
        order = notifyTracks(order, 49, 5);
        checkStatus(order, DELIVERY, OrderSubstatus.USER_RECEIVED);

        order = notifyTracks(order, 50, 6);
        checkStatus(order, DELIVERED, null);
    }

    @Test
    void fallbackToOnDemandAfter45CheckpointFeatureEnabled() throws Exception {
        checkouterFeatureWriter.writeValue(ENABLE_DEFERRED_COURIER_NEW_DELIVERY_SUBSTATUSES, true);
        Order order = createOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);

        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        order = convertToOnDemand(order);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        // Пользователь вызывает курьера
        order = callCourier(order);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        // Заказ отправляется курьером пользователю
        order = notifyTracks(order, 20, 3);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        order = notifyTracks(order, 48, 4);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        // Пользователь получает заказ
        order = notifyTracks(order, 49, 5);
        checkStatus(order, DELIVERY, OrderSubstatus.USER_RECEIVED);

        order = notifyTracks(order, 50, 6);
        checkStatus(order, DELIVERED, null);
    }

    @Test
    void fallbackToOnDemandWhenCourierWasCalled_shouldFinallyGetToUserReceivedStatus()
            throws Exception {
        checkouterFeatureWriter.writeValue(ENABLE_DEFERRED_COURIER_NEW_DELIVERY_SUBSTATUSES, false);
        Order order = createOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);

        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);
        // Заказ отправляется курьером пользователю
        order = notifyTracks(order, 20, 3);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        order = notifyTracks(order, 48, 4);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        order = convertToOnDemand(order);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);


        // Пользователь получает заказ
        order = notifyTracks(order, 49, 5);
        checkStatus(order, DELIVERY, OrderSubstatus.USER_RECEIVED);

        order = notifyTracks(order, 50, 6);
        checkStatus(order, DELIVERED, null);
    }

    @Test
    void fallbackToOnDemandWhenCourierWasCalledFeatureEnabled() throws Exception {
        checkouterFeatureWriter.writeValue(ENABLE_DEFERRED_COURIER_NEW_DELIVERY_SUBSTATUSES, true);
        Order order = createOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);

        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);
        // Заказ отправляется курьером пользователю
        order = notifyTracks(order, 20, 3);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        order = notifyTracks(order, 48, 4);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        order = convertToOnDemand(order);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);


        // Пользователь получает заказ
        order = notifyTracks(order, 49, 5);
        checkStatus(order, DELIVERY, OrderSubstatus.USER_RECEIVED);

        order = notifyTracks(order, 50, 6);
        checkStatus(order, DELIVERED, null);
    }

    @ParameterizedTest
    @ValueSource(ints = {45, 90})
    void fallbackToOnDemand_afterUserReceived_shouldBeSuccessful(int checkpointStatus) throws Exception {

        Order order = createOrderAndProceedToUserReceived();

        // Откат в on-demand
        order = convertToOnDemand(order);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        order = notifyTracks(order, checkpointStatus, 6);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        // Заказ отправляется курьером пользователю
        order = notifyTracks(order, 20, 7);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        order = notifyTracks(order, 48, 8);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        // Пользователь получает заказ
        order = notifyTracks(order, 49, 9);
        checkStatus(order, DELIVERY, OrderSubstatus.USER_RECEIVED);

        order = notifyTracks(order, 50, 10);
        checkStatus(order, DELIVERED, null);
    }

    @Test
    void when45CheckpointArrivesForNonOnDemand_afterUserReceived_shouldStayInUserReceived()
            throws Exception {
        Order order = createOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        // Заказ отправляется курьером пользователю
        order = notifyTracks(order, 20, 3);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        order = notifyTracks(order, 48, 4);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        // Пользователь получает заказ
        order = notifyTracks(order, 49, 5);
        checkStatus(order, DELIVERY, OrderSubstatus.USER_RECEIVED);

        order = notifyTracks(order, 45, 6);
        order = orderService.getOrder(order.getId());
        checkStatus(order, DELIVERY, OrderSubstatus.USER_RECEIVED);
    }

    @Test
    void when90CheckpointArrivesForNonOnDemand_afterUserReceived_shouldStayInUserReceived()
            throws Exception {
        Order order = createOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        // Заказ отправляется курьером пользователю
        order = notifyTracks(order, 20, 3);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        order = notifyTracks(order, 48, 4);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        // Пользователь получает заказ
        order = notifyTracks(order, 49, 5);
        checkStatus(order, DELIVERY, OrderSubstatus.USER_RECEIVED);

        order = notifyTracks(order, 90, 6);
        order = orderService.getOrder(order.getId());
        checkStatus(order, DELIVERY, OrderSubstatus.USER_RECEIVED);
    }

    @Test
    void deliveryFailedForYandexLavka_afterFallbackToOnDemand_shouldSetReadyForLastMileStatus() throws Exception {
        Order order = createOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        order = notifyTracks(order, 48, 4);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        order = convertToOnDemand(order);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        order = notifyTracks(order, DeliveryCheckpointStatus.DELIVERY_ATTEMPT_FAILED.getId(), 6);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);
    }

    @Test
    void fallbackToOnDemand_afterDeliveryFailedForYandexLavka_shouldSetReadyForLastMileStatusWithReason()
            throws Exception {
        Order order = createOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        order = notifyTracks(order, 48, 4);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        order = notifyTracks(order, DeliveryCheckpointStatus.DELIVERY_ATTEMPT_FAILED.getId(), 6);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        order = convertToOnDemand(order);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        assertSubstatusUpdatedWithReason(order.getId(), HistoryEventReason.CONVERT_TO_ON_DEMAND);
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void fallbackToOnDemandByUserAfter45Checkpoint_shouldSetReadyForLastMileStatusWithEventReason(
            boolean enableLastMileStartedAfterConvertToOnDemandToggle
    ) throws Exception {
        checkouterProperties.setEnableLastMileStartedAfterConvertToOnDemand(
                enableLastMileStartedAfterConvertToOnDemandToggle);
        Order order = createOrderWithDsTrack();

        int trackerCheckpointId = 1;
        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, DELIVERY_ARRIVED_PICKUP_POINT.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        order = notifyTracks(order, ON_DEMAND_DELIVERY_REQUESTED.getId(), ++trackerCheckpointId);
        order = convertToOnDemand(order);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        assertSubstatusUpdatedWithReason(order.getId(), HistoryEventReason.CONVERT_TO_ON_DEMAND);

        // Пользователь вызывает курьера
        order = callCourier(order);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        // Заказ отправляется курьером пользователю
        order = notifyTracks(order, DELIVERY_AT_START_2.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        order = notifyTracks(order, DELIVERY_TRANSPORTATION_RECIPIENT.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        // Пользователь получает заказ
        order = notifyTracks(order, DELIVERY_TRANSMITTED_TO_RECIPIENT.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERY, OrderSubstatus.USER_RECEIVED);

        order = notifyTracks(order, DELIVERY_DELIVERED.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERED, null);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void fallbackToOnDemandByUserAfter45Checkpoint_byReason_shouldSetReadyForLastMileStatusWithEventReason(
            boolean enableLastMileStartedAfterConvertToOnDemandToggle
    ) throws Exception {
        var holder = new OrderEditContextHolder.OrderEditContextAttributesHolder();
        checkouterProperties.setEnableLastMileStartedAfterConvertToOnDemand(
                enableLastMileStartedAfterConvertToOnDemandToggle);
        Order order = createOrderWithDsTrack();

        int trackerCheckpointId = 1;
        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, DELIVERY_ARRIVED_PICKUP_POINT.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        order = convertToOnDemand(order, ConvertToOnDemandReason.CALL_COURIER_BY_USER);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        assertSubstatusUpdatedWithReason(order.getId(), HistoryEventReason.CONVERT_TO_ON_DEMAND);

        // Пользователь вызывает курьера
        order = callCourier(order);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        // Заказ отправляется курьером пользователю
        order = notifyTracks(order, DELIVERY_AT_START_2.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        order = notifyTracks(order, DELIVERY_TRANSPORTATION_RECIPIENT.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        // Пользователь получает заказ
        order = notifyTracks(order, DELIVERY_TRANSMITTED_TO_RECIPIENT.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERY, OrderSubstatus.USER_RECEIVED);

        order = notifyTracks(order, DELIVERY_DELIVERED.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERED, null);

        Assertions.assertThat(holder.getAttributes())
                .containsEntry("changeRequestPayloadReason", ConvertToOnDemandReason.CALL_COURIER_BY_USER.name());
    }

    @ParameterizedTest
    @ValueSource(ints = {31, 48})
    void fallbackToOnDemandByUserAfter31Or48Checkpoint_withToggle_shouldSetLastMileStartedWithEventReason(
            int lastMileStartedCheckpoint
    ) throws Exception {
        checkouterProperties.setEnableLastMileStartedAfterConvertToOnDemand(true);
        Order order = createOrderWithDsTrack();

        int trackerCheckpointId = 1;
        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, DELIVERY_ARRIVED_PICKUP_POINT.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        // Лавка создала свой заказ
        order = notifyTracks(order, lastMileStartedCheckpoint, ++trackerCheckpointId);

        // Откатили в он-деманд
        order = notifyTracks(order, ON_DEMAND_DELIVERY_REQUESTED.getId(), ++trackerCheckpointId);
        order = convertToOnDemand(order);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        assertSubstatusUpdatedWithReason(order.getId(), HistoryEventReason.CONVERT_TO_ON_DEMAND);

        // Заказ отправляется курьером пользователю
        order = notifyTracks(order, DELIVERY_AT_START_2.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        order = notifyTracks(order, DELIVERY_TRANSPORTATION_RECIPIENT.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        // Пользователь получает заказ
        order = notifyTracks(order, DELIVERY_TRANSMITTED_TO_RECIPIENT.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERY, OrderSubstatus.USER_RECEIVED);

        order = notifyTracks(order, DELIVERY_DELIVERED.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERED, null);
    }

    @ParameterizedTest
    @ValueSource(ints = {31, 48})
    void fallbackToOnDemandByUserAfter31Or48Checkpoint_withToggleAndReason_shouldSetLastMileStartedWithEventReason(
            int lastMileStartedCheckpoint
    ) throws Exception {
        checkouterProperties.setEnableLastMileStartedAfterConvertToOnDemand(true);
        Order order = createOrderWithDsTrack();

        int trackerCheckpointId = 1;
        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, DELIVERY_ARRIVED_PICKUP_POINT.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        // Лавка создала свой заказ
        order = notifyTracks(order, lastMileStartedCheckpoint, ++trackerCheckpointId);

        // Откатили в он-деманд
        order = convertToOnDemand(order, ConvertToOnDemandReason.CALL_COURIER_BY_USER);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        assertSubstatusUpdatedWithReason(order.getId(), HistoryEventReason.CONVERT_TO_ON_DEMAND);

        // Заказ отправляется курьером пользователю
        order = notifyTracks(order, DELIVERY_AT_START_2.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        order = notifyTracks(order, DELIVERY_TRANSPORTATION_RECIPIENT.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        // Пользователь получает заказ
        order = notifyTracks(order, DELIVERY_TRANSMITTED_TO_RECIPIENT.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERY, OrderSubstatus.USER_RECEIVED);

        order = notifyTracks(order, DELIVERY_DELIVERED.getId(), ++trackerCheckpointId);
        checkStatus(order, DELIVERED, null);
    }

    @Nonnull
    private Order createOrderWithDsTrack() throws Exception {
        return createLavkaOrderWithDsTrack(BlueParametersProvider.DELIVERY_SERVICE_ID, true);
    }

    @Nonnull
    @Override
    protected Order createLavkaOrderWithDsTrack() throws Exception {
        var order = createLavkaOrderWithDsTrack(BlueParametersProvider.DELIVERY_SERVICE_ID, true);
        assertIsNotOnDemand(order);
        order = convertToOnDemand(order);
        assertIsOnDemand(order, DeliveryFeature.ON_DEMAND_YALAVKA);
        return order;
    }

    @Nonnull
    private Order convertToOnDemand(Order order) {
        return convertToOnDemand(order, null);
    }

    @Nonnull
    private Order convertToOnDemand(Order order, ConvertToOnDemandReason reason) {
        sendConvertToOnDemand(order, reason);
        return orderService.getOrder(order.getId());
    }

    private ChangeRequest sendConvertToOnDemand(Order order, ConvertToOnDemandReason reason) {
        var toOnDemandRequest = new ConvertToOnDemandRequest();
        final var onDemandType = OnDemandType.YALAVKA;
        toOnDemandRequest.setOnDemandType(onDemandType);
        toOnDemandRequest.setReason(reason);
        var editRequest = new OrderEditRequest();
        editRequest.setConvertToOnDemandRequest(toOnDemandRequest);

        var expectedPayload = new ConvertToOnDemandRequestPayload(onDemandType);
        expectedPayload.setReason(reason);

        var response = client.editOrder(order.getId(), ClientRole.SYSTEM, BuyerProvider.UID,
                singletonList(BLUE), editRequest);
        assertThat(response, hasSize(1));
        var changeRequest = response.get(0);
        var payload = (ConvertToOnDemandRequestPayload) changeRequest.getPayload();
        assertEquals(expectedPayload, payload);
        return response.get(0);
    }

    @Nonnull
    private Order createOrderAndProceedToUserReceived() throws Exception {
        Order order = createOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        // Заказ отправляется курьером пользователю
        order = notifyTracks(order, 20, 3);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        order = notifyTracks(order, 48, 4);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        // Курьер по ошибки отмечает, что пользователь получил заказ
        order = notifyTracks(order, 49, 5);
        checkStatus(order, DELIVERY, OrderSubstatus.USER_RECEIVED);

        return order;
    }

    private void assertIsNotOnDemand(Order order) {
        order = orderService.getOrder(order.getId());
        assertFalse(OrderTypeUtils.isOnDemandDelivery(order));
        assertFalse(OrderTypeUtils.getDeliveryFeatures(order).contains(DeliveryFeature.ON_DEMAND_YALAVKA));
        assertFalse(OrderTypeUtils.getDeliveryFeatures(order).contains(DeliveryFeature.ON_DEMAND_MARKET_PICKUP));
    }

    private void assertIsOnDemand(Order order, DeliveryFeature expectedDeliveryFeature) {
        assertTrue(OrderTypeUtils.isOnDemandDelivery(order));
        assertTrue(OrderTypeUtils.getDeliveryFeatures(order).contains(expectedDeliveryFeature));
    }

    private void assertSubstatusUpdatedWithReason(long orderId, HistoryEventReason reason) throws Exception {
        PagedEvents orderHistoryEvents = eventsGetHelper.getOrderHistoryEvents(orderId, Integer.MAX_VALUE);
        assertTrue(orderHistoryEvents.getItems().stream()
                .anyMatch(e -> HistoryEventType.ORDER_SUBSTATUS_UPDATED == e.getType() &&
                        e.getReason() == HistoryEventReason.CONVERT_TO_ON_DEMAND));
    }
}
