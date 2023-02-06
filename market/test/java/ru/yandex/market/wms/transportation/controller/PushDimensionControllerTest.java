package ru.yandex.market.wms.transportation.controller;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class PushDimensionControllerTest extends IntegrationTest {

    @Test
    @ExpectedDatabase(value = "/controller/push-dimension/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testPushDimensionOk() throws Exception {
        mockMvc.perform(post("/push-dimensions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/push-dimension/request.json")))
                .andExpect(status().is2xxSuccessful());
    }
}
