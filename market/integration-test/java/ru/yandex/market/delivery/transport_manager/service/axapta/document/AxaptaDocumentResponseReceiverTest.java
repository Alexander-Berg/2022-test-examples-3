package ru.yandex.market.delivery.transport_manager.service.axapta.document;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.axapta.document.AxaptaDocumentRequest;
import ru.yandex.market.delivery.transport_manager.domain.entity.axapta.document.AxaptaDocumentRequestStatus;
import ru.yandex.market.delivery.transport_manager.dto.axapta.document.GetTransportationDocumentResult;
import ru.yandex.market.delivery.transport_manager.repository.mappers.AxaptaDocumentMapper;

@DatabaseSetup({
    "/repository/transportation/multiple_transportations_deps.xml",
    "/repository/transportation/multiple_transportations.xml",
    "/repository/axapta_document_request/several_requests.xml"
})
class AxaptaDocumentResponseReceiverTest extends AbstractContextualTest {
    @Autowired
    private AxaptaDocumentMapper documentMapper;

    @Autowired
    private AxaptaDocumentResponseReceiver documentResponseReceiver;

    @Test
    void receive() {
        GetTransportationDocumentResult result = new GetTransportationDocumentResult()
            .setInboundRequestIds(List.of(123L, 666L, 100500L))
            .setDocumentLinks(List.of("GoodUrl", "YetAnotherUrl"));

        documentMapper.insert(request());
        documentResponseReceiver.receive("axa123", result);

        AxaptaDocumentRequest request = documentMapper.findByAxaptaRequestId("axa123");

        softly.assertThat(request.getStatus()).isEqualTo(AxaptaDocumentRequestStatus.AXAPTA_RESPONSE_RECEIVED);
        softly.assertThat(request.getDocumentUrls()).containsExactlyInAnyOrder("GoodUrl", "YetAnotherUrl");
        softly.assertThat(request.getInboundsUsedByAxapta()).containsExactlyInAnyOrder(123L, 666L, 100500L);
    }

    private static AxaptaDocumentRequest request() {
        return new AxaptaDocumentRequest()
            .setStatus(AxaptaDocumentRequestStatus.WAITING_AXAPTA_RESPONSE)
            .setTransportationId(1L)
            .setInboundsRequestedToShip(List.of(666L))
            .setAxaptaRequestId("axa123");
    }
}
