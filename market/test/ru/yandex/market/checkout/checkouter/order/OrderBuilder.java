package ru.yandex.market.checkout.checkouter.order;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.pushapi.client.entity.BaseBuilder;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryBuilder;

import java.math.BigDecimal;
import java.util.Date;

import static java.util.Arrays.asList;
import static ru.yandex.market.checkout.pushapi.client.entity.BuilderUtil.createDate;

/**
 * @author msavelyev
 */
public class OrderBuilder extends BaseBuilder<Order, OrderBuilder> {

    public OrderBuilder() {
        super(new Order());

        object.setId(1234l);
        object.setStatus(OrderStatus.RESERVED);
        object.setCreationDate(createDate("2013-07-06 15:30:40"));
        object.setCurrency(Currency.RUR);
        object.setItemsTotal(new BigDecimal("10.75"));
        object.setTotal(new BigDecimal("11.43"));
        object.setPaymentType(PaymentType.PREPAID);
        object.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        object.setFake(false);
        object.setContext(Context.MARKET);
        object.setItems(asList(new OrderItemBuilder().build()));
        object.setDelivery(new DeliveryBuilder().build());
        object.setBuyer(new BuyerBuilder().build());
    }

    public OrderBuilder withId(Long id) {
        return withField("id", id);
    }

    public OrderBuilder withStatus(OrderStatus status) {
        return withField("status", status);
    }

    public OrderBuilder withCreationDate(Date creationDate) {
        return withField("creationDate", creationDate);
    }

    public OrderBuilder withCurrency(Currency currency) {
        return withField("currency", currency);
    }

    public OrderBuilder withItemsTotal(BigDecimal itemsTotal) {
        return withField("itemsTotal", itemsTotal);
    }

    public OrderBuilder withTotal(BigDecimal total) {
        return withField("total", total);
    }

    public OrderBuilder withPaymentMethod(PaymentMethod paymentMethod) {
        return withField("paymentMethod", paymentMethod)
            .withField("paymentType", paymentMethod == null ? null :
                paymentMethod.getPaymentType());
    }

    public OrderBuilder withFake(Boolean fake) {
        return withField("fake", fake);
    }

    public OrderBuilder withItems(OrderItemBuilder... items) {
        return withField("items", items);
    }

    public OrderBuilder withDelivery(DeliveryBuilder delivery) {
        return withField("delivery", delivery);
    }

    public OrderBuilder withBuyer(BuyerBuilder buyer) {
        return withField("buyer", buyer);
    }

    public OrderBuilder withRgb(Color rgb)  {
        return withField("rgb", rgb);
    }

    public OrderBuilder withFulfilment(Boolean fulfilment)  {
        return withField("fulfilment", fulfilment);
    }

}
