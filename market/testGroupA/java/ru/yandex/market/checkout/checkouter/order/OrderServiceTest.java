package ru.yandex.market.checkout.checkouter.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import com.google.common.collect.Iterables;
import org.hamcrest.Matchers;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.db.SortingInfo;
import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.backbone.order.reservation.OrderCompletionService;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackId;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentOperations;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.helpers.OrderInsertHelper;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.test.providers.TrackCheckpointProvider;
import ru.yandex.market.checkout.test.providers.TrackProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.common.util.currency.Currency.RUR;
import static ru.yandex.market.checkout.checkouter.order.ControllerUtils.buildUnlimitedPager;

public class OrderServiceTest extends AbstractServicesTestBase {

    private static final long TRACKER_ID = 123L;

    @Autowired
    private OrderCompletionService orderCompletionService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ShopService shopService;
    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private OrderInsertHelper orderInsertHelper;
    @Autowired
    @Qualifier("routingPaymentOperations")
    private PaymentOperations paymentOperations;

    @Test
    public void testInsertOrder() {
        Order order = OrderProvider.getBlueOrder();
        //
        long id = orderCreateService.createOrder(order, ClientInfo.SYSTEM);

        Order order1 = orderService.getOrder(id);
        Assertions.assertNotNull(order1);

        // Проверить идентификаторы строк заказа на заполненность.
        final Collection<OrderItem> items = order1.getItems();
        Assertions.assertNotNull(items);
        items.forEach(item -> Assertions.assertNotNull(item.getId()));
    }

    @Test
    public void testCreateOrderCountAndQuantityFilling() {
        Order order1 = OrderProvider.getBlueOrder();
        OrderItem orderItem1 = order1.getItems().iterator().next();
        orderItem1.setCount(10);
        orderItem1.setQuantity(BigDecimal.TEN);
        //
        Order order2 = OrderProvider.getBlueOrder();
        OrderItem orderItem2 = order2.getItems().iterator().next();
        orderItem2.setCount(null);
        orderItem2.setQuantity(null);

        long id1 = orderCreateService.createOrder(order1, ClientInfo.SYSTEM);
        long id2 = orderCreateService.createOrder(order2, ClientInfo.SYSTEM);

        OrderItem item1 = orderService.getOrder(id1).getItems().iterator().next();
        assertEquals(10, item1.getCount());
        assertThat(BigDecimal.TEN, comparesEqualTo(item1.getQuantity()));
        OrderItem item2 = orderService.getOrder(id2).getItems().iterator().next();
        assertEquals(0, item2.getCount());
        assertNull(item2.getQuantity());
    }

    @Disabled // включить для personal_phone_id после MARKETCHECKOUT-27095
    @Test
    public void testEmptyNormalizedPhone() {
        Order order = OrderProvider.getBlueOrder();
        order.getBuyer().setNormalizedPhone(null);

        try {
            orderCreateService.createOrder(order, ClientInfo.SYSTEM);
            fail("ORDER_BUYER must have not null constraint on NORMALIZED_PHONE");
        } catch (DataAccessException e) {
            assertTrue(e.getCause() instanceof PSQLException, "Must be PSQLException with constraint error");
            assertThat(e.getMessage(), Matchers.containsString("normalized_phone"));
        }
    }

    @Test
    @Disabled
    public void shouldNotDeleteBuyerAddressOnTrackCodeUpdate() throws Exception {
        long order1 = createGlobalOrder();

        Order updatedOrder = putTrackIntoOrder(order1);

        Assertions.assertNotNull(updatedOrder.getDelivery().getBuyerAddress());

        orderUpdateService.updateTrackSetTrackerId(
                order1,
                new TrackId(TrackProvider.TRACK_CODE, TrackProvider.DELIVERY_SERVICE_ID),
                TRACKER_ID
        );
        Order updatedOrder2 = orderService.getOrder(order1);
        Assertions.assertNotNull(updatedOrder2.getDelivery().getBuyerAddress());

        orderServiceHelper.insertCheckpoint();

        Order updatedOrder3 = orderService.getOrder(order1);
        Assertions.assertNotNull(updatedOrder3.getDelivery().getBuyerAddress());
    }

    @Test
    @Disabled
    public void shouldSaveTrackCheckpoints() {
        long orderId = createGlobalOrder();

        putTrackIntoOrder(orderId);
        orderServiceHelper.insertCheckpoint();

        Order orderWithCheckpoints = orderService.getOrder(orderId);

        Track track = Iterables.getOnlyElement(
                orderWithCheckpoints.getDelivery().getParcels().get(0).getTracks()
        );
        TrackCheckpoint checkpoint = Iterables.getOnlyElement(track.getCheckpoints());

        assertEquals(
                TrackCheckpointProvider.DEFAULT_CHECKPOINT_STATUS,
                checkpoint.getDeliveryCheckpointStatus().intValue()
        );
    }

