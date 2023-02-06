package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;

public class CreateOrderSmallWeightTest extends AbstractWebTestBase {

    @Test
    void shouldRoundSmallWeightsUpTo1Gram() {
        Parameters parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        parameters.setWeight(new BigDecimal("0.00028"));

        Order order = orderCreateHelper.createOrder(parameters);
        MatcherAssert.assertThat(order.getItems().iterator().next().getWeight(), CoreMatchers.equalTo(1L));
        MatcherAssert.assertThat(order.getItems().iterator().next().getWeightInKilo(),
                Matchers.comparesEqualTo(new BigDecimal("0.001")));
    }

    @Test
    void shouldNotRoundZeroUpTo1Gram() {
        Parameters parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        parameters.setWeight(BigDecimal.ZERO);

        Order order = orderCreateHelper.createOrder(parameters);
        MatcherAssert.assertThat(order.getItems().iterator().next().getWeight(), CoreMatchers.equalTo(0L));
        MatcherAssert.assertThat(order.getItems().iterator().next().getWeightInKilo(),
                Matchers.comparesEqualTo(BigDecimal.ZERO));
    }
}
