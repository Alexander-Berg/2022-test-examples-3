package ru.yandex.market.checkout.experements;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NoPushApiRequestTest extends AbstractWebTestBase {

    @Autowired
    private WireMockServer pushApiMock;

    @Test
    public void shouldNotCallPushApiForBlueFulfilment() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);

        assertEquals(Color.BLUE, order.getRgb());
        assertEquals(Boolean.TRUE, order.isFulfilment());

        pushApiMock.verify(0, postRequestedFor(urlPathEqualTo("/shops/" + order.getShopId() + "/cart")));
        pushApiMock.verify(0, postRequestedFor(urlPathEqualTo("/shops/" + order.getShopId() + "/accept")));
    }
}