    @Test
    public void shouldNotCreateDuplicateAddress() {
        Order order = OrderProvider.getBlueOrder();

        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);

        SqlRowSet sqlRowSet = getRandomWritableJdbcTemplate()
                .queryForRowSet("select SHOP_ADDRESS_ID, BUYER_ADDRESS_ID from ORDER_DELIVERY where ORDER_ID = ?",
                        orderId);
        Assertions.assertTrue(sqlRowSet.next());
        Assertions.assertEquals(sqlRowSet.getLong(1), sqlRowSet.getLong(2));
    }

    @Test
    @Disabled
    public void shouldRetrieveOrderEventsWithCheckpoints() throws Exception {
        long orderId = createGlobalOrder();
        putTrackIntoOrder(orderId);
        orderServiceHelper.insertCheckpoint();

        PagedEvents events = eventService.getPagedOrderHistoryEvents(
                orderId,
                Pager.atPage(1, 10),
                null,
                null,
                Collections.emptySet(),
                false,
                ClientInfo.SYSTEM,
                null
        );

        Order orderAfter = Iterables.getFirst(events.getItems(), null).getOrderAfter();

        assertThat(
                orderAfter.getDelivery().getParcels().get(0)
                        .getTracks().get(0)
                        .getCheckpoints(),
                hasSize(1)
        );
    }

    @Test
    @Disabled
    public void shouldNotDeleteCheckpointsOnShipmentUpdate() {
        final long deliveryServiceId = DeliveryProvider.RUSPOSTPICKUP_DELIVERY_SERVICE_ID;

        Order order = createPostOrder();
        order = putTrackIntoOrder(order.getId(), deliveryServiceId);

        Delivery delivery = order.getDelivery();
        orderUpdateService.updateOrderDelivery(order.getId(), delivery, ClientInfo.SYSTEM);

        Parcel orderShipment = new Parcel();
        orderShipment.setId(order.getDelivery().getParcels().get(0).getId());
        orderShipment.setWeight(3000L);
        orderShipment.setWidth(2L);
        orderShipment.setHeight(2L);
        orderShipment.setDepth(2L);
        orderShipment.setStatus(ParcelStatus.CREATED);

        Delivery deliveryWithBasicShipment = new Delivery();
        deliveryWithBasicShipment.setParcels(Collections.singletonList(orderShipment));

        orderUpdateService.updateOrderDelivery(order.getId(), deliveryWithBasicShipment, ClientInfo.SYSTEM);

        final String trackCode = "SG1234567890RU";
        order = putTrackIntoOrder(order.getId(), TrackProvider.createTrack(trackCode, deliveryServiceId));

        TrackId trackId = new TrackId(trackCode, deliveryServiceId);
        orderUpdateService.updateTrackSetTrackerId(order.getId(), trackId, 123L);

        orderServiceHelper.insertCheckpoint(trackId, TrackCheckpointProvider.createCheckpoint(123));

        Order updatedOrder = orderService.getOrder(order.getId());

        Parcel shipment = new Parcel();
        shipment.setId(updatedOrder.getDelivery().getParcels().get(0).getId());
        shipment.setStatus(ParcelStatus.ERROR);

        Delivery deliveryWithShipment = new Delivery();
        deliveryWithShipment.setParcels(Collections.singletonList(shipment));

        updatedOrder = orderUpdateService.updateOrderDelivery(order.getId(), deliveryWithShipment, ClientInfo.SYSTEM);
        assertEquals(1, updatedOrder.getDelivery()
                .getParcels().get(0)
                .getTracks().get(0)
                .getCheckpoints().size());

        PagedEvents events = eventService.getPagedOrderHistoryEvents(order.getId(), buildUnlimitedPager(0, 1), null,
                null, Collections.emptySet(), false, ClientInfo.SYSTEM, null);
        Order orderAfter = Iterables.get(events.getItems(), 0).getOrderAfter();

        assertEquals(1, orderAfter.getDelivery()
                .getParcels().get(0)
                .getTracks().get(0)
                .getCheckpoints().size());
    }

    @Test
    @Disabled
    public void shouldSelectCheckpointsForMultipleOrders() {
        IntStream.of(123, 124).forEach(checkpointId -> {
            long orderId = createGlobalOrder();
            long deliveryServiceId = TrackProvider.DELIVERY_SERVICE_ID + checkpointId;
            putTrackIntoOrder(orderId, deliveryServiceId);

            orderUpdateService.updateTrackSetTrackerId(
                    orderId,
                    new TrackId(TrackProvider.TRACK_CODE, deliveryServiceId),
                    123L
            );

            List<TrackCheckpoint> checkpoints = Collections.singletonList(
                    TrackCheckpointProvider.createCheckpoint(123, checkpointId)
            );
            orderServiceHelper.insertCheckpoints(
                    new TrackId(TrackProvider.TRACK_CODE, deliveryServiceId),
                    checkpoints
            );
        });

        OrderSearchRequest orderSearchRequest = new OrderSearchRequest();
        orderSearchRequest.shopId = OrderProvider.SHOP_ID;
        orderSearchRequest.sorting = Collections.singletonList(new SortingInfo<>(OrderSortingField.ID));
        PagedOrders orders = orderService.getOrders(orderSearchRequest, ClientInfo.SYSTEM);

        List<TrackCheckpoint> firstOrderCheckpoints = Iterables.get(orders.getItems(), 0).getDelivery()
                .getParcels().get(0)
                .getTracks().get(0)
                .getCheckpoints();
        assertEquals(1, firstOrderCheckpoints.size());
        assertEquals(123, firstOrderCheckpoints.get(0).getTrackerCheckpointId());

        List<TrackCheckpoint> secondOrderCheckpoints = Iterables.get(orders.getItems(), 1).getDelivery()
                .getParcels().get(0)
                .getTracks().get(0)
                .getCheckpoints();
        assertEquals(1, secondOrderCheckpoints.size());
        assertEquals(124, secondOrderCheckpoints.get(0).getTrackerCheckpointId());
    }

    @Test
    public void shouldSaveBuyerAssessorProperty() {
        Order order = OrderProvider.getBlueOrder();
        order.setBuyer(BuyerProvider.getBuyerAssessor());

        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);

        Order orderFromDatabase = orderService.getOrder(orderId);

        assertTrue(orderFromDatabase.getBuyer().getAssessor());
    }

    @Test
    public void shouldNotHideAssessorFlagIfBuyerIsNotHidden() {
        Order order = OrderProvider.getBlueOrder();
        order.setBuyer(BuyerProvider.getBuyerAssessor());

        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderUpdateService.reserveOrder(orderId, String.valueOf(orderId), order.getDelivery());

        OrderSearchRequest orderSearchRequest = new OrderSearchRequest();
        orderSearchRequest.shopId = order.getShopId();
        PagedOrders ordersByShop = orderService.getOrders(orderSearchRequest, ClientInfo.SYSTEM);
        assertTrue(Iterables.getOnlyElement(ordersByShop.getItems()).getBuyer().getAssessor());
    }

    @Test
    public void shouldHideAssessorFlagIfBuyerIsHidden() {
        Order order = OrderProvider.getBlueOrder();
        order.setBuyer(BuyerProvider.getBuyerAssessor());

        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderUpdateService.reserveOrder(orderId, String.valueOf(orderId), order.getDelivery());

        OrderSearchRequest orderSearchRequest = new OrderSearchRequest();
        orderSearchRequest.shopId = order.getShopId();
        PagedOrders ordersByShop = orderService.getOrders(
                orderSearchRequest,
                new ClientInfo(ClientRole.SHOP, order.getShopId())
        );
        assertNull(Iterables.getOnlyElement(ordersByShop.getItems()).getBuyer());
    }

    @Test
    public void shouldReturnOnlyPersonBuyerType() {
        long buyerUid = 9585769134L;

        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, buyerUid);

        Order order = OrderProvider.getBlueOrder();
        order.setBuyer(BuyerProvider.getDefaultBuyer(buyerUid));

        order.getBuyer().setBusinessBalanceId(null);
        long personOrderId = orderCreateService.createOrder(order, clientInfo);

        order.getBuyer().setBusinessBalanceId(1L);
        long businessOrderId = orderCreateService.createOrder(order, clientInfo);

        OrderSearchRequest orderSearchRequest = new OrderSearchRequest();
        orderSearchRequest.userId = buyerUid;
        orderSearchRequest.buyerType = BuyerType.PERSON;

        PagedOrders result = orderService.getOrders(
                orderSearchRequest,
                new ClientInfo(ClientRole.SYSTEM, 0L)
        );

        boolean hasPersonOrder = result.getItems().stream().anyMatch(o -> o.getId() == personOrderId);
        boolean hasBusinessOrder = result.getItems().stream().anyMatch(o -> o.getId() == businessOrderId);

        assertTrue(hasPersonOrder);
        assertFalse(hasBusinessOrder);
    }

    @Test
    public void shouldReturnLightweightModelForPI() {
        orderServiceHelper.createPrepaidBlueOrder(true);

        OrderSearchRequest orderSearchRequest = new OrderSearchRequest();
        orderSearchRequest.partials = EnumSet.of(OptionalOrderPart.CHANGE_REQUEST);
        orderSearchRequest.shopId = OrderProvider.SHOP_ID;
        orderSearchRequest.lightweightResultForPI = true;
        PagedOrders ordersByShop = orderService.getOrders(orderSearchRequest, ClientInfo.SYSTEM);

        assertEquals(1, ordersByShop.getItems().size());
        Order actual = Iterables.getFirst(ordersByShop.getItems(), null);

        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertNotNull(actual.getBuyer());
        assertEquals(BuyerProvider.getBuyer().getUid(), actual.getBuyer().getUid());
        assertEquals(BuyerProvider.getBuyer().getYandexUid(), actual.getBuyer().getYandexUid());
        assertEquals(Currency.RUR, actual.getBuyerCurrency());
        assertNotNull(actual.getBuyerTotal());

        assertNotNull(actual.getItems());
        assertEquals(1, actual.getItems().size());
        OrderItem actualItem = Iterables.getFirst(actual.getItems(), null);
        assertEquals(OrderItemProvider.defaultOrderItem().getFeedOfferId().getFeedId(),
                actualItem.getFeedOfferId().getFeedId());
        assertEquals(OrderItemProvider.defaultOrderItem().getOfferName(), actualItem.getOfferName());
        assertEquals(OrderItemProvider.defaultOrderItem().getFeedCategoryId(), actualItem.getFeedCategoryId());
        assertNotNull(actualItem.getBuyerPrice());
        assertEquals(OrderItemProvider.defaultOrderItem().getCartShowUid(), actualItem.getCartShowUid());
        assertEquals(OrderItemProvider.defaultOrderItem().getWarehouseId(), actualItem.getWarehouseId());

        assertNotNull(actual.getCreationDate());
        assertEquals(Boolean.FALSE, actual.isFulfilment());
        assertNotNull(actual.getDelivery());
        assertEquals(DeliveryType.DELIVERY, actual.getDelivery().getType());
        assertNotNull(actual.getDelivery().getPrice());
        assertNotNull(actual.getDelivery().getDeliveryDates());
        assertNotNull(actual.getDelivery().getDeliveryDates().getFromDate());
        assertNotNull(actual.getDelivery().getBuyerAddress());
        assertNotNull(actual.getDelivery().getShopAddress());
        assertEquals(DeliveryProvider.yandexDelivery().build().getDeliveryPartnerType(),
                actual.getDelivery().getDeliveryPartnerType());
        assertNotNull(actual.getDelivery().getPrices());
        assertEquals(Boolean.TRUE, actual.isFake());
        assertEquals(OrderProvider.SHOP_ID, actual.getShopId());
        assertNotNull(actual.getShopOrderId());
        assertEquals(OrderStatus.PROCESSING, actual.getStatus());
        assertEquals(OrderSubstatus.STARTED, actual.getSubstatus());
        assertNotNull(actual.getBuyerTotalWithSubsidy());

        assertTrue(actual.getDelivery().getPromos().isEmpty());
        assertTrue(actual.getItems().stream().map(OrderItem::getPromos).allMatch(Set::isEmpty));
    }

    @Test
    public void shouldReturnOnlyBusinessBuyerType() {
        long buyerUid = 9585769137L;

        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, buyerUid);

        Order order = OrderProvider.getBlueOrder();
        order.setBuyer(BuyerProvider.getDefaultBuyer(buyerUid));

        order.getBuyer().setBusinessBalanceId(null);
        long personOrderId = orderCreateService.createOrder(order, clientInfo);

        order.getBuyer().setBusinessBalanceId(1L);
        long businessOrderId = orderCreateService.createOrder(order, clientInfo);

        OrderSearchRequest orderSearchRequest = new OrderSearchRequest();
        orderSearchRequest.userId = buyerUid;
        orderSearchRequest.buyerType = BuyerType.BUSINESS;

        PagedOrders result = orderService.getOrders(
                orderSearchRequest,
                new ClientInfo(ClientRole.SYSTEM, 0L)
        );

        boolean hasPersonOrder = result.getItems().stream().anyMatch(o -> o.getId() == personOrderId);
        boolean hasBusinessOrder = result.getItems().stream().anyMatch(o -> o.getId() == businessOrderId);

        assertFalse(hasPersonOrder);
        assertTrue(hasBusinessOrder);
    }

    @Test
    public void shouldInsertBindKey() {
        String bindKey = "asdasd";

        Order order = OrderProvider.getBlueOrder((o) -> {
            o.getBuyer().setBindKey(bindKey);
        });

        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);

        Order inserted = orderService.getOrder(orderId);
        Assertions.assertEquals(bindKey, inserted.getBuyer().getBindKey());
    }

    @Test
    public void shouldFindOrderByBindKey() {
        String bindKey = "asdasd";

        Order order = OrderProvider.getBlueOrder((o) -> {
            o.getBuyer().setBindKey(bindKey);
        });

        Order order2 = OrderProvider.getBlueOrder((o) -> {
            o.getBuyer().setBindKey("defdef");
        });

        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        long orderId2 = orderCreateService.createOrder(order2, ClientInfo.SYSTEM);

        OrderSearchRequest orderSearchRequest = new OrderSearchRequest();
        orderSearchRequest.bindKey = bindKey;

        PagedOrders orders = orderService.getOrders(orderSearchRequest, ClientInfo.SYSTEM);

        assertThat(orders.getItems(), hasSize(1));
        Order onlyOrder = Iterables.getOnlyElement(orders.getItems());
        Assertions.assertEquals(orderId, onlyOrder.getId().longValue());
        Assertions.assertEquals(bindKey, onlyOrder.getBuyer().getBindKey());
    }

    @Test
    public void shoudReturnAllOrdersForEmptyBindKeyQuery() {
        String bindKey = "";

        Order order = OrderProvider.getBlueOrder((o) -> {
            o.getBuyer().setBindKey(bindKey);
        });

        Order order2 = OrderProvider.getBlueOrder((o) -> {
            o.getBuyer().setBindKey("defdef");
        });

        Order order3 = OrderProvider.getBlueOrder();

        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        long orderId2 = orderCreateService.createOrder(order2, ClientInfo.SYSTEM);
        long orderId3 = orderCreateService.createOrder(order3, ClientInfo.SYSTEM);

        OrderSearchRequest orderSearchRequest = new OrderSearchRequest();
        orderSearchRequest.bindKey = bindKey;

        PagedOrders orders = orderService.getOrders(orderSearchRequest, ClientInfo.SYSTEM);

        assertThat(orders.getItems(), hasSize(3));
    }

    @Test
    public void shouldNotAllowShopToFilterByBindKeyInPersonalDataHiddenStatuses() {
        String bindKey = "asdasd";

        Order order = OrderProvider.getBlueOrder((o) -> {
            o.getBuyer().setBindKey(bindKey);
            o.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        });

        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        Order reserved = orderUpdateService.reserveOrder(orderId, String.valueOf(orderId), order.getDelivery());
        orderCompletionService.completeOrder(reserved, ClientInfo.SYSTEM);

        OrderSearchRequest orderSearchRequest = new OrderSearchRequest();
        orderSearchRequest.bindKey = "asdasd";

        PagedOrders orders = orderService.getOrders(
                orderSearchRequest,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID)
        );

        assertThat(orders.getItems(), hasSize(0));
    }

    @Test
    @Disabled
    public void shouldAllowShopToFilterByBindKeyInNotHiddenStatuses() {
        String bindKey = "asdasd";

        Order order = OrderProvider.getBlueOrder((o) -> {
            o.getBuyer().setBindKey(bindKey);
        });

        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        Order reserved = orderUpdateService.reserveOrder(orderId, String.valueOf(orderId), order.getDelivery());
        orderCompletionService.completeOrder(reserved, ClientInfo.SYSTEM);

        OrderSearchRequest orderSearchRequest = new OrderSearchRequest();
        orderSearchRequest.bindKey = "asdasd";

        PagedOrders orders = orderService.getOrders(
                orderSearchRequest,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID)
        );

        assertThat(orders.getItems(), hasSize(1));
    }

    @Test
    public void shouldGenerateBindKey() {
        long orderId = orderCreateService.createOrder(OrderProvider.getBlueOrder((o) -> o.setNoAuth(true)),
                ClientInfo.SYSTEM);

        Order order = orderService.getOrder(orderId);
        Assertions.assertNotNull(order.getBuyer().getBindKey());
    }

    @Test
    @Disabled
    public void shouldLoadPaymentForEachOrder() {
        List<Long> orderIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            orderIds.add(
                    OrderServiceHelper.createPrepaidOrder(
                            orderCreateService,
                            orderUpdateService,
                            orderCompletionService,
                            shopService,
                            paymentService,
                            paymentOperations
                    )
            );
        }

        OrderSearchRequest orderSearchRequest = new OrderSearchRequest();
        orderSearchRequest.shopId = OrderProvider.SHOP_ID;

        PagedOrders orders = orderService.getOrders(
                orderSearchRequest,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID)
        );

        orders.getItems().forEach(o -> {
            Assertions.assertNotNull(o.getPayment());
        });
    }

    @Test
    public void getOrdersByNotes() {
        createOrderWithNotes("qwerty");
        createOrderWithNotes("qwerty100");
        createOrderWithNotes("qwerty100%");
        createOrderWithNotes("abc");
        createOrderWithNotes("abc_");
        createOrderWithNotes("abc?");

        checkFoundOrders("qwe", 3);
        checkFoundOrders("qwee", 0);
        checkFoundOrders("qwerty100", 2);
        checkFoundOrders("qwerty100%", 1);
        checkFoundOrders("abc", 3);
        checkFoundOrders("_", 1);
        checkFoundOrders("?", 1);
    }

    @Test
    public void shouldFilterByAssessor() {
        Order order = OrderProvider.getBlueOrder(o -> {
            o.setBuyer(BuyerProvider.getBuyerAssessor());
        });

        Order saved = orderServiceHelper.saveOrder(order);

        OrderSearchRequest withAssessor = new OrderSearchRequest();
        withAssessor.assessor = true;

        PagedOrders orders = orderService.getOrders(withAssessor, ClientInfo.SYSTEM);

        Assertions.assertTrue(orders.getItems().stream().anyMatch(o -> saved.getId().equals(o.getId())), "should " +
                "contain saved order");

        OrderSearchRequest withoutAssessor = new OrderSearchRequest();
        withoutAssessor.assessor = false;

        PagedOrders orders2 = orderService.getOrders(withoutAssessor, ClientInfo.SYSTEM);

        Assertions.assertTrue(orders2.getItems().stream().noneMatch(o -> saved.getId().equals(o.getId())), "should " +
                "not contain saved order");
    }

    @Test
    public void shouldNotAllowToFilterByAssessorForShopRoles() {
        ClientInfo clientInfo = new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID);

        Order order = OrderProvider.getBlueOrder(o -> {
            o.setBuyer(BuyerProvider.getBuyerAssessor());
        });

        Order saved = orderServiceHelper.saveOrder(order);

        OrderSearchRequest withAssessor = new OrderSearchRequest();
        withAssessor.assessor = true;

        PagedOrders orders = orderService.getOrders(withAssessor, clientInfo);

        Assertions.assertTrue(orders.getItems().stream().anyMatch(o -> saved.getId().equals(o.getId())), "should " +
                "contain saved order");

        OrderSearchRequest withoutAssessor = new OrderSearchRequest();
        withoutAssessor.assessor = false;

        PagedOrders orders2 = orderService.getOrders(withoutAssessor, clientInfo);

        Assertions.assertTrue(orders2.getItems().stream().anyMatch(o -> saved.getId().equals(o.getId())), "should " +
                "contain saved order");
    }

    @Test
    public void withStatuses() {
        // Arrange
        createOrdersForSearchTest();
        OrderSearchRequest searchRequest = new OrderSearchRequest();

        // Act + Assert
        assertEquals(4, orderService.countOrders(searchRequest, ClientInfo.SYSTEM));

        searchRequest.setStatuses(new OrderStatus[]{OrderStatus.DELIVERED});
        assertEquals(0, orderService.countOrders(searchRequest, ClientInfo.SYSTEM));

        searchRequest.setStatuses(new OrderStatus[]{OrderStatus.CANCELLED});
        assertEquals(2, orderService.countOrders(searchRequest, ClientInfo.SYSTEM));

        searchRequest.setStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.RESERVED});
        assertEquals(3, orderService.countOrders(searchRequest, ClientInfo.SYSTEM));

        searchRequest.setStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.RESERVED, OrderStatus.PLACING});
        assertEquals(4, orderService.countOrders(searchRequest, ClientInfo.SYSTEM));
    }

    @Test
    public void withNotStatuses() {
        // Arrange
        createOrdersForSearchTest();
        OrderSearchRequest searchRequest = new OrderSearchRequest();

        // Act + Assert
        assertEquals(4, orderService.countOrders(searchRequest, ClientInfo.SYSTEM));

        searchRequest.setNotStatuses(new OrderStatus[]{OrderStatus.DELIVERED});
        assertEquals(4, orderService.countOrders(searchRequest, ClientInfo.SYSTEM));

        searchRequest.setNotStatuses(new OrderStatus[]{OrderStatus.RESERVED});
        assertEquals(3, orderService.countOrders(searchRequest, ClientInfo.SYSTEM));

        searchRequest.setNotStatuses(new OrderStatus[]{OrderStatus.RESERVED, OrderStatus.CANCELLED});
        assertEquals(1, orderService.countOrders(searchRequest, ClientInfo.SYSTEM));

        searchRequest.setNotStatuses(new OrderStatus[]{OrderStatus.RESERVED, OrderStatus.CANCELLED,
                OrderStatus.PLACING});
        assertEquals(0, orderService.countOrders(searchRequest, ClientInfo.SYSTEM));
    }

    @Test
    public void withSubstatuses() {
        // Arrange
        createOrdersForSearchTest();
        OrderSearchRequest searchRequest = new OrderSearchRequest();

        // Act + Assert
        assertEquals(4, orderService.countOrders(searchRequest, ClientInfo.SYSTEM));

        searchRequest.setSubstatuses(new OrderSubstatus[]{OrderSubstatus.AWAIT_CONFIRMATION});
        assertEquals(0, orderService.countOrders(searchRequest, ClientInfo.SYSTEM));

        searchRequest.setSubstatuses(new OrderSubstatus[]{OrderSubstatus.USER_CHANGED_MIND});
        assertEquals(1, orderService.countOrders(searchRequest, ClientInfo.SYSTEM));

        searchRequest.setSubstatuses(new OrderSubstatus[]{OrderSubstatus.USER_CHANGED_MIND,
                OrderSubstatus.DELIVERY_SERVICE_FAILED});
        assertEquals(2, orderService.countOrders(searchRequest, ClientInfo.SYSTEM));
    }

    @Test
    public void withNotSubstatuses() {
        // Arrange
        createOrdersForSearchTest();
        OrderSearchRequest searchRequest = new OrderSearchRequest();

        // Act + Assert
        assertEquals(4, orderService.countOrders(searchRequest, ClientInfo.SYSTEM));

        searchRequest.setNotSubstatuses(new OrderSubstatus[]{OrderSubstatus.AWAIT_CONFIRMATION});
        assertEquals(4, orderService.countOrders(searchRequest, ClientInfo.SYSTEM));

        searchRequest.setNotSubstatuses(new OrderSubstatus[]{OrderSubstatus.USER_CHANGED_MIND});
        assertEquals(3, orderService.countOrders(searchRequest, ClientInfo.SYSTEM));

        searchRequest.setNotSubstatuses(new OrderSubstatus[]{OrderSubstatus.USER_CHANGED_MIND,
                OrderSubstatus.DELIVERY_SERVICE_FAILED});
        assertEquals(2, orderService.countOrders(searchRequest, ClientInfo.SYSTEM));
    }

    @Test
    public void getOrderColorForExistingOrder() {
        Order order = OrderProvider.getBlueOrder();
        long id = orderCreateService.createOrder(order, ClientInfo.SYSTEM);

        assertEquals(Color.BLUE, orderService.getOrderColor(id));
    }

    @Test
    public void getOrdersShouldReturnOutletStoragePeriodAndLimitDate() {
        //given:
        Order order = OrderProvider.getBlueOrder();
        int outletStoragePeriod = 88;
        order.getDelivery().setOutletStoragePeriod(outletStoragePeriod);
        LocalDate storageLimitDate = LocalDate.now();
        order.getDelivery().setOutletStorageLimitDate(storageLimitDate);
        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);

        //when:
        PagedOrders pagedOrders = orderService.getOrders(new OrderSearchRequest(), ClientInfo.SYSTEM);

        //then:
        assertThat(pagedOrders.getItems(), is(not(empty())));
        var deliveries = pagedOrders.getItems().stream()
                .filter(i -> i.getId() == orderId)
                .map(Order::getDelivery)
                .collect(toList());
        assertThat(deliveries, not(empty()));
        deliveries.forEach(delivery -> {
            assertThat(delivery.getOutletStoragePeriod(), equalTo(outletStoragePeriod));
            assertThat(delivery.getOutletStorageLimitDate(), equalTo(storageLimitDate));
        });
    }

    @Test
    public void getOrderColorForNonexistentOrder() {
        Assertions.assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrderColor(Long.MAX_VALUE);
        });
    }

    @Test
    public void getPaymentIdByOrderIdSuccessfully() {
        long orderId = orderServiceHelper.createPrepaidBlueOrder(true);
        Order order = orderService.getOrder(orderId);
        Payment payment = order.getPayment();
        long paymentId1 = payment.getId();
        long paymentId2 = orderService.getPaymentIdByOrderId(orderId);
        assertEquals(paymentId1, paymentId2);
    }

    @Test
    public void getPaymentIdByOrderIdForNotPayedOrder() {
        long orderId = orderServiceHelper.createPrepaidBlueOrder(false);
        Long paymentId = orderService.getPaymentIdByOrderId(orderId);
        assertNull(paymentId);
    }

    @Test
    public void getPaymentIdByOrderIdForNonExistingOrder() {
        Random random = new Random();
        long randomId = random.nextLong();
        Long paymentId = orderService.getPaymentIdByOrderId(randomId);
        assertNull(paymentId);
    }

    @Test
    public void buyerEmailLowercaseTest() {
        String emailWithUppercase = "EmailWithUppercaseChars@m.ru";
        String emailWithUppercasePart = emailWithUppercase.substring(3, 10);

        Order order = OrderProvider.getBlueOrder();
        order.getBuyer().setEmail(emailWithUppercase);
        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        order = orderService.getOrder(orderId);
        // Проверяем, что email сохранился в lowercase
        assertEquals(emailWithUppercase.toLowerCase(), order.getBuyer().getEmail());

        OrderSearchRequest request = new OrderSearchRequest();
        request.orderIds = Collections.singletonList(orderId);
        request.buyerEmail = emailWithUppercase.toUpperCase();
        PagedOrders orders = orderService.getOrders(request, ClientInfo.SYSTEM);
        // Проверяем, что заказ ищется по email в uppercase
        assertEquals(orders.getItems().size(), 1);

        OrderSearchRequest request2 = new OrderSearchRequest();
        request2.orderIds = Collections.singletonList(orderId);
        request2.buyerEmailSubstring = emailWithUppercasePart.toUpperCase();
        PagedOrders orders2 = orderService.getOrders(request2, ClientInfo.SYSTEM);
        // Проверяем, что заказ ищется по email в uppercase
        assertEquals(orders2.getItems().size(), 1);
    }

    @Test
    public void checkGetMissingBasicOrder() {
        BasicOrder order = orderService.getBasicOrder(-1, ClientInfo.SYSTEM);
        assertNull(order);
    }

    @Test
    public void shouldAddOrderProperty() {
        Order order = createPostOrder();

        OrderProperty orderProperty = OrderPropertyType.EXPERIMENTS.create(order.getId(), "EXPERIMENT");
        orderUpdateService.addOrderProperty(orderProperty);

        order = orderService.getOrder(order.getId());

        assertEquals(order.getProperty(OrderPropertyType.EXPERIMENTS), "EXPERIMENT");
    }

    @Test
    public void freeLiftingTest() {
        Order order = OrderProvider.getBlueOrder();
        order.getDelivery().setLiftPrice(BigDecimal.ZERO);
        order.getDelivery().setBuyerPrice(BigDecimal.ZERO);
        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        Order fetchedOrder = orderService.getOrder(orderId);
        Assertions.assertNull(fetchedOrder.getDelivery().getVat());
    }

    @Test
    public void notFreeLiftingTest() {
        Order order = OrderProvider.getBlueOrder();
        order.getDelivery().setLiftPrice(BigDecimal.ONE);
        order.getDelivery().setBuyerPrice(BigDecimal.ZERO);
        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        Order fetchedOrder = orderService.getOrder(orderId);
        Assertions.assertNotNull(fetchedOrder.getDelivery().getVat());
    }

    private long createGlobalOrder() {
        return orderServiceHelper.createGlobalOrder();
    }

    private Order putTrackIntoOrder(long order) {
        return OrderServiceHelper.putTrackIntoOrder(order, orderUpdateService);
    }

    private Order putTrackIntoOrder(long orderId, long deliveryServiceId) {
        return OrderServiceHelper.putTrackIntoOrder(orderId, deliveryServiceId, orderUpdateService);
    }

    private Order putTrackIntoOrder(long orderId, Track track) {
        return OrderServiceHelper.putTrackIntoOrder(orderId, track, orderUpdateService);
    }

    private Order createPostOrder() {
        return orderServiceHelper.createPostOrder();
    }

    private void createOrderWithNotes(String notes) {
        Order order = OrderProvider.getBlueOrder();
        order.setNotes(notes);
        order.setItems(new ArrayList<>());
        order.setDelivery(buildDelivery());
        order.setCurrency(RUR);
        order.setBuyerCurrency(RUR);
        order.setContext(Context.MARKET);
        orderInsertHelper.insertOrder(order);
    }

    private void checkFoundOrders(String notes, final int size) {
        OrderSearchRequest request = new InnerOrderSearchRequest();
        request.notes = notes;
        Collection<BasicOrder> orders = orderService.getBasicOrders(request, ClientInfo.SYSTEM).getItems();
        assertEquals(size, orders.size());
    }

    @Nonnull
    private Delivery buildDelivery() {
        Delivery delivery = DeliveryProvider.getShopDelivery();
        delivery.setDeliveryServiceId(123L);
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        return delivery;
    }

    private void createOrdersForSearchTest() {
        Order order = OrderProvider.getBlueOrder();
        Order order2 = OrderProvider.getBlueOrder();
        Order order3 = OrderProvider.getBlueOrder();
        Order order4 = OrderProvider.getBlueOrder();
        orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderCreateService.createOrder(order2, ClientInfo.SYSTEM);
        orderCreateService.createOrder(order3, ClientInfo.SYSTEM);
        orderCreateService.createOrder(order4, ClientInfo.SYSTEM);

        orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND);
        orderUpdateService.updateOrderStatus(order2.getId(), OrderStatus.RESERVED);
        orderUpdateService.updateOrderStatus(order3.getId(), OrderStatus.CANCELLED,
                OrderSubstatus.DELIVERY_SERVICE_FAILED);
    }
}
