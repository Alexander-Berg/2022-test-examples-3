package ru.yandex.market.checkout.checkouter.storage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.FreeDeliveryInfo;
import ru.yandex.market.checkout.checkouter.delivery.outlet.OutletPurpose;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.PartnerPriceMarkup;
import ru.yandex.market.checkout.checkouter.order.PartnerPriceMarkupCoefficient;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author apershukov
 */
public class OrderReadingDaoTest extends AbstractWebTestBase {

    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private OrderSequences orderSequences;
    @Autowired
    private OrderReadingDao orderReadingDao;
    @Autowired
    private OrderCreateHelper orderCreateHelper;

    /**
     * Заказ без посылок но с треками при поднятии получает фейковую посылку
     */
    @Test
    public void testGetOrderWithNoShipmentButWithTracks() {
        Order order = orderServiceHelper.createOrder();
        transactionTemplate.execute(ts -> {
            insertOldFashionedTrack(order, "iddqd", 123);
            return null;
        });

        Order order2 = orderReadingDao.getOrder(order.getId(), ClientInfo.SYSTEM)
                .orElseThrow(() -> new OrderNotFoundException(order.getId()));

        Delivery delivery = order2.getDelivery();
        assertThat(delivery.getParcels(), hasSize(1));

        Parcel shipment = delivery.getParcels().get(0);
        assertNull(shipment.getId());
        assertThat(shipment.getTracks(), hasSize(1));
        assertEquals("iddqd", shipment.getTracks().get(0).getTrackCode());
    }

    @Test
    public void testCountOrdersWithEmptyPicUrlOnItem() {
        final LocalDateTime dateFrom = LocalDateTime.now();
        assertEquals(0, orderReadingDao.countOrdersWithEmptyPicUrlOnItem(Color.ALIVE_COLORS, dateFrom));
        assertEquals(0, orderReadingDao.countOrdersWithEmptyPicUrlOnItem(Color.ALIVE_COLORS, dateFrom));

        orderServiceHelper.createPostOrder(ord -> ord.setRgb(Color.BLUE));

        assertEquals(1, orderReadingDao.countOrdersWithEmptyPicUrlOnItem(Color.ALIVE_COLORS, dateFrom));

        orderServiceHelper.createPostOrder(
                order -> order.getItems().forEach(item -> item.setPictureURL("some_url")));

        assertEquals(1, orderReadingDao.countOrdersWithEmptyPicUrlOnItem(Color.ALIVE_COLORS, dateFrom));

        final Order order = orderServiceHelper.createPostOrder();
        assertEquals(2, orderReadingDao.countOrdersWithEmptyPicUrlOnItem(Color.ALIVE_COLORS, dateFrom));

        // Равенство даты в заказе и даты фильтрации
        final LocalDateTime createdAt = LocalDateTime.ofInstant(
                order.getCreationDate().toInstant(), ZoneId.systemDefault());
        assertEquals(1, orderReadingDao.countOrdersWithEmptyPicUrlOnItem(Color.ALIVE_COLORS, createdAt));

        // Дата фильтрации больше даты заказа
        final LocalDateTime tomorrow = dateFrom.plus(1L, ChronoUnit.DAYS);
        assertEquals(0, orderReadingDao.countOrdersWithEmptyPicUrlOnItem(Color.ALIVE_COLORS, tomorrow));
    }

    @Test
    public void testGetOrdersWithEmptyPicUrlOnItem() {
        final LocalDateTime dateFrom = LocalDateTime.now();
        assertTrue(orderReadingDao.getOrdersWithEmptyPicUrlOnItem(singleton(Color.BLUE), dateFrom).isEmpty());
        assertTrue(orderReadingDao.getOrdersWithEmptyPicUrlOnItem(singleton(Color.GREEN), dateFrom).isEmpty());

        Order blueOrder = orderServiceHelper.createPostOrder(ord -> ord.setRgb(Color.BLUE));

        Collection<Long> orderIds = orderReadingDao.getOrdersWithEmptyPicUrlOnItem(singleton(Color.BLUE), dateFrom);
        assertEquals(1, orderIds.size());
        assertEquals(blueOrder.getId(), orderIds.iterator().next());
        assertTrue(orderReadingDao.getOrdersWithEmptyPicUrlOnItem(singleton(Color.GREEN), dateFrom).isEmpty());

        orderServiceHelper.createPostOrder(
                order -> order.getItems().forEach(item -> item.setPictureURL("some_url")));

        orderIds = orderReadingDao.getOrdersWithEmptyPicUrlOnItem(singleton(Color.BLUE), dateFrom);
        assertEquals(1, orderIds.size());
        assertEquals(blueOrder.getId(), orderIds.iterator().next());
        assertTrue(orderReadingDao.getOrdersWithEmptyPicUrlOnItem(singleton(Color.GREEN), dateFrom).isEmpty());

        Order greenOrder = orderServiceHelper.createPostOrder();

        orderIds = orderReadingDao.getOrdersWithEmptyPicUrlOnItem(singleton(Color.BLUE), dateFrom);
        assertEquals(2, orderIds.size());
        assertEquals(blueOrder.getId(), orderIds.iterator().next());

        orderIds = orderReadingDao.getOrdersWithEmptyPicUrlOnItem(singleton(Color.GREEN), dateFrom);
        assertEquals(0, orderIds.size());

        // Равенство даты в заказе и даты фильтрации
        final LocalDateTime createdAt = LocalDateTime.ofInstant(
                greenOrder.getCreationDate().toInstant(), ZoneId.systemDefault());
        orderIds = orderReadingDao.getOrdersWithEmptyPicUrlOnItem(singleton(Color.BLUE), createdAt);
        assertEquals(1, orderIds.size());
        assertEquals(greenOrder.getId(), orderIds.iterator().next());

        // Дата фильтрации больше даты заказа
        final LocalDateTime tomorrow = dateFrom.plus(1L, ChronoUnit.DAYS);
        orderIds = orderReadingDao.getOrdersWithEmptyPicUrlOnItem(singleton(Color.BLUE), tomorrow);
        assertEquals(0, orderIds.size());

        orderIds = orderReadingDao.getOrdersWithEmptyPicUrlOnItem(singleton(Color.GREEN), tomorrow);
        assertEquals(0, orderIds.size());
    }

