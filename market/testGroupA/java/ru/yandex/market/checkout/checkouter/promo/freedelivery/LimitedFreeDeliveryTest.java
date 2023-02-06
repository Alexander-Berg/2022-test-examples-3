package ru.yandex.market.checkout.checkouter.promo.freedelivery;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.qameta.allure.Epic;
import io.qameta.allure.junit4.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.checkout.checkouter.promo.AbstractPromoTestBase;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.DELIVERY;
import static ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.DELIVERY_PRICE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;


public class LimitedFreeDeliveryTest extends AbstractPromoTestBase {

    private Parameters parameters;

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.stream(Platform.values()).map(v ->
                new Object[]{v}).collect(Collectors.toList()).stream().map(Arguments::of);
    }

    @BeforeEach
    public void init() {
        parameters = createParameters();
    }

    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем, как работает чекаут с разных платформ, если текущая дата попадает в интервал, указанный " +
            "в настройках")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void checkoutAtFreeDeliveryDates(Platform platform) {
        parameters.setPlatform(platform);
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addPickup(MOCK_DELIVERY_SERVICE_ID, 2, Collections.singletonList(12312303L))
                        .addDelivery(MOCK_DELIVERY_SERVICE_ID, 3)
                        .build()
        );
        parameters.setDeliveryType(DELIVERY);
        parameters.getOrder().getDelivery().setType(null);


        Order createdOrder = orderCreateHelper.createOrder(parameters);

        assertThat(createdOrder.getDelivery().getPromos(), anyOf(nullValue(), empty()));
        assertThat(createdOrder.getPromos(), anyOf(nullValue(), empty()));
        assertThat(createdOrder.getDelivery().getPrice(), equalTo(DELIVERY_PRICE));
    }

    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем, что чекаут с разных платформ работает без скидок, если текущая дата не попадает в " +
            "интервал, указанный в настройках")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void checkoutOutOfFreeDeliveryDates(Platform platform) {
        parameters.setPlatform(platform);
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addPickup(MOCK_DELIVERY_SERVICE_ID, 2, Collections.singletonList(12312303L))
                        .addDelivery(MOCK_DELIVERY_SERVICE_ID, 3).build()
        );
        parameters.setDeliveryType(DELIVERY);
        parameters.getOrder().getDelivery().setType(null);


        Order createdOrder = orderCreateHelper.createOrder(parameters);

        assertThat(createdOrder.getDelivery().getPromos(), anyOf(nullValue(), empty()));
        assertThat(createdOrder.getPromos(), anyOf(nullValue(), empty()));
        assertThat(createdOrder.getDelivery().getPrice(), equalTo(DELIVERY_PRICE));
    }

    @Tag(Tags.PROMO)
    @Epic(Epics.CHECKOUT)
    @DisplayName("Проверяем,что чекаут с разных платформ работает без скидок, интервал бесплатной доставки не найден " +
            "в настройках ")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void checkoutWithEmptyFreeDeliveryDates(Platform platform) {
        parameters.setPlatform(platform);
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addPickup(MOCK_DELIVERY_SERVICE_ID, 2, Collections.singletonList(12312303L))
                        .addDelivery(MOCK_DELIVERY_SERVICE_ID, 3).build()
        );
        parameters.setDeliveryType(DELIVERY);
        parameters.getOrder().getDelivery().setType(null);


        Order createdOrder = orderCreateHelper.createOrder(parameters);

        assertThat(createdOrder.getDelivery().getPromos(), anyOf(nullValue(), empty()));
        assertThat(createdOrder.getPromos(), anyOf(nullValue(), empty()));
        assertThat(createdOrder.getDelivery().getPrice(), equalTo(DELIVERY_PRICE));
    }
}
