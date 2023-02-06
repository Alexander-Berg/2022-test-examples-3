package ru.yandex.market.delivery.transport_manager.service.axapta.document;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.axapta.document.AxaptaDocumentRequest;
import ru.yandex.market.delivery.transport_manager.domain.entity.axapta.document.AxaptaDocumentRequestStatus;
import ru.yandex.market.delivery.transport_manager.dto.axapta.ResultStatus;
import ru.yandex.market.delivery.transport_manager.dto.axapta.document.GetTransportationDocumentRequest;
import ru.yandex.market.delivery.transport_manager.dto.axapta.document.GetTransportationDocumentResponse;
import ru.yandex.market.delivery.transport_manager.provider.AxaptaClient;
import ru.yandex.market.delivery.transport_manager.repository.mappers.AxaptaDocumentMapper;

import static org.mockito.Mockito.when;

@DatabaseSetup({
    "/repository/transportation/multiple_transportations_deps.xml",
    "/repository/transportation/multiple_transportations.xml",
    "/repository/axapta_document_request/several_requests.xml"
})
class AxaptaDocumentRequestSenderTest extends AbstractContextualTest {
    private AxaptaDocumentRequestSender requestSender;

    AxaptaClient client = Mockito.mock(AxaptaClient.class);


    @Autowired
    private AxaptaDocumentMapper documentMapper;

    @BeforeEach
    void setUp() {
        requestSender = new AxaptaDocumentRequestSender(
            client,
            documentMapper,
            clock
        );
    }

    @Test
    void send() {
        GetTransportationDocumentRequest request = new GetTransportationDocumentRequest(1L, null);
        GetTransportationDocumentResponse response = new GetTransportationDocumentResponse()
            .setTaskId("ReceivedDocument")
            .setResultStatus(new ResultStatus(true, ""));

        when(client.requestDocumentsForTransportation(request))
            .thenReturn(response);

        requestSender.send(1L);

        AxaptaDocumentRequest record = documentMapper.findById(1L);

        softly.assertThat(record.getAxaptaRequestId()).isEqualTo("ReceivedDocument");
        softly.assertThat(record.getStatus()).isEqualTo(AxaptaDocumentRequestStatus.WAITING_AXAPTA_RESPONSE);
    }
}
