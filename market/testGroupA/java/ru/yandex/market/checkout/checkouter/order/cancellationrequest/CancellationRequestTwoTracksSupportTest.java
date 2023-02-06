package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.CancellationRequestHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.ClientHelper;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;
import ru.yandex.market.checkout.util.tracker.MockTrackerHelper;
import ru.yandex.market.common.report.model.FeedOfferId;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType.FULFILLMENT;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryUtils.requireParcel;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SHOP_FAILED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID;


public class CancellationRequestTwoTracksSupportTest extends AbstractWebTestBase {

    private static final String NOTES = "notes";

    @Autowired
    private CancellationRequestHelper cancellationRequestHelper;
    @Autowired
    private WireMockServer trackerMock;
    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private OrderHistoryEventsTestHelper orderHistoryEventsTestHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private OrderGetHelper orderGetHelper;

    private boolean isCreateByOrderEditApi;

    public static Stream<Arguments> parameterizedTestData() {

        return new ArrayList<>() {{
            add(Boolean.FALSE);
            add(Boolean.TRUE);
        }}.stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testPushTracksForMultiTrackParcel(boolean isCreateByOrderEditApi) throws Exception {
        this.isCreateByOrderEditApi = isCreateByOrderEditApi;
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getSelfDelivery());
        order.setFake(true);
        order.setGlobal(true);
        order.setFulfilment(true);
        order.setRgb(Color.BLUE);
        order.addItem(OrderItemProvider.buildOrderItem("qwerty", 5));
        order = orderServiceHelper.saveOrder(order);

        Track trackCarrier = new Track("iddqd-1_CARRIER", 123L);
        trackCarrier.setDeliveryServiceType(DeliveryServiceType.CARRIER);

        Parcel parcel1 = new Parcel();
        parcel1.addTrack(trackCarrier);

        Track pushedTrack = new Track("iddqd-2", 123L);
        pushedTrack.setTrackerId(100500L);
        parcel1.addTrack(pushedTrack);


        Delivery delivery = new Delivery();
        delivery.setParcels(Arrays.asList(parcel1));

        order = orderUpdateService.updateOrderDelivery(order.getId(), delivery, ClientInfo.SYSTEM);

        orderDeliveryHelper.addTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                new Track("iddqd-1_FF", MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID) {{
                    setDeliveryServiceType(FULFILLMENT);
                }},
                ClientInfo.SYSTEM);

