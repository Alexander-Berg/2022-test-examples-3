package ru.yandex.market.ff.controller.api;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.configuration.DateTimeTestConfig;
import ru.yandex.market.ff.configuration.FeatureToggleTestConfiguration;
import ru.yandex.market.ff.enums.FileExtension;
import ru.yandex.market.ff.util.query.count.JpaQueriesCount;
import ru.yandex.market.logistic.api.utils.TimeZoneUtil;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("AnnotationUseStyle")
@DatabaseSetup("classpath:controller/upload-request/before.xml")
public class RequestUploadControllerWithFileTest extends AbstractRequestUploadControllerWithFileTest {
    private static final String VALID_FILE_PATH_WITHOUT_VAT = VALID_FILE_PATH + "_without_vat";

    private static final long X_DOC_SERVICE_ID = 111;
    private static final String EXTERNAL_REQUEST_ID = "externalId";
    private static final String EXTERNAL_OPERATION_TYPE = "2";
    private static final long UNKNOWN_SHOP_ID = 15;
    private static final long UNKNOWN_SERVICE_ID = 999;
    private static final long SUPPLIER_ID = 1;
    private static final long SUPPLIER_1P_ID = 4;

    private static final long SERVICE_ID = 555;
    private static final Instant SUPPLY_DATE = DateTimeTestConfig.FIXED_SUPPLY_INSTANT
            .plus(1, ChronoUnit.DAYS);
    private static final Instant NOW_WITH_SHIFT = DateTimeTestConfig.FIXED_NOW.plusHours(3)
            .toInstant(TimeZoneUtil.DEFAULT_OFFSET);
    private static final Instant INVALID_DATE = NOW_WITH_SHIFT.minus(1, ChronoUnit.DAYS);
    private static final Instant X_DOC_DATE = DateTimeTestConfig.FIXED_SUPPLY_INSTANT
            .plus(2, ChronoUnit.DAYS);

    private static final int SUPPLY_INBOUND_TYPE = 0;
    private static final String COMMENT = "some comment";

    @Autowired
    private FeatureToggleTestConfiguration.FTConfig ftConfig;

