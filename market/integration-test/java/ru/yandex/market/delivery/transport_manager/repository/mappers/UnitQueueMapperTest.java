package ru.yandex.market.delivery.transport_manager.repository.mappers;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.unit_queue.UnitQueue;
import ru.yandex.market.delivery.transport_manager.domain.entity.unit_queue.UnitQueueStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.unit_queue.UnitQueueType;

class UnitQueueMapperTest extends AbstractContextualTest {
    @Autowired
    private UnitQueueMapper unitQueueMapper;

    @Test
    void insertAndFind() {
        unitQueueMapper.insert(List.of(
            unitQueue("123", UnitQueueType.BAG, 1L, 2L, UnitQueueStatus.NEW),
            unitQueue("1234", UnitQueueType.BAG, 3L, 4L, UnitQueueStatus.PROCESSED),
            unitQueue("1235", UnitQueueType.BAG, 3L, 4L, UnitQueueStatus.QUEUED)
        ));

        unitQueueMapper.insert(List.of(
            unitQueue("1234", UnitQueueType.BAG, 3L, 4L, UnitQueueStatus.NEW),
            unitQueue("666", UnitQueueType.BAG, 3L, 4L, UnitQueueStatus.QUEUED)
        ));

        List<UnitQueue> unitQueues = unitQueueMapper.find(List.of(UnitQueueStatus.NEW, UnitQueueStatus.PROCESSED), 10);

        softly.assertThat(unitQueues).containsExactlyInAnyOrder(
            unitQueue("123", UnitQueueType.BAG, 1L, 2L, UnitQueueStatus.NEW).setId(1L),
            unitQueue("1234", UnitQueueType.BAG, 3L, 4L, UnitQueueStatus.PROCESSED).setId(2L)
        );

        List<UnitQueue> queued = unitQueueMapper.find(List.of(UnitQueueStatus.QUEUED), 10);
        softly.assertThat(queued.size()).isEqualTo(2);
    }

    @Test
    void testInsertAndFindOne() {
        unitQueueMapper.insert(List.of(
            unitQueue("123", UnitQueueType.BAG, 1L, 2L, UnitQueueStatus.NEW)
        ));

        UnitQueue unitQueue = unitQueueMapper.findById(1L);
        softly.assertThat(unitQueue).isEqualTo(
            unitQueue("123", UnitQueueType.BAG, 1L, 2L, UnitQueueStatus.NEW).setId(1L)
        );
    }

    @Test
    void setStatus() {
        unitQueueMapper.insert(List.of(
            unitQueue("123", UnitQueueType.BAG, 1L, 2L, UnitQueueStatus.NEW),
            unitQueue("124", UnitQueueType.BAG, 3L, 4L, UnitQueueStatus.NEW),
            unitQueue("1234", UnitQueueType.BAG, 5L, 6L, UnitQueueStatus.PROCESSED)
        ));

        unitQueueMapper.setStatus(
            UnitQueueStatus.ERROR,
            List.of(1L, 2L),
            LocalDateTime.now(clock)
        );

        List<UnitQueue> unitQueues = unitQueueMapper.find(List.of(UnitQueueStatus.ERROR), 2);

        softly.assertThat(unitQueues).containsExactlyInAnyOrder(
            unitQueue("123", UnitQueueType.BAG, 1L, 2L, UnitQueueStatus.ERROR).setId(1L),
            unitQueue("124", UnitQueueType.BAG, 3L, 4L, UnitQueueStatus.ERROR).setId(2L)
        );
    }

    private static UnitQueue unitQueue(
        String unitId,
        UnitQueueType unitType,
        Long pointFrom,
        Long pointTo,
        UnitQueueStatus status
    ) {
        return new UnitQueue()
            .setUnitId(unitId)
            .setUnitType(unitType)
            .setPointFromId(pointFrom)
            .setPointToId(pointTo)
            .setStatus(status);
    }
}
