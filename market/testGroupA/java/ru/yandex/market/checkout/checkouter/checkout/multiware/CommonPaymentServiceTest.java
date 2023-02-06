package ru.yandex.market.checkout.checkouter.checkout.multiware;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.cart.CommonPayment;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cashback.model.Cashback;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOption;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptions;
import ru.yandex.market.checkout.checkouter.order.CommonPaymentService;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.loyalty.api.model.CashbackPermision.RESTRICTED;
import static ru.yandex.market.loyalty.api.model.CashbackRestrictionReason.UNKNOWN;

public class CommonPaymentServiceTest {

    public static final String CART_LABEL_1 = "cart1";
    public static final String CART_LABEL_2 = "cart2";
    public static final BigDecimal CART_PRICE_1 = BigDecimal.valueOf(5010);
    public static final BigDecimal CART_PRICE_2 = BigDecimal.valueOf(1050);
    public static final BigDecimal CASHBACK_AMOUNT = BigDecimal.valueOf(1337);
    private static final String DEFAULT_PROMO_KEY = "1";

    private final CommonPaymentService service = new CommonPaymentService();

    @Test
    public void multiOrderWithCartsHavingMixedPaymentOptions() {
        MultiCart multiCart = new MultiCart();
        multiCart.addCart(OrderProvider.getBlueFulfilmentCart(o -> {
            o.getPaymentOptions().removeIf(po -> po.getPaymentType() == PaymentType.PREPAID);
            o.setBuyerTotal(CART_PRICE_1);
            o.setLabel(CART_LABEL_1);
        }));
        multiCart.addCart(OrderProvider.getBlueFulfilmentCart(o -> {
            o.getPaymentOptions().removeIf(po -> po.getPaymentType() == PaymentType.POSTPAID);
            o.setBuyerTotal(CART_PRICE_2);
            o.setLabel(CART_LABEL_2);
        }));

        CommonPayment info = service.calculateCommonPayment(multiCart);

        assertThat(info.isPostpayForAll(), equalTo(false));
        assertThat(info.isPrepayForAll(), equalTo(false));

        assertThat(info.getMaxPrepay(), notNullValue());
        assertThat(info.getMaxPrepay().getPrepayLabels(), hasSize(1));
        assertThat(info.getMaxPrepay().getPrepayLabels(), contains(CART_LABEL_2));
        assertThat(info.getMaxPrepay().getPrepayTotal(), equalTo(CART_PRICE_2));
        assertThat(info.getMaxPrepay().getPrepayMoneyTotal(), equalTo(CART_PRICE_2));

        assertThat(info.getMaxPrepay().getPostpayLabels(), hasSize(1));
        assertThat(info.getMaxPrepay().getPostpayLabels(), contains(CART_LABEL_1));
        assertThat(info.getMaxPrepay().getPostpayTotal(), equalTo(CART_PRICE_1));
    }

    @Test
    public void multiOrderWithAllCartsHavingOnlyPrepayOptions() {
        MultiCart multiCart = new MultiCart();
        multiCart.addCart(OrderProvider.getBlueFulfilmentCart(o -> {
            o.getPaymentOptions().removeIf(po -> po.getPaymentType() == PaymentType.POSTPAID);
            o.setBuyerTotal(CART_PRICE_1);
            o.setLabel(CART_LABEL_1);
        }));
        multiCart.addCart(OrderProvider.getBlueFulfilmentCart(o -> {
            o.setBuyerTotal(CART_PRICE_2);
            o.setLabel(CART_LABEL_2);
        }));

        CommonPayment info = service.calculateCommonPayment(multiCart);

        assertThat(info.isPostpayForAll(), equalTo(false));
        assertThat(info.isPrepayForAll(), equalTo(true));

        assertThat(info.getMaxPrepay(), notNullValue());
        assertThat(info.getMaxPrepay().getPrepayLabels(), hasSize(2));
        assertThat(info.getMaxPrepay().getPrepayLabels(), containsInAnyOrder(CART_LABEL_2, CART_LABEL_1));
        BigDecimal prepayTotal = CART_PRICE_2.add(CART_PRICE_1);
        assertThat(info.getMaxPrepay().getPrepayTotal(), equalTo(prepayTotal));
        assertThat(info.getMaxPrepay().getPrepayMoneyTotal(), equalTo(prepayTotal));

        assertThat(info.getMaxPrepay().getPostpayLabels(), hasSize(0));
    }

