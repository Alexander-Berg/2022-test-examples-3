package ru.yandex.market.logistics.logistrator.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.logistrator.AbstractContextualTest
import ru.yandex.market.logistics.logistrator.utils.TOP_CUBIC_BIG_FLOPPA_DOMAIN_LOGIN
import ru.yandex.market.logistics.logistrator.utils.TOP_CUBIC_BIG_FLOPPA_PASSPORT_LOGIN
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

@DisplayName("Работа с ролями пользователей из IDM")
internal class UserRoleControllerTest : AbstractContextualTest() {

    @Test
    @DisplayName("Получение информации о дереве ролей пользователей для IDM")
    fun getInfo() {
        mockMvc.perform(MockMvcRequestBuilders.get("/user-roles/info"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                JsonUnitResultMatchers.json()
                    .isEqualTo(IntegrationTestUtils.extractFileContent("response/user_role/get_info_response.json"))
            )
    }

    @Test
    @DatabaseSetup("/db/user_role/no_user_roles.xml")
    @ExpectedDatabase("/db/user_role/user_role_admin.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @DisplayName("Добавление пользователю новой роли из IDM")
    fun addRole() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/user-roles/add-role")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(String.format(
                    "login=%s&path=/ADMIN/&fields={\"passport-login\":\"%s\"}",
                    TOP_CUBIC_BIG_FLOPPA_DOMAIN_LOGIN,
                    TOP_CUBIC_BIG_FLOPPA_PASSPORT_LOGIN
                ))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                JsonUnitResultMatchers.json()
                    .isEqualTo(IntegrationTestUtils.extractFileContent("response/user_role/ok_response.json"))
            )
    }

    @Test
    @DatabaseSetup("/db/user_role/user_role_viewer.xml")
    @ExpectedDatabase("/db/user_role/no_user_roles.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @DisplayName("Удаление роли пользователя из IDM")
    fun removeRole() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/user-roles/remove-role")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(String.format(
                    "login=%s&path=/VIEWER/&fields={\"passport-login\":\"%s\"}",
                    TOP_CUBIC_BIG_FLOPPA_DOMAIN_LOGIN,
                    TOP_CUBIC_BIG_FLOPPA_PASSPORT_LOGIN
                ))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                JsonUnitResultMatchers.json()
                    .isEqualTo(IntegrationTestUtils.extractFileContent("response/user_role/ok_response_remove.json"))
            )
    }

    @Test
    @DatabaseSetup("/db/user_role/user_role_admin.xml")
    @DisplayName("Получение всех ролей всех пользователей для IDM")
    fun getAllRoles() {
        mockMvc.perform(MockMvcRequestBuilders.get("/user-roles/get-all-roles"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                JsonUnitResultMatchers.json().isEqualTo(
                    IntegrationTestUtils.extractFileContent("response/user_role/get_all_roles_response.json")
                )
            )
    }

    @Test
    @DatabaseSetup("/db/user_role/user_role_admin.xml")
    @DisplayName("Получение списка прав пользователя")
    fun getAuthorities() {
        mockMvc.perform(MockMvcRequestBuilders.get("/user-roles/authorities?login=$TOP_CUBIC_BIG_FLOPPA_PASSPORT_LOGIN"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                JsonUnitResultMatchers.json().isEqualTo(
                    IntegrationTestUtils.extractFileContent("response/user_role/get_authorities_response.json")
                )
            )
    }

    @Test
    @DatabaseSetup("/db/user_role/user_role_admin.xml")
    @DisplayName("Неудачное добавление пользователю новой роли из IDM из-за существования такой роли ранее")
    fun addRoleErrorBecauseOfUniqueConstraintViolation() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/user-roles/add-role")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(String.format(
                    "login=%s&path=/ADMIN/&fields={\"passport-login\":\"%s\"}",
                    TOP_CUBIC_BIG_FLOPPA_DOMAIN_LOGIN,
                    TOP_CUBIC_BIG_FLOPPA_PASSPORT_LOGIN
                ))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                JsonUnitResultMatchers.json()
                    .isEqualTo(IntegrationTestUtils.extractFileContent("response/user_role/error_response.json"))
            )
    }
}
