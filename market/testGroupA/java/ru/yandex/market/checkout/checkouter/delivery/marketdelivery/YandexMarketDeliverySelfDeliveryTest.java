package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.common.report.model.ActualDelivery;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;


public class YandexMarketDeliverySelfDeliveryTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(
                new Object[]{
                        builder()
                                .addDelivery(99L)
                                .build(),
                        DeliveryType.DELIVERY
                },
                new Object[]{
                        builder()
                                .addPost(2, 99L)
                                .build(),
                        DeliveryType.POST
                },
                new Object[]{
                        builder()
                                .addPickup(99L)
                                .build(),
                        DeliveryType.PICKUP
                }
        ).stream().map(Arguments::of);
    }

    private static ActualDeliveryProvider.ActualDeliveryBuilder builder() {
        return ActualDeliveryProvider.builder()
                .addDelivery(BlueParametersProvider.DELIVERY_SERVICE_ID);
    }

    @ParameterizedTest(name = "shouldFilterOutSelfDeliveryFromActualDelivery_{1}")
    @MethodSource("parameterizedTestData")
    public void shouldFilterOutSelfDeliveryFromActualDelivery(ActualDelivery actualDelivery,
                                                              DeliveryType deliveryType) {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withActualDelivery(actualDelivery)
                .buildParameters();
        parameters.setNullPushApiDeliveryResponse();

        MultiCart cart = orderCreateHelper.cart(parameters);
        Order order = cart.getCarts().get(0);
        assertThat(
                order.getDeliveryOptions().stream()
                        .filter(it -> it.getDeliveryServiceId() == 99L
                                && it.getDeliveryPartnerType() == DeliveryPartnerType.YANDEX_MARKET
                                && it.getType() == deliveryType)
                        .collect(Collectors.toList()), empty()
        );
    }
}
