package ru.yandex.market.api.user.order.builders;

import java.math.BigDecimal;
import java.util.Arrays;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class OrderBuilder extends RandomBuilder<Order> {

    private Order order = new Order();

    @Override
    public OrderBuilder random() {
        order.setId((long) random.getInt());
        order.setShopId((long) random.getInt());
        order.setStatus(random.from(OrderStatus.class));
        order.setSubstatus(random.from(OrderSubstatus.class));
        order.setCreationDate(random.getDate());
        order.setUpdateDate(random.getDate());
        order.setStatusUpdateDate(random.getDate());
        order.setCurrency(Currency.RUR);
        order.setBuyerCurrency(Currency.RUR);
        order.setLabel(random.getString());
        order.setTotal(BigDecimal.valueOf(random.getInt()));
        order.setBuyerTotal(BigDecimal.valueOf(random.getInt()));
        order.setItemsTotal(BigDecimal.valueOf(random.getInt()));
        order.setBuyerItemsTotal(BigDecimal.valueOf(random.getInt()));
        return this;
    }
    public OrderBuilder withId(long id) {
        order.setId(id);
        return this;
    }
    public OrderBuilder withStatus(OrderStatus status) {
        order.setStatus(status);
        return this;
    }

    public OrderBuilder withItems(OrderItem ... items) {
        order.setItems(Arrays.asList(items));
        return this;
    }

    public OrderBuilder withDelivery(Delivery delivery) {
        order.setDelivery(delivery);
        return this;
    }

    public OrderBuilder withErrors(ValidationResult... errors) {
        order.setValidationErrors(Arrays.asList(errors));
        return this;
    }

    public OrderBuilder withWarnings(ValidationResult... errors) {
        order.setValidationWarnings(Arrays.asList(errors));
        return this;
    }

    public OrderBuilder withLabel(String label) {
        order.setLabel(label);
        return this;
    }

    public OrderBuilder withTotal(BigDecimal total) {
        order.setTotal(total);
        return this;
    }

    public OrderBuilder withBuyerTotal(BigDecimal buyerTotal) {
        order.setBuyerTotal(buyerTotal);
        return this;
    }

    public OrderBuilder withItemsTotal(BigDecimal itemsTotal) {
        order.setItemsTotal(itemsTotal);
        return this;
    }

    public OrderBuilder withBuyerItemsTotal(BigDecimal buyerItemsTotal) {
        order.setBuyerItemsTotal(buyerItemsTotal);
        return this;
    }

    public OrderBuilder withPayment(Payment payment) {
        order.setPayment(payment);

        return this;
    }


    @Override
    public Order build() {
        return order;
    }
}
