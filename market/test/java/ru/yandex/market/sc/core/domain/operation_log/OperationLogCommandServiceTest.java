package ru.yandex.market.sc.core.domain.operation_log;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.flow.repository.FlowSystemName;
import ru.yandex.market.sc.core.domain.operation.repository.OperationSystemName;
import ru.yandex.market.sc.core.domain.operation_log.model.OperationLogRequest;
import ru.yandex.market.sc.core.domain.operation_log.model.OperationLogResult;
import ru.yandex.market.sc.core.domain.operation_log.repository.OperationLog;
import ru.yandex.market.sc.core.domain.operation_log.repository.OperationLogRepository;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.domain.process.ProcessQueryService.COMMON_PROCESS_SYSTEM_NAME;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OperationLogCommandServiceTest {

    private final OperationLogCommandService operationLogCommandService;
    private final OperationLogRepository operationLogRepository;
    private final Clock clock;
    private final TestFactory testFactory;

    @Test
    void createOperationLogTest() {
        var sortingCenter = testFactory.storedSortingCenter();
        var user = testFactory.storedUser(sortingCenter, 124L);
        var operation = testFactory.storedOperation(OperationSystemName.FLOW_CHECK_IN.name(), "Чек-ин во флоу");
        var flow = testFactory.storedFlow(FlowSystemName.COMMON.name(), "Общий", List.of(operation));
        var process = testFactory.storedProcess(COMMON_PROCESS_SYSTEM_NAME, "Общий", List.of(flow));
        var zone = testFactory.storedZone(sortingCenter, "zone-1", Collections.emptyList());
        var cell1 = testFactory.storedCell(sortingCenter, "123");
        var cell2 = testFactory.storedCell(sortingCenter, "345");

        var logRequest = OperationLogRequest.builder()
                .sortingCenter(sortingCenter)
                .user(user)
                .zoneId(zone)
                .process(COMMON_PROCESS_SYSTEM_NAME)
                .flow(FlowSystemName.COMMON)
                .operation(OperationSystemName.FLOW_CHECK_IN)
                .flowTraceId("flow-trace-id")
                .suffix("suffix")
                .fixedAt(Instant.now(clock))
                .archiveId(123120L)
                .cellBeforeId(cell1.getId())
                .cellAfterId(cell2.getId())
                .fixedAt(Instant.now(clock))
                .stageBefore(35345L)
                .stageAfter(5695L)
                .sortableId(1512350L)
                .placeId(3452354L)
                .sortableBarcode("barcode")
                .sortableType("sortable-type")
                .errorCode(ScErrorCode.ZONE_BY_ID_NOT_FOUND)
                .result(OperationLogResult.ERROR)
                .build();

        operationLogCommandService.createOperationLog(logRequest);

        var expectedLog = new OperationLog(
                sortingCenter,
                user,
                zone.getId(),
                logRequest.getFixedAt(),
                process.getId(),
                flow.getId(),
                operation.getId(),
                logRequest.getResult(),
                logRequest.getFlowTraceId(),
                logRequest.getSuffix(),
                logRequest.getWorkstationId(),
                logRequest.getErrorCode(),
                logRequest.getPlaceId(),
                logRequest.getSortableId(),
                logRequest.getSortableType(),
                logRequest.getSortableBarcode(),
                cell1.getId(),
                cell2.getId(),
                logRequest.getStageBefore(),
                logRequest.getStageAfter(),
                logRequest.getArchiveId()
        );

        var operationLogs = operationLogRepository.findAll();
        assertThat(operationLogs.size()).isEqualTo(1);
        var operationLog = operationLogs.get(0);
        assertThat(operationLog)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(expectedLog);
    }

    @Test
    void workstationLog() {
        var sortingCenter = testFactory.storedSortingCenter();
        var user = testFactory.storedUser(sortingCenter, 140L);
        var operation = testFactory.storedOperation(OperationSystemName.ZONE_CHECK_IN.name(), "Чек-ин в зоне");
        var flow = testFactory.storedFlow(FlowSystemName.COMMON.name(), "Общий", List.of(operation));
        var process = testFactory.storedProcess(COMMON_PROCESS_SYSTEM_NAME, "Общий", List.of(flow));
        var zone = testFactory.storedZone(sortingCenter, "zone-1", Collections.emptyList());
        var ws = testFactory.storedWorkstation(sortingCenter, "ws-1", zone.getId(), process);
        var logRequest = OperationLogRequest.builder()
                .sortingCenter(sortingCenter)
                .user(user)
                .zoneId(ws)
                .workstationId(ws)
                .process(COMMON_PROCESS_SYSTEM_NAME)
                .flow(FlowSystemName.COMMON)
                .operation(OperationSystemName.ZONE_CHECK_IN)
                .flowTraceId("flow-trace-id")
                .suffix("suffix")
                .fixedAt(Instant.now(clock))
                .fixedAt(Instant.now(clock))
                .sortableBarcode("barcode")
                .sortableType("sortable-type")
                .result(OperationLogResult.OK)
                .build();

        operationLogCommandService.createOperationLog(logRequest);
        var operationLog = operationLogRepository.findAll().stream().findFirst().orElse(null);
        assertThat(operationLog).isNotNull();
        assertThat(operationLog.getWorkstationId()).isEqualTo(ws.getId());
        assertThat(operationLog.getZoneId()).isEqualTo(ws.getParentId());
    }
}
