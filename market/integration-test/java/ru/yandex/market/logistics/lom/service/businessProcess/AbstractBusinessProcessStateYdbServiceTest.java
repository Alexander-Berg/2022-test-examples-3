package ru.yandex.market.logistics.lom.service.businessProcess;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.ydb.table.result.ResultSetReader;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.converter.ydb.BusinessProcessStateConverter;
import ru.yandex.market.logistics.lom.converter.ydb.BusinessProcessStateStatusHistoryConverter;
import ru.yandex.market.logistics.lom.entity.BusinessProcessState;
import ru.yandex.market.logistics.lom.entity.BusinessProcessStateEntityId;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.entity.enums.EntityType;
import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateEntityIdYdb;
import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateStatusHistoryYdb;
import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateYdb;
import ru.yandex.market.logistics.lom.jobs.model.ExecutionQueueItemPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.model.RetryBusinessProcessesPayload;
import ru.yandex.market.logistics.lom.repository.BusinessProcessStateRepository;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateEntityIdTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateStatusHistoryTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateTableDescription;
import ru.yandex.market.logistics.lom.service.process.BusinessProcessStateService;
import ru.yandex.market.logistics.lom.utils.YdbTestUtils;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.ydb.converter.BusinessProcessStateEntityIdYdbConverter;
import ru.yandex.market.logistics.lom.utils.ydb.converter.BusinessProcessStateYdbConverter;
import ru.yandex.market.ydb.integration.YdbTableDescription;
import ru.yandex.market.ydb.integration.YdbTemplate;
import ru.yandex.market.ydb.integration.query.QFrom;
import ru.yandex.market.ydb.integration.query.QSelect;
import ru.yandex.market.ydb.integration.query.YdbSelect;
import ru.yandex.market.ydb.integration.utils.ListConverter;

@ParametersAreNonnullByDefault
@DisplayName("Работа с состоянием бизнес-процесса")
public class AbstractBusinessProcessStateYdbServiceTest extends AbstractContextualYdbTest {

    protected static final OrderHistoryEventAuthor AUTHOR = new OrderHistoryEventAuthor()
        .setTvmServiceId(222L)
        .setYandexUid(BigDecimal.ONE);

    protected static final Long PARENT_ID = 777L;
    protected static final Instant FIXED_TIME = Instant.parse("2021-08-30T11:12:13.00Z");
    protected static final String COMMENT = "comment";

    private static final QueueType YDB_QUEUE_TYPE = QueueType.FULFILLMENT_CREATE_ORDER_EXTERNAL;
    protected static final Function<Long, Long> SEQUENCE_ID_FUNC = id -> id + 10;
    private static final BusinessProcessStatus YDB_PROCESS_STATUS = BusinessProcessStatus.ENQUEUED;

    @Autowired
    protected BusinessProcessStateService businessProcessStateService;
    @Autowired
    protected BusinessProcessStateRepository businessProcessStateRepository;

    @Autowired
    protected BusinessProcessStateTableDescription businessProcessStateTable;
    @Autowired
    protected BusinessProcessStateEntityIdTableDescription businessProcessStateEntityIdTable;
    @Autowired
    protected BusinessProcessStateStatusHistoryTableDescription businessProcessStateStatusHistoryTableDescription;

    @Autowired
    protected BusinessProcessStateYdbConverter businessProcessStateYdbConverter;
    @Autowired
    protected BusinessProcessStateEntityIdYdbConverter businessProcessStateEntityIdYdbConverter;
    @Autowired
    private BusinessProcessStateConverter businessProcessStateConverter;
    @Autowired
    private BusinessProcessStateStatusHistoryConverter statusHistoryConverter;

