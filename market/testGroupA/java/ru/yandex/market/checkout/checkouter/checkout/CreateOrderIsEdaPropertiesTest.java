package ru.yandex.market.checkout.checkouter.checkout;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class CreateOrderIsEdaPropertiesTest extends AbstractWebTestBase {

    public static Stream<Arguments> parameterizedTestData() {
        return Stream.of(
                new Object[]{null, false},
                new Object[]{false, false},
                new Object[]{true, true}
        ).map(Arguments::of);
    }

    private static Parameters isEdaParameters(Boolean isEda) {
        Parameters parameters = new Parameters();
        parameters.setColor(Color.WHITE);
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.getReportParameters().setIsEda(isEda);
        parameters.setPushApiDeliveryResponse(DeliveryProvider.shopSelfDelivery()
                .buildResponse(DeliveryResponse::new));
        return parameters;
    }

    @ParameterizedTest(name = "IsEdaPropertyTest, reportIsEdaProperty = {0}, expectedOrderIsEdaProperty = {1}")
    @MethodSource("parameterizedTestData")
    public void isEdaPropertyTest(Boolean reportIsEdaParameter, boolean expectedOrderIsEdaProperty) {
        Order order = orderCreateHelper.createOrder(isEdaParameters(reportIsEdaParameter));
        Boolean isEdaProp = order.getProperty(OrderPropertyType.IS_EDA);
        assertThat("order.property.isEda", isEdaProp, allOf(notNullValue(), is(expectedOrderIsEdaProperty)));
    }
}
