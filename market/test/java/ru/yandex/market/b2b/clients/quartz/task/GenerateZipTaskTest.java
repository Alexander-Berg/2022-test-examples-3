package ru.yandex.market.b2b.clients.quartz.task;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import ru.yandex.market.b2b.clients.AbstractFunctionalTest;
import ru.yandex.market.b2b.clients.Documents;
import ru.yandex.market.b2b.clients.impl.DocumentDaoImpl;
import ru.yandex.market.b2b.clients.impl.PaymentInvoiceNumberDaoImpl;
import ru.yandex.market.b2b.logbroker.mcrm.MultiOrderPaymentInvoiceLogbrokerEventPublisher;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.mj.generated.server.model.DocumentDto;
import ru.yandex.mj.generated.server.model.DocumentType;
import ru.yandex.mj.generated.server.model.GenerationStatusType;

public class GenerateZipTaskTest extends AbstractFunctionalTest {
    @Autowired
    private GenerateZipTask task;
    @Autowired
    private DocumentDaoImpl documentDao;
    @Autowired
    private PaymentInvoiceNumberDaoImpl paymentInvoiceNumberDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MultiOrderPaymentInvoiceLogbrokerEventPublisher publisherMock;
    @Autowired
    private MdsS3Client mdsS3ClientMock;

    @BeforeEach
    void setUp() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "document", "invoice_number");
        Mockito.reset(publisherMock, mdsS3ClientMock);
    }

    @Test
    public void testGenerate() {
        // подготовка документа на отправку
        DocumentDto doc1 = Documents.random(false);
        doc1.setType(DocumentType.PAYMENT_INVOICE);
        DocumentDto doc2 = Documents.random(false);
        doc2.setType(DocumentType.PAYMENT_INVOICE);

        DocumentDto doc1z = Documents.clone(doc1);
        doc1z.setGenerationStatus(GenerationStatusType.ZIP);
        doc1z.setType(DocumentType.ZIP_PAYMENT_INVOICES);
        doc1z.setUrl("path/123-aaa-bbb-ccc.zip");

        DocumentDto doc2z = Documents.clone(doc2);
        doc2z.setGenerationStatus(GenerationStatusType.ZIP);
        doc2z.setType(DocumentType.ZIP_PAYMENT_INVOICES);
        doc2z.setUrl("path/123-aaa-bbb-ccc.zip");

        documentDao.save(List.of(doc1, doc2, doc1z, doc2z));
        paymentInvoiceNumberDao.getInvoiceNumber("aaa-bbb-ccc", List.of(doc1.getOrder(), doc2.getOrder()));

        // запуск таска
        task.start();

        // проверка аплоада зипа
        Mockito.verify(mdsS3ClientMock, Mockito.times(1)).upload(Mockito.any(), Mockito.any());

        // проверка отправки
        Mockito.verify(publisherMock, Mockito.times(1))
                .publishEvent(Mockito.any());

        // проверка статусов
        List<Map<String, Object>> documents =
                jdbcTemplate.queryForList("SELECT generation_status FROM document WHERE type = 4"); // zip
        Assertions.assertEquals(2, documents.size());
        Assertions.assertEquals(0, documents.get(0).get("generation_status")); // NOT_NEED
        Assertions.assertEquals(0, documents.get(1).get("generation_status")); // NOT_NEED
    }

    @Test
    public void testGenerateExceptionWhileDownload() {
        // подготовка документа на отправку
        DocumentDto doc1 = Documents.random(false);
        doc1.setType(DocumentType.PAYMENT_INVOICE);
        DocumentDto doc2 = Documents.random(false);
        doc2.setType(DocumentType.PAYMENT_INVOICE);

        DocumentDto doc1z = Documents.clone(doc1);
        doc1z.setGenerationStatus(GenerationStatusType.ZIP);
        doc1z.setType(DocumentType.ZIP_PAYMENT_INVOICES);
        doc1z.setUrl("path/123-aaa-bbb-ccc.zip");

        DocumentDto doc2z = Documents.clone(doc2);
        doc2z.setGenerationStatus(GenerationStatusType.ZIP);
        doc2z.setType(DocumentType.ZIP_PAYMENT_INVOICES);
        doc2z.setUrl("path/123-aaa-bbb-ccc.zip");

        documentDao.save(List.of(doc1, doc2, doc1z, doc2z));
        paymentInvoiceNumberDao.getInvoiceNumber("aaa-bbb-ccc", List.of(doc1.getOrder(), doc2.getOrder()));

        Mockito.when(mdsS3ClientMock.download(Mockito.any(), Mockito.any())).thenThrow(new RuntimeException());

        // запуск таска
        task.start();

        // проверка аплоада зипа
        Mockito.verify(mdsS3ClientMock, Mockito.times(0)).upload(Mockito.any(), Mockito.any());

        // проверка отправки
        Mockito.verify(publisherMock, Mockito.times(0))
                .publishEvent(Mockito.any());

        // проверка статусов
        List<Map<String, Object>> documents =
                jdbcTemplate.queryForList("SELECT generation_status FROM document WHERE type = 4"); // zip
        Assertions.assertEquals(2, documents.size());
        Assertions.assertEquals(2, documents.get(0).get("generation_status")); // ZIP
        Assertions.assertEquals(2, documents.get(1).get("generation_status")); // ZIP
    }

    @Test
    public void testGenerateExceptionWhilePublish() {
        // подготовка документа на отправку
        DocumentDto doc1 = Documents.random(false);
        doc1.setType(DocumentType.PAYMENT_INVOICE);
        DocumentDto doc2 = Documents.random(false);
        doc2.setType(DocumentType.PAYMENT_INVOICE);

        DocumentDto doc1z = Documents.clone(doc1);
        doc1z.setGenerationStatus(GenerationStatusType.ZIP);
        doc1z.setType(DocumentType.ZIP_PAYMENT_INVOICES);
        doc1z.setUrl("path/123-aaa-bbb-ccc.zip");

        DocumentDto doc2z = Documents.clone(doc2);
        doc2z.setGenerationStatus(GenerationStatusType.ZIP);
        doc2z.setType(DocumentType.ZIP_PAYMENT_INVOICES);
        doc2z.setUrl("path/123-aaa-bbb-ccc.zip");

        documentDao.save(List.of(doc1, doc2, doc1z, doc2z));
        paymentInvoiceNumberDao.getInvoiceNumber("aaa-bbb-ccc", List.of(doc1.getOrder(), doc2.getOrder()));

        Mockito.doThrow(new RuntimeException()).when(publisherMock).publishEvent(Mockito.any());

        // запуск таска
        task.start();

        // проверка аплоада зипа
        Mockito.verify(mdsS3ClientMock, Mockito.times(2)).upload(Mockito.any(), Mockito.any());

        // проверка отправки
        Mockito.verify(publisherMock, Mockito.times(1))
                .publishEvent(Mockito.any());

        // проверка статусов
        List<Map<String, Object>> documents = jdbcTemplate
                .queryForList("SELECT generation_status FROM document WHERE type = 4 ORDER BY \"order\""); // zip
        Assertions.assertEquals(2, documents.size());
        Assertions.assertEquals(2, documents.get(0).get("generation_status")); // ZIP
        Assertions.assertEquals(0, documents.get(1).get("generation_status")); // NOT_NEED

        // почистим
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "document", "invoice_number");
    }
}
