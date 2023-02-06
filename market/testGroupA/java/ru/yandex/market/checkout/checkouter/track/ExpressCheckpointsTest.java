package ru.yandex.market.checkout.checkouter.track;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.controllers.oms.TrackerNotificationController;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.DeliveryTrack;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.NotificationResultStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.NotificationResults;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.providers.DeliveryTrackCheckpointProvider;
import ru.yandex.market.checkout.providers.DeliveryTrackMetaProvider;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_ARRIVED_TO_SENDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_FOUND;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_IN_TRANSIT_TO_SENDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_NOT_DELIVER_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_NOT_FOUND;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_RECEIVED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_RETURNED_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_RETURNS_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_SEARCH;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_DELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PICKUP_SERVICE_RECEIVED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.STARTED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_RECEIVED;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.blueNonFulfilmentOrderWithExpressPickupDelivery;

/**
 * Проверяет изменение подстатуса для экспресс заказов через ручку
 * {@link TrackerNotificationController#notifyTracks /notify-tracks}.
 *
 * @author Vadim Lyalin
 * @see ru.yandex.market.checkout.checkouter.delivery.ExpressOrderCondition
 */
class ExpressCheckpointsTest extends AbstractWebTestBase {

    @Autowired
    protected OrderCreateHelper orderCreateHelper;
    @Autowired
    protected OrderStatusHelper orderStatusHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private NotifyTracksHelper notifyTracksHelper;

    /**
     * Проверяет переходы экспресс заказа при возврате мерчу 60 -> 70 -> 80.
     */
    @Test
    void testExpressReturn() throws Exception {
        Order order = initOrder(DELIVERY, false);
        DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(
                String.valueOf(order.getId()),
                // Порядок чекпоинтов намеренно изменён на обратный.
                // Логистика может выслать чекпоинты в таком порядке, но нужно обработать их корректно.
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(1000944078, 80),
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(1000944077, 70),
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(1000914924, 60)
        );

        notifyTracksHelper.notifyTracksForActions(deliveryTrack)
                .andExpect(status().isOk())
                .andExpect(jsonPath("results[0].status").value("OK"));

        order = orderService.getOrder(order.getId());
        assertThat(order)
                .returns(CANCELLED, Order::getStatus)
                .returns(COURIER_RETURNED_ORDER, Order::getSubstatus);
    }

    /**
     * Проверяет корректные переходы экспресс заказа для чекпоинтов 31-36, 45, 60, 70, 80 в подстатусы.
     *
     * @param checkpoint        добавляемый чекпоинт
     * @param fromStatus        текущий статус заказа
     * @param expectedSubstatus ожидаемый подстатус заказа
     */
    @ParameterizedTest
    @MethodSource("testExpressCheckpointParams")
    void testExpressCheckpoint(
            int checkpoint,
            OrderStatus fromStatus,
            OrderSubstatus expectedSubstatus,
            boolean expressPickup
    ) throws Exception {
        Order order = initOrder(fromStatus, expressPickup);
        DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(
                String.valueOf(order.getId()),
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(checkpoint)
        );

        notifyTracksHelper.notifyTracks(deliveryTrack);
        order = orderService.getOrder(order.getId());
        assertThat(order)
                .returns(expectedSubstatus.getStatus(), Order::getStatus)
                .returns(expectedSubstatus, Order::getSubstatus);
    }

    /**
     * Полный тест по переходу между подстатусами для экспресса курьером.
     *
     * @param checkpoints список чекпоинтов, которые прилетают для заказа, и оджидаемых подстатусов
     */
    @ParameterizedTest(name = "{index}")
    @MethodSource("testExpressCheckpointsCompletelyParams")
    void testExpressCheckpointsCompletely(List<Pair<Integer, OrderSubstatus>> checkpoints)
            throws Exception {
        Order order = initOrder(PROCESSING, false);

        checkpoints.forEach(pair -> {
            DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(
                    String.valueOf(order.getId()),
                    DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(pair.getKey())
            );

            try {
                NotificationResults notificationResults = notifyTracksHelper.notifyTracksForResult(deliveryTrack);
                NotificationResultStatus status = notificationResults.getResults().iterator().next().getStatus();
                assertThat(status).isEqualTo(NotificationResultStatus.OK);
                Order updatedOrder = orderService.getOrder(order.getId());
                assertThat(updatedOrder).returns(pair.getValue(), Order::getSubstatus);
            } catch (Exception e) {
                fail(e.getMessage());
            }
        });
    }

