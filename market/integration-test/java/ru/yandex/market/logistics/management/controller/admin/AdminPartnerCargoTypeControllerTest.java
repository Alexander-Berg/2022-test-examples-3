package ru.yandex.market.logistics.management.controller.admin;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@DisplayName("Контроллер партнёрских карго-типов")
@DatabaseSetup("/data/controller/admin/partnerCargoType/prepare_data.xml")
class AdminPartnerCargoTypeControllerTest extends AbstractContextualTest {
    static final String BLACKLIST_URL = "/admin/lms/" + LMSPlugin.SLUG_PARTNER_FORBIDDEN_CARGO_TYPE;

    @Test
    @DisplayName("Просмотр черного списка карго-типов связанных с партнёром")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_CARGO_TYPE)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCargoType/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testGetPartnerForbiddenCargoTypes_success() throws Exception {
        mockMvc.perform(get(BLACKLIST_URL).param("partnerId", "1"))
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/partnerCargoType/after/get_partner_forbidden_cargo_types.json"
            ));
    }

    @Test
    @DisplayName("Просмотр черного списка карго-типов связанных с не существующим партнёром")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_CARGO_TYPE)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCargoType/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testGetPartnerForbiddenCargoTypes_partnerNotFound() throws Exception {
        mockMvc.perform(get(BLACKLIST_URL).param("partnerId", "4000"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Просмотр запрещенных карго-типов связанных с партнёром")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_CARGO_TYPE_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCargoType/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testGetPartnerForbiddenCargoTypeNew_success() throws Exception {
        mockMvc.perform(get(BLACKLIST_URL + "/new").param("parentId", "1"))
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/partnerCargoType/after/get_partner_forbidden_cargo_type_new.json"
            ));
    }

    @Test
    @DisplayName("Просмотр запрещенных карго-типов связанных с не существующим партнёром")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_CARGO_TYPE_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCargoType/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testGetPartnerForbiddenCargoTypeNew_partnerNotFound() throws Exception {
        mockMvc.perform(get(BLACKLIST_URL + "/new").param("parentId", "4000"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Добавление карго-типа к партнёру")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_CARGO_TYPE_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCargoType/after/add_to_black_list.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testAddCargoTypeToBlacklist_success() throws Exception {
        mockMvc.perform(post(BLACKLIST_URL + "/add")
            .content("{\"partnerId\":1, \"cargoTypeNumber\": 101}")
            .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Добавление запрещенного карго-типа к несуществующему партнёру")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_CARGO_TYPE_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCargoType/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testAddCargoTypeToBlacklist_partnerNotFound() throws Exception {
        mockMvc.perform(post(BLACKLIST_URL + "/add")
            .content("{\"partnerId\":4000, \"cargoTypeNumber\": 101}")
            .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Добавление несуществующего карго-типа к черному списку партнёра")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_CARGO_TYPE_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCargoType/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testAddCargoTypeToBlacklist_cargoTypeNotFound() throws Exception {
        mockMvc.perform(post(BLACKLIST_URL + "/add")
            .content("{\"partnerId\":1, \"cargoTypeNumber\": 4000}")
            .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Удаление карго-типов из blacklist")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_CARGO_TYPE_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCargoType/after/delete_from_black_list.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testRemoveCargoTypesFromBlackList_success() throws Exception {
        mockMvc.perform(post(BLACKLIST_URL + "/remove")
            .param("parentId", "1")
            .content("{\"ids\":[3]}")
            .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());
    }
}
