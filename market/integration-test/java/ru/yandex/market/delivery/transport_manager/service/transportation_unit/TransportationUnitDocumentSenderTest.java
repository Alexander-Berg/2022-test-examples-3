package ru.yandex.market.delivery.transport_manager.service.transportation_unit;

import java.time.LocalDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.service.health.event.TrnEventWriter;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Document;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DocumentType;
import ru.yandex.market.logistic.gateway.common.model.utils.DateTime;

@DatabaseSetup({
    "/repository/transportation/multiple_transportations_deps.xml",
    "/repository/transportation/multiple_transportations.xml"
})
@DatabaseSetup("/repository/transportation_unit_documents/documents.xml")
public class TransportationUnitDocumentSenderTest extends AbstractContextualTest {
    @Autowired
    private TransportationUnitDocumentSender documentSender;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    private TrnEventWriter trnEventWriter;

    @Test
    @DatabaseSetup("/repository/transportation_unit_documents/methods.xml")
    @DatabaseSetup(
        value = "/repository/transportation/update/set_request_id_for_4.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/repository/transportation_unit_documents/after/after_status_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void send() throws GatewayApiException {
        documentSender.send(3L);

        Mockito.verify(fulfillmentClient).putOutboundDocuments(
            ResourceId.builder().setYandexId("777").setPartnerId(null).build(),
            List.of("doc2", "doc3"),
            List.of(
                new Document("doc2", DocumentType.TORG13),
                new Document("doc3", DocumentType.TRN)
            ),
            DateTime.fromLocalDateTime(LocalDateTime.of(2021, 7, 12, 17, 0)),
            new Partner(10L),
            null
        );
        Mockito.verify(trnEventWriter).trnSent(4);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/transportation_unit_documents/after/after_do_not_need_to_send.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void sendWithoutMethod() throws GatewayApiException {
        documentSender.send(3L);
        Mockito.verify(fulfillmentClient, Mockito.times(0)).putOutboundDocuments(
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any()
        );
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit_documents/methods.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_unit_documents/documents.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void sendFromInvalidState() throws GatewayApiException {
        softly.assertThatThrownBy(() -> documentSender.send(1L));
        Mockito.verify(fulfillmentClient, Mockito.times(0)).putOutboundDocuments(
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any()
        );
    }
}
