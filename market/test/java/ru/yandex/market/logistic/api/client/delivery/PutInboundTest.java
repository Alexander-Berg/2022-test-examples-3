package ru.yandex.market.logistic.api.client.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.RequestValidationUtils;
import ru.yandex.market.logistic.api.utils.common.InboundFactory;

public class PutInboundTest extends CommonServiceClientTest {

    @Autowired
    private DeliveryServiceClient client;

    @Test
    void testSuccessfulResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("put_inbound", props.getUrl());
        client.putInbound(InboundFactory.createInbound(), props);
    }

    @Test
    void testErrorResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("put_inbound", "put_inbound_with_errors", props.getUrl());
        assertions.assertThatThrownBy(() -> client.putInbound(InboundFactory.createInbound(), props))
            .hasMessage("error during put inbound method")
            .isInstanceOf(RequestStateErrorException.class);
    }

    @Test
    void testValidationConstraints() {
        assertions.assertThatThrownBy(
            () -> client.putInbound(InboundFactory.createInvalidInbound(), getPartnerProperties())
        )
            .hasMessageContaining(RequestValidationUtils.getNotNullConstraint("inbound.interval"))
            .hasMessageContaining(RequestValidationUtils.getNotNullConstraint("inbound.inboundType"))
            .hasMessageContaining(RequestValidationUtils.getNotNullConstraint("inbound.inboundId"))
            .hasMessageContaining(
                RequestValidationUtils.getNotBlankConstraint("inbound.logisticPoint.logisticPointId.partnerId")
            )
            .hasMessageContaining(
                RequestValidationUtils.getNotBlankConstraint("inbound.logisticPoint.location.country")
            )
            .hasMessageContaining(RequestValidationUtils.getNotBlankConstraint("inbound.logisticPoint.location.region"))
            .hasMessageContaining(
                RequestValidationUtils.getNotBlankConstraint("inbound.logisticPoint.location.locality")
            )
            .hasMessageContaining(RequestValidationUtils.getNotBlankConstraint("inbound.courier.persons[0].name"))
            .hasMessageContaining(RequestValidationUtils.getNotBlankConstraint("inbound.courier.phone.phoneNumber"))
            .hasMessageContaining(RequestValidationUtils.getNotBlankConstraint("inbound.courier.car.number"))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void testInvalidInterval() {
        assertions.assertThatThrownBy(
            () -> client.putInbound(InboundFactory.createInboundInvalidInterval(), getPartnerProperties())
        )
            .hasMessage("Invalid interval format")
            .isInstanceOf(IllegalArgumentException.class);
    }
}
