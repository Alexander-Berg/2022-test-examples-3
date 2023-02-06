package ru.yandex.market.logistics.management.controller.admin;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.controller.MediaTypes;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Контроллер партнёров")
@DatabaseSetup("/data/controller/admin/partner/prepare_data.xml")
public class AdminPartnerControllerTest extends AbstractContextualAspectValidationTest {

    private static final String PARTNER_URL = "/admin/lms/partner";

    @Test
    @DisplayName("Получить всех партнеров - Неавторизованный пользователь")
    void testGetPartners_unauthorized() throws Exception {
        mockMvc.perform(get(PARTNER_URL))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Получить всех партнеров - Недостаточно прав")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void testGetPartners_forbidden() throws Exception {
        mockMvc.perform(get(PARTNER_URL))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получить всех партнеров - Успешно")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER})
    void testGetPartners_success() throws Exception {
        mockMvc.perform(get(PARTNER_URL))
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/partner/get_partners_output.json"
            ));
    }

    @Test
    @DisplayName("Получить партнера по ID")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER})
    void testFindPartnerById_success() throws Exception {
        mockMvc.perform(get(PARTNER_URL + "/1"))
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/partner/get_by_id_output.json"
            ));
    }

    @Test
    @DisplayName("Форма для создания нового партнера")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_EDIT})
    void testCreateNewPartnerForm_success() throws Exception {
        mockMvc.perform(get(PARTNER_URL + "/new"))
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/partner/form_for_creating_new_partner.json"
            ));
    }

    @Test
    @DisplayName("Скачать CSV файл со всеми партнерами")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER})
    void testDownloadCSV_success() throws Exception {
        mockMvc.perform(get(PARTNER_URL + "/download/all"))
            .andExpect(status().isOk())
            .andExpect(header().string(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment;filename=partners.csv"
            ))
            .andExpect(content().contentType(MediaTypes.TEXT_CSV_UTF8))
            .andExpect(content().string(
                extractFileContent("data/controller/admin/partner/csv/download.csv")
            ));
    }
}
