package ru.yandex.market.logistic.api.client.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.RequestValidationUtils;

import static ru.yandex.market.logistic.api.utils.delivery.RegistryDtoFactory.createMinimalInboundRegistry;

public class PutInboundRegistryTest extends CommonServiceClientTest {
    @Autowired
    private DeliveryServiceClient client;

    @Test
    public void testSuccessfulResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "put_inbound_registry/ds_request",
            "put_inbound_registry/success",
            props.getUrl()
        );
        client.putInboundRegistry(createMinimalInboundRegistry(), props);
    }

    @Test
    public void testFailureResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized(
            "put_inbound_registry/ds_request",
            "put_inbound_registry/error",
            props.getUrl()
        );
        assertions.assertThatThrownBy(() -> client.putInboundRegistry(createMinimalInboundRegistry(), props))
            .hasMessage("a terrible error has occurred!")
            .isInstanceOf(RequestStateErrorException.class);
    }

    @Test
    void testMissingRegistry() {
        assertions.assertThatThrownBy(() -> client.putInboundRegistry(null, getPartnerProperties()))
            .hasMessage(RequestValidationUtils.getNotNullErrorMessage("registry"))
            .isInstanceOf(RequestValidationException.class);
    }
}
