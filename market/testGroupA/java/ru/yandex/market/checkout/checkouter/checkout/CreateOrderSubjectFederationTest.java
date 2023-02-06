package ru.yandex.market.checkout.checkouter.checkout;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class CreateOrderSubjectFederationTest extends AbstractWebTestBase {

    public static final long SUBJECT_FEDERATION_REGION_ID = 98582L;

    @Test
    void shouldAllowToCreateOrderToRegionWithTypeSubjectFederation() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.getOrder().getDelivery().setRegionId(SUBJECT_FEDERATION_REGION_ID);

        orderCreateHelper.createOrder(parameters);
    }
}
