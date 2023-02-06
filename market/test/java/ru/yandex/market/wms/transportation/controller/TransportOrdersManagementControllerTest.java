package ru.yandex.market.wms.transportation.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient;
import ru.yandex.market.wms.common.spring.servicebus.model.request.CreateTransportOrderRequest;
import ru.yandex.market.wms.common.spring.servicebus.model.response.VendorApiResponse;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

class TransportOrdersManagementControllerTest extends IntegrationTest {

    @Autowired
    @SpyBean
    private ServicebusClient servicebusClient;

    private final ArgumentCaptor<CreateTransportOrderRequest> createTransportOrderCaptor =
            ArgumentCaptor.forClass(CreateTransportOrderRequest.class);

    @BeforeEach
    void setUp() {
        Mockito.reset(servicebusClient);
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/successful-create-manual-transport-order/initial-state.xml")
    @ExpectedDatabase(value = "/controller/transport-order-management/successful-create-manual-transport-order/final" +
            "-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testSuccessfulCreateManualTransportOrder() throws Exception {
        mockMvc.perform(post("/transport-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/transport-order-management/successful-create-manual-transport-order/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/transport-order-management/successful-create-manual-transport-order/response" +
                                ".json")));
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/successful-create-automatic-transport-order/initial-state" +
            ".xml")
    @ExpectedDatabase(value = "/controller/transport-order-management/successful-create-automatic-transport-order" +
            "/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testSuccessCreateAutomaticTransportOrder() throws Exception {
        when(servicebusClient.createTransportOrder(any())).thenReturn(
                VendorApiResponse.builder().code("200").message("").build());
        mockMvc.perform(post("/transport-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/transport-order-management/successful-create-automatic-transport-order/request" +
                                ".json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/transport-order-management/successful-create-automatic-transport-order/response" +
                                ".json")));

        verify(servicebusClient, times(1)).createTransportOrder(createTransportOrderCaptor.capture());

