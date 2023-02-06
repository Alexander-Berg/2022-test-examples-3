package ru.yandex.market.adv.content.manager.api;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.adv.content.manager.AbstractContentManagerMockServerTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockserver.model.HttpRequest.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Date: 13.10.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
@DisplayName("Тесты на endpoint POST /v1/cms/template.")
@MockServerSettings(ports = 12233)
class PostTemplateCmsTemplateApiServiceTest extends AbstractContentManagerMockServerTest {

    @Autowired
    private MockMvc mvc;

    PostTemplateCmsTemplateApiServiceTest(MockServerClient server) {
        super(server);
    }

    @DisplayName("Сохранение шаблона завершилось ошибкой.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplate/csv/" +
                    "v1CmsTemplatePost_internalErrorFromServer_errorMessage.before.csv",
            after = "CmsTemplateApi/PostTemplate/csv/" +
                    "v1CmsTemplatePost_internalErrorFromServer_errorMessage.after.csv"
    )
    @Test
    void v1CmsTemplatePost_internalErrorFromServer_errorMessage() throws Exception {
        v1CmsTemplatePost("v1CmsTemplatePost_internalErrorFromServer_errorMessage",
                status().isInternalServerError(), "/v1/documents/5233", 500);
    }

    @DisplayName("Шаблона нет. Создали новый шаблон.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplate/csv/" +
                    "v1CmsTemplatePost_baseTemplate_newDocument.before.csv",
            after = "CmsTemplateApi/PostTemplate/csv/" +
                    "v1CmsTemplatePost_baseTemplate_newDocument.after.csv"
    )
    @Test
    void v1CmsTemplatePost_baseTemplate_newDocument() throws Exception {
        mockServerPath("POST",
                "/v1/documents",
                "CmsTemplateApi/PostTemplate/json/server/request/" +
                        "v1CmsTemplatePost_baseTemplate_newDocument_post.json",
                Map.of("userId", List.of("1513471018")),
                200,
                "CmsTemplateApi/PostTemplate/json/server/response/" +
                        "v1CmsTemplatePost_baseTemplate_newDocument_post.json"
        );

        v1CmsTemplatePost("v1CmsTemplatePost_baseTemplate_newDocument",
                status().isOk(), "/v1/documents/4233", 200);

        server.verify(request()
                .withMethod("PUT")
                .withPath("/v1/documents/4233")
                .withQueryStringParameters(Map.of("userId", List.of("1513471018")))
        );
        server.verify(request()
                .withMethod("POST")
                .withPath("/v1/documents")
                .withQueryStringParameters(Map.of("userId", List.of("1513471018")))
        );
    }

    @DisplayName("Шаблона нет. Ошибка. Не создали новый шаблон, так как уже его начали создавать при другом вызове.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplate/csv/" +
                    "v1CmsTemplatePost_baseTemplateAndPreTemplate_error.before.csv",
            after = "CmsTemplateApi/PostTemplate/csv/" +
                    "v1CmsTemplatePost_baseTemplateAndPreTemplate_error.after.csv"
    )
    @Test
    void v1CmsTemplatePost_baseTemplateAndPreTemplate_error() throws Exception {
        sendRequest("v1CmsTemplatePost_baseTemplateAndPreTemplate_error", status().isTooManyRequests(), "593");

        server.verifyZeroInteractions();
    }

    @DisplayName("Шаблон есть. Создали новую ревизию.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplate/csv/" +
                    "v1CmsTemplatePost_customTemplate_newRevision.before.csv",
            after = "CmsTemplateApi/PostTemplate/csv/" +
                    "v1CmsTemplatePost_customTemplate_newRevision.after.csv"
    )
    @Test
    void v1CmsTemplatePost_customTemplate_newRevision() throws Exception {
        v1CmsTemplatePost("v1CmsTemplatePost_customTemplate_newRevision",
                status().isOk(), "/v1/documents/5233", 200);
    }

