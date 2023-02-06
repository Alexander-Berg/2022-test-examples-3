package ru.yandex.market.delivery.transport_manager.service.axapta.document;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.axapta.document.AxaptaDocumentRequestStatus;
import ru.yandex.market.delivery.transport_manager.queue.task.axapta.document.SendAxaptaDocumentRequestProducer;
import ru.yandex.market.delivery.transport_manager.repository.mappers.AxaptaDocumentMapper;

@DatabaseSetup({
    "/repository/transportation/multiple_transportations_deps.xml",
    "/repository/transportation/multiple_transportations.xml",
    "/repository/axapta_document_request/several_requests.xml"
})
class NewDocumentRequestProcessorTest extends AbstractContextualTest {
    @Autowired
    private SendAxaptaDocumentRequestProducer sendAxaptaDocumentRequestProducer;

    @Autowired
    private AxaptaDocumentMapper axaptaDocumentMapper;

    private NewDocumentRequestProcessor newDocumentRequestProcessor;

    @BeforeEach
    void setUp() {
        newDocumentRequestProcessor = new NewDocumentRequestProcessor(
            axaptaDocumentMapper,
            sendAxaptaDocumentRequestProducer,
            clock
        );
    }

    @Test
    void lookup() {
        newDocumentRequestProcessor.lookup();

        Mockito.verify(sendAxaptaDocumentRequestProducer, Mockito.times(4))
            .produce(Mockito.any());

        softly.assertThat(axaptaDocumentMapper.findById(1L).getStatus())
            .isEqualTo(AxaptaDocumentRequestStatus.SENDING_TO_AXAPTA);
        softly.assertThat(axaptaDocumentMapper.findById(2L).getStatus())
            .isEqualTo(AxaptaDocumentRequestStatus.SENDING_TO_AXAPTA);
        softly.assertThat(axaptaDocumentMapper.findById(3L).getStatus())
            .isEqualTo(AxaptaDocumentRequestStatus.SENDING_TO_AXAPTA);
        softly.assertThat(axaptaDocumentMapper.findById(4L).getStatus())
            .isEqualTo(AxaptaDocumentRequestStatus.SENDING_TO_AXAPTA);
        softly.assertThat(axaptaDocumentMapper.findById(5L).getStatus())
            .isEqualTo(AxaptaDocumentRequestStatus.ERROR);
    }
}
