package ru.yandex.market.logistic.api.client.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageConversionException;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.exceptions.ResponseValidationException;
import ru.yandex.market.logistic.api.model.delivery.response.GetOutboundStatusResponse;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.RequestValidationUtils;
import ru.yandex.market.logistic.api.utils.ResponseValidationUtils;
import ru.yandex.market.logistic.api.utils.common.OutboundFactory;

public class GetOutboundStatusTest extends CommonServiceClientTest {

    @Autowired
    private DeliveryServiceClient client;

    @Test
    void testSuccessfulResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("get_outbound_status", props.getUrl());
        GetOutboundStatusResponse response = client.getOutboundStatus(OutboundFactory.createOutboundIds(), props);
        assertions.assertThat(response).isEqualTo(OutboundFactory.createGetOutboundStatusResponseDS());
    }

    @Test
    void testErrorResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_outbound_status",
            "get_outbound_status_with_errors", props.getUrl()
        );
        assertions.assertThatThrownBy(() -> client.getOutboundStatus(OutboundFactory.createOutboundIds(), props))
            .hasMessage("error while getOutboundStatus")
            .isInstanceOf(RequestStateErrorException.class);
    }

    @Test
    void testMissingStatusCode() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_outbound_status",
            "get_outbound_status_without_status_code",
            props.getUrl()
        );
        assertions.assertThatThrownBy(() -> client.getOutboundStatus(OutboundFactory.createOutboundIds(), props))
            .hasMessageContaining(ResponseValidationUtils.getNotNullErrorMessage(
                "delivery",
                "GetOutboundStatusResponse",
                "outboundStatuses[0].status.statusCode"
            ))
            .isInstanceOf(ResponseValidationException.class);
    }

    @Test
    void testInvalidDate() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_outbound_status",
            "get_outbound_status_with_invalid_date", props.getUrl()
        );
        assertions.assertThatThrownBy(() -> client.getOutboundStatus(OutboundFactory.createOutboundIds(), props))
            .hasMessageContaining("Invalid value for HourOfDay (valid values 0 - 23): 25")
            .isInstanceOf(HttpMessageConversionException.class);
    }

    @Test
    void testEmptyPartnerId() {
        PartnerProperties props = getPartnerProperties();
        assertions.assertThatThrownBy(
            () -> client.getOutboundStatus(OutboundFactory.createOutboundIdsWithoutPartnerId(), props)
        )
            .hasMessageContaining(RequestValidationUtils.getNotBlankConstraint(
                "outboundIds[2].partnerId"
            ))
            .isInstanceOf(RequestValidationException.class);
    }
}
