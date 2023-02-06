package ru.yandex.market.logistics.tarifficator.controller;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Интеграционный тест контроллера TagController")
@DatabaseSetup("/controller/tags/db/before/tags.xml")
class TagControllerTest extends AbstractContextualTest {

    @Test
    @DisplayName("Получение тегов тарифа")
    void getTags() throws Exception {
        mockMvc.perform(get("/tariffs/1/tags"))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/tags/response/tariff_tags.json"));
    }

    @Test
    @DisplayName("Получение тегов для несуществующего тарифа")
    void getTagsForNonExistsTariff() throws Exception {
        mockMvc.perform(get("/tariffs/0/tags"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("message").value("Failed to find [TARIFF] with ids [[0]]"));
    }

    @Test
    @DisplayName("Удалить все теги тарифа")
    @DatabaseSetup("/controller/tags/db/before/clear_tags.xml")
    @ExpectedDatabase(value = "/controller/tags/db/after/clear_tags.xml", assertionMode = NON_STRICT_UNORDERED)
    void clearTags() throws Exception {
        performPut("/tariffs/1/tags", "controller/tags/request/clear_tags.json")
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/tags/response/clear_tags.json"));
    }

    @Test
    @DisplayName("Установить теги тарифа")
    @DatabaseSetup("/controller/tags/db/before/update_tariff_tags.xml")
    @ExpectedDatabase(value = "/controller/tags/db/after/update_tariff_tags.xml", assertionMode = NON_STRICT_UNORDERED)
    void updateTags() throws Exception {
        performPut("/tariffs/1/tags", "controller/tags/request/update_tariff_tags.json")
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/tags/response/update_tariff_tags.json"));
    }

    @Test
    @DisplayName("Установить теги тарифа DAAS и другой")
    @DatabaseSetup("/controller/tags/db/before/update_tariff_tags.xml")
    void updateTagsDaasAndOthersMustBeMutuallyExclusive() throws Exception {
        performPut("/tariffs/1/tags", "controller/tags/request/update_tariff_tags_daas_beru.json")
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("DAAS tag and the others must be mutually exclusive"));
    }

    @Test
    @DisplayName("Установка несуществующего тега")
    @ExpectedDatabase(value = "/controller/tags/db/before/tags.xml", assertionMode = NON_STRICT_UNORDERED)
    void updateNonExistsTags() throws Exception {
        performPut("/tariffs/1/tags", "controller/tags/request/update_non_exists_tariff_tags.json")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("message").value("Failed to find [TAG] with ids [[SOME_PROGRAM]]"));
    }

    @Test
    @DisplayName("Установка тегов для несуществующего тарифа")
    @ExpectedDatabase(value = "/controller/tags/db/before/tags.xml", assertionMode = NON_STRICT_UNORDERED)
    void updateTagsForNonExistsTariff() throws Exception {
        performPut("/tariffs/0/tags", "controller/tags/request/update_tariff_tags.json")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("message").value("Failed to find [TARIFF] with ids [[0]]"));
    }

    @Test
    @DisplayName("Получить все теги")
    void getAllTags() throws Exception {
        mockMvc.perform(get("/tariffs/all-tags"))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/tags/response/all_tags.json"));
    }

    @Nonnull
    private ResultActions performPut(String url, String sendingContentPath) throws Exception {
        return mockMvc.perform(
            put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(sendingContentPath))
        );
    }
}
