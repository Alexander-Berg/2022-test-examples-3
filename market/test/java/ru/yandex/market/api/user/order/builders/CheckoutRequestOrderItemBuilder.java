package ru.yandex.market.api.user.order.builders;

import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.user.order.Payload;
import ru.yandex.market.api.user.order.checkout.CheckoutRequest;

import java.math.BigDecimal;
import java.util.Set;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class CheckoutRequestOrderItemBuilder extends RandomBuilder<CheckoutRequest.OrderItem> {

    CheckoutRequest.OrderItem item = new CheckoutRequest.OrderItem();

    @Override
    public CheckoutRequestOrderItemBuilder random() {
        item.setPrice(random.getPrice(100, 99));
        item.setOfferId(new OfferId(random.getString(), null));
        item.setCount(random.getInt(1000));
        item.setPayload(new Payload(
            random.getInt(10000),
            random.getString(),
            random.getString(),
            random.getString()
        ));
        return this;
    }

    public CheckoutRequestOrderItemBuilder withPayload(long feedId, String shopOfferId, String marketOfferId, String fee) {
        return withPayload(new Payload(feedId, shopOfferId, marketOfferId, fee));
    }

    public CheckoutRequestOrderItemBuilder withPayload(Payload payload) {
        item.setPayload(payload);
        return this;
    }

    public CheckoutRequestOrderItemBuilder withId(OfferId id) {
        item.setOfferId(id);
        return this;
    }

    public CheckoutRequestOrderItemBuilder withBuyerPriceNominal(BigDecimal price) {
        item.setBuyerPriceNominal(price);
        return this;
    }

    public CheckoutRequestOrderItemBuilder withBundleId(String bundleId) {
        item.setBundleId(bundleId);
        return this;
    }

    public CheckoutRequestOrderItemBuilder withPrescriptionGuids(Set<String> prescriptionGuids) {
        item.setPrescriptionGuids(prescriptionGuids);
        return this;
    }

    @Override
    public CheckoutRequest.OrderItem build() {
        return item;
    }
}
