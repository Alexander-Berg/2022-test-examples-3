package ru.yandex.market.mdm.integration.test.http;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mdm.http.MdmDocument.AddDocumentRelationsRequest;
import ru.yandex.market.mdm.http.MdmDocument.AddDocumentRelationsResponse;
import ru.yandex.market.mdm.http.MdmDocument.AddDocumentsResponse;
import ru.yandex.market.mdm.http.MdmDocument.AddDocumentsResponse.DocumentResponse;
import ru.yandex.market.mdm.http.MdmDocument.AddDocumentsResponse.Status;
import ru.yandex.market.mdm.http.MdmDocument.AddSupplierDocumentsRequest;
import ru.yandex.market.mdm.http.MdmDocument.AddSupplierDocumentsRequest.DocumentAddition;
import ru.yandex.market.mdm.http.MdmDocument.AddSupplierDocumentsRequest.DocumentAddition.ScanFile;
import ru.yandex.market.mdm.http.MdmDocument.Document;
import ru.yandex.market.mdm.http.MdmDocument.Document.DocumentType;
import ru.yandex.market.mdm.http.MdmDocument.DocumentOfferRelation;
import ru.yandex.market.mdm.http.MdmDocument.FindDocumentByRegistrationNumberRequest;
import ru.yandex.market.mdm.http.MdmDocument.FindDocumentsResponse;
import ru.yandex.market.mdm.http.MdmDocument.FindSupplierDocumentRelationsRequest;
import ru.yandex.market.mdm.http.MdmDocument.FindSupplierDocumentRelationsResponse;
import ru.yandex.market.mdm.http.MdmDocument.FindSupplierDocumentsRequest;
import ru.yandex.market.mdm.http.MdmDocument.RemoveDocumentOfferRelationsRequest;
import ru.yandex.market.mdm.http.MdmDocument.RemoveDocumentOfferRelationsResponse;
import ru.yandex.market.mdm.http.SupplierDocumentService;
import ru.yandex.market.mdm.integration.test.config.HttpIntegrationTestConfig.CommonTestParameters;

import static java.time.temporal.ChronoUnit.YEARS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mdm.http.MdmDocument.FindSupplierDocumentRelationsResponse.Status.OK;


