package ru.yandex.market.checkout.checkouter.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.collect.Streams;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryCheckpointStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.DeliveryTrack;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PICKUP;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;


public class TrackNotificationServiceImplIntegrationTest extends AbstractWebTestBase {

    private static final List<OrderStatus> STATUSES = Arrays.asList(DELIVERY, PICKUP, DELIVERED);
    private static final Comparator<OrderStatus> STATUS_COMPARATOR = Ordering.explicit(Arrays.asList(PROCESSING,
            DELIVERY, PICKUP, DELIVERED));

    private static final int[] COMMON_DELIVERY_STATUSES = Stream.of(
            DeliveryCheckpointStatus.DELIVERY_AT_START,
            DeliveryCheckpointStatus.DELIVERY_AT_START_2,
            DeliveryCheckpointStatus.DELIVERY_TRANSPORTATION,
            DeliveryCheckpointStatus.DELIVERY_ARRIVED
    ).mapToInt(DeliveryCheckpointStatus::getId).toArray();

    private static final Map<DeliveryType, int[]> SPECIFIC_DELIVERY_STATUSES = new LinkedHashMap<>();

    static {
        SPECIFIC_DELIVERY_STATUSES.put(DeliveryType.DELIVERY, new int[]{
                DeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT.getId()
        });
        SPECIFIC_DELIVERY_STATUSES.put(DeliveryType.PICKUP, new int[]{
                DeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT.getId()
        });
        SPECIFIC_DELIVERY_STATUSES.put(DeliveryType.POST, new int[0]);
    }

    private static final Map<DeliveryType, int[]> SPECIFIC_PICKUP_STATUSES = new LinkedHashMap<>();

    static {
        SPECIFIC_PICKUP_STATUSES.put(DeliveryType.DELIVERY, new int[0]);
        SPECIFIC_PICKUP_STATUSES.put(DeliveryType.PICKUP, new int[]{
                DeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT.getId()
        });
        SPECIFIC_PICKUP_STATUSES.put(DeliveryType.POST, new int[]{
                DeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT.getId()
        });
    }

    private static final int[] DELIVERED_STATUSES = {
            DeliveryCheckpointStatus.DELIVERY_DELIVERED.getId()
    };

    private static final int[] CANCEL_FROM_PROCESSINGS_STATUSES = Stream.of(
            DeliveryCheckpointStatus.SERVICE_CENTER_CANCELED
    ).mapToInt(DeliveryCheckpointStatus::getId).toArray();

    private static final int[] CANCEL_STATUSES = Stream.of(
            DeliveryCheckpointStatus.RETURN_ARRIVED,
            DeliveryCheckpointStatus.RETURN_TRANSMITTED_FULFILMENT,
            DeliveryCheckpointStatus.SORTING_CENTER_RETURN_RETURNED,
            DeliveryCheckpointStatus.SORTING_CENTER_RETURN_PARTIALLY_RETURNED
    ).mapToInt(DeliveryCheckpointStatus::getId).toArray();

    private static final long TRACKER_ID = 123L;
    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private TrackerNotificationService trackerNotificationService;
    @Autowired
    private ReturnHelper returnHelper;

    private Order order;
    private int checkpointStatus;
    private OrderStatus expectedStatus;
    private Collection<OrderStatus> initialStatusUpdates;

    private static int[] getDeliveryStatuses(DeliveryType deliveryType) {
        return Streams.concat(
                Arrays.stream(COMMON_DELIVERY_STATUSES),
                Arrays.stream(SPECIFIC_DELIVERY_STATUSES.get(deliveryType))
        ).toArray();
    }

    public static Stream<Arguments> parameterizedTestData() {
        List<Object[]> args = new ArrayList<>();

        addYandexMarketDelivery(args);
        addFulfillmentDeliveryScenarios(args);
        addFulfillmentPickupScenarios(args);
        addFulfillmentPostScenarios(args);
        return args.stream().map(Arguments::of);
    }

    private static void addYandexMarketDelivery(List<Object[]> args) {
        addCommonTransitionAllowedScenarios(
                OrderProvider.getOrderWithYandexMarketDelivery(),
                "yandexMarketDeliveryCommonScenarios",
                args);

        addPickupSpecificTransitionAllowedScenarios(
                OrderProvider.getOrderWithYandexMarketDelivery(),
                "yandexMarketDeliveryPickupSpecificScenarios",
                args
        );
    }

    private static void addFulfillmentDeliveryScenarios(List<Object[]> args) {
        Order fulfillmentOrderWithDeliveryType = OrderProvider.getFulfillmentOrderWithYandexDelivery();
        FulfilmentProvider.fulfilmentize(fulfillmentOrderWithDeliveryType);
        fulfillmentOrderWithDeliveryType.getBuyer().setDontCall(true);
        addCommonTransitionAllowedScenarios(
                fulfillmentOrderWithDeliveryType,
                "fulfillmentDeliveryCommonScenarios",
                args);
    }

