package ru.yandex.market.fulfillment.wrap.marschroute.service.order.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.wrap.core.exception.FulfillmentWrapException;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.DeliveryId;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.tracking.TrackingInfo;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.tracking.TrackingStatus;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.tracking.TrackingStatusTexts;
import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDateTime;
import ru.yandex.market.fulfillment.wrap.marschroute.service.order.status.mapping.TrackingStatusToOrderStatusTypeMapping;
import ru.yandex.market.logistic.api.model.common.OrderStatusCategory;
import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.logistic.api.model.common.OrderStatusType.ORDER_ARRIVED_TO_PICKUP_POINT;
import static ru.yandex.market.logistic.api.model.common.OrderStatusType.ORDER_IS_BEING_DELIVERED_TO_RECIPIENT;

class TrackingStatusToOrderStatusTypeMappingTest extends SoftAssertionSupport {

    private final TrackingStatusToOrderStatusTypeMapping mapping = new TrackingStatusToOrderStatusTypeMapping();


    /**
     * Проверяет на соответствие текущего маппинга между статусами маршрута и статусами FF  в следующей таблице:
     * <p>
     * https://wiki.yandex-team.ru/delivery/development/marschroute_order_status_mapping/
     */
    @Test
    void testFulfillmentMappingIsCorrect() throws Exception {
        assertFulfillmentMapping(1, 100);
        assertFulfillmentMapping(2, 100);
        assertFulfillmentMapping(5, 101);
        assertFulfillmentMapping(10, 101);
        assertFulfillmentMapping(11, 113);
        assertFulfillmentMapping(12, 113);
        assertFulfillmentMapping(13, 101);
        assertFulfillmentMapping(14, 101);
        assertFulfillmentMapping(15, 110);
        assertFulfillmentMapping(16, 120);
        assertFulfillmentMapping(17, 130);
        assertFulfillmentMapping(18, 130);

        assertFulfillmentMapping(20, 130);
        assertFulfillmentMapping(21, 130, DeliveryId.OUTSOURCE_COURIER);
        assertFulfillmentMapping(21, 130, DeliveryId.MARSCHROUTE_COURIER);
        assertFulfillmentMapping(21, 130, DeliveryId.PICKUP_POINT);
        assertFulfillmentMapping(22, 130);

        assertFulfillmentMapping(30, 105);
        assertFulfillmentMapping(31, 105);
        assertFulfillmentMapping(32, 105);
        assertFulfillmentMapping(33, 105);
        assertFulfillmentMapping(34, 105);
        assertFulfillmentMapping(35, 105);
        assertFulfillmentMapping(37, 105);

        assertFulfillmentMapping(36, 170);
        assertFulfillmentMapping(50, 130);
        assertFulfillmentMapping(51, 403);
    }

    /**
     * Проверяет на соответствие текущего маппинга между статусами маршрута и статусами Delivery в следующей таблице:
     * <p>
     * https://wiki.yandex-team.ru/delivery/development/marschroute_order_status_mapping/
     */
    @Test
    void testDeliveryMappingIsCorrect() throws Exception {
        assertDeliveryMapping(1, 0);
        assertDeliveryMapping(2, 0);
        assertDeliveryMapping(5, 1);
        assertDeliveryMapping(10, 1);
        assertDeliveryMapping(11, 1);
        assertDeliveryMapping(12, 1);
        assertDeliveryMapping(13, 1);
        assertDeliveryMapping(14, 1);

        assertDeliveryMapping(15, 10);
        assertDeliveryMapping(16, 10);

        assertDeliveryMapping(17, 20);
        assertDeliveryMapping(18, 20);

        assertDeliveryMapping(20, 30);

        assertDeliveryMapping(21, 48, DeliveryId.MARSCHROUTE_COURIER);
        assertDeliveryMapping(21, 48, DeliveryId.OUTSOURCE_COURIER);
        assertDeliveryMapping(21, 45, DeliveryId.PICKUP_POINT);

        assertDeliveryMapping(22, "ssdsdsd", 30);
        assertDeliveryMapping(22, TrackingStatusTexts.AT_THE_PICKUP_POINT_STATUS_TEXT, 45);

        assertDeliveryMapping(25, 70);

        assertDeliveryMapping(30, 410);
        assertDeliveryMapping(31, 410);
        assertDeliveryMapping(32, 410);
        assertDeliveryMapping(33, 410);
        assertDeliveryMapping(34, 410);
        assertDeliveryMapping(35, 410);
        assertDeliveryMapping(37, 410);

        assertDeliveryMapping(36, 80);
        assertDeliveryMapping(50, 50);
        assertDeliveryMapping(51, 403);
    }

    /**
     * Для курьерки статус 21 преобразуется в наш 48
     */
    @Test
    void testConvertStatus21WithCourierDeliveryType() throws Exception {
        assertMapping(DeliveryId.OUTSOURCE_COURIER, TrackingStatus.IN_DELIVERY_SERVICE, ORDER_IS_BEING_DELIVERED_TO_RECIPIENT);
    }

