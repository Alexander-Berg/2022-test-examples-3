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

import static org.mockserver.model.HttpRequest.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Date: 24.03.2022
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
@DisplayName("Тесты на endpoint GET /v1/cms/template/last.")
@MockServerSettings(ports = 12233)
class GetLastTemplateCmsTemplateApiServiceTest extends AbstractContentManagerMockServerTest {

    @Autowired
    private MockMvc mvc;

    GetLastTemplateCmsTemplateApiServiceTest(MockServerClient server) {
        super(server);
    }

    @DisplayName("Вернули базовый шаблон при его запросе, не сделав запрос шаблона в БД.")
    @Test
    void v1CmsTemplateLastGet_document_ok() throws Exception {
        v1CmsTemplateLastGet("v1CmsTemplateLastGet_document_ok",
                status().isOk(), "152999", 200);
    }

    @DisplayName("Вернули ошибку при запросе шаблона, отсутствующего в CMS.")
    @Test
    void v1CmsTemplateLastGet_unexpectedDocument_notFound() throws Exception {
        v1CmsTemplateLastGet("v1CmsTemplateLastGet_unexpectedDocument_notFound",
                status().isNotFound(), "4233", 404);

        server.verify(
                request()
                        .withMethod("GET")
                        .withPath("/v1/documents/4233")
                        .withQueryStringParameters(Map.of("userId", List.of("1513471018")))
        );
    }

    private void v1CmsTemplateLastGet(String methodName,
                                      ResultMatcher statusMatcher,
                                      String documentId,
                                      int mockResponseCode) throws Exception {
        mockServerPath("GET",
                "/v1/documents/" + documentId,
                null,
                Map.of("userId", List.of("1513471018")),
                mockResponseCode,
                "CmsTemplateApi/GetLastTemplate/json/server/response/" +
                        methodName + ".json"
        );

        mvc.perform(
                        get(
                                "/v1/cms/template/last?document_id=" + documentId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(statusMatcher)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                                loadFile("CmsTemplateApi/GetLastTemplate/json/response/" +
                                        methodName + ".json")
                        )
                );
    }
}
