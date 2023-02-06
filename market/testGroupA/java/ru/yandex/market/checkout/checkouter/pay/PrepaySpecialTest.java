package ru.yandex.market.checkout.checkouter.pay;

import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.TrustTestHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.PaymentParameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.json.JsonTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.checkout.common.util.ChainCalls.safe;

/**
 * @author sergeykoles
 * Created on: 21.03.19
 */
public class PrepaySpecialTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;

    @Test
    public void testReturnPathPaymentIdSubstitution() {
        // в этом тесте специально забито значение константы со строкой !PAYMENT!,
        // чтобы при её изменении было понятно, что ломается обратная совместимость

        String returnPathPattern = "be4d4c5c-5c14-4bd4-b655-d3ba4462e435/" +
                "!PAYMENT_ID!/payment/!PAYMENT_ID!/buzz";

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        final Order createdOrder = orderCreateHelper.createOrder(parameters);
        final Order unpaidOrder = orderStatusHelper.proceedOrderToStatus(createdOrder, OrderStatus.UNPAID);

        final PaymentParameters paymentParameters = new PaymentParameters();
        paymentParameters.setUid(unpaidOrder.getBuyer().getUid());
        paymentParameters.setReturnPath(returnPathPattern);
        final Payment payment = orderPayHelper.pay(unpaidOrder.getId(), paymentParameters);
        final List<String> bodies = trustMockConfigurer.servedEvents().stream()
                .filter(TrustTestHelper.WireMockPredicates.createPaymentEvent())
                .map(se -> safe(se, ServeEvent::getRequest, LoggedRequest::getBodyAsString, "{}"))
                .collect(Collectors.toList());
        assertThat(bodies, hasSize(greaterThanOrEqualTo(1)));
        bodies.forEach(body ->
                JsonTest.checkJson(body,
                        "$.return_path",
                        returnPathPattern.replaceAll("!PAYMENT_ID!", "" + payment.getId())));
    }
}
