package ru.yandex.market.logistic.api.client.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.RequestValidationUtils;
import ru.yandex.market.logistic.api.utils.common.InboundFactory;

class GetInboundTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "inboundYandexId";

    @Autowired
    private DeliveryServiceClient client;

    @Test
    void testSuccessfulResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_inbound",
            "get_inbound/success_DS",
            props.getUrl()
        );
        client.getInbound(InboundFactory.createIdWithoutYandexId(), props);
    }

    @Test
    void testErrorResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "get_inbound",
            "get_inbound/with_errors",
            props.getUrl()
        );
        assertions.assertThatThrownBy(() -> client.getInbound(InboundFactory.createIdWithoutYandexId(), props))
            .hasMessage("error during get inbound method")
            .isInstanceOf(RequestStateErrorException.class);
    }

    @Test
    void testMissingInboundId() {
        assertions.assertThatThrownBy(() -> client.getInbound(null, getPartnerProperties()))
            .hasMessage(RequestValidationUtils.getNotNullErrorMessage("inboundId"))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void testMissingPartnerId() {
        // partnerId is null
        assertions.assertThatThrownBy(() -> client.getInbound(
            new ResourceId(YANDEX_ID, null), getPartnerProperties())
        )
            .hasMessage(RequestValidationUtils.getNotBlankErrorMessage("inboundId.partnerId"))
            .isInstanceOf(RequestValidationException.class);

        // partnerId is blank
        assertions.assertThatThrownBy(() -> client.getInbound(
            new ResourceId(YANDEX_ID, ""), getPartnerProperties())
        )
            .hasMessage(RequestValidationUtils.getNotBlankErrorMessage("inboundId.partnerId"))
            .isInstanceOf(RequestValidationException.class);
    }
}
