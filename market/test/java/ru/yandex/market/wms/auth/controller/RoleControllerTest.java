package ru.yandex.market.wms.auth.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class RoleControllerTest extends AuthIntegrationTest {

    @Test
    @DatabaseSetup(value = "/db/dao/role/before.xml", connection = "authConnection")
    @ExpectedDatabase(
            value = "/db/dao/role/update-role-after.xml",
            connection = "authConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void updateRoleScreensTest() throws Exception {
        mockMvc.perform(
                put("/roles/ADMINISTRATOR")
                        .content(getFileContent("controller/role/update-role-request.json"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void updateRoleScreensWithBadRequest() throws Exception {
        mockMvc.perform(
                        put("/roles/ADMINISTRATOR")
                                .content(getFileContent("controller/role/update-role-bad-request.json"))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent("controller/role/update-role-bad-response.json")));
    }

}
