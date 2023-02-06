package ru.yandex.market.logistic.api.client.fulfillment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageConversionException;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.exceptions.ResponseValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundStatusResponse;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.RequestValidationUtils;
import ru.yandex.market.logistic.api.utils.ResponseValidationUtils;
import ru.yandex.market.logistic.api.utils.common.InboundFactory;

public class GetInboundStatusTest extends CommonServiceClientTest {

    @Autowired
    private FulfillmentClient client;

    @Test
    void testSuccessfulResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("get_inbound_status", props.getUrl());
        GetInboundStatusResponse response = client.getInboundStatus(InboundFactory.createInboundIds(), props);
        assertions.assertThat(response).isEqualTo(InboundFactory.createGetInboundStatusResponseFF());
    }

    @Test
    void testErrorResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_inbound_status",
            "get_inbound_status_with_errors", props.getUrl()
        );
        assertions.assertThatThrownBy(() -> client.getInboundStatus(InboundFactory.createInboundIds(), props))
            .hasMessage("error while getInboundStatus")
            .isInstanceOf(RequestStateErrorException.class);
    }

    @Test
    void testMissingStatusCode() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_inbound_status",
            "get_inbound_status_without_status_code",
            props.getUrl()
        );
        assertions.assertThatThrownBy(() -> client.getInboundStatus(InboundFactory.createInboundIds(), props))
            .hasMessageContaining(ResponseValidationUtils.getNotNullErrorMessage(
                "fulfillment",
                "GetInboundStatusResponse",
                "inboundStatuses[0].status.statusCode"
            ))
            .isInstanceOf(ResponseValidationException.class);
    }

    @Test
    void testInvalidDate() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_inbound_status",
            "get_inbound_status_with_invalid_date", props.getUrl()
        );
        assertions.assertThatThrownBy(() -> client.getInboundStatus(InboundFactory.createInboundIds(), props))
            .hasMessageContaining("Invalid value for HourOfDay (valid values 0 - 23): 25")
            .isInstanceOf(HttpMessageConversionException.class);
    }

    @Test
    void testEmptyPartnerId() {
        PartnerProperties props = getPartnerProperties();
        assertions.assertThatThrownBy(
            () -> client.getInboundStatus(InboundFactory.createInboundIdsWithoutPartnerId(), props)
        )
            .hasMessageContaining(RequestValidationUtils.getNotBlankConstraint(
                "inboundIds[2].partnerId"
            ))
            .isInstanceOf(RequestValidationException.class);
    }

}