    private static void addFulfillmentPickupScenarios(List<Object[]> args) {
        Order fulfillmentOrderWithPickupType = OrderProvider.getFulfillmentOrderWithPickupType();
        FulfilmentProvider.fulfilmentize(fulfillmentOrderWithPickupType);
        fulfillmentOrderWithPickupType.getBuyer().setDontCall(true);
        addCommonTransitionAllowedScenarios(
                fulfillmentOrderWithPickupType,
                "fulfillmentPickupCommonScenarios",
                args);

        addPickupSpecificTransitionAllowedScenarios(
                fulfillmentOrderWithPickupType,
                "fulfillmentPickupSpecificScenarios",
                args
        );
    }

    private static void addFulfillmentPostScenarios(List<Object[]> args) {
        Order fulfillmentOrderWithPostType = OrderProvider.getFulfillmentOrderWithPostType();
        FulfilmentProvider.fulfilmentize(fulfillmentOrderWithPostType);
        fulfillmentOrderWithPostType.getBuyer().setDontCall(true);
        addCommonTransitionAllowedScenarios(
                fulfillmentOrderWithPostType,
                "fulfillmentPostCommonScenarios",
                args);

        addPickupSpecificTransitionAllowedScenarios(
                fulfillmentOrderWithPostType,
                "fulfillmentPostSpecificScenarios",
                args
        );
    }

    /**
     * Общие сценарии, в которых разрешены переходы между статусами (Валидны для всех типов доставки).
     */
    private static void addCommonTransitionAllowedScenarios(Order order, String orderType, List<Object[]> args) {
        // Кейзы с переводом в следующий статус
        // Должны проталкивать статус до DELIVERY елси пришел соответствующий чекпоинт
        int[] deliveryStatuses = getDeliveryStatuses(order.getDelivery().getType());

        Arrays.stream(deliveryStatuses)
                .mapToObj(status -> toCase(order, orderType, status, OrderStatus.DELIVERY, OrderStatus.PROCESSING))
                .forEach(args::add);

        // Должны переводить заказ в DELIVERED , если пришла соответствующая точка, даже если статус был PROCESSING
        Arrays.stream(DELIVERED_STATUSES)
                .mapToObj(status -> toCase(order, orderType, status, OrderStatus.DELIVERED, OrderStatus.PROCESSING))
                .forEach(args::add);

        // Кейзы с игнором пришедших точек, если уже в нужном статусе
        // Не должны падать, если статус уже DELIVERY, но пришли точки, соответствующие DELIVERY
        Arrays.stream(deliveryStatuses)
                .mapToObj(status -> toCase(order, orderType, status, OrderStatus.DELIVERY, OrderStatus.DELIVERY))
                .forEach(args::add);

        // Должны переводить заказ в DELIVERED , если пришла соответствующая точка, даже если статус был DELIVERY
        Arrays.stream(DELIVERED_STATUSES)
                .mapToObj(status -> toCase(order, orderType, status, OrderStatus.DELIVERED, OrderStatus.DELIVERY))
                .forEach(args::add);

        // Должные переводить заказ в CANCELLED из DELIVERY, если пришла соответствующая точка
        Arrays.stream(CANCEL_STATUSES)
                .mapToObj(status -> toCase(order, orderType, status, OrderStatus.CANCELLED, OrderStatus.DELIVERY))
                .forEach(args::add);

        Arrays.stream(CANCEL_FROM_PROCESSINGS_STATUSES)
                .mapToObj(status -> toCase(order, orderType, status, OrderStatus.CANCELLED, OrderStatus.PROCESSING))
                .forEach(args::add);

        Arrays.stream(CANCEL_FROM_PROCESSINGS_STATUSES)
                .mapToObj(status -> toCase(order, orderType, status, OrderStatus.DELIVERY, OrderStatus.DELIVERY))
                .forEach(args::add);

    }

