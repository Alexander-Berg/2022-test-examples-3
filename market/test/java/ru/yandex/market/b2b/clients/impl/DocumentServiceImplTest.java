package ru.yandex.market.b2b.clients.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.b2b.clients.AbstractFunctionalTest;
import ru.yandex.market.b2b.clients.DocumentTypes;
import ru.yandex.market.b2b.clients.Documents;
import ru.yandex.market.b2b.clients.SerializationService;
import ru.yandex.market.b2b.clients.common.ForwardableDocumentStatus;
import ru.yandex.market.b2b.clients.mock.ExecuteCallMock;
import ru.yandex.market.b2b.clients.quartz.task.ForwardDocumentTask;
import ru.yandex.mj.generated.client.yadoc.api.DocumentsApiClient;
import ru.yandex.mj.generated.client.yadoc.model.DocumentStatusInfo;
import ru.yandex.mj.generated.client.yadoc.model.TransformationStatus;
import ru.yandex.mj.generated.server.model.DocumentDto;
import ru.yandex.mj.generated.server.model.DocumentPrintStatus;
import ru.yandex.mj.generated.server.model.DocumentResponseDto;
import ru.yandex.mj.generated.server.model.DocumentResponseDtoPrintStatus;
import ru.yandex.mj.generated.server.model.ForwardDestination;

public class DocumentServiceImplTest extends AbstractFunctionalTest {

    @Autowired
    private DocumentServiceImpl documentService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private SerializationService serializationService;
    @Autowired
    private DocumentsApiClient documentsApiClient;
    @Autowired
    private ForwardDocumentTask task;

