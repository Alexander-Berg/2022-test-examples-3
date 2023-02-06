package ru.yandex.market.checkout.checkouter.client;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.balance.BalanceListPaymentMethodsResult;
import ru.yandex.market.checkout.checkouter.balance.BalanceSimpleResult;
import ru.yandex.market.checkout.checkouter.balance.model.BalanceStatus;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBoxItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.NullifyCashbackEmitRequest;
import ru.yandex.market.checkout.checkouter.order.NullifyCashbackEmitResponse;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstancesPutRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItems;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryServiceCustomerInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.OrderOptionAvailability;
import ru.yandex.market.checkout.checkouter.pay.PagedRefunds;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundReason;
import ru.yandex.market.checkout.checkouter.pay.RefundStatus;
import ru.yandex.market.checkout.checkouter.pay.RefundTestHelper;
import ru.yandex.market.checkout.checkouter.pay.RefundableItems;
import ru.yandex.market.checkout.checkouter.receipt.Receipts;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.ParcelPatchRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.util.DuplicateParcelBoxStrategy;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.helpers.DropshipDeliveryHelper.DROPSHIP_SHOP_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;
import static ru.yandex.market.checkout.test.providers.TrackProvider.TRACK_CODE;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkListPaymentMethodsCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkUnbindCardCall;

public class CheckouterClientTest extends AbstractWebTestBase {

    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private DropshipDeliveryHelper dropshipDeliveryHelper;
    @Autowired
    private RefundHelper refundHelper;

    @Test
    public void canGetOrderById() {
        Order order = orderServiceHelper.prepareOrder();

        Order result = client.getOrder(order.getId(), ClientRole.SYSTEM, null);
        assertEquals(order.getId(), result.getId());
        assertEquals(order.getShopId(), result.getShopId());
        assertEquals(order.getStatus(), result.getStatus());
        assertEquals(order.getDelivery().getType(), result.getDelivery().getType());
        assertEquals(order.getItems().size(), result.getItems().size());

        // Проверка сериализации uid'а
        assertEquals(BuyerProvider.UID, result.getUid().longValue());
        assertEquals(BuyerProvider.UID, result.getBuyer().getUid().longValue());
    }

    @Test
    public void orderUidVisibility() {
        final Order created = dropshipDeliveryHelper.createDropshipOrder();

        Order order = client.getOrder(created.getId(), ClientRole.SYSTEM, null);
        assertNotNull(order);
        assertNotNull(order.getBuyer());
        assertEquals(BuyerProvider.UID, order.getUid().longValue());
        assertEquals(BuyerProvider.UID, order.getBuyer().getUid().longValue());

        order = client.getOrder(created.getId(), ClientRole.SHOP, created.getShopId());
        assertNotNull(order);
        assertNull(order.getBuyer());
        assertNull(order.getUid());
    }

    @Test
    public void mustGetEventById() {
        Order order = orderServiceHelper.prepareOrder(
        );

        PagedEvents events = client.orderHistoryEvents()
                .getOrderHistoryEvents(order.getId(), ClientRole.SYSTEM, null, 0, 10);

        assertTrue(!events.getItems().isEmpty());

        OrderHistoryEvent event =
                client.orderHistoryEvents().getOrderHistoryEvent(events.getItems().iterator().next().getId());
        assertNotNull(event);
    }

    @Test
    public void mustWriteRequestIdToHistory() {
        RequestContextHolder.createNewContext();
        String requestId = RequestContextHolder.getContext().getRequestId();

        Order order = orderServiceHelper.prepareOrder();

        PagedEvents events = client.orderHistoryEvents()
                .getOrderHistoryEvents(order.getId(), ClientRole.SYSTEM, null, 0, 10);

        events.getItems().forEach(e -> assertEquals(requestId, e.getRequestId()));
    }

    @Test
    public void canCreateCart() {
        Buyer buyer = BuyerProvider.getBuyer();
        Parameters parameters = new Parameters(buyer);
        MultiCart expected = orderCreateHelper.cart(parameters);
        Order order = expected.getCarts().get(0);
        expected.setBuyerRegionId(buyer.getRegionId());
        order.setDelivery(parameters.getOrder().getDelivery());
        MultiCart actual = client.cart(expected, buyer.getUid());
        assertEquals(1, actual.getCarts().size());
        assertEquals(order.getItems().size(), actual.getCarts().get(0).getItems().size());
        assertEquals(order.getTotal(), actual.getCarts().get(0).getTotal());
    }

