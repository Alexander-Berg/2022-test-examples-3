package ru.yandex.market.sc.api.controller.v2;

import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.api.FlowOperationTestControllerCaller;
import ru.yandex.market.sc.api.domain.operation.ApiNextOperationDto;
import ru.yandex.market.sc.api.domain.operation.ApiOperationDto;
import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.core.domain.flow.FlowQueryService;
import ru.yandex.market.sc.core.domain.flow.repository.Flow;
import ru.yandex.market.sc.core.domain.flow.repository.FlowSystemName;
import ru.yandex.market.sc.core.domain.flow_operation_context.FlowOperationContextTransitionService;
import ru.yandex.market.sc.core.domain.flow_operation_context.repository.SortableAcceptSimpleFlowOperationContextData;
import ru.yandex.market.sc.core.domain.operation.OperationQueryService;
import ru.yandex.market.sc.core.domain.operation.repository.Operation;
import ru.yandex.market.sc.core.domain.operation.repository.OperationSystemName;
import ru.yandex.market.sc.core.domain.operation_log.model.OperationLogResult;
import ru.yandex.market.sc.core.domain.operation_log.repository.OperationLogRepository;
import ru.yandex.market.sc.core.domain.order.model.AcceptSortableRequestDto;
import ru.yandex.market.sc.core.domain.process.ProcessQueryService;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.zone.ZoneCommandService;
import ru.yandex.market.sc.core.domain.zone.repository.Zone;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.exception.ScNotFoundException;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.process.ProcessQueryService.COMMON_PROCESS_SYSTEM_NAME;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.SC_OPERATION_MODE_WITH_ZONE_ENABLED;
import static ru.yandex.market.sc.core.util.Constants.Header.SC_FLOW_TRACE_ID_HEADER;

@ScApiControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CommonOperationsControllerTest {
    private static final long UID = 124L;
    private static final long SC_ID = 1345L;

    private static final String FLOW_CHECK_IN_URL = String.format("/apiV2/sc/%s/process/COMMON/flow/COMMON/operation" +
            "/FLOW_CHECK_IN", SC_ID);
    private static final String ZONE_CHECK_IN_URL = String.format("/apiV2/sc/%s/process/COMMON/flow/COMMON/operation" +
            "/ZONE_CHECK_IN", SC_ID);

    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final OperationLogRepository operationLogRepository;
    private final ProcessQueryService processQueryService;
    private final FlowQueryService flowQueryService;
    private final OperationQueryService operationQueryService;
    private final FlowOperationContextTransitionService flowOperationContextTransitionService;
    private final ZoneCommandService zoneCommandService;

    private FlowOperationTestControllerCaller flowOperationControllerCaller;
    private SortingCenter sortingCenter;
    private Zone zone;

    @Qualifier("scApiJacksonObjectMapper")
    @Autowired
    private ObjectMapper jacksonObjectMapper;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter(SC_ID);
        testFactory.storedUser(sortingCenter, UID);
        testFactory.storedCommonProcess();
        zone = testFactory.storedZone(sortingCenter, "zone-0");

        this.flowOperationControllerCaller = new FlowOperationTestControllerCaller(
                mockMvc,
                UID,
                zone.getId(),
                SC_ID
        );
    }

    @Test
    @SneakyThrows
    void zoneCheckInWithoutZoneHeader() {
        mockMvc.perform(
                        MockMvcRequestBuilders.post(ZONE_CHECK_IN_URL)
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andExpect(jsonPath("$.error").value(ScErrorCode.ZONE_NOT_FOUND.name()))
                .andExpect(status().is(404));
        assertNoOperationLogsCreated();
    }

    @Test
    @SneakyThrows
    void zoneCheckInWithAbsentZoneTest() {
        flowOperationControllerCaller.checkInZone(345622L)
                .andExpect(jsonPath("$.error").value(ScErrorCode.ZONE_NOT_FOUND.name()))
                .andExpect(status().is(404));
        assertNoOperationLogsCreated();
    }

    @Test
    @SneakyThrows
    void zoneCheckInZoneFromAnotherScTest() {
        var anotherSc = testFactory.storedSortingCenter(12351L);
        var zone = testFactory.storedZone(anotherSc, "zone-2", Collections.emptyList());

        flowOperationControllerCaller.checkInZone(zone.getId())
                .andExpect(jsonPath("$.error").value(ScErrorCode.ZONE_NOT_FROM_CURRENT_SC.name()))
                .andExpect(status().is(400));
        assertNoOperationLogsCreated();
    }

    @Test
    @SneakyThrows
    void zoneCheckInDeletedZoneTest() {
        var zone = testFactory.storedZone(sortingCenter, "zone-1", Collections.emptyList());
        zoneCommandService.deleteZone(sortingCenter, zone.getId());

        flowOperationControllerCaller.checkInZone(zone.getId())
                .andExpect(jsonPath("$.error")
                        .value(ScErrorCode.ZONE_WAS_DELETED_WARNING.name())
                ).andExpect(status().is(400));
        assertNoOperationLogsCreated();
    }

    @Test
    @SneakyThrows
    void zoneCheckInWithoutFlowsTest() {
        var zone = testFactory.storedZone(sortingCenter, "zone-1", Collections.emptyList());
        flowOperationControllerCaller.checkInZone(zone.getId())
                .andExpect(status().isOk())
                .andExpect(content().json("{\"flows\":[]}"));
        assertOperationLog(zone.getId(), null, OperationSystemName.ZONE_CHECK_IN.name(), null);
    }

    @Test
    @SneakyThrows
    void zoneCheckInTest() {
        var operation111 = testFactory.storedOperation("operation-system-111", "operation-display-111");
        var operation112 = testFactory.storedOperation("operation-system-112", "operation-display-112");
        var flow11 = testFactory.storedFlow("flow-system-11", "flow-display-11", List.of(operation111, operation112));
        var flow12 = testFactory.storedFlow("flow-system-12", "flow-display-12", Collections.emptyList());
        var process1 = testFactory.storedProcess("process-system-1", "process-display-1", List.of(flow11, flow12));

        var operation211 = testFactory.storedOperation("operation-system-211", "operation-display-211");
        var flow21 = testFactory.storedFlow("flow-system-21", "flow-display-21", List.of(operation211));
        var process2 = testFactory.storedProcess("process-system-2", "process-display-2", List.of(flow21));
        var zone = testFactory.storedZone(sortingCenter, "zone-1", List.of(process1, process2));

        var expectedJson = new String(Files.readAllBytes(
                new ClassPathResource("/zone_check_in_response.json").getFile().toPath()
        ));
        flowOperationControllerCaller.checkInZone(zone.getId())
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson, false));
        assertOperationLog(zone.getId(), null, OperationSystemName.ZONE_CHECK_IN.name(), null);
    }

    @Test
    @SneakyThrows
    void errorWhenZoneDeletedCheckIn() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SC_OPERATION_MODE_WITH_ZONE_ENABLED, true);
        var zoneDeleted = testFactory.storedDeletedZone(sortingCenter, "zn-deleted");
        flowOperationControllerCaller.checkInZone(zoneDeleted.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(ScErrorCode.ZONE_WAS_DELETED_WARNING.name()));
    }

    @Test
    @SneakyThrows
    void leaveZoneTest() {
        flowOperationControllerCaller.leaveZone(zone.getId()).andExpect(status().isOk());
        assertOperationLog(zone.getId(), null, OperationSystemName.ZONE_LEAVE.name(), null);
    }

    @Test
    @SneakyThrows
    void flowCheckInWithoutZoneHeader() {
        mockMvc.perform(
                MockMvcRequestBuilders.post(FLOW_CHECK_IN_URL)
                        .header("Authorization", "OAuth uid-" + UID)
        ).andExpect(status().is4xxClientError());
        assertNoOperationLogsCreated();
    }

    @Test
    @SneakyThrows
    void flowCheckInWithAbsentZoneTest() {
        var flow = testFactory.storedFlow("some_flow", "some_flow", Collections.emptyList());
        flowOperationControllerCaller.checkInFlow(flow.getSystemName(), 345622L)
                .andExpect(status().is4xxClientError());
        assertNoOperationLogsCreated();
    }

    @Test
    @SneakyThrows
    void flowCheckInWithAbsentFlowTest() {
        flowOperationControllerCaller.checkInFlow("unknown_flow").andExpect(status().is4xxClientError());
        assertNoOperationLogsCreated();
    }

    @Test
    @SneakyThrows
    void flowCheckInTest() {
        var operation = testFactory.storedOperation(OperationSystemName.SORTABLE_ACCEPT_SIMPLE.name());
        var configData = testFactory.toJsonNode("{\"configData\": \"someConfigData\"}");
        var knownFlow = testFactory.storedFlow(
                "known_flow",
                "known_flow",
                Map.of(operation, configData)
        );
        var expectedContent = new ApiNextOperationDto(
                new ApiOperationDto(
                        OperationSystemName.SORTABLE_ACCEPT_SIMPLE.name(),
                        OperationSystemName.SORTABLE_ACCEPT_SIMPLE.name(),
                        configData,
                        new SortableAcceptSimpleFlowOperationContextData()
                )
        );
        var result = flowOperationControllerCaller.checkInFlow(knownFlow.getSystemName())
                .andExpect(status().isOk())
                .andExpect(content().json(jacksonObjectMapper.writeValueAsString(expectedContent)))
                .andReturn();
        assertOperationLog(zone.getId(), knownFlow.getSystemName(), OperationSystemName.FLOW_CHECK_IN.name(),
                result.getResponse().getHeader(SC_FLOW_TRACE_ID_HEADER));
    }

    @Test
    @SneakyThrows
    void flowCheckInDefaultOperationTest() {
        var operationName = "SOME_OPERATION_OP_DEFAULT";
        var operation = testFactory.storedOperation(operationName);
        var operationToConfigMap = new HashMap<Operation, JsonNode>();
        operationToConfigMap.put(operation, null);

        var knownFlow = testFactory.storedFlow(
                "known_flow",
                "known_flow",
                operationToConfigMap
        );
        var expectedContent = new ApiNextOperationDto(new ApiOperationDto(operationName, operationName));
        var result = flowOperationControllerCaller.checkInFlow(knownFlow.getSystemName())
                .andExpect(status().isOk())
                .andExpect(content().json(jacksonObjectMapper.writeValueAsString(expectedContent)))
                .andReturn();
        assertOperationLog(zone.getId(), knownFlow.getSystemName(), OperationSystemName.FLOW_CHECK_IN.name(),
                result.getResponse().getHeader(SC_FLOW_TRACE_ID_HEADER));
    }

    @Test
    @SneakyThrows
    void flowCheckInIdempotencyTest() {
        var operation = testFactory.storedOperation(OperationSystemName.SORTABLE_ACCEPT_SIMPLE.name());
        var configData = testFactory.toJsonNode("{\"configData\": \"someConfigData\"}");
        var knownFlow = testFactory.storedFlow(
                "known_flow",
                "known_flow",
                Map.of(operation, configData)
        );
        var expectedContent = new ApiNextOperationDto(
                new ApiOperationDto(
                        OperationSystemName.SORTABLE_ACCEPT_SIMPLE.name(),
                        OperationSystemName.SORTABLE_ACCEPT_SIMPLE.name(),
                        configData,
                        new SortableAcceptSimpleFlowOperationContextData()
                )
        );
        flowOperationControllerCaller.checkInFlow(knownFlow.getSystemName())
                .andExpect(status().isOk())
                .andExpect(content().json(jacksonObjectMapper.writeValueAsString(expectedContent)))
                .andReturn();

        flowOperationControllerCaller.checkInFlow(knownFlow.getSystemName())
                .andExpect(status().isOk())
                .andExpect(content().json(jacksonObjectMapper.writeValueAsString(expectedContent)))
                .andReturn();
        assertThat(operationLogRepository.findAll().size()).isEqualTo(2);
    }

    @Test
    @SneakyThrows
    @DisplayName("Чекин во флоу после потери сессии")
    void flowCheckInRestoreContextData() {
        var configData = testFactory.toJsonNode("{\"validateMerchant\": true, \"processCancelled\": true}");
        var flow = buildSingleOperationFlow(configData);
        var process = testFactory.storedProcess("some_process", "some_process", List.of(flow));
        var place = testFactory.createForToday(
                TestFactory.order(sortingCenter).externalId("1").places("1-0").build()
        ).getPlace("1-0");
        var cancelledPlace = testFactory.createForToday(
                TestFactory.order(sortingCenter).externalId("2").places("2-0").build()
        ).cancel().getPlace("2-0");
        var warehouse = Objects.requireNonNull(place.getWarehouseFrom());

        var flowTraceId = flowOperationControllerCaller.checkInFlow(flow.getSystemName())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader(SC_FLOW_TRACE_ID_HEADER);

        flowOperationControllerCaller.preAccept(
                process.getSystemName(),
                flow.getSystemName(),
                new AcceptSortableRequestDto(List.of(place.getMainPartnerCode()), null, null),
                flowTraceId
        ).andExpect(status().isOk());

        //возврат отмененной без приемки, не должен попасть в востановление сессии
        flowOperationControllerCaller.preAccept(
                process.getSystemName(),
                flow.getSystemName(),
                new AcceptSortableRequestDto(
                        List.of(cancelledPlace.getMainPartnerCode()),
                        null,
                        false
                ),
                flowTraceId
        ).andExpect(status().isOk());

        var expectedContent = new ApiNextOperationDto(
                new ApiOperationDto(
                        OperationSystemName.SORTABLE_ACCEPT_SIMPLE.name(),
                        OperationSystemName.SORTABLE_ACCEPT_SIMPLE.name(),
                        configData,
                        new SortableAcceptSimpleFlowOperationContextData(
                                List.of(place.getMainPartnerCode()),
                                null,
                                warehouse.getId(),
                                warehouse.getIncorporation()
                        )
                )
        );

        flowOperationControllerCaller.checkInFlow(flow.getSystemName())
                .andExpect(status().isOk())
                .andExpect(content().json(jacksonObjectMapper.writeValueAsString(expectedContent)));

        assertThat(operationLogRepository.findAll().size()).isEqualTo(4);
    }

    @DisplayName("Вызов NEXT_OPERATION в рамках последней операции данного флоу")
    @Test
    @SneakyThrows
    void nextOperationForLastOperationInFlowTest() {
        var flow = buildSingleOperationFlow();

        var flowTraceId = flowOperationControllerCaller.checkInFlowAndGetFlowTraceId(flow.getSystemName());

        flowOperationControllerCaller.nextOperation(flowTraceId)
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
        assertThat(operationLogRepository.findAll().size()).isEqualTo(2);
    }

    @DisplayName("Идемпотентность NEXT_OPERATION")
    @Test
    @SneakyThrows
    void nextOperationIdempotencyTest() {
        var flow = buildSingleOperationFlow();

        var flowTraceId = flowOperationControllerCaller.checkInFlowAndGetFlowTraceId(flow.getSystemName());

        var nextOperationResult1 = flowOperationControllerCaller.nextOperation(flowTraceId)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var nextOperationResult2 = flowOperationControllerCaller.nextOperation(flowTraceId)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(nextOperationResult1).isEqualTo(nextOperationResult2);

        assertThat(operationLogRepository.findAll().size()).isEqualTo(3);
    }

    @DisplayName("Вызов NEXT_OPERATION после прохода по флоу")
    @Test
    @SneakyThrows
    void nextOperationAfterLastOperationOnFlowTest() {
        var flow = buildSingleOperationFlow();

        var flowTraceId = flowOperationControllerCaller.checkInFlowAndGetFlowTraceId(flow.getSystemName());

        finishOperation(flowTraceId);

        flowOperationControllerCaller.nextOperation(flowTraceId)
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));

        flowOperationControllerCaller.nextOperation(flowTraceId)
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
        assertThat(operationLogRepository.findAll().size()).isEqualTo(3);
    }

    @DisplayName("Вызов /finish и NEXT_OPERATION после FLOW_LEAVE")
    @Test
    @SneakyThrows
    void nextOperationAfterLeaveTest() {
        var flow = buildSingleOperationFlow();

        var flowTraceId = flowOperationControllerCaller.checkInFlowAndGetFlowTraceId(flow.getSystemName());

        flowOperationControllerCaller.leaveFlow(flowTraceId)
                .andExpect(status().isOk());

        assertThrows(ScNotFoundException.class,
                () -> finishOperation(flowTraceId),
                "Отсутствует контекст текущей Операции");

        flowOperationControllerCaller.nextOperation(flowTraceId)
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));

        flowOperationControllerCaller.nextOperation(flowTraceId)
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
        assertThat(operationLogRepository.findAll().size()).isEqualTo(4);
    }

    private void finishOperation(String flowTraceId) {
        flowOperationContextTransitionService.transitFlowOperation(flowTraceId);
    }

    private Flow buildSingleOperationFlow() {
        var operation = testFactory.storedOperation(OperationSystemName.SORTABLE_ACCEPT_SIMPLE.name());
        var configData = testFactory.toJsonNode("{\"validateMerchant\": true}");

        return testFactory.storedFlow(
                FlowSystemName.MERCHANT_INITIAL_ACCEPTANCE.name(),
                FlowSystemName.MERCHANT_INITIAL_ACCEPTANCE.name(),
                Map.of(operation, configData)
        );
    }

    private Flow buildSingleOperationFlow(JsonNode configData) {
        var operation = testFactory.storedOperation(OperationSystemName.SORTABLE_ACCEPT_SIMPLE.name());

        return testFactory.storedFlow(
                FlowSystemName.MERCHANT_INITIAL_ACCEPTANCE.name(),
                FlowSystemName.MERCHANT_INITIAL_ACCEPTANCE.name(),
                Map.of(operation, configData)
        );
    }

    private void assertOperationLog(Long zoneId, @Nullable String suffix, String operationSystemName,
                                    @Nullable String flowTraceId) {
        var operationLogs = operationLogRepository.findAll();
        assertThat(operationLogs.size()).isEqualTo(1);
        var operationLog = operationLogs.get(0);

        assertThat(operationLog.getSuffix()).isEqualTo(suffix);
        assertThat(operationLog.getFlowTraceId()).isEqualTo(flowTraceId);
        assertThat(operationLog.getResult()).isEqualTo(OperationLogResult.OK);
        assertThat(operationLog.getZoneId()).isEqualTo(zoneId);
        assertThat(operationLog.getUser().getUid()).isEqualTo(UID);
        assertThat(operationLog.getSortingCenter().getId()).isEqualTo(sortingCenter.getId());
        var commonProcessId = processQueryService.findProcessId(COMMON_PROCESS_SYSTEM_NAME);
        assertThat(operationLog.getProcessId()).isEqualTo(commonProcessId);
        var commonFlowId = flowQueryService.findFlowId(FlowSystemName.COMMON.name());
        assertThat(operationLog.getFlowId()).isEqualTo(commonFlowId);
        var operationId = operationQueryService.findOperationId(operationSystemName);
        assertThat(operationLog.getOperationId()).isEqualTo(operationId);
    }

    private void assertNoOperationLogsCreated() {
        assertThat(operationLogRepository.findAll()).isEmpty();
    }
}
