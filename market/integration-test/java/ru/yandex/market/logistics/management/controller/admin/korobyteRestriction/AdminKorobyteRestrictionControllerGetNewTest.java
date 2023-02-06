package ru.yandex.market.logistics.management.controller.admin.korobyteRestriction;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.READ_ONLY;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.getNew;
import static ru.yandex.market.logistics.management.util.TestUtil.jsonContent;

class AdminKorobyteRestrictionControllerGetNewTest extends AbstractContextualAspectValidationTest {

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_ONLY)
    void shouldGetForbidden_whenHasReadOnlyAuthority() throws Exception {
        performGetNew()
            .andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    void shouldGetNew() throws Exception {
        performGetNew()
            .andExpect(status().isOk())
            .andExpect(jsonContent("data/controller/admin/korobyteRestrictions/response/new.json"));
    }

    @Nonnull
    private ResultActions performGetNew() throws Exception {
        return mockMvc.perform(getNew());
    }
}
