package ru.yandex.market.logistic.api.client.fulfillment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.RequestValidationUtils;
import ru.yandex.market.logistic.api.utils.common.OutboundFactory;

class GetOutboundTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "outboundYandexId";

    @Autowired
    private FulfillmentClient client;

    @Test
    void testSuccessfulResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_outbound",
            "get_outbound/success_ff",
            props.getUrl()
        );
        client.getOutbound(OutboundFactory.createIdWithoutYandexId(), props);
    }

    @Test
    void testErrorResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_outbound",
            "get_outbound/with_errors",
            props.getUrl()
        );
        assertions.assertThatThrownBy(() -> client.getOutbound(OutboundFactory.createIdWithoutYandexId(), props))
            .hasMessage("error during get outbound method")
            .isInstanceOf(RequestStateErrorException.class);
    }

    @Test
    void testMissingOutboundId() {
        assertions.assertThatThrownBy(() -> client.getOutbound(null, getPartnerProperties()))
            .hasMessage(RequestValidationUtils.getNotNullErrorMessage("outboundId"))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void testMissingPartnerId() {
        // partnerId is null
        assertions.assertThatThrownBy(() -> client.getOutbound(
            new ResourceId(YANDEX_ID, null), getPartnerProperties())
        )
            .hasMessage(RequestValidationUtils.getNotBlankErrorMessage("outboundId.partnerId"))
            .isInstanceOf(RequestValidationException.class);

        // partnerId is blank
        assertions.assertThatThrownBy(() -> client.getOutbound(
            new ResourceId(YANDEX_ID, ""), getPartnerProperties())
        )
            .hasMessage(RequestValidationUtils.getNotBlankErrorMessage("outboundId.partnerId"))
            .isInstanceOf(RequestValidationException.class);
    }

}
