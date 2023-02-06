package ru.yandex.market.checkout.checkouter.track;

import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackId;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;
import ru.yandex.market.checkout.util.tracker.MockTrackerHelper;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.DELETE_ORDER_TRACK;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.TRACK_START_TRACKING;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class TrackingTest extends AbstractWebTestBase {

    private static final long ANOTHER_DELIVERY_SERVICE_ID = 123L;

    @Autowired
    private WireMockServer trackerMock;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private OrderStatusHelper orderStatusHelper;
    @Autowired
    private OrderGetHelper orderGetHelper;

    @BeforeEach
    public void setUp() throws Exception {
        MockTrackerHelper.mockGetDeliveryServices(0L, trackerMock);
    }

    @Test
    public void shouldTakeSupportedDeliveryServicesFromCache() throws Exception {
        try {
            setFixedTime(getClock().instant());
            checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, true);

            MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);

            MockTrackerHelper.mockGetDeliveryServices(MOCK_DELIVERY_SERVICE_ID, trackerMock);

            Order order = yandexMarketDeliveryHelper.createMarDoOrder(MOCK_DELIVERY_SERVICE_ID);
            order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

            Long trackId = orderDeliveryHelper.addTrack(order, new Track("QWE", MOCK_DELIVERY_SERVICE_ID),
                    ClientInfo.SYSTEM).getId();

            assertTrue(queuedCallService.existsQueuedCall(TRACK_START_TRACKING, trackId));

            queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, trackId);

            trackId = orderDeliveryHelper.addTrack(order, new Track("RTY", MOCK_DELIVERY_SERVICE_ID),
                    ClientInfo.SYSTEM).getId();

            assertTrue(queuedCallService.existsQueuedCall(TRACK_START_TRACKING, trackId));

            queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, trackId);

            // only 1 call should be done instead of 2
            trackerMock.verify(1, getRequestedFor(urlPathEqualTo("/services")));

            trackerMock.verify(putRequestedFor(urlPathEqualTo("/track"))
                    .withQueryParam("trackCode", equalTo("QWE"))
                    .withQueryParam("deliveryServiceId", equalTo(String.valueOf(MOCK_DELIVERY_SERVICE_ID)))
                    .withQueryParam("orderId", equalTo(String.valueOf(order.getId())))
                    .withQueryParam("deliveryType", equalTo(String.valueOf(order.getDelivery().getType().getId())))
                    .withQueryParam("estimatedArrivalDateFrom",
                            equalTo(new SimpleDateFormat("yyyy-MM-dd")
                                    .format(order.getDelivery().getDeliveryDates().getFromDate())))
                    .withQueryParam("estimatedArrivalDateTo",
                            equalTo(new SimpleDateFormat("yyyy-MM-dd")
                                    .format(order.getDelivery().getDeliveryDates().getToDate())))
            );

            trackerMock.verify(putRequestedFor(urlPathEqualTo("/track"))
                    .withQueryParam("trackCode", equalTo("RTY"))
                    .withQueryParam("deliveryServiceId", equalTo(String.valueOf(MOCK_DELIVERY_SERVICE_ID)))
                    .withQueryParam("orderId", equalTo(String.valueOf(order.getId())))
                    .withQueryParam("deliveryType", equalTo(String.valueOf(order.getDelivery().getType().getId())))
                    .withQueryParam("estimatedArrivalDateFrom",
                            equalTo(new SimpleDateFormat("yyyy-MM-dd")
                                    .format(order.getDelivery().getDeliveryDates().getFromDate())))
                    .withQueryParam("estimatedArrivalDateTo",
                            equalTo(new SimpleDateFormat("yyyy-MM-dd")
                                    .format(order.getDelivery().getDeliveryDates().getToDate())))
            );

            order = orderService.getOrder(order.getId());
            assertEquals(MockTrackerHelper.TRACKER_ID, order.getDelivery()
                    .getParcels().get(0)
                    .getTracks().get(0)
                    .getTrackerId().longValue());
            assertThat(order.getDelivery()
                    .getParcels().get(0)
                    .getTracks(), containsInAnyOrder(
                    hasProperty("trackerId", is(MockTrackerHelper.TRACKER_ID)),
                    hasProperty("trackerId", is(MockTrackerHelper.TRACKER_ID))));

            //move clock forward to expire cached value
            setFixedTime(getClock().instant().plus(11, ChronoUnit.MILLIS));
            trackerMock.resetRequests();

            trackId = orderDeliveryHelper.addTrack(order, new Track("ASDF", MOCK_DELIVERY_SERVICE_ID),
                    ClientInfo.SYSTEM).getId();

            assertTrue(queuedCallService.existsQueuedCall(TRACK_START_TRACKING, trackId));

            queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, trackId);

            // new call should be done
            trackerMock.verify(1, getRequestedFor(urlPathEqualTo("/services")));

        } finally {
            checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, false);
        }
    }

    /**
     * Треки заказа с несколькими посылками пушатся в трекер корректно
     * <p>
     * Подготовка
     * 1. Создать заказ
     * 2. Добавить в заказ посылку A
     * 3. Добавить в посылку A треки с трек-кодами "iddqd-1" и "iddqd-2"
     * 4. Задать трекерный id 100500 для трека с трек-кодом "iddqd-2"
     * 5. Добавить к заказу отправление B
     * 6. Добавить к отправлению B трек с трек-кодом "iddqd-3"
     * <p>
     * Действия
     * 1. Запустить джобу пуша треков в трекер
     * <p>
     * Проверки
     * 1. Количество посылок в заказе не изменилось
     * 2. У треков с трек-кодами "iddqd-1" и "iddqd-3" появился трекерный id
     * 3. У трека с трек-кодом "iddqd-2" трекерный id не изменился
     */
    @Test
    public void testPushTracksForMultiShipmentOrder() {
        // Подготовка
        Order order = OrderProvider.getBluePostPaidOrder();
        order.setDelivery(DeliveryProvider.getSelfDelivery());
        order.setFake(true);
        order.addItem(OrderItemProvider.buildOrderItem("qwerty", 5));
        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        Parcel shipment1 = new Parcel();
        shipment1.addTrack(new Track("iddqd-1", ANOTHER_DELIVERY_SERVICE_ID));

        Track pushedTrack = new Track("iddqd-2", ANOTHER_DELIVERY_SERVICE_ID);
        pushedTrack.setTrackerId(100500L);
        shipment1.addTrack(pushedTrack);

        Parcel shipment2 = new Parcel();
        shipment2.addTrack(new Track("iddqd-3", ANOTHER_DELIVERY_SERVICE_ID));

        // Добавляем товар для стабилизации порядка посылок
        long itemId = order.getItem(new FeedOfferId("qwerty", 1L)).getId();
        shipment2.addParcelItem(new ParcelItem(itemId, 3));

        Delivery delivery = new Delivery();
        delivery.setParcels(Arrays.asList(shipment1, shipment2));

        order = orderUpdateService.updateOrderDelivery(order.getId(), delivery, ClientInfo.SYSTEM);

        trackerMock.stubFor(
                put(urlPathEqualTo("/track"))
                        .withQueryParam("trackCode", equalTo("iddqd-1"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody("{ \"id\": 100501 }")
                                .withHeader("Content-Type", "application/json"))
        );

        trackerMock.stubFor(
                put(urlPathEqualTo("/track"))
                        .withQueryParam("trackCode", equalTo("iddqd-3"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody("{ \"id\": 100502 }")
                                .withHeader("Content-Type", "application/json"))
        );

        // Действие
        runTask();

        // Проверки
        Order updatedOrder = orderService.getOrder(order.getId());
        delivery = updatedOrder.getDelivery();

        assertThat(delivery.getParcels(), hasSize(2));

        shipment1 = delivery.getParcels().get(0);
        assertThat(shipment1.getTracks(), hasSize(2));

        Track track100500 = shipment1.getTracks().stream()
                .filter(t -> 100500 == t.getTrackerId())
                .findAny()
                .orElseGet(() -> {
                    Assertions.fail();
                    return null;
                });

        Track track100501 = shipment1.getTracks().stream()
                .filter(t -> 100501L == t.getTrackerId())
                .findAny()
                .orElseGet(() -> {
                    Assertions.fail();
                    return null;
                });

        assertEquals(100501, (long) track100501.getTrackerId());
        assertEquals("iddqd-1", track100501.getTrackCode());

        assertEquals(100500, (long) track100500.getTrackerId());
        assertEquals("iddqd-2", track100500.getTrackCode());

        shipment2 = delivery.getParcels().get(1);
        assertThat(shipment2.getTracks(), hasSize(1));
        assertEquals(100502, (long) shipment2.getTracks().get(0).getTrackerId());
        assertEquals("iddqd-3", shipment2.getTracks().get(0).getTrackCode());
    }

    @Test
    public void testPushCancelledFulfillmentOrder() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .build();

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

        orderDeliveryHelper.addTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                new Track("iddqd", ANOTHER_DELIVERY_SERVICE_ID),
                ClientInfo.SYSTEM);

        // Действие
        runTask();

        // Проверки
        trackerMock.verify(1, putRequestedFor(urlPathEqualTo("/track"))
                .withQueryParam("trackCode", equalTo("iddqd"))
        );
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PROCESSING", "DELIVERY"})
    public void testDSBSOrder(OrderStatus orderStatus) throws Exception {
        // Подготовка
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, true);
        Order order = orderCreateHelper.createOrder(WhiteParametersProvider.defaultWhiteParameters());
        orderStatusHelper.proceedOrderToStatus(order, orderStatus);
        List<Track> tracks = orderDeliveryHelper.putTrack(
                order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                List.of(new Track("iddqd", ANOTHER_DELIVERY_SERVICE_ID)),
                ClientInfo.SYSTEM
        );

        // Мок трекера
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        // Действие
        queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, tracks.get(0).getId());

        // Проверки
        trackerMock.verify(1, putRequestedFor(urlPathEqualTo("/track"))
                .withQueryParam("trackCode", equalTo("iddqd"))
                .withQueryParam("deliveryServiceId", equalTo("123"))
                .withQueryParam("deliveryType", equalTo("0"))
                .withQueryParam("entityType", equalTo("7"))
        );
    }

    @Test
    public void testPutFullyDuplicatedTrack() throws Exception {
        // Подготовка
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, true);
        Order order = orderCreateHelper.createOrder(WhiteParametersProvider.defaultWhiteParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        List<Track> tracks = orderDeliveryHelper.putTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                List.of(new Track("iddqd", ANOTHER_DELIVERY_SERVICE_ID)),
                ClientInfo.SYSTEM);
        //fully duplicated track
        List<Track> tracks2 = orderDeliveryHelper.putTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                List.of(new Track("iddqd", ANOTHER_DELIVERY_SERVICE_ID)),
                ClientInfo.SYSTEM);

        // Мок трекера
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        // Действие
        queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, tracks.get(0).getId());
        queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, tracks2.get(0).getId());

        // Проверки
        assertEquals(1, tracks.size());
        assertEquals(1, tracks2.size());
        assertEquals(tracks.get(0).getId(), tracks2.get(0).getId());

        trackerMock.verify(1, putRequestedFor(urlPathEqualTo("/track"))
                .withQueryParam("trackCode", equalTo("iddqd"))
                .withQueryParam("deliveryServiceId", equalTo("123"))
                .withQueryParam("deliveryType", equalTo("0"))
                .withQueryParam("entityType", equalTo("7"))
        );
    }

    @Test
    public void testPutChangeDeliveryServiceIdTrack() throws Exception {
        // Подготовка
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, true);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELETE_TRACK, true);
        Order order = orderCreateHelper.createOrder(WhiteParametersProvider.defaultWhiteParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        Track track1 = new Track("iddqd", ANOTHER_DELIVERY_SERVICE_ID);
        List<Track> tracks = orderDeliveryHelper.putTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                List.of(track1),
                ClientInfo.SYSTEM);
        // track with different trackCode
        Track track2 = new Track("ddddd", ANOTHER_DELIVERY_SERVICE_ID);
        List<Track> tracks2 = orderDeliveryHelper.putTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                List.of(track2),
                ClientInfo.SYSTEM);

        // Мок трекера
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        MockTrackerHelper.mockDeleteTrack(trackerMock, tracks.get(0).getId());

        // Действие
        queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, tracks.get(0).getId());
        queuedCallService.executeQueuedCallSynchronously(DELETE_ORDER_TRACK, tracks.get(0).getId());
        queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, tracks2.get(0).getId());

        // Проверки
        assertEquals(1, tracks.size());
        assertEquals(1, tracks2.size());
        assertNotEquals(tracks.get(0).getId(), tracks2.get(0).getId());

        trackerMock.verify(1, putRequestedFor(urlPathEqualTo("/track"))
                .withQueryParam("trackCode", equalTo("ddddd"))
                .withQueryParam("deliveryServiceId", equalTo("123"))
                .withQueryParam("deliveryType", equalTo("0"))
                .withQueryParam("entityType", equalTo("7"))
        );
        String deleteTrackUrl = "/track/" + tracks.get(0).getId() + "/delete";
        trackerMock.verify(1, putRequestedFor(urlPathEqualTo(deleteTrackUrl)));

        Order resultOrder = orderGetHelper.getOrder(order.getId(), order.getUserClientInfo());
        List<TrackId> resultTrackIds = resultOrder.getDelivery().getParcels().get(0).getTracks().stream()
                .map(Track::getBusinessId)
                .collect(Collectors.toList());
        assertEquals(1, resultTrackIds.size());
        assertThat(resultTrackIds, containsInAnyOrder(track2.getBusinessId()));
    }


    @Test
    public void testPutMultipleTracks() throws Exception {
        // Подготовка
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, true);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELETE_TRACK, true);

        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        List<Track> tracks = orderDeliveryHelper.putTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                List.of(new Track("iddqd", ANOTHER_DELIVERY_SERVICE_ID),
                        new Track("ddddd", ANOTHER_DELIVERY_SERVICE_ID)
                ),
                ClientInfo.SYSTEM);

        // Мок трекера
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);

        // Действие
        queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, tracks.get(0).getId());
        queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, tracks.get(1).getId());

        // Проверки
        assertEquals(2, tracks.size());

        trackerMock.verify(1, putRequestedFor(urlPathEqualTo("/track"))
                .withQueryParam("trackCode", equalTo("iddqd"))
                .withQueryParam("deliveryServiceId", equalTo("123"))
                .withQueryParam("deliveryType", equalTo("0"))
                .withQueryParam("entityType", equalTo("0"))
        );
        trackerMock.verify(1, putRequestedFor(urlPathEqualTo("/track"))
                .withQueryParam("trackCode", equalTo("ddddd"))
                .withQueryParam("deliveryServiceId", equalTo("123"))
                .withQueryParam("deliveryType", equalTo("0"))
                .withQueryParam("entityType", equalTo("0"))
        );
    }

    @Test
    public void testPutMultipleTracksAndThenReplace() throws Exception {
        // Подготовка
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, true);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELETE_TRACK, true);

        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        Track track1 = new Track("iddqd", ANOTHER_DELIVERY_SERVICE_ID);
        Track track2 = new Track("ddddd", ANOTHER_DELIVERY_SERVICE_ID);
        Parcel parcel = order.getDelivery().getParcels().get(0);
        List<Track> tracks1 = orderDeliveryHelper.putTrack(order.getId(),
                parcel.getId(),
                List.of(track1, track2),
                ClientInfo.SYSTEM);

        //One same with previous another - new
        Track track3 = new Track("oooooo", ANOTHER_DELIVERY_SERVICE_ID);
        List<Track> tracks2 = orderDeliveryHelper.putTrack(order.getId(),
                parcel.getId(),
                List.of(track1, track3),
                ClientInfo.SYSTEM);

        if (tracks1.get(0).getTrackCode().equals(track1.getTrackCode())) {
            track1 = tracks1.get(0);
            track2 = tracks1.get(1);
        } else {
            track1 = tracks1.get(1);
            track2 = tracks1.get(0);
        }
        if (tracks2.get(0).getTrackCode().equals(track1.getTrackCode())) {
            track3 = tracks2.get(1);
        } else {
            track3 = tracks2.get(0);
        }

        // Мок трекера
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        MockTrackerHelper.mockDeleteTrack(trackerMock, track2.getId());

        // Действие
        queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, track1.getId());
        queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, track2.getId());
        queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, track1.getId());
        queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, track3.getId());
        queuedCallService.executeQueuedCallSynchronously(DELETE_ORDER_TRACK, track2.getId());

        // Проверки
        assertEquals(2, tracks1.size());
        assertEquals(2, tracks2.size());
        assertFalse(queuedCallService.existsQueuedCall(DELETE_ORDER_TRACK, track1.getId()));
        assertFalse(queuedCallService.existsQueuedCall(DELETE_ORDER_TRACK, track3.getId()));

        trackerMock.verify(1, putRequestedFor(urlPathEqualTo("/track"))
                .withQueryParam("trackCode", equalTo(track1.getTrackCode()))
                .withQueryParam("deliveryServiceId", equalTo(Long.toString(track1.getDeliveryServiceId())))
                .withQueryParam("deliveryType", equalTo("0"))
                .withQueryParam("entityType", equalTo("0"))
        );

        trackerMock.verify(1, putRequestedFor(urlPathEqualTo("/track"))
                .withQueryParam("trackCode", equalTo(track3.getTrackCode()))
                .withQueryParam("deliveryServiceId", equalTo(Long.toString(track3.getDeliveryServiceId())))
                .withQueryParam("deliveryType", equalTo("0"))
                .withQueryParam("entityType", equalTo("0"))
        );

        String deleteTrackUrl = "/track/" + track2.getId() + "/delete";
        trackerMock.verify(1, putRequestedFor(urlPathEqualTo(deleteTrackUrl)));

        Order resultOrder = orderGetHelper.getOrder(order.getId(), order.getUserClientInfo());
        List<TrackId> resultTrackIds =
                resultOrder.getDelivery().getParcels().get(0).getTracks().stream()
                        .map(Track::getBusinessId)
                        .collect(Collectors.toList());
        assertEquals(2, resultTrackIds.size());
        assertThat(resultTrackIds, containsInAnyOrder(track1.getBusinessId(), track3.getBusinessId()));
    }

    @Test
    public void allowDuplicateForOwnDelivery() throws Exception {
        // Подготовка
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, true);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELETE_TRACK, true);

        Order oldOrder = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(oldOrder, OrderStatus.DELIVERY);
        Track track1 = new Track("iddqd", 100L);
        Parcel oldParcel = oldOrder.getDelivery().getParcels().get(0);
        List<Track> oldTracks = orderDeliveryHelper.putTrack(oldOrder.getId(),
                oldParcel.getId(),
                List.of(track1),
                ClientInfo.SYSTEM);

        Order newOrder = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(newOrder, OrderStatus.DELIVERY);
        Parcel newParcel = newOrder.getDelivery().getParcels().get(0);
        List<Track> newTracks = orderDeliveryHelper.putTrack(newOrder.getId(),
                newParcel.getId(),
                List.of(track1),
                ClientInfo.SYSTEM);

        // Мок трекера
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);

        Order resultOrder1 = orderGetHelper.getOrder(oldOrder.getId(), newOrder.getUserClientInfo());
        List<Track> resultTracks1 = resultOrder1.getDelivery().getParcels().get(0).getTracks();
        assertThat(resultTracks1, hasSize(1));

        Order resultOrder2 = orderGetHelper.getOrder(newOrder.getId(), newOrder.getUserClientInfo());
        List<Track> resultTracks2 = resultOrder2.getDelivery().getParcels().get(0).getTracks();
        assertThat(resultTracks2, hasSize(1));

        List<TrackId> resultTrackIds1 = resultTracks1.stream()
                .map(Track::getBusinessId)
                .collect(Collectors.toList());
        List<TrackId> resultTrackIds2 = resultTracks2.stream()
                .map(Track::getBusinessId)
                .collect(Collectors.toList());

        assertEquals(resultTrackIds1, resultTrackIds2);
    }

    @Test
    public void createTrackWithUsedBusinessId() throws Exception {
        // Подготовка
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, true);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELETE_TRACK, true);

        Order oldOrder = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(oldOrder, OrderStatus.DELIVERY);
        Track track1 = new Track("iddqd", ANOTHER_DELIVERY_SERVICE_ID);
        Parcel oldParcel = oldOrder.getDelivery().getParcels().get(0);
        orderDeliveryHelper.putTrack(oldOrder.getId(),
                oldParcel.getId(),
                List.of(track1),
                ClientInfo.SYSTEM);

        Order newOrder = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(newOrder, OrderStatus.DELIVERY);
        Track track2 = new Track("ddddd", ANOTHER_DELIVERY_SERVICE_ID);
        Parcel newParcel = newOrder.getDelivery().getParcels().get(0);
        orderDeliveryHelper.putTrack(newOrder.getId(),
                newParcel.getId(),
                List.of(track2),
                ClientInfo.SYSTEM);

        Order resultOrder = orderGetHelper.getOrder(newOrder.getId(), newOrder.getUserClientInfo());
        List<Track> resultTracks = resultOrder.getDelivery().getParcels().get(0).getTracks();
        assertThat(resultTracks, hasSize(1));

        // try to put track that exist for different order
        orderDeliveryHelper.putTrackWithException(newOrder.getId(),
                newParcel.getId(),
                List.of(track1),
                ClientInfo.SYSTEM);

        // Мок трекера
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);

        Order resultOrder2 = orderGetHelper.getOrder(newOrder.getId(), newOrder.getUserClientInfo());
        List<Track> resultTracks2 = resultOrder2.getDelivery().getParcels().get(0).getTracks();
        assertThat(resultTracks2, hasSize(1));

        List<TrackId> resultTrackIds = resultTracks.stream()
                .map(Track::getBusinessId)
                .collect(Collectors.toList());
        assertThat(resultTrackIds, containsInAnyOrder(track2.getBusinessId()));
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"CANCELLED", "DELIVERED"})
    public void testDSBSOrderWrongStatus(OrderStatus orderStatus) throws Exception {
        // Подготовка
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, true);
        Order order = orderCreateHelper.createOrder(WhiteParametersProvider.defaultWhiteParameters());
        orderStatusHelper.proceedOrderToStatus(order, orderStatus);
        orderDeliveryHelper.putTrackWithException(
                order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                List.of(new Track("iddqd", ANOTHER_DELIVERY_SERVICE_ID)),
                ClientInfo.SYSTEM
        );

        // Мок трекера
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);

        // Проверки
        trackerMock.verify(0, putRequestedFor(urlPathEqualTo("/track"))
                .withQueryParam("trackCode", equalTo("iddqd"))
                .withQueryParam("deliveryServiceId", equalTo("123"))
                .withQueryParam("deliveryType", equalTo("0"))
                .withQueryParam("entityType", equalTo("7"))
        );
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PROCESSING", "DELIVERY"})
    public void testDSBSOrderWithOwnDelivery(OrderStatus orderStatus) throws Exception {
        // Подготовка
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, true);
        Order order = orderCreateHelper.createOrder(WhiteParametersProvider.defaultWhiteParameters());
        orderStatusHelper.proceedOrderToStatus(order, orderStatus);
        List<Track> tracks = orderDeliveryHelper.putTrack(
                order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                List.of(new Track("iddqd", 100L)),
                ClientInfo.SYSTEM
        );

        // Мок трекера
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);

        // Проверки
        trackerMock.verify(0, putRequestedFor(urlPathEqualTo("/track")));
    }

    private void runTask() {
        tmsTaskHelper.runRegisterDeliveryTrackTaskV2();
    }
}
