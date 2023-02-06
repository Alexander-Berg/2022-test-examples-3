package ru.yandex.market.replenishment.autoorder.api;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mboc.idm.IdmStatusResult;
import ru.yandex.market.mboc.idm.IdmUser;
import ru.yandex.market.mboc.idm.IdmUsersRoles;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.config.security.UserRoles;
import ru.yandex.market.replenishment.autoorder.security.WithMockTvm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тесты на {@link IdmController}.
 */
@WithMockTvm
public class IdmControllerTest extends ControllerTest {

    @Test
    @DbUnitDataSet(after = "IdmControllerTest.addRole.after.csv")
    public void addRoleWillAddRole() throws Exception {
        var mvcResult = mockMvc.perform(post("/idm/add-role/")
                .param("login", "pusya")
                .param("role", "{\"group\":\"" + UserRoles.ROLE_USER + "\"}"))
            .andExpect(status().isOk())
            .andReturn();

        var status = readJson(mvcResult, IdmStatusResult.class);
        Assertions.assertThat(status)
            .usingRecursiveComparison()
            .isEqualTo(IdmStatusResult.ok());
    }

    @Test
    @DbUnitDataSet(after = "IdmControllerTest.addRole.after.csv")
    public void doubleAddRoleWillAddRoleOnlyOnce() throws Exception {
        var mvcResult = mockMvc.perform(post("/idm/add-role/")
                .param("login", "pusya")
                .param("role", "{\"group\":\"" + UserRoles.ROLE_USER + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
        var status1 = readJson(mvcResult, IdmStatusResult.class);
        Assertions.assertThat(status1)
            .usingRecursiveComparison()
            .isEqualTo(IdmStatusResult.ok());

        var mvcResult2 = mockMvc.perform(post("/idm/add-role/")
                .param("login", "pusya")
                .param("role", "{\"group\":\"" + UserRoles.ROLE_USER + "\"}"))
            .andExpect(status().isOk())
            .andReturn();

        var status2 = readJson(mvcResult2, IdmStatusResult.class);
        Assertions.assertThat(status2)
            .usingRecursiveComparison()
            .isEqualTo(IdmStatusResult.ok());
    }

    @Test
    @DbUnitDataSet(after = "IdmControllerTest.emptyRoles.after.csv")
    public void addIllegalRoleWillFail() throws Exception {
        var mvcResult = mockMvc.perform(post("/idm/add-role/")
                .param("login", "pusya")
                .param("role", "{\"group\":\"" + "ILLEGAL_ROLE" + "\"}"))
            .andExpect(status().isOk())
            .andReturn();

        var status = readJson(mvcResult, IdmStatusResult.class);
        Assertions.assertThat(status)
            .usingRecursiveComparison()
            .isEqualTo(IdmStatusResult.error("Unknown role ILLEGAL_ROLE"));
    }

    @Test
    @DbUnitDataSet(
        before = "IdmControllerTest.roles.before.csv",
        after = "IdmControllerTest.removeRoles.after.csv"
    )
    public void removeRoles() throws Exception {
        var mvcResult = mockMvc.perform(post("/idm/remove-role/")
                .param("login", "pusya")
                .param("role", "{\"group\":\"" + UserRoles.ROLE_USER + "\"}"))
            .andExpect(status().isOk())
            .andReturn();

        var status = readJson(mvcResult, IdmStatusResult.class);
        Assertions.assertThat(status)
            .usingRecursiveComparison()
            .isEqualTo(IdmStatusResult.ok());
    }

    @Test
    @DbUnitDataSet(after = "IdmControllerTest.emptyRoles.after.csv")
    public void removeRoleWontFailIfNoRoleExists() throws Exception {
        var mvcResult = mockMvc.perform(post("/idm/remove-role/")
                .param("login", "pusya")
                .param("role", "{\"group\":\"" + UserRoles.ROLE_USER + "\"}"))
            .andExpect(status().isOk())
            .andReturn();

        var status = readJson(mvcResult, IdmStatusResult.class);
        Assertions.assertThat(status)
            .usingRecursiveComparison()
            .isEqualTo(IdmStatusResult.ok());
    }

    @Test
    @DbUnitDataSet(before = "IdmControllerTest.roles.before.csv")
    public void getAllRoles() throws Exception {
        var mvcResult = mockMvc.perform(get("/idm/get-all-roles/"))
            .andExpect(status().isOk())
            .andReturn();
        var roles = readJson(mvcResult, IdmUsersRoles.class);

        var userToRoles = roles.getUsers().stream()
            .collect(Collectors.toMap(IdmUser::getLogin, v -> v.getRolesBySlug(IdmController.SLUG)));

        Assertions.assertThat(userToRoles)
            .containsExactlyInAnyOrderEntriesOf(Map.of(
                "pusya", Set.of(UserRoles.ROLE_USER),
                "vanya", Set.of(UserRoles.ROLE_USER, UserRoles.ROLE_TMS_UI_ADMIN)
            ));
    }
}
