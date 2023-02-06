package ru.yandex.market.logistics.management.controller.admin.korobyteRestriction;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.KOROBYTE_RESTRICTION_ID_1;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.KOROBYTE_RESTRICTION_ID_2;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.READ_ONLY;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.delete;

@DatabaseSetup("/data/controller/admin/korobyteRestrictions/before/setup.xml")
class AdminKorobyteRestrictionControllerDeleteTest extends AbstractContextualAspectValidationTest {

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_ONLY)
    void shouldGetForbidden_whenHasReadOnlyAuthority() throws Exception {
        performDelete(KOROBYTE_RESTRICTION_ID_2)
            .andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    @ExpectedDatabase(
        value = "/data/controller/admin/korobyteRestrictions/after/delete.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void shouldDelete() throws Exception {
        performDelete(KOROBYTE_RESTRICTION_ID_1)
            .andExpect(status().isOk());
    }

    @Nonnull
    private ResultActions performDelete(long korobyteRestrictionId) throws Exception {
        return mockMvc.perform(delete(korobyteRestrictionId));
    }
}
