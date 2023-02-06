package ru.yandex.market.logistic.api.client.fulfillment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.RequestValidationUtils;
import ru.yandex.market.logistic.api.utils.common.OutboundFactory;

public class PutOutboundTest extends CommonServiceClientTest {

    @Autowired
    private FulfillmentClient client;

    @Test
    void testSuccessfulResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("put_outbound", props.getUrl());
        client.putOutbound(OutboundFactory.createOutbound(), null, props);

    }

    @Test
    void testSuccessfulResponseWithRestrictedData() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("put_outbound_restricted", "put_outbound", props.getUrl());
        client.putOutbound(OutboundFactory.createOutbound(), OutboundFactory.createRestrictedData("TMT100"), props);
    }

    @Test
    void testErrorResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("put_outbound", "put_outbound_with_errors", props.getUrl());
        assertions.assertThatThrownBy(() -> client.putOutbound(OutboundFactory.createOutbound(), null, props))
            .hasMessage("error during put outbound method")
            .isInstanceOf(RequestStateErrorException.class);
    }

    @Test
    void testValidationConstraints() {
        assertions.assertThatThrownBy(
            () -> client.putOutbound(OutboundFactory.createInvalidOutbound(), null,  getPartnerProperties())
        )
            .hasMessageContaining(RequestValidationUtils.getNotNullConstraint("outbound.interval"))
            .hasMessageContaining(RequestValidationUtils.getNotNullConstraint("outbound.outboundId"))
            .hasMessageContaining(
                RequestValidationUtils.getNotBlankConstraint("outbound.logisticPoint.logisticPointId.partnerId")
            )
            .hasMessageContaining(
                RequestValidationUtils.getNotBlankConstraint("outbound.logisticPoint.location.country")
            )
            .hasMessageContaining(
                RequestValidationUtils.getNotBlankConstraint("outbound.logisticPoint.location.region")
            )
            .hasMessageContaining(
                RequestValidationUtils.getNotBlankConstraint("outbound.logisticPoint.location.locality")
            )
            .hasMessageContaining(RequestValidationUtils.getNotBlankConstraint("outbound.courier.persons[0].name"))
            .hasMessageContaining(RequestValidationUtils.getNotBlankConstraint("outbound.courier.phone.phoneNumber"))
            .hasMessageContaining(RequestValidationUtils.getNotBlankConstraint("outbound.courier.car.number"))
            .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void testInvalidInterval() {
        assertions.assertThatThrownBy(
            () -> client.putOutbound(OutboundFactory.createOutboundInvalidInterval(), null, getPartnerProperties())
        )
            .hasMessage("Invalid interval format")
            .isInstanceOf(IllegalArgumentException.class);
    }
}
