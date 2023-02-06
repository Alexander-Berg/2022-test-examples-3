package ru.yandex.market.wms.shippingsorter.sorting.controller;

import java.nio.charset.StandardCharsets;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;
import ru.yandex.market.wms.shippingsorter.sorting.utils.JsonAssertUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@Import(ShippingSorterSecurityTestConfiguration.class)
public class SorterExitSettingsControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup("/sorting/controller/sorter-exit-settings-management/successful-creation/before.xml")
    @ExpectedDatabase(value = "/sorting/controller/sorter-exit-settings-management/successful-creation/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldSuccessCreateSettings() throws Exception {
        assertApiCall(
                MockMvcResultMatchers.status().isOk(),
                "sorting/controller/sorter-exit-settings-management/successful-creation/request.json",
                null,
                post("/sorting/sorter-exit-settings")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-exit-settings-management/creation-with-duplicate/before.xml")
    @ExpectedDatabase(value = "/sorting/controller/sorter-exit-settings-management/creation-with-duplicate/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotCreateSettingsIfFindDuplicate() throws Exception {
        assertApiCall(
                MockMvcResultMatchers.status().is4xxClientError(),
                "sorting/controller/sorter-exit-settings-management/creation-with-duplicate/request.json",
                "sorting/controller/sorter-exit-settings-management/creation-with-duplicate/response.json",
                post("/sorting/sorter-exit-settings")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-exit-settings-management/creation-with-multi-shipdate/before.xml")
    @ExpectedDatabase(value = "/sorting/controller/sorter-exit-settings-management/creation-with-multi-shipdate/after" +
            ".xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotCreateSettingsIfMultiShipDateDeniedAndHas() throws Exception {
        assertApiCall(
                MockMvcResultMatchers.status().is4xxClientError(),
                "sorting/controller/sorter-exit-settings-management/creation-with-multi-shipdate/" +
                        "request.json",
                "sorting/controller/sorter-exit-settings-management/creation-with-multi-shipdate/" +
                        "response.json",
                post("/sorting/sorter-exit-settings")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-exit-settings-management/common.xml")
    @ExpectedDatabase(value = "/sorting/controller/sorter-exit-settings-management/common.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotCreateSettingsIfWeightMaxZero() throws Exception {
        assertApiCall(
                MockMvcResultMatchers.status().is4xxClientError(),
                "sorting/controller/sorter-exit-settings-management/weight-max-zero/request.json",
                null,
                post("/sorting/sorter-exit-settings")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-exit-settings-management/common.xml")
    @ExpectedDatabase(value = "/sorting/controller/sorter-exit-settings-management/common.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotCreateSettingsIfWeighMinGreaterThanWeighMax() throws Exception {
        assertApiCall(
                MockMvcResultMatchers.status().is4xxClientError(),
                "sorting/controller/sorter-exit-settings-management/weight-min-greater-than-weight-max/" +
                        "request.json",
                null,
                post("/sorting/sorter-exit-settings")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-exit-settings-management/common.xml")
    @ExpectedDatabase(value = "/sorting/controller/sorter-exit-settings-management/common.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotCreateSettingsIfWeightNull() throws Exception {
        assertApiCall(
                MockMvcResultMatchers.status().is4xxClientError(),
                "sorting/controller/sorter-exit-settings-management/weight-max-and-weight-min-null/" +
                        "request.json",
                null,
                post("/sorting/sorter-exit-settings")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-exit-settings-management/common.xml")
    @ExpectedDatabase(value = "/sorting/controller/sorter-exit-settings-management/common.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotCreateSettingsIfWeightGreaterThanMaxAvailableWeight() throws Exception {
        assertApiCall(
                MockMvcResultMatchers.status().is4xxClientError(),
                "sorting/controller/sorter-exit-settings-management/" +
                        "weight-max-and-weight-min-greater-max-weight/request.json",
                null,
                post("/sorting/sorter-exit-settings")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-exit-settings-management/successful-get/before.xml")
    public void shouldSuccessGetSettings() throws Exception {
        assertApiCall(
                MockMvcResultMatchers.status().isOk(),
                "sorting/controller/sorter-exit-settings-management/successful-get/response.json",
                get("/sorting/sorter-exit-settings/1")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-exit-settings-management/successful-update/before.xml")
    @ExpectedDatabase(value = "/sorting/controller/sorter-exit-settings-management/successful-update/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldSuccessUpdateSettings() throws Exception {
        assertApiCall(
                MockMvcResultMatchers.status().isOk(),
                "sorting/controller/sorter-exit-settings-management/successful-update/request.json",
                "sorting/controller/sorter-exit-settings-management/successful-update/response.json",
                put("/sorting/sorter-exit-settings/1")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-exit-settings-management/common.xml")
    @ExpectedDatabase(value = "/sorting/controller/sorter-exit-settings-management/common.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotSuccessUpdateSettingsIfWeightMinGreaterThanWeightMax() throws Exception {
        assertApiCall(
                MockMvcResultMatchers.status().is4xxClientError(),
                "sorting/controller/sorter-exit-settings-management/" +
                        "update-weight-min-greater-than-weight-max/request.json",
                null,
                put("/sorting/sorter-exit-settings/1")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-exit-settings-management/update-with-multi-shipdate/before.xml")
    @ExpectedDatabase(value = "/sorting/controller/sorter-exit-settings-management/update-with-multi-shipdate/after" +
            ".xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotSuccessUpdateIfMultiShipDateDeniedAndHas() throws Exception {
        assertApiCall(
                MockMvcResultMatchers.status().is4xxClientError(),
                "sorting/controller/sorter-exit-settings-management/update-with-multi-shipdate/request.json",
                null,
                put("/sorting/sorter-exit-settings/1")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-exit-settings-management/successful-delete/before.xml")
    @ExpectedDatabase(value = "/sorting/controller/sorter-exit-settings-management/successful-delete/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldSuccessDeleteSettings() throws Exception {
        mockMvc.perform(delete("/sorting/sorter-exit-settings/1")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST")))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-exit-settings-management/successful-get-all/before.xml")
    @ExpectedDatabase(value = "/sorting/controller/sorter-exit-settings-management/successful-get-all/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldSuccessGetAllSettings() throws Exception {
        assertApiCall(
                MockMvcResultMatchers.status().isOk(),
                "sorting/controller/sorter-exit-settings-management/successful-get-all/response.json",
                get("/sorting/sorter-exit-settings")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-exit-settings-management/creation-with-null-scheduledshipdate/" +
            "before.xml")
    @ExpectedDatabase(value = "/sorting/controller/sorter-exit-settings-management/creation-with-null" +
            "-scheduledshipdate/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldSuccessCreateSettingsWithoutScheduledShipDate() throws Exception {
        assertApiCall(
                MockMvcResultMatchers.status().isOk(),
                "sorting/controller/sorter-exit-settings-management/creation-with-null-scheduledshipdate/" +
                        "request.json",
                null,
                post("/sorting/sorter-exit-settings")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-exit-settings-management/update-with-null-scheduledshipdate/before.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-exit-settings-management/update-with-null-scheduledshipdate/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void shouldSuccessUpdateSettingsWithoutScheduledShipDate() throws Exception {
        assertApiCall(
                MockMvcResultMatchers.status().isOk(),
                "sorting/controller/sorter-exit-settings-management/update-with-null-scheduledshipdate/" +
                        "request.json",
                null,
                put("/sorting/sorter-exit-settings/1")
        );
    }

    private void assertApiCall(ResultMatcher status, String requestFile, String responseFile,
                               MockHttpServletRequestBuilder request)
            throws Exception {
        MvcResult mvcResult = mockMvc.perform(request
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(requestFile)))
                .andExpect(status)
                .andReturn();
        if (responseFile != null) {
            JsonAssertUtils.assertFileNonExtensibleEquals(responseFile,
                    mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        }
    }

    private void assertApiCall(ResultMatcher status,
                               String responseFile,
                               MockHttpServletRequestBuilder request) throws Exception {
        assertApiCall(status, StringUtils.EMPTY, responseFile, request);
    }
}
