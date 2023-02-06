package ru.yandex.market.logistics.management.controller.admin.korobyteRestriction;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.KOROBYTE_RESTRICTION_ID_3;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.NON_EXISTS_KOROBYTE_RESTRICTION_ID;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.READ_ONLY;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.getDetail;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@DatabaseSetup("/data/controller/admin/korobyteRestrictions/before/setup.xml")
class AdminKorobyteRestrictionControllerGetDetailTest extends AbstractContextualAspectValidationTest {

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_ONLY)
    void shouldGetNotFound() throws Exception {
        performGetDetail(NON_EXISTS_KOROBYTE_RESTRICTION_ID)
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find korobyte restriction with id=9999"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_ONLY)
    void shouldGetDetailWithViewMode() throws Exception {
        performGetDetail(KOROBYTE_RESTRICTION_ID_3)
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/korobyteRestrictions/response/detail_3.json"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    void shouldGetDetailWithEditMode() throws Exception {
        performGetDetail(KOROBYTE_RESTRICTION_ID_3)
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/korobyteRestrictions/response/detail_3_edit.json"));
    }

    @Nonnull
    private ResultActions performGetDetail(long korobyteRestrictionId) throws Exception {
        return mockMvc.perform(getDetail(korobyteRestrictionId));
    }
}
