package ru.yandex.market.ff.controller.api;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.util.FileContentUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class YardRejectionControllerTest extends MvcIntegrationTest {

    @Test
    @DatabaseSetup("classpath:controller/yard-rejection/before.xml")
    @ExpectedDatabase(value = "classpath:controller/yard-rejection/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void testProcessRejection() throws Exception {
        String request = FileContentUtils.getFileContent("controller/yard-rejection/request.json");
        mockMvc.perform(
                        post("/yard/rejection")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/yard-rejection/before.xml")
    @ExpectedDatabase(value = "classpath:controller/yard-rejection/deactivate/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testDeactivateRejection() throws Exception {
        String request = FileContentUtils.getFileContent("controller/yard-rejection/deactivate/request.json");
        mockMvc.perform(
                        post("/yard/rejection")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isOk());
    }
}
