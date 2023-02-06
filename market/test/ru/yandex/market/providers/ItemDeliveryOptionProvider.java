package ru.yandex.market.providers;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.shopadminstub.model.ItemDeliveryOption;
import ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public abstract class ItemDeliveryOptionProvider {
    public static ItemDeliveryOption buildFree() {
        return buildFree(StubPushApiTestUtils.ONLY_CASH);
    }

    public static ItemDeliveryOption buildFree(List<PaymentMethod> paymentMethods) {
        ItemDeliveryOption firstIDO = new ItemDeliveryOption();
        firstIDO.setCurrency(Currency.RUR.name());
        firstIDO.setPrice(BigDecimal.ZERO);
        // хак, потому что ItemDeliveryOption не дает засетить  null
        firstIDO.setFromDay(-1);
        firstIDO.setToDay(null);
        firstIDO.setPaymentMethods(paymentMethods);
        return firstIDO;
    }

    public static ItemDeliveryOption buildAverage() {
        return buildAverage(StubPushApiTestUtils.ONLY_CASH);
    }

    public static ItemDeliveryOption buildAverage(List<PaymentMethod> paymentMethods) {
        ItemDeliveryOption secondIDO = new ItemDeliveryOption();
        secondIDO.setCurrency(Currency.RUR.name());
        secondIDO.setPrice(new BigDecimal("50"));
        secondIDO.setFromDay(14);
        secondIDO.setToDay(14);
        secondIDO.setPaymentMethods(paymentMethods);
        return secondIDO;
    }

    public static ItemDeliveryOption buildFastest() {
        return buildFastest(StubPushApiTestUtils.ALL_POSTPAID);
    }

    public static ItemDeliveryOption buildFastest(List<PaymentMethod> paymentMethods) {
        ItemDeliveryOption thirdIDO = new ItemDeliveryOption();
        thirdIDO.setCurrency(Currency.RUR.name());
        thirdIDO.setPrice(new BigDecimal("100"));
        thirdIDO.setFromDay(1);
        thirdIDO.setToDay(2);
        thirdIDO.setPaymentMethods(paymentMethods);
        return thirdIDO;
    }

    public static ItemDeliveryOption buildMostExpensive() {
        ItemDeliveryOption itemDeliveryOption = new ItemDeliveryOption();
        itemDeliveryOption.setCurrency(Currency.RUR.name());
        itemDeliveryOption.setPrice(new BigDecimal("500"));
        itemDeliveryOption.setFromDay(1);
        itemDeliveryOption.setToDay(2);
        itemDeliveryOption.setPaymentMethods(StubPushApiTestUtils.ALL_POSTPAID);
        return itemDeliveryOption;
    }

    public static ItemDeliveryOption buildGlobal() {
        ItemDeliveryOption globalIDO = new ItemDeliveryOption();
        globalIDO.setCurrency(Currency.RUR.name());
        globalIDO.setPrice(new BigDecimal("300"));
        globalIDO.setFromDay(0);
        globalIDO.setToDay(2);
        globalIDO.setPaymentMethods(Arrays.asList(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.CARD_ON_DELIVERY));
        return globalIDO;
    }
}
