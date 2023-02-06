package ru.yandex.autotests.market.checkouter.api.orders.returns;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.checkouter.beans.PaymentMethod;
import ru.yandex.autotests.market.checkouter.beans.PaymentType;
import ru.yandex.autotests.market.checkouter.client.body.request.checkout.CheckoutRequestBody;
import ru.yandex.autotests.market.checkouter.client.body.response.CheckoutResponseBody;
import ru.yandex.autotests.market.checkouter.client.checkouter.CheckoutApiRequest;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.market.checkouter.api.data.requests.checkout.CheckoutRequests.blueOrderRequestForSupplier;

/**
 * @author : poluektov
 * date: 25.04.18.
 */
@Aqua.Test(title = "Создание синих возвратов")
@Features("BLUE")
@Stories("Returns")
public class OrderReturnTest extends AbstractReturnTest {

    @Test
    public void prepayReturnTest() {
        CheckoutApiRequest<CheckoutRequestBody, CheckoutResponseBody> request =
                blueOrderRequestForSupplier(supplierRule.getSupplierFeedId(), supplierRule.getRegionId(), PaymentType.PREPAID, PaymentMethod.YANDEX);
        createOrderAndReturn(request);
    }

    @Test
    public void postpayReturnTest() {
        CheckoutApiRequest<CheckoutRequestBody, CheckoutResponseBody> checkoutRequest =
                blueOrderRequestForSupplier(supplierRule.getSupplierFeedId(), supplierRule.getRegionId(), PaymentType.POSTPAID, PaymentMethod.YANDEX);
        createOrderAndReturn(checkoutRequest);
    }

}
