package ru.yandex.market.logistics.nesu.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class MdsFileControllerTest extends AbstractContextualTest {
    @Test
    @DisplayName("Получить информацию об MDS файле по идентификатору")
    @DatabaseSetup("/controller/mds-files/before/mds-files-db.xml")
    void getMdsFile() throws Exception {
        mockMvc.perform(
            get("/back-office/mds-files/1")
                .param("shopId", "1")
                .param("userId", "1")
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/mds-files/get_mds_file_response.json"));
    }

    @Test
    @DisplayName("Получить информацию о несуществующем MDS файле")
    void noMdsFileWithSuchId() throws Exception {
        mockMvc.perform(
            get("/back-office/mds-files/1")
                .param("shopId", "1")
                .param("userId", "1")
        )
            .andExpect(status().isNotFound());
    }
}
