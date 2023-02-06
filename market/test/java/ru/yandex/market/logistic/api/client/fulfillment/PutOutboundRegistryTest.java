package ru.yandex.market.logistic.api.client.fulfillment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.RequestValidationUtils;

import static ru.yandex.market.logistic.api.utils.fulfillment.RegistryDtoFactory.createMinimalOutboundRegistry;

public class PutOutboundRegistryTest extends CommonServiceClientTest {

    @Autowired
    private FulfillmentClient client;

    @Test
    public void testSuccessfulResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "put_outbound_registry/ff_request",
            "put_outbound_registry/success",
            props.getUrl()
        );
        client.putOutboundRegistry(createMinimalOutboundRegistry(), props);
    }

    @Test
    public void testFailureResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "put_outbound_registry/ff_request",
            "put_outbound_registry/error",
            props.getUrl()
        );
        assertions.assertThatThrownBy(() -> client.putOutboundRegistry(createMinimalOutboundRegistry(), props))
            .hasMessage("a terrible error has occurred!")
            .isInstanceOf(RequestStateErrorException.class);
    }

    @Test
    void testMissingRegistry() {
        assertions.assertThatThrownBy(() -> client.putOutboundRegistry(null, getPartnerProperties()))
            .hasMessage(RequestValidationUtils.getNotNullErrorMessage("registry"))
            .isInstanceOf(RequestValidationException.class);
    }
}
