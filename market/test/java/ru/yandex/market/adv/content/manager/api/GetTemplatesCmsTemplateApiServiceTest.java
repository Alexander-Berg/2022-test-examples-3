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

import ru.yandex.market.adv.content.manager.AbstractContentManagerMockServerTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockserver.model.HttpRequest.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Date: 12.10.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
@DisplayName("Тесты на endpoint GET /v1/cms/templates.")
@MockServerSettings(ports = 12233)
class GetTemplatesCmsTemplateApiServiceTest extends AbstractContentManagerMockServerTest {

    @Autowired
    private MockMvc mvc;

    GetTemplatesCmsTemplateApiServiceTest(MockServerClient server) {
        super(server);
    }

    @DisplayName("На запрос с бизнесом, по которому есть шаблоны, возвращается их список.")
    @DbUnitDataSet(
            before = "CmsTemplateApi/GetTemplates/csv/v1CmsTemplatesGet_businessInDb_okAndFullTemplateList.before.csv"
    )
    @Test
    void v1CmsTemplatesGet_businessInDb_okAndFullTemplateList() throws Exception {
        mvc.perform(
                        get("/v1/cms/templates?business_id=443")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                                loadFile("CmsTemplateApi/GetTemplates/json/response/" +
                                        "v1CmsTemplatesGet_businessInDb_okAndFullTemplateList.json")
                        )
                );

        server.verifyZeroInteractions();
    }

    @DisplayName("На запрос с бизнесом, по которому нет шаблонов, возвращается базовый шаблон.")
    @Test
    void v1CmsTemplatesGet_unknownBusiness_okAndBaseTemplate() throws Exception {
        mockServerPath("GET",
                "/jsonPageApi/v1/getResourceList",
                null,
                Map.of(
                        "userId", List.of("1513471018"),
                        "limit", List.of("10"),
                        "type", List.of("business"),
                        "sort", List.of("updated:desc"),
                        "search", List.of("business_id:554")
                ),
                200,
                "CmsTemplateApi/GetTemplates/json/server/response/" +
                        "v1CmsTemplatesGet_unknownBusiness_okAndBaseTemplate.json"
        );

        mvc.perform(
                        get("/v1/cms/templates?business_id=554")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                                loadFile("CmsTemplateApi/GetTemplates/json/response/" +
                                        "v1CmsTemplatesGet_unknownBusiness_okAndBaseTemplate.json")
                        )
                );

        server.verify(request()
                .withMethod("GET")
                .withPath("/jsonPageApi/v1/getResourceList")
                .withQueryStringParameters(
                        Map.of(
                                "userId", List.of("1513471018"),
                                "limit", List.of("10"),
                                "type", List.of("business"),
                                "sort", List.of("updated:desc"),
                                "search", List.of("business_id:554")
                        )
                )
        );
    }

    @DisplayName("На запрос с бизнесом, по которому нет шаблонов, возвращается кастомный шаблон из CMS.")
    @Test
    void v1CmsTemplatesGet_unknownBusiness_okAndCustomTemplate() throws Exception {
        mockServerPath("GET",
                "/jsonPageApi/v1/getResourceList",
                null,
                Map.of(
                        "userId", List.of("1513471018"),
                        "limit", List.of("10"),
                        "type", List.of("business"),
                        "sort", List.of("updated:desc"),
                        "search", List.of("business_id:453")
                ),
                200,
                "CmsTemplateApi/GetTemplates/json/server/response/" +
                        "v1CmsTemplatesGet_unknownBusiness_okAndCustomTemplate.json"
        );

        mvc.perform(
                        get("/v1/cms/templates?business_id=453")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                                loadFile("CmsTemplateApi/GetTemplates/json/response/" +
                                        "v1CmsTemplatesGet_unknownBusiness_okAndCustomTemplate.json")
                        )
                );

        server.verify(request()
                .withMethod("GET")
                .withPath("/jsonPageApi/v1/getResourceList")
                .withQueryStringParameters(
                        Map.of(
                                "userId", List.of("1513471018"),
                                "limit", List.of("10"),
                                "type", List.of("business"),
                                "sort", List.of("updated:desc"),
                                "search", List.of("business_id:453")
                        )
                )
        );
    }
}
