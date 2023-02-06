package ru.yandex.market.delivery.transport_manager.controller.axapta;

import java.time.LocalDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Document;
import ru.yandex.market.delivery.transport_manager.domain.entity.DocumentType;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitDocument;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitDocumentStatus;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationUnitDocumentMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class AxaptaControllerTest extends AbstractContextualTest {
    @Autowired
    private TransportationUnitDocumentMapper transportationUnitDocumentMapper;

    @DatabaseSetup({
        "/repository/transportation/xdoc_transport.xml",
        "/repository/axapta_document_request/sent_request.xml"
    })
    @ExpectedDatabase(
        value = "/repository/axapta_document_request/received_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void receiveDocuments() throws Exception {
        mockMvc
            .perform(
                put("/axapta/documents/1")
                    .content(extractFileContent("controller/axapta/document.json"))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml",
        "/repository/transportation_unit_documents/base_document.xml"
    })
    void receiveOutboundDocuments() throws Exception {
        mockMvc
            .perform(
                put("/axapta/outboundDocuments/4")
                    .content(extractFileContent("controller/axapta/outboundDocuments.json"))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk());

        List<Document> documents = List.of(
            new Document("url2", DocumentType.TORG13),
            new Document("url5", DocumentType.TRN),
            new Document("url7", DocumentType.UNKNOWN)
        );

        var insertedDoc = transportationUnitDocumentMapper.findById(2L);
        softly.assertThat(insertedDoc).isEqualTo(
            document(
                4L,
                TransportationUnitDocumentStatus.NEW,
                List.of("url2", "url5", "url7"),
                documents,
                LocalDateTime.of(2022, 3, 25, 21, 34, 55)
            ).setId(2L)
        );

        var oldDoc = transportationUnitDocumentMapper.findById(1L);
        softly.assertThat(oldDoc)
            .isEqualTo(
                document(
                    2L,
                    TransportationUnitDocumentStatus.SUCCESS,
                    List.of("doc6", "doc7", "doc8"),
                    List.of(
                        new Document("doc6", DocumentType.TORG13),
                        new Document("doc7", DocumentType.TRN),
                        new Document("doc8", DocumentType.UNKNOWN)
                    ),
                    LocalDateTime.of(2021, 7, 12, 17, 0, 0)
                ).setId(1L)
            );

        mockMvc
            .perform(
                put("/axapta/outboundDocuments/2")
                    .content(extractFileContent("controller/axapta/outboundDocuments.json"))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk());

        var modifiedDoc = transportationUnitDocumentMapper.findById(1L);
        softly.assertThat(modifiedDoc).isEqualTo(
            document(
                2L,
                TransportationUnitDocumentStatus.NEW,
                List.of("url2", "url5", "url7"),
                documents,
                LocalDateTime.of(2022, 3, 25, 21, 34, 55)
            ).setId(1L)
        );
    }

    private static TransportationUnitDocument document(
        Long transportationUnitId,
        TransportationUnitDocumentStatus status,
        List<String> documentUrs,
        List<Document> documents,
        LocalDateTime documentDate
    ) {
        return new TransportationUnitDocument()
            .setTransportationUnitId(transportationUnitId)
            .setStatus(status)
            .setDocumentUrls(documentUrs)
            .setDocuments(documents)
            .setDocumentDate(documentDate);
    }
}
