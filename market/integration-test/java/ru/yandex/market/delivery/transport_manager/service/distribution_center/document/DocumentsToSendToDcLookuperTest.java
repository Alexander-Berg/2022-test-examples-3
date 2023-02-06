package ru.yandex.market.delivery.transport_manager.service.distribution_center.document;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.axapta.document.AxaptaDocumentRequestStatus;
import ru.yandex.market.delivery.transport_manager.queue.task.distribution_center.document.SendDocumentRequestResultToDcProducer;
import ru.yandex.market.delivery.transport_manager.repository.mappers.AxaptaDocumentMapper;

@DatabaseSetup({
    "/repository/transportation/multiple_transportations_deps.xml",
    "/repository/transportation/multiple_transportations.xml",
    "/repository/transportation/register.xml",
    "/repository/axapta_document_request/several_requests_after_axapta_receiving.xml"
})
class DocumentsToSendToDcLookuperTest extends AbstractContextualTest {
    @Autowired
    private AxaptaDocumentMapper mapper;

    private DocumentsToSendToDcLookuper lookuper;

    private final SendDocumentRequestResultToDcProducer producer =
        Mockito.mock(SendDocumentRequestResultToDcProducer.class);

    @BeforeEach
    void setUp() {
        lookuper = new DocumentsToSendToDcLookuper(
            mapper,
            producer,
            clock
        );
    }

    @Test
    void lookup() {
        lookuper.lookup();

        Mockito.verify(producer, Mockito.times(3)).produce(Mockito.any());

        softly.assertThat(mapper.findById(9L).getStatus())
            .isEqualTo(AxaptaDocumentRequestStatus.SENDING_TO_DC);
        softly.assertThat(mapper.findById(10L).getStatus())
            .isEqualTo(AxaptaDocumentRequestStatus.SENDING_TO_DC);
        softly.assertThat(mapper.findById(11L).getStatus())
            .isEqualTo(AxaptaDocumentRequestStatus.SENDING_TO_DC);
        softly.assertThat(mapper.findById(5L).getStatus())
            .isEqualTo(AxaptaDocumentRequestStatus.ERROR);
    }
}
