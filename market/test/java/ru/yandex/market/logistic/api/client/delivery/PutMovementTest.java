package ru.yandex.market.logistic.api.client.delivery;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.model.delivery.response.PutMovementResponse;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.RequestValidationUtils;
import ru.yandex.market.logistic.api.utils.common.MovementDtoFactory;

public class PutMovementTest extends CommonServiceClientTest {

    @Autowired
    private DeliveryServiceClient client;

    @Test
    void testSuccessfulResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("put_movement", props.getUrl());
        PutMovementResponse response = client.putMovement(MovementDtoFactory.createMovement(), null,  props);
        assertions.assertThat(response).isEqualTo(MovementDtoFactory.createPutMovementResponseDs());
    }

    @Test
    void testErrorResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("put_movement", "put_movement_with_errors", props.getUrl());
        assertions.assertThatThrownBy(() -> client.putMovement(MovementDtoFactory.createMovement(), null, props))
            .hasMessage("Omg something terrible happened")
            .isInstanceOf(RequestStateErrorException.class);
    }

    @Test
    void testMissingInterval() {
        assertions.assertThatThrownBy(
            () -> client.putMovement(MovementDtoFactory.createMovementWithoutInterval(), null, getPartnerProperties())
        )
            .hasMessage(RequestValidationUtils.getNotNullErrorMessage("movement.interval"))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void testMissingVolume() {
        assertions.assertThatThrownBy(
            () -> client.putMovement(MovementDtoFactory.createMovementWithoutVolume(), null, getPartnerProperties())
        )
            .hasMessage(RequestValidationUtils.getNotNullErrorMessage("movement.volume"))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void testWithRestrictedData() throws IOException {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("put_movement_with_restricted_data", "put_movement", props.getUrl());
        PutMovementResponse response = client.putMovement(
            MovementDtoFactory.createMovement(),
            MovementDtoFactory.createPutMovementRestrictedData(17L, 15L),
            props
        );
        assertions.assertThat(response).isEqualTo(MovementDtoFactory.createPutMovementResponseDs());
    }

}
