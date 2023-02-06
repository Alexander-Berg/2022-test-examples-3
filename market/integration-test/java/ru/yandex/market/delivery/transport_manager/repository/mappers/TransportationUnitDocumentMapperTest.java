package ru.yandex.market.delivery.transport_manager.repository.mappers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Document;
import ru.yandex.market.delivery.transport_manager.domain.entity.DocumentType;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitDocument;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitDocumentStatus;

@DatabaseSetup({
    "/repository/transportation/multiple_transportations_deps.xml",
    "/repository/transportation/multiple_transportations.xml"
})
class TransportationUnitDocumentMapperTest extends AbstractContextualTest {
    @Autowired
    private TransportationUnitDocumentMapper mapper;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2022-08-07T21:00:00.00Z"), ZoneOffset.UTC);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit_documents/base_document.xml")
    void findById() {
        var doc = mapper.findById(1L);
        softly.assertThat(doc)
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
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit_documents/base_document.xml")
    void setStatus() {
        mapper.insert(
            document(
                3L,
                TransportationUnitDocumentStatus.NEW,
                List.of("aa", "bb"),
                List.of(),
                LocalDateTime.of(2021, 9, 12, 17, 0, 0)
            )
                .setCreated(LocalDateTime.of(2021, 9, 12, 17, 0, 0))
                .setUpdated(LocalDateTime.of(2021, 9, 12, 17, 0, 0))
        );
        mapper.setStatus(List.of(1L), TransportationUnitDocumentStatus.ERROR, LocalDateTime.now(clock));

        softly.assertThat(mapper.findById(1L).getStatus()).isEqualTo(TransportationUnitDocumentStatus.ERROR);
        softly.assertThat(mapper.findById(2L).getStatus()).isEqualTo(TransportationUnitDocumentStatus.NEW);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit_documents/base_document.xml")
    void findOldestWithStatus() {
        mapper.insert(
            document(
                3L,
                TransportationUnitDocumentStatus.NEW,
                List.of("aa", "bb"),
                List.of(),
                LocalDateTime.of(2021, 9, 12, 17, 0, 0)
            )
                .setCreated(LocalDateTime.of(2021, 9, 12, 17, 0, 0))
                .setUpdated(LocalDateTime.of(2021, 9, 12, 17, 0, 0))
        );
        mapper.insert(
            document(
                4L,
                TransportationUnitDocumentStatus.NEW,
                List.of("aa", "bb"),
                List.of(),
                LocalDateTime.of(2021, 9, 12, 17, 0, 0)
            )
                .setCreated(LocalDateTime.of(2021, 10, 12, 17, 0, 0))
                .setUpdated(LocalDateTime.of(2021, 10, 12, 17, 0, 0))
        );
        mapper.insert(
            document(
                5L,
                TransportationUnitDocumentStatus.NEW,
                List.of("aa", "bb"),
                List.of(),
                LocalDateTime.of(2021, 9, 12, 17, 0, 0)
            )
                .setCreated(LocalDateTime.of(2021, 11, 12, 17, 0, 0))
                .setUpdated(LocalDateTime.of(2021, 11, 12, 17, 0, 0))
        );

        var docs = mapper.findOldestWithStatus(TransportationUnitDocumentStatus.NEW, 2);

        softly.assertThat(docs).hasSize(2);
        softly.assertThat(docs).containsExactlyInAnyOrder(
            document(
                3L,
                TransportationUnitDocumentStatus.NEW,
                List.of("aa", "bb"),
                List.of(),
                LocalDateTime.of(2021, 9, 12, 17, 0, 0)
            ).setId(2L),
            document(
                4L,
                TransportationUnitDocumentStatus.NEW,
                List.of("aa", "bb"),
                List.of(),
                LocalDateTime.of(2021, 9, 12, 17, 0, 0)
            ).setId(3L)
        );
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit_documents/base_document.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_unit_documents/after/after_insert.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insert() {
        var newDoc = document(
            3L,
            TransportationUnitDocumentStatus.NEW,
            List.of("aa", "bb"),
            List.of(new Document("aa", DocumentType.TORG13), new Document("bb", DocumentType.TRN)),
            LocalDateTime.of(2021, 9, 12, 17, 0, 0)
        );
        mapper.insert(
            newDoc.setCreated(LocalDateTime.now(clock)).setUpdated(LocalDateTime.now(clock))
        );

        var doc = mapper.findById(2L);

        softly.assertThat(doc)
            .isEqualTo(newDoc.setId(2L));
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit_documents/documents.xml")
    void getByUnit() {
        TransportationUnitDocument document = mapper.getByUnit(2L);
        TransportationUnitDocument expected = new TransportationUnitDocument()
            .setId(1L)
            .setStatus(TransportationUnitDocumentStatus.NEW)
            .setDocumentUrls(List.of("doc6", "doc7"))
            .setDocuments(List.of(
                new Document("doc6", DocumentType.TORG13),
                new Document("doc7", DocumentType.TRN)
            ))
            .setTransportationUnitId(2L)
            .setDocumentDate(LocalDateTime.of(2021, 7, 12, 17, 0));

        assertThatModelEquals(expected, document);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit_documents/documents.xml")
    void getStatus() {
        softly.assertThat(mapper.getStatus(2L)).isEqualTo(TransportationUnitDocumentStatus.SUCCESS);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit_documents/documents.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_unit_documents/after/after_status_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void switchStatusReturningCount() {
        int count = mapper.switchStatusReturningCount(
            3L,
            TransportationUnitDocumentStatus.PROCESSING,
            TransportationUnitDocumentStatus.LGW_SENT
        );
        softly.assertThat(count).isEqualTo(1);

        count = mapper.switchStatusReturningCount(
            1L,
            TransportationUnitDocumentStatus.PROCESSING,
            TransportationUnitDocumentStatus.ERROR
        );
        softly.assertThat(count).isEqualTo(0);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit_documents/documents.xml")
    void getByStatusUpdatedAfter() {
        List<TransportationUnitDocument> byStatusUpdatedAfter = mapper.getByStatusUpdatedAfter(
            LocalDateTime.of(2021, 7, 13, 12, 0),
            TransportationUnitDocumentStatus.NEW
        );

        TransportationUnitDocument expected = new TransportationUnitDocument()
            .setStatus(TransportationUnitDocumentStatus.NEW)
            .setTransportationUnitId(6L)
            .setDocumentUrls(List.of("doc6", "doc7", "doc8"))
            .setDocuments(List.of())
            .setDocumentDate(LocalDateTime.of(2021, 7, 12, 17, 0));

        softly.assertThat(byStatusUpdatedAfter.size()).isEqualTo(1);
        assertThatModelEquals(expected, byStatusUpdatedAfter.get(0));
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
