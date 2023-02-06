package ru.yandex.cms.client.http;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cms.client.api.CmsClient;
import ru.yandex.cms.client.exception.BadRequestException;
import ru.yandex.cms.client.exception.DocumentNotFound;
import ru.yandex.cms.client.model.CmsImage;
import ru.yandex.cms.client.model.CmsImageData;
import ru.yandex.cms.client.model.CmsReferenceType;
import ru.yandex.cms.client.model.CmsStandInfo;
import ru.yandex.cms.client.model.CmsStandsData;
import ru.yandex.cms.client.model.CmsTemplate;
import ru.yandex.cms.client.model.CmsTemplateData;
import ru.yandex.cms.client.model.CmsTemplateEntity;
import ru.yandex.cms.client.model.CmsTemplateEntry;
import ru.yandex.cms.client.model.CmsTemplateEntrySys;
import ru.yandex.cms.client.model.CmsTemplateIncludes;
import ru.yandex.cms.client.model.CmsTemplateInfo;
import ru.yandex.cms.client.model.CmsTemplateLink;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@MockServerSettings(ports = 12233)
class CmsClientTest extends AbstractCmsMockServerTest {

    private static final long DOCUMENT_ID = 1L;
    private static final long REVISION_ID = 143L;

    @Autowired
    private CmsClient cmsClient;

    CmsClientTest(MockServerClient server) {
        super(server);
    }

    @NotNull
    private static Stream<Arguments> getError() {
        return Stream.of(
                Arguments.of(400, BadRequestException.class, "CMS response Bad Request error: Error"),
                Arguments.of(404, DocumentNotFound.class, "Document 1 not found"),
                Arguments.of(418, BadRequestException.class, "CMS response Client Error (418) error: Error"),
                Arguments.of(500, IllegalStateException.class, "CMS response Internal Server Error error: Error")
        );
    }

    @Test
    @DisplayName("Успешное получение информации по документам партнера")
    void getDocuments_exist_success() {
        initMock("GET", "/jsonPageApi/v1/getResourceList", null, "getDocuments_exist_success",
                Map.of(
                        "userId", List.of("1513471018"),
                        "limit", List.of("10"),
                        "type", List.of("business"),
                        "sort", List.of("updated:desc"),
                        "search", List.of("business_id:443")
                )
        );

        Assertions.assertThat(cmsClient.getDocuments(443L, "business"))
                .containsExactlyInAnyOrder(
                        createCmsTemplateInfo(159255, 321L, "SIS базовый шаблон для бизнеса 443"),
                        createCmsTemplateInfo(159254, 0L, "SIS базовый шаблон")
                );
    }

    @Test
    @DisplayName("Вернулась пустая информация по документам партнера")
    void getDocuments_empty_success() {
        initMock("GET", "/jsonPageApi/v1/getResourceList", null, "getDocuments_empty_success",
                Map.of(
                        "userId", List.of("1513471018"),
                        "limit", List.of("10"),
                        "type", List.of("express"),
                        "sort", List.of("updated:desc"),
                        "search", List.of("business_id:554")
                )
        );

        Assertions.assertThat(cmsClient.getDocuments(554L, "express"))
                .isEmpty();
    }

    @Test
    @DisplayName("Успешное создание документа")
    void createDocument_empty_success() {
        initMock("POST", "/v1/documents", "document", "document",
                Map.of("userId", List.of("1513471018")));

        CmsTemplate requestTemplate = template();
        CmsTemplate template = cmsClient.createDocument(requestTemplate);

        assertTemplate(template, requestTemplate);
    }