    /**
     * Сценарии, в которых разерешены переходы между статусами (Специфичные для типа ПВЗ).
     */
    private static void addPickupSpecificTransitionAllowedScenarios(Order order, String orderType,
                                                                    List<Object[]> args) {
        int[] pickupStatuses = SPECIFIC_PICKUP_STATUSES.get(order.getDelivery().getType());

        // Должны переводить заказ в PICKUP, если пришла соответствующая точка.
        Arrays.stream(pickupStatuses)
                .mapToObj(status -> toCase(order, orderType, status, OrderStatus.PICKUP, OrderStatus.DELIVERY))
                .forEach(args::add);

        // Должны переводить заказ в DELIVERED , если пришла соответствующая точка.
        Arrays.stream(DELIVERED_STATUSES)
                .mapToObj(status -> toCase(order, orderType, status, OrderStatus.DELIVERED, OrderStatus.PICKUP))
                .forEach(args::add);

        // Не должны падать, если заказ уже в PICKUP и пришли точки, соответствующие PICKUP.
        Arrays.stream(pickupStatuses)
                .mapToObj(status -> new Object[]{order, status, OrderStatus.PICKUP, OrderStatus.PICKUP, orderType})
                .forEach(args::add);

        // Должные переводить заказ в CANCELLED из DELIVERY, если пришла соответствующая точка
        Arrays.stream(CANCEL_STATUSES)
                .mapToObj(status -> toCase(order, orderType, status, OrderStatus.CANCELLED, OrderStatus.PICKUP))
                .forEach(args::add);

        // Кейзы с переходом через статус

        // Должны переводить заказ в PICKUP , если пришла соответствующая точка, даже если статус был PROCESSING
        Arrays.stream(pickupStatuses)
                .mapToObj(status -> toCase(order, orderType, status, OrderStatus.PICKUP, OrderStatus.PROCESSING))
                .forEach(args::add);
    }

    /**
     * Общие сценарии, в которых запрещен переход между статусами.
     */
    private static void addTransitionProhibitedScenarios(Order order, String orderType, List<Object[]> args) {
        int[] pickupStatuses = SPECIFIC_PICKUP_STATUSES.getOrDefault(order.getDelivery().getType(), new int[0]);

        Arrays.stream(COMMON_DELIVERY_STATUSES)
                .mapToObj(status -> toCase(order, orderType, status, OrderStatus.PROCESSING, OrderStatus.PROCESSING))
                .forEach(args::add);

        IntStream.concat(Arrays.stream(pickupStatuses), Arrays.stream(DELIVERED_STATUSES))
                .mapToObj(status -> toCase(order, orderType, status, OrderStatus.PROCESSING, OrderStatus.PROCESSING))
                .forEach(args::add);

        IntStream.concat(Arrays.stream(pickupStatuses), Arrays.stream(DELIVERED_STATUSES))
                .mapToObj(status -> toCase(order, orderType, status, OrderStatus.DELIVERY, OrderStatus.DELIVERY))
                .forEach(args::add);

        Arrays.stream(CANCEL_STATUSES)
                .mapToObj(status -> toCase(order, orderType, status, DELIVERED, DELIVERED))
                .forEach(args::add);
    }

    @BeforeEach
    public void setUp() {
        returnHelper.mockSupplierInfo();
        returnHelper.mockShopInfo();
    }

    @ParameterizedTest(name = "checkpointStatus = {1}, expectedStatus = {2}, initialStatus = {3}, orderType = {4}")
    @MethodSource("parameterizedTestData")
    public void shouldUpdateOrderStatusToDeliveryIfCheckpointReceived(Order order1,
                                                                      int checkpointStatus1,
                                                                      OrderStatus expectedStatus1,
                                                                      OrderStatus initialStatus1,
                                                                      Object orderType) {
        this.order = order1;
        this.checkpointStatus = checkpointStatus1;
        this.expectedStatus = expectedStatus1;
        this.initialStatusUpdates = STATUSES.stream()
                .filter(it -> STATUS_COMPARATOR.compare(it, initialStatus1) <= 0)
                .filter(it -> EnumSet.of(DeliveryType.PICKUP, DeliveryType.POST)
                        .contains(order.getDelivery().getType()) || it != OrderStatus.PICKUP)
                .collect(Collectors.toList());

        Order saved = orderServiceHelper.saveOrder(order);
        if (!saved.getStatus().equals(PROCESSING)) {
            saved = orderUpdateService.updateOrderStatus(saved.getId(), OrderStatus.PROCESSING, ClientInfo.SYSTEM);
        }
        assertThat(saved.getStatus(), is(PROCESSING));

        Order orderWithTrack = orderServiceHelper.putTrackIntoOrder(order.getId(),
                order.getDelivery().getDeliveryServiceId(), orderUpdateService);
        Parcel shipment = Iterables.getOnlyElement(orderWithTrack.getDelivery().getParcels());
        Track track = Iterables.getOnlyElement(shipment.getTracks());
        orderUpdateService.updateTrackSetTrackerId(order.getId(), track.getBusinessId(), TRACKER_ID);

        setStatusByShopIfRequired(orderWithTrack, initialStatusUpdates);

        DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(TRACKER_ID, this.checkpointStatus);
        trackerNotificationService.notifyTracks(Collections.singletonList(deliveryTrack));

        Order orderAfterDeliveryPush = orderService.getOrder(order.getId());
        Assertions.assertEquals(this.expectedStatus, orderAfterDeliveryPush.getStatus());
    }

