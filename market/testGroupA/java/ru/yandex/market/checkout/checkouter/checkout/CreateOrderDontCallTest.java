package ru.yandex.market.checkout.checkouter.checkout;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Features;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;

public class CreateOrderDontCallTest extends AbstractWebTestBase {

    /**
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-67
     */
    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS)
    @Feature(Features.FULFILLMENT)
    @DisplayName("Проверить dontCall")
    @Test
    public void shouldCreateOrderWithoutDontCallFlag() throws Exception {
        Parameters parameters = new Parameters();
        parameters.getOrder().getBuyer().setDontCall(false);

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertFalse(order.getBuyer().isDontCall());
    }

    /**
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-67
     */
    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS)
    @Feature(Features.FULFILLMENT)
    @DisplayName("Проверить dontCall")
    @Test
    public void shouldCreateOrderWithDontCallFlag() throws Exception {
        Parameters parameters = new Parameters();
        parameters.getOrder().getBuyer().setDontCall(true);

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertTrue(order.getBuyer().isDontCall());
    }
}