    @Test
    @DisplayName("Создание документа возвращает неверный ответ и завершается исключением")
    void createDocument_errorResponse_exception() {
        server
                .when(request()
                        .withMethod("POST")
                        .withPath("/v1/documents")
                )
                .respond(response()
                        .withStatusCode(500)
                        .withBody("error")
                );

        CmsTemplate requestTemplate = template();
        Assertions.assertThatThrownBy(() -> cmsClient.createDocument(requestTemplate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CMS response Internal Server Error error: error");

    }

    @Test
    @DisplayName("Успешное получение документа")
    void getDocument_success() {
        initMock("GET", "/v1/documents/" + DOCUMENT_ID, null, "document",
                Map.of("userId", List.of("1513471018")));

        CmsTemplate responseTemplate = template();
        CmsTemplate template = cmsClient.getDocument(DOCUMENT_ID, null);

        assertTemplate(template, responseTemplate);
    }

    @Test
    @DisplayName("Успешное получение документа с номером ревизии")
    void getDocumentByRevision_success() {
        initMock("GET", "/v1/documents/" + DOCUMENT_ID + "/revisions/" + REVISION_ID, null, "document",
                Map.of("userId", List.of("1513471018")));

        CmsTemplate responseTemplate = template();
        CmsTemplate template = cmsClient.getDocument(DOCUMENT_ID, REVISION_ID);

        assertTemplate(template, responseTemplate);
    }

    @Test
    @DisplayName("Получение документа возвращает неверный ответ и завершается исключением")
    void getDocument_errorResponse_exception() {
        server
                .when(request()
                        .withMethod("GET")
                        .withPath("/v1/documents/" + DOCUMENT_ID)
                )
                .respond(response()
                        .withStatusCode(500)
                        .withBody("error")
                );

        Assertions.assertThatThrownBy(() -> cmsClient.getDocument(DOCUMENT_ID, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CMS response Internal Server Error error: error");
    }

    @Test
    @DisplayName("Получение документа с номером ревизии возвращает неверный ответ и завершается исключением")
    void getDocumentByRevision_errorResponse_exception() {
        server
                .when(request()
                        .withMethod("GET")
                        .withPath("/v1/documents/" + DOCUMENT_ID + "/revisions/" + REVISION_ID)
                )
                .respond(response()
                        .withStatusCode(500)
                        .withBody("error")
                );

        Assertions.assertThatThrownBy(() -> cmsClient.getDocument(DOCUMENT_ID, REVISION_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CMS response Internal Server Error error: error");
    }

    @Test
    @DisplayName("Успешное сохранение документа")
    void saveDocument_success() {
        initMock("PUT", "/v1/documents/" + DOCUMENT_ID, "document", "document",
                Map.of("userId", List.of("1513471018")));

        CmsTemplate requestTemplate = template();
        CmsTemplate template = cmsClient.saveDocument(DOCUMENT_ID, requestTemplate);

        assertTemplate(template, requestTemplate);
    }

    @Test
    @DisplayName("Успешная публикация документа")
    void publishDocument_success() {
        server.when(request()
                        .withMethod("PUT")
                        .withPath("/v1/documents/" + DOCUMENT_ID + "/published")
                        .withQueryStringParameter("revision_id", Long.toString(REVISION_ID)))
                .respond(response()
                        .withBody(loadFile("json/document.json"), StandardCharsets.UTF_8));

        CmsTemplate template = cmsClient.publishDocument(DOCUMENT_ID, REVISION_ID);

        assertTemplate(template, template());
    }

    @DisplayName("Публикация документа не удалась, так как вернулся ответ c кодом ошибки")
    @MethodSource("getError")
    @ParameterizedTest(name = "{0}")
    void publishDocument_incorrectResponse_exception(int code, Class<?> clazz, String message) {
        server.when(request()
                        .withMethod("PUT")
                        .withPath("/v1/documents/" + DOCUMENT_ID + "/published")
                        .withQueryStringParameter("revision_id", Long.toString(REVISION_ID)))
                .respond(response()
                        .withStatusCode(code)
                        .withBody("Error"));

        Assertions.assertThatThrownBy(() -> cmsClient.publishDocument(DOCUMENT_ID, REVISION_ID))
                .isInstanceOf(clazz)
                .hasMessage(message);
    }

    @Test
    @DisplayName("Сохранение документа возвращает неверный ответ и завершается исключением")
    void saveDocument_errorResponse_exception() {
        server
                .when(request()
                        .withMethod("PUT")
                        .withPath("/v1/documents/" + DOCUMENT_ID)
                )
                .respond(response()
                        .withStatusCode(500)
                        .withBody("error")
                );

        CmsTemplate requestTemplate = template();
        Assertions.assertThatThrownBy(() -> cmsClient.saveDocument(DOCUMENT_ID, requestTemplate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CMS response Internal Server Error error: error");

    }

    @Test
    @DisplayName("Обновление стенда прошло успешно.")
    void updateStand_correctData_success() {
        server
                .when(request()
                        .withMethod("PUT")
                        .withPath("/v1/documents/" + DOCUMENT_ID + "/revisions/" + REVISION_ID + "/stands/draft")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withBody(loadFile("json/stand_data.json"))
                );

        Assertions.assertThat(cmsClient.updateStand(DOCUMENT_ID, REVISION_ID))
                .isEqualTo(standsData(false));
    }

    @Test
    @DisplayName("Обновление стенда прошло успешно, но не все стенды обновились.")
    void updateStand_correctData_oneStandNotUpdate() {
        server
                .when(request()
                        .withMethod("PUT")
                        .withPath("/v1/documents/" + DOCUMENT_ID + "/revisions/" + REVISION_ID + "/stands/draft")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withBody(loadFile("json/stand_data_error.json"))
                );

        Assertions.assertThat(cmsClient.updateStand(DOCUMENT_ID, REVISION_ID))
                .isEqualTo(standsData(true));
    }

    @Test
    @DisplayName("Обновление стенда завершилось ошибкой.")
    void updateStand_correctData_timeoutError() {
        server
                .when(request()
                        .withMethod("PUT")
                        .withPath("/v1/documents/" + DOCUMENT_ID + "/revisions/" + REVISION_ID + "/stands/draft")
                )
                .respond(response()
                        .withStatusCode(500)
                        .withBody("error")
                );

        Assertions.assertThatThrownBy(() -> cmsClient.updateStand(DOCUMENT_ID, REVISION_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CMS response Internal Server Error error: error");
    }

    @Test
    @DisplayName("Успешное удаление документа")
    void deleteDocument_success() {
        server
                .when(request()
                        .withMethod("DELETE")
                        .withPath("/jsonPageApi/v1/deleteResourceById")
                        .withQueryStringParameter("id", Long.toString(DOCUMENT_ID))
                )
                .respond(response()
                        .withStatusCode(200)
                );

        Assertions.assertThatNoException().isThrownBy(() -> cmsClient.deleteDocument(DOCUMENT_ID));
    }

    @Test
    @DisplayName("Удаление документа возвращает неверный ответ и завершается исключением")
    void deleteDocument_errorResponse_exception() {
        server
                .when(request()
                        .withMethod("DELETE")
                        .withPath("/jsonPageApi/v1/deleteResourceById")
                        .withQueryStringParameter("id", Long.toString(DOCUMENT_ID))
                )
                .respond(response()
                        .withStatusCode(500)
                        .withBody("error")
                );

        Assertions.assertThatThrownBy(() -> cmsClient.deleteDocument(DOCUMENT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CMS response Internal Server Error error: error");

    }

    @Test
    @DisplayName("Успешная загрузка изображения")
    void uploadImage_success() {
        server
                .when(request()
                        .withMethod("POST")
                        .withPath("/v1/upload")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withBody(loadFile("json/upload.json"), StandardCharsets.UTF_8)
                );

        CmsImage response = cmsClient.uploadImage("image".getBytes());

        CmsImageData expectedData = new CmsImageData();
        expectedData.setUrl("//avatars.mdst.yandex.net/get-marketcms" +
                "/69442/img-1ddafe65-15f8-4b46-97d1-789508282e4b.png/optimize");
        expectedData.setWidth(200);
        expectedData.setHeight(100);
        CmsImage expected = new CmsImage();
        expected.setData(expectedData);

        Assertions.assertThat(response)
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("Загрузка изображения возвращает неверный ответ и завершается исключением")
    void uploadImage_errorResponse_exception() {
        server
                .when(request()
                        .withMethod("POST")
                        .withPath("/v1/upload")
                )
                .respond(response()
                        .withStatusCode(500)
                        .withBody("error")
                );

        Assertions.assertThatThrownBy(() -> cmsClient.uploadImage("image".getBytes()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CMS response Internal Server Error error: error");

    }

    private void initMock(String method, String path, String requestFile, String responseFile,
                          Map<String, List<String>> parameters) {
        mockServerPath(
                method,
                path,
                requestFile == null ? null : "json/" + requestFile + ".json",
                parameters,
                200,
                "json/" + responseFile + ".json"
        );
    }

    private void assertTemplate(CmsTemplate actual, CmsTemplate expected) {
        Assertions.assertThat(actual)
                .isNotNull();
        Assertions.assertThat(actual.getData())
                .isNotNull();
        Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .ignoringFields("includes.entry.fields") // Не получается Object'ы проверить ...
                .isEqualTo(expected);
        // ... проверим хотя бы так
        Assertions.assertThat(actual.getIncludes().getEntry().get(0).getFields())
                .containsKey("INFO");
        Assertions.assertThat(actual.getIncludes().getEntry().get(1).getFields())
                .containsKey("PAGE_LINK");
    }

    @Nonnull
    private CmsTemplate template() {
        CmsTemplateData data = new CmsTemplateData();
        data.setName("SIS базовый шаблон - тест Копия");
        data.setDocumentType("business");
        data.setType("Resource");
        data.setRevisionId(4001124L);
        data.setLatestRevisionId(4032975L);

        data.setCreatedAt(OffsetDateTime.parse("2021-09-06T20:24:19Z"));
        data.setUpdatedAt(OffsetDateTime.parse("2021-09-22T12:43:39Z"));

        CmsTemplateEntity createdBy = new CmsTemplateEntity();
        createdBy.setId("135418388");
        createdBy.setType("Reference");
        createdBy.setReferenceType(CmsReferenceType.User);
        data.setCreatedBy(createdBy);

        CmsTemplateEntity updatedBy = new CmsTemplateEntity();
        updatedBy.setId("1513471018");
        updatedBy.setType("Reference");
        updatedBy.setReferenceType(CmsReferenceType.User);
        data.setUpdatedBy(updatedBy);

        CmsTemplateEntity entry = new CmsTemplateEntity();
        entry.setId("105434739");
        entry.setType("Reference");
        entry.setReferenceType(CmsReferenceType.Entry);
        data.setEntry(entry);

        CmsTemplateLink link1 = new CmsTemplateLink();
        link1.setLinkType("DRAFT");
        link1.setTargetDevice("PHONE");
        link1.setUrl("//m.market.yandex.ru/business/100000?pda-redir=1&content-preview=once");
        CmsTemplateLink link2 = new CmsTemplateLink();
        link2.setLinkType("DRAFT");
        link2.setTargetDevice("DESKTOP");
        link2.setUrl("//market.yandex.ru/business/100000?content-preview=once");
        data.setPreviewLinks(List.of(link1, link2));
        data.setIsTemplate(false);

        CmsTemplateEntity branchRevision = new CmsTemplateEntity();
        branchRevision.setId("319025");
        branchRevision.setType("Reference");
        branchRevision.setReferenceType(CmsReferenceType.BranchRevision);
        data.setBranchRevision(branchRevision);

        CmsTemplateIncludes includes = new CmsTemplateIncludes();

        CmsTemplateEntry templateEntry1 = new CmsTemplateEntry();
        templateEntry1.setType("Entry");
        templateEntry1.setId("105434739");
        templateEntry1.setParentId(null);

        CmsTemplateEntrySys sys = new CmsTemplateEntrySys();
        sys.setId(105434739L);
        sys.setHiddenExports(List.of());
        CmsTemplateEntity contentType = new CmsTemplateEntity();
        contentType.setId("BRAND_DESKTOP");
        contentType.setType("Reference");
        contentType.setReferenceType(CmsReferenceType.ContentType);
        sys.setContentType(contentType);
        templateEntry1.setSys(sys);

        CmsTemplateEntity field1 = new CmsTemplateEntity();
        field1.setId("105434740");
        field1.setType("Reference");
        field1.setReferenceType(CmsReferenceType.Entry);
        CmsTemplateEntity field2 = new CmsTemplateEntity();
        field2.setId("105434741");
        field2.setType("Reference");
        field2.setReferenceType(CmsReferenceType.Entry);
        CmsTemplateEntity field3 = new CmsTemplateEntity();
        field3.setId("105434749");
        field3.setType("Reference");
        field3.setReferenceType(CmsReferenceType.Entry);
        templateEntry1.setFields(Map.of(
                "LINKS", field1,
                "CONTENT", field2,
                "INFO", field3
        ));

        CmsTemplateEntry templateEntry2 = new CmsTemplateEntry();
        templateEntry2.setType("Entry");
        templateEntry2.setId("105434740");
        templateEntry2.setParentId("105434739");

        CmsTemplateEntrySys sys1 = new CmsTemplateEntrySys();
        sys1.setId(105434740L);
        sys1.setHiddenExports(List.of());
        CmsTemplateEntity contentType1 = new CmsTemplateEntity();
        contentType1.setId("PAGE_LINKS");
        contentType1.setType("Reference");
        contentType1.setReferenceType(CmsReferenceType.ContentType);
        sys1.setContentType(contentType1);
        templateEntry2.setSys(sys1);

        CmsTemplateEntity field4 = new CmsTemplateEntity();
        field4.setId("105447478");
        field4.setType("Reference");
        field4.setReferenceType(CmsReferenceType.Entry);
        CmsTemplateEntity field5 = new CmsTemplateEntity();
        field5.setId("105447479");
        field5.setType("Reference");
        field5.setReferenceType(CmsReferenceType.Entry);
        templateEntry2.setFields(Map.of(
                "PAGE_LINK", List.of(field4, field5)
        ));

        includes.setEntry(List.of(templateEntry1, templateEntry2));

        CmsTemplate template = new CmsTemplate();
        template.setData(data);
        template.setIncludes(includes);

        return template;
    }

    @Nonnull
    private CmsStandsData standsData(boolean isError) {
        CmsStandInfo cmsStandInfoError = new CmsStandInfo();
        cmsStandInfoError.setType("Stand");
        cmsStandInfoError.setId("buker");
        cmsStandInfoError.setStatus(isError ? "ERROR" : "OK");
        cmsStandInfoError.setMessage(isError ? "WRONG" : null);

        CmsStandInfo cmsStandInfo = new CmsStandInfo();
        cmsStandInfo.setType("Stand");
        cmsStandInfo.setId("saas");
        cmsStandInfo.setStatus("OK");

        CmsStandsData cmsStandsData = new CmsStandsData();
        cmsStandsData.setType("Array");
        cmsStandsData.setTotal(2);
        cmsStandsData.setItems(List.of(cmsStandInfoError, cmsStandInfo));

        return cmsStandsData;
    }

    @Nonnull
    private CmsTemplateInfo createCmsTemplateInfo(long id, long revisionId, String title) {
        CmsTemplateInfo cmsTemplateInfo = new CmsTemplateInfo();
        cmsTemplateInfo.setId(id);
        cmsTemplateInfo.setType("business");
        cmsTemplateInfo.setRevisionId(revisionId);
        cmsTemplateInfo.setTitle(title);

        return cmsTemplateInfo;
    }
}
