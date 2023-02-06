package ru.yandex.market.delivery.transport_manager.service.distribution_center.document;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.converter.prefix.IdPrefixConverter;
import ru.yandex.market.delivery.transport_manager.domain.entity.axapta.document.AxaptaDocumentRequest;
import ru.yandex.market.delivery.transport_manager.domain.entity.axapta.document.AxaptaDocumentRequestStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitSendingStrategy;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.document.SendDocumentsBody;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.document.SendDocumentsBodyResultStatus;
import ru.yandex.market.delivery.transport_manager.repository.mappers.AxaptaDocumentMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationUnitMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterMapper;
import ru.yandex.market.delivery.transport_manager.service.distribution_center.client.DcClient;
import ru.yandex.market.delivery.transport_manager.service.health.event.TrnEventWriter;

@DatabaseSetup({
    "/repository/transportation/multiple_transportations_deps.xml",
    "/repository/transportation/multiple_transportations.xml",
    "/repository/transportation/register.xml",
    "/repository/axapta_document_request/several_requests_after_axapta_receiving.xml"
})
class DcDocumentSenderTest extends AbstractContextualTest {
    private final DcClient dcClient = Mockito.mock(DcClient.class);

    @Autowired
    private AxaptaDocumentMapper documentMapper;

    @Autowired
    private TransportationUnitMapper unitMapper;

    @Autowired
    private RegisterMapper registerMapper;

    @Autowired
    private TransportationUnitMapper transportationUnitMapper;

    @Autowired
    private TrnEventWriter trnEventWriter;

    @Autowired
    private IdPrefixConverter idPrefixConverter;

    private DcDocumentSender dcDocumentSender;

    @Captor
    ArgumentCaptor<SendDocumentsBody> bodyCaptor;

    @Captor
    ArgumentCaptor<Long> pointCaptor;

    @Captor
    ArgumentCaptor<String> outboundIdCaptor;

    @BeforeEach
    void setUp() {
        dcDocumentSender = new DcDocumentSender(
            documentMapper,
            dcClient,
            unitMapper,
            registerMapper,
            clock,
            trnEventWriter
        );
    }

    @Test
    void testHappyPath() {
        dcDocumentSender.send(12L);

        Mockito.doNothing().when(dcClient).sendDocuments(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(dcClient).sendDocuments(bodyCaptor.capture(), pointCaptor.capture(), outboundIdCaptor.capture());

        softly.assertThat(bodyCaptor.getValue()).isEqualTo(body(
            123L,
            SendDocumentsBodyResultStatus.SUCCESS,
            List.of("doc7", "doc8", "doc9")
        ));

        softly.assertThat(pointCaptor.getValue()).isEqualTo(1L);
        softly.assertThat(outboundIdCaptor.getValue()).isEqualTo("TMU2");

        AxaptaDocumentRequest request = documentMapper.findById(12L);

        softly.assertThat(request.getStatus()).isEqualTo(AxaptaDocumentRequestStatus.SENT_TO_DC);

        Mockito.verify(trnEventWriter).trnSent(2);
    }


    @Test
    void testHappyPathLgw() {
        transportationUnitMapper.findById(2L).ifPresent(tu -> {
            tu.setSendingStrategy(UnitSendingStrategy.DIRECTLY_TO_LGW);
            transportationUnitMapper.update(tu);
        });

        dcDocumentSender.send(14L);

        Mockito.doNothing().when(dcClient).sendDocuments(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(dcClient).sendDocuments(bodyCaptor.capture(), pointCaptor.capture(), outboundIdCaptor.capture());

        softly.assertThat(bodyCaptor.getValue()).isEqualTo(body(
            123L,
            SendDocumentsBodyResultStatus.SUCCESS,
            List.of("doc7", "doc8", "doc9")
        ));

        softly.assertThat(pointCaptor.getValue()).isEqualTo(3L);
        softly.assertThat(outboundIdCaptor.getValue()).isEqualTo("TMU4");

        AxaptaDocumentRequest request = documentMapper.findById(14L);

        softly.assertThat(request.getStatus()).isEqualTo(AxaptaDocumentRequestStatus.SENT_TO_DC);
    }

    @Test
    void notAllInboundsUsed() {
        dcDocumentSender.send(13L);

        Mockito.doNothing().when(dcClient).sendDocuments(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(dcClient).sendDocuments(bodyCaptor.capture(), pointCaptor.capture(), outboundIdCaptor.capture());

        softly.assertThat(bodyCaptor.getValue()).isEqualTo(body(
            123L,
            SendDocumentsBodyResultStatus.ERROR,
            null
        ));

        softly.assertThat(pointCaptor.getValue()).isEqualTo(1L);
        softly.assertThat(outboundIdCaptor.getValue()).isEqualTo("TMU2");

        AxaptaDocumentRequest request = documentMapper.findById(13L);

        softly.assertThat(request.getStatus()).isEqualTo(AxaptaDocumentRequestStatus.ERROR);
    }

    private static SendDocumentsBody body(Long registryId, SendDocumentsBodyResultStatus status, List<String> docs) {
        return new SendDocumentsBody()
            .setRegistryId(registryId)
            .setStatus(status)
            .setDocs(docs);
    }
}
