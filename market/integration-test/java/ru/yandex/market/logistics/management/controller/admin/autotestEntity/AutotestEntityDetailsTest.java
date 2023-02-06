package ru.yandex.market.logistics.management.controller.admin.autotestEntity;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.jsonContent;

@DatabaseSetup("/data/controller/admin/autotestEntity/before/setup.xml")
public class AutotestEntityDetailsTest extends AbstractContextualAspectValidationTest {
    private static final Long NON_EXIST_AUTOTEST_ENTITY_ID = 1001L;
    @Test
    @DisplayName("Получить информацию об автотестовой сущности будучи неавторизованным")
    void getAutotestEntityDetailInfoIsUnauthorized() throws Exception {
        performGetDetailInfo(1L).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Получить информацию об автотестовой сущности не имея прав")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void getAutotestEntityDetailInfoIsForbidden() throws Exception {
        performGetDetailInfo(1L).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получить информацию об автотестовой сущности")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_AUTOTEST_ENTITY})
    void getAutotestEntityDetailInfo() throws Exception {
        performGetDetailInfo(1L)
            .andExpect(jsonContent("data/controller/admin/autotestEntity/response/detail_id_1.json"));
    }

    @Test
    @DisplayName("Получить информацию о несуществующей автотестовой сущности")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_AUTOTEST_ENTITY})
    void getLocationZoneDetailInfoNotFound() throws Exception {
        performGetDetailInfo(NON_EXIST_AUTOTEST_ENTITY_ID)
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find autotest entity with id=1001"));
    }

    @Nonnull
    private ResultActions performGetDetailInfo(long autotestEntityId) throws Exception {
        return mockMvc.perform(get("/admin/lms/autotest-entity/" + autotestEntityId));
    }
}
