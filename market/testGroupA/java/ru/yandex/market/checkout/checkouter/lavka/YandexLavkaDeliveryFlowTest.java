package ru.yandex.market.checkout.checkouter.lavka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryCheckpointStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.DeliveryTrack;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.YaLavkaHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.b2b.B2bCustomersMockConfigurer;
import ru.yandex.market.checkout.util.tracker.MockTrackerHelper;
import ru.yandex.market.checkout.util.yalavka.YaLavkaDeliveryServiceConfigurer;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_DEFERRED_COURIER_NEW_DELIVERY_SUBSTATUSES;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.TRACK_START_TRACKING;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

public class YandexLavkaDeliveryFlowTest extends AbstractWebTestBase {

    private static final long LAVKA_DS_ID = 19463827;
    @Autowired
    protected CheckouterProperties checkouterProperties;
    @Autowired
    private NotifyTracksHelper notifyTracksHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private WireMockServer trackerMock;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private YaLavkaDeliveryServiceConfigurer yaLavkaDSConfigurer;
    @Autowired
    private EventsGetHelper eventsGetHelper;
    @Autowired
    private B2bCustomersMockConfigurer b2bCustomersMockConfigurer;
    @Autowired
    protected YaLavkaHelper yaLavkaHelper;

    public static Stream<Arguments> badResponsesFromLavka() {
        return Arrays.stream(new Object[][]{
                {
                        "Пустой массив nodes_info",
                        "{\n" +
                                "    \"request_id\": \"3045a971-9ff8-4b6f-8258-eb1fe02852bb\",\n" +
                                "    \"nodes_info\": []\n" +
                                "}",
                },
                {
                        "nodes_info не пришел в ответе",
                        "{\n" +
                                "    \"request_id\": \"3045a971-9ff8-4b6f-8258-eb1fe02852bb\"\n" +
                                "}",
                },
                {
                        "nodes_info = null",
                        "{\n" +
                                "    \"request_id\": \"3045a971-9ff8-4b6f-8258-eb1fe02852bb\",\n" +
                                "    \"nodes_info\": null\n" +
                                "}",
                },
                {
                        "Пустое тело ответа",
                        null,
                },
        }).map(Arguments::of);
    }

    @Nonnull
    private static Track extractDeliveryTrack(@Nonnull Order order) {
        return order.getDelivery().getParcels().get(0).getTracks().get(0);
    }

    protected static void checkStatus(@Nonnull Order order,
                                      @Nullable OrderStatus expectedStatus,
                                      @Nullable OrderSubstatus expectedSubstatus) {
        if (expectedStatus != null) {
            assertEquals(expectedStatus, order.getStatus());
        }
        if (expectedSubstatus != null) {
            assertEquals(expectedSubstatus, order.getSubstatus());
        }
    }

