package ru.yandex.market.api.user.order.builders;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryCustomizer;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalsCollection;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class MultiCartOrderDeliveryBuilder extends RandomBuilder<Delivery> {

    private Delivery delivery = new Delivery();

    public MultiCartOrderDeliveryBuilder(DeliveryType deliveryType) {
        delivery.setType(deliveryType);
    }

    @Override
    public MultiCartOrderDeliveryBuilder random() {
        delivery.setDeliveryOptionId(random.getString());
        delivery.setOutletId((long) random.getInt(10000));
        delivery.setBuyerPrice(random.getPrice(100, 100));
        delivery.setDeliveryDates(new DeliveryDates());
        return this;
    }

    public MultiCartOrderDeliveryBuilder withPaymentOptions(PaymentMethod... options) {
        delivery.setPaymentOptions(Sets.newHashSet(options));
        return this;
    }

    public MultiCartOrderDeliveryBuilder withHiddenPaymentOptions(PaymentOption... options) {
        delivery.setHiddenPaymentOptions(Lists.newArrayList(options));
        return this;
    }

    public MultiCartOrderDeliveryBuilder withDeliveryIntervals(RawDeliveryIntervalsCollection rawDeliveryIntervalsCollection) {
        delivery.setRawDeliveryIntervals(rawDeliveryIntervalsCollection);
        return this;
    }

    public MultiCartOrderDeliveryBuilder withOutletId(Long outletId) {
        delivery.setOutletId(outletId);
        return this;
    }

    public MultiCartOrderDeliveryBuilder withOutletIds(Long... outletIds) {
        delivery.setOutlets(Arrays.stream(outletIds)
            .map(x -> new ShopOutletBuilder()
                .random()
                .outletId(x)
                .build()
            )
            .collect(Collectors.toList())
        );
        return this;
    }

    public MultiCartOrderDeliveryBuilder withCustomizers(List<DeliveryCustomizer> customizers) {
        delivery.setCustomizers(customizers);
        return this;
    }


    @Override
    public Delivery build() {
        return delivery;
    }
}
