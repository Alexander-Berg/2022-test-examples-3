package ru.yandex.market.api.user.order.cashback;

import java.math.BigDecimal;

import org.junit.Test;

import ru.yandex.market.api.domain.v2.PerkType;
import ru.yandex.market.api.domain.v2.PerkStatus;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackPromoResponse;

import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.matchers.PaymentSystemCashbackMatcher.agitationPriority;
import static ru.yandex.market.api.matchers.PaymentSystemCashbackMatcher.amount;
import static ru.yandex.market.api.matchers.PaymentSystemCashbackMatcher.cashbackPercent;
import static ru.yandex.market.api.matchers.PaymentSystemCashbackMatcher.paymentSystemCashback;
import static ru.yandex.market.api.matchers.PaymentSystemCashbackMatcher.promoKey;
import static ru.yandex.market.api.matchers.PaymentSystemCashbackMatcher.system;

public class PaymentSystemCashbackCreateTest {

    @Test
    public void testCreateFromPerk() {
        PerkStatus perkStatus = new PerkStatus();
        perkStatus.setPromoKey("promoKey");
        perkStatus.setType(PerkType.PAYMENT_SYSTEM_EXTRA_CASHBACK.getId());
        perkStatus.setCashbackPercentNominal(20);
        perkStatus.setPaymentSystem("mastercard");
        CashbackPromoResponse cashbackPromoResponse = new CashbackPromoResponse(
                new BigDecimal(123),
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
                null,
                10,
                null,
                null,
                null,
                null
        );

        PaymentSystemCashback paymentSystemCashback = PaymentSystemCashback.fromPerkAndPromo(cashbackPromoResponse,
                perkStatus);

        assertThat(paymentSystemCashback, paymentSystemCashback(
                promoKey("promoKey"),
                cashbackPercent(20),
                system("mastercard"),
                amount(cashbackPromoResponse.getAmount()),
                agitationPriority(cashbackPromoResponse.getAgitationPriority())
        ));
    }
}
