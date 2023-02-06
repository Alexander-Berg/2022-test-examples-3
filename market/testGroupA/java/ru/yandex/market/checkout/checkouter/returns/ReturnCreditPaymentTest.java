package ru.yandex.market.checkout.checkouter.returns;

import java.math.BigDecimal;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.ReturnService;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.providers.ReturnProvider;

import static org.apache.curator.shaded.com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static ru.yandex.market.checkout.util.matching.NumberMatcher.numberEqualsTo;

public class ReturnCreditPaymentTest extends AbstractPaymentTestBase {

    @Autowired
    private ReturnService returnService;
    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private OrderPayHelper paymentHelper;

    @BeforeEach
    public void createCreditOrder() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder(o -> o.setPaymentMethod(PaymentMethod.CREDIT)));
        paymentHelper.doStuffForSupplierPayment(newArrayList(order()));

        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();
        returnHelper.mockActualDelivery();
    }

    @Test
    public void testReturnCreditWithPercentCompensation() throws Exception {
        Return returnRequest = ReturnProvider.generateFullReturn(order.get());
        returnRequest.setUserCreditCompensationSum(BigDecimal.TEN);
        Return ret = returnHelper.createReturn(order.get().getId(), returnRequest);

        assertThat(ret, not(nullValue()));
        assertThat(ret.getUserCreditCompensationSum(), numberEqualsTo(BigDecimal.TEN));

        trustMockConfigurer.resetRequests();
        Collection<Payment> returnPayments = returnService.processCompensation(ret, order());

        Payment userPayment = returnPayments.stream().
                filter(p -> p.getType() == PaymentGoal.USER_COMPENSATION).findFirst().orElseThrow();
        assertThat(userPayment.getTotalAmount(), numberEqualsTo(ret.totalUserCompensation()));


        Payment paymentFromSupplier = returnPayments.stream()
                .filter(p -> p.getType() == PaymentGoal.MARKET_COMPENSATION).findFirst().orElseThrow();
        assertThat(paymentFromSupplier.getTotalAmount(), numberEqualsTo(
                ret.getItems().stream().map(ReturnItem::getSupplierCompensation)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)));

        returnTestHelper.checkReturnBallanceCalls(ret);
    }
}
