package ru.yandex.market.delivery.transport_manager.service.unit_queue;

import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.domain.entity.unit_queue.UnitQueue;
import ru.yandex.market.delivery.transport_manager.domain.entity.unit_queue.UnitQueueStatus;
import ru.yandex.market.delivery.transport_manager.repository.mappers.UnitQueueMapper;
import ru.yandex.market.delivery.transport_manager.service.PropertyService;

class UnitQueueNewBagLookuperTest extends AbstractContextualTest {
    @Autowired
    private UnitQueueNewBagLookuper lookuper;

    @Autowired
    private UnitQueueMapper unitQueueMapper;

    @Autowired
    private PropertyService<TmPropertyKey> propertyService;

    @Test
    @DatabaseSetup({
        "/service/unit_queue/center_state_with_bags.xml",
        "/service/unit_queue/unit_queue_records_in_different_statuses.xml"
    })
    void lookupBagsForSc() {
        Mockito
            .when(propertyService.getBoolean(TmPropertyKey.ENABLE_UNIT_QUEUE_TABLE_FILLING))
            .thenReturn(true);

        lookuper.lookup();

        List<UnitQueue> newRecords = unitQueueMapper.find(List.of(UnitQueueStatus.NEW), 20);

        softly.assertThat(newRecords.size()).isEqualTo(7);
        softly.assertThat(newRecords.stream().map(UnitQueue::getUnitId).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("AAA1", "AAA2", "AAA3", "AAA4", "AAA5", "BAG001", "BAG002");
    }


    @Test
    @DatabaseSetup("/service/unit_queue/sortables/setup.xml")
    @ExpectedDatabase(
        value = "/service/unit_queue/sortables/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Вставка sortables из Дропоффов (прямой поток)")
    void lookupFromDropoff() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_UNIT_QUEUE_TABLE_SORTABLES_FILLING))
            .thenReturn(true);

        lookuper.lookup();
    }

    @Test
    @DatabaseSetup({
        "/service/unit_queue/sortables/setup.xml",
        "/service/unit_queue/sortables/expected.xml",
    })
    @ExpectedDatabase(
        value = "/service/unit_queue/sortables/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Игнорирование sortables из Дропоффов (прямой поток)")
    void ignoreExistingUnitsFromDropoff() {
        Mockito.when(propertyService.getBoolean(TmPropertyKey.ENABLE_UNIT_QUEUE_TABLE_SORTABLES_FILLING))
            .thenReturn(true);

        lookuper.lookup();
    }
}
