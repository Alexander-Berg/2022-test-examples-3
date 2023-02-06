package ru.yandex.market.logistic.api.client.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.exceptions.ResponseValidationException;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.response.CancelMovementResponse;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.RequestValidationUtils;
import ru.yandex.market.logistic.api.utils.ResponseValidationUtils;
import ru.yandex.market.logistic.api.utils.common.MovementDtoFactory;

class CancelMovementTest extends CommonServiceClientTest {
    @Autowired
    private DeliveryServiceClient client;

    @Test
    void testSuccessfulResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "cancel_movement",
            "cancel_movement/success",
            props.getUrl()
        );
        CancelMovementResponse response = client.cancelMovement(MovementDtoFactory.createMovementId(), props);
        assertions.assertThat(response.getMovementId()).isEqualTo(MovementDtoFactory.createMovementId());
    }

    @Test
    void testErrorResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "cancel_movement",
            "cancel_movement/with_errors",
            props.getUrl()
        );
        assertions.assertThatThrownBy(() -> client.cancelMovement(MovementDtoFactory.createMovementId(), props))
            .hasMessage("error during cancel movement method")
            .isInstanceOf(RequestStateErrorException.class);
    }

    @Test
    void testMissingMovementId() {
        assertions.assertThatThrownBy(() -> client.cancelMovement(null, getPartnerProperties()))
            .hasMessage(RequestValidationUtils.getNotNullErrorMessage("movementId"))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void testRequestMissingPartnerId() {
        // partnerId is null
        assertions.assertThatThrownBy(() -> client.cancelMovement(
            new ResourceId(null, null), getPartnerProperties())
        )
            .hasMessage(RequestValidationUtils.getNotBlankErrorMessage("movementId.partnerId"))
            .isInstanceOf(RequestValidationException.class);

        // partnerId is blank
        assertions.assertThatThrownBy(() -> client.cancelMovement(
            new ResourceId(null, ""), getPartnerProperties())
        )
            .hasMessage(RequestValidationUtils.getNotBlankErrorMessage("movementId.partnerId"))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void testResponseMissingPartnerId() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "cancel_movement",
            "cancel_movement/without_partner_id",
            props.getUrl()
        );
        assertions.assertThatThrownBy(() -> client.cancelMovement(MovementDtoFactory.createMovementId(), props))
            .hasMessageContaining(
                ResponseValidationUtils.getNotBlankErrorMessage(
                    "delivery",
                    "CancelMovementResponse",
                    "movementId.partnerId"))
            .isInstanceOf(ResponseValidationException.class);
    }
}
