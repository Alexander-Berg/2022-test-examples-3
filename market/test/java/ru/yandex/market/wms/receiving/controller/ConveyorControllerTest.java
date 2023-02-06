package ru.yandex.market.wms.receiving.controller;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;
import ru.yandex.market.wms.core.base.request.ZoneSelectionRequest;
import ru.yandex.market.wms.core.base.response.GetChildContainersResponse;
import ru.yandex.market.wms.core.base.response.GetParentContainerResponse;
import ru.yandex.market.wms.core.base.response.ZoneSelectionResponse;
import ru.yandex.market.wms.core.client.CoreClient;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@DatabaseSetup("/controller/crossdock/create-serials/datasets/common.xml")
public class ConveyorControllerTest extends ReceivingIntegrationTest {

    @MockBean
    @Autowired
    private CoreClient coreClient;

    @BeforeEach
    void mockReset() {
        Mockito.reset(coreClient);
    }

    @Test
    @DatabaseSetup("/controller/conveyor/close-container/happy-path/before.xml")
    @ExpectedDatabase(value = "/controller/conveyor/close-container/happy-path/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeContainerHappyPath() throws Exception {
        Mockito.when(coreClient.selectZone(any(ZoneSelectionRequest.class)))
                .thenReturn(new ZoneSelectionResponse("CONV_OUT"));
        Mockito.when(coreClient.getChildContainers(any()))
                .thenReturn(new GetChildContainersResponse(new ArrayList<>()));

        assertApiCallOk(
                "controller/conveyor/close-container/happy-path/request.json",
                post("/conveyor/close-container"),
                "controller/conveyor/close-container/happy-path/response.json");

        Mockito.verify(coreClient)
                .selectZone(argThat(ZoneSelectionRequest::getConsiderEnabledZones));
    }

    @Test
    @DatabaseSetup("/controller/conveyor/close-container/nesting/before.xml")
    @ExpectedDatabase(value = "/controller/conveyor/close-container/nesting/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeContainerWithNesting() throws Exception {
        Mockito.when(coreClient.getParentContainer(any()))
                .thenReturn(new GetParentContainerResponse(null));
        Mockito.when(coreClient.getChildContainers(any()))
                .thenReturn(new GetChildContainersResponse(List.of("RCP01", "RCP02", "RCP03")));

        assertApiCallOk(
                "controller/conveyor/close-container/nesting/request.json",
                post("/conveyor/close-container"),
                "controller/conveyor/close-container/happy-path/response.json");
    }

    @Test
    @DatabaseSetup("/controller/conveyor/close-container-forced/happy-path-nesting/before.xml")
    @ExpectedDatabase(value = "/controller/conveyor/close-container-forced/happy-path-nesting/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeContainerForcedWNesting() throws Exception {
        Mockito.when(coreClient.getChildContainers(any()))
                .thenReturn(new GetChildContainersResponse(List.of("RCP01", "RCP02", "RCP03")));

        assertApiCallOk("controller/conveyor/close-container-forced/happy-path-nesting/request.json",
                post("/conveyor/close-container-forced"));
    }

    @Test
    @DatabaseSetup("/controller/conveyor/close-container-forced/happy-path-nesting-si/before.xml")
    @ExpectedDatabase(value = "/controller/conveyor/close-container-forced/happy-path-nesting-si/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeContainerForcedWNestingSiOnly() throws Exception {
        Mockito.when(coreClient.getChildContainers(any()))
                .thenReturn(new GetChildContainersResponse(List.of("RCP01", "RCP02", "RCP03")));

        assertApiCallOk("controller/conveyor/close-container-forced/happy-path-nesting-si/request.json",
                post("/conveyor/close-container-forced"));
    }

    @Test
    @DatabaseSetup("/controller/conveyor/close-container-forced/happy-path/before.xml")
    @ExpectedDatabase(value = "/controller/conveyor/close-container-forced/happy-path/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeContainerForcedWoNesting() throws Exception {
        Mockito.when(coreClient.getChildContainers(any()))
                .thenReturn(new GetChildContainersResponse(List.of()));

        assertApiCallOk("controller/conveyor/close-container-forced/happy-path/request.json",
                post("/conveyor/close-container-forced"));
    }

    @Test
    @DatabaseSetup("/controller/conveyor/close-container/nesting-error/before.xml")
    @ExpectedDatabase(value = "/controller/conveyor/close-container/nesting-error/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void closeContainerWithNestingError() throws Exception {
        Mockito.when(coreClient.getParentContainer(any()))
                .thenReturn(new GetParentContainerResponse("TM10"));
        Mockito.when(coreClient.getChildContainers(any()))
                .thenReturn(new GetChildContainersResponse(List.of()));

        assertApiCallError(
                "controller/conveyor/close-container/nesting-error/request.json",
                post("/conveyor/close-container"),
                "Тара RCP01 вложена в TM10, сканируйте TM10");
    }

    @Test
    @DatabaseSetup("/controller/conveyor/create-transport-order/happy-path/before.xml")
    @ExpectedDatabase(value = "/controller/conveyor/create-transport-order/happy-path/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createTransportOrderHappyPath() throws Exception {
        Mockito.when(coreClient.getChildContainers(any()))
                .thenReturn(new GetChildContainersResponse(Collections.emptyList()));
        Mockito.when(coreClient.selectZone(any(ZoneSelectionRequest.class)))
                .thenReturn(new ZoneSelectionResponse("CONV_OUT"));

        assertApiCallOk(
                "controller/conveyor/create-transport-order/happy-path/request.json",
                post("/conveyor/create-transport-order"));

        Mockito.verify(coreClient)
                .selectZone(argThat(ZoneSelectionRequest::getConsiderEnabledZones));
    }

