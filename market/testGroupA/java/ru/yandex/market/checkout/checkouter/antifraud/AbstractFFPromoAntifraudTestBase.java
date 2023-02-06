package ru.yandex.market.checkout.checkouter.antifraud;

import java.math.BigDecimal;
import java.util.Random;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.HitRateGroup;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.PaymentParameters;
import ru.yandex.market.checkout.providers.FFDeliveryProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.common.zk.ZooClient;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.checkouter.antifraud.FFPromoOrdersAntifraudTest.EXPECTED_ORDER_LIMIT;
import static ru.yandex.market.checkout.checkouter.antifraud.FFPromoOrdersAntifraudTest.FF_PROMO_FRAUD_MATCHER;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNPAID;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.defaultOrderItem;

public class AbstractFFPromoAntifraudTestBase extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    protected ZooClient zooClient;

    protected Order order;

    protected void createOrder(boolean prepaid, boolean fulfillment, boolean promo, Buyer buyer, ResultMatcher
            errorMatcher) {
        Order orderTemplate = OrderProvider.getBlueOrder();

        orderTemplate.setItems(singleton(defaultOrderItem()));
        if (prepaid) {
            orderTemplate.setPaymentType(PaymentType.PREPAID);
            orderTemplate.setPaymentMethod(PaymentMethod.YANDEX);
        } else {
            orderTemplate.setPaymentType(PaymentType.POSTPAID);
            orderTemplate.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        }
        orderTemplate.setAcceptMethod(OrderAcceptMethod.PUSH_API);

        Parameters parameters = new Parameters(buyer, orderTemplate);
        parameters.setDeliveryType(null);
        parameters.setDeliveryPartnerType(null);
        parameters.setDeliveryServiceId(null);

        if (promo) {
            parameters.setupPromo("SOME-PROMO-CODE");
        }
        if (fulfillment) {
            parameters.setupFulfillment(new ItemInfo.Fulfilment(123L, "TEST_SKU", "TEST_SHOP_SKU"));
            FFDeliveryProvider.setFFDeliveryParameters(parameters);
            parameters.setWeight(BigDecimal.valueOf(1));
            parameters.setDimensions("10", "10", "10");
        }

        parameters.setHitRateGroup(HitRateGroup.UNLIMIT);
        if (errorMatcher != null) {
            parameters.setErrorMatcher(errorMatcher);
        }

        order = orderCreateHelper.createOrder(parameters);

        if (errorMatcher == null) {
            assertThat(order.isFulfilment(), is(fulfillment));
            assertThat(order.getPromos(), hasSize(promo ? 1 : 0));
            assertThat(order.getStatus(), is(prepaid ? UNPAID : PROCESSING));
        }
    }

    protected void createOrder(boolean prepaid, boolean fulfillment, boolean promo, Buyer buyer) {
        createOrder(prepaid, fulfillment, promo, buyer, null);
    }

    protected void createOrder(boolean prepaid, boolean fulfillment, boolean promo) {
        createOrder(prepaid, fulfillment, promo, null, null);
    }

    protected void createOrder(Buyer buyer) {
        createOrder(true, true, true, buyer, null);
    }

    protected void createOrder(Buyer buyer, ResultMatcher errorMatcher) {
        createOrder(true, true, true, buyer, errorMatcher);
    }

    protected void createAndPayOrder(String cardNumber) {
        createAndPayOrder(cardNumber, true, false);
    }

    protected void createAndPayOrder(String cardNumber, boolean successfulPayment) {
        createAndPayOrder(cardNumber, successfulPayment, false);
    }

    protected void createAndPayOrder(String cardNumber, boolean successfulPayment, boolean expectedAntifraud) {
        createAndPayOrder(newRandomBuyer(), cardNumber, successfulPayment, expectedAntifraud);
    }

    protected void createAndPayOrder(Buyer buyer, String cardNumber, boolean successfulPayment, boolean
            expectedAntifraud) {
        createOrder(buyer);
        payForOrder(cardNumber, successfulPayment, expectedAntifraud);
    }

    protected void payForOrder(String cardNumber, boolean successfulPayment, boolean expectedAntifraud) {
        PaymentParameters paymentParameters = new PaymentParameters();
        paymentParameters.setUid(order.getBuyer().getUid());
        paymentParameters.setSandbox(order.isFake());

        final Payment payment = orderPayHelper.pay(order.getId(), paymentParameters);


        if (successfulPayment) {
            CheckBasketParams checkBasketParams = new CheckBasketParams();
            checkBasketParams.setBankCard(cardNumber);
            trustMockConfigurer.mockCheckBasket(checkBasketParams);
            trustMockConfigurer.mockStatusBasket(checkBasketParams, null);
        } else {
            trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildFailCheckBasket());
            trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildFailCheckBasket(), null);

        }

        orderPayHelper.notifyPayment(payment);

        order = orderService.getOrder(order.getId());
        if (expectedAntifraud) {
            assertThat(order.getStatus(), is(OrderStatus.CANCELLED));
            assertThat(order.getSubstatus(), is(OrderSubstatus.USER_FRAUD));
        } else if (successfulPayment) {
            assertThat(order.getStatus(), is(OrderStatus.PROCESSING));
        } else {
            assertThat(order.getPayment().getStatus(), is(PaymentStatus.CANCELLED));
        }
    }

    protected void createAndPayOrdersWithBuyer(Buyer buyer, String cardNumber, int count) {
        for (int i = 0; i < count; i++) {
            createAndPayOrder(buyer, cardNumber, true, false);
        }
    }

    protected void tryFraudWithBuyer(Supplier<Buyer> buyerSupplier) {
        for (int i = 0; i < EXPECTED_ORDER_LIMIT; i++) {
            createOrder(buyerSupplier.get());
        }
        createOrder(buyerSupplier.get(), FF_PROMO_FRAUD_MATCHER);
    }

    protected Buyer newBuyer(Long uid, String phone, String email) {
        Buyer buyer = BuyerProvider.getBuyer();
        buyer.setUid(uid);
        buyer.setPhone(phone);
        buyer.setEmail(email);
        return buyer;
    }

    protected Buyer newRandomBuyer() {
        return newBuyer(randomUid(), randomPhone(), randomEmail());
    }

    protected long randomUid() {
        return new Random().nextInt(9999999) + 1111111;
    }

    protected String randomPhone() {
        return "+7495" + (new Random().nextInt(8999999) + 1000000);
    }

    protected String randomEmail() {
        return (new Random().nextInt(8999999) + 1000000) + "@yandex.ru";
    }
}
