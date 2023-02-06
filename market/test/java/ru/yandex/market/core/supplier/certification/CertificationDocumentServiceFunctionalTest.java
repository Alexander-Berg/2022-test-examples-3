package ru.yandex.market.core.supplier.certification;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.supplier.certification.error.CertificationDocumentException;
import ru.yandex.market.core.supplier.certification.model.CertificationDocument;
import ru.yandex.market.core.supplier.certification.model.CertificationDocumentType;
import ru.yandex.market.core.supplier.certification.model.MdmImageSet;
import ru.yandex.market.core.supplier.certification.model.MdmImageSetUpdate;
import ru.yandex.market.core.supplier.certification.model.ScanFile;
import ru.yandex.market.core.supplier.certification.model.response.AddCertificationDocumentResponse;
import ru.yandex.market.core.supplier.certification.model.response.AddCertificationDocumentsResponse;
import ru.yandex.market.core.supplier.certification.model.response.CertDocResponseStatus;
import ru.yandex.market.core.supplier.certification.model.response.relations.AddCertDocRelationsResponse;
import ru.yandex.market.core.supplier.certification.model.response.relations.CertDocRelationAddition;
import ru.yandex.market.core.supplier.certification.model.response.relations.CertDocRelationRemoval;
import ru.yandex.market.core.supplier.certification.model.response.relations.CertificationDocumentOfferRelation;
import ru.yandex.market.core.supplier.certification.model.response.relations.RemoveCertDocRelationsResponse;
import ru.yandex.market.core.tanker.model.UserMessage;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;
import ru.yandex.market.mbi.web.paging.SeekableSlice;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.mdm.http.MdmDocument;
import ru.yandex.market.mdm.http.SupplierDocumentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты сервиса сертификатов {@link CertificationDocumentService}
 */
@DbUnitDataSet(before = "CertificationDocumentServiceFunctionalTest.csv")
class CertificationDocumentServiceFunctionalTest extends FunctionalTest {

    private static final long SUPPLIER_ID = 774L;

    @Autowired
    SupplierDocumentService marketProtoSupplierDocumentService;
    @Autowired
    CertificationDocumentService certificationDocumentService;

    @Test
    @DisplayName("Тест успешного поиска сертификатов.")
    void testFindCertificationDocuments() {
        when(marketProtoSupplierDocumentService.findSupplierDocuments(any()))
                .thenReturn(MdmDocument.FindDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.FindDocumentsResponse.Status.OK)
                        .setNextOffsetKey("MQ")
                        .addDocument(createTestMdmDocument())
                        .build());

        CertificationDocumentFilter filter = new CertificationDocumentFilter.Builder().setTextQuery("number1").build();

        SeekableSlice<CertificationDocument<MdmImageSet>, String> slice = certificationDocumentService.searchDocuments(
                SUPPLIER_ID, filter, SeekSliceRequest.firstN(10));

        Assertions.assertEquals("MQ", slice.nextSliceKey().orElse(null));
        CertificationDocument<MdmImageSet> doc = slice.entries().get(0);

        Assertions.assertEquals(createTestDocument(), doc);