        assertSoftly(assertions -> {
            CreateTransportOrderRequest request = createTransportOrderCaptor.getValue();

            assertions.assertThat(request).isNotNull();
            assertions.assertThat(request.getTransportUnitId()).isNotNull();
            assertions.assertThat(request.getTransportUnitId().getId()).isEqualTo("1");
            assertions.assertThat(request.getTargetLocation()).isNotNull();
            assertions.assertThat(request.getTargetLocation().getId()).isEqualTo("cons_01-01");
        });
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/successful-create-automatic-transport-order/initial-state" +
            ".xml")
    @ExpectedDatabase(value = "/controller/transport-order-management/successful-create-automatic-transport-order" +
            "/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testUnsuccessfulCreatingTransportOrderForErroneousVendorApiResponse() throws Exception {
        when(servicebusClient.createTransportOrder(any())).thenReturn(
                VendorApiResponse.builder().code("401").message("Tu doesn't exist in Wamas").build());
        mockMvc.perform(post("/transport-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/transport-order-management/successful-create-automatic-transport-order/request" +
                                ".json")))
                .andExpect(status().is5xxServerError());

        verify(servicebusClient, times(1)).createTransportOrder(createTransportOrderCaptor.capture());

        assertSoftly(assertions -> {
            CreateTransportOrderRequest request = createTransportOrderCaptor.getValue();

            assertions.assertThat(request).isNotNull();
            assertions.assertThat(request.getTransportUnitId()).isNotNull();
            assertions.assertThat(request.getTransportUnitId().getId()).isEqualTo("1");
            assertions.assertThat(request.getTargetLocation()).isNotNull();
            assertions.assertThat(request.getTargetLocation().getId()).isEqualTo("cons_01-01");
        });
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/filter/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/transport-order-management/filter/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testListOrdersReturnsLastPageWithEmptyCursor() throws Exception {
        ResultActions result = mockMvc.perform(get("/transport-orders")
                .param("cursor",
                        "PMRGG5LSONXXES3FPERDUIRWMQ4DAOLFGYYC2ZBX" +
                                "GA3S2MJRMVQS2OJVGUYC2YJZGU2TGYJXMIYDKNZWE" +
                                "IWCEY3VOJZW64SGNFSWYZBCHJ5SE3TBNVSSEORCON2G" +
                                "C5DVOMRCYITWMFWHKZJCHIRESTS7KBJE6R2SIVJVGIT5PU======")
                .param("sort", "status")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/transport-order-management/filter/last-page-empty-cursor/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/filter/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/transport-order-management/filter/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testListOrdersSequentialPageLoadsFromFirstPage() throws Exception {
        String cursor = "";
        for (int i = 0; i < 3; i++) {
            ResultActions result = mockMvc.perform(get("/transport-orders")
                    .param("cursor", cursor)
                    .param("limit", "2")
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk())
                    .andExpect(content().json(getFileContent(String.format(
                            "controller/transport-order-management/filter/sequential-page-loads/response-%s.json",
                            i + 1))));

            JSONObject obj = new JSONObject(result.andReturn().getResponse().getContentAsString());
            assertions.assertThat(obj).isNotNull();

            cursor = obj.getJSONObject("cursor").getString("value");
        }
        assertions.assertThat(cursor).isEmpty();
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/filter/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/transport-order-management/filter/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testListOrdersSequentialPageLoadsFromFirstPageWithSortBySourceLoc() throws Exception {
        String cursor = "";
        for (int i = 0; i < 3; i++) {
            ResultActions result = mockMvc.perform(get("/transport-orders")
                    .param("cursor", cursor)
                    .param("limit", "2")
                    .param("sort", "fromLoc")
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk())
                    .andDo(print())
                    .andExpect(content().json(getFileContent(String.format(
                            "controller/transport-order-management/filter/sequential-page-loads-with-sort/response-%s" +
                                    ".json", i + 1))));

            JSONObject obj = new JSONObject(result.andReturn().getResponse().getContentAsString());
            assertions.assertThat(obj).isNotNull();

            cursor = obj.getJSONObject("cursor").getString("value");
        }
        assertions.assertThat(cursor).isEmpty();
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/successful-get-incomplete-orders/immutable-state.xml")
    @ExpectedDatabase(
            value = "/controller/transport-order-management/successful-get-incomplete-orders/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testIncompleteOrdersGetControllerReturnsOnlyIncompleteTransportOrders() throws Exception {
        ResultActions result = mockMvc.perform(get("/transport-orders/not-completed-by-user")
                .param("user", "AD1")
                .contentType(MediaType.APPLICATION_JSON));

        String responseFile = "controller/transport-order-management/successful-get-incomplete-orders/response.json";
        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(responseFile)));
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/filter/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/transport-order-management/filter/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testListOrdersSortByStatusAsc() throws Exception {
        ResultActions result = mockMvc.perform(get("/transport-orders")
                .param("sort", "status")
                .param("limit", "4")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/transport-order-management/filter/sort-by-status/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/filter/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/transport-order-management/filter/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testListOrdersSortByUnitKeyDesc() throws Exception {
        ResultActions result = mockMvc.perform(get("/transport-orders")
                .param("sort", "unitKey")
                .param("order", "desc")
                .param("limit", "4")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/transport-order-management/filter/sort-by-unitkey-desc/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/filter/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/transport-order-management/filter/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testListOrdersSortByTypeDesc() throws Exception {
        ResultActions result = mockMvc.perform(get("/transport-orders")
                .param("sort", "type")
                .param("order", "desc")
                .param("limit", "4")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/transport-order-management/filter/sort-by-type-desc/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/successful-get-transport-order/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/transport-order-management/successful-get-transport-order/immutable-state" +
            ".xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testSuccessfulGetTransportOrder() throws Exception {
        mockMvc.perform(get("/transport-orders/6d809e60-d707-11ea-9550-a9553a7b0571")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/transport-order-management/successful-get-transport-order/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/successful-update-manual-transport-order/initial-state.xml")
    @ExpectedDatabase(value = "/controller/transport-order-management/successful-update-manual-transport-order/final" +
            "-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testSuccessfulUpdateOfManualTransportOrder() throws Exception {
        mockMvc.perform(put("/transport-orders/6d809e60-d707-11ea-9550-a9553a7b0571")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/transport-order-management/successful-update-manual-transport-order/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/filter/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/transport-order-management/filter/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testFilterByAssigneeAndStatus() throws Exception {
        ResultActions result = mockMvc.perform(get("/transport-orders")
                .param("sort", "status")
                .param("order", "desc")
                .param("filter", "assignee==AD1;(status==NEW,status==IN_PROGRESS)")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/transport-order-management/filter/by-assignee-and-status/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/filter/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/transport-order-management/filter/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testFilterByEditDate() throws Exception {
        ResultActions result = mockMvc.perform(get("/transport-orders")
                .param("filter", "editDate=ge=2020-04-02;editDate=le=2020-04-10")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/transport-order-management/filter/by-editdate/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/filter/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/transport-order-management/filter/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testFilterByNonExistingFieldReturnsError() throws Exception {
        mockMvc.perform(get("/transport-orders")
                .param("sort", "PRIORITY")
                .param("order", "DESC")
                .param("filter", "assigneesssss=ge=5;(status==CANCELED,status==IN_PROGRESS)")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError())
                .andExpect(content().json(getFileContent(
                        "controller/transport-order-management/filter/error-for-non-existing-field/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/unsuccessful-update-automatic-transport-order/assignee" +
            "/initial-state.xml")
    @ExpectedDatabase(value = "/controller/transport-order-management/unsuccessful-update-automatic-transport-order" +
            "/assignee/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testUnsuccessfulAssigneeUpdateForAutomaticTransportOrder() throws Exception {
        mockMvc.perform(put("/transport-orders/6d809e60-d707-11ea-9550-a9553a7b0571")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/transport-order-management/unsuccessful-update-automatic-transport-order/assignee" +
                                "/request.json")))
                .andExpect(status().is5xxServerError())
                .andExpect(content().json(getFileContent(
                        "controller/transport-order-management/unsuccessful-update-automatic-transport-order/assignee" +
                                "/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/unsuccessful-update-automatic-transport-order/priority" +
            "/initial-state.xml")
    @ExpectedDatabase(value = "/controller/transport-order-management/unsuccessful-update-automatic-transport-order" +
            "/priority/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testUnsuccessfulPriorityUpdateForAutomaticTransportOrder() throws Exception {
        mockMvc.perform(put("/transport-orders/6d809e60-d707-11ea-9550-a9553a7b0571")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/transport-order-management/unsuccessful-update-automatic-transport-order/priority" +
                                "/request.json")))
                .andExpect(status().is5xxServerError())
                .andExpect(content().json(getFileContent(
                        "controller/transport-order-management/unsuccessful-update-automatic-transport-order/priority" +
                                "/response.json")));
    }

    /*
     * Отмена автоматического транспортного ордера
     */
    @Test
    @DatabaseSetup("/controller/transport-order-management/successful-cancel-transport-order/initial-state.xml")
    @ExpectedDatabase(
            value = "/controller/transport-order-management/successful-cancel-transport-order/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testSuccessfulCancelAutomationTransportOrder() throws Exception {
        mockMvc.perform(delete("/transport-orders/active/1cd02aae-257d-11eb-adc1-0242ac120002")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(servicebusClient, times(1)).deleteTransportOrder(any());
    }

    /*
     * Отмена нескольких транспортных ордеров
     */
    @Test
    @DatabaseSetup("/controller/transport-order-management/successful-cancel-batch-transport-orders/initial-state.xml")
    @ExpectedDatabase(
            value = "/controller/transport-order-management/successful-cancel-batch-transport-orders/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testSuccessfulCancelAutomationTransportOrdersBatch() throws Exception {
        when(servicebusClient.deleteTransportOrder(any())).thenReturn(
                VendorApiResponse.builder().code("200").message("OK").build());

        mockMvc.perform(delete("/transport-orders/active")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/transport-order-management/successful-cancel-batch-transport-orders" +
                                "/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/transport-order-management/successful-cancel-batch-transport-orders" +
                                "/response.json")));

        verify(servicebusClient, times(3)).deleteTransportOrder(any());
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/successful-cancel-batch-transport-orders/initial-state.xml")
    @ExpectedDatabase(
            value = "/controller/transport-order-management/successful-cancel-batch-transport-orders/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCancelAutomationTransportOrdersBatchOrdersNotFound() throws Exception {
        when(servicebusClient.deleteTransportOrder(any())).thenReturn(
                VendorApiResponse.builder().code("401").message("There is no TU").build());

        mockMvc.perform(delete("/transport-orders/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/transport-order-management/successful-cancel-batch-transport-orders" +
                                        "/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/transport-order-management/successful-cancel-batch-transport-orders" +
                                "/response.json")));

        verify(servicebusClient, times(3)).deleteTransportOrder(any());
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/successful-cancel-batch-transport-orders/initial-state.xml")
    @ExpectedDatabase(
            value = "/controller/transport-order-management/successful-cancel-batch-transport-orders/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCancelAutomationTransportOrdersBatchUnsuccessful() throws Exception {
        when(servicebusClient.deleteTransportOrder(any())).thenThrow(RuntimeException.class);

        mockMvc.perform(delete("/transport-orders/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/transport-order-management/successful-cancel-batch-transport-orders" +
                                        "/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/transport-order-management/unsuccessful-cancel-batch-transport-orders" +
                                "/response.json")));

        verify(servicebusClient, times(3)).deleteTransportOrder(any());
    }

    /*
     * Отмена ручного транспортного ордера
     */
    @Test
    @DatabaseSetup("/controller/transport-order-management/successful-cancel-manual-transport-order/initial-state.xml")
    @ExpectedDatabase(
            value = "/controller/transport-order-management/successful-cancel-manual-transport-order/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testSuccessfulCancelManualTransportOrder() throws Exception {
        mockMvc.perform(delete("/transport-orders/active/1cd02aae-257d-11eb-adc1-0242ac120002")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(servicebusClient, times(0)).deleteTransportOrder(any());
    }

    @Test
    @DatabaseSetup("/controller/transport-order-management/successful-create-manual-to-zones/initial-state.xml")
    @ExpectedDatabase(value = "/controller/transport-order-management/successful-create-manual-to-zones/final-state" +
            ".xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testSuccessfulCreateZonesTransportOrder() throws Exception {
        mockMvc.perform(post("/transport-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/transport-order-management/successful-create-manual-to-zones/request.json")))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(content().json(getFileContent(
                        "controller/transport-order-management/successful-create-manual-to-zones/response.json")));
    }
}
