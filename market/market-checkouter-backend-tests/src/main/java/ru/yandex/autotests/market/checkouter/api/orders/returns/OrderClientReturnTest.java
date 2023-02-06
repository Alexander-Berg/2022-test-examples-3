package ru.yandex.autotests.market.checkouter.api.orders.returns;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.checkouter.beans.PaymentMethod;
import ru.yandex.autotests.market.checkouter.beans.PaymentType;
import ru.yandex.autotests.market.checkouter.client.body.request.checkout.CheckoutRequestBody;
import ru.yandex.autotests.market.checkouter.client.body.response.CheckoutResponseBody;
import ru.yandex.autotests.market.checkouter.client.body.response.order.payment.OrderReturnOptionsResponseBody;
import ru.yandex.autotests.market.checkouter.client.body.response.order.payment.OrderReturnResponseBody;
import ru.yandex.autotests.market.checkouter.client.checkouter.CheckoutApiRequest;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.market.checkouter.api.data.requests.checkout.CheckoutRequests.blueOrderRequestForSupplier;
import static ru.yandex.autotests.market.common.steps.AssertSteps.assertStep;

@Aqua.Test(title = "Создание клиентских возвратов")
@Features("BLUE")
@Stories("Returns")
public class OrderClientReturnTest extends AbstractReturnTest {

    @Title("Создание и завершение клиентского возврата")
    @Test
    public void clientReturnTest() {
        CheckoutApiRequest<CheckoutRequestBody, CheckoutResponseBody> request = blueOrderRequestForSupplier(
                supplierRule.getSupplierFeedId(),
                supplierRule.getRegionId(),
                PaymentType.PREPAID,
                PaymentMethod.YANDEX
        );
        order = createAndProcessOrderToDelivered(request);
        OrderReturnOptionsResponseBody response = returnSteps.getReturnOptions(order);
        assertStep(response.getDeliveryOptions(), hasSize(greaterThan(0)));

        OrderReturnResponseBody returnResponse = returnSteps.createClientOrderReturn(order,
                response.getDeliveryOptions().get(0));
        assertStep(returnResponse.getId(), notNullValue());
        assertStep(returnResponse.getStatus(), equalTo("STARTED_BY_USER"));
        returnResponse = returnSteps.resumeClientOrderReturn(order.getId(), returnResponse.getId());
        assertStep(returnResponse.getId(), notNullValue());
        assertStep(returnResponse.getStatus(), equalTo("REFUND_IN_PROGRESS"));
    }

}