    @BeforeEach
    void setUp() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
        super.tearDownYdb();
    }

    @Nonnull
    @SneakyThrows
    protected BusinessProcessStateYdb getBusinessProcessStateYdb(ExecutionQueueItemPayload payload, Long id) {
        return new BusinessProcessStateYdb()
            .setId(id)
            .setQueueType(QueueType.GET_ORDER_LABEL)
            .setEntityIds(
                payload.getEntityIds().stream()
                    .map(
                        entityId -> BusinessProcessStateEntityIdYdb.of(entityId.getEntityType(), entityId.getEntityId())
                    )
                    .collect(Collectors.toList())
            )
            .setSequenceId(payload.getSequenceId())
            .setStatus(BusinessProcessStatus.ENQUEUED)
            .setAuthor(AUTHOR)
            .setPayload(objectMapper.writeValueAsString(payload))
            .setMessage(COMMENT)
            .setParentId(PARENT_ID)
            .setCreated(clock.instant())
            .setUpdated(clock.instant());
    }

    @Nonnull
    @SneakyThrows
    protected BusinessProcessStateYdb getBusinessProcessStateYdb(
        Long id,
        Long sequenceId,
        QueueType queueType,
        BusinessProcessStatus status,
        ExecutionQueueItemPayload payload
    ) {
        return new BusinessProcessStateYdb()
            .setId(id)
            .setSequenceId(sequenceId)
            .setQueueType(queueType)
            .setEntityIds(
                payload.getEntityIds().stream()
                    .map(
                        entityId -> BusinessProcessStateEntityIdYdb.of(
                            entityId.getEntityType(),
                            entityId.getEntityId()
                        )
                    )
                    .collect(Collectors.toList())
            )
            .setStatus(status)
            .setAuthor(AUTHOR)
            .setPayload(objectMapper.writeValueAsString(payload))
            .setMessage(COMMENT)
            .setParentId(PARENT_ID)
            .setCreated(clock.instant())
            .setUpdated(clock.instant());
    }

    @Nonnull
    @SneakyThrows
    protected BusinessProcessState getExpectedProcessState(
        Long id,
        Long sequenceId,
        QueueType queueType,
        BusinessProcessStatus status,
        ExecutionQueueItemPayload payload
    ) {
        BusinessProcessState processState = new BusinessProcessState()
            .setId(id)
            .setSequenceId(sequenceId)
            .setQueueType(queueType)
            .setStatus(status)
            .setAuthor(AUTHOR)
            .setComment(COMMENT)
            .setPayload(objectMapper.writeValueAsString(payload))
            .setParentId(PARENT_ID);
        processState.setEntityIds(
            payload.getEntityIds().stream()
                .map(
                    entityId -> new BusinessProcessStateEntityId()
                        .setBusinessProcessState(processState)
                        .setEntityId(entityId.getEntityId())
                        .setEntityType(entityId.getEntityType())
                )
                .collect(Collectors.toList())
        );
        processState.setCreated(clock.instant())
            .setUpdated(clock.instant());

        return processState;
    }

    protected final void assertYdbContainsBusinessProcessWithEntities(
        List<BusinessProcessStateYdb> businessProcesses
    ) {
        assertYdbContainsBusinessProcessWithEntities(businessProcesses, FIXED_TIME);
    }

    protected final void assertYdbContainsBusinessProcessWithEntities(
        List<BusinessProcessStateYdb> businessProcesses,
        Instant nowTime
    ) {
        Map<Long, BusinessProcessStateYdb> actualBusinessProcesses = findAllProcesses().stream()
            .collect(Collectors.toMap(BusinessProcessStateYdb::getId, Function.identity()));
        softly.assertThat(actualBusinessProcesses.values())
            .hasSameSizeAs(businessProcesses);
        for (BusinessProcessStateYdb expectedProcess : businessProcesses) {
            BusinessProcessStateYdb actualProcess = actualBusinessProcesses.get(expectedProcess.getId());
            softly.assertThat(actualProcess)
                .usingRecursiveComparison()
                .isEqualTo(expectedProcess);
        }

        assertExportedFilled(businessProcesses, nowTime);
    }

    protected final void assertYdbNotContainsProcesses() {
        softly.assertThat(findAllProcesses()).isEmpty();
    }

    protected void insertProcessesToYdb(Long... processStateIds) {
        for (long id : processStateIds) {
            insertProcessToYdb(id);
        }
    }

    protected void insertProcessesToYdb(Map<Long, BusinessProcessStatus> processStatusMap) {
        processStatusMap.forEach(this::insertProcessToYdb);
    }

    protected void insertProcessToYdb(Long processStateId) {
        insertProcessToYdb(processStateId, YDB_PROCESS_STATUS);
    }

    protected void insertProcessToYdb(
        Long processStateId,
        BusinessProcessStatus status
    ) {
        insertAllIntoTable(
            businessProcessStateTable,
            List.of(
                getBusinessProcessStateYdb(
                    processStateId,
                    SEQUENCE_ID_FUNC.apply(processStateId),
                    YDB_QUEUE_TYPE,
                    status,
                    getYdbPayload(processStateId)
                )
            ),
            businessProcessStateYdbConverter::mapToItem
        );

        insertAllIntoTable(
            businessProcessStateEntityIdTable,
            List.of(
                new BusinessProcessStateEntityIdYdb()
                    .setEntityType(EntityType.ORDER)
                    .setEntityId(2L),
                new BusinessProcessStateEntityIdYdb()
                    .setEntityType(EntityType.WAYBILL_SEGMENT)
                    .setEntityId(3L)
            ),
            (ydbTableDescription, o) -> businessProcessStateEntityIdYdbConverter.mapToItem(
                ydbTableDescription,
                o,
                processStateId
            )
        );
    }

    @Nonnull
    private ExecutionQueueItemPayload getYdbPayload(long processId) {
        return PayloadFactory.createWaybillSegmentPayload(
            2,
            3,
            SEQUENCE_ID_FUNC.apply(processId).toString(),
            1
        );
    }

    @Nonnull
    protected BusinessProcessState getExpectedYdbProcessState(long processId) {
        return getExpectedProcessState(
            processId,
            SEQUENCE_ID_FUNC.apply(processId),
            YDB_QUEUE_TYPE,
            YDB_PROCESS_STATUS,
            getYdbPayload(processId)
        );
    }

    protected void assertProcessesAreEqual(
        Collection<BusinessProcessState> actual,
        Collection<BusinessProcessState> expected
    ) {
        Map<Long, BusinessProcessState> actualProcesses = actual.stream()
            .collect(Collectors.toMap(BusinessProcessState::getId, Function.identity()));
        softly.assertThat(actualProcesses)
            .hasSize(expected.size());
        expected.forEach(
            process -> assertProcessesAreEqual(actualProcesses.get(process.getId()), process)
        );
    }

    protected void assertProcessesAreEqual(BusinessProcessState actual, BusinessProcessState expected) {
        softly.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("entityIds.id", "entityIds.businessProcessState", "retryAttempt")
            .isEqualTo(expected);
    }

    protected final void assertProcessStatusHistory(BusinessProcessStateStatusHistoryYdb expectedStatus) {
        assertProcessStatusHistory(List.of(expectedStatus));
    }

    protected final void assertProcessStatusHistory(List<BusinessProcessStateStatusHistoryYdb> expectedStatuses) {
        List<BusinessProcessStateStatusHistoryYdb> actualStatuses = YdbTestUtils.findAll(
            ydbTemplate,
            businessProcessStateStatusHistoryTableDescription,
            statusHistoryConverter::convertList
        );

        softly.assertThat(actualStatuses)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(expectedStatuses);
    }

    @Nonnull
    private List<BusinessProcessStateYdb> findAllProcesses() {
        return ydbTemplate.selectList(
            YdbSelect.select(
                QSelect.of(
                        businessProcessStateTable.fields(),
                        businessProcessStateEntityIdTable.fields()
                    )
                    .from(QFrom.table(
                        businessProcessStateTable,
                        QFrom.JoinClause.leftJoin(
                            businessProcessStateEntityIdTable,
                            QFrom.OnClause
                                .on(
                                    businessProcessStateTable.getIdHash().eq(
                                            businessProcessStateEntityIdTable.getBusinessProcessStateIdHash()
                                        )
                                        .and(
                                            businessProcessStateTable.getId().eq(
                                                businessProcessStateEntityIdTable
                                                    .getBusinessProcessStateId()
                                            ))
                                )
                        )
                    ))
                    .select()
            ),
            YdbTemplate.DEFAULT_READ,
            businessProcessStateConverter::convertBusinessProcesses
        );
    }

    private void assertExportedFilled(
        List<BusinessProcessStateYdb> businessProcesses,
        Instant nowTime
    ) {
        int savedProcessesAmount = businessProcesses.size();
        List<Instant> exported = YdbTestUtils.findAll(
            ydbTemplate,
            businessProcessStateTable,
            getExportedConverter(businessProcessStateTable.getExported().alias())
        );
        softly.assertThat(exported).hasSize(savedProcessesAmount);
        softly.assertThat(exported).containsOnly(nowTime);

        int savedEntitiesAmount = (int) businessProcesses.stream()
            .map(BusinessProcessStateYdb::getEntityIds)
            .mapToLong(List::size)
            .sum();
        exported = YdbTestUtils.findAll(
            ydbTemplate,
            businessProcessStateEntityIdTable,
            getExportedConverter(businessProcessStateEntityIdTable.getExported().alias())
        );
        softly.assertThat(exported).hasSize(savedEntitiesAmount);
        if (savedEntitiesAmount > 0) {
            softly.assertThat(exported).containsOnly(nowTime);
        }
    }

    @Nonnull
    private ListConverter<Instant> getExportedConverter(String exportedAliasField) {
        return queryResult -> {
            if (queryResult.isEmpty()) {
                return List.of();
            }
            ResultSetReader resultSetReader = queryResult.getResultSet(0);
            List<Instant> exportedTimes = new ArrayList<>(resultSetReader.getRowCount());
            while (resultSetReader.next()) {
                exportedTimes.add(
                    resultSetReader.getColumn(exportedAliasField).getTimestamp()
                );
            }
            return exportedTimes;
        };
    }

    protected final void assertYdbContainsBusinessProcessWithEntities(BusinessProcessStateYdb businessProcessState) {
        assertYdbContainsBusinessProcessWithEntities(List.of(businessProcessState));
    }

    protected void assertRetryTaskCreated(Long retriedProcessId) {
        assertRetryTaskCreated(REQUEST_ID, List.of(retriedProcessId));
    }

    protected void assertRetryTaskCreated(List<Long> retriedProcessIds) {
        assertRetryTaskCreated(REQUEST_ID, retriedProcessIds);
    }

    protected void assertRetryTaskCreated(String requestId, List<Long> retriedProcessIds) {
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.RETRY_BUSINESS_PROCESSES,
            new RetryBusinessProcessesPayload(requestId, retriedProcessIds)
        );
    }

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(
            businessProcessStateTable,
            businessProcessStateEntityIdTable,
            businessProcessStateStatusHistoryTableDescription
        );
    }
}
