package ru.yandex.market.adv.content.manager.api;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.verify.VerificationTimes;
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

@DisplayName("Тесты на endpoint POST /v1/cms/template/moderation")
@MockServerSettings(ports = 12233)
class PostTemplateModerationCmsTemplateApiServiceTest extends AbstractContentManagerMockServerTest {

    @Autowired
    private MockMvc mvc;

    PostTemplateModerationCmsTemplateApiServiceTest(MockServerClient server) {
        super(server);
    }

    @DisplayName("Шаблон без модерируемых полей. Модерация провалилась, так как публикация сломалась.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_badRequestFromPublished_badRequest.before.csv",
            after = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_badRequestFromPublished_badRequest.after.csv"
    )
    @Test
    void moderate_badRequestFromPublished_badRequest() throws Exception {
        mockServerPath("PUT",
                "/v1/documents/5233/published",
                null,
                Map.of(
                        "userId", List.of("1513471018"),
                        "revision_id", List.of("99232")
                ),
                400,
                "CmsTemplateApi/PostTemplateModeration/json/server/response/" +
                        "moderate_badRequestFromPublished_badRequest_published.json"
        );

        mockAndSendRequest("moderate_badRequestFromPublished_badRequest", status().isBadRequest());

        server.verify(
                request()
                        .withMethod("PUT")
                        .withPath("/v1/documents/5233/published")
                        .withQueryStringParameters(
                                Map.of(
                                        "userId", List.of("1513471018"),
                                        "revision_id", List.of("99232")
                                )
                        ),
                VerificationTimes.once()
        );
        server.verify(
                request()
                        .withMethod("DELETE")
                        .withPath("/jsonPageApi/v1/deleteResourceById")
                        .withQueryStringParameters(
                                Map.of(
                                        "id", List.of("5233"),
                                        "userId", List.of("1513471018")
                                )
                        ),
                VerificationTimes.exactly(0)
        );
    }

    @DisplayName("Шаблон без модерируемых полей. Модерация завершена.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_noModeratedContent_successWithEmptyData.before.csv",
            after = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_noModeratedContent_successWithEmptyData.after.csv"
    )
    @Test
    void moderate_noModeratedContent_successWithEmptyData() throws Exception {
        moderateAndPublish("moderate_noModeratedContent_successWithEmptyData");
    }

    @DisplayName("Шаблон c полями без премодерации. Модерация запущена.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_noPreModeratedContent_taskToModerate.before.csv",
            after = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_noPreModeratedContent_taskToModerate.after.csv"
    )
    @Test
    void moderate_noPreModeratedContent_taskToModerate() throws Exception {
        mockAndSendRequest("moderate_noPreModeratedContent_taskToModerate", status().isOk());
    }

    @DisplayName("Шаблон c полями c успешной премодерацией. Модерация завершена.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_successPreModeratedContent_success.before.csv",
            after = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_successPreModeratedContent_success.after.csv"
    )
    @Test
    void moderate_successPreModeratedContent_success() throws Exception {
        moderateAndPublish("moderate_successPreModeratedContent_success");
    }