/**
 * Последний рубеж обороны работоспособности MDM.
 * <p>
 * Кейс 1:
 * 0. Очищаем мусор.
 * 1. Создаём документ.
 * 2. Ищем его по рег. номеру.
 * 3. Обновляем документ.
 * 4. Помечаем документ как удалённый.
 * <p>
 * Кейс 2:
 * 0. Очищаем мусор.
 * 1. Создаём связь с оффером.
 * 2. Ищем связь с оффером.
 * 3. Удаляем связь с оффером.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MdmHandlesTest extends BaseHttpIntegrationTestClass {

    private static final String REG_NUMBER = "TEST_DOGE_0315609004";
    private static final String ORG_REG_NUMBER = "TEST_DOGE_INC_123456789";
    private static final String SHOP_SKU = "test-doge-shop-sku";

    // Данные об используемой картинке документа. Хранится в аватарнице для надёжности, хотя по факту может быть
    // любой url из сети (при сохранении документа она всё равно пересохраняется с нужным ИД и именем в аватарницу).
    private static final String SCAN_IMAGE_NAME = "mdm-peos-certificate";
    private static final String SCAN_IMAGE_ORIG_NAME = "sobaken.jpg";
    private static final String SCAN_IMG_GROUP = "1100559";

    // http://avatars.mds.yandex.net/get-market-mdm/1100559/mdm-peos-certificate/orig
    private static final String SCAN_IMAGE_URL = String.format(
        "http://avatars.mds.yandex.net/get-market-mdm/%s/%s/orig", SCAN_IMG_GROUP, SCAN_IMAGE_NAME);

    @Autowired
    private SupplierDocumentService mdmDocumentService;
    @Autowired
    private CommonTestParameters config;

    private static void assertDocumentsEqual(Document expected, Document actual) {
        assertEquals(expected.getRegistrationNumber(), actual.getRegistrationNumber());
        assertEquals(expected.getCertificationOrgRegNumber(), actual.getCertificationOrgRegNumber());
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getStartDate(), actual.getStartDate());
        assertEquals(expected.getEndDate(), actual.getEndDate());
    }

    private static ScanFile getScanFile() {
        return ScanFile.newBuilder()
            .setFileName(SCAN_IMAGE_ORIG_NAME)
            .setUrl(SCAN_IMAGE_URL)
            .build();
    }

    @Before
    public void setup() {
        log.info("Using {} as mdm.api.url for remote services.", config.getMdmHost());
        log.info("Using {} as mdm.integration-test.root-uri for raw dev handles.", config.getIntTestHandlesHost());
        cleanup();
    }

    @After
    public void cleanup() {
        String response = restTemplate.getForObject(config.getIntTestHandlesHost() +
            "/int-test/hard-remove-document?regNumber=" + REG_NUMBER, String.class);
        assertEquals("Done", response);
    }

    @Test
    public void testDocumentCRUD() throws IOException {
        checkNoSuchDocument();
        Document document = createQualityDocument();
        findThisDocument(document);
        document = updateThisDocument(document);
        findThisDocument(document);
    }

    @Test
    public void testOfferRelationsCRD() throws IOException {
        checkNoSuchDocument();
        Document document = createQualityDocument();
        DocumentOfferRelation relation = createDocumentOfferRelation(document);
        findThisRelation(relation, true);
        deleteDocumentOfferRelation(relation);
    }

    private void checkNoSuchDocument() {
        FindDocumentByRegistrationNumberRequest findByRegNumberRequest = FindDocumentByRegistrationNumberRequest
            .newBuilder().setRegistrationNumber(REG_NUMBER).build();
        FindDocumentsResponse response =
            mdmDocumentService.findSupplierDocumentByRegistrationNumber(findByRegNumberRequest);
        assertEquals(FindDocumentsResponse.Status.OK, response.getStatus());
        assertEquals(0, response.getDocumentCount());
    }

    private Document createQualityDocument() throws IOException {
        ScanFile scanFile = getScanFile();

        Document document = Document.newBuilder()
            .setRegistrationNumber(REG_NUMBER)
            .setCertificationOrgRegNumber(ORG_REG_NUMBER)
            .setStartDate(LocalDate.now().minus(1L, YEARS).toEpochDay())
            .setEndDate(LocalDate.now().plus(1L, YEARS).toEpochDay())
            .setType(DocumentType.ENVIRONMENTAL_SAFETY_CERTIFICATE)
            .build();

        AddSupplierDocumentsRequest request = AddSupplierDocumentsRequest.newBuilder()
            .setSupplierId(config.getSupplierId())
            .addDocument(DocumentAddition.newBuilder()
                .addNewScanFile(scanFile)
                .setDocument(document)
                .build()
            ).build();
        AddDocumentsResponse response = mdmDocumentService.addSupplierDocuments(request);
        assertEquals(1, response.getDocumentResponseCount());
        assertEquals(Status.OK, response.getStatus());
        DocumentResponse documentResponse = response.getDocumentResponse(0);
        Document savedDocument = documentResponse.getDocument();
        assertDocumentsEqual(document, savedDocument);
        assertEquals(1, savedDocument.getPictureCount());
        return savedDocument;
    }

    private void findThisDocument(Document document) {
        // а) Поиск по сапплаеру
        FindSupplierDocumentsRequest findBySupplierRequest = FindSupplierDocumentsRequest.newBuilder()
            .setSupplierId(config.getSupplierId()).build();
        FindDocumentsResponse response = mdmDocumentService.findSupplierDocuments(findBySupplierRequest);
        assertEquals(FindDocumentsResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getDocumentCount());
        Document foundDocument = response.getDocument(0);
        assertDocumentsEqual(document, foundDocument);

        // б) Поиск по рег. номеру
        FindDocumentByRegistrationNumberRequest findByRegNumberRequest = FindDocumentByRegistrationNumberRequest
            .newBuilder().setRegistrationNumber(REG_NUMBER).build();
        response = mdmDocumentService.findSupplierDocumentByRegistrationNumber(findByRegNumberRequest);
        assertEquals(FindDocumentsResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getDocumentCount());
        foundDocument = response.getDocument(0);
        assertDocumentsEqual(document, foundDocument);
    }

    private Document updateThisDocument(Document document) {
        document = document.toBuilder()
            .setType(DocumentType.CERTIFICATE_OF_CONFORMITY)
            .setStartDate(LocalDate.now().minusYears(10).toEpochDay())
            .setEndDate(LocalDate.now().plusYears(10).toEpochDay())
            .build();
        AddSupplierDocumentsRequest request = AddSupplierDocumentsRequest.newBuilder()
            .setSupplierId(config.getSupplierId())
            .addDocument(DocumentAddition.newBuilder()
                .addNewScanFile(getScanFile())
                .setDocument(document)
                .build()
            ).build();
        AddDocumentsResponse response = mdmDocumentService.addSupplierDocuments(request);
        assertEquals(Status.OK, response.getStatus());
        assertEquals(1, response.getDocumentResponseCount());
        DocumentResponse documentResponse = response.getDocumentResponse(0);
        Document savedDocument = documentResponse.getDocument();
        assertDocumentsEqual(document, savedDocument);
        return savedDocument;
    }

    private DocumentOfferRelation createDocumentOfferRelation(Document document) {
        AddDocumentRelationsRequest request = AddDocumentRelationsRequest.newBuilder()
            .addDocumentOfferRelation(DocumentOfferRelation.newBuilder()
                .setSupplierId(config.getSupplierId())
                .setShopSku(SHOP_SKU)
                .setRegistrationNumber(document.getRegistrationNumber())
                .build())
            .build();

        AddDocumentRelationsResponse response = mdmDocumentService.addDocumentRelations(request);
        assertEquals(AddDocumentRelationsResponse.Status.OK, response.getStatus());
        assertEquals(1, response.getDocumentRelationsCount());
        assertTrue(response.getDocumentRelations(0).hasOfferRelation());
        DocumentOfferRelation relation = response.getDocumentRelations(0).getOfferRelation();

        assertEquals(config.getSupplierId(), relation.getSupplierId());
        assertEquals(SHOP_SKU, relation.getShopSku());
        assertEquals(document.getRegistrationNumber(), relation.getRegistrationNumber());
        return relation;
    }

    private void findThisRelation(DocumentOfferRelation relation, boolean shouldBeFound) {
        FindSupplierDocumentRelationsRequest request = FindSupplierDocumentRelationsRequest.newBuilder()
            .setRegistrationNumber(relation.getRegistrationNumber())
            .setSupplierId(relation.getSupplierId())
            .build();
        FindSupplierDocumentRelationsResponse response = mdmDocumentService.findSupplierDocumentRelations(request);
        assertEquals(OK, response.getStatus());
        assertTrue(response.hasOfferRelations());
        int found = response.getOfferRelations().getShopSkuCount();
        if (shouldBeFound) {
            assertEquals(1, found);
            assertEquals(relation.getShopSku(), response.getOfferRelations().getShopSku(0));
        } else {
            assertEquals(0, found);
        }
    }

    private void deleteDocumentOfferRelation(DocumentOfferRelation relation) {
        RemoveDocumentOfferRelationsRequest request = RemoveDocumentOfferRelationsRequest.newBuilder()
            .addRelation(relation).build();
        RemoveDocumentOfferRelationsResponse response = mdmDocumentService.removeDocumentOfferRelations(request);
        assertEquals(RemoveDocumentOfferRelationsResponse.Status.OK, response.getStatus());
        findThisRelation(relation, false);
    }
}
