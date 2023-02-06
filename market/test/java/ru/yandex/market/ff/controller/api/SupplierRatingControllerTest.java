package ru.yandex.market.ff.controller.api;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.util.FileContentUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SupplierRatingControllerTest extends MvcIntegrationTest {

    @Test
    @DatabaseSetup("classpath:controller/supplier-rating/before-get-config.xml")
    @ExpectedDatabase(value = "classpath:controller/supplier-rating/before-get-config.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void getConfigWorksCorrectForNewbie() throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                get("/supplier-rating/config/10")
        ).andExpect(status().isOk())
                .andReturn();
        assertJsonResponseCorrect("get-config-newbie-response.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:controller/supplier-rating/before-get-config-not-newbie.xml")
    @ExpectedDatabase(value = "classpath:controller/supplier-rating/before-get-config-not-newbie.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void getConfigWorksCorrectForNotNewbie() throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                get("/supplier-rating/config/10")
        ).andExpect(status().isOk())
                .andReturn();
        assertJsonResponseCorrect("get-config-not-newbie-response.json", mvcResult);
    }

    private void assertJsonResponseCorrect(String filename, MvcResult response) throws IOException {
        JSONAssert.assertEquals(FileContentUtils.getFileContent("controller/supplier-rating/" + filename),
                response.getResponse().getContentAsString(),
                JSONCompareMode.NON_EXTENSIBLE);
    }
}
