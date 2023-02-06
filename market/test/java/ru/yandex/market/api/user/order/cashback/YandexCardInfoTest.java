package ru.yandex.market.api.user.order.cashback;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.api.domain.v2.PerkType;
import ru.yandex.market.api.domain.v2.PerkStatus;
import ru.yandex.market.api.user.order.builders.MultiCartBuilder;
import ru.yandex.market.api.user.order.builders.MultiCartTotalsBuilder;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cart.MultiCartTotals;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.matchers.YandexCardInfoMatcher.limit;
import static ru.yandex.market.api.matchers.YandexCardInfoMatcher.yandexCardInfo;
import static ru.yandex.market.api.matchers.YandexCardInfoMatcher.yandexCardPaymentAllowed;

public class YandexCardInfoTest {

    @Test
    public void createAllowedPaymentInfo() {
        BigDecimal limit = new BigDecimal(15000);
        PerkStatus perkStatus = new PerkStatus();
        perkStatus.setType(PerkType.YANDEX_BANK_CASHBACK.getId());
        perkStatus.setMaxOrderTotal(limit);
        Collection<PerkStatus> perks = Collections.singleton(perkStatus);

        MultiCartTotals multiCartTotals = new MultiCartTotalsBuilder().withBuyerTotal(new BigDecimal(10000)).build();
        MultiCart multiCart = new MultiCartBuilder().withTotals(multiCartTotals).build();

        YandexCardInfo yandexCardInfo = YandexCardInfo.fromPerksAndCart(perks, multiCart);

        assertThat(yandexCardInfo, yandexCardInfo(
                limit(limit),
                yandexCardPaymentAllowed(true)
        ));
    }

    @Test
    public void createLimitedPaymentInfo() {
        BigDecimal limit = new BigDecimal(20000);
        PerkStatus perkStatus = new PerkStatus();
        perkStatus.setType(PerkType.YANDEX_BANK_CASHBACK.getId());
        perkStatus.setMaxOrderTotal(limit);
        Collection<PerkStatus> perks = Collections.singleton(perkStatus);

        MultiCartTotals multiCartTotals =
                new MultiCartTotalsBuilder().withBuyerTotal(limit.add(BigDecimal.ONE)).build();
        MultiCart multiCart = new MultiCartBuilder().withTotals(multiCartTotals).build();

        YandexCardInfo yandexCardInfo = YandexCardInfo.fromPerksAndCart(perks, multiCart);

        assertThat(yandexCardInfo, yandexCardInfo(
                limit(limit),
                yandexCardPaymentAllowed(false)
        ));
    }

    @Test
    public void notCreateIfNoBankPerk() {
        PerkStatus perkStatus = new PerkStatus();
        perkStatus.setType(PerkType.GROWING_CASHBACK.getId());
        Collection<PerkStatus> perks = Collections.singleton(perkStatus);

        MultiCartTotals multiCartTotals = new MultiCartTotalsBuilder().withBuyerTotal(BigDecimal.TEN).build();
        MultiCart multiCart = new MultiCartBuilder().withTotals(multiCartTotals).build();

        YandexCardInfo yandexCardInfo = YandexCardInfo.fromPerksAndCart(perks, multiCart);

        assertNull(yandexCardInfo);
    }

    @Test
    public void notCreateIfNoLimitInBankPerk() {
        PerkStatus perkStatus = new PerkStatus();
        perkStatus.setType(PerkType.YANDEX_BANK_CASHBACK.getId());
        Collection<PerkStatus> perks = Collections.singleton(perkStatus);

        MultiCartTotals multiCartTotals = new MultiCartTotalsBuilder().withBuyerTotal(BigDecimal.TEN).build();
        MultiCart multiCart = new MultiCartBuilder().withTotals(multiCartTotals).build();

        YandexCardInfo yandexCardInfo = YandexCardInfo.fromPerksAndCart(perks, multiCart);

        assertNull(yandexCardInfo);
    }
}
