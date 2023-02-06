package ru.yandex.market.api.user.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.market.api.domain.v2.PerkType;
import ru.yandex.market.api.domain.v2.PerkStatus;
import ru.yandex.market.api.user.order.builders.MultiCartBuilder;
import ru.yandex.market.api.user.order.builders.OrderBuilder;
import ru.yandex.market.api.user.order.cashback.YandexCardCashback;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cashback.model.Cashback;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptions;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackProfile;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackPromoResponse;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.loyalty.api.model.CashbackPermision;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.matchers.YandexCardCashbackMatcher.agitationPriority;
import static ru.yandex.market.api.matchers.YandexCardCashbackMatcher.amount;
import static ru.yandex.market.api.matchers.YandexCardCashbackMatcher.cashbackPercent;
import static ru.yandex.market.api.matchers.YandexCardCashbackMatcher.maxOrderTotal;
import static ru.yandex.market.api.matchers.YandexCardCashbackMatcher.promoKey;
import static ru.yandex.market.api.matchers.YandexCardCashbackMatcher.yandexCardCashback;

public class YandexCashbackResolverTest {

    @Test
    public void createCashback() {
        CashbackProfile bankCashbackProfile = createBankCashbackProfile();
        PerkStatus bankPerk = createBankPerk();
        MultiCart multiCart = new MultiCartBuilder().random().build();
        multiCart.setCarts(Collections.singletonList(createOrder(PaymentMethod.GOOGLE_PAY, PaymentMethod.YANDEX)));
        multiCart.setCashbackOptionsProfiles(Collections.singletonList(bankCashbackProfile));


        YandexCardCashback yandexCardCashback = YandexCashbackResolver.resolveYandexCardCashback(
                multiCart,
                Collections.singleton(bankPerk)
        );

        assertThat(
                yandexCardCashback,
                yandexCardCashback(
                        amount(bankCashbackProfile.getCashback().getEmit().getPromos().get(0).getAmount()),
                        agitationPriority(bankCashbackProfile.getCashback().getEmit().getPromos().get(0).getAgitationPriority()),
                        promoKey(bankPerk.getPromoKey()),
                        cashbackPercent(bankPerk.getCashbackPercentNominal()),
                        maxOrderTotal(bankPerk.getMaxOrderTotal())
                )
        );
    }

    @Test
    public void notCreateCashbackIfHasPostpaidCart() {
        CashbackProfile bankCashbackProfile = createBankCashbackProfile();
        PerkStatus bankPerk = createBankPerk();
        MultiCart multiCart = new MultiCartBuilder().random().build();
        List<Order> orders = new ArrayList<>(2);
        orders.add(createOrder(PaymentMethod.GOOGLE_PAY, PaymentMethod.YANDEX));
        orders.add(createOrder(PaymentMethod.CARD_ON_DELIVERY, PaymentMethod.CARD_ON_DELIVERY));
        multiCart.setCarts(orders);
        multiCart.setCashbackOptionsProfiles(Collections.singletonList(bankCashbackProfile));


        YandexCardCashback yandexCardCashback = YandexCashbackResolver.resolveYandexCardCashback(
                multiCart,
                Collections.singleton(bankPerk)
        );

        assertNull(yandexCardCashback);
    }

    @Test
    public void notCreateCashbackWithoutBankPerk() {
        CashbackProfile bankCashbackProfile = createBankCashbackProfile();
        PerkStatus perkStatus = new PerkStatus();
        perkStatus.setType(PerkType.GROWING_CASHBACK.getId());
        perkStatus.setMaxOrderTotal(new BigDecimal(15000));
        perkStatus.setCashbackPercentNominal(10);
        MultiCart multiCart = new MultiCartBuilder().random().build();
        multiCart.setCarts(Collections.singletonList(createOrder(PaymentMethod.YANDEX)));
        multiCart.setCashbackOptionsProfiles(Collections.singletonList(bankCashbackProfile));


        YandexCardCashback yandexCardCashback = YandexCashbackResolver.resolveYandexCardCashback(
                multiCart,
                Collections.singleton(perkStatus)
        );

        assertNull(yandexCardCashback);
    }

    @Test
    public void notCreateCashbackWithoutCashbackInfo() {
        PerkStatus bankPerk = createBankPerk();
        MultiCart multiCart = new MultiCartBuilder().random().build();
        multiCart.setCarts(Collections.singletonList(createOrder(PaymentMethod.GOOGLE_PAY, PaymentMethod.YANDEX)));
        multiCart.setCashback(null);


        YandexCardCashback yandexCardCashback = YandexCashbackResolver.resolveYandexCardCashback(
                multiCart,
                Collections.singleton(bankPerk)
        );

        assertNull(yandexCardCashback);
    }

    @Test
    public void notCreateCashbackIfPromoNotMatched() {
        CashbackPromoResponse cashbackPromoResponse = new CashbackPromoResponse(
                new BigDecimal(123),
                "some_promo",
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
                "test",
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
        CashbackProfile wrongProfile = new CashbackProfile(
                null,
                null,
                null,
                null,
                new Cashback(emit, null),
                null
        );
        PerkStatus bankPerk = createBankPerk();
        MultiCart multiCart = new MultiCartBuilder().random().build();
        multiCart.setCarts(Collections.singletonList(createOrder(PaymentMethod.GOOGLE_PAY, PaymentMethod.YANDEX)));
        multiCart.setCashbackOptionsProfiles(Collections.singletonList(wrongProfile));


        YandexCardCashback yandexCardCashback = YandexCashbackResolver.resolveYandexCardCashback(
                multiCart,
                Collections.singleton(bankPerk)
        );

        assertNull(yandexCardCashback);
    }

    private PerkStatus createBankPerk() {
        PerkStatus bankPerk = new PerkStatus();
        bankPerk.setType(PerkType.YANDEX_BANK_CASHBACK.getId());
        bankPerk.setMaxOrderTotal(new BigDecimal(15000));
        bankPerk.setCashbackPercentNominal(10);
        bankPerk.setPromoKey("yandex_bank");
        return bankPerk;
    }

    private CashbackProfile createBankCashbackProfile() {
        CashbackPromoResponse cashbackPromoResponse = new CashbackPromoResponse(
                new BigDecimal(123),
                "yandex_bank",
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
                "test",
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
        return new CashbackProfile(
                null,
                null,
                null,
                null,
                new Cashback(emit, null),
                null
        );
    }

    private Order createOrder(PaymentMethod... paymentMethods) {
        Order order = new OrderBuilder().random().build();
        order.setPaymentOptions(Arrays.stream(paymentMethods).collect(Collectors.toSet()));
        return order;
    }
}