    @DisplayName("Некорректное тело запроса. Ошибка разбора json.")
    @Test
    void v1CmsTemplatePost_incorrectBody_errorMessage() throws Exception {
        v1CmsTemplatePost("v1CmsTemplatePost_incorrectBody_errorMessage",
                status().isBadRequest(), "/v1/documents/5233", 200);

        server.verifyZeroInteractions();
    }

    @DisplayName("Некорректное тело запроса. Ошибка проверки входных данных.")
    @Test
    void v1CmsTemplatePost_invalidBody_errorMessage() throws Exception {
        v1CmsTemplatePost("v1CmsTemplatePost_invalidBody_errorMessage",
                status().isBadRequest(), "/v1/documents/5233", 200);

        server.verifyZeroInteractions();
    }

    @DisplayName("Некорректное данные в шаблоне. Ошибка валидации данных.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplate/csv/" +
                    "v1CmsTemplatePost_invalidTemplateFields_errorResponse.before.csv"
    )
    @Test
    void v1CmsTemplatePost_invalidTemplateFields_errorResponse() throws Exception {
        v1CmsTemplatePost("v1CmsTemplatePost_invalidTemplateFields_errorResponse",
                status().isBadRequest(), "/v1/documents/5233", 200);

        server.verifyZeroInteractions();
    }

    @DisplayName("Шаблон находится на модерации. Запрещаем сохранять новый.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplate/csv/" +
                    "v1CmsTemplatePost_existedModeration_badRequest.before.csv",
            after = "CmsTemplateApi/PostTemplate/csv/" +
                    "v1CmsTemplatePost_existedModeration_badRequest.after.csv"
    )
    @Test
    void v1CmsTemplatePost_existedModeration_badRequest() throws Exception {
        sendRequest("v1CmsTemplatePost_existedModeration_badRequest", status().isBadRequest(), "550");
        server.verifyZeroInteractions();
    }

    @DisplayName("Шаблон по бизнесу нашелся в БД. Запрет на создание нового документа из базового. Ошибка.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplate/csv/" +
                    "v1CmsTemplatePost_existedTemplateAndBaseTemplate_badRequest.before.csv",
            after = "CmsTemplateApi/PostTemplate/csv/" +
                    "v1CmsTemplatePost_existedTemplateAndBaseTemplate_badRequest.after.csv"
    )
    @Test
    void v1CmsTemplatePost_existedTemplateAndBaseTemplate_badRequest() throws Exception {
        sendRequest("v1CmsTemplatePost_existedTemplateAndBaseTemplate_badRequest", status().isBadRequest(), "551");
        server.verifyZeroInteractions();
    }

    @DisplayName("Шаблон с заданным id не нашелся в БД. Ошибка.")
    @Test
    void v1CmsTemplatePost_unknownTemplate_notFound() throws Exception {
        sendRequest("v1CmsTemplatePost_unknownTemplate_notFound", status().isNotFound(), "552");
        server.verifyZeroInteractions();
    }

    private void v1CmsTemplatePost(String methodName, ResultMatcher statusMatcher, String mockPath,
                                   int mockResponseCode) throws Exception {
        mockServerPath("PUT",
                mockPath,
                "CmsTemplateApi/PostTemplate/json/server/request/" +
                        methodName + ".json",
                Map.of("userId", List.of("1513471018")),
                mockResponseCode,
                "CmsTemplateApi/PostTemplate/json/server/response/" +
                        methodName + ".json"
        );

        sendRequest(methodName, statusMatcher, "443");
    }

    private void sendRequest(String methodName, ResultMatcher statusMatcher, String businessId) throws Exception {
        mvc.perform(
                        post("/v1/cms/template?business_id=" + businessId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        loadFile("CmsTemplateApi/PostTemplate/json/request/" +
                                                methodName + ".json")
                                )
                )
                .andExpect(statusMatcher)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                                loadFile("CmsTemplateApi/PostTemplate/json/response/" +
                                        methodName + ".json")
                        )
                );
    }
}
