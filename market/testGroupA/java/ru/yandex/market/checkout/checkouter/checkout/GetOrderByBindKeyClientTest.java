package ru.yandex.market.checkout.checkouter.checkout;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.auth.AuthInfo;
import ru.yandex.market.checkout.checkouter.auth.AuthService;
import ru.yandex.market.checkout.checkouter.auth.UserInfo;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.HitRateGroup;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;

public class GetOrderByBindKeyClientTest extends AbstractWebTestBase {

    private static final String BIND_KEY = "bindKey";

    @Autowired
    private AuthService authService;

    @Test
    @Disabled
    public void shouldCreateAndGetNoAuthOrder() throws Exception {
        // given
        AuthInfo authInfo = authService.auth(
                null,
                new UserInfo("127.0.0.1", "java 1.8.0"),
                HitRateGroup.LIMIT, false);
        Long muid = authInfo.getMuid();
        String muidCookie = authInfo.getCookie();

        Parameters parameters = new Parameters();
        parameters.getBuyer().setUid(muid);
        parameters.getBuyer().setBindKey(BIND_KEY);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        MultiCart multiCart = parameters.getBuiltMultiCart();

        // create
        orderCreateHelper.initializeMock(parameters);
        MultiCart cart = client.cart(multiCart, muid, false, Context.MARKET, ApiSettings.PRODUCTION, null,
                HitRateGroup.LIMIT, null, true);
        MultiOrder multiOrder = orderCreateHelper.mapCartToOrder(cart, parameters);
        pushApiConfigurer.mockAccept(multiOrder.getCarts().get(0));

        // get
        Order order = client.getOrderByBindKey(BIND_KEY, null, null, muidCookie, true, null);

        // then
        Assertions.assertTrue(order.isNoAuth());
        Assertions.assertEquals(muid, order.getBuyer().getUid());
        Assertions.assertEquals(muid, order.getBuyer().getMuid());
    }
}
