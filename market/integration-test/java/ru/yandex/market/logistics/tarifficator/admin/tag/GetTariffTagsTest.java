package ru.yandex.market.logistics.tarifficator.admin.tag;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.missingParameter;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/controller/tags/db/before/tags.xml")
@DisplayName("Получение программ тарифа через админку")
class GetTariffTagsTest extends AbstractContextualTest {
    @Test
    @DisplayName("Получить программы тарифа")
    void getTariffTags() throws Exception {
        getTariffTags(1)
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/admin/tags/get/response/get_tags_response.json"));
    }

    @Test
    @DisplayName("Получить программы несуществующего тарифа")
    void getTariffTagsTariffNotFound() throws Exception {
        getTariffTags(0)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [TARIFF] with ids [[0]]"));
    }

    @Test
    @DisplayName("Получить программы неуказанного тарифа")
    void getTagsTariffIdNotPresent() throws Exception {
        mockMvc.perform(get("/admin/tariffs/tags"))
            .andExpect(status().isBadRequest())
            .andExpect(missingParameter("tariffId", "long"));
    }

    @Nonnull
    ResultActions getTariffTags(long tariffId) throws Exception {
        return mockMvc.perform(get("/admin/tariffs/tags").param("tariffId", Long.toString(tariffId)));
    }
}
