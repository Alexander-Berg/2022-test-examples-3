package ru.yandex.market.logistic.api.client.delivery;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.model.common.OutboundType;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.response.GetMovementResponse;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.RequestValidationUtils;
import ru.yandex.market.logistic.api.utils.common.MovementDtoFactory;

class GetMovementTest extends CommonServiceClientTest {
    @Autowired
    private DeliveryServiceClient client;

    @Test
    void testSuccessfulResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_movement",
            "get_movement/get_movement_DS",
            props.getUrl()
        );
        GetMovementResponse movementResponse = client.getMovement(MovementDtoFactory.createMovementId(), props);
        assertions.assertThat(movementResponse.getMovement().getTags())
                .as("Should deserialize tags")
                .isEqualTo(Arrays.asList(OutboundType.ORDERS_RETURN, OutboundType.WH2WHDMG));
        assertions.assertThat(movementResponse.getCourier().getCar().getBrand()).isEqualTo("SCANIA");
        assertions.assertThat(movementResponse.getCourier().getCar().getModel()).isEqualTo("MODEL");
    }

    @Test
    void testErrorResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_movement",
            "get_movement/get_movement_with_errors",
            props.getUrl()
        );
        assertions.assertThatThrownBy(() -> client.getMovement(MovementDtoFactory.createMovementId(), props))
            .hasMessage("Omg something terrible happened")
            .isInstanceOf(RequestStateErrorException.class);
    }

    @Test
    void testMissingMovementId() {
        assertions.assertThatThrownBy(() -> client.getMovement(null, getPartnerProperties()))
            .hasMessage(RequestValidationUtils.getNotNullErrorMessage("movementId"))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void testMissingPartnerId() {
        // partnerId is null
        assertions.assertThatThrownBy(() -> client.getMovement(
            new ResourceId(null, null), getPartnerProperties())
        )
            .hasMessage(RequestValidationUtils.getNotBlankErrorMessage("movementId.partnerId"))
            .isInstanceOf(RequestValidationException.class);

        // partnerId is blank
        assertions.assertThatThrownBy(() -> client.getMovement(
            new ResourceId(null, ""), getPartnerProperties())
        )
            .hasMessage(RequestValidationUtils.getNotBlankErrorMessage("movementId.partnerId"))
            .isInstanceOf(RequestValidationException.class);
    }
}
