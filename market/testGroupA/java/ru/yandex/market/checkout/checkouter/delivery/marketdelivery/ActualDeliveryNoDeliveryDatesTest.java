package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.LocalDeliveryOption;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class ActualDeliveryNoDeliveryDatesTest extends AbstractWebTestBase {

    private static final Map<DeliveryType, Function<ActualDeliveryResult, LocalDeliveryOption>>
            ACTUAL_DELIVERY_GETTER_BY_TYPE =
            new HashMap<>() {{
                put(DeliveryType.DELIVERY, r -> Iterables.getOnlyElement(r.getDelivery()));
                put(DeliveryType.POST, r -> Iterables.getOnlyElement(r.getPost()));
                put(DeliveryType.PICKUP, r -> Iterables.getOnlyElement(r.getPickup()));
            }};

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @Test
    public void canCreateOrderWithoutDayFromForDelivery() {
        Parameters parameters = getParametersForDeliveryType(DeliveryType.DELIVERY, option -> option.setDayFrom(null));
        MultiCart cart = orderCreateHelper.cart(parameters);
        assertHasNoOptionsWithType(cart, DeliveryType.DELIVERY);

        orderCreateHelper.createOrder(parameters);
    }

    @Test
    public void canCreateOrderWithoutDayFromForPickup() {
        Parameters parameters = getParametersForDeliveryType(DeliveryType.PICKUP, option -> option.setDayFrom(null));
        MultiCart cart = orderCreateHelper.cart(parameters);
        assertHasNoOptionsWithType(cart, DeliveryType.PICKUP);

        orderCreateHelper.createOrder(parameters);
    }

    @Test
    public void canCreateOrderWithoutDayFromForPost() {
        Parameters parameters = getParametersForDeliveryType(DeliveryType.POST, option -> option.setDayFrom(null));
        MultiCart cart = orderCreateHelper.cart(parameters);
        assertHasNoOptionsWithType(cart, DeliveryType.POST);

        orderCreateHelper.createOrder(parameters);
    }

    @Test
    public void canCreateOrderWithoutDayToForDelivery() {
        Parameters parameters = getParametersForDeliveryType(DeliveryType.DELIVERY, option -> option.setDayTo(null));
        MultiCart cart = orderCreateHelper.cart(parameters);
        assertHasNoOptionsWithType(cart, DeliveryType.DELIVERY);

        orderCreateHelper.createOrder(parameters);
    }

    @Test
    public void canCreateOrderWithoutDayToForPickup() {
        Parameters parameters = getParametersForDeliveryType(DeliveryType.PICKUP, option -> option.setDayTo(null));
        MultiCart cart = orderCreateHelper.cart(parameters);
        assertHasNoOptionsWithType(cart, DeliveryType.PICKUP);

        orderCreateHelper.createOrder(parameters);
    }

    @Test
    public void canCreateOrderWithoutDayToForPost() {
        Parameters parameters = getParametersForDeliveryType(DeliveryType.POST, option -> option.setDayTo(null));
        MultiCart cart = orderCreateHelper.cart(parameters);
        assertHasNoOptionsWithType(cart, DeliveryType.POST);

        orderCreateHelper.createOrder(parameters);
    }

    private void assertHasNoOptionsWithType(MultiCart cart, DeliveryType type) {
        assertTrue(
                Iterables.getOnlyElement(
                        cart.getCarts()
                ).getDeliveryOptions()
                        .stream()
                        .allMatch(option -> option.getType() != type)
        );
    }

    private Parameters getParametersForDeliveryType(DeliveryType type,
                                                    Consumer<LocalDeliveryOption> deliveryDateModifier) {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withDeliveryType(
                        Arrays.stream(DeliveryType.values())
                                .filter(t -> t != type)
                                .filter(t -> t != DeliveryType.UNKNOWN)
                                .findAny()
                                .get()
                )
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(1)
                .buildParameters();
        deliveryDateModifier.accept(
                ACTUAL_DELIVERY_GETTER_BY_TYPE.get(type).apply(
                        Iterables.getOnlyElement(
                                parameters.getReportParameters().getActualDelivery().getResults()
                        )
                )
        );
        return parameters;
    }
}
