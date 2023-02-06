package ru.yandex.market.checkout.checkouter.checkout;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

public class CreateOrderAcceptMethodTest extends AbstractWebTestBase {

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(new Object[][]{
                {OrderAcceptMethod.PUSH_API, ApiSettings.PRODUCTION},
                {OrderAcceptMethod.WEB_INTERFACE, ApiSettings.PRODUCTION},
                {OrderAcceptMethod.PUSHAPI_SANDBOX, ApiSettings.SANDBOX}
        }).stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldCreateOrderWithAcceptMethod(OrderAcceptMethod acceptMethod, ApiSettings apiSettings)
            throws Exception {
        Parameters parameters = BlueParametersProvider.clickAndCollectOrderParameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setApiSettings(apiSettings);
        parameters.setAcceptMethod(acceptMethod);
        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDelivery());

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertEquals(acceptMethod, order.getAcceptMethod());
    }
}
