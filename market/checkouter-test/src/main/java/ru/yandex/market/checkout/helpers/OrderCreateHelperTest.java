package ru.yandex.market.checkout.helpers;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Nikolai Iusiumbeli
 * date: 18/07/2017
 */
public class OrderCreateHelperTest extends AbstractWebTestBase {

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Создание заказа с параметрами по умолчанию")
    @Test
    public void createOrder() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        assertThat(order.getId(), notNullValue());
    }
}
