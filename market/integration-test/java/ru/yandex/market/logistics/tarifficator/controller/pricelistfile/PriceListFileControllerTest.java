package ru.yandex.market.logistics.tarifficator.controller.pricelistfile;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Интеграционный тест контроллера PriceListFileController")
class PriceListFileControllerTest extends AbstractContextualTest {

    @Test
    @DisplayName("Получить загруженные файлы прайс-листов тарифа")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-files.xml")
    void getFiles() throws Exception {
        mockMvc.perform(get("/price-list/files/tariff/1"))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/price-list-files/response/get_files_response.json"));
    }

    @Test
    @DisplayName("Получить загруженные файлы для несуществующего тарифа")
    void getFilesTariffNotFound() throws Exception {
        mockMvc.perform(get("/price-list/files/tariff/1"))
            .andExpect(status().isNotFound())
            .andExpect(TestUtils.jsonContent("controller/price-list-files/response/tariff_not_found_response.json"));
    }

    @Test
    @DisplayName("Получить загруженный файл по идентификатору")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-files.xml")
    void getFile() throws Exception {
        mockMvc.perform(get("/price-list/files/1"))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/price-list-files/response/file_response.json"));
    }

    @Test
    @DisplayName("Получить несуществующий файл по идентификатору")
    void getFileNotFound() throws Exception {
        mockMvc.perform(get("/price-list/files/1"))
            .andExpect(status().isNotFound())
            .andExpect(TestUtils.jsonContent("controller/price-list-files/response/file_not_found_response.json"));
    }
}
