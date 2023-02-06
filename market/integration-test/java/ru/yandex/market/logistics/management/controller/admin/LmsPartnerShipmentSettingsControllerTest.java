package ru.yandex.market.logistics.management.controller.admin;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PARTNER_SHIPMENT_SETTINGS;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PARTNER_SHIPMENT_SETTINGS_EDIT;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@DatabaseSetup("/data/controller/admin/partnerShipmentSettings/before/prepare_data.xml")
@DisplayName("Интеграционный тест контроллера LmsPartnerShipmentSettingsController")
class LmsPartnerShipmentSettingsControllerTest extends AbstractContextualTest {
    private static final String PARTNER_SHIPMENT_SETTINGS_URL = "/admin/lms/partner-shipment-settings";

    @Test
    @DisplayName("Получить грид с настройками отгрузки")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PARTNER_SHIPMENT_SETTINGS)
    void getGrid() throws Exception {
        mockMvc.perform(get(PARTNER_SHIPMENT_SETTINGS_URL).param("platformClientPartnersId", "2"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/partnerShipmentSettings/response/grid.json"));
    }

    @Test
    @DisplayName("Получить пустой грид, если настроек отгрузки еще нет")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PARTNER_SHIPMENT_SETTINGS)
    void getGridNotFound() throws Exception {
        mockMvc.perform(get(PARTNER_SHIPMENT_SETTINGS_URL).param("platformClientPartnersId", "10"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/partnerShipmentSettings/response/empty_grid.json"));
    }

    @Test
    @DisplayName("Получить форму для создания новых настроек отгрузки")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PARTNER_SHIPMENT_SETTINGS_EDIT)
    void getNew() throws Exception {
        mockMvc.perform(get(PARTNER_SHIPMENT_SETTINGS_URL + "/new"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/partnerShipmentSettings/response/new.json"));
    }

    @Test
    @DisplayName("Создать новую настройку отгрузки")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PARTNER_SHIPMENT_SETTINGS_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerShipmentSettings/after/created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void create() throws Exception {
        mockMvc.perform(
            post(PARTNER_SHIPMENT_SETTINGS_URL)
                .param("parentSlug", "platform-client-partner")
                .param("idFieldName", "id")
                .param("parentId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/partnerShipmentSettings/request/create.json"))
        )
            .andExpect(status().isCreated())
            .andExpect(header().string("location", "http://localhost/admin/lms/platform-client-partner/1"));
    }

    @Test
    @DisplayName("Попытка создать новую настройку отгрузки для несуществующей связки партнера с платформой")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PARTNER_SHIPMENT_SETTINGS_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerShipmentSettings/before/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createForInvalidPlatformClientPartner() throws Exception {
        mockMvc.perform(
            post(PARTNER_SHIPMENT_SETTINGS_URL)
                .param("parentSlug", "UNKNOWN")
                .param("idFieldName", "UNKNOWN")
                .param("parentId", "100500")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/partnerShipmentSettings/request/create.json"))
        )
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find platform client with id=100500"));
    }

    @Test
    @DisplayName("Попытка создать невалидные настройки отгрузки")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PARTNER_SHIPMENT_SETTINGS_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerShipmentSettings/before/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createInvalidSettings() throws Exception {
        mockMvc.perform(
            post(PARTNER_SHIPMENT_SETTINGS_URL)
                .param("parentSlug", "platform-client-partner")
                .param("idFieldName", "id")
                .param("parentId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(testJson(
                "data/controller/admin/partnerShipmentSettings/response/create_invalid_settings.json",
                Option.IGNORING_EXTRA_FIELDS,
                Option.IGNORING_ARRAY_ORDER
            ));
    }

    @Test
    @DisplayName("Удаление настроек отгрузки")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PARTNER_SHIPMENT_SETTINGS_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerShipmentSettings/after/delete.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteSettings() throws Exception {
        mockMvc.perform(
            post(PARTNER_SHIPMENT_SETTINGS_URL + "/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/partnerShipmentSettings/request/delete.json"))
        )
            .andExpect(status().isOk());
    }
}
