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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Date: 13.10.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
@DisplayName("Тесты на endpoint DELETE /v1/cms/template.")
@MockServerSettings(ports = 12233)
class DeleteTemplateCmsTemplateApiServiceTest extends AbstractContentManagerMockServerTest {

    @Autowired
    private MockMvc mvc;

    DeleteTemplateCmsTemplateApiServiceTest(MockServerClient server) {
        super(server);
    }

    @DisplayName("Успешное удаление шаблона.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/DeleteTemplate/csv/" +
                    "v1CmsTemplateDelete_existingTemplate_success.before.csv",
            after = "CmsTemplateApi/DeleteTemplate/csv/" +
                    "v1CmsTemplateDelete_existingTemplate_success.after.csv"
    )
    @Test
    void v1CmsTemplateDelete_existingTemplate_success() throws Exception {
        deleteTemplate("", "5233");
    }

    @DisplayName("Успешное удаление шаблона.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/DeleteTemplate/csv/" +
                    "v1CmsTemplateDelete_existingExpressTemplate_success.before.csv",
            after = "CmsTemplateApi/DeleteTemplate/csv/" +
                    "v1CmsTemplateDelete_existingExpressTemplate_success.after.csv"
    )
    @Test
    void v1CmsTemplateDelete_existingExpressTemplate_success() throws Exception {
        deleteTemplate("&document_type=express", "6233");
    }

    @DisplayName("Шаблона нет в БД. Ошибка 404.")
    @Test
    void v1CmsTemplateDelete_notExistingTemplate_notFound() throws Exception {
        checkError("743", status().isNotFound(), "v1CmsTemplateDelete_notExistingTemplate_notFound");
    }

    @DisplayName("Шаблона в БД в статусе MODERATION. Ошибка 400.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/DeleteTemplate/csv/" +
                    "v1CmsTemplateDelete_moderationTemplateExist_badRequest.before.csv"
    )
    @Test
    void v1CmsTemplateDelete_moderationTemplateExist_badRequest() throws Exception {
        checkError("552", status().isBadRequest(), "v1CmsTemplateDelete_moderationTemplateExist_badRequest");
    }

    private void deleteTemplate(String suffix, String documentId) throws Exception {
        mockServerPath("DELETE",
                "/jsonPageApi/v1/deleteResourceById",
                null,
                Map.of(
                        "userId", List.of("1513471018"),
                        "id", List.of(documentId)
                ),
                200,
                null);
        mvc.perform(
                        delete("/v1/cms/template?business_id=443&document_id=" + documentId + suffix)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());
        server.verify(request()
                .withMethod("DELETE")
                .withPath("/jsonPageApi/v1/deleteResourceById")
                .withQueryStringParameters(
                        Map.of(
                                "userId", List.of("1513471018"),
                                "id", List.of(documentId)
                        )
                )
        );
    }

    private void checkError(String businessId, ResultMatcher resultMatcher, String methodName) throws Exception {
        mvc.perform(
                        delete("/v1/cms/template?business_id=" + businessId + "&document_id=5233")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(resultMatcher)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content()
                        .json(
                                loadFile("CmsTemplateApi/DeleteTemplate/json/response/" + methodName + ".json"),
                                true
                        )
                );
        server.verifyZeroInteractions();
    }
}