    /**
     * Полный тест по переходу между подстатусами для экспресса в ПВЗ.
     *
     * @param checkpoints список чекпоинтов, которые прилетают для заказа, и оджидаемых подстатусов
     */
    @ParameterizedTest(name = "{index}")
    @MethodSource("testExpressPickupCheckpointsCompletelyParams")
    void testExpressPickupCheckpointsCompletely(
            OrderSubstatus substatus,
            List<Pair<Integer, OrderSubstatus>> checkpoints
    ) throws Exception {
        Order order = initOrder(PROCESSING, true);
        orderStatusHelper.updateOrderStatus(order.getId(), substatus.getStatus(), substatus);

        checkpoints.forEach(pair -> {
            DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(
                    String.valueOf(order.getId()),
                    DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(pair.getKey())
            );

            try {
                notifyTracksHelper.notifyTracks(deliveryTrack);
                Order updatedOrder = orderService.getOrder(order.getId());
                assertThat(updatedOrder).returns(pair.getValue(), Order::getSubstatus);
            } catch (Exception e) {
                fail(e.getMessage());
            }
        });
    }

    public static Stream<Arguments> testExpressCheckpointParams() {
        return Stream.of(
                // Экспресс курьером
                // проверяем переход в DELIVERY
                Arguments.of(31, PROCESSING, COURIER_SEARCH, false),
                Arguments.of(32, PROCESSING, COURIER_FOUND, false),
                Arguments.of(33, PROCESSING, COURIER_IN_TRANSIT_TO_SENDER, false),
                Arguments.of(34, PROCESSING, COURIER_ARRIVED_TO_SENDER, false),
                Arguments.of(35, PROCESSING, COURIER_RECEIVED, false),
                Arguments.of(36, PROCESSING, COURIER_NOT_FOUND, false),
                Arguments.of(49, DELIVERY, USER_RECEIVED, false),
                // проверяем переход в CANCELLED
                Arguments.of(60, DELIVERY, COURIER_NOT_DELIVER_ORDER, false),
                Arguments.of(70, DELIVERY, COURIER_RETURNS_ORDER, false),
                Arguments.of(80, DELIVERY, COURIER_RETURNED_ORDER, false),
                Arguments.of(70, CANCELLED, COURIER_RETURNS_ORDER, false),
                Arguments.of(80, CANCELLED, COURIER_RETURNED_ORDER, false),
                // Экспресс в ПВЗ
                // проверяем переход в PICKUP
                Arguments.of(45, DELIVERY, PICKUP_SERVICE_RECEIVED, true)
        );
    }

    public static Stream<Arguments> testExpressCheckpointsCompletelyParams() {
        return Stream.of(
                // полный DELIVERED сценарий для экспресса курьером
                Arguments.of(List.of(
                        Pair.of(10, STARTED),
                        Pair.of(31, COURIER_SEARCH),
                        Pair.of(32, COURIER_FOUND),
                        Pair.of(33, COURIER_IN_TRANSIT_TO_SENDER),
                        Pair.of(34, COURIER_ARRIVED_TO_SENDER),
                        Pair.of(35, COURIER_RECEIVED),
                        Pair.of(49, USER_RECEIVED),
                        Pair.of(50, DELIVERY_SERVICE_DELIVERED)
                )),
                // полный CANCELLED сценарий для экспресса курьером
                Arguments.of(List.of(
                        Pair.of(10, STARTED),
                        Pair.of(31, COURIER_SEARCH),
                        Pair.of(36, COURIER_NOT_FOUND),
                        Pair.of(32, COURIER_FOUND),
                        Pair.of(33, COURIER_IN_TRANSIT_TO_SENDER),
                        Pair.of(34, COURIER_ARRIVED_TO_SENDER),
                        Pair.of(35, COURIER_RECEIVED),
                        Pair.of(60, COURIER_NOT_DELIVER_ORDER),
                        Pair.of(70, COURIER_RETURNS_ORDER),
                        Pair.of(80, COURIER_RETURNED_ORDER)
                )),
                Arguments.of(List.of(
                        Pair.of(10, STARTED),
                        Pair.of(32, COURIER_FOUND),
                        Pair.of(34, COURIER_ARRIVED_TO_SENDER),
                        Pair.of(35, COURIER_RECEIVED),
                        Pair.of(80, COURIER_RETURNED_ORDER)
                )),
                Arguments.of(List.of(
                        Pair.of(10, STARTED),
                        Pair.of(32, COURIER_FOUND),
                        Pair.of(31, COURIER_SEARCH),
                        Pair.of(32, COURIER_FOUND)
                ))
        );
    }

