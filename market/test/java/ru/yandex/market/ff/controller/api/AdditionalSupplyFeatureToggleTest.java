package ru.yandex.market.ff.controller.api;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.configuration.FeatureToggleTestConfiguration;
import ru.yandex.market.ff.util.FileContentUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@DatabaseSetup("classpath:controller/additional-supply-ft/before.xml")
public class AdditionalSupplyFeatureToggleTest extends MvcIntegrationTest {

    @Autowired
    private FeatureToggleTestConfiguration.FTConfig ftConfig;

    @Test
    @ExpectedDatabase(
            value = "classpath:controller/additional-supply-ft/after-create-additional-supply-all-enabled.xml",
            assertionMode = NON_STRICT)
    void submitSupplyAndCheckAdditionalSupplyAllEnabled() throws Exception {
        ftConfig.setSupplyEnabled(true);
        String data = getJsonFromFile("valid-supply.json");
        MvcResult mvcResult = doPostSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response.json", mvcResult);

        data = getJsonFromFile("valid-supply-2.json");
        mvcResult = doPostSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-2.json", mvcResult);
    }

    @Test
    @ExpectedDatabase(
            value = "classpath:controller/additional-supply-ft/after-create-additional-supply-all-disabled.xml",
            assertionMode = NON_STRICT)
    void submitSupplyAndCheckAdditionalSupplyAllDisabled() throws Exception {
        ftConfig.setSupplyEnabled(false);
        String data = getJsonFromFile("valid-supply.json");
        MvcResult mvcResult = doPostSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response.json", mvcResult);

        data = getJsonFromFile("valid-supply-2.json");
        mvcResult = doPostSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-2.json", mvcResult);
    }

    @Test
    @ExpectedDatabase(
            value = "classpath:controller/additional-supply-ft/after-create-additional-supply-part-enabled.xml",
            assertionMode = NON_STRICT)
    void submitSupplyAndCheckAdditionalSupplyPartEnabled() throws Exception {
        ftConfig.setSupplyEnabled(true);
        String data = getJsonFromFile("valid-supply.json");
        MvcResult mvcResult = doPostSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response.json", mvcResult);

        ftConfig.setSupplyEnabled(false);
        data = getJsonFromFile("valid-supply-2.json");
        mvcResult = doPostSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-2.json", mvcResult);
    }

    @Test
    @ExpectedDatabase(
            value = "classpath:controller/additional-supply-ft/after-create-additional-supply-part-enabled.xml",
            assertionMode = NON_STRICT)
    void createSupplyAndGetRequestByIdHasField() throws Exception {
        ftConfig.setSupplyEnabled(true);
        String data = getJsonFromFile("valid-supply.json");
        MvcResult mvcResult = doPostSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response.json", mvcResult);

        ftConfig.setSupplyEnabled(false);
        data = getJsonFromFile("valid-supply-2.json");
        mvcResult = doPostSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-2.json", mvcResult);

        ftConfig.setSupplyEnabled(true);
        MvcResult result = mockMvc.perform(
                get("/suppliers/1/requests/1")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("get-request-by-id-response.json", result);
    }

    private ResultActions doPostSupplyNoFile(final String data) throws Exception {
        return mockMvc.perform(
                post("/upload-request/shadow-supply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(data)
        ).andDo(print());
    }

    private void assertJsonResponseCorrect(String filename, MvcResult response) throws IOException {
        JSONAssert.assertEquals(getJsonFromFile(filename), response.getResponse().getContentAsString(),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    private String getJsonFromFile(final String name) throws IOException {
        return FileContentUtils.getFileContent("controller/additional-supply-ft/" + name);
    }

}
