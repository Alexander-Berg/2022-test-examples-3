package ru.yandex.market.wms.servicebus.api.external.vendor.server.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.wms.common.model.dto.TransportUnitId;
import ru.yandex.market.wms.common.model.dto.TransportUnitLocation;
import ru.yandex.market.wms.common.model.dto.TransportUnitTrackingDTO;
import ru.yandex.market.wms.common.model.enums.TransportUnitStatus;
import ru.yandex.market.wms.common.spring.servicebus.vendor.VendorProvider;
import ru.yandex.market.wms.common.spring.utils.WeightsAndDimensions;
import ru.yandex.market.wms.servicebus.api.external.vendor.server.service.RequestTimeService;
import ru.yandex.market.wms.servicebus.api.external.vendor.server.strategy.TransportUnitTrackingStrategyDematicImpl;
import ru.yandex.market.wms.servicebus.async.dto.TransportUnitTrackingPayload;
import ru.yandex.market.wms.servicebus.core.async.model.request.ConveyorType;
import ru.yandex.market.wms.servicebus.core.async.model.request.TransportUnitTrackingLogRequest;
import ru.yandex.market.wms.servicebus.core.dematic.NewDestinationsDTO;
import ru.yandex.market.wms.servicebus.model.enums.VendorHttpHeaders;
import ru.yandex.market.wms.shippingsorter.client.ShippingsorterClient;
import ru.yandex.market.wms.shippingsorter.core.sorting.model.request.PushSortingUnitTrackingRequest;
import ru.yandex.market.wms.transportation.client.TransportationClient;
import ru.yandex.market.wms.transportation.core.model.request.EmptyToteConfirmationTransportationRequest;
import ru.yandex.market.wms.transportation.core.model.request.PushConsolidationUnitTrackingRequest;
import ru.yandex.market.wms.transportation.core.model.request.PushDimensionRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

class DematicControllerTest extends CommonVendorControllerTest {

