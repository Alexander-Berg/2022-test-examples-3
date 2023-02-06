package ru.yandex.market.checkout.checkouter.delivery;

import java.time.Clock;
import java.util.Collections;
import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.storage.track.checkpoint.TrackCheckpointWritingDao;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.helpers.utils.ParcelComparisonUtils.assertParcelListsSimilar;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.shopSelfDelivery;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

/**
 * @author mmetlov
 */
public class TrackControllerTest extends AbstractWebTestBase {

    public static final String TRACK_CODE = "TRACK_CODE";
    private static final String SECOND_TRACK_CODE = "SECOND_TRACK_CODE";
    public static final String LONG_TRACK_CODE = "A".repeat(101);

    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private Clock clock;
    @Autowired
    private TrackCheckpointWritingDao trackCheckpointWritingDao;

    @Test
    public void addTrack() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order createdOrder = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .build();
        orderStatusHelper.proceedOrderToStatus(createdOrder, OrderStatus.PROCESSING);

        Delivery deliveryRequest = new Delivery();
        Parcel parcel = new Parcel();
        deliveryRequest.setParcels(Collections.singletonList(parcel));
        long parcelId = orderDeliveryHelper.updateOrderDelivery(createdOrder.getId(), deliveryRequest)
                .getDelivery().getParcels().get(0).getId();

        Track trackReq = new Track(TRACK_CODE, MOCK_DELIVERY_SERVICE_ID);
        Track track = orderDeliveryHelper.addTrack(createdOrder.getId(), parcelId, trackReq, ClientInfo.SYSTEM);
        assertEquals(track.getDeliveryServiceId(), MOCK_DELIVERY_SERVICE_ID);
        assertEquals(track.getTrackCode(), TRACK_CODE);
        assertEquals(track.getDeliveryServiceType(), DeliveryServiceType.CARRIER);

        Order updatedOrder = orderService.getOrder(createdOrder.getId());
        Track trackFromDB = updatedOrder.getDelivery().getParcels().get(0).getTracks().get(0);
        assertEquals(trackFromDB.getDeliveryServiceId(), MOCK_DELIVERY_SERVICE_ID);
        assertEquals(trackFromDB.getTrackCode(), TRACK_CODE);
        assertEquals(trackFromDB.getDeliveryServiceType(), DeliveryServiceType.CARRIER);

        trackReq = new Track(SECOND_TRACK_CODE, MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID);
        Track secondTrack = orderDeliveryHelper.addTrack(updatedOrder.getId(), parcelId, trackReq, ClientInfo.SYSTEM);
        assertEquals(secondTrack.getDeliveryServiceId(), MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID);
        assertEquals(secondTrack.getTrackCode(), SECOND_TRACK_CODE);
        assertEquals(secondTrack.getDeliveryServiceType(), DeliveryServiceType.CARRIER);
        assertThat(secondTrack.getCreationDate(), notNullValue());

        updatedOrder = client.getOrder(updatedOrder.getId(), ClientRole.SYSTEM, 1L);
        assertThat(updatedOrder.getDelivery().getParcels().get(0).getTracks(), containsInAnyOrder(
                allOf(
                        hasProperty("trackCode", is(TRACK_CODE)),
                        hasProperty("deliveryServiceId", is(MOCK_DELIVERY_SERVICE_ID)),
                        hasProperty("deliveryServiceType", is(DeliveryServiceType.CARRIER))
                ),
                allOf(
                        hasProperty("trackCode", is(SECOND_TRACK_CODE)),
                        hasProperty("deliveryServiceId",
                                is(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID)),
                        hasProperty("deliveryServiceType", is(DeliveryServiceType.CARRIER))
                )
        ));

