package ru.yandex.market.delivery.transport_manager.controller.admin;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class AdminRegisterDownloadTest extends AbstractContextualTest {

    private static final String CONTROLLER = "/admin/register/download/register-file-csv";

    @DisplayName("Контроллер скачивания реестра из админки: контент")
    @DatabaseSetup("/repository/register/register.xml")
    @DatabaseSetup("/repository/register/register_dependencies.xml")
    @DatabaseSetup("/repository/register/related_transportation.xml")
    @Test
    void csvIsDownloaded() throws Exception {
        var params = new LinkedMultiValueMap<String, String>();
        params.add("id", "1");
        mockMvc.perform(MockMvcRequestBuilders.get(CONTROLLER).params(params))
            .andExpect(status().isOk())
            .andExpect(content().string(extractFileContent("controller/admin/register/register.csv")));
    }

    @DisplayName("Контроллер скачивания реестра из админки: несколько реестров")
    @DatabaseSetup("/repository/register/several_registers_with_dependencies.xml")
    @Test
    void allRegistersDownloaded() throws Exception {
        var params = new LinkedMultiValueMap<String, String>();
        params.add("id", "1");
        mockMvc.perform(MockMvcRequestBuilders.get(CONTROLLER).params(params))
            .andExpect(status().isOk())
            .andExpect(content().string(extractFileContent("controller/admin/register/multiple-registers.csv")))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=multiple-registers.csv"));
    }

    @DisplayName("Контроллер скачивания реестра из админки: выбранные реестры")
    @DatabaseSetup("/repository/register/several_registers_with_dependencies.xml")
    @Test
    void selectedRegistersDownloaded() throws Exception {
        var params = new LinkedMultiValueMap<String, String>();
        params.add("id", "1");
        params.add("ids", "3");
        mockMvc.perform(MockMvcRequestBuilders.get(CONTROLLER).params(params))
            .andExpect(status().isOk())
            .andExpect(content().string(extractFileContent("controller/admin/register/selected-registers.csv")))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=3-register.csv"));
    }

    @DisplayName("Контроллер скачивания реестра из админки: имя файла")
    @DatabaseSetup("/repository/register/single_register_with_deps.xml")
    @Test
    void csvNameIsValidSingleRegister() throws Exception {
        var params = new LinkedMultiValueMap<String, String>();
        params.add("id", "1");
        mockMvc.perform(MockMvcRequestBuilders.get(CONTROLLER).params(params))
            .andExpect(status().isOk())
            .andExpect(header().string(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=1_register1-register-2020-10-24.csv"
            ));
    }
}