    public static Stream<Arguments> testExpressPickupCheckpointsCompletelyParams() {
        return Stream.of(
                // полный DELIVERED сценарий для экспресса в ПВЗ
                Arguments.of(
                        COURIER_SEARCH,
                        List.of(
                                Pair.of(45, PICKUP_SERVICE_RECEIVED),
                                Pair.of(50, DELIVERY_SERVICE_DELIVERED)
                        )
                ),
                Arguments.of(
                        COURIER_FOUND,
                        List.of(
                                Pair.of(45, PICKUP_SERVICE_RECEIVED),
                                Pair.of(50, DELIVERY_SERVICE_DELIVERED)
                        )
                ),
                Arguments.of(
                        COURIER_IN_TRANSIT_TO_SENDER,
                        List.of(
                                Pair.of(45, PICKUP_SERVICE_RECEIVED),
                                Pair.of(50, DELIVERY_SERVICE_DELIVERED)
                        )
                ),
                Arguments.of(
                        COURIER_ARRIVED_TO_SENDER,
                        List.of(
                                Pair.of(45, PICKUP_SERVICE_RECEIVED),
                                Pair.of(50, DELIVERY_SERVICE_DELIVERED)
                        )
                ),
                Arguments.of(
                        COURIER_RECEIVED,
                        List.of(
                                Pair.of(45, PICKUP_SERVICE_RECEIVED),
                                Pair.of(50, DELIVERY_SERVICE_DELIVERED)
                        )
                )
        );
    }

    private Order initOrder(OrderStatus status, boolean expressPickup) throws Exception {
        Order order = orderCreateHelper.createOrder(expressPickup ?
                blueNonFulfilmentOrderWithExpressPickupDelivery() : blueNonFulfilmentOrderWithExpressDelivery());
        order = orderStatusHelper.proceedOrderToStatus(order, status);

        Track track = new Track("EXPRESS", DeliveryProvider.MOCK_DELIVERY_SERVICE_ID);
        track.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        track = orderDeliveryHelper.addTrack(order, track, ClientInfo.SYSTEM);
        orderUpdateService.updateTrackSetTrackerId(
                order.getId(),
                track.getBusinessId(),
                DeliveryTrackMetaProvider.getDeliveryTrackMeta(String.valueOf(order.getId())).getId()
        );
        return order;
    }


    @Test
    void testExpressCheckpoint() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.DISABLE_DELIVERY_COURIER_SEARCH_FOR_WIDE_EXPRESS, true);
        int checkpoint = 31;
        OrderSubstatus expectedSubstatus = STARTED;
        boolean expressPickup = false;
        Order order = initOrder(PROCESSING, expressPickup);

        Delivery orderDelivery = order.getDelivery().clone();
        orderDelivery.setFeatures(Set.of(DeliveryFeature.EXPRESS_DELIVERY, DeliveryFeature.EXPRESS_DELIVERY_WIDE));
        orderDeliveryHelper.updateOrderDelivery(order.getId(), orderDelivery);

        DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(
                String.valueOf(order.getId()),
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(checkpoint)
        );

        notifyTracksHelper.notifyTracks(deliveryTrack);
        order = orderService.getOrder(order.getId());
        assertThat(order)
                .returns(expectedSubstatus.getStatus(), Order::getStatus)
                .returns(expectedSubstatus, Order::getSubstatus);
    }
}
