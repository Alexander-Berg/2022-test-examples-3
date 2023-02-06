package ru.yandex.market.api.user.order;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

import ru.yandex.market.api.domain.v2.PerkStatus;
import ru.yandex.market.api.user.order.builders.MultiCartBuilder;
import ru.yandex.market.api.user.order.builders.OrderBuilder;
import ru.yandex.market.api.user.order.cashback.Cashback;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptions;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackProfile;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackPromoResponse;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.loyalty.api.model.CashbackPermision;

import static org.junit.Assert.assertNotNull;

public class YandexCardOrderTest {
    @Test
    public void yandexBankCashback() {
        PerkStatus perkStatus = new PerkStatus();
        perkStatus.setType(ru.yandex.market.api.domain.v2.PerkType.YANDEX_BANK_CASHBACK.getId());
        perkStatus.setMaxOrderTotal(BigDecimal.valueOf(15000));
        perkStatus.setPromoKey("yandexBank");
        perkStatus.setCashbackPercentNominal(5);

        MultiCart multiCart = new MultiCartBuilder().random().build();
        CashbackPromoResponse cashbackPromoResponse = new CashbackPromoResponse(
                new BigDecimal(123),
                perkStatus.getPromoKey(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                null,
                null,
                null,
                null
        );
        CashbackOptions emit = new CashbackOptions(
                perkStatus.getPromoKey(),
                1,
                BigDecimal.TEN,
                null,
                Collections.singletonList(cashbackPromoResponse),
                CashbackPermision.ALLOWED,
                null,
                null,
                null,
                null
        );
        CashbackProfile bankProfile = new CashbackProfile(
                null,
                null,
                null,
                null,
                new ru.yandex.market.checkout.checkouter.cashback.model.Cashback(emit, null),
                null
        );
        multiCart.setCashbackOptionsProfiles(Collections.singletonList(bankProfile));
        multiCart.setCashback(new ru.yandex.market.checkout.checkouter.cashback.model.Cashback(emit, null));
        Order order = new OrderBuilder().random().build();
        order.setPaymentOptions(new HashSet<>(Collections.singleton(PaymentMethod.YANDEX)));
        multiCart.setCarts(Collections.singletonList(order));
        Cashback cashback = Cashback.fromMultiCart(multiCart, Collections.singletonList(perkStatus));

        assertNotNull(cashback.getYandexCardCashback());
        assertNotNull(cashback.getYandexCardCashback().getCashbackPercent());
        assertNotNull(cashback.getYandexCardCashback().getPromoKey());
        assertNotNull(cashback.getYandexCardCashback().getAmount());
        assertNotNull(cashback.getYandexCardCashback().getMaxOrderTotal());
    }

}
