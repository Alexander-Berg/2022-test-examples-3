package ru.yandex.market.checkout.pushapi.providers;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Collections;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

public abstract class OrderShipmentStatusProvider {

    private OrderShipmentStatusProvider() {
        throw new UnsupportedOperationException();
    }

    public static Order buildOrderShipmentStatus() {
        Order order = new Order();
        order.setId(123L);
        order.setStatus(OrderStatus.PROCESSING);
        order.setCreationDate(Date.from(LocalDateTime.of(2014, Month.OCTOBER, 7, 16, 12, 58)
                .atZone(ZoneId.of("Europe/Moscow"))
                .toInstant()));
        order.setCurrency(Currency.RUR);
        order.setItemsTotal(new BigDecimal("2190"));
        order.setTotal(new BigDecimal("2190"));
        order.setPaymentType(PaymentType.PREPAID);
        order.setPaymentMethod(PaymentMethod.YANDEX);
        order.setFake(false);

        order.addItem(buildOrderItem());
        order.setDelivery(buildDelivery());
        order.setBuyer(BuyerProvider.getBuyer());
        return order;
    }

    private static OrderItem buildOrderItem() {
        OrderItem orderItem = new OrderItem();
        orderItem.setFeedId(200305173L);
        orderItem.setOfferId("4");
        orderItem.setFeedCategoryId("{{feedcategory}}");
        orderItem.setOfferName("{{offername}}");
        orderItem.setCount(1);
        orderItem.setQuantity(BigDecimal.ONE);
        orderItem.setPrice(new BigDecimal("100"));
        orderItem.setQuantPrice(new BigDecimal("100"));
        return orderItem;
    }

    private static Delivery buildDelivery() {
        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.PICKUP);
        delivery.setPrice(BigDecimal.ZERO);
        delivery.setServiceName("Почта России");
        delivery.setRegionId(2L);
        delivery.setDeliveryDates(buildDeliveryDates());
        delivery.setParcels(Collections.singletonList(ParcelProvider.buildShipment()));
        delivery.setOutletId(567633L);
        return delivery;
    }

    private static DeliveryDates buildDeliveryDates() {
        DeliveryDates deliveryDates = new DeliveryDates();
        deliveryDates.setFromDate(Date.from(LocalDate.of(2015, Month.AUGUST, 25).atStartOfDay(ZoneId.of("Europe" +
                "/Moscow")).toInstant()));
        deliveryDates.setToDate(Date.from(LocalDate.of(2015, Month.SEPTEMBER, 25).atStartOfDay(ZoneId.of("Europe" +
                "/Moscow")).toInstant()));
        return deliveryDates;
    }

}