    @Test
    public void multiOrderWithAllCartsHavingOnlyPostpayOptions() {
        MultiCart multiCart = new MultiCart();
        multiCart.addCart(OrderProvider.getBlueFulfilmentCart(o -> {
            o.getPaymentOptions().removeIf(po -> po.getPaymentType() == PaymentType.PREPAID);
            o.setBuyerTotal(CART_PRICE_1);
            o.setLabel(CART_LABEL_1);
        }));
        multiCart.addCart(OrderProvider.getBlueFulfilmentCart(o -> {
            o.setBuyerTotal(CART_PRICE_2);
            o.setLabel(CART_LABEL_2);
        }));

        CommonPayment info = service.calculateCommonPayment(multiCart);

        assertThat(info.isPostpayForAll(), equalTo(true));
        assertThat(info.isPrepayForAll(), equalTo(false));

        assertThat(info.getMaxPrepay(), notNullValue());
        assertThat(info.getMaxPrepay().getPrepayLabels(), hasSize(1));
        assertThat(info.getMaxPrepay().getPrepayLabels(), contains(CART_LABEL_2));
        assertThat(info.getMaxPrepay().getPrepayTotal(), equalTo(CART_PRICE_2));
        assertThat(info.getMaxPrepay().getPrepayMoneyTotal(), equalTo(CART_PRICE_2));

        assertThat(info.getMaxPrepay().getPostpayLabels(), hasSize(1));
        assertThat(info.getMaxPrepay().getPostpayLabels(), contains(CART_LABEL_1));
        assertThat(info.getMaxPrepay().getPostpayTotal(), equalTo(CART_PRICE_1));
    }

    @Test
    public void multiOrderWithAllCartsPrepayAndPostpayOptions() {
        MultiCart multiCart = new MultiCart();
        multiCart.addCart(OrderProvider.getBlueFulfilmentCart(o -> {
            o.setBuyerTotal(CART_PRICE_1);
            o.setLabel(CART_LABEL_1);
        }));
        multiCart.addCart(OrderProvider.getBlueFulfilmentCart(o -> {
            o.setBuyerTotal(CART_PRICE_2);
            o.setLabel(CART_LABEL_2);
        }));

        CommonPayment info = service.calculateCommonPayment(multiCart);

        assertThat(info.isPostpayForAll(), equalTo(true));
        assertThat(info.isPrepayForAll(), equalTo(true));

        assertThat(info.getMaxPrepay(), notNullValue());
        assertThat(info.getMaxPrepay().getPrepayLabels(), hasSize(2));
        assertThat(info.getMaxPrepay().getPrepayLabels(), containsInAnyOrder(CART_LABEL_2, CART_LABEL_1));
        BigDecimal prepayTotal = CART_PRICE_2.add(CART_PRICE_1);
        assertThat(info.getMaxPrepay().getPrepayTotal(), equalTo(prepayTotal));
        assertThat(info.getMaxPrepay().getPrepayMoneyTotal(), equalTo(prepayTotal));

        assertThat(info.getMaxPrepay().getPostpayLabels(), hasSize(0));
    }

    @Test
    public void testPrepayMoneyTotalWithAllowedCashback() {
        MultiCart multiCart = new MultiCart();
        multiCart.addCart(OrderProvider.getBlueFulfilmentCart(o -> {
            o.setBuyerTotal(CART_PRICE_1);
            o.setLabel(CART_LABEL_1);
        }));
        multiCart.addCart(OrderProvider.getBlueFulfilmentCart(o -> {
            o.setBuyerTotal(CART_PRICE_2);
            o.setLabel(CART_LABEL_2);
        }));
        multiCart.setSelectedCashbackOption(CashbackOption.SPEND);
        multiCart.setCashback(new Cashback(null, CashbackOptions.allowed(CASHBACK_AMOUNT, DEFAULT_PROMO_KEY)));

        CommonPayment info = service.calculateCommonPayment(multiCart);

        assertThat(info.isPostpayForAll(), equalTo(true));
        assertThat(info.isPrepayForAll(), equalTo(true));

        assertThat(info.getMaxPrepay(), notNullValue());
        assertThat(info.getMaxPrepay().getPrepayLabels(), hasSize(2));
        assertThat(info.getMaxPrepay().getPrepayLabels(), containsInAnyOrder(CART_LABEL_2, CART_LABEL_1));
        BigDecimal totalPrepay = CART_PRICE_2.add(CART_PRICE_1);
        assertThat(info.getMaxPrepay().getPrepayTotal(), equalTo(totalPrepay));
        BigDecimal totalMoneyPrepay = totalPrepay.subtract(CASHBACK_AMOUNT);
        assertThat(info.getMaxPrepay().getPrepayMoneyTotal(), equalTo(totalMoneyPrepay));
        assertThat(info.getMaxPrepay().getPostpayLabels(), hasSize(0));
    }

