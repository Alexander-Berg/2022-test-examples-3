package ru.yandex.market.wms.api.controller;

import java.util.HashSet;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class NSqlConfigControllerTest extends IntegrationTest {

    private static final String DB_PATH = "/nsqlconfig/db.xml";

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getMultipleNSqlValues() throws Exception {
        assertGetNSqlValues(
                "nsqlconfig/get-values/response/multiple-values-response.json",
                status().is2xxSuccessful(),
                "KEY2", "KEY3");
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getNSqlValue() throws Exception {
        assertGetNSqlValues(
                "nsqlconfig/get-values/response/one-value-response.json",
                status().is2xxSuccessful(),
                "KEY2");
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getManyNSqlValues() throws Exception {
        assertGetNSqlValues(
                "nsqlconfig/get-values/response/many-values-response.json",
                status().is2xxSuccessful(),
                "KEY1", "KEY2", "KEY3");
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getNonexistentNSqlValue() throws Exception {
        assertGetNSqlValues(
                "nsqlconfig/get-values/response/empty-response.json",
                status().is2xxSuccessful(),
                "KEY5", "KEY6"
        );
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getSameNSqlValues() throws Exception {
        assertGetNSqlValues(
                "nsqlconfig/get-values/response/one-value-response.json",
                status().is2xxSuccessful(),
                "KEY2", "KEY2");
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getExistentAndNonNSqlValues() throws Exception {
        assertGetNSqlValues(
                "nsqlconfig/get-values/response/one-value-response.json",
                status().is2xxSuccessful(),
                "KEY2", "KEY6");
    }

    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void getNoNSqlValues() throws Exception {
        assertGetNSqlValues(
                "nsqlconfig/get-values/response/empty-parameter-response.json",
                status().is4xxClientError(),
                "");
    }

    private void assertGetNSqlValueHistory(String responseFile, ResultMatcher status, String... configkeys)
            throws Exception {
        mockMvc.perform(get("/nsqlconfig/values/history")
                        .param("configkeys", configkeys)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status)
                .andExpect(content().json(getFileContent(responseFile)))
                .andReturn();
    }

    private void assertGetNSqlValues(String responseFile, ResultMatcher status, String... configkeys) throws Exception {
        mockMvc.perform(get("/nsqlconfig/values")
                        .param("configkeys", configkeys)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status)
                .andExpect(content().json(getFileContent(responseFile)))
                .andReturn();
    }

    private void assertGetAllSqlValues(String responseFile, ResultMatcher status) throws Exception {
        mockMvc.perform(get("/nsqlconfig/values/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status)
                .andExpect(content().json(getFileContent(responseFile)))
                .andReturn();
    }

    /**
     * Updating all known values
     */
    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = "/nsqlconfig/update-values/db/update-values-all.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void updateNSqlValuesAll() throws Exception {
        updateNSqlValues(
                getFileContent("nsqlconfig/update-values/request/update-values-all.json"),
                status().is2xxSuccessful());
    }

    /**
     * Updating 0 values
     */
    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    void updateNSqlValuesEmpty() throws Exception {
        updateNSqlValues("{}", status().is2xxSuccessful());
    }

    /**
     * Updating one value
     */
    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = "/nsqlconfig/update-values/db/update-value.xml", assertionMode = NON_STRICT_UNORDERED)
    void updateNSqlValue() throws Exception {
        updateNSqlValues(
                getFileContent("nsqlconfig/update-values/request/update-value.json"),
                status().is2xxSuccessful());
    }

    /**
     * Trying to update configs with invalid values (less than lower bound)
     */
    @Test
    void updateNSqlValuesLessThanLowerBound() throws Exception {
        Set<String> expectedErrors = new HashSet<>();
        expectedErrors.add("replenishmentSchedulerStartHour: must be greater than or equal to 0");
        expectedErrors.add("replenishmentMaxVolumeToReduction: must be greater than or equal to 0");
        expectedErrors.add("replenishmentExcessItemPercent: must be greater than or equal to 0");
        expectedErrors.add("replenishmentAllowFullContainerPicking: must be greater than or equal to 0");
        expectedErrors.add("replenishmentMinItemAmountPercent: must be greater than or equal to 0");
        expectedErrors.add("replenishmentSchedulerPeriodHours: must be greater than or equal to 1");
        expectedErrors.add("replenishmentMaxBatchTasks: must be greater than or equal to 0");
        expectedErrors.add("replenishmentAheadDays: must be greater than or equal to 0");
        expectedErrors.add("replenishmentMaxActiveContainerDescentTasks: must be greater than or equal to 0");
        expectedErrors.add("replenishmentSalesStatisticsPeriodDays: must be greater than or equal to 0");
        expectedErrors.add("replenishmentMaxItemsToReduction: must be greater than or equal to 0");
        String response = updateNSqlValues(
                getFileContent("nsqlconfig/update-values/request/update-values-less-than-lower-bound.json"),
                status().is4xxClientError());
        Assertions.assertTrue(expectedErrors.stream().allMatch(response::contains));
    }

    /**
     * Trying to update configs with invalid values (greater than upper bound)
     */
    @Test
    void updateNSqlValuesGreaterThanUpperBound() throws Exception {
        Set<String> expectedErrors = new HashSet<>();
        expectedErrors.add("replenishmentSchedulerStartHour: must be less than or equal to 23");
        expectedErrors.add("replenishmentAllowFullContainerPicking: must be less than or equal to 1");
        expectedErrors.add("replenishmentMinItemAmountPercent: must be less than or equal to 100");
        expectedErrors.add("replenishmentSchedulerPeriodHours: must be less than or equal to 24");
        String response = updateNSqlValues(
                getFileContent("nsqlconfig/update-values/request/update-values-greater-than-upper-bound.json"),
                status().is4xxClientError());
        Assertions.assertTrue(expectedErrors.stream().allMatch(response::contains));
    }

    /**
     * Trying to update config not in {@link ru.yandex.market.wms.api.model.dto.NSqlConfigDTO}
     */
    @Test
    void updateNSqlValuesUnknown() throws Exception {
        String response = updateNSqlValues(
                getFileContent("nsqlconfig/update-values/request/update-values-unknown.json"),
                status().is4xxClientError());
        Assertions.assertTrue(response.contains("Unrecognized field \\\"UNKNOWN_NSQLCONFIG_KEY\\\""));
    }

    /**
     * Trying to update values with wrong type
     */
    @Test
    void updateNSqlValuesWrongType() throws Exception {
        String response = updateNSqlValues(
                "{\"YM_REP_SALES_STAT_PERIOD_DAYS\" : 0.1}",
                status().is4xxClientError());
        Assertions.assertTrue(response.contains("Cannot coerce Floating-point value (0.1) " +
                "to `java.lang.Integer` value"));

        response = updateNSqlValues(
                "{\"YM_REP_SALES_STAT_PERIOD_DAYS\" : \"zero\"}",
                status().is4xxClientError());
        Assertions.assertTrue(response.contains("Cannot deserialize value of type `int` " +
                "from String \"zero\""));
    }

    private String updateNSqlValues(String requestBody, ResultMatcher status) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/nsqlconfig/values")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status)
                .andReturn();
        return mvcResult.getResponse().getContentAsString();
    }
}
