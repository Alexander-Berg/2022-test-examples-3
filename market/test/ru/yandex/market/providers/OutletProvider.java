package ru.yandex.market.providers;

import java.math.BigDecimal;
import java.util.Collections;

import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.common.report.model.outlet.Outlet;
import ru.yandex.market.common.report.model.outlet.SelfDeliveryRule;

public abstract class OutletProvider {

    private OutletProvider() {
        throw new UnsupportedOperationException();
    }

    public static Outlet buildFirst() {
        SelfDeliveryRule selfDeliveryRule = new SelfDeliveryRule(null, null, new BigDecimal(50));
        return new Outlet("69", selfDeliveryRule, Collections.singleton(PaymentMethod.CARD_ON_DELIVERY.name()));
    }

    public static Outlet buildSecond() {
        SelfDeliveryRule selfDeliveryRule = new SelfDeliveryRule(null, null, new BigDecimal(50));
        return new Outlet("96", selfDeliveryRule, Collections.singleton(PaymentMethod.CASH_ON_DELIVERY.name()));
    }

    public static Outlet buildThird() {
        SelfDeliveryRule selfDeliveryRule = new SelfDeliveryRule(null, null, new BigDecimal(50));
        return new Outlet("70", selfDeliveryRule, Collections.singletonList(PaymentMethod.CASH_ON_DELIVERY.name()));
    }

    public static Outlet buildFourth() {
        SelfDeliveryRule selfDeliveryRule = new SelfDeliveryRule(null, null, null);
        return new Outlet("71", selfDeliveryRule, Collections.singletonList(PaymentMethod.CASH_ON_DELIVERY.name()));
    }
}
