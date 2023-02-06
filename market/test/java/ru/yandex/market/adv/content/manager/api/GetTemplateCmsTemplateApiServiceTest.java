package ru.yandex.market.adv.content.manager.api;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.adv.content.manager.AbstractContentManagerMockServerTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockserver.model.HttpRequest.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Date: 18.10.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
@DisplayName("Тесты на endpoint GET /v1/cms/template.")
@MockServerSettings(ports = 12233)
class GetTemplateCmsTemplateApiServiceTest extends AbstractContentManagerMockServerTest {

    @Autowired
    private MockMvc mvc;

    GetTemplateCmsTemplateApiServiceTest(MockServerClient server) {
        super(server);
    }

    @DisplayName("Вернули базовый шаблон при его запросе, не сделав запрос шаблона в БД.")
    @Test
    void v1CmsTemplateGet_baseDocument_okAndBaseTemplate() throws Exception {
        v1CmsTemplateGet("v1CmsTemplateGet_baseDocument_okAndBaseTemplate",
                status().isOk(), "152999", "", 200, "");
    }

    @DisplayName("Вернули ошибку при запросе шаблона, отсутствующего в БД.")
    @Test
    void v1CmsTemplateGet_documentNotFoundInDb_errorMessage() throws Exception {
        v1CmsTemplateGet("v1CmsTemplateGet_documentNotFoundInDb_errorMessage",
                status().isNotFound(), "4233", "/revisions/99531", 200, "");

        server.verifyZeroInteractions();
    }

    @DisplayName("Вернули ошибку при запросе шаблона, отсутствующего в CMS.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/GetTemplate/csv/" +
                    "v1CmsTemplateGet_documentNotFoundInCms_errorMessage.before.csv"
    )
    @Test
    void v1CmsTemplateGet_documentNotFoundInCms_errorMessage() throws Exception {
        v1CmsTemplateGet("v1CmsTemplateGet_documentNotFoundInCms_errorMessage",
                status().isNotFound(), "4233", "/revisions/99531", 404, "");

        server.verify(
                request()
                        .withMethod("GET")
                        .withPath("/v1/documents/4233/revisions/99531")
                        .withQueryStringParameters(Map.of("userId", List.of("1513471018")))
        );
    }

    @DisplayName("Вернули шаблон в статусе PUBLISHED и историю его изменения.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/GetTemplate/csv/" +
                    "v1CmsTemplateGet_publishedDocumentInDb_okAndTemplate.before.csv"
    )
    @Test
    void v1CmsTemplateGet_publishedDocumentInDb_okAndTemplate() throws Exception {
        v1CmsTemplateGet("v1CmsTemplateGet_publishedDocumentInDb_okAndTemplate",
                status().isOk(), "4233", "/revisions/99531", 200, "");
    }

    @DisplayName("Вернули шаблон в статусе DRAFT с типом express и историю его изменения.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/GetTemplate/csv/" +
                    "v1CmsTemplateGet_draftExpressDocumentInDb_okAndTemplate.before.csv"
    )
    @Test
    void v1CmsTemplateGet_draftExpressDocumentInDb_okAndTemplate() throws Exception {
        v1CmsTemplateGet("v1CmsTemplateGet_draftExpressDocumentInDb_okAndTemplate",
                status().isOk(), "7233", "/revisions/99531", 200, "&document_type=express");
    }

    @DisplayName("Вернули ошибку при запросе шаблона в статусе MODERATION_ERROR и отсутствие информации по модерации.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/GetTemplate/csv/" +
                    "v1CmsTemplateGet_moderationTaskNotFound_errorMessage.before.csv"
    )
    @Test
    void v1CmsTemplateGet_moderationTaskNotFound_errorMessage() throws Exception {
        v1CmsTemplateGet("v1CmsTemplateGet_moderationTaskNotFound_errorMessage",
                status().isBadRequest(), "5233", "/revisions/99531", 200, "");

        server.verifyZeroInteractions();
    }

    @DisplayName("Вернули шаблон в статусе MODERATION_ERROR, историю его изменения и информацию по ошибкам модерации.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/GetTemplate/csv/" +
                    "v1CmsTemplateGet_moderationErrorDocumentInDb_okAndTemplateAndModerationInfo.before.csv"
    )
    @Test
    void v1CmsTemplateGet_moderationErrorDocumentInDb_okAndTemplateAndModerationInfo() throws Exception {
        v1CmsTemplateGet("v1CmsTemplateGet_moderationErrorDocumentInDb_okAndTemplateAndModerationInfo",
                status().isOk(), "5233", "/revisions/99531", 200, "");
    }

    @DisplayName("Вернули шаблон в статусе MODERATION_ERROR и историю его изменения без ошибок модерации.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/GetTemplate/csv/" +
                    "v1CmsTemplateGet_moderationErrorDocumentInDb_okAndTemplateWithoutModerationInfo.before.csv"
    )
    @Test
    void v1CmsTemplateGet_moderationErrorDocumentInDb_okAndTemplateWithoutModerationInfo() throws Exception {
        v1CmsTemplateGet("v1CmsTemplateGet_moderationErrorDocumentInDb_okAndTemplateWithoutModerationInfo",
                status().isOk(), "4963", "/revisions/99531", 200, "");
    }

    @DisplayName("Вернули ошибку при запросе шаблона без обязательных параметров.")
    @CsvSource({
            "&document_id=10003,Revision id can't be null.",
            ",Required request parameter 'document_id' for method parameter type Long is not present"
    })
    @ParameterizedTest(name = "{1}")
    void v1CmsTemplateGet_wrongRequestParam_okAndTemplateAndModerationInfo(String suffix,
                                                                           String error) throws Exception {
        mvc.perform(
                        get("/v1/cms/template?business_id=443" + (suffix == null ? "" : suffix))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(error));
    }

    private void v1CmsTemplateGet(String methodName, ResultMatcher statusMatcher,
                                  String documentId, String suffix, int mockResponseCode,
                                  String documentType) throws Exception {
        mockServerPath("GET",
                "/v1/documents/" + documentId + suffix,
                null,
                Map.of("userId", List.of("1513471018")),
                mockResponseCode,
                "CmsTemplateApi/GetTemplate/json/server/response/" +
                        methodName + ".json"
        );

        mvc.perform(
                        get(
                                "/v1/cms/template?business_id=443&revision_id=99531&document_id=" +
                                        documentId + documentType
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(statusMatcher)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                                loadFile("CmsTemplateApi/GetTemplate/json/response/" +
                                        methodName + ".json")
                        )
                );
    }
}
