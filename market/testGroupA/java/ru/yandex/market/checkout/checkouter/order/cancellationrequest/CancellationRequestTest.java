package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.client.OrderFilter;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.history.OrderHistoryEventsRequest;
import ru.yandex.market.checkout.checkouter.util.Utils;
import ru.yandex.market.checkout.common.db.HasIntId;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.helpers.CancellationRequestHelper;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.ParcelBoxHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.ClientHelper;
import ru.yandex.market.checkout.util.tracker.MockTrackerHelper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.Color.WHITE;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_FAILED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.READY_TO_SHIP;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SHOP_FAILED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;

/**
 * @author mmetlov
 */

public class CancellationRequestTest extends AbstractWebTestBase {

    private static final String NOTES = "notes";

    private static final ClientInfo CALL_CENTER_OPERATOR_CLIENT_INFO = new ClientInfo(
            ClientRole.CALL_CENTER_OPERATOR,
            123L
    );

    @Autowired
    private CancellationRequestHelper cancellationRequestHelper;
    @Autowired
    private ParcelBoxHelper parcelBoxHelper;
    @Autowired
    private OrderGetHelper orderGetHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private WireMockServer trackerMock;
    @Autowired
    private NotifyTracksHelper notifyTracksHelper;
    @Autowired
    private OrderHistoryEventsTestHelper orderHistoryEventsTestHelper;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void createCancellationRequest(boolean isCreateByOrderEditApi) throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, NOTES);
        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, BuyerProvider.UID);
        Order response;
        if (isCreateByOrderEditApi) {
            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(), cancellationRequest, clientInfo);
            response = orderGetHelper.getOrder(order.getId(), clientInfo);
        } else {
            response = cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, clientInfo);
        }

        assertEquals(USER_CHANGED_MIND, response.getCancellationRequest().getSubstatus());
        assertEquals(NOTES, response.getCancellationRequest().getNotes());

        Order orderFromDB = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);
        assertEquals(USER_CHANGED_MIND, orderFromDB.getCancellationRequest().getSubstatus());
        assertEquals(NOTES, orderFromDB.getCancellationRequest().getNotes());

        order = orderStatusHelper.updateOrderStatus(order.getId(), CANCELLED);
        assertEquals(CANCELLED, order.getStatus());
        assertEquals(USER_CHANGED_MIND, order.getSubstatus());

        orderFromDB = orderGetHelper.getOrder(order.getId(), clientInfo);
        assertNull(orderFromDB.getCancellationRequest());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldntAllowRequestCancellationTwoTimes(boolean isCreateByOrderEditApi) throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, NOTES);
        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, BuyerProvider.UID);

        if (isCreateByOrderEditApi) {
            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(), cancellationRequest, clientInfo);

            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(),
                    cancellationRequest,
                    clientInfo,
                    new ResultActionsContainer().andExpect(status().is(400))
                            .andExpect(content().json("{\"status\":400," +
                                    "\"code\":\"CANCELLATION_ALREADY_REQUESTED\"," +
                                    "\"message\":\"Cancellation has already been requested for order " + order.getId() +
                                    "\"}")
                            )
            );
        } else {
            cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, clientInfo);

            cancellationRequestHelper.createCancellationRequest(
                    order.getId(),
                    cancellationRequest,
                    clientInfo,
                    new ResultActionsContainer().andExpect(status().is(400))
                            .andExpect(content().json("{\"status\":400," +
                                    "\"code\":\"CANCELLATION_ALREADY_REQUESTED\"," +
                                    "\"message\":\"Cancellation has already been requested for order " + order.getId() +
                                    "\"}")
                            )
            );
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldntAllowNotesLongerThan500Chars(boolean isCreateByOrderEditApi) throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        String largeNotes = IntStream.range(0, 501).mapToObj(i -> "a").collect(Collectors.joining());
        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, largeNotes);
        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, BuyerProvider.UID);

        if (isCreateByOrderEditApi) {
            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(),
                    cancellationRequest,
                    clientInfo,
                    new ResultActionsContainer().andExpect(status().is(400))
                            .andExpect(content().json("{\"status\":400," +
                                    "\"code\":\"INVALID_REQUEST\"," +
                                    "\"message\":\"Length of cancellation request notes (501) must be less than or " +
                                    "equal " +
                                    "to " +
                                    "500\"}")
                            )
            );
        } else {
            cancellationRequestHelper.createCancellationRequest(
                    order.getId(),
                    cancellationRequest,
                    clientInfo,
                    new ResultActionsContainer().andExpect(status().is(400))
                            .andExpect(content().json("{\"status\":400," +
                                    "\"code\":\"INVALID_REQUEST\"," +
                                    "\"message\":\"Length of cancellation request notes (501) must be less than or " +
                                    "equal " +
                                    "to " +
                                    "500\"}")
                            )
            );
        }

    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldAllowNotesWith500Chars(boolean isCreateByOrderEditApi) throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        String largeNotes = IntStream.range(0, 500).mapToObj(i -> "a").collect(Collectors.joining());
        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, largeNotes);
        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, BuyerProvider.UID);
        if (isCreateByOrderEditApi) {
            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(), cancellationRequest, clientInfo);
        } else {
            cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, clientInfo);
        }

    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldHideCancellationRequestInDeliveredForUser(boolean isCreateByOrderEditApi) throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, NOTES);
        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, BuyerProvider.UID);

        Order response;
        if (isCreateByOrderEditApi) {
            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(), cancellationRequest, clientInfo);
            response = orderGetHelper.getOrder(order.getId(), clientInfo);
        } else {
            response = cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, clientInfo);
        }

        orderStatusHelper.proceedOrderToStatus(response, DELIVERED);
        Order orderFromDB = orderGetHelper.getOrder(order.getId(), new ClientInfo(ClientRole.USER, BuyerProvider.UID));
        assertNull(orderFromDB.getCancellationRequest());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldSyncCancelGoldOrders(boolean isCreateByOrderEditApi) throws Exception {
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        assertEquals(WHITE, order.getRgb());
        assertEquals(DeliveryPartnerType.SHOP, order.getDelivery().getDeliveryPartnerType());

        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, NOTES);
        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, BuyerProvider.UID);

        Order response;
        if (isCreateByOrderEditApi) {
            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(), cancellationRequest, clientInfo);
            response = orderGetHelper.getOrder(order.getId(), clientInfo);
        } else {
            response = cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, clientInfo);
        }

        assertNull(response.getCancellationRequest());
        assertEquals(CANCELLED, response.getStatus());
        assertEquals(USER_CHANGED_MIND, response.getSubstatus());

        Order orderFromDB = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);
        assertEquals(USER_CHANGED_MIND, orderFromDB.getCancellationRequest().getSubstatus());
        assertEquals(NOTES, orderFromDB.getCancellationRequest().getNotes());
        assertEquals(CANCELLED, orderFromDB.getStatus());
        assertEquals(USER_CHANGED_MIND, orderFromDB.getSubstatus());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldSyncCancelDropshipOrdersBeforeReadyToShip(boolean isCreateByOrderEditApi) throws Exception {
        Parameters parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        Order order = orderCreateHelper.createOrder(parameters);

        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, NOTES);
        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, BuyerProvider.UID);

        Order response;
        if (isCreateByOrderEditApi) {
            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(), cancellationRequest, clientInfo);
            response = orderGetHelper.getOrder(order.getId(), clientInfo);
        } else {
            response = cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, clientInfo);
        }

        assertEquals(CANCELLED, response.getStatus());
        assertEquals(USER_CHANGED_MIND, response.getSubstatus());

        orderHistoryEventsTestHelper.assertHasEventWithType(order, HistoryEventType.ORDER_CANCELLATION_REQUESTED);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldNotSyncCancelDropshipOrdersAfterReadyToShip(boolean isCreateByOrderEditApi) throws Exception {
        Parameters parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        Order order = orderCreateHelper.createOrder(parameters);

        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        parcelBoxHelper.arrangeBoxes(order);
        orderUpdateService.updateOrderStatus(order.getId(), PROCESSING, READY_TO_SHIP);

        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, NOTES);
        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, BuyerProvider.UID);

        Order response;
        if (isCreateByOrderEditApi) {
            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(), cancellationRequest, clientInfo);
            response = orderGetHelper.getOrder(order.getId(), clientInfo);
        } else {
            response = cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, clientInfo);
        }
        assertNotNull(response.getCancellationRequest());
        assertThat(response.getStatus(), not(equalTo(CANCELLED)));

        orderHistoryEventsTestHelper.assertHasEventWithType(order, HistoryEventType.ORDER_CANCELLATION_REQUESTED);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldSyncCancelOrdersFromShopInProcessing(boolean isCreateByOrderEditApi) throws Exception {
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        assertEquals(WHITE, order.getRgb());
        assertEquals(DeliveryPartnerType.SHOP, order.getDelivery().getDeliveryPartnerType());

        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        orderUpdateService.updateOrderStatus(order.getId(), PROCESSING, READY_TO_SHIP);

        CancellationRequest cancellationRequest = new CancellationRequest(SHOP_FAILED, NOTES);

        Order response;
        if (isCreateByOrderEditApi) {
            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(), cancellationRequest, ClientHelper.shopClientFor(order));
            response = orderGetHelper.getOrder(order.getId(), ClientHelper.shopClientFor(order));
        } else {
            response = cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, ClientHelper.shopClientFor(order));
        }
        assertNotNull(response.getCancellationRequest());
        assertThat(response.getStatus(), equalTo(CANCELLED));
        assertThat(response.getSubstatus(), equalTo(SHOP_FAILED));

        orderHistoryEventsTestHelper.assertHasEventWithType(order, HistoryEventType.ORDER_CANCELLATION_REQUESTED);

        Order updatedOrder = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);
        assertNotNull(updatedOrder.getCancellationRequest());
        assertThat(updatedOrder.getStatus(), equalTo(CANCELLED));
        assertThat(updatedOrder.getSubstatus(), equalTo(SHOP_FAILED));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldCancelWhenOrderLostCheckpointWasSent(boolean isCreateByOrderEditApi) throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, DELIVERY);

        orderDeliveryHelper.addTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                new Track("iddqd", MOCK_DELIVERY_SERVICE_ID),
                ClientInfo.SYSTEM);

        MockTrackerHelper.mockGetDeliveryServices(MOCK_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        tmsTaskHelper.runRegisterDeliveryTrackTaskV2();

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);
        trustMockConfigurer.mockCreateRefund(null);
        trustMockConfigurer.mockDoRefund();

        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 403));

        CancellationRequest cancellationRequest = new CancellationRequest(DELIVERY_SERVICE_FAILED, NOTES);

        Order response;
        if (isCreateByOrderEditApi) {
            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(), cancellationRequest, CALL_CENTER_OPERATOR_CLIENT_INFO);
            response = orderGetHelper.getOrder(order.getId(), CALL_CENTER_OPERATOR_CLIENT_INFO);
        } else {
            response = cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, CALL_CENTER_OPERATOR_CLIENT_INFO);
        }

        assertEquals(DELIVERY_SERVICE_FAILED, response.getCancellationRequest().getSubstatus());
        assertEquals(NOTES, response.getCancellationRequest().getNotes());
        assertEquals(CANCELLED, response.getStatus());
        assertEquals(DELIVERY_SERVICE_FAILED, response.getSubstatus());

        Order orderFromDB = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);
        assertEquals(DELIVERY_SERVICE_FAILED, orderFromDB.getCancellationRequest().getSubstatus());
        assertEquals(NOTES, orderFromDB.getCancellationRequest().getNotes());
        assertEquals(CANCELLED, orderFromDB.getStatus());
        assertEquals(DELIVERY_SERVICE_FAILED, orderFromDB.getSubstatus());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldCancelWhenOrderLostCheckpointWasSentAfterCancelationRequest(boolean isCreateByOrderEditApi)
            throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, DELIVERY);

        CancellationRequest cancellationRequest = new CancellationRequest(DELIVERY_SERVICE_FAILED, NOTES);

        Order response;
        if (isCreateByOrderEditApi) {
            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(), cancellationRequest, CALL_CENTER_OPERATOR_CLIENT_INFO);
            response = orderGetHelper.getOrder(order.getId(), CALL_CENTER_OPERATOR_CLIENT_INFO);
        } else {
            response = cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, CALL_CENTER_OPERATOR_CLIENT_INFO);
        }
        assertEquals(DELIVERY_SERVICE_FAILED, response.getCancellationRequest().getSubstatus());
        assertEquals(NOTES, response.getCancellationRequest().getNotes());
        assertEquals(DELIVERY, response.getStatus());

        orderDeliveryHelper.addTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                new Track("iddqd", MOCK_DELIVERY_SERVICE_ID),
                ClientInfo.SYSTEM);

        MockTrackerHelper.mockGetDeliveryServices(MOCK_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        tmsTaskHelper.runRegisterDeliveryTrackTaskV2();

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);
        trustMockConfigurer.mockCreateRefund(null);
        trustMockConfigurer.mockDoRefund();

        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 403));

        Order orderFromDB = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);
        assertEquals(DELIVERY_SERVICE_FAILED, orderFromDB.getCancellationRequest().getSubstatus());
        assertEquals(NOTES, orderFromDB.getCancellationRequest().getNotes());
        assertEquals(CANCELLED, orderFromDB.getStatus());
        assertEquals(DELIVERY_SERVICE_FAILED, orderFromDB.getSubstatus());
    }

    @Test
    public void shouldntCancelWhenOrderLostCheckpointWasSentWithoutCancelationRequest() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, DELIVERY);

        orderDeliveryHelper.addTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                new Track("iddqd", MOCK_DELIVERY_SERVICE_ID),
                ClientInfo.SYSTEM);

        MockTrackerHelper.mockGetDeliveryServices(MOCK_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        tmsTaskHelper.runRegisterDeliveryTrackTaskV2();

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);
        trustMockConfigurer.mockCreateRefund(null);
        trustMockConfigurer.mockDoRefund();

        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 403));

        Order orderFromDB = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);
        assertEquals(DELIVERY, orderFromDB.getStatus());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldWriteCorrectFromDateInEvent(boolean isCreateByOrderEditApi) throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        String largeNotes = IntStream.range(0, 500).mapToObj(i -> "a").collect(Collectors.joining());

        Instant cancellationRequestDate = INTAKE_AVAILABLE_DATE.plus(1, ChronoUnit.HOURS);
        setFixedTime(cancellationRequestDate);

        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, largeNotes);
        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, BuyerProvider.UID);

        if (isCreateByOrderEditApi) {
            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(), cancellationRequest, clientInfo);
        } else {
            cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, clientInfo);
        }

        PagedEvents events = eventService.getPagedOrderHistoryEvents(
                order.getId(), Pager.atPage(1, 10), null,
                null, Set.of(HistoryEventType.ORDER_CANCELLATION_REQUESTED), false,
                ClientInfo.SYSTEM, null
        );

        assertThat(events.getItems(), hasSize(1));

        OrderHistoryEvent event = events.getItems().iterator().next();
        assertThat(event.getFromDate().getTime(), equalTo(cancellationRequestDate.toEpochMilli()));
    }

    @DisplayName("Получить сведения об инициаторе отмены, используя событие ORDER_CANCELLATION_REQUESTED и без " +
            "лишнего похода в БД")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldFillAndReturnCancellationRequestAuthorInHistoryEvent(boolean isCreateByOrderEditApi)
            throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        Instant cancellationRequestDate = INTAKE_AVAILABLE_DATE.plus(1, ChronoUnit.HOURS);
        setFixedTime(cancellationRequestDate);

        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, "note");
        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, BuyerProvider.UID);

        if (isCreateByOrderEditApi) {
            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(), cancellationRequest, clientInfo);
        } else {
            cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, clientInfo);
        }

        final OrderFilter orderFilter = OrderFilter.builder()
                .setRgb(HasIntId.getAllKnownValues(Color.values()))
                .build();

        final OrderHistoryEventsRequest request = OrderHistoryEventsRequest.builder(0)
                .setBatchSize(100)
                .setClientInfo(ClientInfo.SYSTEM)
                .setOrderFilter(orderFilter)
                .setPartials(Utils.cleanCopy(OptionalOrderPart.values()))
                .build();

        final List<OrderHistoryEvent> events = eventService.getOrderHistoryEventsFromQueue(request, false);
        assertThat(events, hasSize(9));

        final List<OrderHistoryEvent> withoutCancellationRequest = events.stream()
                .filter(e -> e.getOrderAfter().getCancellationRequest() == null)
                .collect(Collectors.toList());
        assertThat(withoutCancellationRequest, hasSize(6));
        withoutCancellationRequest.forEach(e -> assertNull(e.getCancellationRequestAuthor()));

        final List<OrderHistoryEvent> withCancellationRequest = events.stream()
                .filter(e -> e.getOrderAfter().getCancellationRequest() != null)
                .collect(Collectors.toList());
        assertThat(withCancellationRequest, hasSize(3));
        withCancellationRequest.forEach(e -> {
            assertNotNull(e.getCancellationRequestAuthor());
            assertEquals(ClientRole.USER, e.getCancellationRequestAuthor().getRole());
        });
    }

    @DisplayName("Получить сведения об инициаторе отмены без события ORDER_CANCELLATION_REQUESTED и с дополнительным " +
            "походом в БД")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldFillAndReturnCancellationRequestAuthorInHistoryEventFromDB(boolean isCreateByOrderEditApi)
            throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .build();
        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        Instant cancellationRequestDate = INTAKE_AVAILABLE_DATE.plus(1, ChronoUnit.HOURS);
        setFixedTime(cancellationRequestDate);

        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, "note");
        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, BuyerProvider.UID);

        if (isCreateByOrderEditApi) {
            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(), cancellationRequest, clientInfo);
        } else {
            cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, clientInfo);
        }

        final OrderFilter orderFilter = OrderFilter.builder()
                .setRgb(HasIntId.getAllKnownValues(Color.values()))
                .build();

        final OrderHistoryEventsRequest request = OrderHistoryEventsRequest.builder(0)
                .setBatchSize(100)
                .setClientInfo(ClientInfo.SYSTEM)
                .setEventTypes(Set.of(HistoryEventType.ORDER_CANCELLATION_REQUESTED))
                .setIgnoreEventTypes(true)
                .setOrderFilter(orderFilter)
                .setPartials(Utils.cleanCopy(OptionalOrderPart.values()))
                .build();

        final List<OrderHistoryEvent> events = eventService.getOrderHistoryEventsFromQueue(request, false);
        assertThat(events, hasSize(8));

        final List<OrderHistoryEvent> withoutCancellationRequest = events.stream()
                .filter(e -> e.getOrderAfter().getCancellationRequest() == null)
                .collect(Collectors.toList());
        assertThat(withoutCancellationRequest, hasSize(6));
        withoutCancellationRequest.forEach(e -> assertNull(e.getCancellationRequestAuthor()));

        final List<OrderHistoryEvent> withCancellationRequest = events.stream()
                .filter(e -> e.getOrderAfter().getCancellationRequest() != null)
                .collect(Collectors.toList());
        assertThat(withCancellationRequest, hasSize(2));
        withCancellationRequest.forEach(e -> {
            assertNotNull(e.getCancellationRequestAuthor());
            assertEquals(ClientRole.USER, e.getCancellationRequestAuthor().getRole());
        });
    }
}
