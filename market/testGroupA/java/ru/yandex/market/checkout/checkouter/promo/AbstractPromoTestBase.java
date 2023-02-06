package ru.yandex.market.checkout.checkouter.promo;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.util.fulfillment.FulfillmentConfigurer;

public class AbstractPromoTestBase extends AbstractWebTestBase {

    @Autowired
    protected FulfillmentConfigurer fulfillmentConfigurer;

    protected static void patchAddressForFreeDelivery(Parameters parameters) {
        // льва толстого
        parameters.getOrder().getDelivery().setBuyerAddress(
                // я тут для того, чтобы вызывать cache miss в клиенте геокодера,
                // который в тестах включен для ускорения тестирования
                AddressProvider.getAnotherAddress(
                        a -> a.setStreet(a.getStreet() + UUID.randomUUID().toString())
                )
        );
        // и координаты Красной Розы
        parameters.getGeocoderParameters().setGps("37.588244 55.733742");
    }


    @BeforeEach
    public void setUp() throws Exception {
        setFixedTime(Instant.parse("2027-07-04T05:15:30.00Z"));
    }

    protected Parameters createParameters() {
        return createParameters(null);
    }

    protected Parameters createParameters(Consumer<Parameters> configurer) {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        if (configurer != null) {
            configurer.accept(parameters);
        }
        return parameters;
    }
}
