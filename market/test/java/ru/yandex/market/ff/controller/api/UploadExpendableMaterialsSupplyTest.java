package ru.yandex.market.ff.controller.api;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.util.FileContentUtils;
import ru.yandex.market.ff.util.query.count.JpaQueriesCount;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Функциональный тест для ручки: /upload-request/expendable-materials-supply
 */
@DatabaseSetup("classpath:controller/upload-request/before.xml")
class UploadExpendableMaterialsSupplyTest extends MvcIntegrationTest {


    @Test
    @JpaQueriesCount(15)
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-expendable-material-supply.xml",
            assertionMode = NON_STRICT
    )
    void submitShadowSupplyWithCalendaringModeAsRequired() throws Exception {
        final String data = getJsonFromFile("valid-expendable-materials-supply.json");
        final MvcResult mvcResult = doPostShadowSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-expendable-materials-supply.json", mvcResult);
    }

    @Test
    @JpaQueriesCount(0)
    void submitInvalidShadowSupplyWithUnsupportedCalendaringMode() throws Exception {
        final String data =
                getJsonFromFile("invalid-expendable-materials-supply-with-unsupported-calendaring-mode.json");
        final MvcResult mvcResult = doPostShadowSupplyNoFile(data)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Only REQUIRED calendaring mode is applicable\"}"));
    }

    private String getJsonFromFile(final String name) throws IOException {
        return FileContentUtils.getFileContent("controller/upload-request/" + name);
    }

    private ResultActions doPostShadowSupplyNoFile(final String data) throws Exception {
        return mockMvc.perform(
                post("/upload-request/expendable-materials-supply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(data)
        ).andDo(print());
    }

    private void assertJsonResponseCorrect(String filename, MvcResult response) throws IOException {
        JSONAssert.assertEquals(getJsonFromFile(filename), response.getResponse().getContentAsString(),
                JSONCompareMode.NON_EXTENSIBLE);
    }
}