        assertParcelListsSimilar(createdOrder.getDelivery().getParcels(), updatedOrder.getDelivery().getParcels());
    }

    @Test
    public void addTooLongTrackCodeShouldResponseBadRequest() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order createdOrder = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .build();
        Long orderId = createdOrder.getId();
        orderStatusHelper.proceedOrderToStatus(createdOrder, OrderStatus.PROCESSING);

        Delivery deliveryRequest = new Delivery();
        Parcel parcel = new Parcel();
        deliveryRequest.setParcels(Collections.singletonList(parcel));
        long parcelId = orderDeliveryHelper.updateOrderDelivery(orderId, deliveryRequest)
                .getDelivery().getParcels().get(0).getId();

        Track trackReq = new Track(LONG_TRACK_CODE, MOCK_DELIVERY_SERVICE_ID);

        ResultActionsContainer resultActionsContainer = new ResultActionsContainer().andExpect(status().isBadRequest());
        orderDeliveryHelper.addTrack(orderId, parcelId, trackReq, ClientInfo.SYSTEM, resultActionsContainer);
    }

    @Test
    public void addTrackWith2Parcels() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        var deliveryOption = shopSelfDelivery()
                .dates(DeliveryDates.deliveryDates(clock, 0, 2));

        parameters.setPushApiDeliveryResponse(deliveryOption.buildResponse(DeliveryResponse::new));
        parameters.getReportParameters().setGlobal(true);
        parameters.getOrder().setDelivery(deliveryOption.build());
        Order createdOrder = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(createdOrder, OrderStatus.PROCESSING);

        Delivery deliveryRequest = new Delivery();
        deliveryRequest.setParcels(Lists.newArrayList(new Parcel(), new Parcel()));
        createdOrder = orderDeliveryHelper.updateOrderDelivery(createdOrder.getId(), deliveryRequest);
        long parcel1Id = createdOrder.getDelivery().getParcels().get(0).getId();
        long parcel2Id = createdOrder.getDelivery().getParcels().get(1).getId();

        Track trackReq = new Track(TRACK_CODE, MOCK_DELIVERY_SERVICE_ID);
        Track track = orderDeliveryHelper.addTrack(createdOrder.getId(), parcel1Id, trackReq, ClientInfo.SYSTEM);
        assertEquals(track.getDeliveryServiceId(), MOCK_DELIVERY_SERVICE_ID);
        assertEquals(track.getTrackCode(), TRACK_CODE);
        assertEquals(track.getDeliveryServiceType(), DeliveryServiceType.CARRIER);

        Order order = orderService.getOrder(createdOrder.getId());
        Track trackFromDB = order.getDelivery().getParcels().get(0).getTracks().get(0);
        assertEquals(trackFromDB.getDeliveryServiceId(), MOCK_DELIVERY_SERVICE_ID);
        assertEquals(trackFromDB.getTrackCode(), TRACK_CODE);

        trackReq = new Track(SECOND_TRACK_CODE, MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID);
        Track secondTrack = orderDeliveryHelper.addTrack(order.getId(), parcel2Id, trackReq, ClientInfo.SYSTEM);
        assertEquals(secondTrack.getDeliveryServiceId(), MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID);
        assertEquals(secondTrack.getTrackCode(), SECOND_TRACK_CODE);

        Order updatedOrder = orderService.getOrder(createdOrder.getId());
        assertThat(updatedOrder.getDelivery().getParcels(), containsInAnyOrder(
                hasProperty("tracks", contains(allOf(
                        hasProperty("trackCode", is(TRACK_CODE)),
                        hasProperty("deliveryServiceId", is(MOCK_DELIVERY_SERVICE_ID))
                ))),
                hasProperty("tracks", contains(allOf(
                        hasProperty("trackCode", is(SECOND_TRACK_CODE)),
                        hasProperty("deliveryServiceId",
                                is(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID))
                )))
        ));

        assertParcelListsSimilar(createdOrder.getDelivery().getParcels(), updatedOrder.getDelivery().getParcels());
    }

    @Test
    public void getTracksWithCheckpointsTest() throws Exception {
        Order createdOrder = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .build();
        orderStatusHelper.proceedOrderToStatus(createdOrder, OrderStatus.PROCESSING);

        Delivery deliveryRequest = new Delivery();
        Parcel parcel = new Parcel();
        deliveryRequest.setParcels(Collections.singletonList(parcel));
        long parcelId = orderDeliveryHelper.updateOrderDelivery(createdOrder.getId(), deliveryRequest)
                .getDelivery().getParcels().get(0).getId();

        Track trackReq = new Track(TRACK_CODE, MOCK_DELIVERY_SERVICE_ID);
        trackReq = orderDeliveryHelper.addTrack(createdOrder.getId(), parcelId, trackReq, ClientInfo.SYSTEM);

        TrackCheckpoint checkpoint = EntityHelper.createTrackCheckpoint();
        checkpoint.setTrackId(trackReq.getId());
        trackCheckpointWritingDao.insertCheckpoints(Collections.singletonList(checkpoint), 123L);


        List<Track> tracks = client.getTracksByOrderId(createdOrder.getId(), ClientRole.SYSTEM, 1L);
        assertEquals(1, tracks.size());
        Track track = tracks.get(0);
        assertEquals(TRACK_CODE, track.getTrackCode());
        assertEquals(parcelId, track.getShipmentId());
        assertEquals(MOCK_DELIVERY_SERVICE_ID, track.getDeliveryServiceId());

        assertEquals(1, track.getCheckpoints().size());
    }


    @Test
    public void getTracksWithCheckpointsBadTest() throws Exception {
        //if not found order
        List<Track> tracks = client.getTracksByOrderId(-6, ClientRole.SYSTEM, 1L);

        assertEquals(0, tracks.size());


        //if order found but track not found
        Order createdOrder = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .build();
        createdOrder = orderStatusHelper.proceedOrderToStatus(createdOrder, OrderStatus.PROCESSING);
        tracks = client.getTracksByOrderId(createdOrder.getId(), ClientRole.SYSTEM, 1L);

        assertEquals(0, tracks.size());

        //if order incorect id
        tracks = client.getTracksByOrderId(-10, ClientRole.SYSTEM, 1L);

        assertEquals(0, tracks.size());

        //if order found and track found but checkpoint not found

        Delivery deliveryRequest = new Delivery();
        Parcel parcel = new Parcel();
        deliveryRequest.setParcels(Collections.singletonList(parcel));
        long parcelId = orderDeliveryHelper.updateOrderDelivery(createdOrder.getId(), deliveryRequest)
                .getDelivery().getParcels().get(0).getId();

        Track trackReq = new Track(TRACK_CODE, MOCK_DELIVERY_SERVICE_ID);
        trackReq = orderDeliveryHelper.addTrack(createdOrder.getId(), parcelId, trackReq, ClientInfo.SYSTEM);


        tracks = client.getTracksByOrderId(createdOrder.getId(), ClientRole.SYSTEM, 1L);
        assertEquals(1, tracks.size());

    }

    public static class PositiveStatusTest extends AbstractWebTestBase {

        @Autowired
        private OrderDeliveryHelper orderDeliveryHelper;

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"PROCESSING", "DELIVERY", "PICKUP", "DELIVERED"})
        public void testAddTrackWithTypeFromSettings(OrderStatus status) throws Exception {
            Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
            parameters.setDeliveryType(DeliveryType.PICKUP);
            parameters.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
            Order order = orderCreateHelper.createOrder(parameters);
            orderStatusHelper.proceedOrderToStatus(order, status);

            Delivery deliveryRequest = new Delivery();
            Parcel parcel = new Parcel();
            deliveryRequest.setParcels(Collections.singletonList(parcel));
            long parcelId = orderDeliveryHelper.updateOrderDelivery(order.getId(), deliveryRequest)
                    .getDelivery().getParcels().get(0).getId();

            Track track = new Track(TRACK_CODE, MOCK_DELIVERY_SERVICE_ID);
            Track createdTrack = orderDeliveryHelper.addTrack(order.getId(), parcelId, track, ClientInfo.SYSTEM);
            assertEquals(track.getTrackCode(), createdTrack.getTrackCode());
            assertEquals(track.getDeliveryServiceId(), createdTrack.getDeliveryServiceId());
            assertEquals(DeliveryServiceType.CARRIER, createdTrack.getDeliveryServiceType());
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"PROCESSING", "DELIVERY", "PICKUP", "DELIVERED"})
        public void testAddTrackWithType(OrderStatus status) throws Exception {
            Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
            parameters.setDeliveryType(DeliveryType.PICKUP);
            parameters.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
            Order order = orderCreateHelper.createOrder(parameters);
            orderStatusHelper.proceedOrderToStatus(order, status);

            Delivery deliveryRequest = new Delivery();
            Parcel parcel = new Parcel();
            deliveryRequest.setParcels(Collections.singletonList(parcel));
            long parcelId = orderDeliveryHelper.updateOrderDelivery(order.getId(), deliveryRequest)
                    .getDelivery().getParcels().get(0).getId();

            Track track = new Track(TRACK_CODE, MOCK_DELIVERY_SERVICE_ID);
            track.setDeliveryServiceType(DeliveryServiceType.SORTING_CENTER);
            Track createdTrack = orderDeliveryHelper.addTrack(order.getId(), parcelId, track, ClientInfo.SYSTEM);
            assertEquals(track.getTrackCode(), createdTrack.getTrackCode());
            assertEquals(track.getDeliveryServiceId(), createdTrack.getDeliveryServiceId());
            assertEquals(DeliveryServiceType.SORTING_CENTER, createdTrack.getDeliveryServiceType());
        }
    }

    public static class PositiveStatusCanceledTest extends AbstractWebTestBase {

        @Autowired
        private OrderDeliveryHelper orderDeliveryHelper;

        @Test
        public void testAddTrackWithTypeFromSettings() throws Exception {
            Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
            parameters.setDeliveryType(DeliveryType.PICKUP);
            parameters.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
            Order order = orderCreateHelper.createOrder(parameters);
            orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

            Delivery deliveryRequest = new Delivery();
            Parcel parcel = new Parcel();
            deliveryRequest.setParcels(Collections.singletonList(parcel));
            long parcelId = orderDeliveryHelper.updateOrderDelivery(order.getId(), deliveryRequest)
                    .getDelivery().getParcels().get(0).getId();

            orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

            Track track = new Track(TRACK_CODE, MOCK_DELIVERY_SERVICE_ID);
            Track createdTrack = orderDeliveryHelper.addTrack(order.getId(), parcelId, track, ClientInfo.SYSTEM);
            assertEquals(track.getTrackCode(), createdTrack.getTrackCode());
            assertEquals(track.getDeliveryServiceId(), createdTrack.getDeliveryServiceId());
            assertEquals(DeliveryServiceType.CARRIER, createdTrack.getDeliveryServiceType());
        }

        @Test
        public void testAddTrackWithType() throws Exception {
            Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
            parameters.setDeliveryType(DeliveryType.PICKUP);
            parameters.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
            Order order = orderCreateHelper.createOrder(parameters);

            orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

            Delivery deliveryRequest = new Delivery();
            Parcel parcel = new Parcel();
            deliveryRequest.setParcels(Collections.singletonList(parcel));
            long parcelId = orderDeliveryHelper.updateOrderDelivery(order.getId(), deliveryRequest)
                    .getDelivery().getParcels().get(0).getId();

            orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

            Track track = new Track(TRACK_CODE, MOCK_DELIVERY_SERVICE_ID);
            track.setDeliveryServiceType(DeliveryServiceType.SORTING_CENTER);
            Track createdTrack = orderDeliveryHelper.addTrack(order.getId(), parcelId, track, ClientInfo.SYSTEM);
            assertEquals(track.getTrackCode(), createdTrack.getTrackCode());
            assertEquals(track.getDeliveryServiceId(), createdTrack.getDeliveryServiceId());
            assertEquals(DeliveryServiceType.SORTING_CENTER, createdTrack.getDeliveryServiceType());
        }
    }

    public static class NegativeStatusTest extends AbstractWebTestBase {

        @Autowired
        private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
        @Autowired
        private OrderDeliveryHelper orderDeliveryHelper;


        @Test
        public void testAddTrack() throws Exception {
            Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                    .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                    .withDeliveryType(DeliveryType.DELIVERY)
                    .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                    .withColor(BLUE)
                    .withPartnerInterface(true)
                    .build();

            order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.UNPAID);

            long parcelId = order.getDelivery().getParcels().get(0).getId();

            Track track = new Track(TRACK_CODE, MOCK_DELIVERY_SERVICE_ID);
            orderDeliveryHelper.addTrack(order.getId(), parcelId, track, ClientInfo.SYSTEM,
                    new ResultActionsContainer().andExpect(status().is(400)));
        }
    }
}
