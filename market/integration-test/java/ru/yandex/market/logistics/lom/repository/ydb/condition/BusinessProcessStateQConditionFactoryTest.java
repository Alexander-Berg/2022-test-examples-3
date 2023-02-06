package ru.yandex.market.logistics.lom.repository.ydb.condition;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.entity.enums.EntityType;
import ru.yandex.market.logistics.lom.filter.BusinessProcessStateFilter;
import ru.yandex.market.logistics.lom.jobs.model.EntityId;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateEntityIdTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateTableDescription;
import ru.yandex.market.logistics.lom.utils.HashUtils;
import ru.yandex.market.ydb.integration.YdbTableDescription;
import ru.yandex.market.ydb.integration.query.QCondition;

@ParametersAreNonnullByDefault
@DisplayName("Формирование условий для запроса поиска бизнес-процессов в YDB")
class BusinessProcessStateQConditionFactoryTest extends AbstractContextualYdbTest {

    private static final long ID = 123L;
    private static final long ID_HASH = HashUtils.hashLong(ID);
    private static final Set<Long> IDS = Set.of(123L, 345L, 678L);
    private static final Set<Long> IDS_HASH = IDS.stream().map(HashUtils::hashLong).collect(Collectors.toSet());

    private static final long SEQUENCE_ID = 111L;
    private static final long SEQUENCE_ID_HASH = HashUtils.hashLong(111L);

    private static final long PARENT_ID = 222L;
    private static final long PARENT_ID_HASH = HashUtils.hashLong(PARENT_ID);

    private static final Set<QueueType> QUEUE_TYPES = Set.of(
        QueueType.GET_ACCEPTANCE_CERTIFICATE,
        QueueType.PROCESS_CHECKPOINTS_FROM_TRACKER
    );
    private static final Set<String> QUEUE_TYPES_STRING = QUEUE_TYPES.stream()
        .map(QueueType::name)
        .collect(Collectors.toSet());

    private static final Set<BusinessProcessStatus> STATUSES = Set.of(
        BusinessProcessStatus.ASYNC_REQUEST_SENT,
        BusinessProcessStatus.ENQUEUED
    );
    private static final Set<String> STATUSES_STRING = STATUSES.stream()
        .map(BusinessProcessStatus::name)
        .collect(Collectors.toSet());

    private static final List<EntityId> ENTITY_IDS = List.of(
        EntityId.of(EntityType.ORDER, 1L),
        EntityId.of(EntityType.PARTNER, 333L)
    );
    private static final Set<Long> ENTITY_IDS_IDS = ENTITY_IDS.stream()
        .map(EntityId::getEntityId)
        .collect(Collectors.toSet());
    private static final Set<Long> ENTITY_IDS_IDS_HASH = ENTITY_IDS.stream()
        .map(EntityId::getEntityId)
        .map(HashUtils::hashLong)
        .collect(Collectors.toSet());
    private static final Set<String> ENTITY_TYPES = ENTITY_IDS.stream()
        .map(EntityId::getEntityType)
        .map(EntityType::name)
        .collect(Collectors.toSet());

    private static final Instant CREATED_FROM = Instant.parse("2022-05-17T01:02:03Z");
    private static final Instant CREATED_TO = Instant.parse("2022-05-18T01:02:03Z");
    private static final Instant UPDATED_FROM = Instant.parse("2022-05-17T03:02:03Z");
    private static final Instant UPDATED_TO = Instant.parse("2022-05-17T23:55:53Z");

