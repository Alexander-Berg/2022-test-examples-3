package ru.yandex.market.ff.controller.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.configuration.DateTimeTestConfig;
import ru.yandex.market.ff.enums.FileExtension;
import ru.yandex.market.ff.util.query.count.JpaQueriesCount;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("classpath:controller/upload-request/before.xml")
public class RequestUploadControllerWithdrawWithFileTest extends AbstractRequestUploadControllerWithFileTest {

    private static final String PRIMITIVE_FILE_PATH = "integration/%s/v%s/request_without_name_and_amount";
    private static final String REQUEST_WITH_DUPLICATES_FILE_PATH = "integration/%s/v%s/request_with_duplicated_items";
    private static final Instant WITHDRAW_DATE = DateTimeTestConfig.FIXED_SUPPLY_INSTANT
            .plus(1, ChronoUnit.DAYS);
    private static final long SUPPLIER_ID = 1;
    private static final long SERVICE_ID = 555;
    private static final String COMMENT = "some comment";
    private static final String EXTERNAL_REQUEST_ID = "externalId";
    private static final String EXTERNAL_OPERATION_TYPE = "2";
    private static final Map<String, String> VALID_WITHDRAW_PARAMS = withdrawParam(WITHDRAW_DATE);

    @BeforeEach
    void init() throws MalformedURLException {
        doNothing().when(mdsS3Client).upload(any(), any());
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(FILE_URL));
    }

    @Test
    void uploadWithdrawWithUnknownExternalOperationType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.WITHDRAW, "csv", FileExtension.CSV.getMimeType());
        Map<String, String> params = withdrawParamWithExternalData(DateTimeTestConfig.FIXED_WITHDRAW_FROM,
                UNKNOWN_EXTERNAL_OPERATION_TYPE);
        MvcResult mvcResult = upload(SUPPLIER_ID, file, "withdraw", null, SERVICE_ID, params)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"There is no ExternalOperationType with id: 666\"," +
                        "\"type\":\"INVALID_OPERATION_TYPE\"}"));
    }

    @Test
    @JpaQueriesCount(24)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-withdraw-xlsx.xml",
            assertionMode = NON_STRICT)
    void uploadWithdrawXlsx() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.WITHDRAW, "xlsx", FileExtension.XLSX.getMimeType());
        verifyOkWithdrawUpload(file, DateTimeTestConfig.FIXED_WITHDRAW_FROM, "withdraw");
    }

    @Test
    @JpaQueriesCount(23)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-shadow-withdraw-xlsx.xml",
            assertionMode = NON_STRICT)
    void uploadShadowWithdrawXlsx() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.WITHDRAW, "xlsx", FileExtension.XLSX.getMimeType());
        verifyOkShadowWithdrawUpload(file, "shadow-withdraw");
    }

    @Test
    @JpaQueriesCount(24)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-withdraw-xlsx.xml",
            assertionMode = NON_STRICT)
    void uploadWithdrawXlsxWithDuplicatedItems() throws Exception {
        MockMultipartFile file =
                getFileWithDuplicatedItems(RequestType.WITHDRAW, "xlsx", FileExtension.XLSX.getMimeType());
        verifyOkWithdrawUpload(file, DateTimeTestConfig.FIXED_WITHDRAW_FROM, "withdraw");
    }

    @Test
    @JpaQueriesCount(23)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-shadow-withdraw-xlsx.xml",
            assertionMode = NON_STRICT)
    void uploadShadowWithdrawXlsxWithDuplicatedItems() throws Exception {
        MockMultipartFile file =
                getFileWithDuplicatedItems(RequestType.WITHDRAW, "xlsx", FileExtension.XLSX.getMimeType());
        verifyOkShadowWithdrawUpload(file, "shadow-withdraw");
    }

    @Test
    @JpaQueriesCount(24)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-withdraw-xls.xml",
            assertionMode = NON_STRICT)
    void uploadWithdrawXls() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.WITHDRAW, "xls", FileExtension.XLS.getMimeType());
        verifyOkWithdrawUpload(file, DateTimeTestConfig.FIXED_WITHDRAW_TO, "withdraw");
    }

    @Test
    @JpaQueriesCount(23)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-shadow-withdraw-xls.xml",
            assertionMode = NON_STRICT)
    void uploadShadowWithdrawXls() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.WITHDRAW, "xls", FileExtension.XLS.getMimeType());
        verifyOkShadowWithdrawUpload(file, "shadow-withdraw");
    }

    @Test
    @JpaQueriesCount(23)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-withdraw-csv.xml",
            assertionMode = NON_STRICT)
    void uploadWithdrawCsv() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.WITHDRAW, "csv", FileExtension.CSV.getMimeType());
        verifyOkWithdrawUpload(file, DateTimeTestConfig.FIXED_WITHDRAW_FROM.plus(7, ChronoUnit.DAYS), "withdraw");
    }

    @Test
    @JpaQueriesCount(22)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-shadow-withdraw-csv.xml",
            assertionMode = NON_STRICT)
    void uploadShadowWithdrawCsv() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.WITHDRAW, "csv", FileExtension.CSV.getMimeType());
        verifyOkShadowWithdrawUpload(file, "shadow-withdraw");
    }

    @Test
    void uploadWithdrawCsvWrongParams() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.WITHDRAW, "csv", FileExtension.CSV.getMimeType());
        ImmutableMap<String, String> params = ImmutableMap.<String, String>builder()
                .put("date", WITHDRAW_DATE.toString())
                .put("stock", "0")
                .put("contactPersonName", "")
                .put("contactPersonSurname", "Безумный")
                .put("phoneNumber", "+7 923 556 24 56 (12)")
                .build();
        MvcResult result = upload(SUPPLIER_ID, file, "withdraw", null, SERVICE_ID, params)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertJsonResponseCorrect("withdraw-wrong-params-response.json", result);
    }

    @Test
    void uploadWithdrawCsvWithWrongRequestSubType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.WITHDRAW, "csv", FileExtension.CSV.getMimeType());

        Map<String, String> params = withdrawParamWithRequestSubType(WITHDRAW_DATE, "3");

        MvcResult result = upload(SUPPLIER_ID, file, "withdraw", null, SERVICE_ID, params)
            .andExpect(status().isBadRequest())
            .andReturn();

        assertJsonResponseCorrect("withdraw-wrong-type-response.json", result);
    }

    @Test
    void uploadShadowWithdrawCsvWithWrongRequestSubType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SHADOW_WITHDRAW, "csv", FileExtension.CSV.getMimeType());

        Map<String, String> params = shadowWithdrawParamWithRequestSubType(WITHDRAW_DATE, "3");

        MvcResult result = upload(SUPPLIER_ID, file, "shadow-withdraw", null, SERVICE_ID, params)
            .andExpect(status().isBadRequest())
            .andReturn();

        assertJsonResponseCorrect("shadow-withdraw-wrong-type-response.json", result);
    }

    @Test
    @JpaQueriesCount(24)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-withdraw-xlsx-with-subtype.xml",
        assertionMode = NON_STRICT)
    void uploadWithdrawXlsxWithRequestSubType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.WITHDRAW, "xlsx", FileExtension.XLSX.getMimeType());
        upload(SUPPLIER_ID, file, "withdraw", null, SERVICE_ID,
            withdrawParamWithRequestSubType(DateTimeTestConfig.FIXED_WITHDRAW_FROM, "1006")).andExpect(status().isOk());

        verify(mdsS3Client).upload(any(), any());
        verify(mdsS3Client).getUrl(any());
    }

    @Test
    @JpaQueriesCount(23)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-shadow-withdraw-xlsx-with-subtype.xml",
        assertionMode = NON_STRICT)
    void uploadShadowWithdrawXlsxWithRequestSubType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.WITHDRAW, "xlsx", FileExtension.XLSX.getMimeType());
        upload(SUPPLIER_ID, file, "shadow-withdraw", null, SERVICE_ID,
            shadowWithdrawParamWithRequestSubType(WITHDRAW_DATE, "1025")).andExpect(status().isOk());
        verify(mdsS3Client).upload(any(), any());
        verify(mdsS3Client).getUrl(any());
    }

    @Test
    void uploadWithdrawCsvWrongDate() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.WITHDRAW, "csv", FileExtension.CSV.getMimeType());
        Map<String, String> params =
                withdrawParam(DateTimeTestConfig.FIXED_WITHDRAW_TO.plus(1, ChronoUnit.SECONDS));
        MvcResult result = upload(SUPPLIER_ID, file, "withdraw", null, SERVICE_ID, params)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertJsonResponseCorrect("invalid-withdraw-date-error.json", result);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadWithdrawErrorsXlsx() throws Exception {
        MockMultipartFile file = getFileWithErrors(RequestType.WITHDRAW, "xlsx",
                FileExtension.XLSX.getMimeType());
        MvcResult result = upload(SUPPLIER_ID, file, "withdraw", null, SERVICE_ID, VALID_WITHDRAW_PARAMS)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertJsonResponseCorrect("withdraw-error-validation-response.json", result);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadWithdrawWithFileSizeExceededErrorXlsx() throws Exception {
        String origFileName = String.format("filename.%s", "xlsx");
        MockMultipartFile file = new MockMultipartFile("file", origFileName, FileExtension.XLSX.getMimeType(),
                InputStream.nullInputStream());
        MvcResult result = upload(SUPPLIER_ID, file, "withdraw", null, SERVICE_ID, VALID_WITHDRAW_PARAMS)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertJsonResponseCorrect("invalid-file-format.json", result);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup(
                    "classpath:controller/upload-request/small-max-withdraw-items-total-count-environment-param.xml")
    })
    void uploadWithdrawWithItemsTotalCountErrorXlsx() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.WITHDRAW, "xlsx",
                FileExtension.XLSX.getMimeType());
        MvcResult result = upload(SUPPLIER_ID, file, "withdraw", null, SERVICE_ID, VALID_WITHDRAW_PARAMS)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertJsonResponseCorrect("invalid-withdraw-items-total-count-error.json", result);
    }

    @Test
    @JpaQueriesCount(23)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-upload-withdraw-csv-with-withdraw-all-flag.xml",
            assertionMode = NON_STRICT)
    void uploadWithdrawWithoutNameAndAmountsCsv() throws Exception {
        MockMultipartFile file = getFileWithoutNamesAndAmounts(RequestType.WITHDRAW, "csv",
                FileExtension.CSV.getMimeType());
        MvcResult result = upload(SUPPLIER_ID, file, "withdraw", null, SERVICE_ID, withdrawAllParam(WITHDRAW_DATE))
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("withdraw-with-withdraw-all-flag-as-true.json", result);
    }

    @Test
    @JpaQueriesCount(23)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-withdraw-csv.xml",
            assertionMode = NON_STRICT)
    void uploadWithWithdrawAllFlagCsv() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.WITHDRAW, "csv", FileExtension.CSV.getMimeType());
        upload(SUPPLIER_ID, file, "withdraw", null,
                SERVICE_ID, withdrawAllParam(DateTimeTestConfig.FIXED_WITHDRAW_FROM.plus(7, ChronoUnit.DAYS)))
                .andExpect(status().isOk());
        verify(mdsS3Client).upload(any(), any());
        verify(mdsS3Client).getUrl(any());
    }

    @Test
    @JpaQueriesCount(22)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-withdraw-csv-with-external-data.xml",
            assertionMode = NON_STRICT)
    void uploadWithdrawWithExternalData() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.WITHDRAW, "csv", FileExtension.CSV.getMimeType());
        verifyOkWithdrawUploadWithFilledExternalData(file,
                withdrawParamWithExternalData(DateTimeTestConfig.FIXED_WITHDRAW_FROM, EXTERNAL_OPERATION_TYPE));
    }

    @Test
    @JpaQueriesCount(23)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-withdraw-csv-with-ignore-errors.xml",
            assertionMode = NON_STRICT)
    void uploadWithdrawWithIgnoreErrorItemsFlag() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.WITHDRAW, "csv", FileExtension.CSV.getMimeType());
        verifyOkWithdrawUploadWithFilledExternalData(file,
                withdrawParamWithIgnoreErrorsFlag(DateTimeTestConfig.FIXED_WITHDRAW_FROM));
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadWithdrawErrorsXls() throws Exception {
        MockMultipartFile file = getFileWithErrors(RequestType.WITHDRAW, "xls",
                FileExtension.XLS.getMimeType());
        MvcResult result = upload(SUPPLIER_ID, file, "withdraw", null, SERVICE_ID, VALID_WITHDRAW_PARAMS)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertJsonResponseCorrect("withdraw-error-validation-response.json", result);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadWithdrawErrorsCsv() throws Exception {
        MockMultipartFile file = getFileWithErrors(RequestType.WITHDRAW, "csv",
                FileExtension.CSV.getMimeType());
        MvcResult result = upload(SUPPLIER_ID, file, "withdraw", null, SERVICE_ID, VALID_WITHDRAW_PARAMS)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertJsonResponseCorrect("withdraw-error-validation-response.json", result);
    }

    @Test
    @JpaQueriesCount(23)
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-withdraw-csv.xml",
            assertionMode = NON_STRICT)
    void uploadWithdrawCsvWithPermittedRequest() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.WITHDRAW, "csv", FileExtension.CSV.getMimeType());
        verifyOkWithdrawUpload(file, DateTimeTestConfig.FIXED_WITHDRAW_FROM.plus(7, ChronoUnit.DAYS), "withdraw");
    }

    @Test
    @JpaQueriesCount(24)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-movement-withdraw-xlsx.xml",
            assertionMode = NON_STRICT)
    void uploadMovementWithdrawXlsx() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.WITHDRAW, "xlsx", FileExtension.XLSX.getMimeType());
        verifyOkWithdrawUpload(file, DateTimeTestConfig.FIXED_WITHDRAW_FROM, "movement-withdraw");
    }

    private void verifyOkWithdrawUploadWithFilledExternalData(MockMultipartFile file,
                                                              Map<String, String> params) throws Exception {
        upload(SUPPLIER_ID, file, "withdraw", null, SERVICE_ID, params).andExpect(status().isOk());
        verify(mdsS3Client, times(1)).upload(any(), any());
        verify(mdsS3Client, times(1)).getUrl(any());
    }

    private static Map<String, String> withdrawParam(Instant date) {
        return ImmutableMap.<String, String>builder()
                .put("date", date.toString())
                .put("stock", "0")
                .put("consignee", "ООО НЕФТЬ VAPE")
                .put("phoneNumber", "79234568797")
                .put("contactPersonName", "Извозчик")
                .put("contactPersonSurname", "Безумный")
                .put("comment", COMMENT) .build();
    }

    private static Map<String, String> shadowWithdrawParam() {
        return ImmutableMap.<String, String>builder()
                .put("stock", "0")
                .put("calendaringMode", "0")
                .put("ignoreItemsWithError", "true")
                .put("withdrawAllWithLimit", "false")
                .put("realSupplierId", "realSupplierId1")
                .put("realSupplierName", "realSupplierName1")
                .build();
    }

    private static Map<String, String> withdrawAllParam(Instant date) {
        return ImmutableMap.<String, String>builder()
                .put("date", date.toString())
                .put("stock", "0")
                .put("consignee", "ООО НЕФТЬ VAPE")
                .put("phoneNumber", "79234568797")
                .put("contactPersonName", "Извозчик")
                .put("contactPersonSurname", "Безумный")
                .put("withdrawAllWithLimit", "true")
                .put("comment", COMMENT)
                .build();
    }

    private static Map<String, String> withdrawParamWithExternalData(Instant date,
                                                                     String externalOperationType) {
        return ImmutableMap.<String, String>builder()
                .put("date", date.toString())
                .put("stock", "0")
                .put("consignee", "ООО НЕФТЬ VAPE")
                .put("phoneNumber", "79234568797")
                .put("contactPersonName", "Извозчик")
                .put("contactPersonSurname", "Безумный")
                .put("comment", COMMENT)
                .put("externalRequestId", EXTERNAL_REQUEST_ID)
                .put("externalOperationType", externalOperationType)
                .build();
    }

    private static Map<String, String> withdrawParamWithRequestSubType(Instant date,
                                                                     String type) {
        return ImmutableMap.<String, String>builder()
            .put("date", date.toString())
            .put("type", type)
            .put("stock", "0")
            .put("consignee", "ООО НЕФТЬ VAPE")
            .put("phoneNumber", "79234568797")
            .put("contactPersonName", "Извозчик")
            .put("contactPersonSurname", "Безумный")
            .put("comment", COMMENT)
            .build();
    }

    private static Map<String, String> shadowWithdrawParamWithRequestSubType(Instant date,
                                                                    String type) {
        return ImmutableMap.<String, String>builder()
            .put("stock", "0")
            .put("type", type)
            .put("calendaringMode", "0")
            .put("ignoreItemsWithError", "true")
            .put("withdrawAllWithLimit", "false")
            .put("realSupplierId", "realSupplierId1")
            .put("realSupplierName", "realSupplierName1")
            .build();
    }

    private static Map<String, String> withdrawParamWithIgnoreErrorsFlag(Instant date) {
        return ImmutableMap.<String, String>builder()
                .put("date", date.toString())
                .put("stock", "0")
                .put("consignee", "ООО НЕФТЬ VAPE")
                .put("phoneNumber", "79234568797")
                .put("contactPersonName", "Извозчик")
                .put("contactPersonSurname", "Безумный")
                .put("comment", COMMENT)
                .put("ignoreItemsWithError", "true")
                .build();
    }

    private void verifyOkWithdrawUpload(MockMultipartFile file, Instant date, String typePath) throws Exception {
        upload(SUPPLIER_ID, file, typePath, null, SERVICE_ID, withdrawParam(date)).andExpect(status().isOk());
        verify(mdsS3Client).upload(any(), any());
        verify(mdsS3Client).getUrl(any());
    }

    private void verifyOkShadowWithdrawUpload(MockMultipartFile file, String typePath) throws Exception {
        upload(SUPPLIER_ID, file, typePath, null, SERVICE_ID, shadowWithdrawParam()).andExpect(status().isOk());
        verify(mdsS3Client).upload(any(), any());
        verify(mdsS3Client).getUrl(any());
    }

    private MockMultipartFile getFileWithoutNamesAndAmounts(RequestType type,
                                                            String extension,
                                                            String mimeType) throws IOException {
        return getFile(PRIMITIVE_FILE_PATH, type, extension, mimeType);
    }

    private MockMultipartFile getFileWithDuplicatedItems(RequestType type,
                                                         String extension,
                                                         String mimeType) throws IOException {
        return getFile(REQUEST_WITH_DUPLICATES_FILE_PATH, type, extension, mimeType);
    }
}