    @Test
    @SuppressWarnings("checkstyle:HiddenField")
    public void shouldNotCancelExpressOrderWhen60CheckpointReceivedAndPropertyIsOn() {
        checkouterProperties.setRejectCancellationOnCheckpoint(Set.of(DeliveryCheckpointStatus.RETURN_PREPARING));
        var params = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        var order = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(order, DELIVERY);

        Order orderWithTrack = orderServiceHelper.putTrackIntoOrder(order.getId(),
                order.getDelivery().getDeliveryServiceId(), orderUpdateService);
        Parcel shipment = Iterables.getOnlyElement(orderWithTrack.getDelivery().getParcels());
        Track track = Iterables.getOnlyElement(shipment.getTracks());
        orderUpdateService.updateTrackSetTrackerId(order.getId(), track.getBusinessId(), TRACKER_ID);

        DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(TRACKER_ID,
                DeliveryCheckpointStatus.RETURN_PREPARING.getId());
        trackerNotificationService.notifyTracks(Collections.singletonList(deliveryTrack));

        Order orderAfter = orderService.getOrder(order.getId());
        Assertions.assertEquals(DELIVERY, orderAfter.getStatus());
    }

    @Test
    @SuppressWarnings("checkstyle:HiddenField")
    public void shouldCancelExpressOrderWhen60CheckpointReceivedAndPropertyIsOff() {
        checkouterProperties.setRejectCancellationOnCheckpoint(Collections.emptySet());
        var params = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        var order = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(order, DELIVERY);

        Order orderWithTrack = orderServiceHelper.putTrackIntoOrder(order.getId(),
                order.getDelivery().getDeliveryServiceId(), orderUpdateService);
        Parcel shipment = Iterables.getOnlyElement(orderWithTrack.getDelivery().getParcels());
        Track track = Iterables.getOnlyElement(shipment.getTracks());
        orderUpdateService.updateTrackSetTrackerId(order.getId(), track.getBusinessId(), TRACKER_ID);

        DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(TRACKER_ID,
                DeliveryCheckpointStatus.RETURN_PREPARING.getId());
        trackerNotificationService.notifyTracks(Collections.singletonList(deliveryTrack));

        Order orderAfter = orderService.getOrder(order.getId());
        Assertions.assertEquals(CANCELLED, orderAfter.getStatus());
    }

    @Test
    @SuppressWarnings("checkstyle:HiddenField")
    public void shouldNotCancelNonExpressOrderWhen60CheckpointReceivedAndPropertyIsOn() {
        checkouterProperties.setRejectCancellationOnCheckpoint(Set.of(DeliveryCheckpointStatus.RETURN_PREPARING));
        var params = BlueParametersProvider.defaultBlueOrderParameters();
        var order = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(order, DELIVERY);

        Order orderWithTrack = orderServiceHelper.putTrackIntoOrder(order.getId(),
                order.getDelivery().getDeliveryServiceId(), orderUpdateService);
        Parcel shipment = Iterables.getOnlyElement(orderWithTrack.getDelivery().getParcels());
        Track track = Iterables.getOnlyElement(shipment.getTracks());
        orderUpdateService.updateTrackSetTrackerId(order.getId(), track.getBusinessId(), TRACKER_ID);

        DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(TRACKER_ID,
                DeliveryCheckpointStatus.RETURN_PREPARING.getId());
        trackerNotificationService.notifyTracks(Collections.singletonList(deliveryTrack));

        Order orderAfter = orderService.getOrder(order.getId());
        Assertions.assertEquals(DELIVERY, orderAfter.getStatus());
    }

    private void setStatusByShopIfRequired(Order order, Collection<OrderStatus> inititalStatusUpdates) {
        for (OrderStatus statusUpdate : inititalStatusUpdates) {
            ClientInfo clientInfo;
            if (statusUpdate == PROCESSING) {
                clientInfo = new ClientInfo(ClientRole.SHOP, order.getShopId());
            } else {
                clientInfo = ClientInfo.SYSTEM;
            }
            orderUpdateService.updateOrderStatus(order.getId(), statusUpdate, clientInfo);
        }
    }

    private static Object[] toCase(Order order,
                                   String orderType,
                                   int deliveryCheckpointStatus,
                                   OrderStatus expectedStatus,
                                   OrderStatus startingStatus) {
        return new Object[]{order, deliveryCheckpointStatus, expectedStatus, startingStatus, orderType};
    }
}
