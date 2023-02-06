package ru.yandex.market.wms.picking.modules.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ContainerStatusControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup("/controller/container-status/no-containers/before.xml")
    @ExpectedDatabase(value = "/controller/container-status/no-containers/before.xml", assertionMode = NON_STRICT)
    public void containerStatusNoContainers() throws Exception {
        callApiSuccess("controller/container-status/no-containers/response.json");
    }

    @Test
    @DatabaseSetup("/controller/container-status/few-containers/before.xml")
    @ExpectedDatabase(value = "/controller/container-status/few-containers/before.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void containerStatusFewContainersDifferentStatuses() throws Exception {
        callApiSuccess("controller/container-status/few-containers/response.json");
    }

    @Test
    @DatabaseSetup("/controller/container-status/all-empty/before.xml")
    @ExpectedDatabase(value = "/controller/container-status/all-empty/before.xml", assertionMode = NON_STRICT)
    public void containerStatusAllEmpty() throws Exception {
        callApiSuccess("controller/container-status/all-empty/response.json");
    }

    @Test
    @DatabaseSetup("/controller/container-status/all-full/before.xml")
    @ExpectedDatabase(value = "/controller/container-status/all-full/before.xml", assertionMode = NON_STRICT)
    public void containerStatusAllFull() throws Exception {
        callApiSuccess("controller/container-status/all-full/response.json");
    }

    @Test
    @DatabaseSetup("/controller/container-status/sorted-order/before.xml")
    @ExpectedDatabase(value = "/controller/container-status/sorted-order/before.xml", assertionMode = NON_STRICT)
    public void containerStatusSortedOrder() throws Exception {
        callApiSuccess("controller/container-status/sorted-order/response.json");
    }

    @Test
    @DatabaseSetup("/controller/container-status/overweight/before.xml")
    public void containerStatusOverweight() throws Exception {
        callApiSuccess("controller/container-status/overweight/response.json");
    }

    private void callApiSuccess(final String responseFile) throws Exception {
        final MvcResult mvcResult = mockMvc.perform(get("/container-status"))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        JsonAssertUtils.assertFileNonExtensibleEquals(responseFile,
                mvcResult.getResponse().getContentAsString());
    }
}
