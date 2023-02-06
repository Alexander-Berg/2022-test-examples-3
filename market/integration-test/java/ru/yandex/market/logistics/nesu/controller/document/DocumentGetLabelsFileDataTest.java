package ru.yandex.market.logistics.nesu.controller.document;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение файла с ярлыками")
@DatabaseSetup("/controller/document/before/get_label_file_url_setup.xml")
class DocumentGetLabelsFileDataTest extends AbstractContextualTest {
    @Test
    @DisplayName("Получение URL для загрузки файла с ярлыками")
    void getLabelsFileData() throws Exception {
        getLabelsFileData(1L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/document/labels_file_url.json"));
    }

    @Test
    @DisplayName("Задание на печать не найдено")
    void getLabelsFileDataNotFound() throws Exception {
        getLabelsFileData(2L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [LABEL_FILE_GENERATION_TASK] with ids [2]"));
    }

    @Nonnull
    private ResultActions getLabelsFileData(long fileRequestId) throws Exception {
        return mockMvc.perform(
            get("/back-office/documents/labels/download")
                .param("fileRequestId", String.valueOf(fileRequestId))
        );
    }
}
