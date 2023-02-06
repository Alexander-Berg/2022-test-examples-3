package ru.yandex.market.wms.servicebus.api.external.vendor.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.wms.core.base.request.PushDimensionRequest;
import ru.yandex.market.wms.core.client.CoreClient;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.external.vendor.model.schaefer.DimensionWcsDTO;
import ru.yandex.market.wms.servicebus.api.internal.api.client.impl.WmsApiClientImpl;
import ru.yandex.market.wms.servicebus.model.enums.VendorHttpHeaders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class SchaeferPushDimensionControllerTest extends IntegrationTest {

    @MockBean
    @Autowired
    private WmsApiClientImpl wmsApiClientImpl;

    @MockBean
    @Autowired
    private CoreClient coreClient;

    @MockBean
    @Autowired
    private JmsTemplate defaultJmsTemplate;

    private ObjectMapper mapper = new ObjectMapper();

    @AfterEach
    public void resetMocks() {
        Mockito.reset(wmsApiClientImpl, defaultJmsTemplate, coreClient);
    }

    @Test
    public void synchronousHappyPath() throws Exception {
        //отправка в RMQ отключена
        Mockito.when(dbConfigService.getConfigAsBoolean("ASYNC_PUSH_DIMENSION")).thenReturn(false);

        DimensionWcsDTO dimensionWcsDTO =
                mapper.readValue(
                        getFileContent("api/external/vendor/server/controller/dimensionControl.json"),
                        new TypeReference<DimensionWcsDTO>() {
                        }
                );

        mockMvc.perform(post("/rpc/dc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(VendorHttpHeaders.SCHAEFER_AUTHORIZATION.value(), "SCHAEFER")
                        .content(getFileContent("api/external/vendor/server/controller/dimensionControl.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        ArgumentCaptor<PushDimensionRequest> requestCaptor =
                ArgumentCaptor.forClass(PushDimensionRequest.class);
        verify(coreClient, times(1)).pushDimensions(requestCaptor.capture());

        PushDimensionRequest value = requestCaptor.getValue();

        Assertions.assertAll(
                () -> Assertions.assertEquals(dimensionWcsDTO.getTransportUnitId(), value.getUnitId()),
                () -> Assertions.assertEquals(
                        dimensionWcsDTO.getWeight(),
                        value.getWeight()
                                .intValue()
                )
        );
    }

    @Test
    public void asynchronousHappyPath() throws Exception {
        //отправка в RMQ включена
        Mockito.when(dbConfigService.getConfigAsBoolean("ASYNC_PUSH_DIMENSION")).thenReturn(true);

        DimensionWcsDTO dimensionWcsDTO =
                mapper.readValue(
                        getFileContent("api/external/vendor/server/controller/dimensionControl.json"),
                        new TypeReference<DimensionWcsDTO>() {
                        }
                );

        mockMvc.perform(post("/rpc/dc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(VendorHttpHeaders.SCHAEFER_AUTHORIZATION.value(), "SCHAEFER")
                        .content(getFileContent("api/external/vendor/server/controller/dimensionControl.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        ArgumentCaptor<PushDimensionRequest> requestCaptor =
                ArgumentCaptor.forClass(PushDimensionRequest.class);
        verify(defaultJmsTemplate, times(1))
                .convertAndSend(eq("{mq}_{wrh}_push-dimension"), requestCaptor.capture(), notNull());

        PushDimensionRequest value = requestCaptor.getValue();

        Assertions.assertAll(
                () -> Assertions.assertEquals(dimensionWcsDTO.getTransportUnitId(), value.getUnitId()),
                () -> Assertions.assertEquals(
                        dimensionWcsDTO.getWeight(),
                        value.getWeight()
                                .intValue()
                )
        );
    }

    @Test
    public void testPushDimensionTuIdIsNull() throws Exception {
        //отправка некорректного запроса из WCS

        mockMvc.perform(
                        post("/rpc/dc")
                                .header(VendorHttpHeaders.SCHAEFER_AUTHORIZATION.value(), "SCHAEFER")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent(
                                        "api/external/vendor/server/controller/dimensionControlWithoutTuId.json")
                                )
                )
                .andExpect(status().isBadRequest());

        verify(coreClient, times(0)).pushDimensions(any());
    }
}
