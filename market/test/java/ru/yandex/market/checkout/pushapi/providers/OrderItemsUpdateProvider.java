package ru.yandex.market.checkout.pushapi.providers;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.common.report.model.FeedOfferId;

public abstract class OrderItemsUpdateProvider {
    private OrderItemsUpdateProvider() {
        throw new UnsupportedOperationException();
    }

    public static Order buildOrderItemsUpdate() {
        ru.yandex.market.checkout.checkouter.order.Order order = new ru.yandex.market.checkout.checkouter.order.Order();
        order.setId(123L);
        order.setStatus(OrderStatus.PROCESSING);
        order.setCreationDate(Date.from(
                LocalDateTime.of(2014, Month.OCTOBER, 7, 16, 12, 58)
                        .atZone(ZoneId.of("Europe/Moscow"))
                        .toInstant()));
        order.setCurrency(Currency.RUR);
        order.setItemsTotal(new BigDecimal("2190"));
        order.setTotal(new BigDecimal("2190"));
        order.setPaymentType(PaymentType.PREPAID);
        order.setPaymentMethod(PaymentMethod.YANDEX);
        order.setFake(false);

        order.setItems(Arrays.asList(
                buildItem("4", 5, 234L, "100"),
                buildItem("45", 4, 235L, "200")
        ));
        order.setDelivery(buildDelivery());
        Buyer buyer = buildBuyer();
        order.setBuyer(buyer);
        return order;
    }

    private static Delivery buildDelivery() {
        Delivery delivery = new Delivery(DeliveryType.PICKUP, BigDecimal.ZERO, "ПОчта россии",
                new DeliveryDates(
                        java.util.Date.from(LocalDate.of(2015, Month.AUGUST, 25).atStartOfDay(ZoneId.of("Europe/Moscow")).toInstant()),
                        java.util.Date.from(LocalDate.of(2015, Month.AUGUST, 25).atStartOfDay(ZoneId.of("Europe/Moscow")).toInstant())
                ), 2L, 567633L);
        delivery.setShipment(ParcelProvider.buildShipment());
        return delivery;
    }

    private static OrderItem buildItem(String offerId, int count, long id, String price) {
        OrderItem first = new OrderItem(
                new FeedOfferId(offerId, 200305173L), null, null, "{{feedcategory}}", "{{offername}}", count
        );
        first.setId(id);
        first.setPrice(new BigDecimal(price));
        first.setQuantPrice(new BigDecimal(price));
        return first;
    }

    private static Buyer buildBuyer() {
        Buyer buyer = new Buyer();
        buyer.setLastName("последнееимя");
        buyer.setFirstName("первоеимя");
        buyer.setMiddleName("среднееимя");
        buyer.setPhone("+77777777777");
        buyer.setEmail("ymail@y.mail");
        buyer.setUid(54321L);
        return buyer;
    }
}
