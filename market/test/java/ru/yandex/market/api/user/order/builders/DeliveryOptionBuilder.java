package ru.yandex.market.api.user.order.builders;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import ru.yandex.market.api.user.order.DeliveryOption;
import ru.yandex.market.api.user.order.Outlet;
import ru.yandex.market.api.user.order.OutletDeliveryOption;
import ru.yandex.market.api.user.order.PostDeliveryOption;
import ru.yandex.market.api.user.order.ServiceDeliveryOption;
import ru.yandex.market.api.user.order.checkout.DeliveryPointId;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class DeliveryOptionBuilder extends RandomBuilder<DeliveryOption> {

    DeliveryOption deliveryOption;

    public DeliveryOptionBuilder(Class<? extends DeliveryOption> clazz) {
        if (clazz.equals(OutletDeliveryOption.class)) {
            deliveryOption = new OutletDeliveryOption();
        } else if (clazz.equals(ServiceDeliveryOption.class)) {
            deliveryOption = new ServiceDeliveryOption();
        } else if (clazz.equals(PostDeliveryOption.class)) {
            deliveryOption = new PostDeliveryOption();
        } else {
            throw new IllegalArgumentException("Class doesn't extends DeliveryOption");
        }
    }

    @Override
    public DeliveryOptionBuilder random() {
        deliveryOption.setId(new DeliveryPointId());
        deliveryOption.setPrice(random.getPrice(100, 100));
        deliveryOption.setPaymentMethods(Sets.newHashSet(random.from(PaymentMethod.class)));

        LocalDate begin = random.getLocalDate();
        deliveryOption.setBeginDate(begin);
        deliveryOption.setEndDate(begin.plus(2, ChronoUnit.DAYS));
        return this;
    }

    public DeliveryOptionBuilder withPaymentOptions(PaymentMethod ... methods) {
        deliveryOption.setPaymentMethods(Sets.newHashSet(methods));
        return this;
    }

    public DeliveryOptionBuilder withHiddenPaymentOptions(DeliveryOption.HiddenPaymentOption... options) {
        deliveryOption.setHiddenPaymentMethods(Sets.newHashSet(options));
        return this;
    }

    public DeliveryOptionBuilder withId(DeliveryPointId id) {
        deliveryOption.setId(id);
        return this;
    }

    public DeliveryOptionBuilder withOutlets(Outlet ... outlets) {
        if (deliveryOption instanceof OutletDeliveryOption) {
            ((OutletDeliveryOption) deliveryOption).setOutlets(Lists.newArrayList(outlets));
        } else {
            throw new IllegalArgumentException("Class doesn't extends OutletDeliveryOption");
        }
        return this;
    }

    @Override
    public DeliveryOption build() {
        return deliveryOption;
    }
}
