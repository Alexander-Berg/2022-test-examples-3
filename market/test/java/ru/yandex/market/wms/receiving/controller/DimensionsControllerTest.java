package ru.yandex.market.wms.receiving.controller;

import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.dimensionmanagement.client.DimensionManagementClient;
import ru.yandex.market.wms.dimensionmanagement.core.dto.MeasurementOrderDto;
import ru.yandex.market.wms.dimensionmanagement.core.request.GetActiveMeasurementOrderRequest;
import ru.yandex.market.wms.dimensionmanagement.core.response.GetMeasurementOrdersResponse;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.receiving.service.wmsapi.WmsApiClient;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class DimensionsControllerTest extends ReceivingIntegrationTest {

    @Autowired
    @SpyBean
    private WmsApiClient wmsApiClient;

    @Autowired
    @MockBean
    private DimensionManagementClient dimensionManagementClient;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(wmsApiClient);
        Mockito.reset(dimensionManagementClient);
    }


    @Test
    @DatabaseSetup(value = "/controller/dimensions/check-item.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/dimensions/check-item.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void checkItemIsConveyable() throws Exception {
        mockMvc.perform(get("/dimensions/check-item?storerKey=465852&sku=fit_sku_1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/dimensions/conveyable-response.json")));
    }

    @Test
    @DatabaseSetup(value = "/controller/dimensions/check-item-with-checking-active-measurement-order-existence.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/dimensions/check-item-with-checking-active-measurement-order-existence.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void checkItemIsConveyableUsingCheckingActiveMeasurementOrderExistence() throws Exception {
        MeasurementOrderDto measurementOrder = new MeasurementOrderDto.Builder()
                .serialNumber("1234567890")
                .storer("465852")
                .sku("fit_sku_1")
                .build();
        when(dimensionManagementClient
                .getActiveMeasurementOrder(new GetActiveMeasurementOrderRequest("465852", "fit_sku_1")))
                .thenReturn(new GetMeasurementOrdersResponse(20, 0, 1, List.of(measurementOrder)));
        mockMvc.perform(get("/dimensions/check-item?storerKey=465852&sku=fit_sku_1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/dimensions/conveyable-response.json")));
    }

    @Test
    @DatabaseSetup(value = "/controller/dimensions/check-item.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/dimensions/check-item.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void checkItemIsNonConveyable() throws Exception {
        mockMvc.perform(get("/dimensions/check-item?storerKey=465852&sku=oversize_sku_1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/dimensions/non-conveyable-response.json")));
    }

    @Test
    @DatabaseSetup(value = "/controller/dimensions/check-item-with-checking-active-measurement-order-existence.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/dimensions/check-item-with-checking-active-measurement-order-existence.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void checkItemIsNonConveyableUsingCheckingActiveMeasurementOrderExistence() throws Exception {
        when(dimensionManagementClient
                .getActiveMeasurementOrder(new GetActiveMeasurementOrderRequest("465852", "oversize_sku_1")))
                .thenReturn(new GetMeasurementOrdersResponse(20, 0, 1, Collections.emptyList()));
        mockMvc.perform(get("/dimensions/check-item?storerKey=465852&sku=oversize_sku_1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/dimensions/non-conveyable-response.json")));
    }

    @Test
    @DatabaseSetup(value = "/controller/dimensions/check-item.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/dimensions/check-item.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void checkItemMeasurementRequired() throws Exception {
        mockMvc.perform(get("/dimensions/check-item?storerKey=465852&sku" +
                        "=unknown_dimensions_sku_1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/dimensions/measurement-required-response.json")));
    }

    @Test
    @DatabaseSetup(value = "/controller/dimensions/check-item-with-checking-active-measurement-order-existence.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/dimensions/check-item-with-checking-active-measurement-order-existence.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void checkItemMeasurementRequiredUsingCheckingActiveMeasurementOrderExistence() throws Exception {
        when(dimensionManagementClient
                .getActiveMeasurementOrder(new GetActiveMeasurementOrderRequest("465852", "unknown_dimensions_sku_1")))
                .thenReturn(new GetMeasurementOrdersResponse(20, 0, 1, Collections.emptyList()));
        mockMvc.perform(get("/dimensions/check-item?storerKey=465852&sku" +
                        "=unknown_dimensions_sku_1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/dimensions/measurement-required-response.json")));
    }

    @Test
    @DatabaseSetup(value = "/controller/dimensions/measurement-buffer/immutable.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/dimensions/measurement-buffer/immutable.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void getAdjacentBufferLocByZone() throws Exception {
        mockMvc.perform(get("/dimensions/measurement-buffer/DOCK")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/dimensions/measurement-buffer/response.json")));
    }

    @Test
    @DatabaseSetup(value = "/controller/dimensions/measurement-buffer/immutable.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/dimensions/measurement-buffer/immutable.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void getAdjacentBufferLocByZoneNotFound() throws Exception {
        mockMvc.perform(get("/dimensions/measurement-buffer/DOCK2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/dimensions/measurement-buffer/response-loc-not-found.json"))
                );
    }
}
