package ru.yandex.market.logistic.api.client.fulfillment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageConversionException;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.exceptions.ResponseValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOutboundStatusHistoryResponse;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.RequestValidationUtils;
import ru.yandex.market.logistic.api.utils.ResponseValidationUtils;
import ru.yandex.market.logistic.api.utils.common.OutboundFactory;

public class GetOutboundStatusHistoryTest extends CommonServiceClientTest {

    @Autowired
    private FulfillmentClient client;

    @Test
    void testSuccessfulResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("get_outbound_status_history", props.getUrl());
        GetOutboundStatusHistoryResponse response = client.getOutboundStatusHistory(
            OutboundFactory.createOutboundIds(),
            props
        );
        assertions.assertThat(response).isEqualTo(OutboundFactory.createOutboundStatusHistoryResponseFF());
    }

    @Test
    void testErrorResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_outbound_status_history",
            "get_outbound_status_history_with_errors",
            props.getUrl()
        );
        assertions.assertThatThrownBy(() -> client.getOutboundStatusHistory(OutboundFactory.createOutboundIds(), props))
            .hasMessage("error while getOutboundStatusHistory")
            .isInstanceOf(RequestStateErrorException.class);
    }

    @Test
    void testMissingStatusCode() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_outbound_status_history",
            "get_outbound_status_history_without_status_code",
            props.getUrl()
        );
        assertions.assertThatThrownBy(() -> client.getOutboundStatusHistory(OutboundFactory.createOutboundIds(), props))
            .hasMessageContaining(ResponseValidationUtils.getNotNullErrorMessage(
                "fulfillment",
                "GetOutboundStatusHistoryResponse",
                "outboundStatusHistories[0].history[0].statusCode"
            ))
            .isInstanceOf(ResponseValidationException.class);
    }

    @Test
    void testInvalidDate() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_outbound_status_history",
            "get_outbound_status_history_with_invalid_date",
            props.getUrl()
        );
        assertions.assertThatThrownBy(() -> client.getOutboundStatusHistory(OutboundFactory.createOutboundIds(), props))
            .hasMessageContaining("Invalid value for HourOfDay (valid values 0 - 23): 25")
            .isInstanceOf(HttpMessageConversionException.class);
    }

    @Test
    void testEmptyPartnerId() {
        PartnerProperties props = getPartnerProperties();
        assertions.assertThatThrownBy(
            () -> client.getOutboundStatusHistory(OutboundFactory.createOutboundIdsWithoutPartnerId(), props)
        )
            .hasMessageContaining(RequestValidationUtils.getNotBlankConstraint(
                "outboundIds[2].partnerId"
            ))
            .isInstanceOf(RequestValidationException.class);
    }

}