        trackerMock.stubFor(
                put(urlPathEqualTo("/track"))
                        .withQueryParam("trackCode", WireMock.equalTo("iddqd-1"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody("{ \"id\": 100501 }")
                                .withHeader("Content-Type", "application/json"))
        );

        trackerMock.stubFor(
                put(urlPathEqualTo("/track"))
                        .withQueryParam("trackCode", WireMock.equalTo("iddqd-3"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody("{ \"id\": 100502 }")
                                .withHeader("Content-Type", "application/json"))
        );

        MockTrackerHelper.mockGetDeliveryServices(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID, trackerMock);
        // Действие
        tmsTaskHelper.runRegisterDeliveryTrackTaskV2();

        // Проверки
        Order updatedOrder = orderService.getOrder(order.getId());
        delivery = updatedOrder.getDelivery();

        assertThat(delivery.getParcels(), hasSize(1));

        parcel1 = delivery.getParcels().get(0);
        assertThat(parcel1.getTracks(), hasSize(3));

        Track track100500 = parcel1.getTracks().stream()
                .filter(t -> nonNull(t.getTrackerId()))
                .filter(t -> 100500 == t.getTrackerId())
                .findAny()
                .orElseGet(() -> {
                    Assertions.fail();
                    return null;
                });

        assertEquals(100500, (long) track100500.getTrackerId());
        assertEquals("iddqd-2", track100500.getTrackCode());

        assertEquals(3, parcel1.getTracks().size());

        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, NOTES);

        if (isCreateByOrderEditApi) {
            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(), cancellationRequest, ClientHelper.userClientFor(order));
        } else {
            cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, ClientHelper.userClientFor(order));
        }

        orderHistoryEventsTestHelper.assertHasEventWithType(order, HistoryEventType.ORDER_CANCELLATION_REQUESTED);
        orderHistoryEventsTestHelper.assertHasEventWithType(order, HistoryEventType.PARCEL_CANCELLATION_REQUESTED);

        Order modifiedOrder = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);

        parcel1 = requireParcel(parcel1.getId(), modifiedOrder);

        assertNotNull(modifiedOrder.getCancellationRequest());
        assertNotNull(parcel1.getCancellationRequest());

        assertEquals(1, modifiedOrder.getDelivery().getParcels().size());

        assertNotNull(parcel1);
        assertNotNull(parcel1.getTracks());

        assertEquals(3, parcel1.getTracks().size());

        long cnt =
                parcel1.getTracks().stream().filter(t -> Objects.equals(t.getDeliveryId(),
                        modifiedOrder.getInternalDeliveryId())).count();
        assertEquals(3, cnt);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testPushTracksForMultiShipmentOrder(boolean isCreateByOrderEditApi) throws Exception {
        this.isCreateByOrderEditApi = isCreateByOrderEditApi;
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getSelfDelivery());
        order.setFake(true);
        order.setGlobal(true);
        order.setFulfilment(true);
        order.setRgb(Color.BLUE);
        order.addItem(OrderItemProvider.buildOrderItem("qwerty", 5));
        order = orderServiceHelper.saveOrder(order);

        Track trackCarrier = new Track("iddqd-1_CARRIER", 123L);
        trackCarrier.setDeliveryServiceType(DeliveryServiceType.CARRIER);

        Parcel parcel1 = new Parcel();
        parcel1.addTrack(trackCarrier);

        Track pushedTrack = new Track("iddqd-2", 123L);
        pushedTrack.setTrackerId(100500L);
        parcel1.addTrack(pushedTrack);

        Parcel parcel2 = new Parcel();
        parcel2.addTrack(new Track("iddqd-3", 123L));

        long itemId = order.getItem(new FeedOfferId("qwerty", 1L)).getId();
        parcel2.addParcelItem(new ParcelItem(itemId, 3));

        Delivery delivery = new Delivery();
        delivery.setParcels(Arrays.asList(parcel1, parcel2));

        order = orderUpdateService.updateOrderDelivery(order.getId(), delivery, ClientInfo.SYSTEM);

        orderDeliveryHelper.addTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                new Track("iddqd-1_FF", MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID) {{
                    setDeliveryServiceType(FULFILLMENT);
                }},
                ClientInfo.SYSTEM);

        trackerMock.stubFor(
                put(urlPathEqualTo("/track"))
                        .withQueryParam("trackCode", WireMock.equalTo("iddqd-1"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody("{ \"id\": 100501 }")
                                .withHeader("Content-Type", "application/json"))
        );

        trackerMock.stubFor(
                put(urlPathEqualTo("/track"))
                        .withQueryParam("trackCode", WireMock.equalTo("iddqd-3"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody("{ \"id\": 100502 }")
                                .withHeader("Content-Type", "application/json"))
        );

        MockTrackerHelper.mockGetDeliveryServices(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID, trackerMock);
        // Действие
        tmsTaskHelper.runRegisterDeliveryTrackTaskV2();

        // Проверки
        Order updatedOrder = orderService.getOrder(order.getId());
        delivery = updatedOrder.getDelivery();

        Assertions.assertEquals(2, delivery.getParcels().size());

        parcel1 = delivery.getParcels().get(0);
        parcel2 = delivery.getParcels().get(1);
        assertThat(parcel1.getTracks(), hasSize(3));

        Track track100500 = parcel1.getTracks().stream()
                .filter(t -> nonNull(t.getTrackerId()))
                .filter(t -> 100500 == t.getTrackerId())
                .findAny()
                .orElseGet(() -> {
                    Assertions.fail();
                    return null;
                });

        assertEquals(100500, (long) track100500.getTrackerId());
        assertEquals("iddqd-2", track100500.getTrackCode());

        assertEquals(3, parcel1.getTracks().size());

        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

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


        orderHistoryEventsTestHelper.assertHasEventWithType(order, HistoryEventType.ORDER_CANCELLATION_REQUESTED);
        orderHistoryEventsTestHelper.assertHasEventWithType(order, HistoryEventType.PARCEL_CANCELLATION_REQUESTED);

        Order modifiedOrder = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);
        parcel1 = requireParcel(parcel1.getId(), modifiedOrder);
        parcel2 = requireParcel(parcel2.getId(), modifiedOrder);

        assertNotNull(modifiedOrder.getCancellationRequest());
        assertNotNull(parcel1.getCancellationRequest());
        assertNotNull(parcel2.getCancellationRequest());

        assertEquals(2, modifiedOrder.getDelivery().getParcels().size());

        assertNotNull(parcel1);
        assertNotNull(parcel1.getTracks());

        assertEquals(1, parcel2.getTracks().size());
        assertEquals(3, parcel1.getTracks().size());

        long cnt =
                parcel1.getTracks().stream()
                        .filter(t -> t.getDeliveryId() == modifiedOrder.getInternalDeliveryId()).count();
        assertEquals(3, cnt);
    }
}
