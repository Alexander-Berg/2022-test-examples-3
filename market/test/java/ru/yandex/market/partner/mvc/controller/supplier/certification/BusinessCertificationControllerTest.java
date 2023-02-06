package ru.yandex.market.partner.mvc.controller.supplier.certification;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feed.mds.FeedFileStorage;
import ru.yandex.market.core.feed.mds.StoreInfo;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.mdm.http.MdmDocument;
import ru.yandex.market.mdm.http.SupplierDocumentService;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

class BusinessCertificationControllerTest extends FunctionalTest {
    private static final long UNITED_CATALOG_BUSINESS_ID = 1775L;

    @Autowired
    SupplierDocumentService marketProtoSupplierDocumentService;

    @Autowired
    FeedFileStorage feedFileStorage;

    @Nonnull
    static HttpEntity<?> json(String requestText) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(requestText, headers);
    }

    @DisplayName("Тест успешного поиска сертификатов бизнеса.")
    @Test
    void testFindCertificationDocuments() {
        Mockito.when(marketProtoSupplierDocumentService.findSupplierDocuments(Mockito.any()))
                .thenReturn(MdmDocument.FindDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.FindDocumentsResponse.Status.OK)
                        .setNextOffsetKey("MQ")
                        .addDocument(MdmDocument.Document.newBuilder()
                                .setRegistrationNumber("number1")
                                .setStartDate(17500)
                                .setEndDate(17865)
                                .setType(MdmDocument.Document.DocumentType.CERTIFICATE_OF_CONFORMITY)
                                .addPicture("http://picture.ru/pict1")
                                .build())
                        .build());

        String url = String.format("%s/search?q={query}", certificationDocumentUrl(UNITED_CATALOG_BUSINESS_ID));
        ResponseEntity<String> response = FunctionalTestHelper.get(url, "number1");
        MatcherAssert.assertThat(
                response,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches(
                                "result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ "{\n" +
                                        "  \"nextPageToken\":\"MQ\"," +
                                        "  \"certificationDocuments\": [\n" +
                                        "    {\n" +
                                        "      \"registrationNumber\": \"number1\",\n" +
                                        "      \"type\": \"CERTIFICATE_OF_CONFORMITY\",\n" +
                                        "      \"customsCommodityCodes\": [],\n" +
                                        "      \"startDate\": \"2017-11-30\",\n" +
                                        "      \"pictureUrls\": [\n" +
                                        "        \"http://picture.ru/pict1\"\n" +
                                        "      ],\n" +
                                        "      \"endDate\": \"2018-11-30\"\n" +
                                        "    }\n" +
                                        "  ]\n" +
                                        "}"))));

        Mockito.verify(marketProtoSupplierDocumentService).findSupplierDocuments(
                ArgumentMatchers.argThat(request -> request.getSupplierId() == UNITED_CATALOG_BUSINESS_ID
                        && request.getSearchQuery().equals("number1")
                        && request.getLimit() == 20)
        );
    }

    @DisplayName("Тест успешного пейджированного поиска сертификатов бизнеса.")
    @Test
    void testFindSupplierDocumentsPaged() {
        Mockito.when(marketProtoSupplierDocumentService.findSupplierDocuments(Mockito.any()))
                .thenReturn(MdmDocument.FindDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.FindDocumentsResponse.Status.OK)
                        .setNextOffsetKey("MQ")
                        .addDocument(MdmDocument.Document.newBuilder()
                                .setRegistrationNumber("number1")
                                .setStartDate(17500)
                                .setEndDate(17865)
                                .setType(MdmDocument.Document.DocumentType.CERTIFICATE_OF_CONFORMITY)
                                .addPicture("http://picture.ru/pict1")
                                .build())
                        .build());

        String url = String.format(
                "%s/search?q={query}&page_token={pageToken}&limit={limit}",
                certificationDocumentUrl(UNITED_CATALOG_BUSINESS_ID)
        );
        FunctionalTestHelper.get(url, "number1", "MW", 10);

        Mockito.verify(marketProtoSupplierDocumentService).findSupplierDocuments(
                ArgumentMatchers.argThat(request -> request.getSupplierId() == UNITED_CATALOG_BUSINESS_ID
                        && request.getSearchQuery().equals("number1")
                        && request.getOffsetKey().equals("MW")
                        && request.getLimit() == 10)
        );
    }

    @Test
    @DisplayName("Тест возникновения ошибки при поиске сертификатов бизнеса со статусом ошибка.")
    void testFindCertificationDocumentsWithErrorStatus() throws IOException {
        Mockito.when(marketProtoSupplierDocumentService.findSupplierDocuments(Mockito.any()))
                .thenReturn(MdmDocument.FindDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.FindDocumentsResponse.Status.ERROR)
                        .build());

        String url = String.format("%s/search", certificationDocumentUrl(UNITED_CATALOG_BUSINESS_ID));
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(new HttpGet(url));
        Assertions.assertEquals(
                org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE,
                response.getStatusLine().getStatusCode()
        );
        Assertions.assertEquals("Service Unavailable", response.getStatusLine().getReasonPhrase());
        JSONAssert.assertEquals("{" +
                        "\"errors\":[{" +
                        "   \"code\":\"SERVICE_UNAVAILABLE\"," +
                        "   \"message\":\"Certification document mboc call error.\", " +
                        "   \"details\":{\"scope\":\"CERTIFICATION_DOCUMENT_MBOC_CALL\",\"emergency\":\"ONE_HOUR\"}" +
                        "}]," +
                        "\"result\":[]" +
                        "}",
                IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset()), JSONCompareMode.LENIENT);
    }

    private String certificationDocumentUrl(long businessId) {
        return String.format("%s/businesses/%d/documents", baseUrl, businessId);
    }

    @DisplayName("Тест успешного запроса сертификата по регистрационному номеру.")
    @Test
    void testGetCertificationDocument() {
        Mockito.when(marketProtoSupplierDocumentService.findSupplierDocumentByRegistrationNumber(Mockito.any()))
                .thenReturn(MdmDocument.FindDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.FindDocumentsResponse.Status.OK)
                        .addDocument(MdmDocument.Document.newBuilder()
                                .setRegistrationNumber("number1")
                                .setStartDate(17500)
                                .setEndDate(17865)
                                .setType(MdmDocument.Document.DocumentType.CERTIFICATE_OF_CONFORMITY)
                                .addPicture("http://picture.ru/pict1")
                                .build())
                        .build());

        String url = String.format("%s?registrationNumber={registrationNumber}",
                certificationDocumentUrl(UNITED_CATALOG_BUSINESS_ID));
        ResponseEntity<String> response = FunctionalTestHelper.get(url, "number1");
        MatcherAssert.assertThat(
                response,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches(
                                "result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ "" +
                                        "    {\n" +
                                        "      \"registrationNumber\": \"number1\",\n" +
                                        "      \"type\": \"CERTIFICATE_OF_CONFORMITY\",\n" +
                                        "      \"startDate\": \"2017-11-30\",\n" +
                                        "      \"customsCommodityCodes\": [],\n" +
                                        "      \"pictureUrls\": [\n" +
                                        "        \"http://picture.ru/pict1\"\n" +
                                        "      ],\n" +
                                        "      \"endDate\": \"2018-11-30\"\n" +
                                        "    }"
                                ))));
        Mockito.verify(marketProtoSupplierDocumentService).findSupplierDocumentByRegistrationNumber(
                ArgumentMatchers.argThat(request -> request.getRegistrationNumber().equals("number1")));
    }

    @DisplayName("Тест успешного запроса и получения сертификата,"
            + " содержащего серию документа, коды ТНВЭД и требования, по регистрационному номеру.")
    @Test
    void testGetCertificationDocumentWithSerialNumberAndCustomsCodesAndRequirements() {
        Mockito.when(marketProtoSupplierDocumentService.findSupplierDocumentByRegistrationNumber(Mockito.any()))
                .thenReturn(MdmDocument.FindDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.FindDocumentsResponse.Status.OK)
                        .addDocument(MdmDocument.Document.newBuilder()
                                .setRegistrationNumber("number1")
                                .setStartDate(17500)
                                .setEndDate(17865)
                                .setType(MdmDocument.Document.DocumentType.CERTIFICATE_OF_CONFORMITY)
                                .addPicture("http://picture.ru/pict1")
                                .addCustomsCommodityCodes("TNW2352")
                                .addCustomsCommodityCodes("TNZ3490")
                                .setSerialNumber("X5Z")
                                .setRequirements("Fire, Water, Air, Earth, Fifth Element")
                                .build())
                        .build());

        String url = String.format("%s?registrationNumber={registrationNumber}",
                certificationDocumentUrl(UNITED_CATALOG_BUSINESS_ID));
        ResponseEntity<String> response = FunctionalTestHelper.get(url, "number1");
        MatcherAssert.assertThat(
                response,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches(
                                "result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ "" +
                                        "    {\n" +
                                        "      \"registrationNumber\": \"number1\",\n" +
                                        "      \"type\": \"CERTIFICATE_OF_CONFORMITY\",\n" +
                                        "      \"startDate\": \"2017-11-30\",\n" +
                                        "      \"customsCommodityCodes\": [\"TNW2352\", \"TNZ3490\"],\n" +
                                        "      \"serialNumber\": \"X5Z\",\n" +
                                        "      \"requirements\": \"Fire, Water, Air, Earth, Fifth Element\",\n" +
                                        "      \"pictureUrls\": [\n" +
                                        "        \"http://picture.ru/pict1\"\n" +
                                        "      ],\n" +
                                        "      \"endDate\": \"2018-11-30\"\n" +
                                        "    }"
                                ))));
        Mockito.verify(marketProtoSupplierDocumentService).findSupplierDocumentByRegistrationNumber(
                ArgumentMatchers.argThat(request -> request.getRegistrationNumber().equals("number1")));
    }

    @DisplayName("Тест возникновения ошибки документ не найден.")
    @Test
    void testGetCertificationDocumentWithErrorStatus() {
        Mockito.when(marketProtoSupplierDocumentService.findSupplierDocumentByRegistrationNumber(Mockito.any()))
                .thenReturn(MdmDocument.FindDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.FindDocumentsResponse.Status.OK)
                        .addDocument(MdmDocument.Document.newBuilder()
                                .setRegistrationNumber("number1")
                                .setStartDate(17500)
                                .setEndDate(17865)
                                .setType(MdmDocument.Document.DocumentType.CERTIFICATE_OF_CONFORMITY)
                                .addPicture("http://picture.ru/pict1")
                                .build())
                        .build());
        String url = String.format("%s?registrationNumber={registrationNumber}",
                certificationDocumentUrl(UNITED_CATALOG_BUSINESS_ID));
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(url, "number2")
        );
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches(
                                        "errors",
                                        MbiMatchers.jsonArrayEquals(/*language=JSON*/ ""
                                                + "{"
                                                + "    \"code\":\"BAD_PARAM\","
                                                + "    \"details\":{"
                                                + "        \"entity_name\":\"certification_document\","
                                                + "        \"entity_id\":\"number2\","
                                                + "        \"subcode\":\"ENTITY_NOT_FOUND\""
                                                + "    }"
                                                + "}")
                                )
                        )));
    }

    @DisplayName("Тест успешного обновления списка сертификатов: Партнер с подключенным Единым каталогом.")
    @Test
    @DbUnitDataSet(before = "BusinessCertificationControllerTest.update.before.csv")
    void testUpdateSuccess() {
        Mockito.when(marketProtoSupplierDocumentService.addSupplierDocuments(Mockito.any()))
                .thenReturn(MdmDocument.AddDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.AddDocumentsResponse.Status.OK)
                        .addDocumentResponse(MdmDocument.AddDocumentsResponse.DocumentResponse.newBuilder()
                                .setStatus(MdmDocument.AddDocumentsResponse.Status.OK)
                                .setDocument(createTestMdmDocument())
                                .build())
                        .build());

        String url = String.format("%s/update", certificationDocumentUrl(UNITED_CATALOG_BUSINESS_ID));
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{\n" +
                "  \"certificationDocuments\": [\n" +
                "    {\n" +
                "      \"registrationNumber\": \"number1\",\n" +
                "      \"type\": \"CERTIFICATE_OF_CONFORMITY\",\n" +
                "      \"startDate\": \"2017-11-30\",\n" +
                "      \"endDate\": \"2018-11-30\",\n" +
                "      \"newScanFileIds\": [101,201],\n" +
                "      \"deletePictureMdmUrls\": [\"http://avatar/picture3.url\",\"http://avatar/picture4.url\"]\n" +
                "    }\n" +
                "  ]\n" +
                "}");

        ResponseEntity<String> processedEntity = FunctionalTestHelper.post(url, request);
        MatcherAssert.assertThat(processedEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches("result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "{\n" +
                                        "    \"status\": \"OK\",\n" +
                                        "    \"addCertificationDocumentResponses\": [\n" +
                                        "      {\n" +
                                        "        \"status\": \"OK\",\n" +
                                        "        \"errors\": [],\n" +
                                        "        \"certificationDocument\": {\n" +
                                        "          \"registrationNumber\": \"number1\",\n" +
                                        "          \"type\": \"CERTIFICATE_OF_CONFORMITY\",\n" +
                                        "          \"startDate\": \"2017-11-30\",\n" +
                                        "          \"endDate\": \"2018-11-30\",\n" +
                                        "          \"customsCommodityCodes\": [],\n" +
                                        "          \"pictureUrls\": [\n" +
                                        "            \"http://avatar/picture1.url\",\n" +
                                        "            \"http://avatar/picture2.url\"\n" +
                                        "          ]\n" +
                                        "        }\n" +
                                        "      }\n" +
                                        "    ]\n" +
                                        "  }"))));

        Mockito.verify(
                marketProtoSupplierDocumentService).addSupplierDocuments(
                Mockito.argThat(r -> {
                            if (r.getSupplierId() != UNITED_CATALOG_BUSINESS_ID) {
                                return false;
                            }
                            MdmDocument.AddSupplierDocumentsRequest.DocumentAddition documentAddition =
                                    r.getDocument(0);

                            if (!documentAddition.getNewScanFile(0).equals(
                                    MdmDocument.AddSupplierDocumentsRequest.DocumentAddition.ScanFile.newBuilder()
                                            .setUrl("http://mds/picture1.url")
                                            .setFileName("picture1.jpeg")
                                            .build()
                            ) || !documentAddition.getNewScanFile(0).equals(
                                    MdmDocument.AddSupplierDocumentsRequest.DocumentAddition.ScanFile.newBuilder()
                                            .setUrl("http://mds/picture1.url")
                                            .setFileName("picture1.jpeg")
                                            .build()
                            )) {
                                return false;
                            }

                            if (!documentAddition.getDeletePictureMdmUrl(0).equals("http://avatar/picture3.url") ||
                                    !documentAddition.getDeletePictureMdmUrl(1).equals("http://avatar/picture4.url")) {
                                return false;
                            }

                            MdmDocument.Document document = documentAddition.getDocument();
                            return document.getRegistrationNumber().equals("number1")
                                    && document.getStartDate() == 17500L && document.getEndDate() == 17865L
                                    && document.getType() == MdmDocument.Document.DocumentType.CERTIFICATE_OF_CONFORMITY;
                        }
                ));
    }

    @DisplayName("Тест успешного обновления списка сертификатов.")
    @Test
    @DbUnitDataSet(before = "BusinessCertificationControllerTest.update.before.csv")
    void testUpdateWithSerialNumberAndRequirementsAndCustomsCodesSuccess() {
        Mockito.when(marketProtoSupplierDocumentService.addSupplierDocuments(Mockito.any()))
                .thenReturn(MdmDocument.AddDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.AddDocumentsResponse.Status.OK)
                        .addDocumentResponse(MdmDocument.AddDocumentsResponse.DocumentResponse.newBuilder()
                                .setStatus(MdmDocument.AddDocumentsResponse.Status.OK)
                                .setDocument(createTestMdmDocument1())
                                .build())
                        .build());

        String url = String.format("%s/update", certificationDocumentUrl(UNITED_CATALOG_BUSINESS_ID));

        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{\n" +
                "  \"certificationDocuments\": [\n" +
                "    {\n" +
                "      \"registrationNumber\": \"number1\",\n" +
                "      \"type\": \"CERTIFICATE_OF_CONFORMITY\",\n" +
                "      \"serialNumber\": \"XH54\",\n" +
                "      \"requirements\": \"Dead rat, bat's wings, pure soul\",\n" +
                "      \"customsCommodityCodes\": [\"GSD1-1412\", \"WSD5-6097\"],\n" +
                "      \"startDate\": \"2017-11-30\",\n" +
                "      \"endDate\": \"2018-11-30\",\n" +
                "      \"newScanFileIds\": [101,201],\n" +
                "      \"deletePictureMdmUrls\": [\"http://avatar/picture3.url\",\"http://avatar/picture4.url\"]\n" +
                "    }\n" +
                "  ]\n" +
                "}");

        ResponseEntity<String> processedEntity = FunctionalTestHelper.post(url, request);
        MatcherAssert.assertThat(processedEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches("result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "{\n" +
                                        "    \"status\": \"OK\",\n" +
                                        "    \"addCertificationDocumentResponses\": [\n" +
                                        "      {\n" +
                                        "        \"status\": \"OK\",\n" +
                                        "        \"errors\": [],\n" +
                                        "        \"certificationDocument\": {\n" +
                                        "          \"registrationNumber\": \"number1\",\n" +
                                        "          \"type\": \"CERTIFICATE_OF_CONFORMITY\",\n" +
                                        "          \"serialNumber\": \"XH54\",\n" +
                                        "          \"requirements\": \"Dead rat, bat's wings, pure soul\",\n" +
                                        "          \"customsCommodityCodes\": [\"GSD1-1412\", \"WSD5-6097\"],\n" +
                                        "          \"startDate\": \"2017-11-30\",\n" +
                                        "          \"endDate\": \"2018-11-30\",\n" +
                                        "          \"pictureUrls\": [\n" +
                                        "            \"http://avatar/picture1.url\",\n" +
                                        "            \"http://avatar/picture2.url\"\n" +
                                        "          ]\n" +
                                        "        }\n" +
                                        "      }\n" +
                                        "    ]\n" +
                                        "  }"))));

        Mockito.verify(
                marketProtoSupplierDocumentService).addSupplierDocuments(
                Mockito.argThat(r -> {
                            if (r.getSupplierId() != UNITED_CATALOG_BUSINESS_ID) {
                                return false;
                            }
                            MdmDocument.AddSupplierDocumentsRequest.DocumentAddition documentAddition =
                                    r.getDocument(0);

                            if (!documentAddition.getNewScanFile(0).equals(
                                    MdmDocument.AddSupplierDocumentsRequest.DocumentAddition.ScanFile.newBuilder()
                                            .setUrl("http://mds/picture1.url")
                                            .setFileName("picture1.jpeg")
                                            .build()
                            ) || !documentAddition.getNewScanFile(1).equals(
                                    MdmDocument.AddSupplierDocumentsRequest.DocumentAddition.ScanFile.newBuilder()
                                            .setUrl("http://mds/picture2.url")
                                            .setFileName("picture2.jpeg")
                                            .build()
                            )) {
                                return false;
                            }

                            if (!documentAddition.getDeletePictureMdmUrl(0).equals("http://avatar/picture3.url") ||
                                    !documentAddition.getDeletePictureMdmUrl(1).equals("http://avatar/picture4.url")) {
                                return false;
                            }

                            MdmDocument.Document document = documentAddition.getDocument();
                            return document.getRegistrationNumber().equals("number1")
                                    && document.getStartDate() == 17500L && document.getEndDate() == 17865L
                                    && document.getType() == MdmDocument.Document.DocumentType.CERTIFICATE_OF_CONFORMITY
                                    && document.getSerialNumber().equals("XH54")
                                    && document.getRequirements().equals("Dead rat, bat's wings, pure soul")
                                    && document.getCustomsCommodityCodesList()
                                    .equals(Arrays.asList("GSD1-1412", "WSD5-6097"));
                        }
                ));
    }

    @DisplayName("Тест успешного обновления списка сертификатов без списка сканированных документов.")
    @Test
    void testUpdateWithoutScanFilesSuccess() {
        Mockito.when(marketProtoSupplierDocumentService.addSupplierDocuments(Mockito.any()))
                .thenReturn(MdmDocument.AddDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.AddDocumentsResponse.Status.OK)
                        .addDocumentResponse(MdmDocument.AddDocumentsResponse.DocumentResponse.newBuilder()
                                .setStatus(MdmDocument.AddDocumentsResponse.Status.OK)
                                .setDocument(createTestMdmDocument())
                                .build())
                        .build());

        String url = String.format("%s/update", certificationDocumentUrl(UNITED_CATALOG_BUSINESS_ID));
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{\n" +
                "  \"certificationDocuments\": [\n" +
                "    {\n" +
                "      \"registrationNumber\": \"number1\",\n" +
                "      \"type\": \"CERTIFICATE_OF_CONFORMITY\",\n" +
                "      \"customsCommodityCodes\": [],\n" +
                "      \"startDate\": \"2017-11-30\",\n" +
                "      \"endDate\": \"2018-11-30\"\n" +
                "    }\n" +
                "  ]\n" +
                "}");

        ResponseEntity<String> processedEntity = FunctionalTestHelper.post(url, request);
        MatcherAssert.assertThat(processedEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches("result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "{\n" +
                                        "    \"status\": \"OK\",\n" +
                                        "    \"addCertificationDocumentResponses\": [\n" +
                                        "      {\n" +
                                        "        \"status\": \"OK\",\n" +
                                        "        \"errors\": [],\n" +
                                        "        \"certificationDocument\": {\n" +
                                        "          \"registrationNumber\": \"number1\",\n" +
                                        "          \"type\": \"CERTIFICATE_OF_CONFORMITY\",\n" +
                                        "          \"customsCommodityCodes\": [],\n" +
                                        "          \"startDate\": \"2017-11-30\",\n" +
                                        "          \"endDate\": \"2018-11-30\",\n" +
                                        "          \"pictureUrls\": [\n" +
                                        "            \"http://avatar/picture1.url\",\n" +
                                        "            \"http://avatar/picture2.url\"\n" +
                                        "          ]\n" +
                                        "        }\n" +
                                        "      }\n" +
                                        "    ]\n" +
                                        "  }"))));

        Mockito.verify(
                marketProtoSupplierDocumentService).addSupplierDocuments(
                Mockito.argThat(r -> {
                            if (r.getSupplierId() != UNITED_CATALOG_BUSINESS_ID) {
                                return false;
                            }
                            MdmDocument.AddSupplierDocumentsRequest.DocumentAddition documentAddition =
                                    r.getDocument(0);

                            if (!documentAddition.getNewScanFileList().isEmpty()) {
                                return false;
                            }

                            if (!documentAddition.getDeletePictureMdmUrlList().isEmpty()) {
                                return false;
                            }

                            MdmDocument.Document document = documentAddition.getDocument();
                            return document.getRegistrationNumber().equals("number1")
                                    && document.getStartDate() == 17500L && document.getEndDate() == 17865L
                                    && document.getType() == MdmDocument.Document.DocumentType.CERTIFICATE_OF_CONFORMITY;
                        }
                ));

    }

    @DisplayName("Тест возникновения ошибки при передаче несуществующего newScanFileId.")
    @Test
    void testInvalidNewScanFileId() {
        String url = String.format("%s/update", certificationDocumentUrl(UNITED_CATALOG_BUSINESS_ID));
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{\n" +
                "  \"certificationDocuments\": [\n" +
                "    {\n" +
                "      \"registrationNumber\": \"number1\",\n" +
                "      \"type\": \"CERTIFICATE_OF_CONFORMITY\",\n" +
                "      \"startDate\": \"2017-11-30\",\n" +
                "      \"endDate\": \"2018-11-30\",\n" +
                "      \"newScanFileIds\": [404]\n" +
                "    }\n" +
                "  ]\n" +
                "}");

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(url, request)
        );
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches(
                                        "errors",
                                        MbiMatchers.jsonArrayEquals(/*language=JSON*/ ""
                                                + "{"
                                                + "    \"code\":\"BAD_PARAM\","
                                                + "    \"details\":{"
                                                + "        \"reason\":\"Can not find upload info by id: 404\","
                                                + "        \"field\":\"UPLOAD_ID\","
                                                + "        \"subcode\":\"INVALID\""
                                                + "    }"
                                                + "}")
                                )
                        )));
    }

    @DisplayName("Тест корректности результата при возврате статуса ошибка списка документов из mboc.")
    @Test
    @DbUnitDataSet(before = "BusinessCertificationControllerTest.update.before.csv")
    void testUpdateError() {
        Mockito.when(marketProtoSupplierDocumentService.addSupplierDocuments(Mockito.any()))
                .thenReturn(MdmDocument.AddDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.AddDocumentsResponse.Status.ERROR)
                        .setError(MbocCommon.Message.newBuilder()
                                .setMessageCode("resp error code 1")
                                .setMustacheTemplate("resp error template 1")
                                .build())
                        .build());

        String url = String.format("%s/update", certificationDocumentUrl(UNITED_CATALOG_BUSINESS_ID));
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{\n" +
                "  \"certificationDocuments\": [\n" +
                "    {\n" +
                "      \"registrationNumber\": \"number1\",\n" +
                "      \"type\": \"CERTIFICATE_OF_CONFORMITY\",\n" +
                "      \"startDate\": \"2017-11-30\",\n" +
                "      \"endDate\": \"2018-11-30\",\n" +
                "      \"newScanFileIds\": [101,201],\n" +
                "      \"deletePictureMdmUrls\": [\"http://avatar/picture3.url\",\"http://avatar/picture4.url\"]\n" +
                "    }\n" +
                "  ]\n" +
                "}");

        ResponseEntity<String> processedEntity = FunctionalTestHelper.post(url, request);
        MatcherAssert.assertThat(processedEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches("result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "{\n" +
                                        "    \"status\": \"ERROR\",\n" +
                                        "    \"error\": {\n" +
                                        "      \"code\": \"mboc.resp error code 1\",\n" +
                                        "      \"template\": \"resp error template 1\",\n" +
                                        "      \"templateArguments\": \"{}\",\n" +
                                        "      \"text\": \"resp error template 1\"\n" +
                                        "    },\n" +
                                        "    \"addCertificationDocumentResponses\": []\n" +
                                        "  }"))));

    }

    @DisplayName("Тест возникновения ошибки при передачи невалидного регистрацонного номера при создании сертификата.")
    @Test
    void testInvalidRegistrationNumberError() {
        String url = String.format("%s/update", certificationDocumentUrl(UNITED_CATALOG_BUSINESS_ID));
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{\n" +
                "  \"certificationDocuments\": [\n" +
                "    {\n" +
                "      \"registrationNumber\": \"shrt\",\n" +
                "      \"type\": \"CERTIFICATE_OF_CONFORMITY\",\n" +
                "      \"startDate\": \"2017-11-30\",\n" +
                "      \"endDate\": \"2018-11-30\",\n" +
                "      \"newScanFileIds\": [101,201],\n" +
                "      \"deletePictureMdmUrls\": [\"http://avatar/picture3.url\",\"http://avatar/picture4.url\"]\n" +
                "    }\n" +
                "  ]\n" +
                "}");

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(url, request)
        );
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches(
                                        "errors",
                                        MbiMatchers.jsonArrayEquals(/*language=JSON*/ ""
                                                + "{"
                                                + "    \"code\":\"BAD_PARAM\","
                                                + "    \"message\":\"size must be between 5 and 2147483647\","
                                                + "    \"details\":{"
                                                + "        \"field\":\"certificationDocuments[0].registrationNumber\","
                                                + "        \"subcode\":\"INVALID\""
                                                + "    }"
                                                + "}")
                                )
                        )
                )
        );
    }

    @DisplayName("Тест корректности результата при возврате статуса ошибка от mboc в одном из докуметов.")
    @Test
    @DbUnitDataSet(before = "BusinessCertificationControllerTest.update.before.csv")
    void testUpdateWithDocumentResponseError() {
        Mockito.when(marketProtoSupplierDocumentService.addSupplierDocuments(Mockito.any()))
                .thenReturn(MdmDocument.AddDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.AddDocumentsResponse.Status.OK)
                        .addDocumentResponse(MdmDocument.AddDocumentsResponse.DocumentResponse.newBuilder()
                                .setStatus(MdmDocument.AddDocumentsResponse.Status.ERROR)
                                .addError(MbocCommon.Message.newBuilder()
                                        .setMessageCode("doc error code 1")
                                        .setMustacheTemplate("doc error template 1")
                                        .build())
                                .build())
                        .build());

        String url = String.format("%s/update", certificationDocumentUrl(UNITED_CATALOG_BUSINESS_ID));
        HttpEntity<?> request = json(/*language=JSON*/ ""
                + "{\n" +
                "  \"certificationDocuments\": [\n" +
                "    {\n" +
                "      \"registrationNumber\": \"number1\",\n" +
                "      \"type\": \"CERTIFICATE_OF_CONFORMITY\",\n" +
                "      \"startDate\": \"2017-11-30\",\n" +
                "      \"endDate\": \"2018-11-30\",\n" +
                "      \"newScanFileIds\": [101,201],\n" +
                "      \"deletePictureMdmUrls\": [\"http://avatar/picture3.url\",\"http://avatar/picture4.url\"]\n" +
                "    }\n" +
                "  ]\n" +
                "}");

        ResponseEntity<String> processedEntity = FunctionalTestHelper.post(url, request);
        MatcherAssert.assertThat(processedEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches("result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "{\n" +
                                        "    \"status\": \"OK\",\n" +
                                        "    \"addCertificationDocumentResponses\": [\n" +
                                        "      {\n" +
                                        "        \"status\": \"ERROR\",\n" +
                                        "        \"errors\": [\n" +
                                        "          {\n" +
                                        "            \"code\": \"mboc.doc error code 1\",\n" +
                                        "            \"template\": \"doc error template 1\",\n" +
                                        "            \"templateArguments\": \"{}\",\n" +
                                        "            \"text\": \"doc error template 1\"\n" +
                                        "          }\n" +
                                        "        ],\n" +
                                        "        \"certificationDocument\": {\n" +
                                        "          \"registrationNumber\": \"\",\n" +
                                        "          \"type\": \"DECLARATION_OF_CONFORMITY\",\n" +
                                        "          \"customsCommodityCodes\": [],\n" +
                                        "          \"startDate\": \"1970-01-01\",\n" +
                                        "          \"pictureUrls\": []\n" +
                                        "        }\n" +
                                        "      }\n" +
                                        "    ]\n" +
                                        "  }"))));
    }

    private static MdmDocument.Document createTestMdmDocument() {
        return MdmDocument.Document.newBuilder()
                .setRegistrationNumber("number1")
                .setStartDate(17500)
                .setEndDate(17865)
                .setType(MdmDocument.Document.DocumentType.CERTIFICATE_OF_CONFORMITY)
                .addPicture("http://avatar/picture1.url")
                .addPicture("http://avatar/picture2.url")
                .build();
    }

    private static MdmDocument.Document createTestMdmDocument1() {
        return MdmDocument.Document.newBuilder()
                .setRegistrationNumber("number1")
                .setStartDate(17500)
                .setEndDate(17865)
                .setType(MdmDocument.Document.DocumentType.CERTIFICATE_OF_CONFORMITY)
                .addPicture("http://avatar/picture1.url")
                .addPicture("http://avatar/picture2.url")
                .setSerialNumber("XH54")
                .setRequirements("Dead rat, bat's wings, pure soul")
                .addCustomsCommodityCodes("GSD1-1412")
                .addCustomsCommodityCodes("WSD5-6097")
                .build();
    }

    @DisplayName("Тест загрузки приложенного файла для сертификата.")
    @Test
    @DbUnitDataSet(before = "BusinessCertificationControllerTest.update.before.csv")
    void testUploadAttachment() throws IOException {
        ClassPathResource uploadedResource = new ClassPathResource("supplier/certificate/empty.jpg");
        Mockito.when(feedFileStorage.open(Mockito.any())).thenAnswer(invocation -> uploadedResource.getInputStream());
        Mockito.when(feedFileStorage.upload(Mockito.any(), Mockito.anyLong()))
                .thenReturn(new StoreInfo(123, "http://mds/picture1.url"));

        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("upload", uploadedResource);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<?> request = new HttpEntity<>(multipart, headers);
        ResponseEntity<String> processedEntity =
                FunctionalTestHelper.post(String.format("%s/attachment", certificationDocumentUrl(UNITED_CATALOG_BUSINESS_ID)), request);
        MatcherAssert.assertThat(processedEntity,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches("result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ ""
                                        + "{\n" +
                                        "    \"id\": 1,\n" +
                                        "    \"url\": \"http://mds/picture1.url\",\n" +
                                        "    \"size\": 165\n" +
                                        "  }"))));
    }
}
