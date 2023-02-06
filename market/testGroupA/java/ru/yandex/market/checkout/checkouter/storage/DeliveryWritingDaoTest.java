package ru.yandex.market.checkout.checkouter.storage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.FreeDeliveryInfo;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.OrderInsertHelper;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryWritingDaoTest extends AbstractServicesTestBase {

    private static final long ORDER_ID   = Integer.MAX_VALUE + 1L;

    @Autowired
    private DeliveryWritingDao deliveryWritingDao;
    @Autowired
    private OrderInsertHelper orderInsertHelper;
    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private OrderReadingDao orderReadingDao;

    @BeforeEach
    void setUp() {
        final Order order = OrderProvider.getBlueOrder();
        orderInsertHelper.insertOrder(ORDER_ID, order);
    }

    @Test
    void shouldWriteFreeDeliveryInfo() {
        JdbcTemplate jdbcTemplate = getRandomWritableJdbcTemplate();
        Delivery delivery = DeliveryProvider.getShopDelivery();
        delivery.setFreeDeliveryInfo(new FreeDeliveryInfo(
                        new BigDecimal("1.23"),
                        new BigDecimal("4.61")
                )
        );
        Long deliverId = transactionTemplate.execute(ts ->
                deliveryWritingDao.insertDelivery(delivery, ORDER_ID)
        );
        assertNotNull(deliverId);

        Map<String, Object> result = jdbcTemplate.queryForMap(
                "select FREE_DELIVERY_THRESHOLD, FREE_DELIVERY_REMAINING " +
                        " from ORDER_DELIVERY where id = ?",
                deliverId);

        assertEquals(new BigDecimal("1.23"), result.get("free_delivery_threshold"));
        assertEquals(new BigDecimal("4.61"), result.get("free_delivery_remaining"));
    }

    @Test
    void shouldWriteEmptyFreeDeliveryInfo() {
        JdbcTemplate jdbcTemplate = getRandomWritableJdbcTemplate();
        Delivery delivery = DeliveryProvider.getShopDelivery();
        Long deliverId = transactionTemplate.execute(ts ->
                deliveryWritingDao.insertDelivery(delivery, ORDER_ID)
        );
        assertNotNull(deliverId);

        Map<String, Object> result = jdbcTemplate.queryForMap(
                "select FREE_DELIVERY_THRESHOLD, FREE_DELIVERY_REMAINING " +
                        " from ORDER_DELIVERY where id = ?",
                deliverId);

        assertNull(result.get("free_delivery_threshold"));
        assertNull(result.get("free_delivery_remaining"));
    }

    @Test
    @SuppressWarnings("checkstyle:HiddenField")
    void shouldNotCreateRecordsWithZeroInsteadNull() {
        Order rawOrder = orderServiceHelper.prepareOrder(Color.BLUE);
        rawOrder.setFulfilment(true);
        rawOrder.setDelivery(DeliveryProvider.yandexDelivery().build());
        rawOrder.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        rawOrder.getDelivery().setBuyerAddress(null);
        final Order order = orderServiceHelper.saveOrder(rawOrder);
        final long orderId = order.getId();
        final Long firstDeliveryId = order.getInternalDeliveryId();

        assertEquals(OrderStatus.PROCESSING, order.getStatus());
        assertNotNull(firstDeliveryId);
        assertNotNull(orderReadingDao.getOrderShopAddressId(orderId));
        assertNull(orderReadingDao.getOrderBuyerAddressId(orderId));
        assertNull(orderReadingDao.getPostAddressId(orderId));
        checkBuyerAddressIdInDB(orderId, 1);

        final Long secondDeliveryId = transactionTemplate.execute(ts -> deliveryWritingDao.bindOrderDelivery(order));
        assertNotNull(secondDeliveryId);
        assertNotEquals(secondDeliveryId, firstDeliveryId);
        assertNotNull(orderReadingDao.getOrderShopAddressId(orderId));
        assertNull(orderReadingDao.getOrderBuyerAddressId(orderId));
        assertNull(orderReadingDao.getPostAddressId(orderId));
        checkBuyerAddressIdInDB(orderId, 2);
    }

    private void checkBuyerAddressIdInDB(final long orderId, final int expectedCount) {
        List<Map<String, Object>> rows = masterJdbcTemplate.queryForList(
                "select * from order_delivery where order_id = ?", orderId);
        assertNotNull(rows);
        assertEquals(expectedCount, rows.size());
        assertTrue(rows.stream()
                .flatMap(m -> m.entrySet().stream())
                .filter(e -> e.getKey().equals("buyer_address_id"))
                .allMatch(e -> Objects.isNull(e.getValue())));
    }

    @Test
    void shouldWriteOutletStoragePeriod() {
        //given:
        Integer outletStoragePeriod = 13;
        JdbcTemplate jdbcTemplate = getRandomWritableJdbcTemplate();
        Delivery delivery = DeliveryProvider.getShopDelivery();
        delivery.setOutletStoragePeriod(outletStoragePeriod);

        //when:
        Long deliveryId = transactionTemplate.execute(ts ->
                deliveryWritingDao.insertDelivery(delivery, ORDER_ID)
        );
        assertNotNull(deliveryId);

        //then:
        Integer fetchedOutletStoragePeriod = jdbcTemplate.queryForObject(
                "select OUTLET_STORAGE_PERIOD " +
                        " from ORDER_DELIVERY where id = ?",
                Integer.class,
                deliveryId);
        assertEquals(outletStoragePeriod, fetchedOutletStoragePeriod);
    }

    @Test
    void shouldWriteOutletStorageLimitDate() {
        //given:
        LocalDate outletStorageLimitDate = LocalDate.now();
        JdbcTemplate jdbcTemplate = getRandomWritableJdbcTemplate();
        Delivery delivery = DeliveryProvider.getShopDelivery();
        delivery.setOutletStorageLimitDate(outletStorageLimitDate);

        //when:
        Long deliveryId = transactionTemplate.execute(ts ->
                deliveryWritingDao.insertDelivery(delivery, ORDER_ID)
        );
        assertNotNull(deliveryId);

        //then:
        LocalDate fetchedOutletStoragePeriod = jdbcTemplate.queryForObject(
                "select OUTLET_STORAGE_LIMIT_DATE " +
                        " from ORDER_DELIVERY where id = ?",
                LocalDate.class,
                deliveryId);
        assertEquals(outletStorageLimitDate, fetchedOutletStoragePeriod);
    }

    @Test
    void shouldSaveAndReadDeliveryFeatures() {
        Delivery delivery = DeliveryProvider.getShopDelivery();
        Set<DeliveryFeature> expectedFeatures = Set.of(DeliveryFeature.ON_DEMAND, DeliveryFeature.ON_DEMAND_YALAVKA);
        delivery.setFeatures(expectedFeatures);

        Long deliveryId = transactionTemplate.execute(ts -> {
            long id = deliveryWritingDao.insertDelivery(delivery, ORDER_ID);
            // привязываем к заказу новую delivery
            masterJdbcTemplate.update("UPDATE orders SET delivery_id = ? WHERE id = ?", id, ORDER_ID);
            return id;
        });

        Order modifiedOrder = orderService.getOrder(ORDER_ID);
        assertEquals(expectedFeatures, modifiedOrder.getDelivery().getFeatures());
    }

    @Test
    void shouldSaveMarketBrandedDelivery() {
        Delivery delivery = DeliveryProvider.getShopDelivery();
        delivery.setMarketBranded(true);

        transactionTemplate.execute(ts -> {
            long id = deliveryWritingDao.insertDelivery(delivery, ORDER_ID);
            // привязываем к заказу новую delivery
            masterJdbcTemplate.update("UPDATE orders SET delivery_id = ? WHERE id = ?", id, ORDER_ID);
            return id;
        });
        Order modifiedOrder = orderService.getOrder(ORDER_ID);
        assertTrue(modifiedOrder.getDelivery().isMarketBranded());
    }
}