    private static final String DEMATIC_REQUEST_PATH = "/yandex/resources/YWMS/v1.0";
    private static final String DEMATIC_AUTHENTICATION_SCHEME = "Basic ";
    private static final String USERNAME = "test";
    private static final String PASSWORD = "test";
    private static final String DEMATIC_AUTH_HEADER_VALUE = DEMATIC_AUTHENTICATION_SCHEME
            + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes());
    private static final String TRANSPORT_UNIT_ID_1 = "TS012345SF";
    private static final String TRANSPORT_UNIT_ID_2 = "TS123456SF";
    private static final String TRANSPORT_UNIT_ID_3 = "TS234567SF";
    private static final String MINI_BUTCH_SCALES_LOCATION = "IN01-WC-01";
    private static final String LOCATION_ID = "MZ-OUT_TP-27";
    private static final List<String> TRANSPORT_UNIT_IDS =
            List.of(TRANSPORT_UNIT_ID_1, TRANSPORT_UNIT_ID_2, TRANSPORT_UNIT_ID_3);
    private static final String YM_TRANS_LOC_CONS_PATTERN_SUPER_WH =
            "^(MZ\\-FL\\sN_TP\\-\\d{2}|MZ\\-OUT_TP\\-\\d{2}|MB1_TP\\-\\d{2})$";
    private static final String YM_TRANS_LOC_SORT_PATTERN_SUPER_WH = "^SR1_TP\\-\\d{2}$";
    private static final String YM_TRANS_LOC_UNDEFINED_PATTERN_SUPER_WH = "^(ANNOUNCEMENT|LOST)$";
    private static final Instant TEST_TIME = Instant.ofEpochMilli(1640023387495L);

    @Autowired
    TransportUnitTrackingStrategyDematicImpl transportUnitTrackingStrategyDematicImpl;

    @Autowired
    private TransportationClient transportationClient;

    @Autowired
    private ShippingsorterClient shippingsorterClient;

    @MockBean
    @Autowired
    private JmsTemplate defaultJmsTemplate;

    @MockBean
    @Autowired
    RequestTimeService requestTimeService;

    private final ArgumentCaptor<EmptyToteConfirmationTransportationRequest> requestEmptyToteCaptor =
            ArgumentCaptor.forClass(EmptyToteConfirmationTransportationRequest.class);

    @BeforeEach
    void setupConfig() {
        Mockito.reset(transportationClient, defaultJmsTemplate, shippingsorterClient, requestTimeService);

        Mockito.when(dbConfigService.getConfig(
                        TransportUnitTrackingStrategyDematicImpl.YM_TRANS_LOC_CONS_PATTERN_SUPER_WH
                ))
                .thenReturn(YM_TRANS_LOC_CONS_PATTERN_SUPER_WH);
        Mockito.when(dbConfigService.getConfig(
                        TransportUnitTrackingStrategyDematicImpl.YM_TRANS_LOC_SORT_PATTERN_SUPER_WH
                ))
                .thenReturn(YM_TRANS_LOC_SORT_PATTERN_SUPER_WH);
        Mockito.when(dbConfigService.getConfig(
                        TransportUnitTrackingStrategyDematicImpl.YM_TRANS_LOC_UNDEFINED_PATTERN_SUPER_WH))
                .thenReturn(YM_TRANS_LOC_UNDEFINED_PATTERN_SUPER_WH);

        Mockito.when(requestTimeService.getNowInstant()).thenReturn(TEST_TIME);
    }

    @AfterEach
    public void resetMocks() {
        Mockito.reset(defaultJmsTemplate);
    }

    @Test
    void confirmTransferOrderTest() throws Exception {
        TransportUnitTrackingPayload confirmed = TransportUnitTrackingPayload.builder()
                .payload(TransportUnitTrackingDTO.builder()
                        .transportUnitId(TransportUnitId.of(TRANSPORT_UNIT_ID_1))
                        .currentLocation(TransportUnitLocation.of(LOCATION_ID))
                        .time(TEST_TIME)
                        .status(TransportUnitStatus.FINISHED)
                        .vendorProvider(VendorProvider.DEMATIC)
                        .build())
                .build();

        mockMvc.perform(post(DEMATIC_REQUEST_PATH + "/confirmTransferOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(VendorHttpHeaders.DEMATIC_AUTHORIZATION.value(), DEMATIC_AUTH_HEADER_VALUE)
                        .content(getFileContent(
                                "api/external/vendor/dematic/1/transferOrderConfirmationRequest.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        TransportUnitTrackingLogRequest request = TransportUnitTrackingLogRequest.builder()
                .unitId(TRANSPORT_UNIT_ID_1)
                .currentLocation(LOCATION_ID)
                .conveyorType(ConveyorType.MINI_BATCH)
                .status(TransportUnitStatus.FINISHED)
                .vendorProvider(VendorProvider.DEMATIC)
                .build();

        verify(defaultJmsTemplate, times(1))
                .convertAndSend(eq(TRANSPORT_UNIT_TRACKING_LOG_QUEUE), eq(request), notNull());

        verify(defaultJmsTemplate, times(1))
                .convertAndSend(eq(TRANSPORT_UNIT_TRACKING_QUEUE), eq(confirmed), notNull());
    }

    @Test
    void confirmTransferOrderDestNotReacheableTest() throws Exception {
        PushConsolidationUnitTrackingRequest confirmed = PushConsolidationUnitTrackingRequest.builder()
                .transportUnitTrackingDTO(
                        TransportUnitTrackingDTO.builder()
                                .transportUnitId(TransportUnitId.of(TRANSPORT_UNIT_ID_1))
                                .currentLocation(TransportUnitLocation.of(LOCATION_ID))
                                .time(TEST_TIME)
                                .status(TransportUnitStatus.DEST_NOT_REACHABLE)
                                .vendorProvider(VendorProvider.DEMATIC)
                                .build()
                )
                .build();

        mockMvc.perform(post(DEMATIC_REQUEST_PATH + "/confirmTransferOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(VendorHttpHeaders.DEMATIC_AUTHORIZATION.value(), DEMATIC_AUTH_HEADER_VALUE)
                        .content(getFileContent(
                                "api/external/vendor/dematic/1/transferOrderRequestDestNotReacheable.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        TransportUnitTrackingLogRequest request = TransportUnitTrackingLogRequest.builder()
                .unitId(TRANSPORT_UNIT_ID_1)
                .currentLocation(LOCATION_ID)
                .conveyorType(ConveyorType.MINI_BATCH)
                .status(TransportUnitStatus.DEST_NOT_REACHABLE)
                .vendorProvider(VendorProvider.DEMATIC)
                .build();

        verify(defaultJmsTemplate, times(1))
                .convertAndSend(eq(TRANSPORT_UNIT_TRACKING_LOG_QUEUE), eq(request), notNull());

        verify(transportationClient, times(1)).pushConsolidationUnitTracking(confirmed);
    }

    @Test
    void confirmTransferOrderForLoadUnitIdDoesNotMatchPatternTest() throws Exception {
        mockMvc.perform(post(DEMATIC_REQUEST_PATH + "/confirmTransferOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(VendorHttpHeaders.DEMATIC_AUTHORIZATION.value(), DEMATIC_AUTH_HEADER_VALUE)
                        .content(getFileContent("api/external/vendor/dematic/1/incorrectTransferOrderRequest.json")))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    //todo Дематик-апи фаза-2
    @Test
    @Disabled
    void requestDestinationsTest() throws Exception {
        NewDestinationsDTO newDestinationsDTO = NewDestinationsDTO.builder()
                .transportUnitIds(TRANSPORT_UNIT_IDS.stream()
                        .map(TransportUnitId::of)
                        .collect(Collectors.toList()))
                .build();

        mockMvc.perform(post(DEMATIC_REQUEST_PATH + "/requestDestinations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(VendorHttpHeaders.DEMATIC_AUTHORIZATION.value(), DEMATIC_AUTH_HEADER_VALUE)
                        .content(getFileContent("api/external/vendor/dematic/2/destinationsRequest.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(transportUnitTrackingStrategyDematicImpl, times(1)).processDestinationsRequest(newDestinationsDTO);
    }

    //todo Дематик-апи фаза-2
    @Test
    @Disabled
    void requestDestinationsForLoadUnitIdDoesNotMatchPatternTest() throws Exception {
        mockMvc.perform(post(DEMATIC_REQUEST_PATH + "/requestDestinations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(VendorHttpHeaders.DEMATIC_AUTHORIZATION.value(), DEMATIC_AUTH_HEADER_VALUE)
                        .content(getFileContent("api/external/vendor/dematic/2/incorrectDestinationsRequest.json")))
                .andExpect(status().is4xxClientError())
                .andReturn();

        verify(transportUnitTrackingStrategyDematicImpl, never()).process(any());
    }

    @Test
    void registerTransportUnitInfoTest() throws Exception {
        TransportUnitTrackingPayload notification = TransportUnitTrackingPayload.builder()
                .payload(TransportUnitTrackingDTO.builder()
                        .transportUnitId(TransportUnitId.of(TRANSPORT_UNIT_ID_1))
                        .currentLocation(TransportUnitLocation.of(LOCATION_ID))
                        .time(TEST_TIME)
                        .status(TransportUnitStatus.NOTIFICATION)
                        .vendorProvider(VendorProvider.DEMATIC)
                        .build())
                .build();

        mockMvc.perform(post(DEMATIC_REQUEST_PATH + "/infoTu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(VendorHttpHeaders.DEMATIC_AUTHORIZATION.value(), DEMATIC_AUTH_HEADER_VALUE)
                        .content(getFileContent("api/external/vendor/dematic/3/infoTuRequest.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        TransportUnitTrackingLogRequest request = TransportUnitTrackingLogRequest.builder()
                .unitId(TRANSPORT_UNIT_ID_1)
                .currentLocation(LOCATION_ID)
                .conveyorType(ConveyorType.MINI_BATCH)
                .status(TransportUnitStatus.NOTIFICATION)
                .vendorProvider(VendorProvider.DEMATIC)
                .build();

        verify(defaultJmsTemplate, times(1))
                .convertAndSend(eq(TRANSPORT_UNIT_TRACKING_LOG_QUEUE), eq(request), notNull());

        verify(defaultJmsTemplate, times(1))
                .convertAndSend(eq(TRANSPORT_UNIT_TRACKING_QUEUE), eq(notification), notNull());
    }

    @Test
    void registerPushDimensionTest() throws Exception {
        Mockito.when(dbConfigService.getConfigAsBoolean("ASYNC_PUSH_DIMENSION_MINI_BATCH"))
                .thenReturn(false);

        mockMvc.perform(post(DEMATIC_REQUEST_PATH + "/infoTu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(VendorHttpHeaders.DEMATIC_AUTHORIZATION.value(), DEMATIC_AUTH_HEADER_VALUE)
                        .content(getFileContent("api/external/vendor/dematic/3/infoTuPushDimensionRequest.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        PushDimensionRequest request = PushDimensionRequest.builder()
                .unitId(TRANSPORT_UNIT_ID_1)
                .vendorProvider(VendorProvider.DEMATIC)
                .weight(WeightsAndDimensions.gramsToKilograms(BigDecimal.valueOf(10)))
                .loadUnitType("TO")
                .build();

        verify(defaultJmsTemplate, never())
                .convertAndSend(eq(TRANSPORTER_PUSH_DIMENSION), eq(request), notNull());

        verify(transportationClient, times(1)).pushDimensions(request);

    }

    @Test
    void registerPushDimensionAsyncTest() throws Exception {
        Mockito.when(dbConfigService.getConfigAsBoolean("ASYNC_PUSH_DIMENSION_MINI_BATCH"))
                .thenReturn(true);

        mockMvc.perform(post(DEMATIC_REQUEST_PATH + "/infoTu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(VendorHttpHeaders.DEMATIC_AUTHORIZATION.value(), DEMATIC_AUTH_HEADER_VALUE)
                        .content(getFileContent("api/external/vendor/dematic/3/infoTuPushDimensionRequest.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        PushDimensionRequest request = PushDimensionRequest.builder()
                .unitId(TRANSPORT_UNIT_ID_1)
                .vendorProvider(VendorProvider.DEMATIC)
                .weight(WeightsAndDimensions.gramsToKilograms(BigDecimal.valueOf(10)))
                .loadUnitType("TO")
                .build();

        verify(defaultJmsTemplate, times(1))
                .convertAndSend(eq(TRANSPORTER_PUSH_DIMENSION), eq(request), notNull());

        verify(transportationClient, never()).pushDimensions(request);
    }

    @Test
    void registerTransportUnitInfoForLoadUnitIdDoesNotMatchPatternTest() throws Exception {
        mockMvc.perform(post(DEMATIC_REQUEST_PATH + "/infoTu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(VendorHttpHeaders.DEMATIC_AUTHORIZATION.value(), DEMATIC_AUTH_HEADER_VALUE)
                        .content(getFileContent("api/external/vendor/dematic/3/incorrectInfoTuRequest.json")))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    //todo ждем решения по вычисления location и/или area в запросах /confirmCancellation
    @Test
    void confirmCancellationTest() throws Exception {
        TransportUnitTrackingDTO trackingDTO = TransportUnitTrackingDTO.builder()
                .transportUnitId(TransportUnitId.of(TRANSPORT_UNIT_ID_1))
                .currentLocation(TransportUnitLocation.of("ANNOUNCEMENT"))
                .time(TEST_TIME)
                .status(TransportUnitStatus.MANUAL_CANCELED_ORDER)
                .vendorProvider(VendorProvider.DEMATIC)
                .build();
        PushConsolidationUnitTrackingRequest cancelledToTrans = PushConsolidationUnitTrackingRequest.builder()
                .transportUnitTrackingDTO(trackingDTO)
                .build();
        PushSortingUnitTrackingRequest cancelledToShipSort = PushSortingUnitTrackingRequest.builder()
                .transportUnitTrackingDTO(trackingDTO)
                .build();

        mockMvc.perform(post(DEMATIC_REQUEST_PATH + "/confirmCancellation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(VendorHttpHeaders.DEMATIC_AUTHORIZATION.value(), DEMATIC_AUTH_HEADER_VALUE)
                        .content(getFileContent("api/external/vendor/dematic/4/confirmCancellationRequest.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        TransportUnitTrackingLogRequest logRequest = TransportUnitTrackingLogRequest.builder()
                .unitId(TRANSPORT_UNIT_ID_1)
                .currentLocation("ANNOUNCEMENT")
                .conveyorType(ConveyorType.UNDEFINED)
                .status(TransportUnitStatus.MANUAL_CANCELED_ORDER)
                .vendorProvider(VendorProvider.DEMATIC)
                .build();

        verify(defaultJmsTemplate, times(1))
                .convertAndSend(eq(TRANSPORT_UNIT_TRACKING_LOG_QUEUE), eq(logRequest), notNull());

        verify(transportationClient, times(1)).pushConsolidationUnitTracking(cancelledToTrans);
        verify(shippingsorterClient, times(1)).pushSortingUnitTracking(cancelledToShipSort);
    }

    @Test
    void confirmCancellationForLoadUnitIdDoesNotMatchPatternTest() throws Exception {
        mockMvc.perform(post(DEMATIC_REQUEST_PATH + "/confirmCancellation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(VendorHttpHeaders.DEMATIC_AUTHORIZATION.value(), DEMATIC_AUTH_HEADER_VALUE)
                        .content(getFileContent("api/external/vendor/dematic/4/incorrectConfirmCancellationRequest" +
                                ".json")))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    void confirmEmptyToteRequestTest() throws Exception {
        mockMvc.perform(post(DEMATIC_REQUEST_PATH + "/confirmEmptyTote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(VendorHttpHeaders.DEMATIC_AUTHORIZATION.value(), DEMATIC_AUTH_HEADER_VALUE)
                        .content(getFileContent("api/external/vendor/dematic/5/confirmEmptyToteRequest.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(transportationClient, times(1)).confirmEmptyToteRequest(
                requestEmptyToteCaptor.capture());
    }
}
