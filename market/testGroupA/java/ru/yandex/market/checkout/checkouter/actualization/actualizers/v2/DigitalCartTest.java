package ru.yandex.market.checkout.checkouter.actualization.actualizers.v2;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.AvailableDeliveryType;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

public class DigitalCartTest extends AbstractWebTestBase {

    @Test
    void shouldActualizeDigitalCart() {
        var whiteParameters = WhiteParametersProvider.digitalOrderPrameters();
        var response = orderCreateHelper.multiCartActualize(whiteParameters);

        var availableDeliveryTypes = response.getCarts().get(0).getAvailableDeliveryTypes();

        assertThat(availableDeliveryTypes, hasSize(1));
        assertThat(availableDeliveryTypes, hasItems(AvailableDeliveryType.DIGITAL));

    }
}
