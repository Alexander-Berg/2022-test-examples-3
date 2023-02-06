package ru.yandex.market.checkout.checkouter.returns;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.rest.InvalidRequestException;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.providers.ReturnProvider;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReturnTinkoffCreditTest extends AbstractPaymentTestBase {

    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private OrderPayHelper paymentHelper;

    @BeforeEach
    public void createCreditOrder() {
        order.set(
                orderServiceTestHelper.createDeliveredBlueOrder(o -> o.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT))
        );

        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();
        returnHelper.mockActualDelivery();
    }

    @Test
    void testNonZeroUserCompensationReturn() {
        Return returnRequest = ReturnProvider.generateFullReturn(order.get());
        returnRequest.setUserCompensationSum(BigDecimal.ONE);

        assertThrows(InvalidRequestException.class,
                () -> returnHelper.createReturn(order.get().getId(), returnRequest));

    }

}
