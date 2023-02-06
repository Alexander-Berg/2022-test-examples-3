package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.api.response.orders.OrderItem;
import ru.yandex.market.fulfillment.wrap.marschroute.api.response.orders.OrderResponseData;
import ru.yandex.market.fulfillment.wrap.marschroute.api.response.orders.OrderResponseDataOrder;
import ru.yandex.market.fulfillment.wrap.marschroute.configuration.ConversionConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.model.MarschrouteLocation;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteDeliveryInterval;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschroutePaymentType;
import ru.yandex.market.fulfillment.wrap.marschroute.model.converter.util.ItemIdentifierUtil;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.tracking.TrackingStatus;
import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDate;
import ru.yandex.market.fulfillment.wrap.marschroute.service.SystemPropertyService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.Order;
import ru.yandex.market.logistic.api.model.fulfillment.PaymentType;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ConversionConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(OrderResponseDataConverter.class)
@MockBean(GeoInformationProvider.class)
@MockBean(SystemPropertyService.class)
class OrderResponseDataConverterTest extends BaseIntegrationTest {

    @Autowired
    private OrderResponseDataConverter orderResponseDataConverter;

    @Test
    void convert() {
        OrderResponseData orderResponseData = getOrderResponseData();
        OrderResponseDataOrder order = orderResponseData.getOrder();
        Order.OrderBuilder convertedOrderBuilder = orderResponseDataConverter.convert(orderResponseData);
        Order convertedOrder = convertedOrderBuilder.build();
        assertEquals(order.getId(), convertedOrder.getOrderId().getYandexId(), "YandexId must be equal to Id");
        assertEquals(order.getOrderId(), convertedOrder.getOrderId().getPartnerId(), "PartnerId must be equal to orderId");
        assertEquals(BigDecimal.valueOf(order.getDeliverySum()), convertedOrder.getDeliveryCost(), "deliveryCost must be equal to deliverySum");
        assertEquals(PaymentType.CASH, convertedOrder.getPaymentMethod(), "paymentMethod must be equal to CASH");
        assertEquals(order.getComment(), convertedOrder.getComment(), "comment must be equal to comment");
        assertEquals("10:00:00+03:00/18:00:00+03:00", convertedOrder.getDeliveryInterval().getFormattedTimeInterval(), "deliveryInterval must have valid format");
        assertNull(convertedOrder.getLocationTo(), "locationTo must be equal to null");
        assertItems(orderResponseData.getItems(), convertedOrder.getItems());
    }

    private void assertItems(List<OrderItem> orderItems, List<Item> items) {
        OrderItem item = orderItems.get(0);
        Item convertedItem = items.get(0);
        assertEquals(item.getName(), convertedItem.getName(), "name must be equal to name");
        assertEquals(BigDecimal.valueOf(item.getPrice()), convertedItem.getPrice(), "price must be equal to price");
        assertEquals(item.getQuantity(), convertedItem.getCount(), "quantity must be equal to count");
        assertEquals(ItemIdentifierUtil.toUnitId(item.getItemId()), convertedItem.getUnitId(), "unitId must match itemId");
    }

    @Test
    void convertNullObject() {
        Order.OrderBuilder convertedOrderBuilder = orderResponseDataConverter.convert(null);
        assertNull(convertedOrderBuilder, "converted object must be equal to null");
    }

    /**
     * Проверяет, что при конвертации объекта с пустыми полями не падаем с NullPointerException
     */
    @Test
    void convertEmptyObject() {
        OrderResponseData orderResponseData = new OrderResponseData();
        orderResponseDataConverter.convert(orderResponseData);
    }

    private OrderResponseData getOrderResponseData() {
        OrderResponseData orderResponseData = new OrderResponseData();

        OrderResponseDataOrder order = new OrderResponseDataOrder();
        order.setId("id");
        order.setOrderId("orderId");

        order.setCreateDate(getDate());
        order.setShipDate(getDate());
        order.setStatusDate(getDate());

        order.setSendDate(MarschrouteDate.create(getLocalDate()));
        order.setStatus(TrackingStatus.AWAITING_GOODS);
        order.setOrderSum(1000);
        order.setDeliverySum(1000);
        order.setShipDate(getDate());

        MarschrouteLocation location = new MarschrouteLocation();
        location.setBuilding1("house");
        location.setBuilding2("building/housing");
        location.setIndex("119017");
        location.setStreet("Street");
        location.setRoom("room");
        order.setLocation(location);

        order.setDeliveryCode("code");
        order.setPaymentType(MarschroutePaymentType.CASH);
        order.setWeight(1000);
        order.setType(1);
        order.setComment("comment");
        order.setDeliveryInterval(MarschrouteDeliveryInterval.WORKING_TIME);

        order.setTimeFrom(LocalTime.parse("10:00:35"));
        order.setTimeTo(LocalTime.parse("18:00:34"));
        orderResponseData.setOrder(order);

        orderResponseData.setItems(getItems());

        return orderResponseData;
    }

    private List<OrderItem> getItems() {
        LinkedList<OrderItem> orderItems = new LinkedList<>();
        OrderItem orderItem = new OrderItem();
        orderItem.setItemId("10122.10264538");
        orderItem.setDateReturn(getLocalDateTime());
        orderItem.setName("iPhone");
        orderItem.setQuantity(1000);
        orderItem.setPrice(1000);
        orderItems.add(orderItem);
        return orderItems;
    }

    private LocalDateTime getLocalDateTime() {
        return LocalDateTime.of(2018, 7, 7, 0, 0);
    }

    private LocalDate getLocalDate() {
        return LocalDate.of(2018, 5, 5);
    }

    private LocalDateTime getDate() {
        return LocalDateTime.of(2018, 5, 3, 0, 0);
    }
}