    @Test
    public void canCheckout() {
        Buyer buyer = BuyerProvider.getBuyer();
        Parameters parameters = new Parameters(buyer);
        parameters.setDeliveryServiceId(DeliveryProvider.FF_DELIVERY_SERVICE_ID);
        MultiCart expected = orderCreateHelper.cart(parameters);
        Order order = expected.getCarts().get(0);
        expected.setBuyerRegionId(buyer.getRegionId());
        order.setDelivery(parameters.getOrder().getDelivery());
        MultiCart cart = client.cart(expected, buyer.getUid());
        MultiOrder multiOrder = orderCreateHelper.mapCartToOrder(cart, parameters);
        pushApiConfigurer.mockAccept(multiOrder.getCarts().get(0));

        MultiOrder actual = client.checkout(multiOrder, buyer.getUid());
        assertEquals(1, actual.getCarts().size());
        assertEquals(order.getItems().size(), actual.getCarts().get(0).getItems().size());
        assertEquals(order.getTotal(), actual.getCarts().get(0).getTotal());
        List<OrderFailure> failures = actual.getOrderFailures();
        if (failures != null) {
            assertEquals(0, failures.size());
        }
    }


    @Test
    public void canCreateRefund() {
        checkouterProperties.setEnableServicesPrepay(true);
        final long orderId = orderServiceHelper.createPrepaidBlueOrder();
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.DELIVERY);
        final Order order = orderService.getOrder(orderId, ClientInfo.SYSTEM, Set.of(OptionalOrderPart.ITEM_SERVICES));
        assertTrue(order.isFake());
        assertTrue(order.getPayment().isFake());
        final Receipts receipts = client.getOrderReceipts(orderId, ClientRole.SYSTEM, null, null);
        assertEquals(1, receipts.getContent().size());

        RefundableItems refundableItems = RefundTestHelper.refundableItemsFromOrder(order);
        Refund refund = client.refunds().postRefund(order.getId(),
                ClientRole.SHOP_USER,
                order.getShopId(), order.getShopId(), RefundReason.ORDER_CANCELLED,
                "Some text",
                true, PaymentGoal.ORDER_PREPAY, refundableItems.toRefundItems(),
                refundableItems.getRefundableAmount().doubleValue());
        assertNotNull(refund.getId());
        var isAsync = refundHelper.isAsyncRefundStrategyEnabled(refund);
        var refundStatus = isAsync ? RefundStatus.DRAFT : RefundStatus.RETURNED;
        assertThat(refund.getStatus(), equalTo(refundStatus));
        assertEquals(orderId, (long) refund.getOrderId());

