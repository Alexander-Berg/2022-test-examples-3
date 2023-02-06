package ru.yandex.market.logistic.api.client.fulfillment;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.RequestValidationException;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.request.entities.Document;
import ru.yandex.market.logistic.api.model.fulfillment.request.entities.DocumentType;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.RequestValidationUtils;

public class PutOutboundDocumentsTest extends CommonServiceClientTest {
    @Autowired
    private FulfillmentClient client;

    @Test
    void testSuccessfulResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("put_outbound_documents", props.getUrl());
        client.putOutboundDocuments(
            new ResourceId("123", "11"),
            Arrays.asList("url1", "url2"),
            Arrays.asList(new Document("url3", DocumentType.TORG13), new Document("url4", null)),
            new DateTime("2022-03-17T10:00:00+02:00"),
            props
        );
    }

    @Test
    void testErrorResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("put_outbound_documents", "put_outbound_documents_error", props.getUrl());
        assertions.assertThatThrownBy(() -> client.putOutboundDocuments(
                new ResourceId("123", "11"),
                Arrays.asList("url1", "url2"),
                Arrays.asList(new Document("url3", DocumentType.TORG13), new Document("url4", null)),
                new DateTime("2022-03-17T10:00:00+02:00"),
                props
            ))
            .hasMessage("error during put outbound documents method")
            .isInstanceOf(RequestStateErrorException.class);
    }

    @Test
    void testValidationConstraints() {
        assertions.assertThatThrownBy(
                () -> client.putOutboundDocuments(null, null, null, null, getPartnerProperties())
            )
            .hasMessageContaining(RequestValidationUtils.getNotNullConstraint("outboundId"))
            .hasMessageContaining(RequestValidationUtils.getNotNullConstraint("documentsDate"))
            .isInstanceOf(RequestValidationException.class);
    }
}
