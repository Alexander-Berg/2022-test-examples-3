package ru.yandex.market.ff.controller.api;

import java.io.IOException;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.configuration.FeatureToggleTestConfiguration;
import ru.yandex.market.ff.util.FileContentUtils;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ParametersAreNonnullByDefault
class RequestRegistryControllerTest extends MvcIntegrationTest {

    @Autowired
    private FeatureToggleTestConfiguration.FTConfig ftConfig;

    @Autowired
    private CalendaringServiceClientApi calendaringServiceClient;

    @BeforeEach
    void init() {
        reset(calendaringServiceClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/db-state.xml")
    void registriesSuccessGetMany() throws Exception {
        mockMvc.perform(get("/requests/1/registries"))
                .andExpect(status().isOk())
                .andExpect(content().json(FileContentUtils.getFileContent(
                        "controller/registry/1/response.json")));
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/db-state.xml")
    void registriesSuccessGetOne() throws Exception {
        mockMvc.perform(get("/requests/2/registries"))
                .andExpect(status().isOk())
                .andExpect(content().json(FileContentUtils.getFileContent(
                        "controller/registry/2/response.json")));
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/db-state.xml")
    void registriesSuccessGetNone() throws Exception {
        mockMvc.perform(get("/requests/3/registries"))
                .andExpect(status().isOk())
                .andExpect(content().json(FileContentUtils.getFileContent(
                        "controller/registry/3/response.json")));
    }

    @Test
    @DatabaseSetup(value = "classpath:controller/registry/delete-all.xml", type = DatabaseOperation.DELETE_ALL)
    void registries404NoRequest() throws Exception {
        mockMvc.perform(get("/requests/1/registries"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(FileContentUtils.getFileContent(
                        "controller/registry/request-not-found.json")));
    }

    @Test
    void acceptSuccessfulFFGetInbound() throws Exception {
        mockMvc.perform(
                put("/requests/1/accept-successful-ff-get-inbound")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(getJsonFromFile("controller/registry/4/request.json")))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void acceptSuccessfulFFGetOutbound() throws Exception {
        mockMvc.perform(
                put("/requests/1/accept-successful-ff-get-outbound")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(getJsonFromFile("controller/registry/7/request.json")))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void acceptSuccessfulRegistry() throws Exception {
        mockMvc.perform(
                put("/requests/accept-successful-registry")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(getJsonFromFile("controller/registry/31/request.json")))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/6/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/registry/6/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptErrorFFGetInbound() throws Exception {
        executeRequest("/requests/1/accept-error-ff-get-inbound", "controller/registry/6/request.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/9/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/registry/9/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptErrorFFGetOutbound() throws Exception {
        executeRequest("/requests/1/accept-error-ff-get-outbound", "controller/registry/9/request.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/19/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/registry/19/after.xml",
            assertionMode = NON_STRICT
    )
    void putFFInboundRegistry() throws Exception {
        executeRequest("/requests/ff-inbound-register", "controller/registry/19/request.json",
                "controller/registry/19/response.json", status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/20/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/registry/20/after.xml",
            assertionMode = NON_STRICT
    )
    void putFFInboundRegistryWithValidationErrors() throws Exception {
        executeRequest("/requests/ff-inbound-register", "controller/registry/20/request.json",
                "controller/registry/20/response.json", status().isBadRequest());
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/21/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/registry/21/after.xml",
            assertionMode = NON_STRICT
    )
    void putFFInboundRegistryWithNotPlannedTypeError() throws Exception {
        executeRequest("/requests/ff-inbound-register", "controller/registry/21/request.json",
                "controller/registry/not-planned-type.json", status().isBadRequest());
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/39/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/registry/39/after.xml",
            assertionMode = NON_STRICT
    )
    void putFFInboundRegistryWithShopRequestProcessedStatus() throws Exception {
        executeRequest("/requests/ff-inbound-register", "controller/registry/39/request.json",
                "controller/registry/39/response.json", status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/22/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/registry/22/after.xml",
            assertionMode = NON_STRICT
    )
    void putFFOutboundRegistry() throws Exception {
        executeRequest("/requests/ff-outbound-register", "controller/registry/22/request.json",
                "controller/registry/22/response.json", status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/23/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/registry/23/after.xml",
            assertionMode = NON_STRICT
    )
    void putFFOutboundRegistryWithValidationErrors() throws Exception {
        executeRequest("/requests/ff-outbound-register", "controller/registry/23/request.json",
                "controller/registry/23/response.json", status().isBadRequest());
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/24/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/registry/24/after.xml",
            assertionMode = NON_STRICT
    )
    void putFFOutboundRegistryWithNotPlannedTypeError() throws Exception {
        executeRequest("/requests/ff-outbound-register", "controller/registry/24/request.json",
                "controller/registry/not-planned-type.json", status().isBadRequest());
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/33/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/registry/33/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptErrorRegistry() throws Exception {
        executeRequest("/requests/accept-error-registry", "controller/registry/33/request.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/34/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/registry/34/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptErrorRegistryWithCalendaringError() throws Exception {
        executeRequest("/requests/accept-error-registry", "controller/registry/34/request.json");
        verify(calendaringServiceClient, times(0))
                .getSlotByExternalIdentifiers(any(), any(), any());
    }

    @Test
    @DatabaseSetup(value = "classpath:controller/registry/36/before.xml")
    void getRegistryUnits() throws Exception {
        MvcResult result = mockMvc.perform(get("/requests/1/registry-units").param("registryIds", "1"))
            .andExpect(status().isOk())
            .andReturn();

        assertResultExpected(result, "controller/registry/36/response.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/37/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/registry/37/after.xml",
            assertionMode = NON_STRICT
    )
    void putRegistryWithEmptyPalletAndBoxCounts() throws Exception {
        executeRequest("/requests/ff-inbound-register", "controller/registry/37/request.json",
                "controller/registry/37/response.json", status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/38/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/registry/38/before.xml",
            assertionMode = NON_STRICT
    )
    void putFFInboundRegistryForRequestInIncorrectStatus() throws Exception {
        executeRequest("/requests/ff-inbound-register", "controller/registry/38/request.json",
                "controller/registry/38/response.json", status().isBadRequest());
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/discrepancy/requests.xml")
    void registryUnitDiscrepancyPage0Test() throws Exception {
        MvcResult result = mockMvc.perform(get("/requests/1/registry-discrepancy")
                .param("page", "0")
                .param("size", "3")
                .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andDo(print()).andReturn();

        assertResultExpected(result, "controller/registry/discrepancy/page0.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/discrepancy/requests.xml")
    void registryUnitDiscrepancyPage1Test() throws Exception {
        MvcResult result = mockMvc.perform(get("/requests/1/registry-discrepancy")
                .param("page", "1")
                .param("size", "3")
                .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andDo(print()).andReturn();

        assertResultExpected(result, "controller/registry/discrepancy/page1.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/discrepancy/requests.xml")
    void registryUnitDiscrepancyPage2Test() throws Exception {
        MvcResult result = mockMvc.perform(get("/requests/1/registry-discrepancy")
                .param("page", "2")
                .param("size", "3")
                .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andDo(print()).andReturn();

        assertResultExpected(result, "controller/registry/discrepancy/page2.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/registry/discrepancy/factual-acceptance/requests.xml")
    void registryUnitDiscrepancyForFactualAcceptance() throws Exception {

        ftConfig.setSupplyEnabled(true);

        MvcResult result = mockMvc.perform(get("/requests/1/registry-discrepancy")
                .param("page", "2")
                .param("size", "3")
                .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andDo(print()).andReturn();

        assertResultExpected(result, "controller/registry/discrepancy/factual-acceptance/response.json");
    }

    private void executeRequest(String url, String contentFileName) throws Exception {
        mockMvc.perform(
                put(url)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(getJsonFromFile(contentFileName)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }

    private void executeRequest(String url,
                                String contentFileName,
                                String response,
                                ResultMatcher resultMatcher) throws Exception {
        MvcResult result = mockMvc.perform(
                put(url)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(getJsonFromFile(contentFileName)))
                .andDo(print())
                .andExpect(resultMatcher)
                .andReturn();

        assertResponseEquals(result, response);
    }

    private void assertResultExpected(MvcResult result, String path) throws IOException {
        String expected = getJsonFromFile(path);
        JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    private String getJsonFromFile(final String name) throws IOException {
        return FileContentUtils.getFileContent(name);
    }
}