    @Test
    @DatabaseSetup("/controller/conveyor/create-transport-order/happy-path-placement-buf/before.xml")
    @ExpectedDatabase(value = "/controller/conveyor/create-transport-order/happy-path-placement-buf/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createTransportOrderFromPlacementBufHappyPath() throws Exception {
        Mockito.when(coreClient.getChildContainers(any()))
                .thenReturn(new GetChildContainersResponse(Collections.emptyList()));
        Mockito.when(coreClient.selectZone(any(ZoneSelectionRequest.class)))
                .thenReturn(new ZoneSelectionResponse("PLC_BUF2"));

        assertApiCallOk(
                "controller/conveyor/create-transport-order/happy-path-placement-buf/request.json",
                post("/conveyor/create-transport-order"));

        Mockito.verify(coreClient)
                .selectZone(argThat((request) -> !request.getConsiderEnabledZones()));
    }

    @Test
    @DatabaseSetup(value = "/controller/conveyor/validate-container/before.xml", type = DatabaseOperation.INSERT)
    void validateContainer() throws Exception {
        String response = "controller/conveyor/validate-container/response-stock.json";
        Mockito.when(coreClient.getChildContainers(any()))
                .thenReturn(new GetChildContainersResponse(Collections.emptyList()));
        MvcResult result = mockMvc.perform(get("/conveyor/validate-container/TM10"))
                .andExpect(status().isOk())
                .andReturn();

        JsonAssertUtils.assertFileNonExtensibleEquals(
                response,
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    @DatabaseSetup(value = "/controller/conveyor/validate-container/before.xml", type = DatabaseOperation.INSERT)
    void validateContainerThermalType() throws Exception {
        String response = "controller/conveyor/validate-container/response-thermal.json";
        Mockito.when(coreClient.getChildContainers(any()))
                .thenReturn(new GetChildContainersResponse(Collections.emptyList()));
        MvcResult result = mockMvc.perform(get("/conveyor/validate-container/TM999"))
                .andExpect(status().isOk())
                .andReturn();

        JsonAssertUtils.assertFileNonExtensibleEquals(
                response,
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    @DatabaseSetup(value = "/controller/conveyor/validate-container/before.xml", type = DatabaseOperation.INSERT)
    void validateParentContainerExpensiveType() throws Exception {
        String response = "controller/conveyor/validate-container/response-expensive.json";
        Mockito.when(coreClient.getChildContainers(any()))
                .thenReturn(new GetChildContainersResponse(List.of("RCP1", "RCP2")));
        MvcResult result = mockMvc.perform(get("/conveyor/validate-container/TM123"))
                .andExpect(status().isOk())
                .andReturn();

        JsonAssertUtils.assertFileNonExtensibleEquals(
                response,
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    @DatabaseSetup(value = "/controller/conveyor/validate-container/before.xml", type = DatabaseOperation.INSERT)
    void validateContainerWrongType() throws Exception {
        String response = "controller/conveyor/validate-container/response-wrong-type.json";
        Mockito.when(coreClient.getChildContainers(any()))
                .thenReturn(new GetChildContainersResponse(Collections.emptyList()));
        MvcResult result = mockMvc.perform(get("/conveyor/validate-container/VS000001"))
                .andExpect(status().is4xxClientError())
                .andReturn();

        JsonAssertUtils.assertFileNonExtensibleEquals(
                response,
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void validateContainerNonConveyable() throws Exception {
        String response = "controller/conveyor/validate-container/response-non-conveyable.json";
        Mockito.when(coreClient.getChildContainers(any()))
                .thenReturn(new GetChildContainersResponse(Collections.emptyList()));
        MvcResult result = mockMvc.perform(get("/conveyor/validate-container/PLT00001"))
                .andExpect(status().is4xxClientError())
                .andReturn();

        JsonAssertUtils.assertFileNonExtensibleEquals(
                response,
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void createTransportOrderWrongContainerId() throws Exception {
        assertApiCallError(
                "controller/conveyor/create-transport-order/wrong-container-id/request.json",
                post("/conveyor/create-transport-order"),
                "Id контейнера должен начинаться на TM!");
    }

    private void assertApiCallOk(String requestFile, MockHttpServletRequestBuilder request) throws Exception {
        assertApiCallOk(requestFile, request, null);
    }

    private void assertApiCallOk(String requestFile, MockHttpServletRequestBuilder request, String responseFile)
            throws Exception {
        MvcResult mvcResult = mockMvc.perform(request
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(requestFile)))
                .andExpect(status().isOk())
                .andReturn();

        if (StringUtils.isNotBlank(responseFile)) {
            JsonAssertUtils.assertFileNonExtensibleEquals(responseFile,
                    mvcResult.getResponse().getContentAsString());
        }
    }

    private void assertApiCallError(String requestFile, MockHttpServletRequestBuilder request, String errorInfo)
            throws Exception {
        MvcResult mvcResult = mockMvc.perform(request
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(requestFile)))
                .andExpect(status().is4xxClientError())
                .andReturn();
        assertions.assertThat(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains(errorInfo);
    }
}