    @Autowired
    private BusinessProcessStateQConditionFactory qConditionFactory;
    @Autowired
    private QConditionEqualityService qConditionEqualityService;
    @Autowired
    protected BusinessProcessStateTableDescription businessProcessStateTable;
    @Autowired
    protected BusinessProcessStateEntityIdTableDescription businessProcessStateEntityIdTable;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(businessProcessStateTable, businessProcessStateEntityIdTable);
    }

    @Test
    @DisplayName("Получение бизнес-процесса по идентификатору")
    void conditionForId() {
        QCondition expected = businessProcessStateTable.getIdHash().eq(ID_HASH)
            .and(businessProcessStateTable.getId().eq(ID));

        softly.assertThat(qConditionEqualityService.equalConditions(expected, qConditionFactory.getConditionForId(ID)))
            .isTrue();
    }

    @Test
    @DisplayName("Получение бизнес-процессов по списку идентификаторов")
    void conditionForIds() {
        QCondition expected = businessProcessStateTable.getIdHash().in(IDS_HASH)
            .and(businessProcessStateTable.getId().in(IDS));

        softly.assertThat(qConditionEqualityService.equalConditions(
                expected,
                qConditionFactory.getConditionForIds(IDS)
            )
        )
            .isTrue();
    }

    @Test
    @DisplayName("Получение бизнес-процессов по идентификатору родительского процесса")
    void conditionForParentId() {
        QCondition expected = businessProcessStateTable.getParentIdHash().eq(PARENT_ID_HASH)
            .and(businessProcessStateTable.getParentId().eq(PARENT_ID));

        softly.assertThat(qConditionEqualityService.equalConditions(
                expected,
                qConditionFactory.getConditionForParentId(PARENT_ID)
            )
        )
            .isTrue();
    }

    @Test
    @DisplayName("Получение бизнес-процессов по sequenceId пейлоада")
    void conditionForSequenceId() {
        QCondition expected = businessProcessStateTable.getSequenceIdHash().eq(SEQUENCE_ID_HASH)
            .and(businessProcessStateTable.getSequenceId().eq(SEQUENCE_ID));

        softly.assertThat(qConditionEqualityService.equalConditions(
                expected,
                qConditionFactory.getConditionForSequenceId(SEQUENCE_ID)
            )
        )
            .isTrue();
    }

    @Test
    @DisplayName("Получение бизнес-процессов по статусам")
    void conditionForStatuses() {
        QCondition expected = businessProcessStateTable.getStatus().in(STATUSES_STRING);

        softly.assertThat(qConditionEqualityService.equalConditions(
                expected,
                qConditionFactory.getConditionForStatuses(STATUSES)
            )
        )
            .isTrue();
    }

    @Test
    @DisplayName("Получение бизнес-процессов по типам процессов")
    void conditionForQueueTypes() {
        QCondition expected = businessProcessStateTable.getQueueType().in(QUEUE_TYPES_STRING);

        softly.assertThat(qConditionEqualityService.equalConditions(
                expected,
                qConditionFactory.getConditionForQueueTypes(QUEUE_TYPES)
            )
        )
            .isTrue();
    }

    @Test
    @DisplayName("Получение бизнес-процессов по идентификаторам сущностей")
    void conditionForEntityIds() {
        QCondition expected = businessProcessStateEntityIdTable.getEntityIdHash().in(ENTITY_IDS_IDS_HASH)
            .and(businessProcessStateEntityIdTable.getEntityId().in(ENTITY_IDS_IDS))
            .and(businessProcessStateEntityIdTable.getEntityType().in(ENTITY_TYPES));

        softly.assertThat(qConditionEqualityService.equalConditions(
                expected,
                qConditionFactory.getConditionForEntityIds(ENTITY_IDS)
            )
        )
            .isTrue();
    }

    @Test
    @DisplayName("Условия без указанного условия")
    void conditionsExceptCondition() {
        QCondition exceptCondition = qConditionFactory.getConditionForParentId(PARENT_ID);
        BusinessProcessStateFilter filterOnlyByParentId = BusinessProcessStateFilter.builder()
            .parentId(PARENT_ID)
            .build();

        softly.assertThat(qConditionFactory.getConditionsFromFilterExceptCondition(
                filterOnlyByParentId,
                exceptCondition
            ))
            .isEmpty();
    }

    @Test
    @DisplayName("С полностью заполненным фильтром")
    void withFilledFilter() {
        QCondition expected = businessProcessStateTable.getIdHash().in(IDS_HASH)
            .and(businessProcessStateTable.getId().in(IDS))
            .and(businessProcessStateTable.getSequenceIdHash().eq(SEQUENCE_ID_HASH))
            .and(businessProcessStateTable.getSequenceId().eq(SEQUENCE_ID))
            .and(businessProcessStateTable.getParentIdHash().eq(PARENT_ID_HASH))
            .and(businessProcessStateTable.getParentId().eq(PARENT_ID))
            .and(businessProcessStateTable.getQueueType().in(QUEUE_TYPES_STRING))
            .and(businessProcessStateTable.getStatus().in(STATUSES_STRING))
            .and(businessProcessStateEntityIdTable.getEntityIdHash().in(ENTITY_IDS_IDS_HASH))
            .and(businessProcessStateEntityIdTable.getEntityId().in(ENTITY_IDS_IDS))
            .and(businessProcessStateEntityIdTable.getEntityType().in(ENTITY_TYPES))
            .and(businessProcessStateTable.getCreated().greaterOrEq(CREATED_FROM))
            .and(businessProcessStateTable.getCreated().lessOrEq(CREATED_TO))
            .and(businessProcessStateTable.getUpdated().greaterOrEq(UPDATED_FROM))
            .and(businessProcessStateTable.getUpdated().lessOrEq(UPDATED_TO));

        softly.assertThat(qConditionEqualityService.equalConditions(
                expected,
                qConditionFactory.getConditionsFromFilter(filledFilter())
            ))
            .isTrue();
    }

    @Nonnull
    private BusinessProcessStateFilter filledFilter() {
        return BusinessProcessStateFilter.builder()
            .ids(IDS)
            .parentId(PARENT_ID)
            .statuses(STATUSES)
            .sequenceId(SEQUENCE_ID)
            .queueTypes(QUEUE_TYPES)
            .entityIdsIntersection(ENTITY_IDS)
            .createdFrom(CREATED_FROM)
            .createdTo(CREATED_TO)
            .updatedFrom(UPDATED_FROM)
            .updatedTo(UPDATED_TO)
            .comment("unused for ydb comment ;(")
            .build();
    }
}