        verify(marketProtoSupplierDocumentService).findSupplierDocuments(
                argThat(request -> request.getSupplierId() == SUPPLIER_ID
                        && request.getSearchQuery().equals("number1")
                        && request.getLimit() == 10));
    }

    @Test
    @DisplayName("Тест ошибки при статусе ошибка от mboc при поиске сертификатов.")
    void testFindCertificationDocumentsBadStatus() {
        when(marketProtoSupplierDocumentService.findSupplierDocuments(any()))
                .thenReturn(MdmDocument.FindDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.FindDocumentsResponse.Status.ERROR)
                        .build());
        CertificationDocumentFilter filter = new CertificationDocumentFilter.Builder().setTextQuery("number1").build();
        Assertions.assertThrows(CertificationDocumentException.class, () ->
                certificationDocumentService.searchDocuments(SUPPLIER_ID, filter, SeekSliceRequest.firstN(10)));
    }

    @Test
    @DisplayName("Тест успешного создания списка сертификатов.")
    void testCreateCertificationDocuments() {
        when(marketProtoSupplierDocumentService.addSupplierDocuments(any()))
                .thenReturn(MdmDocument.AddDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.AddDocumentsResponse.Status.OK)
                        .setError(MbocCommon.Message.newBuilder()
                                .setMessageCode("resp error code 1")
                                .setMustacheTemplate("resp error template 1")
                                .build())
                        .addDocumentResponse(MdmDocument.AddDocumentsResponse.DocumentResponse.newBuilder()
                                .setStatus(MdmDocument.AddDocumentsResponse.Status.OK)
                                .addError(MbocCommon.Message.newBuilder()
                                        .setMessageCode("doc error code 1")
                                        .setMustacheTemplate("doc error template 1")
                                        .build())
                                .setDocument(createTestMdmDocument())
                                .build())
                        .build());

        AddCertificationDocumentsResponse docsResponse = certificationDocumentService.createDocuments(
                SUPPLIER_ID, Collections.singletonList(createUploadTestDocument()));

        verify(
                marketProtoSupplierDocumentService).addSupplierDocuments(
                argThat(request -> {
                            if (request.getSupplierId() != SUPPLIER_ID) {
                                return false;
                            }
                            MdmDocument.AddSupplierDocumentsRequest.DocumentAddition documentAddition
                                    = request.getDocument(0);
                            MdmDocument.Document document = documentAddition.getDocument();
                            return documentAddition.getNewScanFile(0).equals(
                                    MdmDocument.AddSupplierDocumentsRequest.DocumentAddition.ScanFile.newBuilder()
                                            .setUrl("http://picture.ru/pict1")
                                            .setFileName("pict1.jpeg")
                                            .build()
                            )
                                    && document.getRegistrationNumber().equals("number1")
                                    && document.getStartDate() == 17500L && document.getEndDate() == 17865L
                                    && document.getType() == MdmDocument.Document.DocumentType.CERTIFICATE_OF_CONFORMITY;
                        }
                ));

        Assertions.assertEquals(CertDocResponseStatus.OK, docsResponse.getStatus().orElse(null));
        Assertions.assertEquals(
                new UserMessage.Builder()
                        .setMessageCode("resp error code 1")
                        .setDefaultTranslation("resp error template 1")
                        .build(), docsResponse.getError().orElse(null));
        for (AddCertificationDocumentResponse docResponse : docsResponse.getDocumentResponses()) {
            Assertions.assertEquals(docResponse.getStatus().orElse(null), CertDocResponseStatus.OK);
            Assertions.assertEquals(
                    new UserMessage.Builder()
                            .setMessageCode("doc error code 1")
                            .setDefaultTranslation("doc error template 1")
                            .build(), docResponse.getErrors().iterator().next());
            Assertions.assertEquals(createTestDocument(), docResponse.getDocument().orElse(null));
        }
    }

    @Test
    @DisplayName("Тест успешного создания списка сертификатов.")
    void testCreateCertificationDocumentsWithoutScanFiles() {
        when(marketProtoSupplierDocumentService.addSupplierDocuments(any()))
                .thenReturn(MdmDocument.AddDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.AddDocumentsResponse.Status.OK)
                        .addDocumentResponse(MdmDocument.AddDocumentsResponse.DocumentResponse.newBuilder()
                                .setStatus(MdmDocument.AddDocumentsResponse.Status.OK)
                                .setDocument(createTestMdmDocument())
                                .build())
                        .build());

        AddCertificationDocumentsResponse docsResponse = certificationDocumentService.createDocuments(
                SUPPLIER_ID, Collections.singletonList(createUploadTestDocumentWithoutScanFiles()));

        verify(
                marketProtoSupplierDocumentService).addSupplierDocuments(
                argThat(request -> {
                            MdmDocument.AddSupplierDocumentsRequest.DocumentAddition documentAddition
                                    = request.getDocument(0);
                            MdmDocument.Document document = documentAddition.getDocument();
                            return documentAddition.getNewScanFileList().isEmpty()
                                    && documentAddition.getDeletePictureMdmUrlList().isEmpty()
                                    && document.getRegistrationNumber().equals("number1");
                        }
                ));
        Assertions.assertEquals(CertDocResponseStatus.OK, docsResponse.getStatus().orElse(null));
    }

    @Test
    @DisplayName("Тест успешного получения списка shopSku сертификата по регистрационному номеру.")
    void testGetSupplierDocumentSkusDocuments() {
        when(marketProtoSupplierDocumentService.findSupplierDocumentRelations(any()))
                .thenReturn(MdmDocument.FindSupplierDocumentRelationsResponse.newBuilder()
                        .setStatus(MdmDocument.FindSupplierDocumentRelationsResponse.Status.OK)
                        .setNextOffsetKey("MQ")
                        .setOfferRelations(MdmDocument.FindSupplierDocumentRelationsResponse.SupplierOfferRelations.newBuilder()
                                .setRegistrationNumber("number1")
                                .setSupplierId((int) SUPPLIER_ID)
                                .addShopSku("sku1")
                                .addShopSku("sku2"))
                        .build()
                );

        SeekableSlice<String, String> slice =
                certificationDocumentService.getDocumentShopSkus(SUPPLIER_ID, "number1",
                        SeekSliceRequest.firstN(10));

        Assertions.assertEquals("MQ", slice.nextSliceKey().orElse(null));
        Assertions.assertIterableEquals(Arrays.asList("sku1", "sku2"), slice.entries());

        verify(marketProtoSupplierDocumentService).findSupplierDocumentRelations(
                argThat(request -> request.getSupplierId() == SUPPLIER_ID
                        && request.getRegistrationNumber().equals("number1")
                        && request.getLimit() == 10));
    }

    @Test
    @DisplayName("Тест успешного получения пустого списка shopSku сертификата по регистрационному номеру.")
    void testGetSupplierDocumentEmptySkusDocuments() {
        when(marketProtoSupplierDocumentService.findSupplierDocumentRelations(any()))
                .thenReturn(MdmDocument.FindSupplierDocumentRelationsResponse.newBuilder()
                        .setStatus(MdmDocument.FindSupplierDocumentRelationsResponse.Status.OK)
                        .setNextOffsetKey("MQ")
                        .build()
                );

        SeekableSlice<String, String> slice =
                certificationDocumentService.getDocumentShopSkus(SUPPLIER_ID, "number1",
                        SeekSliceRequest.firstN(10));

        Assertions.assertEquals("MQ", slice.nextSliceKey().orElse(null));
        Assertions.assertIterableEquals(Collections.emptyList(), slice.entries());

        verify(marketProtoSupplierDocumentService).findSupplierDocumentRelations(
                argThat(request -> request.getSupplierId() == SUPPLIER_ID
                        && request.getRegistrationNumber().equals("number1")
                        && request.getLimit() == 10));
    }

    @Test
    @DisplayName("Тест возникновения ошибки при получении статуса ошибка от mboc при поиске оферов сертификата.")
    void testGetSupplierDocumentSkusBadStatus() {
        when(marketProtoSupplierDocumentService.findSupplierDocumentRelations(any()))
                .thenReturn(MdmDocument.FindSupplierDocumentRelationsResponse.newBuilder()
                        .setStatus(MdmDocument.FindSupplierDocumentRelationsResponse.Status.ERROR)
                        .build()
                );
        Assertions.assertThrows(CertificationDocumentException.class, () ->
                certificationDocumentService.getDocumentShopSkus(
                        SUPPLIER_ID, "number1", SeekSliceRequest.firstN(10)));
    }

    @Test
    @DisplayName("Тест на успешное добавление офера к сертификату поставщика.")
    void testAddSupplierDocumentRelation() {
        when(marketProtoSupplierDocumentService.addDocumentRelations(any()))
                .thenReturn(MdmDocument.AddDocumentRelationsResponse.newBuilder()
                        .setStatus(MdmDocument.AddDocumentRelationsResponse.Status.OK)
                        .addDocumentRelations(
                                MdmDocument.AddDocumentRelationsResponse.DocumentRelationAddition.newBuilder()
                                        .setStatus(MdmDocument.AddDocumentRelationsResponse.Status.OK)
                                        .setOfferRelation(MdmDocument.DocumentOfferRelation.newBuilder()
                                                .setSupplierId((int) SUPPLIER_ID)
                                                .setRegistrationNumber("number1")
                                                .setShopSku("sku1")
                                                .build())
                                        .build())
                        .build());

        AddCertDocRelationsResponse response =
                certificationDocumentService.addDocumentShopSkusRelation(SUPPLIER_ID, Collections.singleton(
                        CertificationDocumentOfferRelation.builder()
                                .setRegistrationNumber("number1")
                                .setShopSku("sku1")
                                .build()));


        verify(
                marketProtoSupplierDocumentService).addDocumentRelations(
                argThat(request -> {
                            MdmDocument.DocumentOfferRelation relation = request.getDocumentOfferRelation(0);
                            return relation.getSupplierId() == SUPPLIER_ID && relation.getRegistrationNumber().equals("number1")
                                    && relation.getShopSku().equals("sku1");

                        }
                ));

        Assertions.assertEquals(CertDocResponseStatus.OK, response.getStatus());
        CertDocRelationAddition addition = response.getDocumentRelationAdditions().iterator().next();
        Assertions.assertEquals(CertDocResponseStatus.OK, addition.getStatus());
        CertificationDocumentOfferRelation relation = addition.getDocumentOfferRelation().get();
        Assertions.assertEquals("number1", relation.getRegistrationNumber());
        Assertions.assertEquals("sku1", relation.getShopSku());
    }

    @DisplayName("Тест на ошибки при добавлении офера к сертификату поставщика.")
    @Test
    void testAddSupplierDocumentRelationWithErrorStatus() {
        when(marketProtoSupplierDocumentService.addDocumentRelations(any()))
                .thenReturn(MdmDocument.AddDocumentRelationsResponse.newBuilder()
                        .setStatus(MdmDocument.AddDocumentRelationsResponse.Status.ERROR)
                        .setError(MbocCommon.Message.newBuilder()
                                .setMessageCode("resp error code 1")
                                .setMustacheTemplate("resp error template 1")
                                .build())
                        .addDocumentRelations(
                                MdmDocument.AddDocumentRelationsResponse.DocumentRelationAddition.newBuilder()
                                        .setStatus(MdmDocument.AddDocumentRelationsResponse.Status.ERROR)
                                        .setError(MbocCommon.Message.newBuilder()
                                                .setMessageCode("relation error code 1")
                                                .setMustacheTemplate("relation error template 1")
                                                .build())
                                        .build())
                        .build());

        AddCertDocRelationsResponse response =
                certificationDocumentService.addDocumentShopSkusRelation(SUPPLIER_ID, Collections.singleton(
                        CertificationDocumentOfferRelation.builder()
                                .setRegistrationNumber("number1")
                                .setShopSku("sku1")
                                .build()));

        Assertions.assertEquals(CertDocResponseStatus.ERROR, response.getStatus());
        CertDocRelationAddition addition = response.getDocumentRelationAdditions().iterator().next();
        Assertions.assertEquals(
                new UserMessage.Builder()
                        .setMessageCode("resp error code 1")
                        .setDefaultTranslation("resp error template 1")
                        .build(), response.getError().get());
        Assertions.assertEquals(
                new UserMessage.Builder()
                        .setMessageCode("relation error code 1")
                        .setDefaultTranslation("relation error template 1")
                        .build(), addition.getError().get());
        Assertions.assertEquals(CertDocResponseStatus.ERROR, addition.getStatus());
        CertificationDocumentOfferRelation defaultRelation = addition.getDocumentOfferRelation().get();
        Assertions.assertEquals("", defaultRelation.getRegistrationNumber());
        Assertions.assertEquals("", defaultRelation.getShopSku());
    }

    @DisplayName("Тест на успешное удаление офера от сертификата поставщика.")
    @Test
    void testRemoveSupplierDocumentRelation() {
        when(marketProtoSupplierDocumentService.removeDocumentOfferRelations(any()))
                .thenReturn(MdmDocument.RemoveDocumentOfferRelationsResponse.newBuilder()
                        .setStatus(MdmDocument.RemoveDocumentOfferRelationsResponse.Status.OK)
                        .addRelationResponse(
                                MdmDocument.RemoveDocumentOfferRelationsResponse.RemoveRelationResponse.newBuilder()
                                        .setStatus(MdmDocument.RemoveDocumentOfferRelationsResponse.Status.OK)
                                        .setRelation(MdmDocument.DocumentOfferRelation.newBuilder()
                                                .setSupplierId((int) SUPPLIER_ID)
                                                .setRegistrationNumber("number1")
                                                .setShopSku("sku1")
                                                .build())
                                        .build())
                        .build());

        RemoveCertDocRelationsResponse response =
                certificationDocumentService.removeDocumentShopSkusRelation(SUPPLIER_ID, Collections.singleton(
                        CertificationDocumentOfferRelation.builder()
                                .setRegistrationNumber("number1")
                                .setShopSku("sku1")
                                .build()));

        verify(
                marketProtoSupplierDocumentService).removeDocumentOfferRelations(
                argThat(request -> {
                            MdmDocument.DocumentOfferRelation relation = request.getRelation(0);
                            return relation.getSupplierId() == SUPPLIER_ID && relation.getRegistrationNumber().equals("number1")
                                    && relation.getShopSku().equals("sku1");

                        }
                ));

        Assertions.assertEquals(CertDocResponseStatus.OK, response.getStatus());
        CertDocRelationRemoval removal = response.getDocumentRelationRemovals().iterator().next();
        Assertions.assertEquals(CertDocResponseStatus.OK, removal.getStatus());
        CertificationDocumentOfferRelation relation = removal.getDocumentOfferRelation().get();
        Assertions.assertEquals("number1", relation.getRegistrationNumber());
        Assertions.assertEquals("sku1", relation.getShopSku());
    }

    @DisplayName("Тест на ошибки при удалении офера от сертификата поставщика.")
    @Test
    void testRemoveSupplierDocumentRelationWithErrorStatus() {
        when(marketProtoSupplierDocumentService.removeDocumentOfferRelations(any()))
                .thenReturn(MdmDocument.RemoveDocumentOfferRelationsResponse.newBuilder()
                        .setStatus(MdmDocument.RemoveDocumentOfferRelationsResponse.Status.ERROR)
                        .setError(MbocCommon.Message.newBuilder()
                                .setMessageCode("resp error code 1")
                                .setMustacheTemplate("resp error template 1")
                                .build())
                        .addRelationResponse(
                                MdmDocument.RemoveDocumentOfferRelationsResponse.RemoveRelationResponse.newBuilder()
                                        .setStatus(MdmDocument.RemoveDocumentOfferRelationsResponse.Status.ERROR)
                                        .setError(MbocCommon.Message.newBuilder()
                                                .setMessageCode("relation error code 1")
                                                .setMustacheTemplate("relation error template 1")
                                                .build())
                                        .build())
                        .build());

        RemoveCertDocRelationsResponse response =
                certificationDocumentService.removeDocumentShopSkusRelation(SUPPLIER_ID, Collections.singleton(
                        CertificationDocumentOfferRelation.builder()
                                .setRegistrationNumber("number1")
                                .setShopSku("sku1")
                                .build()));

        Assertions.assertEquals(CertDocResponseStatus.ERROR, response.getStatus());
        CertDocRelationRemoval removal = response.getDocumentRelationRemovals().iterator().next();
        Assertions.assertEquals(
                new UserMessage.Builder()
                        .setMessageCode("resp error code 1")
                        .setDefaultTranslation("resp error template 1")
                        .build(), response.getError().get());
        Assertions.assertEquals(
                new UserMessage.Builder()
                        .setMessageCode("relation error code 1")
                        .setDefaultTranslation("relation error template 1")
                        .build(), removal.getError().get());
        Assertions.assertEquals(CertDocResponseStatus.ERROR, removal.getStatus());
        CertificationDocumentOfferRelation defaultRelation = removal.getDocumentOfferRelation().get();
        Assertions.assertEquals("", defaultRelation.getRegistrationNumber());
        Assertions.assertEquals("", defaultRelation.getShopSku());
    }

    @DisplayName("Получить сертификат по регистрационному номеру.")
    @Test
    void testGetCertificationDocument() {
        when(marketProtoSupplierDocumentService.findSupplierDocumentByRegistrationNumber(any()))
                .thenReturn(MdmDocument.FindDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.FindDocumentsResponse.Status.OK)
                        .addDocument(createTestMdmDocument())
                        .build());
        CertificationDocument doc = certificationDocumentService.getCertificationDocument("number1").get();

        Assertions.assertEquals(createTestDocument(), doc);

        verify(marketProtoSupplierDocumentService).findSupplierDocumentByRegistrationNumber(
                argThat(request -> request.getRegistrationNumber().equals("number1")));
    }

    @DisplayName("Ошибка в случае если mboc вернул сертификат с несовпадающим регистрационным номером.")
    @Test
    void testGetCertificationDocumentRegNumberNotMatch() {
        when(marketProtoSupplierDocumentService.findSupplierDocumentByRegistrationNumber(any()))
                .thenReturn(MdmDocument.FindDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.FindDocumentsResponse.Status.OK)
                        .addDocument(createTestMdmDocument())
                        .build());
        Assertions.assertFalse(
                certificationDocumentService.getCertificationDocument("number2").isPresent());
    }

    @DisplayName("Ошибка в случае если mboc вернул статус не OK")
    @Test
    void testGetCertificationDocumentErrorStatus() {
        when(marketProtoSupplierDocumentService.findSupplierDocumentByRegistrationNumber(any()))
                .thenReturn(MdmDocument.FindDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.FindDocumentsResponse.Status.ERROR)
                        .build());
        Assertions.assertThrows(CertificationDocumentException.class, () ->
                certificationDocumentService.getCertificationDocument("number2"));
    }

    private MdmDocument.Document createTestMdmDocument() {
        return MdmDocument.Document.newBuilder()
                .setRegistrationNumber("number1")
                .setStartDate(17500)
                .setEndDate(17865)
                .setType(MdmDocument.Document.DocumentType.CERTIFICATE_OF_CONFORMITY)
                .addPicture("http://picture.ru/pict1")
                .build();
    }

    private CertificationDocument<MdmImageSet> createTestDocument() {
        return new CertificationDocument.Builder<MdmImageSet>()
                .setType(CertificationDocumentType.CERTIFICATE_OF_CONFORMITY)
                .setRegistrationNumber("number1")
                .setStartDate(LocalDate.ofEpochDay(17500))
                .setEndDate(LocalDate.ofEpochDay(17865))
                .setImages(MdmImageSet.of(Collections.singletonList("http://picture.ru/pict1")))
                .build();
    }

    private CertificationDocument<MdmImageSetUpdate> createUploadTestDocument() {
        return new CertificationDocument.Builder<MdmImageSetUpdate>()
                .setType(CertificationDocumentType.CERTIFICATE_OF_CONFORMITY)
                .setRegistrationNumber("number1")
                .setStartDate(LocalDate.ofEpochDay(17500))
                .setEndDate(LocalDate.ofEpochDay(17865))
                .setImages(MdmImageSetUpdate.builder()
                        .addNewScanFiles(Collections.singletonList(
                                ScanFile.builder()
                                        .setUrl("http://picture.ru/pict1")
                                        .setFileName("pict1.jpeg")
                                        .build()))
                        .addDeletePictureMdmUrls(Collections.singletonList("http://picture.ru/to_delete"))
                        .build())
                .build();
    }

    private CertificationDocument<MdmImageSetUpdate> createUploadTestDocumentWithoutScanFiles() {
        return new CertificationDocument.Builder<MdmImageSetUpdate>()
                .setType(CertificationDocumentType.CERTIFICATE_OF_CONFORMITY)
                .setRegistrationNumber("number1")
                .setStartDate(LocalDate.ofEpochDay(17500))
                .setEndDate(LocalDate.ofEpochDay(17865))
                .setImages(MdmImageSetUpdate.builder().build())
                .build();
    }
}
