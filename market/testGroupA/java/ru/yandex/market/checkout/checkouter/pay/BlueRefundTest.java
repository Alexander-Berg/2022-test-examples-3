package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.ReturnHelper;

import static org.apache.commons.httpclient.HttpStatus.SC_BAD_REQUEST;
import static ru.yandex.market.checkout.checkouter.pay.RefundRequests.refundRequest;
import static ru.yandex.market.checkout.checkouter.pay.RefundTestHelper.refundableItemsFromOrder;

public class BlueRefundTest extends AbstractPaymentTestBase {

    @Autowired
    private ReturnHelper returnHelper;

    @BeforeEach
    public void prepareOrder() throws Exception {
        createUnpaidBlueOrder();
        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.clearPayment();
        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();
    }

    @Epic(Epics.REFUND)
    @Story(Stories.REFUND)
    @DisplayName("Проверяем, что нельзя сделать рефанд синего заказа в DELIVERED")
    @Test
    public void doesNotAllowToRefundBlueDeliveredOrder() throws Exception {
        Order order = orderStatusHelper.proceedOrderToStatus(order(), OrderStatus.DELIVERED);
        refundTestHelper.checkRefundError(
                refundRequest(refundableItemsFromOrder(order()), BigDecimal.ZERO, order(), RefundReason.ORDER_CHANGED,
                        false),
                SC_BAD_REQUEST,
                String.format("Can't refund DELIVERED order(%d) in BLUE market (when returns are enabled)",
                        order.getId())
        );
    }
}
