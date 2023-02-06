package ru.yandex.market.logistics.management.controller.admin.autotestEntity;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("/data/controller/admin/autotestEntity/before/setup.xml")
public class AutotestEntityDeleteTest extends AbstractContextualAspectValidationTest {

    @Test
    @DisplayName("Удалить автотестовую сущность, будучи неавторизованным")
    void deleteAutotestEntityIsUnauthorized() throws Exception {
        performDeleteAutotestEntity(1L).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Удалить автотестовую сущность, не имея прав")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void deleteAutotestEntityIsForbidden() throws Exception {
        performDeleteAutotestEntity(1L).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Удалить автотестовую сущность, имея права только на чтение")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOCATION_ZONE})
    void deleteAutotestEntityReadOnly() throws Exception {
        performDeleteAutotestEntity(1L).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Удалить автотестовую сущность")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_AUTOTEST_ENTITY_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/autotestEntity/after/delete.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteAutotestEntityChangePath() throws Exception {
        performDeleteAutotestEntity(1L).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Удалить несуществующую автотестовую сущность")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_AUTOTEST_ENTITY_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/autotestEntity/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteAutotestEntityNotFound() throws Exception {
        performDeleteAutotestEntity(3L)
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find autotest entity with id=3"));
    }

    @Nonnull
    private ResultActions performDeleteAutotestEntity(long entityId) throws Exception {
        return mockMvc.perform(delete("/admin/lms/autotest-entity/" + entityId));
    }
}