    @Test
    public void testPrepayMoneyTotalWithRestrictedCashback() {
        MultiCart multiCart = new MultiCart();
        multiCart.addCart(OrderProvider.getBlueFulfilmentCart(o -> {
            o.getPaymentOptions().removeIf(po -> po.getPaymentType() == PaymentType.PREPAID);
            o.setBuyerTotal(CART_PRICE_1);
            o.setLabel(CART_LABEL_1);
        }));
        multiCart.addCart(OrderProvider.getBlueFulfilmentCart(o -> {
            o.setBuyerTotal(CART_PRICE_2);
            o.setLabel(CART_LABEL_2);
        }));
        multiCart.setSelectedCashbackOption(CashbackOption.SPEND);
        multiCart.setCashback(new Cashback(null, new CashbackOptions(
                DEFAULT_PROMO_KEY, 1, CASHBACK_AMOUNT, Collections.singletonMap(DEFAULT_PROMO_KEY, CASHBACK_AMOUNT),
                null, RESTRICTED, UNKNOWN, null, null, null
        )));

        CommonPayment info = service.calculateCommonPayment(multiCart);

        assertThat(info.isPostpayForAll(), equalTo(true));
        assertThat(info.isPrepayForAll(), equalTo(false));

        assertThat(info.getMaxPrepay(), notNullValue());
        assertThat(info.getMaxPrepay().getPrepayLabels(), hasSize(1));
        assertThat(info.getMaxPrepay().getPrepayLabels(), contains(CART_LABEL_2));
        assertThat(info.getMaxPrepay().getPrepayTotal(), equalTo(CART_PRICE_2));
        assertThat(info.getMaxPrepay().getPrepayMoneyTotal(), equalTo(CART_PRICE_2));

        assertThat(info.getMaxPrepay().getPostpayLabels(), hasSize(1));
        assertThat(info.getMaxPrepay().getPostpayLabels(), contains(CART_LABEL_1));
        assertThat(info.getMaxPrepay().getPostpayTotal(), equalTo(CART_PRICE_1));
    }

    @Test
    public void testPrepayMoneyTotalWithNullSpendCashback() {
        MultiCart multiCart = new MultiCart();
        multiCart.addCart(OrderProvider.getBlueFulfilmentCart(o -> {
            o.getPaymentOptions().removeIf(po -> po.getPaymentType() == PaymentType.PREPAID);
            o.setBuyerTotal(CART_PRICE_1);
            o.setLabel(CART_LABEL_1);
        }));
        multiCart.addCart(OrderProvider.getBlueFulfilmentCart(o -> {
            o.setBuyerTotal(CART_PRICE_2);
            o.setLabel(CART_LABEL_2);
        }));
        multiCart.setSelectedCashbackOption(CashbackOption.SPEND);
        multiCart.setCashback(new Cashback(CashbackOptions.allowed(CASHBACK_AMOUNT, DEFAULT_PROMO_KEY), null));

        CommonPayment info = service.calculateCommonPayment(multiCart);

        assertThat(info.isPostpayForAll(), equalTo(true));
        assertThat(info.isPrepayForAll(), equalTo(false));

        assertThat(info.getMaxPrepay(), notNullValue());
        assertThat(info.getMaxPrepay().getPrepayLabels(), hasSize(1));
        assertThat(info.getMaxPrepay().getPrepayLabels(), contains(CART_LABEL_2));
        assertThat(info.getMaxPrepay().getPrepayTotal(), equalTo(CART_PRICE_2));
        assertThat(info.getMaxPrepay().getPrepayMoneyTotal(), equalTo(CART_PRICE_2));

        assertThat(info.getMaxPrepay().getPostpayLabels(), hasSize(1));
        assertThat(info.getMaxPrepay().getPostpayLabels(), contains(CART_LABEL_1));
        assertThat(info.getMaxPrepay().getPostpayTotal(), equalTo(CART_PRICE_1));
    }
}
