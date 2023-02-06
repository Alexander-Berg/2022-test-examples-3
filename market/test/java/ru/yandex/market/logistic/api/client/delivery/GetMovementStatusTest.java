package ru.yandex.market.logistic.api.client.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageConversionException;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.exceptions.ResponseValidationException;
import ru.yandex.market.logistic.api.model.delivery.response.GetMovementStatusResponse;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.RequestValidationUtils;
import ru.yandex.market.logistic.api.utils.ResponseValidationUtils;
import ru.yandex.market.logistic.api.utils.common.MovementDtoFactory;

public class GetMovementStatusTest extends CommonServiceClientTest {

    @Autowired
    private DeliveryServiceClient client;

    @Test
    void testSuccessfulResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("get_movement_status", props.getUrl());
        GetMovementStatusResponse response = client.getMovementStatus(MovementDtoFactory.createMovementIds(), props);
        assertions.assertThat(response).isEqualTo(MovementDtoFactory.createGetMovementStatusResponseDS());
    }

    @Test
    void testErrorResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("get_movement_status", "get_movement_status_with_errors", props.getUrl());
        assertions.assertThatThrownBy(() -> client.getMovementStatus(MovementDtoFactory.createMovementIds(), props))
            .hasMessage("Omg something terrible happened")
            .isInstanceOf(RequestStateErrorException.class);
    }

    @Test
    void testMissingStatusCode() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("get_movement_status", "get_movement_status_without_status_code", props.getUrl());
        assertions.assertThatThrownBy(() -> client.getMovementStatus(MovementDtoFactory.createMovementIds(), props))
            .hasMessageContaining(ResponseValidationUtils.getNotNullErrorMessage(
                "delivery",
                "GetMovementStatusResponse",
                "movementStatuses[0].status.statusCode"
            ))
            .isInstanceOf(ResponseValidationException.class);
    }

    @Test
    void testInvalidDate() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("get_movement_status", "get_movement_status_with_invalid_date", props.getUrl());
        assertions.assertThatThrownBy(() -> client.getMovementStatus(MovementDtoFactory.createMovementIds(), props))
            .hasMessageContaining("Invalid value for HourOfDay (valid values 0 - 23): 68")
            .isInstanceOf(HttpMessageConversionException.class);
    }

    @Test
    void testEmptyPartnerId() {
        PartnerProperties props = getPartnerProperties();
        assertions.assertThatThrownBy(
            () -> client.getMovementStatus(MovementDtoFactory.createMovementIdsWithoutPartnerId(), props)
        )
            .hasMessageContaining(RequestValidationUtils.getNotBlankConstraint(
                "movementIds[0].partnerId"
            ))
            .isInstanceOf(RequestValidationException.class);
    }

}
