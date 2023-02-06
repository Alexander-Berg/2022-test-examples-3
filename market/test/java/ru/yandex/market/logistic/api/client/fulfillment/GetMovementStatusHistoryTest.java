package ru.yandex.market.logistic.api.client.fulfillment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageConversionException;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.exceptions.ResponseValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetMovementStatusHistoryResponse;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.RequestValidationUtils;
import ru.yandex.market.logistic.api.utils.ResponseValidationUtils;
import ru.yandex.market.logistic.api.utils.common.MovementDtoFactory;

public class GetMovementStatusHistoryTest extends CommonServiceClientTest {

    @Autowired
    private FulfillmentClient client;

    @Test
    void testSuccessfulResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("get_movement_status_history", props.getUrl());
        GetMovementStatusHistoryResponse response = client.getMovementStatusHistory(
            MovementDtoFactory.createMovementIds(),
            props
        );
        assertions.assertThat(response).isEqualTo(MovementDtoFactory.createGetMovementStatusHistoryResponseFF());
    }

    @Test
    void testErrorResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_movement_status_history",
            "get_movement_status_history_with_errors",
            props.getUrl()
        );
        assertions.assertThatThrownBy(
            () -> client.getMovementStatusHistory(MovementDtoFactory.createMovementIds(), props)
        )
            .hasMessage("Omg something terrible happened")
            .isInstanceOf(RequestStateErrorException.class);
    }

    @Test
    void testMissingStatusCode() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_movement_status_history",
            "get_movement_status_history_without_status_code",
            props.getUrl()
        );
        assertions.assertThatThrownBy(
            () -> client.getMovementStatusHistory(MovementDtoFactory.createMovementIds(), props)
        )
            .hasMessageContaining(ResponseValidationUtils.getNotNullErrorMessage(
                "fulfillment",
                "GetMovementStatusHistoryResponse",
                "movementStatusHistories[2].history[2].statusCode"
            ))
            .isInstanceOf(ResponseValidationException.class);
    }

    @Test
    void testInvalidDate() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_movement_status_history",
            "get_movement_status_history_with_invalid_date",
            props.getUrl()
        );
        assertions.assertThatThrownBy(
            () -> client.getMovementStatusHistory(MovementDtoFactory.createMovementIds(), props)
        )
            .hasMessageContaining("Invalid value for HourOfDay (valid values 0 - 23): 71")
            .isInstanceOf(HttpMessageConversionException.class);
    }

    @Test
    void testEmptyPartnerId() {
        PartnerProperties props = getPartnerProperties();
        assertions.assertThatThrownBy(
            () -> client.getMovementStatusHistory(MovementDtoFactory.createMovementIdsWithoutPartnerId(), props)
        )
            .hasMessageContaining(RequestValidationUtils.getNotBlankConstraint(
                "movementIds[0].partnerId"
            ))
            .isInstanceOf(RequestValidationException.class);
    }

}
