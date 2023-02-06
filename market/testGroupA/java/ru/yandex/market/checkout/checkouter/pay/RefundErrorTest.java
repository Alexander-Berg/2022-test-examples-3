package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;

import static org.apache.commons.httpclient.HttpStatus.SC_BAD_REQUEST;
import static org.apache.commons.httpclient.HttpStatus.SC_NOT_FOUND;
import static org.apache.commons.httpclient.HttpStatus.SC_NO_CONTENT;
import static ru.yandex.market.checkout.checkouter.pay.RefundRequests.refundByAmount;
import static ru.yandex.market.checkout.checkouter.pay.RefundRequests.refundRequest;
import static ru.yandex.market.checkout.checkouter.pay.RefundRequests.refundUnknownOrderId;
import static ru.yandex.market.checkout.checkouter.pay.RefundRequests.refundWithEmptyBody;
import static ru.yandex.market.checkout.checkouter.pay.RefundRequests.refundWithUserRole;
import static ru.yandex.market.checkout.checkouter.pay.RefundRequests.refundWithoutReason;
import static ru.yandex.market.checkout.checkouter.pay.RefundRequests.refundWrongUid;
import static ru.yandex.market.checkout.checkouter.pay.RefundTestHelper.refundableItemsFromOrder;

/**
 * @author : poluektov
 * date: 11.07.17.
 */

public class RefundErrorTest extends AbstractPaymentTestBase {

    @Override
    @BeforeEach
    public void init() throws Exception {
        super.init();
        createUnpaidOrder();
        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.clearPayment();
    }

    @Epic(Epics.REFUND)
    @Story(Stories.REFUND)
    @DisplayName("Проверяем, что ручка /refund возвращает 404, если заказ не найден")
    @Test
    public void wrongOrderId() throws Exception {
        refundTestHelper.checkRefundError(refundUnknownOrderId(order()), SC_NOT_FOUND, "Order not found");
    }

    @Epic(Epics.REFUND)
    @DisplayName("Проверяем, что ручка /refund возвращает заказ не найден (404), " +
            "если пытаемся зарефандить не свой заказ")
    @Test
    public void wrongUserUid() throws Exception {
        refundTestHelper.checkRefundError(refundWrongUid(order()), SC_NOT_FOUND, "Order not found");
    }

    @Epic(Epics.REFUND)
    @DisplayName("Проверяем, что ручка /refund возвращает 400, если поле reason не задано")
    @Test
    public void withoutReason() throws Exception {
        refundTestHelper.checkRefundError(
                refundWithoutReason(order()), SC_BAD_REQUEST, "parameter 'reason' is not present"
        );
    }

    @Epic(Epics.REFUND)
    @DisplayName("Проверяем, что ручка /refund возвращает 400, если пытаемся зарефандить от пользовательской роли")
    @Test
    public void clientRoleUser() throws Exception {
        refundTestHelper.checkRefundError(
                refundWithUserRole(order()), SC_BAD_REQUEST, "no permission to create refund"
        );
    }

    @Epic(Epics.REFUND)
    @DisplayName("Проверяем, что ручка /refund возвращает 400, если пытаемся зарефандить определенное количество денег")
    @Test
    public void cantRefundAmount() throws Exception {
        refundTestHelper.checkRefundError(refundByAmount(order()), SC_BAD_REQUEST, "Cannot refund by amount");
    }

    @Epic(Epics.REFUND)
    @DisplayName("Проверяем, что ручка /refund возвращает 400, если пытаемся зарефандить, но не передаем тело")
    @Test
    public void refundEmptyBody() throws Exception {
        refundTestHelper.checkRefundError(
                refundWithEmptyBody(order()), SC_BAD_REQUEST, "Missing RefundItems"
        );
    }

    @Epic(Epics.REFUND)
    @DisplayName("Проверяем, что ручка /refund возвращает 400, если пытаемся зарефандить отмененный заказ")
    @Test
    public void testRefundCancelledOrder() throws Exception {
        createUnpaidOrder();
        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.cancelPayment();
        refundTestHelper.tryMakeRefundForItems(HttpStatus.SC_BAD_REQUEST);
    }

    @Epic(Epics.REFUND)
    @DisplayName("Проверяем, что ручка /refund возвращает 204, " +
            "если пытаемся зарефандить уже полностью зарефанженный заказ")
    @Test
    public void alreadyRefunded() throws Exception {
        checkouterProperties.setEnableServicesPrepay(true);
        RefundableItems refundBody = refundableItemsFromOrder(order());
        refundTestHelper.makeFullRefund();
        refundTestHelper.checkRefundError(
                refundRequest(refundBody, BigDecimal.ZERO, order(), RefundReason.ORDER_CHANGED, false),
                SC_NO_CONTENT, ""
        );
    }
}
