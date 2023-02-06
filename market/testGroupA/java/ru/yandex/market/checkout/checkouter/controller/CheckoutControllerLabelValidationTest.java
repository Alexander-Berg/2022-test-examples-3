package ru.yandex.market.checkout.checkouter.controller;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author zagidullinri
 * @date 30.09.2021
 */
public class CheckoutControllerLabelValidationTest extends AbstractWebTestBase {
    @Test
    public void cartShouldThrowExceptionWhenLabelsIsNotUnique() {
        Parameters parameters = prepareParametersWithLabel("1");
        parameters.addOrder(prepareParametersWithLabel("2"));
        parameters.addOrder(prepareParametersWithLabel("1"));

        parameters.setCheckCartErrors(false);
        parameters.setExpectedCartReturnCode(HttpStatus.BAD_REQUEST_400);
        orderCreateHelper.cart(parameters);
    }

    @Test
    public void cartShouldNotThrowExceptionWhenLabelsIsUniqueOrNull() {
        Parameters parameters = prepareParametersWithLabel("1");
        parameters.addOrder(prepareParametersWithLabel("2"));
        parameters.addOrder(prepareParametersWithLabel("3"));
        parameters.addOrder(prepareParametersWithLabel(null));
        parameters.addOrder(prepareParametersWithLabel(null));

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertEquals(5, cart.getCarts().size());
    }

    public Parameters prepareParametersWithLabel(String label) {
        Order order = OrderProvider.getBlueOrder();
        order.setLabel(label);
        return BlueParametersProvider.defaultBlueOrderParameters(order);
    }
}
