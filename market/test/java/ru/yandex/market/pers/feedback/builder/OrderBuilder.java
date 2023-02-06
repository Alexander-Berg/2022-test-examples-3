package ru.yandex.market.pers.feedback.builder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.outlet.OutletPurpose;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;

public class OrderBuilder {

    private static final List<Consumer<Order>> DEFAULT_CONSUMERS = List.of(
        o -> {
            o.setId(1234L);
            o.setPaymentType(PaymentType.PREPAID);
            o.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
            o.setStatusUpdateDate(new Date());
            o.setDelivery(new Delivery());
            o.getDelivery().setType(DeliveryType.DELIVERY);
            o.getDelivery().setOutletPurpose(OutletPurpose.UNKNOWN);
            o.getDelivery().setDeliveryDates(new DeliveryDates(new Date(), new Date()));
        }
    );

    private List<Consumer<Order>> consumers = new ArrayList<>(DEFAULT_CONSUMERS);

    public static OrderBuilder builder() {
        return new OrderBuilder();
    }

    public OrderBuilder withId(long id) {
        return withConsumer(o -> o.setId(id));
    }

    public OrderBuilder withPaymentType(PaymentType paymentType) {
        return withConsumer(o -> o.setPaymentType(paymentType));
    }

    public OrderBuilder withPaymentMethod(PaymentMethod paymentMethod) {
        return withConsumer(o -> o.setPaymentMethod(paymentMethod));
    }

    public OrderBuilder withDeliveryType(DeliveryType deliveryType) {
        return withConsumer(o -> o.getDelivery().setType(deliveryType));
    }

    public OrderBuilder withOutletPurpose(OutletPurpose outletPurpose) {
        return withConsumer(o -> o.getDelivery().setOutletPurpose(outletPurpose));
    }

    public OrderBuilder withDeliveryDates(Date fromDate, Date toDate) {
        return withDeliveryDates(new DeliveryDates(fromDate, toDate));
    }

    public OrderBuilder withDeliveryDates(DeliveryDates dates) {
        return withConsumer(o -> o.getDelivery().setDeliveryDates(dates));
    }

    public OrderBuilder withStatusUpdateDate(Date date) {
        return withConsumer(o -> o.setStatusUpdateDate(date));
    }

    public OrderBuilder withConsumer(Consumer<Order> consumer) {
        this.consumers.add(consumer);
        return this;
    }

    public Order build() {
        Order order = new Order();
        for (Consumer<Order> consumer : consumers) {
            consumer.accept(order);
        }
        return order;
    }
}
