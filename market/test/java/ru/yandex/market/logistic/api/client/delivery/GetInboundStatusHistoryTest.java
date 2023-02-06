package ru.yandex.market.logistic.api.client.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageConversionException;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.exceptions.ResponseValidationException;
import ru.yandex.market.logistic.api.model.delivery.response.GetInboundStatusHistoryResponse;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.RequestValidationUtils;
import ru.yandex.market.logistic.api.utils.ResponseValidationUtils;
import ru.yandex.market.logistic.api.utils.common.InboundFactory;

public class GetInboundStatusHistoryTest extends CommonServiceClientTest {

    @Autowired
    private DeliveryServiceClient client;

    @Test
    void testSuccessfulResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("get_inbound_status_history", props.getUrl());
        GetInboundStatusHistoryResponse response = client.getInboundStatusHistory(
            InboundFactory.createInboundIds(),
            props
        );
        assertions.assertThat(response).isEqualTo(InboundFactory.createInboundStatusHistoryResponseDS());
    }

    @Test
    void testErrorResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_inbound_status_history",
            "get_inbound_status_history_with_errors",
            props.getUrl()
        );
        assertions.assertThatThrownBy(() -> client.getInboundStatusHistory(InboundFactory.createInboundIds(), props))
            .hasMessage("error while getInboundStatusHistory")
            .isInstanceOf(RequestStateErrorException.class);
    }

    @Test
    void testMissingStatusCode() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_inbound_status_history",
            "get_inbound_status_history_without_status_code",
            props.getUrl()
        );
        assertions.assertThatThrownBy(() -> client.getInboundStatusHistory(InboundFactory.createInboundIds(), props))
            .hasMessageContaining(ResponseValidationUtils.getNotNullErrorMessage(
                "delivery",
                "GetInboundStatusHistoryResponse",
                "inboundStatusHistories[0].history[0].statusCode"
            ))
            .isInstanceOf(ResponseValidationException.class);
    }

    @Test
    void testInvalidDate() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_inbound_status_history",
            "get_inbound_status_history_with_invalid_date", props.getUrl()
        );
        assertions.assertThatThrownBy(() -> client.getInboundStatusHistory(InboundFactory.createInboundIds(), props))
            .hasMessageContaining("Invalid value for HourOfDay (valid values 0 - 23): 25")
            .isInstanceOf(HttpMessageConversionException.class);
    }

    @Test
    void testEmptyPartnerId() {
        PartnerProperties props = getPartnerProperties();
        assertions.assertThatThrownBy(
            () -> client.getInboundStatusHistory(InboundFactory.createInboundIdsWithoutPartnerId(), props)
        )
            .hasMessageContaining(RequestValidationUtils.getNotBlankConstraint(
                "inboundIds[2].partnerId"
            ))
            .isInstanceOf(RequestValidationException.class);
    }

}
