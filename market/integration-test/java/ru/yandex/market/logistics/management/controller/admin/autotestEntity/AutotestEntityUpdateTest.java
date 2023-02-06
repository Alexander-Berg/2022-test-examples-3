package ru.yandex.market.logistics.management.controller.admin.autotestEntity;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.domain.dto.front.autotestEntity.AutotestEntityUpdateDto;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.pojoToString;

@DatabaseSetup("/data/controller/admin/autotestEntity/before/setup.xml")
public class AutotestEntityUpdateTest extends AbstractContextualAspectValidationTest {

    @Test
    @DisplayName("Обновить информацию об автотестовой сущности, будучи неавторизованным")
    void updateAutotestEntityIsUnauthorized() throws Exception {
        performUpdateAutotestEntity(defaultUpdateDto()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Обновить информацию об автотестовой сущности, не имея прав")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void updateAutotestEntityIsForbidden() throws Exception {
        performUpdateAutotestEntity(defaultUpdateDto()).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Обновить информацию об автотестовой сущности, имея права только на чтение")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_AUTOTEST_ENTITY})
    void updateAutotestEntityReadOnly() throws Exception {
        performUpdateAutotestEntity(defaultUpdateDto()).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Обновить информацию об автотестовой сущности — без изменений")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_AUTOTEST_ENTITY_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/autotestEntity/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateAutotestEntityNoChanges() throws Exception {
        performUpdateAutotestEntity(defaultUpdateDto().setPath("lms/partner/1")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Обновить информацию об автотестовой сущности — изменение пути")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_AUTOTEST_ENTITY_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/autotestEntity/after/update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateAutotestEntityChangePath() throws Exception {
        performUpdateAutotestEntity(defaultUpdateDto()).andExpect(status().isOk());
    }

    @Nonnull
    private ResultActions performUpdateAutotestEntity(AutotestEntityUpdateDto updateDto) throws Exception {
        return mockMvc.perform(put("/admin/lms/autotest-entity/1")
            .content(pojoToString(updateDto))
            .contentType(MediaType.APPLICATION_JSON)
        );
    }

    @Nonnull
    private AutotestEntityUpdateDto defaultUpdateDto() {
        return new AutotestEntityUpdateDto()
            .setPath("lms/partner/10");
    }
}
