package ru.yandex.market.partner.mvc.controller.supplier.certification;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.mdm.http.MdmDocument;
import ru.yandex.market.mdm.http.SupplierDocumentService;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Функциональные тесты на на {@link CertificationDocumentController}.
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "CertificationDocumentControllerFunctionalTest.csv")
class CertificationDocumentControllerFunctionalTest extends FunctionalTest {

    private static final long CAMPAIGN_ID = 10774L;
    private static final long SUPPLIER_ID = 774L;

    private static final long UNITED_CATALOG_CAMPAIGN_ID = 10775L;
    private static final long UNITED_CATALOG_BUSINESS_ID = 1775L;

    @Autowired
    SupplierDocumentService marketProtoSupplierDocumentService;

    @Autowired
    FeedFileStorage feedFileStorage;

    @Autowired
    private MboMappingsService patientMboMappingsService;

    @Nonnull
    static HttpEntity<?> json(String requestText) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(requestText, headers);
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

    private static Stream<Arguments> unitedCatalogDependentTestsMethodSource() {
        return Stream.of(
                Arguments.of("Партнер с подключенным Единым каталогом", CAMPAIGN_ID, SUPPLIER_ID),
                Arguments.of("Партнер без Единого каталога", UNITED_CATALOG_CAMPAIGN_ID, UNITED_CATALOG_BUSINESS_ID)
        );
    }