    @Test
    public void testGetOrdersWithModifiedSubStatus() {
        Order order = orderServiceHelper.createOrder();

        List<Order> orders = orderReadingDao.getOrders(Collections.singletonList(order.getId()),
                new ClientInfo(ClientRole.SHOP, order.getShopId()), null, null);

        assertThat(orders, hasSize(1));
        assertEquals(order.getStatus(), orders.get(0).getStatus());
        assertEquals(order.getSubstatus(), orders.get(0).getSubstatus());

        order.setStatus(OrderStatus.CANCELLED);
        order.setSubstatus(OrderSubstatus.USER_FRAUD);
        orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.CANCELLED, OrderSubstatus.USER_FRAUD);

        orders = orderReadingDao.getOrders(Collections.singletonList(order.getId()),
                ClientInfo.SYSTEM, null, null);

        assertThat(orders, hasSize(1));
        assertEquals(OrderStatus.CANCELLED, orders.get(0).getStatus());
        assertEquals(OrderSubstatus.USER_FRAUD, orders.get(0).getSubstatus());

        orders = orderReadingDao.getOrders(Collections.singletonList(order.getId()),
                new ClientInfo(ClientRole.SHOP, order.getShopId()), null, null);

        assertThat(orders, hasSize(1));
        assertEquals(OrderStatus.CANCELLED, orders.get(0).getStatus());
        assertEquals(OrderSubstatus.USER_CHANGED_MIND, orders.get(0).getSubstatus());
    }

    /**
     * Если у заказа уже есть единственная посылка, но трек к ней не привязан, трек привязывается к посылке
     */
    @Test
    public void testGetOrderWithFreeDeliveryInfo() {
        FreeDeliveryInfo freeDeliveryInfo = new FreeDeliveryInfo(
                new BigDecimal("1.23"),
                new BigDecimal("4.61")
        );

        final Order order =
                orderServiceHelper.createPostOrder(o -> o.getDelivery().setFreeDeliveryInfo(freeDeliveryInfo));

        Order order2 = orderReadingDao.getOrder(order.getId(), ClientInfo.SYSTEM)
                .orElseThrow(() -> new OrderNotFoundException(order.getId()));

        Delivery readDelivery = order2.getDelivery();
        assertThat(readDelivery.getFreeDeliveryInfo(), is(freeDeliveryInfo));
    }

    private void insertOldFashionedTrack(Order order, String trackCode, long deliveryServiceId) {
        transactionTemplate.execute(ts -> {
            masterJdbcTemplate.update(
                    "INSERT INTO DELIVERY_TRACK (ID, ORDER_ID, DELIVERY_ID, TRACK_CODE, DELIVERY_SERVICE_ID)\n" +
                            "VALUES (?, ?, ?, ?, ?)",
                    orderSequences.getNextTrackId(), order.getId(), order.getInternalDeliveryId(), trackCode,
                    deliveryServiceId
            );
            return null;
        });
    }

    @Test
    public void testGetOrdersWithFeedPrice() {
        BigDecimal feedPrice = new BigDecimal("123.45");
        Order order = orderServiceHelper.createPostOrder(
                o -> o.getItems().iterator().next().getPrices().setFeedPrice(feedPrice));

        Order savedOrder = orderReadingDao.getOrder(order.getId(), ClientInfo.SYSTEM)
                .orElseThrow(() -> new OrderNotFoundException(order.getId()));

        assertThat(savedOrder, notNullValue());
        assertThat(savedOrder.getItems().iterator().next().getPrices().getFeedPrice(), comparesEqualTo(feedPrice));
    }

    @Test
    public void testGetOrdersWithFeedGroupIdHash() {
        String feedGroupIdHash = "feedGroupIdHash";
        Order order = orderServiceHelper.createPostOrder(
                o -> o.getItems().iterator().next().setFeedGroupIdHash(feedGroupIdHash));

        Order savedOrder = orderReadingDao.getOrder(order.getId(), ClientInfo.SYSTEM)
                .orElseThrow(() -> new OrderNotFoundException(order.getId()));

        assertThat(savedOrder, notNullValue());
        assertThat(savedOrder.getItems().iterator().next().getFeedGroupIdHash(), equalTo(feedGroupIdHash));
    }

    @Test
    public void testGetOrdersWithOutletStoragePeriod() {
        //given:
        Integer outletStoragePeriod = 14;
        Order order = orderServiceHelper.createPostOrder(
                o -> o.getDelivery().setOutletStoragePeriod(outletStoragePeriod));

        //when:
        Order savedOrder = orderReadingDao.getOrder(order.getId(), ClientInfo.SYSTEM)
                .orElseThrow(() -> new OrderNotFoundException(order.getId()));

        //then:
        assertThat(savedOrder, notNullValue());
        assertThat(savedOrder.getDelivery(), notNullValue());
        assertThat(savedOrder.getDelivery().getOutletStoragePeriod(), equalTo(outletStoragePeriod));
    }

    @Test
    public void testGetOrdersWithOutletStorageLimitDate() {
        //given:
        LocalDate outletStorageLimitDate = LocalDate.now();
        Order order = orderServiceHelper.createPostOrder(
                o -> o.getDelivery().setOutletStorageLimitDate(outletStorageLimitDate));

        //when:
        Order savedOrder = orderReadingDao.getOrder(order.getId(), ClientInfo.SYSTEM)
                .orElseThrow(() -> new OrderNotFoundException(order.getId()));
        //then:
        assertThat(savedOrder, notNullValue());
        assertThat(savedOrder.getDelivery(), notNullValue());
        assertThat(savedOrder.getDelivery().getOutletStorageLimitDate(), equalTo(outletStorageLimitDate));
    }

    @Test
    public void testGetOrdersWithOutletPurpose() {
        //given:
        OutletPurpose purpose = OutletPurpose.PICKUP;
        Order order = orderServiceHelper.createPostOrder(
                o -> o.getDelivery().setOutletPurpose(purpose)
        );

        //when:
        Order savedOrder = orderReadingDao.getOrder(order.getId(), ClientInfo.SYSTEM)
                .orElseThrow(() -> new OrderNotFoundException(order.getId()));
        //then:
        assertThat(savedOrder, notNullValue());
        assertThat(savedOrder.getDelivery(), notNullValue());
        assertThat(savedOrder.getDelivery().getOutletPurpose(), equalTo(purpose));
    }

    @Test
    public void testOrderSavesAndRetrievesPartnerInfo() {
        Order order = orderServiceHelper.createOrder();
        Collection<OrderItem> items1 = order.getItems();
        Order order2 = orderReadingDao.getOrder(order.getId(), ClientInfo.SYSTEM)
                .orElseThrow(() -> new OrderNotFoundException(order.getId()));

        Collection<OrderItem> items2 = order2.getItems();
        assertEquals(items1.size(), items2.size());
        OrderItem item1 = items1.stream().findFirst().orElseThrow(RuntimeException::new);
        OrderItem item2 = items2.stream().findFirst().orElseThrow(RuntimeException::new);
        assertNotNull(item1.getPrices().getPartnerPrice());
        assertEquals(item1.getPrices().getPartnerPrice(), item2.getPrices().getPartnerPrice());
        final PartnerPriceMarkup item1partnerPriceMarkup = item1.getPrices().getPartnerPriceMarkup();
        assertNotNull(item1partnerPriceMarkup);
        assertFalse(item1partnerPriceMarkup.getCoefficients().isEmpty());
        List<PartnerPriceMarkupCoefficient> markups1 = item1partnerPriceMarkup.getCoefficients();
        List<PartnerPriceMarkupCoefficient> markups2 = item2.getPrices().getPartnerPriceMarkup().getCoefficients();
        assertTrue(markups1.containsAll(markups2));
        assertTrue(markups2.containsAll(markups1));
    }

    @Test
    public void shouldFindAllMultiOrderParts() {
        MultiOrder fiveMultiOrder = orderCreateHelper.createMultiOrder(new Parameters(3));
        List<Long> orderIds =
                orderReadingDao.findAllMultiOrderIdsByAnyParticular(fiveMultiOrder.getOrders().get(0).getId());
        assertEquals(3, orderIds.size());
    }

    @Test
    public void singleOrderParts() {
        Order order = orderServiceHelper.createOrder();
        List<Long> orderIds =
                orderReadingDao.findAllMultiOrderIdsByAnyParticular(order.getId());
        assertEquals(0, orderIds.size());
    }
}
