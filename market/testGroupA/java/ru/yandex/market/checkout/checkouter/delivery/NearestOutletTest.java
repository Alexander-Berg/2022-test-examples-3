package ru.yandex.market.checkout.checkouter.delivery;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.outlet.NearestOutlet;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;

public class NearestOutletTest extends AbstractWebTestBase {

    @BeforeEach
    public void before() {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndGps();
    }

    @Test
    public void sendGpsLocationInActualDeliveryRequest() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        orderCreateHelper.cart(parameters);
        List<String> urls = reportMock.getAllServeEvents().stream()
                .filter(e -> e.getRequest().queryParameter("place").firstValue().equals("actual_delivery"))
                .map(e -> e.getRequest().getUrl())
                .collect(Collectors.toList());

        assertThat(urls, everyItem(containsString("geo-location")));
    }

    @Test
    public void cartReturnNearestOutletField() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getCarts(), everyItem(hasProperty("nearestOutlet", notNullValue())));
        NearestOutlet nearestOutlet = multiCart.getCarts().get(0).getNearestOutlet();
        assertThat(nearestOutlet, notNullValue());
        assertThat(nearestOutlet.getId(), equalTo(12345L));
        assertThat(nearestOutlet.getGps(), equalTo("12.1,24.2"));
    }

}
