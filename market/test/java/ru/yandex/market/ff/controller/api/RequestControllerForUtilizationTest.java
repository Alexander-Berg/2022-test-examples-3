package ru.yandex.market.ff.controller.api;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ff.base.MvcIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class RequestControllerForUtilizationTest extends MvcIntegrationTest {

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests-for-utilization.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests-for-utilization.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void findRequests() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/requests")
                        .param("types", "3", "12")
                        .param("serviceIds", "100")
                        .param("shopIds", "1")
                        .param("stockTypeTo", "4")
        ).andReturn();

        assertResponseEquals(result, "controller/request-api/response/search_utilization_result.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests-for-utilization.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests-for-utilization.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void findOnlyTransferRequests() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/requests")
                        .param("types", "3")
                        .param("serviceIds", "100")
                        .param("shopIds", "1")
                        .param("stockTypeTo", "4")
        ).andReturn();

        assertResponseEquals(result, "controller/request-api/response/search_utilization_transfers_result.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests-for-utilization.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests-for-utilization.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void findOnlyInternalRequests() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/requests")
                        .param("types", "12")
                        .param("notNullSupplier", "true")
        ).andReturn();

        assertResponseEquals(result, "controller/request-api/response/search_utilization_only_internal_result.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests-for-utilization.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests-for-utilization.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void findRequestDetails() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/requests/11")
        ).andReturn();

        assertResponseEquals(result, "controller/request-api/response/get_utilization_details.json");
    }
}
