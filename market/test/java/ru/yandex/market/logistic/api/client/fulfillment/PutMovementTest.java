package ru.yandex.market.logistic.api.client.fulfillment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.response.PutMovementResponse;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.RequestValidationUtils;
import ru.yandex.market.logistic.api.utils.common.MovementDtoFactory;

public class PutMovementTest extends CommonServiceClientTest {

    @Autowired
    private FulfillmentClient client;

    @Test
    void testSuccessfulResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("put_movement", props.getUrl());
        PutMovementResponse response = client.putMovement(MovementDtoFactory.createMovement(), props);
        assertions.assertThat(response).isEqualTo(MovementDtoFactory.createPutMovementResponseFF());
    }

    @Test
    void testErrorResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("put_movement", "put_movement_with_errors", props.getUrl());
        assertions.assertThatThrownBy(() -> client.putMovement(MovementDtoFactory.createMovement(), props))
            .hasMessage("Omg something terrible happened")
            .isInstanceOf(RequestStateErrorException.class);
    }

    @Test
    void testMissingInterval() {
        assertions.assertThatThrownBy(
            () -> client.putMovement(MovementDtoFactory.createMovementWithoutInterval(), getPartnerProperties())
        )
            .hasMessage(RequestValidationUtils.getNotNullErrorMessage("movement.interval"))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void testMissingVolume() {
        assertions.assertThatThrownBy(
            () -> client.putMovement(MovementDtoFactory.createMovementWithoutVolume(), getPartnerProperties())
        )
            .hasMessage(RequestValidationUtils.getNotNullErrorMessage("movement.volume"))
            .isInstanceOf(RequestValidationException.class);
    }

}
