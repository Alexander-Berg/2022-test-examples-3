package ru.yandex.market.ff.controller.api;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.configuration.FeatureToggleTestConfiguration;
import ru.yandex.market.ff.service.EnvironmentParamService;
import ru.yandex.market.ff.util.FileContentUtils;
import ru.yandex.market.ff.util.query.count.JpaQueriesCount;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Функциональный тест для {@link RequestUploadController}.
 *
 * @author avetokhin 13.06.18.
 */
@SuppressWarnings("AnnotationUseStyle")
@DatabaseSetup("classpath:controller/upload-request/before.xml")
class RequestUploadControllerTest extends MvcIntegrationTest {

    private static final String FILE_URL = "http://localhost:8080/file";

    @Autowired
    private EnvironmentParamService environmentParamService;

    @Autowired
    private FeatureToggleTestConfiguration.FTConfig ftConfig;

    @BeforeEach
    void init() throws MalformedURLException {
        doNothing().when(mdsS3Client).upload(any(), any());
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(FILE_URL));
        environmentParamService.clearCache();
    }

    //// ТЕСТЫ НА СОЗДАНИЕ БЕЗ ФАЙЛОВ
    @Test
    @JpaQueriesCount(15)
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-create-supply.xml", assertionMode = NON_STRICT)
    void submitSupplySuccessfully() throws Exception {
        final String data = getJsonFromFile("valid-supply.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
            .andExpect(status().isOk())
            .andReturn();

        assertJsonResponseCorrect("created-response.json", mvcResult);
    }

    @Test
    @JpaQueriesCount(31)
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-request/before-additional-supply-xlsx.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-additional-supply-with-cises.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void uploadAdditionalSupply() throws Exception {

        ftConfig.setSupplyEnabled(true);
        final String data = getJsonFromFile("valid-additional-supply.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-additional-supply-response.json", mvcResult);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-request/additional-supply-with-no-barcode/before.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-request/additional-supply-with-no-barcode/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void uploadAdditionalWithNoBarcodeSupply() throws Exception {

        ftConfig.setSupplyEnabled(true);
        final String data = getJsonFromFile("additional-supply-with-no-barcode/request.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("additional-supply-with-no-barcode/response.json", mvcResult);
    }


    @Test
    @JpaQueriesCount(23)
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-request/before-additional-supply-xlsx.xml")
    })
    void uploadAnomalyWithdraw() throws Exception {

        ftConfig.setSupplyEnabled(true);
        final String data = getJsonFromFile("valid-anomaly-withdraw.json");
        final MvcResult mvcResult = doPostAnomalyWithdrawNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-anomaly-withdraw-response.json", mvcResult);
    }

    @Test
    @JpaQueriesCount(15)
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-create-supply-without-vat-rate.xml",
            assertionMode = NON_STRICT)
    void submitSupplySuccessfullyWithoutVatRate() throws Exception {
        final String data = getJsonFromFile("valid-supply-without-vat-rate.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
            .andExpect(status().isOk())
            .andReturn();

        assertJsonResponseCorrect("created-response.json", mvcResult);
    }

    @Test
    @JpaQueriesCount(15)
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-create-supply-for-dbqueue-validation.xml",
        assertionMode = NON_STRICT)
    void submitSupplySuccessfullyForDbQueueValidation() throws Exception {
        final String data = getJsonFromFile("valid-supply.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
            .andExpect(status().isOk())
            .andReturn();

        assertJsonResponseCorrect("created-response.json", mvcResult);
    }

    @Test
    @JpaQueriesCount(15)
    @DatabaseSetup(value = "classpath:controller/upload-request/cutoff-split-turned-on.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-create-supply-without-shift.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void submitSupplySuccessfullyWithoutDateShift() throws Exception {
        final String data = getJsonFromFile("valid-supply-with-requested-date.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
            .andExpect(status().isOk())
            .andReturn();

        assertJsonResponseCorrect("created-response-without-shift.json", mvcResult);
    }

    @Test
    @JpaQueriesCount(15)
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-create-supply.xml", assertionMode = NON_STRICT)
    void submitSupplySuccessfullyWithSupplyType() throws Exception {
        final String data = getJsonFromFile("valid-supply-with-supply-type.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
            .andExpect(status().isOk())
            .andReturn();

        assertJsonResponseCorrect("created-response.json", mvcResult);
    }

    @Test
    @JpaQueriesCount(15)
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-create-supply-with-calendaring.xml",
        assertionMode = NON_STRICT)
    void submitSupplyWithCalendaringModeSuccessfully() throws Exception {
        final String data = getJsonFromFile("valid-supply-with-calendaring-mode.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
            .andExpect(status().isOk())
            .andReturn();

        assertJsonResponseCorrect("created-response-with-calendaring-mode.json", mvcResult);
    }

    @Test
    @JpaQueriesCount(15)
    void submitSupplyWithRequestCreatorSuccessfully() throws Exception {
        final String data = getJsonFromFile("valid-supply-with-request-creator.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
            .andExpect(status().isOk())
            .andReturn();

        assertJsonResponseCorrect("created-response-with-request-creator.json", mvcResult);
    }

    @Test
    @JpaQueriesCount(15)
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-create-supply-no-file-external-data.xml",
        assertionMode = NON_STRICT)
    void submitSupplyRequestNoFile() throws Exception {
        final String data = getJsonFromFile("valid-move.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
            .andExpect(status().isOk())
            .andReturn();

        assertJsonResponseCorrect("created-external-move.json", mvcResult);
    }

    @Test
    @JpaQueriesCount(15)
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-create-supply-no-file-external-data.xml",
        assertionMode = NON_STRICT)
    void submitSupplyRequestNoFileWithSupplyType() throws Exception {
        final String data = getJsonFromFile("valid-move-with-supply-type.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
            .andExpect(status().isOk())
            .andReturn();

        assertJsonResponseCorrect("created-external-move.json", mvcResult);
    }

    @Test
    @JpaQueriesCount(15)
    @DatabaseSetup("classpath:empty.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-xdoc-supply-no-file.xml",
        assertionMode = NON_STRICT)
    void submitXDocSupplyRequestNoFile() throws Exception {
        final String data = getJsonFromFile("valid-xdoc-supply.json");
        doPostSupplyNoFile(data)
            .andExpect(status().isOk())
            .andExpect(content().json(getJsonFromFile("xdoc-supply-response.json")));
    }

    @Test
    @DatabaseSetup("classpath:empty.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-new-xdoc-supply-no-file.xml",
            assertionMode = NON_STRICT)
    void submitNewXDocRequestNoFile() throws Exception {
        final String data = getJsonFromFile("valid-new-xdoc-supply.json");
        doPostSupplyNoFile(data)
                .andExpect(status().isOk())
                .andExpect(content().json(getJsonFromFile("new-xdoc-supply-response.json")));
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:empty.xml")
    })
    void submitCrossdockXDocSupplyRequestNoFile() throws Exception {
        final String data = getJsonFromFile("valid-xdoc-supply-with-crossdock-type.json");
        doPostSupplyNoFile(data)
            .andExpect(status().isInternalServerError());
    }

    @Test
    void submitSupplyInvalidData() throws Exception {
        final String data = getJsonFromFile("invalid-supply.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
            .andExpect(status().isBadRequest())
            .andReturn();

        assertJsonResponseCorrect("validation-errors.json", mvcResult);
    }

    @Test
    void submitCrossdockSupplyInvalidData() throws Exception {
        final String data = getJsonFromFile("invalid-crossdock-supply.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
            .andExpect(status().isBadRequest())
            .andReturn();

        assertJsonResponseCorrect("crossdock-validation-errors.json", mvcResult);
    }

    @Test
    void submitSupplyWithoutDate() throws Exception {
        doPostSupplyNoFile(getJsonFromFile("supply-without-date.json"))
            .andExpect(status().isBadRequest())
            .andExpect(content().json(getJsonFromFile("supply-without-date-response.json")));
    }

    @Test
    @DatabaseSetup("classpath:empty.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/before.xml", assertionMode = NON_STRICT)
    void submitXDocSupplyWithoutXDocDate() throws Exception {
        doPostSupplyNoFile(getJsonFromFile("xdoc-supply-without-date.json"))
            .andExpect(status().isBadRequest())
            .andExpect(content().json(getJsonFromFile("supply-without-x-doc-date-response.json")));
    }

    @Test
    void submitSupplyNoItems() throws Exception {
        final String data = getJsonFromFile("no-items-supply.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
            equalTo("{\"message\":\"items size must be between 1 and 10000\"}"));
    }

    @Test
    void submitCrossdockSupplyNoItems() throws Exception {
        final String data = getJsonFromFile("no-items-supply-with-crossdock-type.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
            equalTo("{\"message\":\"items size must be between 1 and 10000\"}"));
    }

    @Test
    void submitCrossdockSupplyDuplicatedSku() throws Exception {
        final String data = getJsonFromFile("duplicated-sku-supply.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
            equalTo("{\"message\":\"items all elements must have unique article\"}"));
    }

    @Test
    void submitSupplyDuplicatedSku() throws Exception {
        final String data = getJsonFromFile("duplicated-sku-supply.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
            equalTo("{\"message\":\"items all elements must have unique article\"}"));
    }

    @Test
    @JpaQueriesCount(15)
    @ExpectedDatabase(
        value = "classpath:controller/upload-request/after-create-supply-with-confirmation-flag-as-true.xml",
        assertionMode = NON_STRICT
    )
    void submitSupplyWithConfirmationFlag() throws Exception {
        final String data = getJsonFromFile("valid-supply-with-confirmation-flag-as-true.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
            .andExpect(status().isOk())
            .andReturn();

        assertJsonResponseCorrect("created-response-with-confirmation-flag.json", mvcResult);
    }

    @Test
    @JpaQueriesCount(15)
    @ExpectedDatabase(
        value = "classpath:controller/upload-request/after-create-supply-with-e-document-type.xml",
        assertionMode = NON_STRICT
    )
    void submitSupplyWithElectronicDocumentType() throws Exception {
        final String data = getJsonFromFile("valid-supply-with-e-document-type.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
            .andExpect(status().isOk())
            .andReturn();
        assertJsonResponseCorrect("created-response-with-e-document-type.json", mvcResult);
    }

    @Test
    void submitWithdrawWithConfirmationFlag() throws Exception {
        final String data = getJsonFromFile("withdraw-with-confirmation-flag-as-true.json");
        final String errorMsg = "{\"message\":\"Given request type doesn't support confirmation flow.\"," +
            "\"type\":\"UNSUPPORTED_REQUEST_TYPE_FOR_CONFIRMATION\"}";

        final MvcResult mvcResult = doPostSupplyNoFile(data)
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(), equalTo(errorMsg));
    }

    private ResultActions doPostSupplyNoFile(final String data) throws Exception {
        return mockMvc.perform(
            post("/upload-request/supply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(data)
        ).andDo(print());
    }
    private ResultActions doPostAnomalyWithdrawNoFile(final String data) throws Exception {
        return mockMvc.perform(
                post("/upload-request/shadow-anomaly-withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(data)
        ).andDo(print());
    }

    @Test
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-create-supply-import.xml",
            assertionMode = NON_STRICT
    )
    void submitSupplyImportSuccessfully() throws Exception {
        final String data = getJsonFromFile("supply-import.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("create-response-for-supply-import.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:controller/upload-request/before-validate-bbxd.xml")
    void validateBbxdExceptionTest() throws Exception {
        final String data = getJsonFromFile("create-supply-bbxd.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
                .andExpect(status().isConflict())
                .andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(),
                "{\"message\":\"ShopRequest with serviceId: 100, supplyRequestId: 2 type: " +
                        "X_DOC_PARTNER_SUPPLY_TO_FF and subtype: BREAK_BULK_XDOCK already exist\"}");
    }

    private void assertJsonResponseCorrect(String filename, MvcResult response) throws IOException {
        JSONAssert.assertEquals(getJsonFromFile(filename), response.getResponse().getContentAsString(),
            JSONCompareMode.NON_EXTENSIBLE);
    }

    private String getJsonFromFile(final String name) throws IOException {
        return FileContentUtils.getFileContent("controller/upload-request/" + name);
    }

    private MockMultipartFile getFile(String path,
                                      RequestType type,
                                      String extension,
                                      String mimeType) throws IOException {
        final String typePath = String.format(path, type.name().toLowerCase());
        final String origFileName = String.format("filename.%s", extension);
        final String resourceFileName = String.format("%s.%s", typePath, extension);
        return new MockMultipartFile("file", origFileName, mimeType,
            getSystemResourceAsStream(resourceFileName));
    }
}
