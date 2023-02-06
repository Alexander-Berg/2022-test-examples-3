package ru.yandex.market.checkout.checkouter.antifraud;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;

public class AntifraudRequestTest extends AbstractWebTestBase {

    @Test
    void shouldSendPriceFromReport() {
        var params = BlueParametersProvider.defaultBlueOrderParameters();

        orderCreateHelper.cart(params);

        var events = mstatAntifraudConfigurer.collectDetectEvents();

        assertThat(events, hasSize(1));
        int price = JsonPath.read(events.get(0).getBodyAsString(), "$.items[0].price");

        assertThat(price, comparesEqualTo(250));
    }
}