    @ParameterizedTest(name = "Тест успешного поиска сертификатов: {0}")
    @MethodSource("unitedCatalogDependentTestsMethodSource")
    void testFindCertificationDocuments(String caseName, long campaignId, long expectedPartnerId) {
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

        String url = String.format("%s/search?q={query}", certificationDocumentUrl(campaignId));
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
                ArgumentMatchers.argThat(request -> request.getSupplierId() == expectedPartnerId
                        && request.getSearchQuery().equals("number1")
                        && request.getLimit() == 20)
        );
    }

    @DisplayName("Тест успешного пейджированного поиска сертификатов.")
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

        String url = String.format("%s/search?q={query}&page_token={pageToken}&limit={limit}", certificationDocumentUrl(CAMPAIGN_ID));
        FunctionalTestHelper.get(url, "number1", "MW", 10);

        Mockito.verify(marketProtoSupplierDocumentService).findSupplierDocuments(
                ArgumentMatchers.argThat(request -> request.getSupplierId() == SUPPLIER_ID
                        && request.getSearchQuery().equals("number1")
                        && request.getOffsetKey().equals("MW")
                        && request.getLimit() == 10)
        );
    }

    @DisplayName("Тест возникновения ошибки при поиске сертификатов со статусом ошибка.")
    @Test
    void testFindCertificationDocumentsWithErrorStatus() throws IOException {
        Mockito.when(marketProtoSupplierDocumentService.findSupplierDocuments(Mockito.any()))
                .thenReturn(MdmDocument.FindDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.FindDocumentsResponse.Status.ERROR)
                        .build());

        String url = String.format("%s/search", certificationDocumentUrl(CAMPAIGN_ID));
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(new HttpGet(url));
        Assertions.assertEquals(org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE, response.getStatusLine().getStatusCode());
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

        String url = String.format("%s?registrationNumber={registrationNumber}", certificationDocumentUrl(CAMPAIGN_ID));
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

        String url = String.format("%s?registrationNumber={registrationNumber}", certificationDocumentUrl(CAMPAIGN_ID));
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
        String url = String.format("%s?registrationNumber={registrationNumber}", certificationDocumentUrl(CAMPAIGN_ID));
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

    @ParameterizedTest(name = "Тест успешного обновления списка сертификатов: {0}")
    @MethodSource("unitedCatalogDependentTestsMethodSource")
    void testUpdateSuccess(String caseName, long campaignId, long expectedPartnerId) {
        Mockito.when(marketProtoSupplierDocumentService.addSupplierDocuments(Mockito.any()))
                .thenReturn(MdmDocument.AddDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.AddDocumentsResponse.Status.OK)
                        .addDocumentResponse(MdmDocument.AddDocumentsResponse.DocumentResponse.newBuilder()
                                .setStatus(MdmDocument.AddDocumentsResponse.Status.OK)
                                .setDocument(createTestMdmDocument())
                                .build())
                        .build());

        String url = String.format("%s/update", certificationDocumentUrl(campaignId));
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
                            if (r.getSupplierId() != expectedPartnerId) {
                                return false;
                            }
                            MdmDocument.AddSupplierDocumentsRequest.DocumentAddition documentAddition = r.getDocument(0);

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
    void testUpdateWithSerialNumberAndRequirementsAndCustomsCodesSuccess() {
        Mockito.when(marketProtoSupplierDocumentService.addSupplierDocuments(Mockito.any()))
                .thenReturn(MdmDocument.AddDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.AddDocumentsResponse.Status.OK)
                        .addDocumentResponse(MdmDocument.AddDocumentsResponse.DocumentResponse.newBuilder()
                                .setStatus(MdmDocument.AddDocumentsResponse.Status.OK)
                                .setDocument(createTestMdmDocument1())
                                .build())
                        .build());

        String url = String.format("%s/update", certificationDocumentUrl(CAMPAIGN_ID));

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
                            if (r.getSupplierId() != SUPPLIER_ID) {
                                return false;
                            }
                            MdmDocument.AddSupplierDocumentsRequest.DocumentAddition documentAddition = r.getDocument(0);

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

        String url = String.format("%s/update", certificationDocumentUrl(CAMPAIGN_ID));
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
                            if (r.getSupplierId() != SUPPLIER_ID) {
                                return false;
                            }
                            MdmDocument.AddSupplierDocumentsRequest.DocumentAddition documentAddition = r.getDocument(0);

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
        String url = String.format("%s/update", certificationDocumentUrl(CAMPAIGN_ID));
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
    void testUpdateError() {
        Mockito.when(marketProtoSupplierDocumentService.addSupplierDocuments(Mockito.any()))
                .thenReturn(MdmDocument.AddDocumentsResponse.newBuilder()
                        .setStatus(MdmDocument.AddDocumentsResponse.Status.ERROR)
                        .setError(MbocCommon.Message.newBuilder()
                                .setMessageCode("resp error code 1")
                                .setMustacheTemplate("resp error template 1")
                                .build())
                        .build());

        String url = String.format("%s/update", certificationDocumentUrl(CAMPAIGN_ID));
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
        String url = String.format("%s/update", certificationDocumentUrl(CAMPAIGN_ID));
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

        String url = String.format("%s/update", certificationDocumentUrl(CAMPAIGN_ID));
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

    @DisplayName("Тест загрузки приложенного файла для сертификата.")
    @Test
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
                FunctionalTestHelper.post(String.format("%s/attachment", certificationDocumentUrl(CAMPAIGN_ID)), request);
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

    @DisplayName("Тест проверяет корректную пейджированную выдачу оферов по регистрационному номеру сертификата.")
    @Test
    void testGetCertificationDocumentOffers() {
        Mockito.when(marketProtoSupplierDocumentService.findSupplierDocumentRelations(Mockito.any()))
                .thenReturn(MdmDocument.FindSupplierDocumentRelationsResponse.newBuilder()
                        .setStatus(MdmDocument.FindSupplierDocumentRelationsResponse.Status.OK)
                        .setNextOffsetKey("MQ")
                        .setOfferRelations(MdmDocument.FindSupplierDocumentRelationsResponse.SupplierOfferRelations.newBuilder()
                                .setRegistrationNumber("number1")
                                .setSupplierId((int) SUPPLIER_ID)
                                .addShopSku("H123"))
                        .build()
                );

        Mockito.when(patientMboMappingsService.searchMappingsByKeys(Mockito.any()))
                .thenReturn(
                        MboMappings.SearchMappingsResponse.newBuilder()
                                .addOffers(
                                        SupplierOffer.Offer.newBuilder()
                                                .setTitle("Test H123")
                                                .setSupplierId(SUPPLIER_ID)
                                                .setShopSkuId("H123")
                                                .setApprovedMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1288)
                                                                .setSkuName("MarketSku1288")
                                                                .setCategoryId(123)
                                                                .build())
                                                .setSupplierMapping(
                                                        SupplierOffer.Mapping.newBuilder()
                                                                .setSkuId(1214)
                                                                .setSkuName("MarketSku1214")
                                                                .setCategoryId(123)
                                                                .build())
                                                .setSupplierMappingStatus(
                                                        SupplierOffer.Offer.MappingProcessingStatus.newBuilder()
                                                                .setStatus(SupplierOffer.Offer.MappingProcessingStatus.ChangeStatus.MODERATION)
                                                                .build())
                                                .addUrls("https://beru.ru/product/100324822646")
                                                .build())
                                .build());


        String url = String.format("%s/offers?registrationNumber={registrationNumber}&page_token={pageToken}&limit={limit}",
                certificationDocumentUrl(CAMPAIGN_ID));
        ResponseEntity<String> response = FunctionalTestHelper.get(url, "H123", "MW", 10);
        MatcherAssert.assertThat(
                response,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches(
                                "result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ "{\n" +
                                        "  \"nextPageToken\": \"MQ\",\n" +
                                        "  \"shopSkus\": [\n" +
                                        "    {\n" +
                                        "      \"shopSku\": \"H123\",\n" +
                                        "      \"title\": \"Test H123\",\n" +
                                        "      \"barcodes\": [],\n" +
                                        "      \"description\":\"\",\n" +
                                        "      \"vendorCode\":\"\",\n" +
                                        "      \"urls\": [\n" +
                                        "        \"https://beru.ru/product/100324822646\"\n" +
                                        "      ],\n" +
                                        "      \"offerProcessingComments\": [],\n" +
                                        "      \"availability\" : \"ACTIVE\", \n" +
                                        "      \"acceptGoodContent\" : false,\n" +
                                        "      \"mappings\": {\n" +
                                        "        \"rejected\": [],\n" +
                                        "        \"active\": {\n" +
                                        "          \"marketSku\": 1288,\n" +
                                        "          \"contentType\": \"market\",\n" +
                                        "          \"categoryName\": \"\"\n" +
                                        "        },\n" +
                                        "        \"awaitingModeration\": {\n" +
                                        "          \"marketSku\": 1214,\n" +
                                        "          \"contentType\": \"market\",\n" +
                                        "          \"categoryName\": \"\"\n" +
                                        "        }\n" +
                                        "      },\n" +
                                        "      \"offerProcessingStatus\": \"IN_WORK\"\n" +
                                        "    }\n" +
                                        "  ]\n" +
                                        "}"))));


        Mockito.verify(marketProtoSupplierDocumentService).findSupplierDocumentRelations(
                ArgumentMatchers.argThat(request -> request.getSupplierId() == SUPPLIER_ID
                        && request.getRegistrationNumber().equals("H123")
                        && request.getOffsetKey().equals("MW")
                        && request.getLimit() == 10)
        );
    }

    @DisplayName("Тест проверяет возникновение ошибки поиска оферов при получении от mboc статуса ошибка.")
    @Test
    void testGetCertificationDocumentOffersWithErrorStatus() throws IOException {
        Mockito.when(marketProtoSupplierDocumentService.findSupplierDocumentRelations(Mockito.any()))
                .thenReturn(MdmDocument.FindSupplierDocumentRelationsResponse.newBuilder()
                        .setStatus(MdmDocument.FindSupplierDocumentRelationsResponse.Status.ERROR)
                        .build()
                );


        String url = String.format("%s/offers?registrationNumber=H123", certificationDocumentUrl(CAMPAIGN_ID));
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(new HttpGet(url));
        Assertions.assertEquals(org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE, response.getStatusLine().getStatusCode());
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

    @DisplayName("Тест на успешное добавление офера к сертификату поставщика.")
    @Test
    void testAddSupplierDocumentRelation() {
        Mockito.when(marketProtoSupplierDocumentService.addDocumentRelations(Mockito.any()))
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
                        .addDocumentRelations(
                                MdmDocument.AddDocumentRelationsResponse.DocumentRelationAddition.newBuilder()
                                        .setStatus(MdmDocument.AddDocumentRelationsResponse.Status.OK)
                                        .setOfferRelation(MdmDocument.DocumentOfferRelation.newBuilder()
                                                .setSupplierId((int) SUPPLIER_ID)
                                                .setRegistrationNumber("number1")
                                                .setShopSku("sku2")
                                                .build())
                                        .build())
                        .build());

        String url = String.format(
                "%s/offers?registrationNumber={registrationNumber}&shopSku={shopSku}", certificationDocumentUrl(CAMPAIGN_ID));
        FunctionalTestHelper.post(url, null, "H123", "sku1");
    }

    @DisplayName("Тест на добавление документов со статусами ошибки.")
    @Test
    void testAddSupplierDocumentRelationWithErrorStatus() {
        Mockito.when(marketProtoSupplierDocumentService.addDocumentRelations(Mockito.any()))
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

        String url = String.format(
                "%s/offers?registrationNumber={registrationNumber}&shopSku={shopSku}", certificationDocumentUrl(CAMPAIGN_ID));
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(url, null, "H123", "sku1")
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
                                                + "    \"message\":\"Add shop sku failed\","
                                                + "    \"details\":{"
                                                + "        \"message\":\"relation error template 1\","
                                                + "        \"subcode\":\"INVALID\""
                                                + "    }"
                                                + "}")
                                )
                        )));
    }

    @DisplayName("Тест на успешную отвязку офера от сертификата поставщика.")
    @Test
    void testRemoveSupplierDocumentRelation() {
        Mockito.when(marketProtoSupplierDocumentService.removeDocumentOfferRelations(Mockito.any()))
                .thenReturn(MdmDocument.RemoveDocumentOfferRelationsResponse.newBuilder()
                        .setStatus(MdmDocument.RemoveDocumentOfferRelationsResponse.Status.OK)
                        .addRelationResponse(
                                MdmDocument.RemoveDocumentOfferRelationsResponse.RemoveRelationResponse.newBuilder()
                                        .setStatus(MdmDocument.RemoveDocumentOfferRelationsResponse.Status.OK)
                                        .setRelation(MdmDocument.DocumentOfferRelation.newBuilder()
                                                .setSupplierId((int) SUPPLIER_ID)
                                                .setRegistrationNumber("H123")
                                                .setShopSku("sku1")
                                                .build())
                                        .build())
                        .build());

        String url = String.format(
                "%s/offers?registrationNumber={registrationNumber}&shopSku={shopSku}", certificationDocumentUrl(CAMPAIGN_ID));
        FunctionalTestHelper.delete(url, "H123", "sku1");

        Mockito.verify(
                marketProtoSupplierDocumentService).removeDocumentOfferRelations(
                Mockito.argThat(request -> {
                            MdmDocument.DocumentOfferRelation relation = request.getRelation(0);
                            return relation.getSupplierId() == SUPPLIER_ID && relation.getRegistrationNumber().equals("H123")
                                    && relation.getShopSku().equals("sku1");

                        }
                ));
    }

    @DisplayName("Тест на отвязку документов от сертификата со статусами ошибки.")
    @Test
    void testRemoveSupplierDocumentRelationWithErrorStatus() {
        Mockito.when(marketProtoSupplierDocumentService.removeDocumentOfferRelations(Mockito.any()))
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

        String url = String.format(
                "%s/offers?registrationNumber={registrationNumber}&shopSku={shopSku}", certificationDocumentUrl(CAMPAIGN_ID));
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.delete(url, "H123", "sku1")
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
                                                + "    \"message\":\"Remove shop sku failed\","
                                                + "    \"details\":{"
                                                + "        \"message\":\"relation error template 1\","
                                                + "        \"subcode\":\"INVALID\""
                                                + "    }"
                                                + "}")
                                )
                        )));
    }

    private String certificationDocumentUrl(long campaignId) {
        return String.format("%s/suppliers/%d/documents", baseUrl, campaignId);
    }
}
