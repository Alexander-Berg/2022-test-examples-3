package ru.yandex.direct.core.entity.aggregatedstatuses;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesResyncQueueRepository;
import ru.yandex.direct.core.aggregatedstatuses.service.AggregatedStatusesResyncQueueService;
import ru.yandex.direct.core.entity.aggregatedstatus.model.AggregatedEntityIdWithType;
import ru.yandex.direct.core.entity.aggregatedstatus.model.AggregatedStatusQueueEntityStatus;
import ru.yandex.direct.core.entity.aggregatedstatus.model.AggregatedStatusQueueEntityType;
import ru.yandex.direct.core.entity.aggregatedstatus.model.AggregatedStatusResyncQueueEntity;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;

@CoreTest
@RunWith(SpringRunner.class)
public class AggregatedStatusesResyncQueueServiceTest {
    @Autowired
    public AggregatedStatusesResyncQueueService aggregatedStatusesResyncQueueService;

    @Autowired
    public AggregatedStatusesResyncQueueRepository aggregatedStatusesResyncQueueRepository;

    private int shard;
    private long entityId;
    private long idInQueue;
    private List<AggregatedStatusResyncQueueEntity> aggregatedEntityIdWithTypes;

    @Before
    public void before() {
        shard = 1;
        entityId = RandomNumberUtils.nextPositiveLong();
        idInQueue = RandomNumberUtils.nextPositiveLong();
        aggregatedEntityIdWithTypes = List.of(new AggregatedStatusResyncQueueEntity()
                .withId(idInQueue)
                .withEntityId(entityId)
                .withStatus(AggregatedStatusQueueEntityStatus.WAITING)
                .withType(AggregatedStatusQueueEntityType.CAMPAIGN));
        aggregatedStatusesResyncQueueRepository
                .addAggregatedStatusResyncQueueEntities(shard, aggregatedEntityIdWithTypes);
    }

    @Test
    public void addEntitiesToAggregatedStatusesResyncQueue() {
        aggregatedStatusesResyncQueueService.addEntitiesToAggregatedStatusesResyncQueue(shard,
                List.of(new AggregatedEntityIdWithType()
                        .withEntityId(RandomNumberUtils.nextPositiveLong())
                        .withType(AggregatedStatusQueueEntityType.CAMPAIGN)));

        List<AggregatedStatusResyncQueueEntity> aggregatedStatusResyncQueueEntities =
                aggregatedStatusesResyncQueueRepository.getAggregatedStatusResyncQueueEntities(shard, 5000);

        AggregatedStatusResyncQueueEntity entity = aggregatedStatusResyncQueueEntities.stream()
                .filter(queueEntity -> queueEntity.getEntityId().equals(entityId))
                .findAny().orElse(null);
        assertThat(entity).isNotNull();
        assertThat(entity.getStatus()).isEqualTo(AggregatedStatusQueueEntityStatus.WAITING);
    }

    @Test
    public void getResyncQueueEntitiesAndMarkAsProcessing() {
        aggregatedStatusesResyncQueueService.getResyncQueueEntitiesAndMarkAsProcessing(shard);
        List<AggregatedStatusResyncQueueEntity> aggregatedStatusResyncQueueEntities =
                aggregatedStatusesResyncQueueRepository.getAggregatedStatusResyncQueueEntities(shard, 5000);
        AggregatedStatusResyncQueueEntity entity = aggregatedStatusResyncQueueEntities.stream()
                .filter(queueEntity -> queueEntity.getEntityId().equals(entityId))
                .findAny().orElse(null);
        assertThat(entity).isNotNull();
        assertThat(entity.getStatus()).isEqualTo(AggregatedStatusQueueEntityStatus.PROCESSING);
    }

    @Test
    public void deleteProcessedAggregatedStatusResyncQueueEntities() {
        long secondIdInQueue = RandomNumberUtils.nextPositiveLong();
        List<AggregatedStatusResyncQueueEntity> secondAggregatedEntityIdWithTypes =
                List.of(new AggregatedStatusResyncQueueEntity()
                        .withId(secondIdInQueue)
                        .withEntityId(RandomNumberUtils.nextPositiveLong())
                        .withStatus(AggregatedStatusQueueEntityStatus.PROCESSING)
                        .withType(AggregatedStatusQueueEntityType.CAMPAIGN));

        aggregatedStatusesResyncQueueRepository
                .addAggregatedStatusResyncQueueEntities(shard, secondAggregatedEntityIdWithTypes);

        aggregatedStatusesResyncQueueService.deleteProcessedAggregatedStatusResyncQueueEntities(shard,
                List.of(idInQueue, secondIdInQueue));
        Set<Long> entityIds = Set.of(this.entityId, secondIdInQueue);

        List<AggregatedStatusResyncQueueEntity> actualAggregatedStatusResyncQueueEntities =
                filterList(aggregatedStatusesResyncQueueRepository.getAggregatedStatusResyncQueueEntities(shard, 5000),
                        x -> entityIds.contains(x.getEntityId()));
        assertThat(actualAggregatedStatusResyncQueueEntities).isEqualTo(aggregatedEntityIdWithTypes);
    }

}