        PagedRefunds refunds = client.refunds().getRefunds(orderId,
                new RefundStatus[]{refundStatus},
                ClientRole.SHOP, order.getShopId(),
                0, 50, PaymentGoal.ORDER_PREPAY);
        assertEquals(1, (long) refunds.getPager().getTotal());
        assertEquals(1, refunds.getItems().size());
        ArrayList<Refund> refundsList = new ArrayList<>(refunds.getItems());
        assertEquals(refundableItems.getRefundableAmount().doubleValue(),
                refundsList.get(0).getAmount().doubleValue(), 0.1);
    }

    @Test
    public void canGetActivePayment() {
        long orderId = orderServiceHelper.createGlobalOrder();
        Order order = orderService.getOrder(orderId);

        Payment payment = client.payments().getActivePayment(order.getId(), ClientRole.SYSTEM, order.getShopId());
        assertEquals(PaymentStatus.HOLD, payment.getStatus());
        assertEquals(order.getPayment().getId(), payment.getId());
        assertNull(payment.getFailReason());
    }

    @Test
    public void unbindCardTest() throws Exception {
        trustMockConfigurer.mockUnbindCard();
        final String sessionId = "PASSPORT_OAUTH_TOKEN";
        final String userIp = "127.127.127.4";
        final String cardId = "card_id_string";

        BalanceSimpleResult result = client.unbindCard(sessionId, userIp, cardId, Color.BLUE);
        assertEquals(BalanceStatus.success, result.getStatus());

        Iterator<ServeEvent> serveEvents = trustMockConfigurer.eventsIterator();
        checkUnbindCardCall(serveEvents, sessionId, userIp, cardId);
    }

    @Test
    public void userCardListTest() throws Exception {
        trustMockConfigurer.mockListPaymentMethods();
        final String uid = "1234567898765";
        final String userIp = "127.127.127.4";
        final Long regionId = 333L;

        BalanceListPaymentMethodsResult listPaymentMethodsResult = client.userCardList(
                uid,
                userIp,
                regionId,
                Color.BLUE
        );
        assertEquals(BalanceStatus.success, listPaymentMethodsResult.getStatus());
        assertNotNull(listPaymentMethodsResult.getPaymentMethods());
        assertEquals(2, listPaymentMethodsResult.getPaymentMethods().size());
        assertNotNull(listPaymentMethodsResult.getPaymentMethods().get("card-x5991b708795be261c6c5f2bd"));
        assertEquals(
                "card",
                listPaymentMethodsResult.getPaymentMethods().get("card-x5991b708795be261c6c5f2bd").getType()
        );
        assertEquals(
                "card-x5991b708795be261c6c5f2bd",
                listPaymentMethodsResult.getPaymentMethods().get("card-x5991b708795be261c6c5f2bd").getId()
        );
        assertNull(listPaymentMethodsResult.getPaymentMethods().get("card-x5991b708795be261c6c5f2bd").getCurrency());
        assertEquals(
                "225",
                listPaymentMethodsResult.getPaymentMethods().get("card-x5991b708795be261c6c5f2bd").getRegionId()
        );
        assertNull(listPaymentMethodsResult.getPaymentMethods().get("card-x5991b708795be261c6c5f2bd").getName());
        assertEquals(
                "411111****1111",
                listPaymentMethodsResult.getPaymentMethods().get("card-x5991b708795be261c6c5f2bd").getNumber()
        );

        Iterator<ServeEvent> serveEvents = trustMockConfigurer.eventsIterator();
        checkListPaymentMethodsCall(serveEvents, uid, userIp, regionId.toString());
    }

    @Test
    public void userCardListNoRegionIdTest() throws Exception {
        trustMockConfigurer.mockListPaymentMethods();
        final String uid = "1234567898765";
        final String userIp = "127.127.127.4";

        BalanceListPaymentMethodsResult listPaymentMethodsResult = client.userCardList(
                uid,
                userIp,
                null,
                Color.BLUE
        );
        assertEquals(BalanceStatus.success, listPaymentMethodsResult.getStatus());
        assertNotNull(listPaymentMethodsResult.getPaymentMethods());
        assertEquals(2, listPaymentMethodsResult.getPaymentMethods().size());
        assertNotNull(listPaymentMethodsResult.getPaymentMethods().get("card-x5991b708795be261c6c5f2bd"));
        assertEquals(
                "card",
                listPaymentMethodsResult.getPaymentMethods().get("card-x5991b708795be261c6c5f2bd").getType()
        );
        assertEquals(
                "card-x5991b708795be261c6c5f2bd",
                listPaymentMethodsResult.getPaymentMethods().get("card-x5991b708795be261c6c5f2bd").getId()
        );
        assertNull(listPaymentMethodsResult.getPaymentMethods().get("card-x5991b708795be261c6c5f2bd").getCurrency());
        assertEquals(
                "225",
                listPaymentMethodsResult.getPaymentMethods().get("card-x5991b708795be261c6c5f2bd").getRegionId()
        );
        assertNull(listPaymentMethodsResult.getPaymentMethods().get("card-x5991b708795be261c6c5f2bd").getName());
        assertEquals(
                "411111****1111",
                listPaymentMethodsResult.getPaymentMethods().get("card-x5991b708795be261c6c5f2bd").getNumber()
        );

        checkListPaymentMethodsCall(trustMockConfigurer.eventsIterator(), uid, userIp, null);
    }

    @Test
    public void testGetReturnWithoutBankDetails() {
        // делаем заказ с двумя офферами
        Parameters orderParameters = new Parameters(OrderProvider.getBlueOrder(o -> o.setItems(Arrays.asList(
                OrderItemProvider.orderItemWithSortingCenter()
                        .offer("First_Offer")
                        .build(),
                OrderItemProvider.orderItemWithSortingCenter()
                        .offer("Second_Offer")
                        .build()
        ))));
        // создаём заказ и возврат на все две позиции
        Pair<Order, Return> orderAndReturn = returnHelper.createOrderAndReturn(orderParameters, null);

        Order order = orderAndReturn.getFirst();
        Return ret = orderAndReturn.getSecond();

        // получаем информацию о возврате
        Return readRet = client.returns().getReturn(order.getId(), ret.getId(), false,
                ClientRole.SYSTEM, 123L);

        assertEquals(ret.getId(), readRet.getId());
        assertEquals(ret.getOrderId(), readRet.getOrderId());
        assertEquals(ret.getStatus(), readRet.getStatus());
        assertThat(readRet.getUserCompensationSum(), comparesEqualTo(readRet.getUserCompensationSum()));
        assertEquals(ret.getComment(), readRet.getComment());
        assertThat(readRet.getBankDetails(), nullValue());

        Map<Long, ReturnItem> readItemsById = readRet.getItems().stream()
                .collect(Collectors.toMap(ReturnItem::getItemId, Function.identity()));
        ret.getItems().forEach(i -> {
            assertThat(readItemsById, hasKey(i.getItemId()));
            ReturnItem ri = readItemsById.get(i.getItemId());
            assertEquals(i.getCount(), ri.getCount());
            assertEquals(i.getSupplierCompensation(), ri.getSupplierCompensation());
            assertEquals(i.isDeliveryService(), ri.isDeliveryService());
            assertThat(i.getQuantityIfExistsOrCount(), comparesEqualTo(ri.getQuantityIfExistsOrCount()));
        });
    }

    @Test
    public void testAddTrack() throws Exception {
        Parameters parameters = new Parameters();
        Order order = orderCreateHelper.createOrder(parameters);

        Delivery deliveryRequest = new Delivery();
        Parcel parcel = new Parcel();
        deliveryRequest.setParcels(Collections.singletonList(parcel));
        long parcelId = orderDeliveryHelper.updateOrderDelivery(order.getId(), deliveryRequest)
                .getDelivery().getParcels().get(0).getId();

        Track trackReq = new Track(TRACK_CODE, DeliveryProvider.MOCK_DELIVERY_SERVICE_ID);
        Track track = client.addTrack(order.getId(), parcelId, trackReq, ClientRole.SYSTEM, null);
        assertEquals(DeliveryProvider.MOCK_DELIVERY_SERVICE_ID, track.getDeliveryServiceId());
        assertEquals(TRACK_CODE, track.getTrackCode());
        assertEquals(DeliveryServiceType.CARRIER, track.getDeliveryServiceType());
    }

    @Test
    public void testAddTrackWithType() throws Exception {
        Parameters parameters = new Parameters();
        Order order = orderCreateHelper.createOrder(parameters);

        Delivery deliveryRequest = new Delivery();
        Parcel parcel = new Parcel();
        deliveryRequest.setParcels(Collections.singletonList(parcel));
        long parcelId = orderDeliveryHelper.updateOrderDelivery(order.getId(), deliveryRequest)
                .getDelivery().getParcels().get(0).getId();

        Track trackReq = new Track(TRACK_CODE, DeliveryProvider.MOCK_DELIVERY_SERVICE_ID);
        trackReq.setDeliveryServiceType(DeliveryServiceType.SORTING_CENTER);
        Track track = client.addTrack(order.getId(), parcelId, trackReq, ClientRole.SYSTEM, null);
        assertEquals(DeliveryProvider.MOCK_DELIVERY_SERVICE_ID, track.getDeliveryServiceId());
        assertEquals(TRACK_CODE, track.getTrackCode());
        assertEquals(DeliveryServiceType.SORTING_CENTER, track.getDeliveryServiceType());
    }

    @Test
    public void testPutTrack() throws Exception {
        Parameters parameters = new Parameters();
        Order order = orderCreateHelper.createOrder(parameters);

        Delivery deliveryRequest = new Delivery();
        Parcel parcel = new Parcel();
        deliveryRequest.setParcels(Collections.singletonList(parcel));
        long parcelId = orderDeliveryHelper.updateOrderDelivery(order.getId(), deliveryRequest)
                .getDelivery().getParcels().get(0).getId();

        Track trackReq = new Track(TRACK_CODE, DeliveryProvider.MOCK_DELIVERY_SERVICE_ID);
        List<Track> tracks =
                client.updateDeliveryTracks(order.getId(), parcelId, List.of(trackReq), ClientRole.SYSTEM, null);
        assertEquals(1, tracks.size());
        Track track = tracks.get(0);
        assertEquals(DeliveryProvider.MOCK_DELIVERY_SERVICE_ID, track.getDeliveryServiceId());
        assertEquals(TRACK_CODE, track.getTrackCode());
        assertEquals(DeliveryServiceType.CARRIER, track.getDeliveryServiceType());
    }

    @Test
    public void testGetFirstEventIdAfter() {
        orderServiceHelper.prepareOrder();
        Date futureDate = new Date(Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());
        @Nullable Long eventId = client.getFirstEventIdAfter(futureDate);

        assertThat(eventId, is(nullValue()));
    }

    @Test
    public void putParcelBoxes() {
        Order order = createOrder();

        long parcelId = order.getDelivery().getParcels().get(0).getId();

        ParcelBox parcelBox1 = new ParcelBox();
        parcelBox1.setFulfilmentId("ffId");
        parcelBox1.setWeight(1L);
        parcelBox1.setWidth(2L);
        parcelBox1.setHeight(3L);
        parcelBox1.setDepth(4L);

        ParcelBox parcelBox2 = new ParcelBox();
        parcelBox2.setExternalId("exId");

        ParcelBox parcelBox3 = new ParcelBox();
        parcelBox3.setWeight(10L);

        List<ParcelBox> parcelBoxes = client.putParcelBoxes(order.getId(), parcelId,
                Arrays.asList(parcelBox1, parcelBox2, parcelBox3), ClientRole.SYSTEM, 0L);

        MatcherAssert.assertThat(parcelBoxes, containsInAnyOrder(
                allOf(
                        hasProperty("id"), notNullValue(),
                        hasProperty("fulfilmentId", is("ffId")),
                        hasProperty("externalId", nullValue()),
                        hasProperty("weight", is(1L)),
                        hasProperty("width", is(2L)),
                        hasProperty("height", is(3L)),
                        hasProperty("depth", is(4L))
                ),
                allOf(
                        hasProperty("id"), notNullValue(),
                        hasProperty("fulfilmentId", nullValue()),
                        hasProperty("externalId", is("exId")),
                        hasProperty("weight", nullValue()),
                        hasProperty("width", nullValue()),
                        hasProperty("height", nullValue()),
                        hasProperty("depth", nullValue())
                ),
                allOf(
                        hasProperty("id"), notNullValue(),
                        hasProperty("fulfilmentId", nullValue()),
                        hasProperty("externalId", nullValue()),
                        hasProperty("weight", is(10L)),
                        hasProperty("width", nullValue()),
                        hasProperty("height", nullValue()),
                        hasProperty("depth", nullValue())
                )
        ));
    }

    @Test
    public void shouldAllowToPutParcelBoxesInCancelledOrder() {
        Order order = createOrder();
        orderStatusHelper.proceedOrderToStatus(order, CANCELLED);


        long parcelId = order.getDelivery().getParcels().get(0).getId();

        ParcelBox parcelBox1 = new ParcelBox();
        parcelBox1.setFulfilmentId("ffId");
        parcelBox1.setWeight(1L);
        parcelBox1.setWidth(2L);
        parcelBox1.setHeight(3L);
        parcelBox1.setDepth(4L);

        List<ParcelBox> parcelBoxes = client.putParcelBoxes(order.getId(), parcelId,
                List.of(parcelBox1), ClientRole.SYSTEM, 0L);

        MatcherAssert.assertThat(parcelBoxes, hasSize(1));
        assertEquals("ffId", parcelBoxes.get(0).getFulfilmentId());
    }

    @Test
    public void shouldAllowSecondaryPut() {
        Order order = createOrder();

        long parcelId = order.getDelivery().getParcels().get(0).getId();

        ParcelBox parcelBox1 = new ParcelBox();
        parcelBox1.setFulfilmentId("ffId");
        parcelBox1.setWeight(1L);
        parcelBox1.setWidth(2L);
        parcelBox1.setHeight(3L);
        parcelBox1.setDepth(4L);

        List<ParcelBox> parcelBoxes = client.putParcelBoxes(order.getId(), parcelId,
                List.of(parcelBox1), ClientRole.SYSTEM, 0L);

        MatcherAssert.assertThat(parcelBoxes, hasSize(1));
        assertEquals("ffId", parcelBoxes.get(0).getFulfilmentId());
        Long parcelBoxId = parcelBoxes.get(0).getId();

        List<ParcelBox> parcelBoxes2 = client.putParcelBoxes(order.getId(), parcelId,
                List.of(parcelBox1), ClientRole.SYSTEM, 0L);

        Long parcelBoxId2 = parcelBoxes2.get(0).getId();
        assertNotEquals(parcelBoxId, parcelBoxId2);
    }

    @Test
    public void shouldSkipSecondaryPut() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.DUPLICATE_PARCEL_BOX_STRATEGY,
                DuplicateParcelBoxStrategy.SKIP);
        Order order = createOrder();

        long parcelId = order.getDelivery().getParcels().get(0).getId();

        ParcelBox parcelBox1 = new ParcelBox();
        parcelBox1.setFulfilmentId("ffId");
        parcelBox1.setWeight(1L);
        parcelBox1.setWidth(2L);
        parcelBox1.setHeight(3L);
        parcelBox1.setDepth(4L);

        List<ParcelBox> parcelBoxes = client.putParcelBoxes(order.getId(), parcelId,
                List.of(parcelBox1), ClientRole.SYSTEM, 0L);

        MatcherAssert.assertThat(parcelBoxes, hasSize(1));
        assertEquals("ffId", parcelBoxes.get(0).getFulfilmentId());
        Long parcelBoxId = parcelBoxes.get(0).getId();

        List<ParcelBox> parcelBoxes2 = client.putParcelBoxes(order.getId(), parcelId,
                List.of(parcelBox1), ClientRole.SYSTEM, 0L);

        Long parcelBoxId2 = parcelBoxes2.get(0).getId();
        assertEquals(parcelBoxId, parcelBoxId2);
    }

    @Test
    public void shouldRaiseExceptionOnSecondaryPut() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.DUPLICATE_PARCEL_BOX_STRATEGY,
                DuplicateParcelBoxStrategy.RAISE);
        Order order = createOrder();

        long parcelId = order.getDelivery().getParcels().get(0).getId();

        ParcelBox parcelBox1 = new ParcelBox();
        parcelBox1.setFulfilmentId("ffId");
        parcelBox1.setWeight(1L);
        parcelBox1.setWidth(2L);
        parcelBox1.setHeight(3L);
        parcelBox1.setDepth(4L);

        List<ParcelBox> parcelBoxes = client.putParcelBoxes(order.getId(), parcelId,
                List.of(parcelBox1), ClientRole.SYSTEM, 0L);

        MatcherAssert.assertThat(parcelBoxes, hasSize(1));
        assertEquals("ffId", parcelBoxes.get(0).getFulfilmentId());

        assertThrows(ErrorCodeException.class, () -> {
            client.putParcelBoxes(order.getId(), parcelId,
                    List.of(parcelBox1), ClientRole.SYSTEM, 0L);
        });
    }

    @Test
    public void wrongItemCountCausesPutParcelBoxesReturn400() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            Order order = createOrder();

            long parcelId = order.getDelivery().getParcels().get(0).getId();

            long itemId = order.getItems().iterator().next().getId();

            ParcelBoxItem parcelItem = new ParcelBoxItem();
            parcelItem.setItemId(itemId);
            parcelItem.setCount(2);

            ParcelBox parcelBox = new ParcelBox();
            parcelBox.setItems(Collections.singletonList(parcelItem));

            client.putParcelBoxes(order.getId(), parcelId,
                    Collections.singletonList(parcelBox), ClientRole.SYSTEM, 0L);
        });
    }

    @Test
    public void wrongItemIdCausesPutParcelBoxesReturn400() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {

            Order order = createOrder();

            long parcelId = order.getDelivery().getParcels().get(0).getId();

            long itemId = order.getItems().iterator().next().getId();

            ParcelBoxItem parcelItem = new ParcelBoxItem();
            parcelItem.setItemId(itemId + 1);
            parcelItem.setCount(1);

            ParcelBox parcelBox = new ParcelBox();
            parcelBox.setItems(Collections.singletonList(parcelItem));

            client.putParcelBoxes(order.getId(), parcelId,
                    Collections.singletonList(parcelBox), ClientRole.SYSTEM, 0L);
        });
    }

    /**
     * Проверяет замену коробок для множественного clientId.
     */
    @Test
    public void shouldAllowToPutParcelBoxesInCancelledOrderWithMultiClientId() {
        Parameters parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        long parcelId = order.getDelivery().getParcels().get(0).getId();

        ParcelBox parcelBox1 = new ParcelBox();
        parcelBox1.setFulfilmentId("ffId");
        parcelBox1.setWeight(1L);
        parcelBox1.setWidth(2L);
        parcelBox1.setHeight(3L);
        parcelBox1.setDepth(4L);

        List<ParcelBox> parcelBoxes = client.putParcelBoxes(order.getId(), parcelId, List.of(parcelBox1),
                RequestClientInfo.builder(ClientRole.SHOP)
                        .withClientIds(Set.of(1L, DROPSHIP_SHOP_ID, 2L))
                        .build()
        );

        MatcherAssert.assertThat(parcelBoxes, hasSize(1));
        assertEquals("ffId", parcelBoxes.get(0).getFulfilmentId());
    }

    @Test
    public void updateParcelDeliveredAt() {
        Order order = createOrder();

        Instant expectedDeliveredAt = Instant.now();
        Parcel parcelForUpdate = order.getDelivery().getParcels().get(0);

        ParcelPatchRequest request = new ParcelPatchRequest();
        request.setDeliveredAt(expectedDeliveredAt);
        Parcel parcel = client.updateParcel(order.getId(), parcelForUpdate.getId(), request, ClientRole.SYSTEM, 0L);

        Assertions.assertNotNull(parcel);
        Assertions.assertNotNull(parcel.getDeliveredAt());
        assertEquals(
                parcel.getDeliveredAt().truncatedTo(ChronoUnit.SECONDS),
                expectedDeliveredAt.truncatedTo(ChronoUnit.SECONDS)
        );

        Order orderAfterAction = orderService.getOrder(order.getId());
        Parcel updatedParcel = orderAfterAction.getDelivery().getParcels().get(0);

        Assertions.assertNotNull(updatedParcel);
        Assertions.assertNotNull(updatedParcel.getDeliveredAt());
        assertEquals(
                updatedParcel.getDeliveredAt().truncatedTo(ChronoUnit.SECONDS),
                expectedDeliveredAt.truncatedTo(ChronoUnit.SECONDS)
        );
    }

    @Test
    public void updateParcelStatus() {
        Order order = createOrder();
        Parcel parcelForUpdate = order.getDelivery().getParcels().get(0);
        ParcelPatchRequest request = new ParcelPatchRequest();
        request.setParcelStatus(ParcelStatus.READY_TO_SHIP);

        checkouterProperties.setIsCheckParcelStatusTransitionEnabled(true);

        Assertions.assertThrows(ErrorCodeException.class,
                () -> client.updateParcel(order.getId(),
                        parcelForUpdate.getId(), request, ClientRole.SYSTEM, 0L));
    }

    private Order createOrder() {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        return order;
    }

    @Test
    public void unfreezeStock() {
        Order order = orderServiceHelper.prepareOrder();
        Boolean unfreeze = client.stocks().unfreezeStock(order.getId());
        Assertions.assertTrue(unfreeze);
    }

    @Test
    public void unfreezeNotExistingStock() {
        Assertions.assertThrows(OrderNotFoundException.class, () -> {
            client.stocks().unfreezeStock(2);
        });
    }

    @Test
    public void unfreezeStocks() {
        Order order1 = orderServiceHelper.prepareOrder();
        Order order2 = orderServiceHelper.prepareOrder();
        Set<Long> ids2Unfreeze = new HashSet<>(Arrays.asList(order1.getId(), order2.getId()));
        Set<Long> actualUnfreeze = client.stocks().unfreezeStocks(ids2Unfreeze);
        assertEquals(2, actualUnfreeze.size());
        assertEquals(ids2Unfreeze, actualUnfreeze);
    }

    @Test
    public void unfreezeNotExistingStocks() {
        Set<Long> ids2Unfreeze = new HashSet<>(Arrays.asList(1L, 2L));
        Set<Long> actualUnfreeze = client.stocks().unfreezeStocks(ids2Unfreeze);
        assertTrue(actualUnfreeze.isEmpty());
    }

    @Test
    public void getOrderOptionsAvailabilitiesShouldNotFailOnNonexistentOrder() {
        final List<OrderOptionAvailability> availabilities = client.getOrderOptionsAvailabilities(
                Collections.singleton(11L), ClientRole.SYSTEM, 1L);
        assertNotNull(availabilities);
        assertThat(availabilities, empty());
    }

    @Test
    public void getOrderOptionsAvailabilitiesShouldReturnEmptyResultForOrderInDelivery() {
        final Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        final Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, DELIVERY);

        final List<OrderOptionAvailability> availabilities = client.getOrderOptionsAvailabilities(
                Collections.singleton(order.getId()), ClientRole.SYSTEM, 1L);
        assertNotNull(availabilities);
        assertThat(availabilities, hasSize(1));
        final OrderOptionAvailability optionAvailability = availabilities.get(0);
        assertThat(optionAvailability.getAvailableOptions(), empty());
        final DeliveryServiceCustomerInfo info = optionAvailability.getDeliveryServiceCustomerInfo();
        assertNotNull(info);
        assertThat(info.getPhones(), empty());
        assertNull(info.getTrackOrderSite());
    }

    @Test
    public void getOrderOptionsAvailabilitiesShouldReturnEmptyResultForOrderInProcessing() {
        final Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        final Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        final List<OrderOptionAvailability> availabilities = client.getOrderOptionsAvailabilities(
                Collections.singleton(order.getId()), ClientRole.SYSTEM, 1L);
        assertNotNull(availabilities);
        assertThat(availabilities, hasSize(1));
        final OrderOptionAvailability optionAvailability = availabilities.get(0);
        assertThat(optionAvailability.getAvailableOptions(), empty());
        final DeliveryServiceCustomerInfo info = optionAvailability.getDeliveryServiceCustomerInfo();
        assertNotNull(info);
        assertThat(info.getPhones(), empty());
        assertNull(info.getTrackOrderSite());
    }

    @Test
    public void canPutOrderItemInstances() {
        Order order = orderServiceHelper.prepareOrder();

        OrderItems result = client.putOrderItemInstances(order.getId(), ClientRole.SYSTEM, null,
                new OrderItemInstancesPutRequest(Collections.emptyList()));

        assertEquals(order.getItems().size(), result.getContent().size());

        String cis1 = "010465006531553121CtPoNqNB7qOdc";
        try {
            client.putOrderItemInstances(order.getId(), order.getItems().iterator().next().getId(),
                    ClientRole.SYSTEM, null, Collections.singletonList(cis1)
            );
        } catch (Exception e) {
            Assertions.assertTrue(e.getMessage().contains("Loading CIS not allowed for item"));
        }
    }

    @Test
    public void canPutOrderItemInstancesWithMultiClientId() {
        Order order = orderServiceHelper.prepareOrder();

        OrderItems result = client.putOrderItemInstances(order.getId(),
                RequestClientInfo.builder(ClientRole.SHOP)
                        .withClientIds(Set.of(1L, order.getShopId(), 2L))
                        .build(),
                new OrderItemInstancesPutRequest(Collections.emptyList()));

        assertEquals(order.getItems().size(), result.getContent().size());

        String cis1 = "010465006531553121CtPoNqNB7qOdc";
        try {
            client.putOrderItemInstances(order.getId(), order.getItems().iterator().next().getId(),
                    ClientRole.SYSTEM, null, Collections.singletonList(cis1)
            );
        } catch (Exception e) {
            Assertions.assertTrue(e.getMessage().contains("Loading CIS not allowed for item"));
        }
    }

    @Test
    public void nullifyCashbackEmit() {
        Order order = orderServiceHelper.prepareOrder();
        Order deliveredOrder = orderServiceHelper.prepareOrder();
        orderStatusHelper.proceedOrderToStatus(deliveredOrder, DELIVERED);
        var request = new NullifyCashbackEmitRequest(Set.of(order.getId(), deliveredOrder.getId()));
        RequestClientInfo clientInfo = new RequestClientInfo(ClientRole.SYSTEM, null);
        NullifyCashbackEmitResponse response = client.nullifyCashbackEmit(clientInfo, request);
        assertThat(response.getSucceededOrderIds(), hasItem(is(order.getId())));
        assertThat(response.getFailedOrderIds(), hasItem(is(deliveredOrder.getId())));
    }

    @Test
    public void shouldReturnMarketBrandedOrders() throws IOException {
        Parameters parameters = new Parameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.PICKUP)
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addPickup(BlueParametersProvider.PICKUP_SERVICE_ID)
                                .build()
                )
                .buildParameters();
        parameters.getReportParameters()
                .getActualDelivery()
                .getResults()
                .get(0)
                .getPickup()
                .forEach(p -> p.setMarketBranded(true));
        Order order = orderCreateHelper.createOrder(parameters);

        assertTrue(order.getDelivery().isMarketBranded());

        OrderSearchRequest request = new OrderSearchRequest();
        request.setRgbs(new Color[]{Color.BLUE});
        request.setMarketBranded(true);
        RequestClientInfo requestClientInfo = new RequestClientInfo(ClientRole.SHOP, order.getShopId());
        PagedOrders pagedOrders = client.getOrders(requestClientInfo, request);
        Order foundOrder =
                pagedOrders.getItems().stream().filter(o -> o.getId().equals(order.getId())).findAny().orElseThrow();
        assertEquals(order.getDelivery().isMarketBranded(), foundOrder.getDelivery().isMarketBranded());
    }

    @Test
    public void shouldGetParcelInfoFromReport() {
        Parameters parameters = new Parameters();
        String parcelInfo = "w:5;p:5;pc:RUR;tp:5;tpc:RUR;d:10x20x30;ct:1/2/3;wh:145;ffwh:145;";
        parameters.getReportParameters().getActualDelivery().getResults().forEach(r -> r.setParcelInfo(parcelInfo));
        MultiCart multiCart = orderCreateHelper.cart(parameters);

        assertEquals(1, multiCart.getCarts().size());
        String parcelInfoRs = multiCart.getCarts().get(0).getParcelInfo();
        assertEquals(parcelInfo, parcelInfoRs);
    }

    @Test
    public void shouldCartWithNullParcelInfo() {
        Parameters parameters = new Parameters();
        parameters.getReportParameters().getActualDelivery().getResults().forEach(r -> r.setParcelInfo(null));
        MultiCart multiCart = orderCreateHelper.cart(parameters);

        assertEquals(1, multiCart.getCarts().size());
        String parcelInfoRs = multiCart.getCarts().get(0).getParcelInfo();
        assertNull(parcelInfoRs);
    }

    private String getReportCombinatorResponseAsJson() throws IOException {
        return IOUtils.readInputStream(Objects.requireNonNull(getClass()
                .getResourceAsStream("/json/reportResponse.json")));
    }

    /**
     * Проверяет получение заказа при множественном clientId.
     */
    @Test
    public void testGetOrderWithMultipleClientIds() {
        Order order = createOrder();
        var requestClientInfo = RequestClientInfo.builder(ClientRole.SHOP)
                .withClientIds(Set.of(1L, SHOP_ID_WITH_SORTING_CENTER, 2L))
                .build();
        assertNotNull(client.getOrder(requestClientInfo, OrderRequest.builder(order.getId()).build()));
    }
}