    @DisplayName("Шаблон c частью полей c успешной премодерацией. Модерация запущена (часть полей готово).")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_partiallySuccessPreModeratedContent_taskToModerate.before.csv",
            after = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_partiallySuccessPreModeratedContent_taskToModerate.after.csv"
    )
    @Test
    void moderate_partiallySuccessPreModeratedContent_taskToModerate() throws Exception {
        mockAndSendRequest("moderate_partiallySuccessPreModeratedContent_taskToModerate", status().isOk());
    }

    @DisplayName("Шаблон c полями без премодерации и равным премодерированным контентом другого правила. " +
            "Модерация запущена.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_noPreModeratedContentWithAnotherRuleSuccessPremoderated_taskToModerate.before.csv",
            after = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_noPreModeratedContentWithAnotherRuleSuccessPremoderated_taskToModerate.after.csv"
    )
    @Test
    void moderate_noPreModeratedContentWithAnotherRuleSuccessPremoderated_taskToModerate() throws Exception {
        mockAndSendRequest("moderate_noPreModeratedContentWithAnotherRuleSuccessPremoderated_taskToModerate",
                status().isOk());
    }

    @DisplayName("Шаблон c полями c неуспешной премодерацией. Неуспешная модерация.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_failedPreModeratedContent_failModeration.before.csv",
            after = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_failedPreModeratedContent_failModeration.after.csv"
    )
    @Test
    void moderate_failedPreModeratedContent_failModeration() throws Exception {
        sendRequest("moderate_failedPreModeratedContent_failModeration", status().isBadRequest(), "443");
        server.verifyZeroInteractions();
    }

    @DisplayName("Шаблон c невалидными полями. Ошибка валидации.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_invalidContent_validateError.before.csv",
            after = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_invalidContent_validateError.after.csv"
    )
    @Test
    void moderate_invalidContent_validateError() throws Exception {
        sendRequest("moderate_invalidContent_validateError", status().isBadRequest(), "443");
        server.verifyZeroInteractions();
    }

    @DisplayName("Шаблон находится на модерации. Запрещаем сохранять новый.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_existedModeration_badRequest.before.csv",
            after = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_existedModeration_badRequest.after.csv"
    )
    @Test
    void moderate_existedModeration_badRequest() throws Exception {
        sendRequest("moderate_existedModeration_badRequest", status().isBadRequest(), "550");
        server.verifyZeroInteractions();
    }

    @DisplayName("Шаблон по бизнесу нашелся в БД. Запрет на создание нового документа из базового. Ошибка.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_existedTemplateAndBaseTemplate_badRequest.before.csv",
            after = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_existedTemplateAndBaseTemplate_badRequest.after.csv"
    )
    @Test
    void moderate_existedTemplateAndBaseTemplate_badRequest() throws Exception {
        sendRequest("moderate_existedTemplateAndBaseTemplate_badRequest", status().isBadRequest(), "551");
        server.verifyZeroInteractions();
    }

    @DisplayName("Шаблон с заданным id не нашелся в БД. Ошибка.")
    @Test
    void moderate_unknownTemplate_notFound() throws Exception {
        sendRequest("moderate_unknownTemplate_notFound", status().isNotFound(), "552");
        server.verifyZeroInteractions();
    }

    @DisplayName("Шаблон c полями c успешной премодерацией. Ошибка при публикации. Ошибка и ничего не сохранили")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_successPreModeratedContentAndErrorPublished_badRequest.before.csv",
            after = "CmsTemplateApi/PostTemplateModeration/csv/" +
                    "moderate_successPreModeratedContentAndErrorPublished_badRequest.after.csv"
    )
    @Test
    void moderate_successPreModeratedContentAndErrorPublished_badRequest() throws Exception {
        mockServerPath("POST",
                "/v1/documents",
                "CmsTemplateApi/PostTemplateModeration/json/server/request/" +
                        "moderate_successPreModeratedContentAndErrorPublished_badRequest_post.json",
                Map.of("userId", List.of("1513471018")),
                200,
                "CmsTemplateApi/PostTemplateModeration/json/server/response/" +
                        "moderate_successPreModeratedContentAndErrorPublished_badRequest_post.json"
        );
        mockServerPath("PUT",
                "/v1/documents/5233/published",
                null,
                Map.of(
                        "userId", List.of("1513471018"),
                        "revision_id", List.of("99232")
                ),
                404,
                null
        );
        mockServerPath("DELETE",
                "/jsonPageApi/v1/deleteResourceById",
                null,
                Map.of(
                        "id", List.of("5233"),
                        "userId", List.of("1513471018")
                ),
                204,
                null
        );

        mockAndSendRequest("moderate_successPreModeratedContentAndErrorPublished_badRequest", status().isNotFound());

        server.verify(
                request()
                        .withMethod("PUT")
                        .withPath("/v1/documents/5233/published")
                        .withQueryStringParameters(
                                Map.of(
                                        "userId", List.of("1513471018"),
                                        "revision_id", List.of("99232")
                                )
                        ),
                VerificationTimes.once()
        );
        server.verify(
                request()
                        .withMethod("DELETE")
                        .withPath("/jsonPageApi/v1/deleteResourceById")
                        .withQueryStringParameters(
                                Map.of(
                                        "id", List.of("5233"),
                                        "userId", List.of("1513471018")
                                )
                        ),
                VerificationTimes.once()
        );
    }

    private void moderateAndPublish(String methodName) throws Exception {
        mockServerPath("PUT",
                "/v1/documents/5233/published",
                null,
                Map.of(
                        "userId", List.of("1513471018"),
                        "revision_id", List.of("99232")
                ),
                200,
                "CmsTemplateApi/PostTemplateModeration/json/server/response/" +
                        methodName + "_published.json"
        );

        mockAndSendRequest(methodName, status().isOk());

        server.verify(
                request()
                        .withMethod("PUT")
                        .withPath("/v1/documents/5233/published")
                        .withQueryStringParameters(
                                Map.of(
                                        "userId", List.of("1513471018"),
                                        "revision_id", List.of("99232")
                                )
                        ),
                VerificationTimes.once()
        );
    }

    private void mockAndSendRequest(String methodName, ResultMatcher statusMatcher) throws Exception {
        mockServerPath("PUT",
                "/v1/documents/5233",
                "CmsTemplateApi/PostTemplateModeration/json/server/request/" +
                        methodName + ".json",
                Map.of("userId", List.of("1513471018")),
                200,
                "CmsTemplateApi/PostTemplateModeration/json/server/response/" +
                        methodName + ".json"
        );

        sendRequest(methodName, statusMatcher, "443");
    }

    private void sendRequest(String methodName, ResultMatcher statusMatcher, String businessId) throws Exception {
        mvc.perform(
                        post("/v1/cms/template/moderation?business_id=" + businessId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        loadFile("CmsTemplateApi/PostTemplateModeration/json/request/" +
                                                methodName + ".json")
                                )
                )
                .andExpect(statusMatcher)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                                loadFile("CmsTemplateApi/PostTemplateModeration/json/response/" +
                                        methodName + ".json")
                        )
                );
    }
}
