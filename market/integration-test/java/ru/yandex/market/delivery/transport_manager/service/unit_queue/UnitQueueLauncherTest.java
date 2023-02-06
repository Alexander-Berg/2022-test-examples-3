package ru.yandex.market.delivery.transport_manager.service.unit_queue;

import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.domain.entity.unit_queue.UnitQueue;
import ru.yandex.market.delivery.transport_manager.domain.entity.unit_queue.UnitQueueStatus;
import ru.yandex.market.delivery.transport_manager.queue.task.tpl.unit.UnitQueueProcessingProducer;
import ru.yandex.market.delivery.transport_manager.repository.mappers.UnitQueueMapper;
import ru.yandex.market.delivery.transport_manager.service.PropertyService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class UnitQueueLauncherTest extends AbstractContextualTest {
    @Autowired
    private UnitQueueMapper unitQueueMapper;

    @Autowired
    private PropertyService<TmPropertyKey> propertyService;

    private final UnitQueueProcessingProducer producer = Mockito.mock(UnitQueueProcessingProducer.class);

    private UnitQueueLauncher unitQueueLauncher;

    @BeforeEach
    void setUp() {
        Mockito
            .when(propertyService.getBoolean(TmPropertyKey.ENABLE_UNIT_QUEUE_LAUNCHING))
            .thenReturn(true);

        unitQueueLauncher = new UnitQueueLauncher(
            unitQueueMapper,
            producer,
            clock,
            propertyService
        );
    }

    @Test
    @DatabaseSetup("/service/unit_queue/unit_queue_records_in_different_statuses.xml")
    void launch() {
        unitQueueLauncher.launch();
        verify(producer, times(5)).produce(any(UnitQueue.class));

        List<UnitQueue> queuedRecords = unitQueueMapper.find(List.of(UnitQueueStatus.QUEUED), 10);

        softly.assertThat(queuedRecords.size()).isEqualTo(7);
        softly.assertThat(queuedRecords.stream().map(UnitQueue::getUnitId).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("AAA1", "AAA2", "AAA3", "AAA4", "AAA5", "AAA6", "AAA7");
    }
}
