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
 * Функциональный тест для ручки: /upload-request/shadow-supply
 */
class UploadShadowSupplyTest extends MvcIntegrationTest {


    @Test
    @DatabaseSetup("classpath:controller/upload-request/before.xml")
    @JpaQueriesCount(15)
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-shadow-supply-with-calendaring-mode-as-required" +
                    ".xml",
            assertionMode = NON_STRICT
    )
    void submitShadowSupplyWithCalendaringModeAsRequired() throws Exception {
        final String data = getJsonFromFile("valid-shadow-supply-with-calendaring-mode-as-required.json");
        final MvcResult mvcResult = doPostShadowSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-shadow-supply-with-calendaring-mode.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:controller/upload-request/before.xml")
    @JpaQueriesCount(15)
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-shadow-supply-with-calendaring-mode-as-required" +
                    ".xml",
            assertionMode = NON_STRICT
    )
    void submitShadowSupplyWithDefaultCalendaringMode() throws Exception {
        final String data = getJsonFromFile("valid-shadow-supply-without-calendaring-mode.json");
        final MvcResult mvcResult = doPostShadowSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-shadow-supply-with-calendaring-mode.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:controller/upload-request/before.xml")
    @JpaQueriesCount(15)
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-shadow-supply-with-confirmation-flag-as-true.xml",
            assertionMode = NON_STRICT
    )
    void submitShadowSupplyWithNeedConfirmationAsTrue() throws Exception {
        final String data = getJsonFromFile("valid-shadow-supply-with-confirmation-flag-as-true.json");
        final MvcResult mvcResult = doPostShadowSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-shadow-supply-with-confirmation-flag-as-true.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:controller/upload-request/before.xml")
    @JpaQueriesCount(15)
    @ExpectedDatabase(
        value = "classpath:controller/upload-request/after-create-shadow-supply-with-e-document-type.xml",
        assertionMode = NON_STRICT
    )
    void submitShadowSupplyWithElectronicDocumentType() throws Exception {
        final String data = getJsonFromFile("valid-shadow-supply-with-e-document-type.json");
        final MvcResult mvcResult = doPostShadowSupplyNoFile(data)
            .andExpect(status().isOk())
            .andReturn();
        assertJsonResponseCorrect(
            "created-response-for-shadow-supply-with-e-document-type.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:controller/upload-request/before-shadow-supply-for-third-party.xml")
    @JpaQueriesCount(15)
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-third-party-shadow-supply-without-date.xml",
            assertionMode = NON_STRICT
    )
    void submitThirdPartyShadowSupplyWithoutRequestedDate() throws Exception {
        final String data = getJsonFromFile("valid-third-party-shadow-supply-without-date.json");
        final MvcResult mvcResult = doPostShadowSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-third-party-shadow-supply-without-date.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:controller/upload-request/before-create-first-party-x-doc-shadow-supply.xml")
    @JpaQueriesCount(17)
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-first-party-x-doc-shadow-supply.xml",
            assertionMode = NON_STRICT
    )
    void submitFirstPartyXDocShadowSupply() throws Exception {
        final String data = getJsonFromFile("valid-first-party-x-doc-shadow-supply.json");
        final MvcResult mvcResult = doPostShadowSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-for-first-party-x-doc-shadow-supply.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:controller/upload-request/before.xml")
    @JpaQueriesCount(4)
    void submitInvalidFirstPartyShadowSupplyWithoutRequestedDate() throws Exception {
        final String data = getJsonFromFile("invalid-first-party-shadow-supply-without-date.json");
        final MvcResult mvcResult = doPostShadowSupplyNoFile(data)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Requested date is not set but required\"," +
                        "\"type\":\"SUPPLY_DATE_IS_NOT_SET\"}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/upload-request/before.xml")
    @JpaQueriesCount(0)
    void submitInvalidShadowSupplyWithUnsupportedCalendaringMode() throws Exception {
        final String data = getJsonFromFile("invalid-shadow-supply-with-unsupported-calendaring-mode.json");
        final MvcResult mvcResult = doPostShadowSupplyNoFile(data)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Only REQUIRED calendaring mode is applicable\"}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/upload-request/before.xml")
    @JpaQueriesCount(15)
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-shadow-supply-import.xml",
            assertionMode = NON_STRICT
    )
    void submitShadowSupplyImportSuccessfully() throws Exception {
        final String data = getJsonFromFile("create-shadow-supply-import.json");
        final MvcResult mvcResult = doPostShadowSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("create-response-for-shadow-supply-import.json", mvcResult);
    }

    private String getJsonFromFile(final String name) throws IOException {
        return FileContentUtils.getFileContent("controller/upload-request/" + name);
    }

    private ResultActions doPostShadowSupplyNoFile(final String data) throws Exception {
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
}
