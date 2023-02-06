package ru.yandex.market.api.user.order.builders;

import java.util.Arrays;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.cart.CartChange;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.outlet.NearestOutlet;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.OrderPromo;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class MultiCartOrderBuilder extends RandomBuilder<Order> {

    private Order order = new Order();

    @Override
    public MultiCartOrderBuilder random() {
        order.setShopId((long) random.getInt(1, 1000));
        order.setId((long) random.getInt(0, Integer.MAX_VALUE));
        order.setShopId((long) random.getInt(0, 1000));
        order.setFeeTotal(random.getPrice(10, 50));
        order.setBuyerCurrency(Currency.RUR);
        order.setLabel(random.getString());
        return this;
    }

    public MultiCartOrderBuilder randomCarted() {
        order.setShopId((long) random.getInt(1, 1000));
        return this;
    }

    public MultiCartOrderBuilder withId(long id) {
        order.setId(id);
        return this;
    }

    public MultiCartOrderBuilder withItem(OrderItem item) {
        order.addItem(item);
        return this;
    }

    public MultiCartOrderBuilder withDeliveryOptions(Delivery... options) {
        order.setDeliveryOptions(Arrays.asList(options));
        return this;
    }

    public MultiCartOrderBuilder withChanges(CartChange... changes) {
        order.setChanges(Sets.newHashSet(changes));
        return this;
    }

    public MultiCartOrderBuilder withShopId(long shopId) {
        order.setShopId(shopId);
        return this;
    }

    public MultiCartOrderBuilder withShopOrderId(String shopOrderId) {
        order.setShopOrderId(shopOrderId);
        return this;
    }

    public MultiCartOrderBuilder withPromos(OrderPromo... promos) {
        order.setPromos(Arrays.asList(promos));
        return this;
    }

    public MultiCartOrderBuilder withLabel(String label) {
        order.setLabel(label);
        return this;
    }

    @NotNull
    public MultiCartOrderBuilder withValidationErrors(@NotNull final ValidationResult... errors) {
        order.setValidationErrors(Arrays.asList(errors));
        return this;
    }

    public MultiCartOrderBuilder withNearestOutlet(NearestOutlet nearestOutlet) {
        order.setNearestOutlet(nearestOutlet);
        return this;
    }

    public MultiCartOrderBuilder withParcelInfo(String parcelInfo) {
        order.setParcelInfo(parcelInfo);
        return this;
    }

    @Override
    public Order build() {
        return order;
    }
}