    @BeforeEach
    void init() throws MalformedURLException {
        doNothing().when(mdsS3Client).upload(any(), any());
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(FILE_URL));
    }

    //// ТЕСТЫ НА СОЗДАНИЕ С ФАЙЛОМ
    @Test
    @JpaQueriesCount(20)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-shadow-supply-xlsx.xml",
            assertionMode = NON_STRICT)
    void uploadOptionsXlsx() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SHADOW_SUPPLY, "xlsx", FileExtension.XLSX.getMimeType());
        verifyOkShadowSupplyUpload(file);
    }

    @Test
    @JpaQueriesCount(19)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-shadow-supply-csv.xml",
            assertionMode = NON_STRICT)
    void uploadOptionsCsv() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SHADOW_SUPPLY, "csv", FileExtension.CSV.getMimeType());
        verifyOkShadowSupplyUpload(file);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadWithNullServiceId() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SHADOW_SUPPLY, "csv", FileExtension.CSV.getMimeType());
        final MvcResult mvcResult = upload(SUPPLIER_1P_ID, file, "shadow-supply", null, null, null)
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Service id can't be null for shadow supplies 1P\"}"));
    }


    @Test
    @JpaQueriesCount(6)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadOptionsWithValidationErrorsCsv() throws Exception {
        MockMultipartFile file = getFileWithErrors(RequestType.SHADOW_SUPPLY, "csv",
                FileExtension.CSV.getMimeType());
        final MvcResult mvcResult = upload(SUPPLIER_ID, file, "shadow-supply", null, null, null)
                .andExpect(status().isBadRequest())
                .andReturn();
        assertJsonResponseCorrect("supply-error-validation-response.json", mvcResult);
    }

    @Test
    @JpaQueriesCount(21)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-xlsx.xml", assertionMode = NON_STRICT)
    void uploadSupplyXlsx() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xlsx", FileExtension.XLSX.getMimeType());
        verifyOkSupplyUpload(file, null);
    }

    @Test
    @JpaQueriesCount(33)
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-request/before-additional-supply-xlsx.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-additional-supply-xlsx.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void uploadAdditionalSupplyXlsx() throws Exception {
        ftConfig.setSupplyEnabled(true);
        RequestType requestType = RequestType.ADDITIONAL_SUPPLY;
        MockMultipartFile file = getValidFile(requestType, "xlsx", FileExtension.XLSX.getMimeType());
        verifyOkRequestUpload(file, null, requestType, 100L);
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadAdditionalSupplyWhenNoParentRequestXlsx() throws Exception {
        ftConfig.setSupplyEnabled(true);
        RequestType requestType = RequestType.ADDITIONAL_SUPPLY;
        MockMultipartFile file = getValidFile(requestType, "xlsx", FileExtension.XLSX.getMimeType());
        uploadSubRequestWithoutDate(SUPPLIER_ID, file, null, requestType, 100L)
                .andExpect(status().isNotFound())
                .andExpect(content()
                        .json("{\"message\":\"Failed to find [REQUEST] with id [100]\"," +
                                "\"resourceType\":\"REQUEST\",\"identifier\":\"100\"}"));
    }

    @Test
    @JpaQueriesCount(13)
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-request/before-additional-supply-unacceptable-xlsx.xml")
    })
    void uploadAdditionalSupplyWhenParentCountLessThanRequiredXlsx() throws Exception {
        ftConfig.setSupplyEnabled(true);
        RequestType requestType = RequestType.ADDITIONAL_SUPPLY;
        MockMultipartFile file = getValidFile(requestType, "xlsx", FileExtension.XLSX.getMimeType());
        uploadSubRequestWithoutDate(SUPPLIER_ID, file, null, requestType, 100L)
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .json("{\"message\":\"Item with article: 1714984234 and consignments: " +
                                "consignment_id_1 was not found in parent supply " +
                                "or it's given count: 4 is greater " +
                                "than allowed of: 0\",\"type\":\"INVALID_REQUEST_ITEM\"}"));
    }

    @Test
    @JpaQueriesCount(13)
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-request/before-additional-supply-less-count-xlsx.xml")
    })
    void uploadAdditionalSupplyWhenUnitCountUnacceptableXlsx() throws Exception {
        ftConfig.setSupplyEnabled(true);
        RequestType requestType = RequestType.ADDITIONAL_SUPPLY;
        MockMultipartFile file = getValidFile(requestType, "xlsx", FileExtension.XLSX.getMimeType());
        uploadSubRequestWithoutDate(SUPPLIER_ID, file, null, requestType, 100L)
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .json("{\"message\":\"Item with article: 1714984176 and consignments: " +
                                "consignment_id_2, consignment_id_3 was not found in parent supply " +
                                "or it's given count: 3 is greater than " +
                                "allowed of: 1\",\"type\":\"INVALID_REQUEST_ITEM\"}"));
    }

    @Test
    @JpaQueriesCount(21)
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-for-dbqueue-validation-xlsx.xml",
            assertionMode = NON_STRICT)
    void uploadSupplyForDbQueueValidationXlsx() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xlsx", FileExtension.XLSX.getMimeType());
        verifyOkSupplyUpload(file, null);
    }

    @Test
    @JpaQueriesCount(21)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-upload-xlsx-with-confirmation-flag-as-true.xml",
            assertionMode = NON_STRICT
    )
    void uploadSupplyXlsxWithConfirmationFlagAsTrue() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xlsx", FileExtension.XLSX.getMimeType());

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
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-xlsx-with-calendaring-mode.xml",
            assertionMode = NON_STRICT)
    void uploadSupplyWithCalendaringModeXlsx() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xlsx", FileExtension.XLSX.getMimeType());

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
    void uploadSupplyWithRequestCreatorXlsx() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xlsx", FileExtension.XLSX.getMimeType());

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
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-xlsx.xml", assertionMode = NON_STRICT)
    void uploadSupplyXlsxWithType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xlsx", FileExtension.XLSX.getMimeType());
        verifyOkSupplyUpload(file, SUPPLY_INBOUND_TYPE);
    }

    @Test
    @JpaQueriesCount(21)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-xlsx-without-vat.xml",
            assertionMode = NON_STRICT)
    void uploadSupplyXlsxWithoutVatWithType() throws Exception {
        MockMultipartFile file = getValidFileWithoutVat(RequestType.SUPPLY, "xlsx",
                FileExtension.XLSX.getMimeType());
        verifyOkSupplyUpload(file, SUPPLY_INBOUND_TYPE);
    }

    @Test
    @JpaQueriesCount(21)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(
            value = "classpath:controller/upload-request/after-upload-xlsx-with-e-document-type.xml",
            assertionMode = NON_STRICT
    )
    void uploadSupplyXlsxWithElectronicDocumentType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xlsx", FileExtension.XLSX.getMimeType());
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
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-xls.xml", assertionMode = NON_STRICT)
    void uploadSupplyXls() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xls",
                FileExtension.XLS.getMimeTypes().get(0));
        verifyOkSupplyUpload(file, null);
    }

    @Test
    @JpaQueriesCount(21)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-xls.xml", assertionMode = NON_STRICT)
    void uploadSupplyXlsWithType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xls",
                FileExtension.XLS.getMimeTypes().get(0));
        verifyOkSupplyUpload(file, SUPPLY_INBOUND_TYPE);
    }

    @Test
    @JpaQueriesCount(21)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-xls-without-vat.xml",
            assertionMode = NON_STRICT)
    void uploadSupplyXlsWithoutVatWithType() throws Exception {
        MockMultipartFile file = getValidFileWithoutVat(RequestType.SUPPLY, "xls",
                FileExtension.XLS.getMimeTypes().get(0));
        verifyOkSupplyUpload(file, SUPPLY_INBOUND_TYPE);
    }

    @Test
    @JpaQueriesCount(21)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-xls.xml", assertionMode = NON_STRICT)
    void uploadSupplyXlsWpsOffice() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xls",
                FileExtension.XLS.getMimeTypes().get(1));
        verifyOkSupplyUpload(file, null);
    }

    @Test
    @JpaQueriesCount(21)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-xls.xml", assertionMode = NON_STRICT)
    void uploadSupplyXlsWpsOfficeWithType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xls",
                FileExtension.XLS.getMimeTypes().get(1));
        verifyOkSupplyUpload(file, SUPPLY_INBOUND_TYPE);
    }

    @Test
    @JpaQueriesCount(20)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-csv.xml", assertionMode = NON_STRICT)
    void uploadSupplyCsv() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());
        verifyOkSupplyUpload(file, null);
    }


    @Test
    @JpaQueriesCount(20)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-csv-without-vat.xml",
            assertionMode = NON_STRICT)
    void uploadSupplyCsvWithoutVat() throws Exception {
        MockMultipartFile file = getValidFileWithoutVat(RequestType.SUPPLY, "csv",
                FileExtension.CSV.getMimeType());
        verifyOkSupplyUpload(file, null);
    }

    @Test
    @JpaQueriesCount(20)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-day-to-day-csv.xml",
            assertionMode = NON_STRICT)
    void uploadSupplyCsvWithDayToDay() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());
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
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-csv.xml", assertionMode = NON_STRICT)
    void uploadSupplyCsvWithType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());
        verifyOkSupplyUpload(file, SUPPLY_INBOUND_TYPE);
    }

    @Test
    void uploadSupplyCsvWithoutDate() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());
        uploadSupplyWithoutDate(SUPPLIER_ID, file, null)
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getJsonFromFile("supply-without-date-response.json")));
    }

    @Test
    void uploadSupplyCsvWithoutDateWithType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());
        uploadSupplyWithoutDate(SUPPLIER_ID, file, SUPPLY_INBOUND_TYPE)
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getJsonFromFile("supply-without-date-response.json")));
    }

    @Test
    @DatabaseSetup("classpath:empty.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/before.xml", assertionMode = NON_STRICT)
    void uploadXDocSupplyCsvWithoutXDocDate() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());
        uploadXDocSupplyWithoutXDocDate(SUPPLIER_ID, file, null)
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getJsonFromFile("supply-without-x-doc-date-response.json")));
    }

    @Test
    @DatabaseSetup("classpath:empty.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/before.xml", assertionMode = NON_STRICT)
    void uploadXDocSupplyCsvWithoutXDocDateWithType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());
        uploadXDocSupplyWithoutXDocDate(SUPPLIER_ID, file, SUPPLY_INBOUND_TYPE)
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getJsonFromFile("supply-without-x-doc-date-response.json")));
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
    @JpaQueriesCount(22)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value =
            "classpath:controller/upload-request/after-upload-withdraw-csv-with-external-data-and-fake-values.xml",
            assertionMode = NON_STRICT)
    void uploadWithdrawWithExternalDataAndContactDetailsMissing() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.WITHDRAW, "csv", FileExtension.CSV.getMimeType());
        verifyOkWithdrawUploadWithFilledExternalData(file, withdrawParamWithExternalDataWithoutContactDetails(
                DateTimeTestConfig.FIXED_WITHDRAW_FROM, EXTERNAL_OPERATION_TYPE));
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
    @JpaQueriesCount(20)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-supply-csv-with-external-data.xml",
            assertionMode = NON_STRICT)
    void uploadSupplyWithExternalData() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());
        verifyOkSupplyUploadWithFilledExternalData(file, null);
    }

    @Test
    @JpaQueriesCount(20)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-supply-csv-with-external-data.xml",
            assertionMode = NON_STRICT)
    void uploadSupplyWithExternalDataWithType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());
        verifyOkSupplyUploadWithFilledExternalData(file, SUPPLY_INBOUND_TYPE);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    })
    @JpaQueriesCount(20)
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-supply-csv-with-x-doc-data.xml",
            assertionMode = NON_STRICT)
    void uploadXDocSupply() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());
        verifyOkXDocSupplyUpload(file, null);
    }

    @Test
    @JpaQueriesCount(20)
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-upload-supply-csv-with-x-doc-data.xml",
            assertionMode = NON_STRICT)
    void uploadXDocSupplyWithType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());
        verifyOkXDocSupplyUpload(file, SUPPLY_INBOUND_TYPE);
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request/before.xml", assertionMode = NON_STRICT)
    void uploadXDocSupplyWhileXDocSuppliesAreNotAllowed() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());
        uploadXDocSupply(SUPPLIER_ID, file, null).andExpect(status().isInternalServerError());
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request/before.xml", assertionMode = NON_STRICT)
    void uploadXDocSupplyWhileXDocSuppliesAreNotAllowedWithType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());
        uploadXDocSupply(SUPPLIER_ID, file, SUPPLY_INBOUND_TYPE).andExpect(status().isInternalServerError());
    }

    @Test
    void uploadSupplyWithUnknownExternalOperationType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());
        MvcResult mvcResult = uploadSupplyWithExternalData(SUPPLIER_ID, file, UNKNOWN_EXTERNAL_OPERATION_TYPE, null)
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"There is no ExternalOperationType with id: 666\"," +
                        "\"type\":\"INVALID_OPERATION_TYPE\"}"));
    }

    @Test
    void uploadSupplyWithUnknownExternalOperationTypeWithType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());
        MvcResult mvcResult =
                uploadSupplyWithExternalData(SUPPLIER_ID, file, UNKNOWN_EXTERNAL_OPERATION_TYPE, SUPPLY_INBOUND_TYPE)
                        .andExpect(status().isBadRequest())
                        .andReturn();
        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"There is no ExternalOperationType with id: 666\"," +
                        "\"type\":\"INVALID_OPERATION_TYPE\"}"));
    }

    @Test
    void uploadSupplyWithoutExtension() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", "");
        uploadSupplyWithDate(SUPPLIER_ID, file, null).andExpect(status().isBadRequest());
    }

    @Test
    void uploadSupplyWithoutExtensionWithType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", "");
        uploadSupplyWithDate(SUPPLIER_ID, file, SUPPLY_INBOUND_TYPE).andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void supplierNotFound() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());
        final MvcResult mvcResult = uploadSupplyWithDate(UNKNOWN_SHOP_ID, file, null)
                .andExpect(status().isNotFound()).andReturn();
        assertJsonResponseCorrect("supplier-not-found.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void supplierNotFoundWithType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());
        final MvcResult mvcResult = uploadSupplyWithDate(UNKNOWN_SHOP_ID, file, SUPPLY_INBOUND_TYPE)
                .andExpect(status().isNotFound()).andReturn();
        assertJsonResponseCorrect("supplier-not-found.json", mvcResult);
    }

    @Test
    void uploadWithInvalidDate() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());
        final MvcResult mvcResult =
                upload(
                        SUPPLIER_ID, file, "supply",
                        null, SERVICE_ID, Collections.singletonMap("date", INVALID_DATE.toString())
                )
                        .andExpect(status().isBadRequest())
                        .andReturn();

        assertJsonResponseCorrect("invalid-supply-date-error.json", mvcResult);
    }

    @Test
    void uploadWithInvalidDateWithType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());
        final MvcResult mvcResult =
                upload(
                        SUPPLIER_ID, file, "supply",
                        SUPPLY_INBOUND_TYPE, SERVICE_ID, Collections.singletonMap("date", INVALID_DATE.toString())
                )
                        .andExpect(status().isBadRequest())
                        .andReturn();

        assertJsonResponseCorrect("invalid-supply-date-error.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadWithValidationErrorsCsv() throws Exception {
        MockMultipartFile file = getFileWithErrors(RequestType.SUPPLY, "csv",
                FileExtension.CSV.getMimeType());
        final MvcResult mvcResult = uploadSupplyWithDate(SUPPLIER_ID, file, null)
                .andExpect(status().isBadRequest())
                .andReturn();
        assertJsonResponseCorrect("supply-error-validation-response.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadWithValidationErrorsCsvWithType() throws Exception {
        MockMultipartFile file = getFileWithErrors(RequestType.SUPPLY, "csv",
                FileExtension.CSV.getMimeType());
        final MvcResult mvcResult = uploadSupplyWithDate(SUPPLIER_ID, file, SUPPLY_INBOUND_TYPE)
                .andExpect(status().isBadRequest())
                .andReturn();
        assertJsonResponseCorrect("supply-error-validation-response.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadWithValidationErrorsXls() throws Exception {
        MockMultipartFile file = getFileWithErrors(RequestType.SUPPLY, "xls",
                FileExtension.XLS.getMimeType());
        final MvcResult mvcResult = uploadSupplyWithDate(SUPPLIER_ID, file, null)
                .andExpect(status().isBadRequest())
                .andReturn();
        assertJsonResponseCorrect("supply-error-validation-response.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadWithValidationErrorsXlsWithType() throws Exception {
        MockMultipartFile file = getFileWithErrors(RequestType.SUPPLY, "xls",
                FileExtension.XLS.getMimeType());
        final MvcResult mvcResult = uploadSupplyWithDate(SUPPLIER_ID, file, SUPPLY_INBOUND_TYPE)
                .andExpect(status().isBadRequest())
                .andReturn();
        assertJsonResponseCorrect("supply-error-validation-response.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadWithValidationErrorsXlsx() throws Exception {
        MockMultipartFile file = getFileWithErrors(RequestType.SUPPLY, "xlsx",
                FileExtension.XLSX.getMimeType());
        final MvcResult mvcResult = uploadSupplyWithDate(SUPPLIER_ID, file, null)
                .andExpect(status().isBadRequest())
                .andReturn();
        assertJsonResponseCorrect("supply-error-validation-response.json", mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void uploadWithValidationErrorsXlsxWithType() throws Exception {
        MockMultipartFile file = getFileWithErrors(RequestType.SUPPLY, "xlsx",
                FileExtension.XLSX.getMimeType());
        final MvcResult mvcResult = uploadSupplyWithDate(SUPPLIER_ID, file, SUPPLY_INBOUND_TYPE)
                .andExpect(status().isBadRequest())
                .andReturn();
        assertJsonResponseCorrect("supply-error-validation-response.json", mvcResult);
    }

    @Test
    void ffServiceNotFound() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());

        final MockHttpServletRequestBuilder upload = multipart("/upload-request-file/supply")
                .file(file)
                .param("date", SUPPLY_DATE.toString())
                .param("supplierId", String.valueOf(SUPPLIER_ID))
                .param("serviceId", String.valueOf(UNKNOWN_SERVICE_ID));

        final MvcResult mvcResult = mockMvc.perform(upload)
                .andExpect(status().isNotFound())
                .andReturn();

        assertJsonResponseCorrect("ff-service-not-found.json", mvcResult);
    }

    @Test
    void ffServiceNotFoundWithType() throws Exception {
        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "csv", FileExtension.CSV.getMimeType());

        final MockHttpServletRequestBuilder upload = multipart("/upload-request-file/supply")
                .file(file)
                .param("date", SUPPLY_DATE.toString())
                .param("supplierId", String.valueOf(SUPPLIER_ID))
                .param("serviceId", String.valueOf(UNKNOWN_SERVICE_ID))
                .param("type", String.valueOf(SUPPLY_INBOUND_TYPE));

        final MvcResult mvcResult = mockMvc.perform(upload)
                .andExpect(status().isNotFound())
                .andReturn();

        assertJsonResponseCorrect("ff-service-not-found.json", mvcResult);
    }

    @Test
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-create-supply-with-real-supplier-info.xml",
            assertionMode = NON_STRICT)
    void submitSupplySuccessfullyWithRealSupplier() throws Exception {
        final String data = getJsonFromFile("valid-supply-with-real-supplier-info.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-response-with-real-supplier.json", mvcResult);
    }

    @Test
    void submitSupplyWithRealSupplierWithoutId() throws Exception {
        final String data = getJsonFromFile("invalid-supply-with-real-supplier-info-without-id.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(), equalTo(
                "{\"message\":\"items[0].realSupplier.id must not be blank\"}"
        ));
    }

    @Test
    void submitSupplyWithRealSupplierWithoutName() throws Exception {
        final String data = getJsonFromFile("invalid-supply-with-real-supplier-info-without-name.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(), equalTo(
                "{\"message\":\"items[0].realSupplier.name must not be blank\"}"
        ));
    }

    @Test
    void submitSupplyWithRealSupplierWithoutAnyInfo() throws Exception {
        final String data = getJsonFromFile("valid-supply-with-real-supplier-info-without-anything.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(), equalTo(
                "{\"message\":\"" +
                        "items[0].realSupplier.id must not be blank; " +
                        "items[0].realSupplier.name must not be blank\"}"
        ));
    }

    @Test
    void submitSupplyWithRealSupplierWithDifferentIds() throws Exception {
        final String data = getJsonFromFile("invalid-supply-with-real-supplier-info-with-different-ids.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(), equalTo(
                "{\"message\":\"Encountered more than one real supplier id\"," +
                        "\"type\":\"ITEMS_WITH_DIFFERENT_REAL_SUPPLIER_IDS\"}"
        ));
    }

    @Test
    void submitSupplyWithRealSupplierWithOneSupplierInfoMissing() throws Exception {
        final String data = getJsonFromFile("invalid-supply-with-one-real-supplier-info-missing.json");
        final MvcResult mvcResult = doPostSupplyNoFile(data)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(), equalTo(
                "{\"message\":\"Encountered more than one real supplier id\"," +
                        "\"type\":\"ITEMS_WITH_DIFFERENT_REAL_SUPPLIER_IDS\"}"
        ));
    }

    @Test
    void checkRequestAllowedForSupply() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get("/upload-request/1/is-upload-allowed")
                        .param("requestType", "0")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("check-request-allowed-supply-response.json", mvcResult);
    }

    @Test
    void checkRequestAllowedForWithdraw() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get("/upload-request/1/is-upload-allowed")
                        .param("requestType", "1")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("check-request-allowed-withdraw-response.json", mvcResult);
    }

    @Test
    void checkRequestAllowedForCustomerReturnSupply() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get("/upload-request/1/is-upload-allowed")
                        .param("requestType", "2")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("check-request-allowed-customer-return-supply-response.json", mvcResult);
    }

    @Test
    void checkRequestAllowedForTransfer() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get("/upload-request/1/is-upload-allowed")
                        .param("requestType", "3")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("check-request-allowed-transfer-response.json", mvcResult);
    }

    @Test
    void checkRequestAllowedForValidUnredeemed() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get("/upload-request/1/is-upload-allowed")
                        .param("requestType", "5")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("check-request-allowed-valid-unredeemed-response.json", mvcResult);
    }

    @Test
    void checkRequestAllowedForInvalidUnredeemed() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get("/upload-request/1/is-upload-allowed")
                        .param("requestType", "6")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("check-request-allowed-invalid-unredeemed-response.json", mvcResult);
    }

    @Test
    void checkRequestAllowedForCustomerReturn() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get("/upload-request/1/is-upload-allowed")
                        .param("requestType", "7")
        ).andDo(print()).andExpect(status().isOk()).andReturn();

        assertJsonResponseCorrect("check-request-allowed-customer-return-response.json", mvcResult);
    }

    @Test
    void checkRequestAllowedForShadowSupply() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get("/upload-request/1/is-upload-allowed")
                        .param("requestType", "8")
        ).andDo(print()).andExpect(status().isOk()).andReturn();

        assertJsonResponseCorrect("check-request-allowed-shadow-supply-response.json", mvcResult);
    }

    @Test
    @JpaQueriesCount(15)
    @ExpectedDatabase(value = "classpath:controller/upload-request/after-create-movement-supply.xml",
            assertionMode = NON_STRICT)
    void submitMovementSupplySuccessfully() throws Exception {
        final String data = getJsonFromFile("valid-supply.json");
        final MvcResult mvcResult = doPostMovementSupplyNoFile(data)
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("created-movement-supply-response.json", mvcResult);
    }


    private ResultActions doPostMovementSupplyNoFile(final String data) throws Exception {
        return mockMvc.perform(
                post("/upload-request/movement-supply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(data)
        ).andDo(print());
    }

    private void verifyOkSupplyUpload(final MockMultipartFile file, Integer inboundType) throws Exception {
        uploadSupplyWithDate(SUPPLIER_ID, file, inboundType).andExpect(status().isOk());
        verify(mdsS3Client, times(1)).upload(any(), any());
        verify(mdsS3Client, times(1)).getUrl(any());
    }

    private void verifyOkRequestUpload(final MockMultipartFile file, Integer inboundType, RequestType type,
                                       Long supplyRequestId)
            throws Exception {
        uploadSubRequestWithoutDate(SUPPLIER_ID, file, inboundType, type, supplyRequestId).andExpect(status().isOk());
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

    private void verifyOkWithdrawUploadWithFilledExternalData(final MockMultipartFile file,
                                                              Map<String, String> params) throws Exception {
        upload(SUPPLIER_ID, file, "withdraw", null, SERVICE_ID, params).andExpect(status().isOk());
        verify(mdsS3Client, times(1)).upload(any(), any());
        verify(mdsS3Client, times(1)).getUrl(any());
    }

    private static Map<String, String> withdrawAllParam(final Instant date) {
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

    private static Map<String, String> withdrawParamWithExternalData(final Instant date,
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

    private static Map<String, String> withdrawParamWithExternalDataWithoutContactDetails(
            final Instant date,
            String externalOperationType) {
        return ImmutableMap.<String, String>builder()
                .put("date", date.toString())
                .put("stock", "0")
                .put("consignee", "ООО НЕФТЬ VAPE")
                .put("comment", COMMENT)
                .put("externalRequestId", EXTERNAL_REQUEST_ID)
                .put("externalOperationType", externalOperationType)
                .build();
    }

    private static Map<String, String> withdrawParamWithIgnoreErrorsFlag(final Instant date) {
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

    private ResultActions uploadSupplyWithDate(final long shopId, final MockMultipartFile file,
                                               Integer inboundType) throws Exception {

        final ImmutableMap<String, String> params = ImmutableMap.<String, String>builder()
                .put("date", getDateBasedOnInboundType())
                .put("comment", COMMENT)
                .build();
        return upload(shopId, file, "supply", inboundType, SERVICE_ID, params);
    }

    private ResultActions uploadSubRequestWithoutDate(final long shopId,
                                                      final MockMultipartFile file,
                                                      Integer inboundType,
                                                      RequestType type,
                                                      Long supplyRequestId
    ) throws Exception {

        final ImmutableMap<String, String> params = ImmutableMap.<String, String>builder()
                .put("comment", COMMENT)
                .put("type", type.name())
                .put("supplyRequestId", supplyRequestId.toString())
                .build();
        return upload(shopId, file, "supply", inboundType, SERVICE_ID, params);
    }

    private ResultActions uploadSupplyWithoutDate(final long shopId, final MockMultipartFile file,
                                                  Integer inboundType) throws Exception {
        final ImmutableMap<String, String> params = ImmutableMap.<String, String>builder()
                .put("comment", COMMENT)
                .build();
        return upload(shopId, file, "supply", inboundType, SERVICE_ID, params);
    }

    private ResultActions uploadXDocSupplyWithoutXDocDate(final long shopId,
                                                          final MockMultipartFile file,
                                                          Integer inboundType) throws Exception {
        final ImmutableMap<String, String> params = ImmutableMap.<String, String>builder()
                .put("date", getDateBasedOnInboundType())
                .put("xDocServiceId", String.valueOf(X_DOC_SERVICE_ID))
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

    private MockMultipartFile getValidFileWithoutVat(RequestType type,
                                                     String extension,
                                                     String mimeType) throws IOException {
        return getFile(VALID_FILE_PATH_WITHOUT_VAT, type, extension, mimeType);
    }

    private ResultActions doPostSupplyNoFile(final String data) throws Exception {
        return mockMvc.perform(
                post("/upload-request/supply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(data)
        ).andDo(print());
    }
}
