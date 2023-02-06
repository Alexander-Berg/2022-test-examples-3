package ru.yandex.market.wms.shippingsorter.sorting.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.service.balance.BalanceService;
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient;
import ru.yandex.market.wms.common.spring.servicebus.model.response.VendorApiResponse;
import ru.yandex.market.wms.core.base.dto.DimensionDto;
import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxStatus;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;
import ru.yandex.market.wms.shippingsorter.sorting.api.internal.CoreIntegrationService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@Import(ShippingSorterSecurityTestConfiguration.class)
public class TransportUnitTrackingControllerTest extends IntegrationTest {

    private static final BigDecimal ONE_THOUSAND = BigDecimal.valueOf(1000);

    @MockBean
    @Autowired
    private JmsTemplate defaultJmsTemplate;

    @MockBean
    @Autowired
    protected ServicebusClient servicebusClient;

    @MockBean
    @Autowired
    protected CoreIntegrationService coreIntegrationService;

    @MockBean
    @Autowired
    protected DbConfigService configService;

    @BeforeEach
    protected void reset() {
        Mockito.reset(servicebusClient);
        Mockito.reset(defaultJmsTemplate);
        Mockito.reset(coreIntegrationService);
        Mockito.reset(configService);
    }

    @Test
    @DatabaseSetup("/sorting/controller/transport-unit-tracking/successful-push-tracking/initial-state.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/transport-unit-tracking/successful-push-tracking/initial-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void shouldSuccessPushTransportUnitTrackingInProgress() throws Exception {
        when(coreIntegrationService.getLocationsByBoxId(any())).thenReturn(Collections.singletonList("UPACK-01"));

        mockMvc.perform(post("/sorting/transport-unit-tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "sorting/controller/transport-unit-tracking/successful-push-tracking/request.json")))
                .andExpect(status().isOk());

        verify(defaultJmsTemplate, times(1)).convertAndSend(anyString(), any(), any());
    }

    @Test
    @DatabaseSetup("/sorting/controller/transport-unit-tracking/unsuccessful-push-tracking/initial-state.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/transport-unit-tracking/unsuccessful-push-tracking/initial-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void shouldThrowLocationNotFoundException() throws Exception {
        when(coreIntegrationService.getLocationsByBoxId(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/sorting/transport-unit-tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "sorting/controller/transport-unit-tracking/unsuccessful-push-tracking/request.json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DatabaseSetup("/sorting/controller/transport-unit-tracking/successful-push-tracking-to-finished/initial-state.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/transport-unit-tracking/successful-push-tracking-to-finished/" +
                    "initial-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void shouldSuccessPushTransportUnitTrackingToFinished() throws Exception {
        when(coreIntegrationService.getLocationsByBoxId(any())).thenReturn(Collections.singletonList("UPACK-01"));

        mockMvc.perform(post("/sorting/transport-unit-tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "sorting/controller/transport-unit-tracking/" +
                                "successful-push-tracking-to-finished/request.json")))
                .andExpect(status().isOk());

        verify(defaultJmsTemplate, times(1)).convertAndSend(anyString(), any(), any());
    }

    @Test
    @DatabaseSetup("/sorting/controller/transport-unit-tracking/successful-push-tracking-to-canceled/initial-state.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/transport-unit-tracking/successful-push-tracking-to-canceled/1/" +
                    "final-state.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void shouldSuccessPushTransportUnitTrackingFromInProgressToCanceled() throws Exception {
        mockOkApiResponse();
        mockMvc.perform(post("/sorting/transport-unit-tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "sorting/controller/transport-unit-tracking/successful-push-tracking-to-canceled/" +
                                "1/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(servicebusClient, times(1)).createSorterOrder(any());
    }

    @Test
    @DatabaseSetup("/sorting/controller/transport-unit-tracking/successful-push-tracking-to-canceled/initial-state.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/transport-unit-tracking/successful-push-tracking-to-canceled/" +
                    "initial-state.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void shouldNotSuccessPushTransportUnitTrackingByWrongUnitId() throws Exception {
        mockMvc.perform(post("/sorting/transport-unit-tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "sorting/controller/transport-unit-tracking/successful-push-tracking-to-canceled/" +
                                "2/request.json")))
                .andExpect(status().is4xxClientError())
                .andReturn();

        verify(servicebusClient, never()).deleteTransportOrder(any());
    }

    @Test
    @DatabaseSetup("/sorting/controller/transport-unit-tracking/successful-push-tracking-to-canceled/initial-state.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/transport-unit-tracking/successful-push-tracking-to-canceled/3/" +
                    "final-state.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void shouldSuccessPushTransportUnitTrackingFromAssignedToCanceled() throws Exception {
        mockOkApiResponse();
        mockMvc.perform(post("/sorting/transport-unit-tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "sorting/controller/transport-unit-tracking/successful-push-tracking-to-canceled/" +
                                "3/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(servicebusClient, times(1)).createSorterOrder(any());
    }

    @Test
    @DatabaseSetup("/sorting/controller/transport-unit-tracking/should-not-run-change-balances/initial-state.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/transport-unit-tracking/should-not-run-change-balances/initial-state.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void shouldNotRunChangeBalances() throws Exception {
        BalanceService balanceService = Mockito.mock(BalanceService.class);
        when(coreIntegrationService.getLocationsByBoxId(any())).thenReturn(Collections.singletonList("UPACK-01"));
        when(coreIntegrationService.getTransitLocByExternalZoneName("SHIPPING1"))
                .thenReturn("S_TRANSIT");

        mockMvc.perform(post("/sorting/transport-unit-tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "sorting/controller/transport-unit-tracking/should-not-run-change-balances/" +
                                "request.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(balanceService, never()).moveContainer(any(), any(), any());
    }

    @Test
    @DatabaseSetup("/sorting/controller/transport-unit-tracking/" +
            "successful-push-tracking-to-no-order/with-wms-order/initial-state.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/transport-unit-tracking/" +
                    "successful-push-tracking-to-no-order/with-wms-order/initial-state.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void shouldSuccessPushTransportUnitTrackingToNoOrderWithWMSOrder() throws Exception {
        mockOkApiResponse();
        mockConfigs();
        when(coreIntegrationService.getLocationsByBoxId(any())).thenReturn(Collections.singletonList("UPACK-01"));
        when(coreIntegrationService.getTransitLocByExternalZoneName("SHIPPING1"))
                .thenReturn("S_TRANSIT");
        List<DimensionDto> dimensions = List.of(
                new DimensionDto(ONE_THOUSAND, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, Instant.now())
        );
        when(coreIntegrationService.getDimensionsByBoxId(any())).thenReturn(dimensions);

        mockMvc.perform(post("/sorting/transport-unit-tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "sorting/controller/transport-unit-tracking/" +
                                "successful-push-tracking-to-no-order/with-wms-order/request.json"
                )))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(servicebusClient, times(1)).createSorterOrder(any());
        verify(defaultJmsTemplate, times(1)).convertAndSend(anyString(), any(), any());
    }

    @Test
    @DatabaseSetup("/sorting/controller/transport-unit-tracking/successful-push-tracking-to-no-order/without-wms" +
            "-order/initial-state.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/transport-unit-tracking/successful-push-tracking-to-no-order/without-wms" +
                    "-order/final-state.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void shouldSuccessPushTransportUnitTrackingToNoOrderWithoutWMSOrder() throws Exception {
        mockOkApiResponse();
        mockConfigs();
        when(coreIntegrationService.getLocationsByBoxId(any())).thenReturn(Collections.singletonList("UPACK-01"));
        when(coreIntegrationService.getTransitLocByExternalZoneName("SHIPPING1"))
                .thenReturn("S_TRANSIT");
        List<DimensionDto> dimensions = List.of(
                new DimensionDto(ONE_THOUSAND, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, Instant.now())
        );
        when(coreIntegrationService.getDimensionsByBoxId(any())).thenReturn(dimensions);
        when(coreIntegrationService.getBoxStatus(any())).thenReturn(BoxStatus.builder()
                .isBoxDropped(false)
                .isBoxLoaded(false)
                .isBoxShipped(false)
                .build());

        mockMvc.perform(post("/sorting/transport-unit-tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "sorting/controller/transport-unit-tracking/" +
                                "successful-push-tracking-to-no-order/without-wms-order/request.json"
                )))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(servicebusClient, times(1)).createSorterOrder(any());
        verify(defaultJmsTemplate, times(1)).convertAndSend(anyString(), any(), any());
    }


    @Test
    @DatabaseSetup("/sorting/controller/transport-unit-tracking/successful-push-tracking-to-no-order/wrong-conveyor" +
            "/initial-state.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/transport-unit-tracking/successful-push-tracking-to-no-order/wrong-conveyor" +
                    "/final-state.xml",
            assertionMode = NON_STRICT
    )
    public void shouldSuccessPushTransportUnitTrackingWhenBoxOnWrongConveyor() throws Exception {
        mockOkApiResponse();
        mockConfigs();
        when(coreIntegrationService.getLocationsByBoxId(any())).thenReturn(Collections.singletonList("UPACK-01"));
        when(coreIntegrationService.getTransitLocByExternalZoneName("SHIPPING2"))
                .thenReturn("S_TRANSIT2");
        List<DimensionDto> dimensions = List.of(
                new DimensionDto(ONE_THOUSAND, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, Instant.now())
        );
        when(coreIntegrationService.getDimensionsByBoxId(any())).thenReturn(dimensions);
        when(coreIntegrationService.getBoxStatus(any())).thenReturn(BoxStatus.builder()
                .isBoxDropped(false)
                .isBoxLoaded(false)
                .isBoxShipped(false)
                .build());

        mockMvc.perform(post("/sorting/transport-unit-tracking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "sorting/controller/transport-unit-tracking/successful-push-tracking-to-no-order" +
                                "/wrong-conveyor/request.json"
                )))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(servicebusClient, times(1)).createSorterOrder(any());
        verify(defaultJmsTemplate, times(1)).convertAndSend(anyString(), any(), any());
    }

    private void mockOkApiResponse() {
        VendorApiResponse apiResponse = VendorApiResponse.builder().code("200").message("OK").build();

        Mockito.when(servicebusClient.createSorterOrder(any())).thenReturn(apiResponse);
    }

    private void mockConfigs() {
        when(configService.getConfigAsBoolean("AUTO_RECREATE_SORTER_ORDER", false)).thenReturn(true);
        when(configService.getConfigAsBoolean("RECREATE_IF_BOX_ON_WRONG_CONV", false)).thenReturn(true);
        when(configService.getConfigAsInteger("AUTO_RECREATE_ATTEMPTS_THRESHOLD", 3)).thenReturn(3);
        when(configService.getConfigAsDouble("WEIGHT_LIMIT_FOR_ROUND_GRAMS")).thenReturn(31000D);
        when(configService.getConfigAsDouble("PACKING_MIN_WEIGHT_GRAMS")).thenReturn(50D);
        when(configService.getConfigAsDouble("PACKING_MAX_WEIGHT_GRAMS")).thenReturn(10000D);
        when(configService.getConfigAsDouble("PACKING_MAX_WIDTH")).thenReturn(60D);
        when(configService.getConfigAsDouble("PACKING_MAX_LENGTH")).thenReturn(60D);
        when(configService.getConfigAsDouble("PACKING_MAX_HEIGHT")).thenReturn(60D);
    }
}