    @BeforeEach
    void setUp() {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndGps();
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELIVERY_TRACKING, true);
        yaLavkaDSConfigurer.configureOrderReservationRequest(HttpStatus.OK);
        b2bCustomersMockConfigurer.mockIsClientCanOrder(BuyerProvider.UID,
                B2bCustomersTestProvider.BUSINESS_BALANCE_ID, true);
        //по дефолту выключаем тогл, в тестах где он нужен, включаем его дополнительно
        checkouterFeatureWriter.writeValue(ENABLE_DEFERRED_COURIER_NEW_DELIVERY_SUBSTATUSES, false);
    }

    @AfterEach
    void tearDown() {
        yaLavkaDSConfigurer.reset();
        b2bCustomersMockConfigurer.resetAll();
    }

    @Test
    @DisplayName("Проверяем успешный сценарий доставки заказа 1")
    void testSuccessFlow1() throws Exception {
        Order order = createLavkaOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
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
    @DisplayName("Проверяем успешный сценарий доставки заказа 1 с подстатусом DELIVERY_TO_STORE_STARTED")
    void testSuccessFlow1_withDeliveryToStoreStarted() throws Exception {
        Order order = createLavkaOrderWithDsTrack();

        order = orderStatusHelper.updateOrderStatus(order.getId(), DELIVERY, OrderSubstatus.DELIVERY_TO_STORE_STARTED);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_TO_STORE_STARTED);

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
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
    @DisplayName("Проверяем успешный сценарий доставки заказа 2")
    void testSuccessFlow2() throws Exception {
        Order order = createLavkaOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        order = notifyTracks(order, 50, 6);
        checkStatus(order, DELIVERED, null);
    }

    @Test
    @DisplayName("Проверяем успешный сценарий доставки заказа 2 с подстатусом DELIVERY_TO_STORE_STARTED")
    void testSuccessFlow2_withDeliveryToStoreStarted() throws Exception {
        Order order = createLavkaOrderWithDsTrack();

        order = orderStatusHelper.updateOrderStatus(order.getId(), DELIVERY, OrderSubstatus.DELIVERY_TO_STORE_STARTED);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_TO_STORE_STARTED);

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        order = notifyTracks(order, 50, 6);
        checkStatus(order, DELIVERED, null);
    }

    @Test
    @DisplayName("Проверяем успешный сценарий доставки заказа 3")
    void testSuccessFlow3() throws Exception {
        Order order = createLavkaOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        // Пользоваетль вызывает курьера
        order = callCourier(order);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        order = notifyTracks(order, 50, 6);
        checkStatus(order, DELIVERED, null);
    }

    @Test
    @DisplayName("Проверяем успешный сценарий доставки заказа 3 с подстатусом DELIVERY_TO_STORE_STARTED")
    void testSuccessFlow3_withDeliveryToStoreStarted() throws Exception {
        Order order = createLavkaOrderWithDsTrack();

        order = orderStatusHelper.updateOrderStatus(order.getId(), DELIVERY, OrderSubstatus.DELIVERY_TO_STORE_STARTED);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_TO_STORE_STARTED);

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        // Пользоваетль вызывает курьера
        order = callCourier(order);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        order = notifyTracks(order, 50, 6);
        checkStatus(order, DELIVERED, null);
    }

    @Test
    @DisplayName("Проверяем сценарий доставки заказа, когда курьер был вызван через службу поддержки")
    void testSupportFlow() throws Exception {
        Order order = createLavkaOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        // Если вызов курьера штатным способом не удается, пользователь связывается с поддержкой
        // и вызов происходит в оффлайне

        // Пользователь получает заказ
        order = notifyTracks(order, 49, 3);
        checkStatus(order, DELIVERY, OrderSubstatus.USER_RECEIVED);

        order = notifyTracks(order, 50, 4);
        checkStatus(order, DELIVERED, null);
    }

    @Test
    @DisplayName("Проверяем сценарий доставки заказа, когда курьер был вызван " +
            "через службу поддержки с подстатусом DELIVERY_TO_STORE_STARTED")
    void testSupportFlow_withDeliveryToStoreStarted() throws Exception {
        Order order = createLavkaOrderWithDsTrack();

        order = orderStatusHelper.updateOrderStatus(order.getId(), DELIVERY, OrderSubstatus.DELIVERY_TO_STORE_STARTED);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_TO_STORE_STARTED);

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        // Если вызов курьера штатным способом не удается, пользователь связывается с поддержкой
        // и вызов происходит в оффлайне

        // Пользователь получает заказ
        order = notifyTracks(order, 49, 3);
        checkStatus(order, DELIVERY, OrderSubstatus.USER_RECEIVED);

        order = notifyTracks(order, 50, 4);
        checkStatus(order, DELIVERED, null);
    }

    @Test
    @DisplayName("Проверяем сценарий возврата заказа, когда заказ \"протух\" в сортцентре")
    void testReturnFlow1() throws Exception {
        Order order = createLavkaOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        // Пользователь не вызвал курьера и заказ "протух"

        // Служба доставки запускает сценарий возврата заказа
        order = notifyTracks(order, 70, 3);
        checkStatus(order, CANCELLED, OrderSubstatus.DELIVERY_SERVICE_UNDELIVERED);
    }

    @Test
    @DisplayName("Проверяем сценарий возврата заказа, когда курьер не смог передать заказ пользователю")
    void testReturnFlow2() throws Exception {
        Order order = createLavkaOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        // Пользоваетль вызывает курьера
        order = callCourier(order);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        // Курьер не смог передать пользователю заказ по неведомым причинам

        // Служба доставки запускает сценарий возврата заказа
        order = notifyTracks(order, 70, 3);
        checkStatus(order, CANCELLED, OrderSubstatus.DELIVERY_SERVICE_UNDELIVERED);
    }

    @Test
    @DisplayName("Проверяем, что заказ переводится в LAST_MILE_STARTED по 48 чекпоинту")
    void testLastMileStartedOn48Checkpoint() throws Exception {
        Order order = createLavkaOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        order = notifyTracks(order, 48, 4);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);
    }

    @Test
    @DisplayName("Проверяем, что заказ переводится в LAST_MILE_STARTED по 31 чекпоинту")
    void testLastMileStartedOn31Checkpoint() throws Exception {
        Order order = createLavkaOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        order = notifyTracks(order, 31, 4);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);
    }

    @Test
    @DisplayName("Возвращаемся в READY_FOR_LAST_MILE на 90 чекпоинт")
    void testDeliveryFailedForYandexLavka() throws Exception {
        Order order = createLavkaOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        order = notifyTracks(order, 48, 4);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        order = notifyTracks(order, DeliveryCheckpointStatus.DELIVERY_ATTEMPT_FAILED.getId(), 6);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);
    }

    @ParameterizedTest
    @ValueSource(ints = {31, 48})
    @DisplayName("Не возвращаемся в LAST_MILE_STARTED на 31/48 чекпоинт из USER_RECEIVED")
    void fromUserReceived_toLastMileStarted_shouldBeNotAllowed_onDemand(int checkpointStatus) throws Exception {
        Order order = createLavkaOrderAndProceedToUserReceived();

        order = notifyTracks(order, checkpointStatus, 6);
        checkStatus(order, DELIVERY, OrderSubstatus.USER_RECEIVED);
    }

    @ParameterizedTest
    @ValueSource(ints = {45, 90})
    @DisplayName("Возвращаемся в READY_FOR_LAST_MILE на 45/90 чекпоинт даже из USER_RECEIVED")
    void fromUserReceived_toReadyForLastMile_shouldBeAllowed_onDemand(int checkpointStatus) throws Exception {
        Order order = createLavkaOrderAndProceedToUserReceived();

        order = notifyTracks(order, checkpointStatus, 6);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);
    }

    @Test
    @DisplayName("Проверяем успешный сценарий доставки заказа 1 с опозданием чекпоинтов")
    void testSuccessFlowWithCheckpointsDelay() throws Exception {
        Order order = createLavkaOrderWithDsTrack();

        order = notifyTracks(order, List.of(45, 20, 48, 49), 2);
        checkStatus(order, DELIVERY, OrderSubstatus.USER_RECEIVED);

        PagedEvents orderHistoryEvents = eventsGetHelper.getOrderHistoryEvents(order.getId(), Integer.MAX_VALUE);
        var actualSubstatusesDesc = orderHistoryEvents.getItems().stream()
                .filter(e -> HistoryEventType.ORDER_SUBSTATUS_UPDATED == e.getType())
                .map(e -> e.getOrderAfter().getSubstatus())
                .toArray();
        OrderSubstatus[] expectedSubstatusesDesc = {
                OrderSubstatus.USER_RECEIVED,
                OrderSubstatus.LAST_MILE_STARTED,
                OrderSubstatus.READY_FOR_LAST_MILE
        };
        Assertions.assertArrayEquals(expectedSubstatusesDesc, actualSubstatusesDesc);

        order = notifyTracks(order, 50, 6);
        checkStatus(order, DELIVERED, null);
    }

    @Test
    @DisplayName("Сохранение нашего ID для ПВЗ Яндекс.GO")
    void testLavkaOperatorStationIdSaving() throws Exception {
        yaLavkaDSConfigurer.configureOrderReservationRequest(
                HttpStatus.OK,
                "{\n" +
                        "    \"request_id\": \"3045a971-9ff8-4b6f-8258-eb1fe02852bb\",\n" +
                        "    \"nodes_info\": [\n" +
                        "        {\n" +
                        "            \"operator_id\": \"market\",\n" +
                        "            \"operator_station_id\": \"10001635254\",\n" +
                        "            \"station_id\": \"83788ecf-7c21-4f60-9ea4-04602d5e6e1d\"\n" +
                        "        }\n" +
                        "    ]\n" +
                        "}"
        );
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withDeliveryServiceId(LAVKA_DS_ID)
                .withColor(Color.BLUE)
                .build();

        assertEquals("10001635254", order.getDelivery().getOnDemandOutletId());
        assertEquals(Set.of(DeliveryFeature.ON_DEMAND, DeliveryFeature.ON_DEMAND_MARKET_PICKUP),
                order.getDelivery().getFeatures());
    }

    @Test
    @DisplayName("Сохранение стороннего ID для ПВЗ Яндекс.GO")
    void testLavkaStationIdSaving() throws Exception {
        yaLavkaDSConfigurer.configureOrderReservationRequest(
                HttpStatus.OK,
                "{\n" +
                        "    \"request_id\": \"3045a971-9ff8-4b6f-8258-eb1fe02852bb\",\n" +
                        "    \"nodes_info\": [\n" +
                        "        {\n" +
                        "            \"operator_id\": \"lavka\",\n" +
                        "            \"operator_station_id\": \"33e45753daa84a298edc4f1789f9ba24000200010000\",\n" +
                        "            \"station_id\": \"01e5f11b-b54c-4304-9913-2aedf356d46f\"\n" +
                        "        }\n" +
                        "    ]\n" +
                        "}"
        );
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withDeliveryServiceId(LAVKA_DS_ID)
                .withColor(Color.BLUE)
                .build();

        assertEquals("01e5f11b-b54c-4304-9913-2aedf356d46f", order.getDelivery().getOnDemandOutletId());
        assertTrue(order.getDelivery().getFeatures().contains(DeliveryFeature.ON_DEMAND_YALAVKA));
    }

    @ParameterizedTest(name = TEST_DISPLAY_NAME)
    @MethodSource("badResponsesFromLavka")
    @DisplayName("Некорректные ответы от лавки")
    void testLavkaBadResponses(String caseName, String response) throws Exception {
        yaLavkaDSConfigurer.configureOrderReservationRequest(HttpStatus.OK, response);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withDeliveryServiceId(LAVKA_DS_ID)
                .withColor(Color.BLUE)
                .build();

        assertNull(order.getDelivery().getOutletCode());
        assertTrue(order.getDelivery().getFeatures().contains(DeliveryFeature.ON_DEMAND_YALAVKA));
    }

    @Nonnull
    protected Order callCourier(@Nonnull Order order) {
        return client.updateOrderStatus(
                order.getId(),
                ClientRole.USER,
                order.getBuyer().getUid(),
                null,
                DELIVERY,
                OrderSubstatus.LAST_MILE_STARTED
        );
    }

    @Nonnull
    protected Order notifyTracks(@Nonnull Order order, int rawStatus, long trackerCheckpointId) throws Exception {
        return notifyTracks(order, List.of(rawStatus), trackerCheckpointId);
    }

    @Nonnull
    protected Order notifyTracks(@Nonnull Order order, List<Integer> rawStatuses, long firstTrackerCheckpointId)
            throws Exception {
        if (rawStatuses.size() == 0) {
            return order;
        }

        long trackerId = extractDeliveryTrack(order).getTrackerId();
        DeliveryTrack track = DeliveryTrackProvider
                .getDeliveryTrack(trackerId, rawStatuses.get(0), firstTrackerCheckpointId);
        var checkpoints = new ArrayList<>(track.getDeliveryTrackCheckpoints());
        for (int i = 1; i < rawStatuses.size(); i++) {
            checkpoints.add(
                    DeliveryTrackProvider.getDeliveryTrackCheckpoint(rawStatuses.get(i), firstTrackerCheckpointId + i));
        }
        track.setDeliveryTrackCheckpoints(checkpoints);

        notifyTracksHelper.notifyTracks(track);
        order = orderService.getOrder(order.getId());
        var orderCheckpoints = extractDeliveryTrack(order).getCheckpoints();
        for (int i = 0; i < rawStatuses.size(); i++) {
            int rawStatus = rawStatuses.get(i);
            long id = firstTrackerCheckpointId + i;
            assertTrue(orderCheckpoints.stream()
                    .anyMatch(cp -> cp.getDeliveryCheckpointStatus() == rawStatus
                            && cp.getTrackerCheckpointId() == id));
        }
        return order;
    }

    protected Order createLavkaOrder(Long deliveryServiceId) throws Exception {
        return yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withDeliveryServiceId(deliveryServiceId)
                .withPartnerInterface(false)
                .withColor(Color.BLUE)
                .build();
    }

    @Nonnull
    protected Order createLavkaOrderWithDsTrack() throws Exception {
        return createLavkaOrderWithDsTrack(LAVKA_DS_ID, false);
    }

    @Nonnull
    protected Order createLavkaOrderWithDsTrack(Long deliveryServiceId, boolean withDeferredCourier) throws Exception {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withDeliveryServiceId(deliveryServiceId)
                .withPartnerInterface(false)
                .withColor(Color.BLUE)
                .build();
        if (withDeferredCourier) {
            Delivery orderDelivery = order.getDelivery().clone();
            orderDelivery.setFeatures(Set.of(DeliveryFeature.DEFERRED_COURIER));
            orderDeliveryHelper.updateOrderDelivery(order.getId(), orderDelivery);
        }
        order = orderStatusHelper.proceedOrderToStatus(order, DELIVERY);
        return addDsTrack(order);
    }

    protected Order addDsTrack(Order order) throws Exception {
        Long trackId = orderDeliveryHelper.addTrack(
                        order,
                        new Track("QWE", MOCK_DELIVERY_SERVICE_ID),
                        ClientInfo.SYSTEM
                )
                .getId();

        MockTrackerHelper.mockGetDeliveryServices(MOCK_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);

        queuedCallService.executeQueuedCallSynchronously(TRACK_START_TRACKING, trackId);

        assertThat(order.getDelivery().getType(), is(DeliveryType.DELIVERY));
        assertThat(order.getStatus(), is(DELIVERY));
        assertThat(order.getSubstatus(), is(OrderSubstatus.DELIVERY_SERVICE_RECEIVED));

        order = orderService.getOrder(order.getId());

        order = notifyTracks(order, 1, 1);
        checkStatus(order, DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        return order;
    }

    private Order createLavkaOrderAndProceedToUserReceived() throws Exception {
        Order order = createLavkaOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        // Пользователь вызывает курьера
        order = callCourier(order);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        // Заказ отправляется курьером пользователю
        order = notifyTracks(order, 20, 3);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        order = notifyTracks(order, 48, 4);
        checkStatus(order, DELIVERY, OrderSubstatus.LAST_MILE_STARTED);

        // Курьер нажал, что доставил пользователю
        order = notifyTracks(order, 49, 5);
        checkStatus(order, DELIVERY, OrderSubstatus.USER_RECEIVED);

        return order;
    }

    @Test
    @DisplayName("Проверяем сценарий возврата заказа, когда у заказа \"истек срок хранения\" в Лавке")
    void cancelWithPickupExpired() throws Exception {
        Order order = createLavkaOrderWithDsTrack();

        // Заказ приезжает на сортцентр службы доставки
        order = notifyTracks(order, 45, 2);
        checkStatus(order, DELIVERY, OrderSubstatus.READY_FOR_LAST_MILE);

        // Отмена по 43 ЧП от Лавки - "истек срок хранения".
        client.updateOrderStatus(order.getId(), RequestClientInfo.builder(ClientRole.SYSTEM).build(),
                CANCELLED, OrderSubstatus.PICKUP_EXPIRED);
        order = orderService.getOrder(order.getId());
        checkStatus(order, CANCELLED, OrderSubstatus.PICKUP_EXPIRED);
    }
}