    /**
     * Для ПВЗ статус 21 преобразуется в наш 45
     */
    @Test
    void testConvertStatus21WithPickupDeliveryType() throws Exception {
        assertMapping(DeliveryId.PICKUP_POINT, TrackingStatus.IN_DELIVERY_SERVICE, ORDER_ARRIVED_TO_PICKUP_POINT);
    }

    /**
     * Для Почты трансформация 21 статуса приводит к ошибке.
     */
    @Test
    void testConvertStatus21WithPostDeliveryType() throws Exception {
        softly.assertThatThrownBy(
            () -> this.mapping.map(getTrackingInfo(TrackingStatus.IN_DELIVERY_SERVICE), DeliveryId.POST_RF)
        ).isInstanceOf(FulfillmentWrapException.class);
    }

    private void assertMapping(DeliveryId deliveryId,
                               TrackingStatus trackingStatus,
                               OrderStatusType orderStatusType) {

        TrackingInfo trackingInfo = getTrackingInfo(trackingStatus, null);
        List<OrderStatusType> mapping = this.mapping.map(trackingInfo, deliveryId)
            .stream()
            .filter(statusType -> statusType.getCategory() == orderStatusType.getCategory())
            .collect(Collectors.toList());

        assertThat(mapping)
            .as("Asserting that array contains only 1 element")
            .hasSize(1);

        OrderStatusType statusType = mapping.get(0);

        assertThat(statusType)
            .as("Asserting that element is status [" + orderStatusType.getCode() + "]")
            .isEqualTo(orderStatusType);
    }

    private TrackingInfo getTrackingInfo(TrackingStatus trackingStatus, String statusText) {
        return new TrackingInfo(
            statusText,
            MarschrouteDateTime.create(LocalDateTime.now()),
            trackingStatus
        );
    }

    private TrackingInfo getTrackingInfo(TrackingStatus trackingStatus) {
        return getTrackingInfo(trackingStatus, null);
    }


    private void assertFulfillmentMapping(int marschrouteStatus,
                                          @Nullable Integer fulfillmentStatus) {
        assertMapping(marschrouteStatus, fulfillmentStatus, DeliveryId.MARKET_DELIVERY, OrderStatusCategory.FULFILLMENT);
    }

    private void assertFulfillmentMapping(int marschrouteStatus,
                                          @Nullable Integer fulfillmentStatus,
                                          DeliveryId deliveryId) {
        assertMapping(marschrouteStatus, fulfillmentStatus, deliveryId, OrderStatusCategory.FULFILLMENT);
    }

    private void assertDeliveryMapping(int marschrouteStatus,
                                       @Nullable Integer deliveryStatus) {
        assertMapping(marschrouteStatus, deliveryStatus, DeliveryId.MARSCHROUTE_COURIER, OrderStatusCategory.DELIVERY);
    }

    private void assertDeliveryMapping(int marschrouteStatus,
                                       String statusText,
                                       @Nullable Integer deliveryStatus) {
        assertMapping(marschrouteStatus, statusText, deliveryStatus, DeliveryId.MARSCHROUTE_COURIER, OrderStatusCategory.DELIVERY);
    }

    private void assertDeliveryMapping(int marschrouteStatus,
                                       @Nullable Integer deliveryStatus,
                                       DeliveryId deliveryId) {
        assertMapping(marschrouteStatus, deliveryStatus, deliveryId, OrderStatusCategory.DELIVERY);
    }


    private void assertMapping(int marschrouteStatus,
                               @Nullable Integer orderStatusCode,
                               DeliveryId deliveryId,
                               OrderStatusCategory requiredCategory) {
        assertMapping(marschrouteStatus, null, orderStatusCode, deliveryId, requiredCategory);
    }

    private void assertMapping(int marschrouteStatus,
                               String statusString,
                               @Nullable Integer orderStatusCode,
                               DeliveryId deliveryId,
                               OrderStatusCategory requiredCategory) {

        TrackingInfo trackingInfo = getTrackingInfo(TrackingStatus.create(marschrouteStatus), statusString);
        List<OrderStatusType> filteredStatuses = mapping.map(trackingInfo, deliveryId).stream()
            .filter(status -> isValidCategory(requiredCategory, status))
            .collect(Collectors.toList());

        if (orderStatusCode == null) {
            softly.assertThat(filteredStatuses)
                .as("Assert that there are no statuses mapped")
                .isEmpty();
        } else {
            softly.assertThat(filteredStatuses)
                .as("Assert that there is only 1 mapped status")
                .hasSize(1);

            OrderStatusType expectedStatus = OrderStatusType.createBasedOn(orderStatusCode);

            OrderStatusType actualStatus = filteredStatuses.iterator().next();
            softly.assertThat(actualStatus)
                .as("Asserting that mapped status is [" + expectedStatus + "]")
                .isEqualTo(expectedStatus);
        }
    }

    private boolean isValidCategory(OrderStatusCategory requiredCategory,
                                    OrderStatusType status) {
        OrderStatusCategory statusCategory = status.getCategory();

        return requiredCategory.equals(statusCategory) ||
            OrderStatusCategory.OTHER.equals(statusCategory);
    }
}
