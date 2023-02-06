package ru.yandex.market.logistic.api.client.fulfillment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.response.PutMovementRegistryResponse;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.RequestValidationUtils;
import ru.yandex.market.logistic.api.utils.common.MovementDtoFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.logistic.api.utils.fulfillment.RegistryDtoFactory.createMinimalMovementRegistry;

class PutMovementRegistryTest extends CommonServiceClientTest {
    @Autowired
    private FulfillmentClient client;

    @Test
    void testSuccessfulResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "put_movement_registry/ff_request",
            "put_movement_registry/success",
            props.getUrl()
        );
        PutMovementRegistryResponse response =
            client.putMovementRegistry(createMinimalMovementRegistry(), props);
        assertThat(response.getRegistryId()).isEqualTo(MovementDtoFactory.createMovementId());
    }

    @Test
    void testErrorResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "put_movement_registry/ff_request",
            "put_movement_registry/error",
            props.getUrl()
        );
        assertions.assertThatThrownBy(
            () -> client.putMovementRegistry(createMinimalMovementRegistry(), props)
        )
            .hasMessage("an error has occurred")
            .isInstanceOf(RequestStateErrorException.class);
    }

    @Test
    void testMissingRegistry() {
        assertions.assertThatThrownBy(() -> client.putMovementRegistry(null, getPartnerProperties()))
            .hasMessage(RequestValidationUtils.getNotNullErrorMessage("registry"))
            .isInstanceOf(RequestValidationException.class);
    }
}
