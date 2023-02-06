package ru.yandex.market.adv.content.manager.api;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.adv.content.manager.AbstractContentManagerMockServerTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Date: 30.11.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
@DisplayName("Тесты на endpoint POST /v1/cms/template/stand.")
@MockServerSettings(ports = 12233)
@ParametersAreNonnullByDefault
class PostTemplateStandApiServiceTest extends AbstractContentManagerMockServerTest {

    @Autowired
    private MockMvc mvc;

    PostTemplateStandApiServiceTest(MockServerClient server) {
        super(server);
    }

    @DisplayName("Обновление стенда предпросмотра прошло успешно.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplateStand/csv/" +
                    "v1CmsTemplateStandPost_correctTemplate_success.before.csv",
            after = "CmsTemplateApi/PostTemplateStand/csv/" +
                    "v1CmsTemplateStandPost_correctTemplate_success.after.csv"
    )
    @Test
    void v1CmsTemplateStandPost_correctTemplate_success() throws Exception {
        v1CmsTemplateStandPost("v1CmsTemplateStandPost_correctTemplate_success", "5233", "&document_type=express")
                .andExpect(status().isNoContent());
    }

    @SuppressWarnings("unused")
    @DisplayName("Обновление стенда предпросмотра шаблона не требуется.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplateStand/csv/" +
                    "v1CmsTemplateStandPost_template_successWithoutUpdate.before.csv",
            after = "CmsTemplateApi/PostTemplateStand/csv/" +
                    "v1CmsTemplateStandPost_template_successWithoutUpdate.after.csv"
    )
    @CsvSource({
            "152999,Base template.",
            "5231,Already update template."
    })
    @ParameterizedTest(name = "{1}")
    void v1CmsTemplateStandPost_template_successWithoutUpdate(String documentId, String test) throws Exception {
        mvc.perform(
                        post(
                                "/v1/cms/template/stand?business_id=443&revision_id=99531&document_id=" +
                                        documentId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());

        server.verifyZeroInteractions();
    }

    @DisplayName("Обновление стенда предпросмотра прошло успешно для опубликованного шаблона.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplateStand/csv/" +
                    "v1CmsTemplateStandPost_correctPublishedTemplate_success.before.csv",
            after = "CmsTemplateApi/PostTemplateStand/csv/" +
                    "v1CmsTemplateStandPost_correctPublishedTemplate_success.after.csv"
    )
    @Test
    void v1CmsTemplateStandPost_correctPublishedTemplate_success() throws Exception {
        v1CmsTemplateStandPost("v1CmsTemplateStandPost_correctPublishedTemplate_success", "5230",
                "&document_type=business")
                .andExpect(status().isNoContent());
    }

    @DisplayName("Обновление стенда предпросмотра шаблона завершилось ошибкой в CMS.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplateStand/csv/" +
                    "v1CmsTemplateStandPost_correctTemplate_standUpdateError.before.csv",
            after = "CmsTemplateApi/PostTemplateStand/csv/" +
                    "v1CmsTemplateStandPost_correctTemplate_standUpdateError.after.csv"
    )
    @Test
    void v1CmsTemplateStandPost_correctTemplate_standUpdateError() throws Exception {
        checkError("v1CmsTemplateStandPost_correctTemplate_standUpdateError", "5237",
                status().isBadRequest());
    }

    @DisplayName("Обновление стенда предпросмотра шаблона вернуло пустой список стендов из CMS.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplateStand/csv/" +
                    "v1CmsTemplateStandPost_correctTemplate_emptyStandUpdate.before.csv",
            after = "CmsTemplateApi/PostTemplateStand/csv/" +
                    "v1CmsTemplateStandPost_correctTemplate_emptyStandUpdate.after.csv"
    )
    @Test
    void v1CmsTemplateStandPost_correctTemplate_emptyStandUpdate() throws Exception {
        checkError("v1CmsTemplateStandPost_correctTemplate_emptyStandUpdate", "5236",
                status().isBadRequest());
    }

    @DisplayName("Обновление стенда предпросмотра шаблона происходит по неизвестному шаблону.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/PostTemplateStand/csv/" +
                    "v1CmsTemplateStandPost_unknownTemplate_documentNotFound.before.csv",
            after = "CmsTemplateApi/PostTemplateStand/csv/" +
                    "v1CmsTemplateStandPost_unknownTemplate_documentNotFound.after.csv"
    )
    @Test
    void v1CmsTemplateStandPost_unknownTemplate_documentNotFound() throws Exception {
        checkError("v1CmsTemplateStandPost_unknownTemplate_documentNotFound", "5235",
                status().isNotFound());

        server.verifyZeroInteractions();
    }

    private void checkError(String methodName,
                            String documentId,
                            ResultMatcher resultMatcher) throws Exception {
        v1CmsTemplateStandPost(methodName, documentId, "")
                .andExpect(resultMatcher)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content()
                        .json(
                                loadFile("CmsTemplateApi/PostTemplateStand/json/response/" + methodName + ".json"),
                                true
                        )
                );
    }

    @Nonnull
    private ResultActions v1CmsTemplateStandPost(String methodName,
                                                 String documentId,
                                                 String documentType) throws Exception {
        mockServerPath("PUT",
                "/v1/documents/" + documentId + "/revisions/99531/stands/draft",
                null,
                Map.of("userId", List.of("1513471018")),
                200,
                "CmsTemplateApi/PostTemplateStand/json/server/response/" +
                        methodName + ".json"
        );

        return mvc.perform(
                post(
                        "/v1/cms/template/stand?business_id=443&revision_id=99531&document_id=" +
                                documentId + documentType
                )
                        .contentType(MediaType.APPLICATION_JSON)
        );
    }
}