    @BeforeEach
    void setUp() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "document", "document_forwardable");
        Mockito.reset(documentsApiClient);
    }

    @Test
    public void testAddDocuments() {
        DocumentDto document = addDocument(false);

        assertDocumentInDb(document);
        Long forwardableCount = jdbcTemplate.queryForObject("SELECT count(*) FROM document_forwardable", Long.class);
        Assertions.assertEquals(0, forwardableCount);
    }

    @Test
    public void testAddDocumentsWithForwardable() {
        DocumentDto document = addDocument(true);

        assertDocumentInDb(document);
        assertDocumentForwardableInDb(document, ForwardableDocumentStatus.PENDING);
    }

    @Test
    public void testGetDocuments() {
        DocumentDto document = addDocument(false);

        List<DocumentResponseDto> actualDocuments = documentService.getDocuments(document.getOrder());
        Assertions.assertEquals(1, actualDocuments.size());

        assertDocumentEquals(document, null, actualDocuments.get(0));
        Assertions.assertNull(actualDocuments.get(0).getPrintStatus());
    }

    @Test
    public void testGetDocumentsWithPrintStatusPending() {
        DocumentDto document = addDocument(true);

        List<DocumentResponseDto> actualDocuments = documentService.getDocuments(document.getOrder(), true);
        Assertions.assertEquals(1, actualDocuments.size());

        DocumentResponseDtoPrintStatus status = new DocumentResponseDtoPrintStatus()
                .status(DocumentPrintStatus.PENDING);
        assertDocumentEquals(document, status, actualDocuments.get(0));
    }

    @Test
    public void testGetDocumentsWithPrintStatusDone() {
        DocumentStatusInfo printStatus = new DocumentStatusInfo()
                .status(TransformationStatus.DONE)
                .statusMessage("Test done");
        Mockito.when(documentsApiClient.apiDocumentsStatusBySourceIdGet(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new ExecuteCallMock<>(printStatus));

        DocumentDto document = addDocument(true);

        task.start();

        List<DocumentResponseDto> actualDocuments = documentService.getDocuments(document.getOrder(), true);
        Assertions.assertEquals(1, actualDocuments.size());

        DocumentResponseDtoPrintStatus expectStatus = new DocumentResponseDtoPrintStatus()
                .status(DocumentPrintStatus.DONE)
                .message(printStatus.getStatusMessage());
        assertDocumentEquals(document, expectStatus, actualDocuments.get(0));
    }

    @Test
    public void testGetDocumentsWithPrintStatusError() {
        DocumentDto document = addDocument(true);

        task.start();

        List<DocumentResponseDto> actualDocuments = documentService.getDocuments(document.getOrder(), true);
        Assertions.assertEquals(1, actualDocuments.size());

        DocumentResponseDtoPrintStatus expectStatus = new DocumentResponseDtoPrintStatus()
                .status(DocumentPrintStatus.ERROR);
        assertDocumentEquals(document, expectStatus, actualDocuments.get(0));
    }

    @Test
    public void testGetDocumentsByList() {
        DocumentDto document = addDocument(false);
        DocumentDto document2 = addDocument(false);
        DocumentDto document3 = addDocument(false);

        List<DocumentResponseDto> allDocuments = documentService.getDocuments(
                List.of(document.getOrder(), document2.getOrder(), document3.getOrder()));

        Assertions.assertEquals(3, allDocuments.size());

        for (DocumentDto doc : List.of(document, document2, document3)) {
            boolean find = false;
            for (DocumentResponseDto response : allDocuments) {
                if (response.getOrder().equals(doc.getOrder())) {
                    find = true;
                    assertDocumentEquals(doc, null, response);
                    Assertions.assertNull(response.getPrintStatus());
                    break;
                }
            }
            Assertions.assertTrue(find);
        }
    }

    private DocumentDto addDocument(boolean withForwardAndMeta) {
        DocumentDto document = Documents.random(withForwardAndMeta);
        transactionTemplate.execute(status -> {
            documentService.addDocuments(List.of(document));
            return null;
        });

        return document;
    }

    private void assertDocumentInDb(DocumentDto expected) {
        List<Map<String, Object>> select = jdbcTemplate.queryForList("SELECT * FROM document");
        Assertions.assertEquals(1, select.size());
        Map<String, Object> row = select.get(0);
        Long order = (Long) row.get("order");
        Integer type = (Integer) row.get("type");
        String number = (String) row.get("number");
        Timestamp date = (Timestamp) row.get("date");
        String url = (String) row.get("url");
        PGobject meta = (PGobject) row.get("meta");
        Map<String, ?> metaMap = jsonToMap(meta);

        Assertions.assertEquals(expected.getOrder(), BigDecimal.valueOf(order));
        Assertions.assertEquals(expected.getType(), DocumentTypes.of(type));
        Assertions.assertEquals(expected.getNumber(), number);
        Assertions.assertEquals(
                expected.getDate().toInstant().truncatedTo(ChronoUnit.MILLIS),
                date.toInstant().truncatedTo(ChronoUnit.MILLIS));
        Assertions.assertEquals(expected.getUrl(), url);
        Assertions.assertEquals(expected.getMeta(), metaMap);
    }

    private void assertDocumentForwardableInDb(DocumentDto expected, ForwardableDocumentStatus expectedStatus) {
        List<Map<String, Object>> select = jdbcTemplate.queryForList("SELECT * FROM document_forwardable");
        Assertions.assertEquals(1, select.size());
        Map<String, Object> row = select.get(0);
        Long order = (Long) row.get("order");
        Integer type = (Integer) row.get("type");
        String number = (String) row.get("number");
        String forwardDestination = (String) row.get("forward_destination");
        String forwardStatus = (String) row.get("forward_status");
        Timestamp createTimestamp = (Timestamp) row.get("create_timestamp");
        Timestamp forwardTimestamp = (Timestamp) row.get("forward_timestamp");

        Assertions.assertEquals(expected.getOrder(), BigDecimal.valueOf(order));
        Assertions.assertEquals(expected.getType(), DocumentTypes.of(type));
        Assertions.assertEquals(expected.getNumber(), number);
        Assertions.assertEquals(expected.getForward(), ForwardDestination.valueOf(forwardDestination));
        Assertions.assertEquals(expectedStatus, ForwardableDocumentStatus.valueOf(forwardStatus));
        Assertions.assertNotNull(createTimestamp);
        Assertions.assertNull(forwardTimestamp);
    }

    private void assertDocumentEquals(DocumentDto expectedDocument,
                                      DocumentResponseDtoPrintStatus expectedPrintStatus,
                                      DocumentResponseDto actual) {
        Assertions.assertEquals(expectedDocument.getNumber(), actual.getNumber());
        Assertions.assertEquals(expectedDocument.getOrder(), actual.getOrder());
        Assertions.assertEquals(expectedDocument.getType(), actual.getType());
        Assertions.assertEquals(expectedDocument.getUrl(), actual.getUrl());
        Assertions.assertTrue(expectedDocument.getDate().truncatedTo(ChronoUnit.MILLIS)
                .isEqual(actual.getDate().truncatedTo(ChronoUnit.MILLIS)));

        if (expectedPrintStatus != null) {
            Assertions.assertNotNull(actual.getPrintStatus());
            Assertions.assertEquals(expectedPrintStatus.getStatus(), actual.getPrintStatus().getStatus());
            Assertions.assertEquals(expectedPrintStatus.getMessage(), actual.getPrintStatus().getMessage());
        } else {
            Assertions.assertNull(actual.getPrintStatus());
        }
    }

    private Map<String, ?> jsonToMap(PGobject pgo) {
        if (pgo == null || pgo.getValue() == null) {
            return null;
        }

        try {
            return serializationService.deserializeToMap(pgo.getValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
