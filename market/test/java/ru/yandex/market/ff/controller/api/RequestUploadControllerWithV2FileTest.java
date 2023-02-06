package ru.yandex.market.ff.controller.api;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.configuration.DateTimeTestConfig;
import ru.yandex.market.ff.enums.FileExtension;
import ru.yandex.market.ff.util.query.count.JpaQueriesCount;
import ru.yandex.market.logistic.api.utils.TimeZoneUtil;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тесты для файла имеющего две версии
 */
@DatabaseSetup("classpath:controller/upload-request/before.xml")
public class RequestUploadControllerWithV2FileTest extends AbstractRequestUploadControllerWithFileTest {

    private static final long X_DOC_SERVICE_ID = 111;
    private static final String EXTERNAL_REQUEST_ID = "externalId";
    private static final String EXTERNAL_OPERATION_TYPE = "2";
    private static final long UNKNOWN_SHOP_ID = 15;
    private static final long SUPPLIER_ID = 1;
    private static final long SERVICE_ID = 555;
    private static final Instant SUPPLY_DATE = DateTimeTestConfig.FIXED_SUPPLY_INSTANT
            .plus(1, ChronoUnit.DAYS);
    private static final Instant NOW_WITH_SHIFT = DateTimeTestConfig.FIXED_NOW.plusHours(3)
            .toInstant(TimeZoneUtil.DEFAULT_OFFSET);
    private static final Instant X_DOC_DATE = DateTimeTestConfig.FIXED_SUPPLY_INSTANT
            .plus(2, ChronoUnit.DAYS);

    private static final int SUPPLY_INBOUND_TYPE = 0;
    private static final String COMMENT = "some comment";

