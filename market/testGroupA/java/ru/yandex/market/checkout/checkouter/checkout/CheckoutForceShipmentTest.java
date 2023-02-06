package ru.yandex.market.checkout.checkouter.checkout;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CheckoutForceShipmentTest extends AbstractWebTestBase {

    @Autowired
    private WireMockServer reportMock;

    @Test
    public void shouldRequestForceShipmentDay() throws Exception {
        long forceShipmentDay = 113L;
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setForceShipmentDay(forceShipmentDay);

        MultiOrder order = orderCreateHelper.mapCartToOrder(orderCreateHelper.cart(parameters), parameters);
        reportMock.resetRequests();
        orderCreateHelper.checkout(order, parameters);

        assertEquals(
                reportMock.findAll(
                        getRequestedFor(anyUrl())
                                .withQueryParam(
                                        "inlet-shipment-day", WireMock.equalTo(String.valueOf(forceShipmentDay))
                                )
                ).size(),
                2
        );
    }

    @Test
    public void shouldUseForceShipmentDayInCart() throws Exception {
        long forceShipmentDay = 113L;

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setForceShipmentDay(forceShipmentDay);

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertEquals(
                reportMock.findAll(
                        getRequestedFor(anyUrl())
                                .withQueryParam("inlet-shipment-day",
                                        WireMock.equalTo(String.valueOf(forceShipmentDay)))
                ).size(),
                1 // второй вызов только если уже выбрана доставка
        );
    }
}
