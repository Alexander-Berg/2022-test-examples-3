package ru.yandex.market.wms.transportation.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient;
import ru.yandex.market.wms.common.spring.servicebus.model.request.CreateTransportOrderRequest;
import ru.yandex.market.wms.common.spring.servicebus.model.request.EmptyTotesRequest;
import ru.yandex.market.wms.common.spring.servicebus.model.response.EmptyTotesResponse;
import ru.yandex.market.wms.common.spring.servicebus.model.response.VendorApiResponse;
import ru.yandex.market.wms.common.spring.utils.uuid.UuidGenerator;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class EmptyTotesControllerTest extends IntegrationTest {

    private final ArgumentCaptor<EmptyTotesRequest> requestEmptyToteCaptor =
            ArgumentCaptor.forClass(EmptyTotesRequest.class);
    private final ArgumentCaptor<CreateTransportOrderRequest> createTransportOrderCaptor =
            ArgumentCaptor.forClass(CreateTransportOrderRequest.class);
    @Autowired
    @SpyBean
    private ServicebusClient servicebusClient;

    @Autowired
    @SpyBean
    private UuidGenerator uuidGenerator;

    @MockBean
    @Autowired
    private Clock clock;

    @BeforeEach
    void setUp() {
        clockSetup();
        Mockito.reset(servicebusClient, uuidGenerator);
    }

    void clockSetup() {
        Instant parse = Instant.parse("2022-02-02T12:05:00.000Z");
        doReturn(Clock.fixed(parse, ZoneOffset.UTC).instant()).when(this.clock).instant();
        doReturn(Clock.fixed(parse, ZoneOffset.UTC).getZone()).when(this.clock).getZone();
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/immutable-state.xml")
    void listAllTotes() throws Exception {
        ResultActions result = mockMvc.perform(get("/emptytotes")
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/emptytotes/list-all-totes/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/immutable-state.xml")
    void listAllEmptyTotes() throws Exception {
        ResultActions result = mockMvc.perform(get("/emptytotes")
                .param("filter", "fillingStatus==EMPTY")
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/emptytotes/list-all-empty-totes/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/immutable-state.xml")
    void listAllTotesMovedAfterDate() throws Exception {
        ResultActions result = mockMvc.perform(get("/emptytotes")
                .param("filter", "editDate=ge='2022-01-14 00:00:00'")
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/emptytotes/list-all-totes-moved-after-date/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/no-transporter/initial-state.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/no-transporter/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testRequestEmptyToteNoTransporter() throws Exception {
        mockMvc.perform(post("/emptytotes/requestEmptyTotes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/emptytotes/request-empty-totes-success/request.json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/success-request-manual-repl-vendor/initial-state.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/success-request-manual-repl-vendor/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testRequestEmptyToteSuccessManualReplVendor() throws Exception {
        mockMvc.perform(post("/emptytotes/requestEmptyTotes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/emptytotes/request-empty-totes-success/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/emptytotes/request-empty-totes-success/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/success-request-auto-repl-vendor/initial-state.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/success-request-auto-repl-vendor/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testRequestEmptyToteSuccessAutoReplVendor() throws Exception {
        when(servicebusClient.requestEmptyTotesDematic(any())).thenReturn(
                EmptyTotesResponse.builder().success(true).requestedToteCount(10).message("OK").build());

        mockMvc.perform(post("/emptytotes/requestEmptyTotes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/emptytotes/request-empty-totes-success/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/emptytotes/request-empty-totes-success/response.json")));

        verify(servicebusClient, times(1)).requestEmptyTotesDematic(
                requestEmptyToteCaptor.capture());
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/success-request-auto-repl-vendor/initial-state.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/success-request-auto-repl-vendor/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testRequestEmptyToteErrorAutoReplVendor() throws Exception {
        when(servicebusClient.requestEmptyTotesDematic(any())).thenReturn(
                EmptyTotesResponse.builder().success(false).requestedToteCount(3).message("FAIL").build());

        mockMvc.perform(post("/emptytotes/requestEmptyTotes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/emptytotes/request-empty-totes-success/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/emptytotes/request-empty-totes-fail/response.json")));

        verify(servicebusClient, times(1)).requestEmptyTotesDematic(
                requestEmptyToteCaptor.capture());
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/confirm-empty-tote-request/initial-state.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/confirm-empty-tote-request/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testConfirmEmptyToteRequest() throws Exception {
        mockMvc.perform(post("/emptytotes/confirmEmptyTote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/emptytotes/confirm-empty-tote-request/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    public void testRequestTooManyEmptyTotes() throws Exception {
        mockMvc.perform(post("/emptytotes/requestEmptyTotes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/emptytotes/request-empty-totes-fail/request.json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/get-all-balances/initial-state.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/get-all-balances/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getAllBalances() throws Exception {
        ResultActions result = mockMvc.perform(get("/emptytotes/balances")
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/emptytotes/get-all-balances/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/immutable-state.xml")
    void getBalancesFilterByRequestedCount() throws Exception {
        ResultActions result = mockMvc.perform(get("/emptytotes/balances")
                .param("filter", "requestedCount=gt=0")
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/emptytotes/get-balances-filter-by-requested-count/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/buffer-replenishment-manual-vendor/initial-state.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/buffer-replenishment-manual-vendor/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void bufferReplenishmentManualReplVendor() throws Exception {
        when(servicebusClient.createTransportOrder(any())).thenReturn(
                VendorApiResponse.builder().code("200").message("").build());
        mockMvc.perform(post("/emptytotes/bufferReplenishment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/emptytotes/buffer-replenishment-manual-vendor/request.json")))
                .andExpect(status().isOk());

        verify(servicebusClient, times(1)).createTransportOrder(createTransportOrderCaptor.capture());

        assertSoftly(assertions -> {
            CreateTransportOrderRequest request = createTransportOrderCaptor.getValue();

            assertions.assertThat(request).isNotNull();
            assertions.assertThat(request.getTransportUnitId()).isNotNull();
            assertions.assertThat(request.getTransportUnitId().getId()).isEqualTo("1");
            assertions.assertThat(request.getTargetLocation()).isNotNull();
            assertions.assertThat(request.getTargetLocation().getId()).isEqualTo("con_01-01");
        });
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/buffer-replenishment-auto-vendor/initial-state.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/buffer-replenishment-auto-vendor/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void bufferReplenishmentAutoReplVendor() throws Exception {
        when(servicebusClient.createTransportOrder(any())).thenReturn(
                VendorApiResponse.builder().code("200").message("").build());
        when(uuidGenerator.generate()).thenReturn(
                UUID.fromString("6d809e60-d707-11ea-9550-a9553a7b0571"),
                UUID.fromString("6d809e60-d707-11ea-9550-a9553a7b0572"),
                UUID.fromString("6d809e60-d707-11ea-9550-a9553a7b0573")
        );
        mockMvc.perform(post("/emptytotes/bufferReplenishment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/emptytotes/buffer-replenishment-auto-vendor/request.json")))
                .andExpect(status().isOk());

        verify(servicebusClient, times(3)).createTransportOrder(createTransportOrderCaptor.capture());

        assertSoftly(assertions -> {
            CreateTransportOrderRequest request = createTransportOrderCaptor.getValue();

            assertions.assertThat(request).isNotNull();
            assertions.assertThat(request.getTransportUnitId()).isNotNull();
            assertions.assertThat(request.getTransportUnitId().getId()).isIn("1", "2", "3");
            assertions.assertThat(request.getTargetLocation()).isNotNull();
            assertions.assertThat(request.getTargetLocation().getId()).isEqualTo("con_01-01");
        });
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/buffer-replenishment-not-empty-container/initial-state.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/buffer-replenishment-not-empty-container/initial-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void bufferReplenishmentNotEmptyContainer() throws Exception {
        mockMvc.perform(post("/emptytotes/bufferReplenishment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/emptytotes/buffer-replenishment-not-empty-container/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(
                        getFileContent(
                                "controller/emptytotes/buffer-replenishment-not-empty-container/response.json"
                        )));
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/buffer-replenishment-not-inbound-loc/initial-state.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/buffer-replenishment-not-inbound-loc/initial-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void bufferReplenishmentNotInboundLoc() throws Exception {
        mockMvc.perform(post("/emptytotes/bufferReplenishment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/emptytotes/buffer-replenishment-not-inbound-loc/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(
                        getFileContent(
                                "controller/emptytotes/buffer-replenishment-not-inbound-loc/response.json"
                        )));
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/change-priority-weight/initial-state.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/change-priority-weight/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void changePriorityWeightForLoc() throws Exception {
        mockMvc.perform(put("/emptytotes/changePriorityWeight")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/emptytotes/change-priority-weight/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/change-current-count/initial-state.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/change-current-count/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void changeCurrentCountForLoc() throws Exception {
        mockMvc.perform(put("/emptytotes/changeCurrentCount")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/emptytotes/change-current-count/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/make-tote-empty-put-on-conveyor/initial-state.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/make-tote-empty-put-on-conveyor/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void makeToteEmptyFromReceivingPutOnConveyor() throws Exception {
        when(servicebusClient.createTransportOrder(any())).thenReturn(
                VendorApiResponse.builder().code("200").message("").build());

        mockMvc.perform(put("/emptytotes/TM001/makeEmpty")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/emptytotes/make-tote-empty-put-on-conveyor/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/emptytotes/make-tote-empty-put-on-conveyor/response.json")));

        verify(servicebusClient, times(1)).createTransportOrder(createTransportOrderCaptor.capture());

        assertSoftly(assertions -> {
            CreateTransportOrderRequest request = createTransportOrderCaptor.getValue();

            assertions.assertThat(request).isNotNull();
            assertions.assertThat(request.getTransportUnitId()).isNotNull();
            assertions.assertThat(request.getTransportUnitId().getId()).isEqualTo("TM001");
            assertions.assertThat(request.getTargetLocation()).isNotNull();
            assertions.assertThat(request.getSourceLocation().getId()).isEqualTo("DO-03-IN");
            assertions.assertThat(request.getTargetLocation().getId()).isEqualTo("con_01-01");
        });
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/make-tote-empty-put-on-buffer/initial-state.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/make-tote-empty-put-on-buffer/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void makeToteEmptyFromReceivingPutOnBuffer() throws Exception {
        mockMvc.perform(put("/emptytotes/TM001/makeEmpty")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/emptytotes/make-tote-empty-put-on-buffer/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/emptytotes/make-tote-empty-put-on-buffer/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/make-tote-empty-from-cons-put-on-conveyor/initial-state.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/make-tote-empty-from-cons-put-on-conveyor/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void makeToteEmptyFromConsolidationPutOnConveyor() throws Exception {
        when(servicebusClient.createTransportOrder(any())).thenReturn(
                VendorApiResponse.builder().code("200").message("").build());

        mockMvc.perform(put("/emptytotes/TM001/makeEmpty")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/emptytotes/make-tote-empty-from-cons-put-on-conveyor/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                                "controller/emptytotes/make-tote-empty-from-cons-put-on-conveyor/response.json")));

        verify(servicebusClient, times(1)).createTransportOrder(createTransportOrderCaptor.capture());

        assertSoftly(assertions -> {
            CreateTransportOrderRequest request = createTransportOrderCaptor.getValue();

            assertions.assertThat(request).isNotNull();
            assertions.assertThat(request.getTransportUnitId()).isNotNull();
            assertions.assertThat(request.getTransportUnitId().getId()).isEqualTo("TM001");
            assertions.assertThat(request.getTargetLocation()).isNotNull();
            assertions.assertThat(request.getTargetLocation().getId()).isEqualTo("con_01-01");
        });
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/make-tote-empty-put-on-conveyor/initial-state2.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/make-tote-empty-put-on-conveyor/final-state2.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void makeToteEmptyFromReceivingPutOnConveyorToTargetLocation() throws Exception {
        when(servicebusClient.createTransportOrder(any())).thenReturn(
                VendorApiResponse.builder().code("200").message("").build());

        mockMvc.perform(put("/emptytotes/TM001/makeEmpty")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/emptytotes/make-tote-empty-from-cons-put-on-conveyor/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/emptytotes/make-tote-empty-put-on-conveyor/response.json")));

        verify(servicebusClient, times(1)).createTransportOrder(createTransportOrderCaptor.capture());

        assertSoftly(assertions -> {
            CreateTransportOrderRequest request = createTransportOrderCaptor.getValue();

            assertions.assertThat(request).isNotNull();
            assertions.assertThat(request.getTransportUnitId()).isNotNull();
            assertions.assertThat(request.getTransportUnitId().getId()).isEqualTo("TM001");
            assertions.assertThat(request.getTargetLocation()).isNotNull();
            assertions.assertThat(request.getTargetLocation().getId()).isEqualTo("con_01-02");
        });
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/make-tote-empty-put-on-buffer/initial-state2.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/make-tote-empty-put-on-buffer/final-state2.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void makeToteEmptyFromReceivingPutOnBufferManualVendor() throws Exception {
        mockMvc.perform(put("/emptytotes/TM001/makeEmpty").contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/emptytotes/make-tote-empty-from-cons-put-on-conveyor/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/emptytotes/make-tote-empty-put-on-buffer/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/make-tote-empty-from-cons-put-on-conveyor/initial-state2.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/make-tote-empty-put-on-conveyor/final-state2.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void makeToteEmptyFromConsPutOnConveyorToTargetLocation() throws Exception {
        when(servicebusClient.createTransportOrder(any())).thenReturn(
                VendorApiResponse.builder().code("200").message("").build());

        mockMvc.perform(put("/emptytotes/TM001/makeEmpty").contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/emptytotes/make-tote-empty-from-cons-put-on-conveyor/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/emptytotes/make-tote-empty-put-on-conveyor/response.json")));

        verify(servicebusClient, times(1)).createTransportOrder(createTransportOrderCaptor.capture());

        assertSoftly(assertions -> {
            CreateTransportOrderRequest request = createTransportOrderCaptor.getValue();

            assertions.assertThat(request).isNotNull();
            assertions.assertThat(request.getTransportUnitId()).isNotNull();
            assertions.assertThat(request.getTransportUnitId().getId()).isEqualTo("TM001");
            assertions.assertThat(request.getTargetLocation()).isNotNull();
            assertions.assertThat(request.getTargetLocation().getId()).isEqualTo("con_01-02");
        });
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/make-tote-empty-put-on-conveyor/initial-state-buffer.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/make-tote-empty-put-on-conveyor/final-state-buffer.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void balancingBetweenFloorsPutOnConveyorToAnotherFloor() throws Exception {
        when(servicebusClient.createTransportOrder(any())).thenReturn(
                VendorApiResponse.builder().code("200").message("").build());

        mockMvc.perform(put("/emptytotes/TM001/makeEmpty")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/emptytotes/make-tote-empty-from-cons-put-on-conveyor/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/emptytotes/make-tote-empty-put-on-conveyor/response.json")));

        verify(servicebusClient, times(1)).createTransportOrder(createTransportOrderCaptor.capture());

        assertSoftly(assertions -> {
            CreateTransportOrderRequest request = createTransportOrderCaptor.getValue();

            assertions.assertThat(request).isNotNull();
            assertions.assertThat(request.getTransportUnitId()).isNotNull();
            assertions.assertThat(request.getTransportUnitId().getId()).isEqualTo("TM001");
            assertions.assertThat(request.getTargetLocation()).isNotNull();
            assertions.assertThat(request.getSourceLocation().getId()).isEqualTo("PICKTO");
            assertions.assertThat(request.getTargetLocation().getId()).isEqualTo("con_01-02");
        });
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/make-tote-empty-put-on-buffer/initial-state-buffer.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/make-tote-empty-put-on-buffer/final-state-buffer.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void balancingBetweenFloorsPutOnBufferKeepOnThisFloor() throws Exception {
        mockMvc.perform(put("/emptytotes/TM001/makeEmpty")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/emptytotes/make-tote-empty-put-on-buffer/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/emptytotes/make-tote-empty-put-on-buffer/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/emptytotes/get-all-balances-updates-counters/initial-state.xml")
    @ExpectedDatabase(value = "/controller/emptytotes/get-all-balances-updates-counters/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getAllBalancesUpdatesCounters() throws Exception {
        ResultActions result = mockMvc.perform(get("/emptytotes/balances")
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/emptytotes/get-all-balances-updates-counters/response.json")));
    }
}
