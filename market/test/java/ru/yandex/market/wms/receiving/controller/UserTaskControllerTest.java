package ru.yandex.market.wms.receiving.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;


class UserTaskControllerTest extends ReceivingIntegrationTest {

    @Test
    @DatabaseSetup("/controller/user-task/permissions/ok/db.xml")
    @ExpectedDatabase(value = "/controller/user-task/permissions/ok/db.xml", assertionMode = NON_STRICT_UNORDERED)
    void getUserTaskPermissions() throws Exception {
        assertApiCallOk(null, "controller/user-task/permissions/ok/response.json", get("/usertasks/permissions"));
    }

    @Test
    @DatabaseSetup("/controller/user-task/permissions/replenishment/db.xml")
    @ExpectedDatabase(value = "/controller/user-task/permissions/replenishment/db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getReplenishmentTaskPermissions() throws Exception {
        assertApiCallOk(null, "controller/user-task/permissions/replenishment/response.json",
                get("/usertasks/permissions"));
    }

    private void assertApiCallOk(String requestFile, String responseFile,
                                 MockHttpServletRequestBuilder request) throws Exception {
        if (requestFile != null) {
            request.contentType(MediaType.APPLICATION_JSON)
                    .content(getFileContent(requestFile));
        }
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(responseFile), false));
    }
}
