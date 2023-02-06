package ru.yandex.autotests.market.checkouter.api.red;

import org.apache.commons.lang3.ObjectUtils;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.checkouter.api.steps.RefundSteps;
import ru.yandex.autotests.market.checkouter.beans.ClientRole;
import ru.yandex.autotests.market.checkouter.beans.Status;
import ru.yandex.autotests.market.checkouter.beans.orders.pay.Payment;
import ru.yandex.autotests.market.checkouter.beans.orders.refund.Refund;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataOrder;
import ru.yandex.autotests.market.checkouter.client.body.request.payment.OrderRefundRequestBody;
import ru.yandex.autotests.market.checkouter.client.body.response.CheckBasketResponseBody;
import ru.yandex.autotests.market.checkouter.client.body.response.order.OrderRefundResponseBody;
import ru.yandex.autotests.market.checkouter.client.body.response.order.OrderResponseBody;
import ru.yandex.autotests.market.checkouter.client.checkouter.CheckoutApiRequest;
import ru.yandex.autotests.market.checkouter.utils.CheckouterNumberUtils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;

import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.market.checkouter.api.data.requests.orders.refund.RefundRequests.amountRefundRequest;
import static ru.yandex.autotests.market.common.steps.AssertSteps.assertStep;

/**
 * @author kukabara
 */
@Aqua.Test(title = "Возврат для Красного заказа")
@Features("RED")
@Issue("https://st.yandex-team.ru/MARKETCHECKOUT-6729")
public class RefundRedMultiOrderTest extends AbstractRedOrderTest {

    private static final int SCALE = 0;
    private final RefundSteps refunds = new RefundSteps();

    @Test
    public void refundAmountSeveralTimes() throws Exception {
        sandbox = false;
        List<TestDataOrder> orders = processRedMultiOrder(Status.DELIVERY, Status.CANCELLED);

        TestDataOrder order = orders.get(0);

        Double currentRefundAmount = getOrderRefundAmount(order.getId());
        Payment payment = ordersSteps.getOrderById(order.getId()).getPayment();

        Double refundAmount2 = CheckouterNumberUtils.scaleDouble(order.getItems().get(0).getBuyerPrice(), SCALE, RoundingMode.DOWN);
        Double refundAmount1 = CheckouterNumberUtils.scaleDouble(order.getBuyerTotal() - refundAmount2, SCALE, RoundingMode.DOWN);
        checkRefund(order, payment, refundAmount1, currentRefundAmount + refundAmount1);
        checkRefund(order, payment, refundAmount2, currentRefundAmount + refundAmount1 + refundAmount2);
    }

    private void checkRefund(TestDataOrder order, Payment payment, Double amount, Double orderRefundAmount) {
        CheckoutApiRequest<OrderRefundRequestBody, OrderRefundResponseBody> request =
                amountRefundRequest(order, ClientRole.SHOP, order.getBuyer().getUid(), amount);
        Refund refund = refunds.refund(request);
        checkRefundAmount(amount, orderRefundAmount, refund, payment, order.getId());
    }

    @Step("Проверяем, что сумма рефанда = {0}, а сумма всех рефандов в заказе и Балансе = {1}")
    private void checkRefundAmount(Double expectedAmount, Double orderAmount,
                                   Refund refund, Payment payment, long orderId) {
        assertStep("Сумма возврата в ответе", refund.getAmount(), equalTo(expectedAmount));
        assertStep("Возврат сохранился в заказе", getOrderRefundAmount(orderId), equalTo(orderAmount));

        CheckBasketResponseBody basket = paySteps.checkBasket(payment);
        assertStep("В Балансе есть рефанд " + expectedAmount,
                Arrays.stream(basket.getRefunds()).anyMatch(r -> r.getAmount() == expectedAmount), is(true)
        );
    }

    private Double getOrderRefundAmount(long orderId) {
        OrderResponseBody ordersResponse = ordersSteps.getOrderById(orderId);
        return ObjectUtils.defaultIfNull(ordersResponse.getRefundActual(), 0d) +
                ObjectUtils.defaultIfNull(ordersResponse.getRefundPlanned(), 0d);
    }

}
