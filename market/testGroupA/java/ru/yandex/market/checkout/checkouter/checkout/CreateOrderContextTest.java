package ru.yandex.market.checkout.checkouter.checkout;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;

//FIXME переделать или убрать
@Deprecated
@Disabled
public class CreateOrderContextTest extends AbstractWebTestBase {

    @ParameterizedTest
    @EnumSource(value = Context.class, names = {"MARKET", "SANDBOX", "SELF_CHECK", "CHECK_ORDER"})
    public void testCreateOrderWithContext(Context context) {
        Parameters parameters = new Parameters();
        parameters.setContext(context);

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertEquals(context, order.getContext());


        pushApiConfigurer.assertCartOrder(
                allOf(
                        hasProperty("shopId", equalTo(order.getShopId())),
                        hasProperty("context", equalTo(context))
                )
        );

        pushApiConfigurer.assertAcceptOrder(
                allOf(
                        hasProperty("shopId", equalTo(order.getShopId())),
                        hasProperty("context", equalTo(context))
                )
        );
    }
}