    @BeforeEach
    void init() throws MalformedURLException {
        doNothing().when(mdsS3Client).upload(any(), any());
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(FILE_URL));
    }

    //// ТЕСТЫ НА СОЗДАНИЕ С ФАЙЛОМ
    @Test
    @JpaQueriesCount(20)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-shadow-supply-xlsx-v2.xml",
            assertionMode = NON_STRICT)
    void uploadOptionsXlsxV2() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SHADOW_SUPPLY, "xlsx", FileExtension.XLSX.getMimeType(), 2);
        verifyOkShadowSupplyUpload(file);
    }

    @Test
    @JpaQueriesCount(19)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-shadow-supply-csv-v2.xml",
            assertionMode = NON_STRICT)
    void uploadOptionsCsvV2() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SHADOW_SUPPLY, "csv", FileExtension.CSV.getMimeType(), 2);
        verifyOkShadowSupplyUpload(file);
    }

    @Test
    @JpaQueriesCount(6)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadOptionsWithValidationErrorsCsvV2() throws Exception {
        MockMultipartFile file = getFileWithErrors(RequestType.SHADOW_SUPPLY, "csv",
                FileExtension.CSV.getMimeType(), 2);
        final MvcResult mvcResult = upload(SUPPLIER_ID, file, "shadow-supply", null, null, null)
                .andExpect(status().isBadRequest())
                .andReturn();
        assertJsonResponseCorrect("supply-error-validation-response-v2.json", mvcResult);
    }

    @Test
    @JpaQueriesCount(21)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-xlsx-v2.xml",
            assertionMode = NON_STRICT)
    void uploadSupplyXlsxV2() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xlsx", FileExtension.XLSX.getMimeType(), 2);
        verifyOkSupplyUpload(file, null);
    }


    @Test
    @JpaQueriesCount(21)
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-for-dbqueue-validation-xlsx-v2.xml",
            assertionMode = NON_STRICT)
    void uploadSupplyForDbQueueValidationXlsxV2() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xlsx", FileExtension.XLSX.getMimeType(), 2);
        verifyOkSupplyUpload(file, null);
    }

    @Test
    @JpaQueriesCount(21)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-upload-xlsx-v2-with-confirmation-flag-as-true.xml",
            assertionMode = NON_STRICT
    )
    void uploadSupplyXlsxV2WithConfirmationFlagAsTrue() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xlsx", FileExtension.XLSX.getMimeType(), 2);

        final ImmutableMap<String, String> params = ImmutableMap.<String, String>builder()
                .put("date", getDateBasedOnInboundType())
                .put("comment", COMMENT)
                .put("needConfirmation", "true")
                .build();

        MvcResult mvcResult = upload(SUPPLIER_ID, file, "supply", null, SERVICE_ID, params)
                .andExpect(status().isOk())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(), containsString("\"needConfirmation\":true"));
    }

    @Test
    @JpaQueriesCount(21)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-xlsx-v2-with-calendaring-mode.xml",
            assertionMode = NON_STRICT)
    void uploadSupplyWithCalendaringModeXlsxV2() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xlsx", FileExtension.XLSX.getMimeType(), 2);

        final ImmutableMap<String, String> params = ImmutableMap.<String, String>builder()
                .put("date", getDateBasedOnInboundType())
                .put("comment", COMMENT)
                .put("calendaringMode", "1")
                .build();

        MvcResult mvcResult = upload(SUPPLIER_ID, file, "supply", 0, SERVICE_ID, params)
                .andExpect(status().isOk())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(), containsString("\"calendaringMode\":1"));
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadSupplyWithRequestCreatorXlsxV2() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xlsx", FileExtension.XLSX.getMimeType(), 2);

        final ImmutableMap<String, String> params = ImmutableMap.<String, String>builder()
                .put("date", getDateBasedOnInboundType())
                .put("comment", COMMENT)
                .put("requestCreator", "CreatorFirstName CreatorLastName")
                .build();

        MvcResult mvcResult = upload(SUPPLIER_ID, file, "supply", 0, SERVICE_ID, params)
                .andExpect(status().isOk())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                containsString("\"requestCreator\":\"CreatorFirstName CreatorLastName\""));
    }

    @Test
    @JpaQueriesCount(21)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-xlsx-v2.xml",
            assertionMode = NON_STRICT)
    void uploadSupplyXlsxV2WithType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xlsx", FileExtension.XLSX.getMimeType(), 2);
        verifyOkSupplyUpload(file, SUPPLY_INBOUND_TYPE);
    }

    @Test
    @JpaQueriesCount(21)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-upload-xlsx-v2-with-e-document-type.xml",
            assertionMode = NON_STRICT
    )
    void uploadSupplyXlsxWithV2ElectronicDocumentType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xlsx", FileExtension.XLSX.getMimeType(), 2);
        ImmutableMap<String, String> params = ImmutableMap.<String, String>builder()
                .put("date", getDateBasedOnInboundType())
                .put("comment", COMMENT)
                .put("documentType", "ELECTRONIC")
                .build();
        MvcResult mvcResult = upload(SUPPLIER_ID, file, "supply", null, SERVICE_ID, params)

                .andExpect(status().isOk())
                .andReturn();
        assertThat(mvcResult.getResponse().getContentAsString(), containsString("\"documentType\":1"));
    }


    @Test
    @JpaQueriesCount(21)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-xls-v2.xml", assertionMode = NON_STRICT)
    void uploadSupplyXlsV2() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xls",
                FileExtension.XLS.getMimeTypes().get(0), 2);
        verifyOkSupplyUpload(file, null);
    }

    @Test
    @JpaQueriesCount(21)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-xls-v2.xml", assertionMode = NON_STRICT)
    void uploadSupplyXlsV2WithType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xls",
                FileExtension.XLS.getMimeTypes().get(0), 2);
        verifyOkSupplyUpload(file, SUPPLY_INBOUND_TYPE);
    }

    @Test
    @JpaQueriesCount(21)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-xls-v2.xml", assertionMode = NON_STRICT)
    void uploadSupplyXlsV2WpsOffice() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xls",
                FileExtension.XLS.getMimeTypes().get(1), 2);
        verifyOkSupplyUpload(file, null);
    }

    @Test
    @JpaQueriesCount(21)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-xls-v2.xml", assertionMode = NON_STRICT)
    void uploadSupplyXlsV2WpsOfficeWithType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xls",
                FileExtension.XLS.getMimeTypes().get(1), 2);
        verifyOkSupplyUpload(file, SUPPLY_INBOUND_TYPE);
    }

    @Test
    @JpaQueriesCount(20)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-csv-v2.xml", assertionMode = NON_STRICT)
    void uploadSupplyCsvV2() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType(), 2);
        verifyOkSupplyUpload(file, null);
    }

    @Test
    @JpaQueriesCount(20)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-day-to-day-csv-v2.xml",
            assertionMode = NON_STRICT)
    void uploadSupplyCsvV2WithDayToDay() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType(), 2);
        ImmutableMap<String, String> params = ImmutableMap.<String, String>builder()
                .put("date", NOW_WITH_SHIFT.toString())
                .put("comment", COMMENT)
                .put("externalRequestId", EXTERNAL_REQUEST_ID)
                .put("externalOperationType", EXTERNAL_OPERATION_TYPE)
                .build();
        upload(SUPPLIER_ID, file, "supply", SUPPLY_INBOUND_TYPE, SERVICE_ID, params);
    }


    @Test
    @JpaQueriesCount(20)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-csv-v2.xml", assertionMode = NON_STRICT)
    void uploadSupplyCsvV2WithType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType(), 2);
        verifyOkSupplyUpload(file, SUPPLY_INBOUND_TYPE);
    }

    @Test
    @JpaQueriesCount(20)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-supply-csv-v2-with-external-data.xml",
            assertionMode = NON_STRICT)
    void uploadSupplyWithExternalDataV2() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType(), 2);
        verifyOkSupplyUploadWithFilledExternalData(file, null);
    }

    @Test
    @JpaQueriesCount(20)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-supply-csv-v2-with-external-data.xml",
            assertionMode = NON_STRICT)
    void uploadSupplyWithExternalDataWithTypeV2() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType(), 2);
        verifyOkSupplyUploadWithFilledExternalData(file, SUPPLY_INBOUND_TYPE);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    })
    @JpaQueriesCount(20)
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-supply-csv-v2-with-x-doc-data.xml",
            assertionMode = NON_STRICT)
    void uploadXDocSupplyV2() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType(), 2);
        verifyOkXDocSupplyUpload(file, null);
    }

    @Test
    @JpaQueriesCount(20)
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-supply-csv-v2-with-x-doc-data.xml",
            assertionMode = NON_STRICT)
    void uploadXDocSupplyWithTypeV2() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType(), 2);
        verifyOkXDocSupplyUpload(file, SUPPLY_INBOUND_TYPE);
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request/before.xml", assertionMode = NON_STRICT)
    void uploadXDocSupplyWhileXDocSuppliesAreNotAllowedV2() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType(), 2);
        uploadXDocSupply(SUPPLIER_ID, file, null).andExpect(status().isInternalServerError());
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request/before.xml", assertionMode = NON_STRICT)
    void uploadXDocSupplyWhileXDocSuppliesAreNotAllowedWithTypeV2() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType(), 2);
        uploadXDocSupply(SUPPLIER_ID, file, SUPPLY_INBOUND_TYPE).andExpect(status().isInternalServerError());
    }

    @Test
    void uploadSupplyWithoutExtensionV2() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", "", 2);
        uploadSupplyWithDate(SUPPLIER_ID, file, null).andExpect(status().isBadRequest());
    }

    @Test
    void uploadSupplyWithoutExtensionWithTypeV2() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", "", 2);
        uploadSupplyWithDate(SUPPLIER_ID, file, SUPPLY_INBOUND_TYPE).andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void supplierNotFoundV2() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType(), 2);
        final MvcResult mvcResult = uploadSupplyWithDate(UNKNOWN_SHOP_ID, file, null)
                .andExpect(status().isNotFound()).andReturn();
        assertJsonResponseCorrect("supplier-not-found.json", mvcResult);
    }


    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void supplierNotFoundWithTypeV2() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType(), 2);
        final MvcResult mvcResult = uploadSupplyWithDate(UNKNOWN_SHOP_ID, file, SUPPLY_INBOUND_TYPE)
                .andExpect(status().isNotFound()).andReturn();
        assertJsonResponseCorrect("supplier-not-found.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadWithValidationErrorsCsvV2() throws Exception {
        MockMultipartFile file = getFileWithErrors(RequestType.SUPPLY, "csv",
                FileExtension.CSV.getMimeType(), 2);
        final MvcResult mvcResult = uploadSupplyWithDate(SUPPLIER_ID, file, null)
                .andExpect(status().isBadRequest())
                .andReturn();
        assertJsonResponseCorrect("supply-error-validation-response-v2.json", mvcResult);
    }


    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadWithValidationErrorsCsvV2WithType() throws Exception {
        MockMultipartFile file = getFileWithErrors(RequestType.SUPPLY, "csv",
                FileExtension.CSV.getMimeType(), 2);
        final MvcResult mvcResult = uploadSupplyWithDate(SUPPLIER_ID, file, SUPPLY_INBOUND_TYPE)
                .andExpect(status().isBadRequest())
                .andReturn();
        assertJsonResponseCorrect("supply-error-validation-response-v2.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadWithValidationErrorsXlsV2() throws Exception {
        MockMultipartFile file = getFileWithErrors(RequestType.SUPPLY, "xls",
                FileExtension.XLS.getMimeType(), 2);
        final MvcResult mvcResult = uploadSupplyWithDate(SUPPLIER_ID, file, null)
                .andExpect(status().isBadRequest())
                .andReturn();
        assertJsonResponseCorrect("supply-error-validation-response-v2.json", mvcResult);
    }


    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadWithValidationErrorsXlsV2WithType() throws Exception {
        MockMultipartFile file = getFileWithErrors(RequestType.SUPPLY, "xls",
                FileExtension.XLS.getMimeType(), 2);
        final MvcResult mvcResult = uploadSupplyWithDate(SUPPLIER_ID, file, SUPPLY_INBOUND_TYPE)
                .andExpect(status().isBadRequest())
                .andReturn();
        assertJsonResponseCorrect("supply-error-validation-response-v2.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadWithValidationErrorsXlsxV2() throws Exception {
        MockMultipartFile file = getFileWithErrors(RequestType.SUPPLY, "xlsx",
                FileExtension.XLSX.getMimeType(), 2);
        final MvcResult mvcResult = uploadSupplyWithDate(SUPPLIER_ID, file, null)
                .andExpect(status().isBadRequest())
                .andReturn();
        assertJsonResponseCorrect("supply-error-validation-response-v2.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadWithValidationErrorsXlsxV2WithType() throws Exception {
        MockMultipartFile file = getFileWithErrors(RequestType.SUPPLY, "xlsx",
                FileExtension.XLSX.getMimeType(), 2);
        final MvcResult mvcResult = uploadSupplyWithDate(SUPPLIER_ID, file, SUPPLY_INBOUND_TYPE)
                .andExpect(status().isBadRequest())
                .andReturn();
        assertJsonResponseCorrect("supply-error-validation-response-v2.json", mvcResult);
    }

    private void verifyOkSupplyUpload(final MockMultipartFile file, Integer inboundType) throws Exception {
        uploadSupplyWithDate(SUPPLIER_ID, file, inboundType).andExpect(status().isOk());
        verify(mdsS3Client, times(1)).upload(any(), any());
        verify(mdsS3Client, times(1)).getUrl(any());
    }

    private void verifyOkShadowSupplyUpload(final MockMultipartFile file) throws Exception {
        final ImmutableMap<String, String> params = ImmutableMap.<String, String>builder()
                .put("comment", COMMENT)
                .put("vetis", "true")
                .build();
        upload(SUPPLIER_ID, file, "shadow-supply", null, null, params).andExpect(status().isOk());
        verify(mdsS3Client, times(1)).upload(any(), any());
        verify(mdsS3Client, times(1)).getUrl(any());
    }

    private void verifyOkSupplyUploadWithFilledExternalData(final MockMultipartFile file,
                                                            Integer inboundType) throws Exception {
        uploadSupplyWithExternalData(SUPPLIER_ID, file, EXTERNAL_OPERATION_TYPE, inboundType)
                .andExpect(status().isOk());
        verify(mdsS3Client, times(1)).upload(any(), any());
        verify(mdsS3Client, times(1)).getUrl(any());
    }

    private void verifyOkXDocSupplyUpload(final MockMultipartFile file, Integer inboundType) throws Exception {
        uploadXDocSupply(SUPPLIER_ID, file, inboundType).andExpect(status().isOk());
        verify(mdsS3Client, times(1)).upload(any(), any());
        verify(mdsS3Client, times(1)).getUrl(any());
    }

    private ResultActions uploadSupplyWithDate(final long shopId, final MockMultipartFile file,
                                               Integer inboundType) throws Exception {

        final ImmutableMap<String, String> params = ImmutableMap.<String, String>builder()
                .put("date", getDateBasedOnInboundType())
                .put("comment", COMMENT)
                .build();
        return upload(shopId, file, "supply", inboundType, SERVICE_ID, params);
    }

    private ResultActions uploadSupplyWithExternalData(final long shopId,
                                                       final MockMultipartFile file,
                                                       String externalOperationType,
                                                       Integer inboundType) throws Exception {
        final ImmutableMap<String, String> params = ImmutableMap.<String, String>builder()
                .put("date", getDateBasedOnInboundType())
                .put("comment", COMMENT)
                .put("externalRequestId", EXTERNAL_REQUEST_ID)
                .put("externalOperationType", externalOperationType)
                .build();
        return upload(shopId, file, "supply", inboundType, SERVICE_ID, params);
    }

    private String getDateBasedOnInboundType() {
        return SUPPLY_DATE.toString();
    }

    private ResultActions uploadXDocSupply(final long shopId,
                                           final MockMultipartFile file,
                                           Integer inboundType) throws Exception {
        final ImmutableMap<String, String> params = ImmutableMap.<String, String>builder()
                .put("xDocServiceId", String.valueOf(X_DOC_SERVICE_ID))
                .put("xDocDate", X_DOC_DATE.toString())
                .put("comment", COMMENT)
                .build();
        return upload(shopId, file, "supply", inboundType, SERVICE_ID, params);
    }
}
