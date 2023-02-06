package ru.yandex.market.logistics.management.controller.admin;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PLATFORM_CLIENT;

@CleanDatabase
@Sql("/data/controller/admin/platformClient/prepare_data.sql")
class LmsControllerPlatformClientTest extends AbstractContextualTest {

    @Test
    void platformClientGridUnauthorized() throws Exception {
        getPlatformClientGrid()
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void platformClientGridForbidden() throws Exception {
        getPlatformClientGrid()
            .andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PLATFORM_CLIENT)
    void platformClientGrid() throws Exception {
        getPlatformClientGrid()
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/platformClient/platform_client_grid.json",
                false
            ));
    }

    @Test
    void platformClientUnauthorized() throws Exception {
        getPlatformClient(1)
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void platformClientForbidden() throws Exception {
        getPlatformClient(1)
            .andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PLATFORM_CLIENT)
    void platformClientDetail() throws Exception {
        getPlatformClient(1)
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/platformClient/platform_client_detail.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PLATFORM_CLIENT)
    void platformClientDetailMissing() throws Exception {
        getPlatformClient(0)
            .andExpect(status().isNotFound());
    }

    private ResultActions getPlatformClientGrid() throws Exception {
        return mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/lms/platform-client")
        );
    }

    private ResultActions getPlatformClient(long id) throws Exception {
        return mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/lms/platform-client/" + id)
        );
    }

}
