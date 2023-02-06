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
import ru.yandex.market.common.report.model.FoodtechType;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class CreateOrderIsEatsPropertiesTest extends AbstractWebTestBase {

    public static Stream<Arguments> parameterizedTestData() {
        return Stream.of(
                new Object[]{null, false},
                new Object[]{"anyOtherType", false},
                new Object[]{FoodtechType.LAVKA.getId(), true},
                new Object[]{FoodtechType.EDA_RETAIL.getId(), true},
                new Object[]{FoodtechType.EDA_RESTAURANTS.getId(), true}
        ).map(Arguments::of);
    }

    private static Parameters createParameters(String foodtechType) {
        Parameters parameters = new Parameters();
        parameters.getReportParameters().setFoodtechType(foodtechType);
        parameters.setColor(Color.WHITE);
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.setPushApiDeliveryResponse(DeliveryProvider.shopSelfDelivery()
                .buildResponse(DeliveryResponse::new));
        return parameters;
    }

    @ParameterizedTest(name = "IsEatsPropertyTest, reportFoodtechTypeParam = {0}, expectedOrderIsEatsProp = {1}")
    @MethodSource("parameterizedTestData")
    public void isEatsPropertyTest(String reportFoodtechTypeParam, boolean expectedOrderIsEatsProp) {
        Order order = orderCreateHelper.createOrder(createParameters(reportFoodtechTypeParam));
        Boolean isEdaProp = order.getProperty(OrderPropertyType.IS_EATS);
        assertThat("order.property.isEats", isEdaProp, allOf(notNullValue(), is(expectedOrderIsEatsProp)));
    }
}
