package ru.yandex.market.wms.servicebus.api.external.vendor.server.controller;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.util.NestedServletException;

import ru.yandex.market.wms.common.model.dto.TransportUnitId;
import ru.yandex.market.wms.common.model.dto.TransportUnitLocation;
import ru.yandex.market.wms.common.model.dto.TransportUnitTrackingDTO;
import ru.yandex.market.wms.common.model.enums.TransportUnitStatus;
import ru.yandex.market.wms.common.spring.servicebus.vendor.VendorProvider;
import ru.yandex.market.wms.servicebus.api.external.vendor.server.strategy.TransportUnitTrackingStrategyShaeferImpl;
import ru.yandex.market.wms.servicebus.async.dto.TransportUnitTrackingPayload;
import ru.yandex.market.wms.servicebus.core.async.model.request.ConveyorType;
import ru.yandex.market.wms.servicebus.core.async.model.request.TransportUnitTrackingLogRequest;
import ru.yandex.market.wms.servicebus.model.enums.VendorHttpHeaders;
import ru.yandex.market.wms.shippingsorter.client.ShippingsorterClient;
import ru.yandex.market.wms.shippingsorter.core.sorting.model.request.PushSortingUnitTrackingRequest;
import ru.yandex.market.wms.transportation.client.TransportationClient;
import ru.yandex.market.wms.transportation.core.model.request.PushConsolidationUnitTrackingRequest;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class SchaeferControllerTest extends CommonVendorControllerTest {

    private static final String SCHAEFER_AUTH_HEADER_VALUE = "SCHAEFER";
    public static final String YM_TRANS_AREA_CONS_PATTERN = "^(MEZZANINE|MB1|MB2|MB3|RECEIVING)$";
    public static final String YM_TRANS_AREA_SORT_PATTERN = "^(SHIPPING1|SHIPPING2)$";
    public static final String YM_TRANS_AREA_UNDEFINED_PATTERN = "^(LOST|ANNOUNCEMENT)$";
    private static final String PUSH_CONSOLIDATION_MOCK_METHOD = "pushConsolidationUnitTracking";
    private static final String PUSH_SORTING_MOCK_METHOD = "pushSortingUnitTracking";

    @Autowired
    private TransportationClient transportationClient;

    @Autowired
    private ShippingsorterClient shippingsorterClient;

    @MockBean
    @Autowired
    private JmsTemplate defaultJmsTemplate;

    @BeforeEach
    void setupConfig() {
        Mockito.reset(transportationClient, defaultJmsTemplate, shippingsorterClient);

        Mockito.when(dbConfigService.getConfig(TransportUnitTrackingStrategyShaeferImpl.YM_TRANS_AREA_CONS_PATTERN))
                .thenReturn(YM_TRANS_AREA_CONS_PATTERN);
        Mockito.when(dbConfigService.getConfig(TransportUnitTrackingStrategyShaeferImpl.YM_TRANS_AREA_SORT_PATTERN))
                .thenReturn(YM_TRANS_AREA_SORT_PATTERN);
        Mockito.when(dbConfigService.getConfig(
                TransportUnitTrackingStrategyShaeferImpl.YM_TRANS_AREA_UNDEFINED_PATTERN))
                .thenReturn(YM_TRANS_AREA_UNDEFINED_PATTERN);
    }

    @AfterEach
    public void resetMocks() {
        Mockito.reset(defaultJmsTemplate);
    }

    @Test
    public void successExecuteErrorStatus() throws Exception {
        String unitId = "T000000012";
        String currentLocation = "MZ-OUT_TP-27";
        String area = "MEZZANINE";

        PushConsolidationUnitTrackingRequest mockRequest = PushConsolidationUnitTrackingRequest.builder()
                .transportUnitTrackingDTO(createTransportUnitTracking(unitId,
                        TransportUnitStatus.ERROR_NOORDER,
                        currentLocation, area))
                .build();

        mockMvc.perform(post("/rpc/tut")
                .contentType(MediaType.APPLICATION_JSON)
                .header(VendorHttpHeaders.SCHAEFER_AUTHORIZATION.value(), SCHAEFER_AUTH_HEADER_VALUE)
                .content(getFileContent("api/external/vendor/server.controller/transport-unit/1/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(transportationClient, times(1)).pushConsolidationUnitTracking(mockRequest);

        TransportUnitTrackingLogRequest request = TransportUnitTrackingLogRequest.builder()
                .unitId(unitId)
                .currentLocation(currentLocation)
                .area(area)
                .conveyorType(ConveyorType.MINI_BATCH)
                .status(TransportUnitStatus.ERROR_NOORDER)
                .vendorProvider(VendorProvider.SCHAEFER)
                .build();

        verify(defaultJmsTemplate, times(1))
                .convertAndSend(eq(TRANSPORT_UNIT_TRACKING_LOG_QUEUE), eq(request), notNull());

        assertionActualInvocation(
                transportationClient,
                PUSH_CONSOLIDATION_MOCK_METHOD,
                0,
                mockRequest
        );
    }

    @Test
    public void successAsyncExecuteFinishedStatus() throws Exception {
        String unitId = "T000000012";
        String currentLocation = "MZ-OUT_TP-27";
        String area = "MB1";

        TransportUnitTrackingPayload mockPayload = TransportUnitTrackingPayload.builder()
                .payload(
                        createTransportUnitTracking(
                                unitId, TransportUnitStatus.FINISHED, currentLocation, area
                        )
                )
                .build();

        mockMvc.perform(post("/rpc/tut")
                .contentType(MediaType.APPLICATION_JSON)
                .header(VendorHttpHeaders.SCHAEFER_AUTHORIZATION.value(), SCHAEFER_AUTH_HEADER_VALUE)
                .content(getFileContent("api/external/vendor/server.controller/transport-unit/2/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(defaultJmsTemplate, times(1))
                .convertAndSend(eq(TRANSPORT_UNIT_TRACKING_LOG_QUEUE), notNull(), notNull());

        TransportUnitTrackingLogRequest request = TransportUnitTrackingLogRequest.builder()
                .unitId(unitId)
                .currentLocation(currentLocation)
                .area(area)
                .conveyorType(ConveyorType.MINI_BATCH)
                .status(TransportUnitStatus.FINISHED)
                .vendorProvider(VendorProvider.SCHAEFER)
                .build();

        verify(defaultJmsTemplate, times(1))
                .convertAndSend(eq(TRANSPORT_UNIT_TRACKING_LOG_QUEUE), eq(request), notNull());

        assertionActualInvocation(defaultJmsTemplate, "convertAndSend", 1, mockPayload);
    }

    @Test
    public void successAsyncExecuteNotificationStatus() throws Exception {
        String unitId = "T000000012";
        String currentLocation = "MZ-OUT_TP-27";
        String area = "MB2";

        TransportUnitTrackingPayload mockPayload = TransportUnitTrackingPayload.builder()
                .payload(
                        createTransportUnitTracking(
                                unitId,
                                TransportUnitStatus.NOTIFICATION,
                                currentLocation, area)
                )
                .build();

        mockMvc.perform(post("/rpc/tut")
                .contentType(MediaType.APPLICATION_JSON)
                .header(VendorHttpHeaders.SCHAEFER_AUTHORIZATION.value(), SCHAEFER_AUTH_HEADER_VALUE)
                .content(getFileContent("api/external/vendor/server.controller/transport-unit/3/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(defaultJmsTemplate, times(1))
                .convertAndSend(eq("{mq}_{wrh}_transport-unit-tracking"), notNull(), notNull());

        TransportUnitTrackingLogRequest request = TransportUnitTrackingLogRequest.builder()
                .unitId(unitId)
                .currentLocation(currentLocation)
                .area(area)
                .conveyorType(ConveyorType.MINI_BATCH)
                .status(TransportUnitStatus.NOTIFICATION)
                .vendorProvider(VendorProvider.SCHAEFER)
                .build();

        verify(defaultJmsTemplate, times(1))
                .convertAndSend(
                        eq(TRANSPORT_UNIT_TRACKING_LOG_QUEUE), eq(request), notNull()
                );

        assertionActualInvocation(defaultJmsTemplate, "convertAndSend", 1, mockPayload);
    }

    @Test
    public void shouldNotSuccessAsyncExecuteIfTransportStatusNotValid() {
        assertThrows(NestedServletException.class,
                () -> mockMvc.perform(post("/rpc/tut")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(VendorHttpHeaders.SCHAEFER_AUTHORIZATION.value(), SCHAEFER_AUTH_HEADER_VALUE)
                        .content(getFileContent("api/external/vendor/server" +
                                ".controller/transport-unit/4/request.json")))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn()
        );
    }

    @Test
    public void shouldSuccessAsyncExecuteIfTransportIdUnknown() throws Exception {
        String unitId = "UNKNOWN";
        String currentLocation = "MZ-OUT_TP-27";
        String area = "RECEIVING";

        TransportUnitTrackingPayload mockPayload = TransportUnitTrackingPayload.builder()
                .payload(
                        createTransportUnitTracking(
                                unitId,
                                TransportUnitStatus.FINISHED,
                                currentLocation, area)
                )
                .build();

        mockMvc.perform(post("/rpc/tut")
                .contentType(MediaType.APPLICATION_JSON)
                .header(VendorHttpHeaders.SCHAEFER_AUTHORIZATION.value(), SCHAEFER_AUTH_HEADER_VALUE)
                .content(getFileContent("api/external/vendor/server.controller/transport-unit/5/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(defaultJmsTemplate, times(1))
                .convertAndSend(eq("{mq}_{wrh}_transport-unit-tracking"), notNull(), notNull());

        TransportUnitTrackingLogRequest request = TransportUnitTrackingLogRequest.builder()
                .unitId(unitId)
                .currentLocation(currentLocation)
                .area(area)
                .conveyorType(ConveyorType.MINI_BATCH)
                .status(TransportUnitStatus.FINISHED)
                .vendorProvider(VendorProvider.SCHAEFER)
                .build();

        verify(defaultJmsTemplate, times(1))
                .convertAndSend(
                        eq(TRANSPORT_UNIT_TRACKING_LOG_QUEUE), eq(request), notNull()
                );

        assertionActualInvocation(defaultJmsTemplate, "convertAndSend", 1, mockPayload);
    }

    @Test
    public void shouldNotSuccessSyncExecuteIfTransportStatusNotValid() throws Exception {
        String unitId = "T000000012";
        String currentLocation = "SR1_TP-16";
        String area = "SHIPPING1";

        PushSortingUnitTrackingRequest mockRequest = PushSortingUnitTrackingRequest.builder()
                .transportUnitTrackingDTO(createTransportUnitTracking(unitId,
                        TransportUnitStatus.ERROR_NOORDER,
                        currentLocation, "SHIPPING1"))
                .build();

        mockMvc.perform(post("/rpc/tut")
                .contentType(MediaType.APPLICATION_JSON)
                .header(VendorHttpHeaders.SCHAEFER_AUTHORIZATION.value(), SCHAEFER_AUTH_HEADER_VALUE)
                .content(getFileContent("api/external/vendor/server.controller/transport-unit/6/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(shippingsorterClient, times(1)).pushSortingUnitTracking(mockRequest);

        TransportUnitTrackingLogRequest request = TransportUnitTrackingLogRequest.builder()
                .unitId(unitId)
                .currentLocation(currentLocation)
                .area(area)
                .conveyorType(ConveyorType.SHIPPINGSORTER)
                .status(TransportUnitStatus.ERROR_NOORDER)
                .vendorProvider(VendorProvider.SCHAEFER)
                .build();

        verify(defaultJmsTemplate, times(1))
                .convertAndSend(
                        eq(TRANSPORT_UNIT_TRACKING_LOG_QUEUE), eq(request), notNull()
                );

        assertionActualInvocation(
                shippingsorterClient,
                PUSH_SORTING_MOCK_METHOD,
                0,
                mockRequest
        );
    }

    @Test
    public void shouldSuccessSyncExecuteIfCurrentLocationAnnouncement() throws Exception {
        String unitId = "P000000012";
        String currentLocation = "ANNOUNCEMENT";
        String area = "ANNOUNCEMENT";

        final PushSortingUnitTrackingRequest mockSortingRequest = PushSortingUnitTrackingRequest.builder()
                .transportUnitTrackingDTO(createTransportUnitTracking(unitId,
                        TransportUnitStatus.MANUAL_CANCELED_ORDER,
                        currentLocation, area))
                .build();

        final PushConsolidationUnitTrackingRequest mockConsRequest = PushConsolidationUnitTrackingRequest.builder()
                .transportUnitTrackingDTO(createTransportUnitTracking(unitId,
                        TransportUnitStatus.MANUAL_CANCELED_ORDER,
                        currentLocation, area))
                .build();

        mockMvc.perform(post("/rpc/tut")
                .contentType(MediaType.APPLICATION_JSON)
                .header(VendorHttpHeaders.SCHAEFER_AUTHORIZATION.value(), SCHAEFER_AUTH_HEADER_VALUE)
                .content(getFileContent("api/external/vendor/server.controller/transport-unit/7/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(shippingsorterClient, times(1)).pushSortingUnitTracking(mockSortingRequest);
        verify(transportationClient, times(1)).pushConsolidationUnitTracking(mockConsRequest);

        TransportUnitTrackingLogRequest request = TransportUnitTrackingLogRequest.builder()
                .unitId(unitId)
                .currentLocation(currentLocation)
                .area(area)
                .conveyorType(ConveyorType.UNDEFINED)
                .status(TransportUnitStatus.MANUAL_CANCELED_ORDER)
                .vendorProvider(VendorProvider.SCHAEFER)
                .build();

        verify(defaultJmsTemplate, times(1))
                .convertAndSend(
                        eq(TRANSPORT_UNIT_TRACKING_LOG_QUEUE), eq(request), notNull()
                );

        assertionActualInvocation(
                shippingsorterClient,
                PUSH_SORTING_MOCK_METHOD,
                0,
                mockSortingRequest
        );
        assertionActualInvocation(
                transportationClient,
                PUSH_CONSOLIDATION_MOCK_METHOD,
                0,
                mockConsRequest
        );
    }

    @Test
    public void shouldSuccessSyncExecuteIfCurrentLocationLost() throws Exception {
        String unitId = "P000000012";
        String currentLocation = "LOST";
        String area = "LOST";

        final PushSortingUnitTrackingRequest mockSortingRequest = PushSortingUnitTrackingRequest.builder()
                .transportUnitTrackingDTO(createTransportUnitTracking(unitId,
                        TransportUnitStatus.MANUAL_CANCELED_ORDER,
                        currentLocation, area))
                .build();

        final PushConsolidationUnitTrackingRequest mockConsRequest = PushConsolidationUnitTrackingRequest.builder()
                .transportUnitTrackingDTO(createTransportUnitTracking(unitId,
                        TransportUnitStatus.MANUAL_CANCELED_ORDER,
                        currentLocation, area))
                .build();

        mockMvc.perform(post("/rpc/tut")
                .contentType(MediaType.APPLICATION_JSON)
                .header(VendorHttpHeaders.SCHAEFER_AUTHORIZATION.value(), SCHAEFER_AUTH_HEADER_VALUE)
                .content(getFileContent("api/external/vendor/server.controller/transport-unit/8/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(shippingsorterClient, times(1)).pushSortingUnitTracking(mockSortingRequest);
        verify(transportationClient, times(1)).pushConsolidationUnitTracking(mockConsRequest);

        TransportUnitTrackingLogRequest request = TransportUnitTrackingLogRequest.builder()
                .unitId(unitId)
                .currentLocation(currentLocation)
                .area(area)
                .conveyorType(ConveyorType.UNDEFINED)
                .status(TransportUnitStatus.MANUAL_CANCELED_ORDER)
                .vendorProvider(VendorProvider.SCHAEFER)
                .build();

        verify(defaultJmsTemplate, times(1))
                .convertAndSend(
                        eq(TRANSPORT_UNIT_TRACKING_LOG_QUEUE), eq(request), notNull()
                );

        assertionActualInvocation(
                shippingsorterClient,
                PUSH_SORTING_MOCK_METHOD,
                0,
                mockSortingRequest
        );
        assertionActualInvocation(
                transportationClient,
                PUSH_CONSOLIDATION_MOCK_METHOD,
                0,
                mockConsRequest
        );
    }

    @Test
    public void shouldNotSuccessSyncExecuteIfIntegrationFailed() throws Exception {
        String unitId = "P000000012";
        String currentLocation = "LOST";
        String area = "LOST";

        doThrow(new RuntimeException()).when(shippingsorterClient).pushSortingUnitTracking(any());
        doThrow(new RuntimeException()).when(transportationClient).pushConsolidationUnitTracking(any());

        mockMvc.perform(post("/rpc/tut")
                .contentType(MediaType.APPLICATION_JSON)
                .header(VendorHttpHeaders.SCHAEFER_AUTHORIZATION.value(), SCHAEFER_AUTH_HEADER_VALUE)
                .content(getFileContent("api/external/vendor/server.controller/transport-unit/8/request.json")))
                .andExpect(status().is4xxClientError())
                .andReturn();

        TransportUnitTrackingLogRequest request = TransportUnitTrackingLogRequest.builder()
                .unitId(unitId)
                .currentLocation(currentLocation)
                .area(area)
                .conveyorType(ConveyorType.UNDEFINED)
                .status(TransportUnitStatus.MANUAL_CANCELED_ORDER)
                .vendorProvider(VendorProvider.SCHAEFER)
                .build();

        verify(defaultJmsTemplate, times(1))
                .convertAndSend(
                        eq(TRANSPORT_UNIT_TRACKING_LOG_QUEUE), eq(request), notNull()
                );
    }

    private TransportUnitTrackingDTO createTransportUnitTracking(String transportUnitId,
                                                                 TransportUnitStatus status,
                                                                 String location,
                                                                 String area) {
        return TransportUnitTrackingDTO.builder()
                .transportUnitId(TransportUnitId.builder().id(transportUnitId).build())
                .currentLocation(TransportUnitLocation.builder().id(location).build())
                .status(status)
                .externalZoneName(area)
                .time(LocalDateTime.of(2020, 10, 21, 10, 0, 21)
                        .toInstant(ZoneOffset.UTC)
                )
                .vendorProvider(VendorProvider.SCHAEFER)
                .build();
    }
}
