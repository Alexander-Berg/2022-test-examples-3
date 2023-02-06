package ru.yandex.market.logistics.cs.service;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.cs.dbqueue.counting.AbstractRecountingTest;
import ru.yandex.market.logistics.cs.domain.jdbc.RecalculationData;

import static ru.yandex.market.logistics.cs.domain.enumeration.DayOffType.UNSET;
import static ru.yandex.market.logistics.cs.domain.enumeration.UnitType.ITEM;
import static ru.yandex.market.logistics.cs.domain.enumeration.UnitType.ORDER;

@DatabaseSetup("/repository/counting/before/base_multiservice_mapping_recounting.xml")
class MultiserviceCapacityMappingRecalculationTest extends AbstractRecountingTest {
    @Autowired
    private CapacityValueCounterService service;

    @Test
    @DisplayName("Пересчет при последовательном обновлениии мультисервисных маппингов")
    @DatabaseSetup(
        value = "/repository/counting/before/multiservice_mapping.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/repository/counting/after/after_multiservice_mapping_adding.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void sequentialMultiServiceMappingAddition() {
        RecalculationData data1 = service.recalculateForAdding(List.of(
            versionedMapping(1L, 10L, 40L, 1L)
        ), false);
        RecalculationData data2 = service.recalculateForRemoving(List.of(
            versionedMapping(1L, 10L, 40L, 1L)
        ), false);
        RecalculationData data3 = service.recalculateForAdding(List.of(
            versionedMapping(1L, 10L, 40L, 3L),
            versionedMapping(2L, 20L, 40L, 1L)
        ), false);

        softly.assertThat(data1.getExceptions()).isEmpty();
        softly.assertThat(data1.getCapacityValueCounterChangedData()).hasSameElementsAs(List.of(
            changedData(1L, 10L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(2L, 30L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(3L, 40L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(4L, 10L, 1, 1, now(), ORDER, -1L, UNSET),
            changedData(5L, 30L, 1, 1, now(), ORDER, -1L, UNSET),
            changedData(6L, 40L, 1, 1, now(), ORDER, 4L, UNSET)
        ));

        softly.assertThat(data2.getExceptions()).isEmpty();
        softly.assertThat(data2.getCapacityValueCounterChangedData()).hasSameElementsAs(List.of(
            changedData(1L, 10L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(2L, 30L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(3L, 40L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(4L, 10L, 0, -1, now(), ORDER, -1L, UNSET),
            changedData(5L, 30L, 0, -1, now(), ORDER, -1L, UNSET),
            changedData(6L, 40L, 0, -1, now(), ORDER, 4L, UNSET)
        ));

        softly.assertThat(data3.getExceptions()).isEmpty();
        softly.assertThat(data3.getCapacityValueCounterChangedData()).hasSameElementsAs(List.of(
            changedData(1L, 10L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(2L, 30L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(3L, 40L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(4L, 10L, 1, 1, now(), ORDER, -1L, UNSET),
            changedData(5L, 30L, 1, 1, now(), ORDER, -1L, UNSET),
            changedData(6L, 40L, 1, 1, now(), ORDER, 4L, UNSET),
            changedData(7L, 10L, 0, 0, now().plusDays(1), ITEM, -1L, UNSET),
            changedData(8L, 30L, 0, 0, now().plusDays(1), ITEM, -1L, UNSET),
            changedData(9L, 40L, 0, 0, now().plusDays(1), ITEM, -1L, UNSET),
            changedData(10L, 10L, 1, 1, now().plusDays(1), ORDER, -1L, UNSET),
            changedData(11L, 30L, 1, 1, now().plusDays(1), ORDER, -1L, UNSET),
            changedData(12L, 40L, 1, 1, now().plusDays(1), ORDER, 4L, UNSET),
            changedData(13L, 10L, 0, 0, now().plusDays(2), ITEM, -1L, UNSET),
            changedData(14L, 30L, 0, 0, now().plusDays(2), ITEM, -1L, UNSET),
            changedData(15L, 40L, 0, 0, now().plusDays(2), ITEM, -1L, UNSET),
            changedData(16L, 10L, 1, 1, now().plusDays(2), ORDER, -1L, UNSET),
            changedData(17L, 30L, 1, 1, now().plusDays(2), ORDER, -1L, UNSET),
            changedData(18L, 40L, 1, 1, now().plusDays(2), ORDER, 4L, UNSET)
        ));
    }

    @Test
    @DisplayName("Пересчет при одновременном добавления мультисервисного маппинга")
    @DatabaseSetup(
        value = "/repository/counting/before/multiservice_mapping.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/repository/counting/after/after_multiservice_mapping_adding.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void simultaneousMultiServiceMappingAddition() {
        RecalculationData data = service.recalculateForAdding(List.of(
            versionedMapping(1L, 10L, 40L, 3L),
            versionedMapping(2L, 20L, 40L, 1L)
        ), false);

        softly.assertThat(data.getExceptions()).isEmpty();
        softly.assertThat(data.getCapacityValueCounterChangedData()).hasSameElementsAs(List.of(
            changedData(1L, 10L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(2L, 30L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(3L, 40L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(4L, 10L, 1, 1, now(), ORDER, -1L, UNSET),
            changedData(5L, 30L, 1, 1, now(), ORDER, -1L, UNSET),
            changedData(6L, 40L, 1, 1, now(), ORDER, 4L, UNSET),
            changedData(7L, 10L, 0, 0, now().plusDays(1), ITEM, -1L, UNSET),
            changedData(8L, 30L, 0, 0, now().plusDays(1), ITEM, -1L, UNSET),
            changedData(9L, 40L, 0, 0, now().plusDays(1), ITEM, -1L, UNSET),
            changedData(10L, 10L, 1, 1, now().plusDays(1), ORDER, -1L, UNSET),
            changedData(11L, 30L, 1, 1, now().plusDays(1), ORDER, -1L, UNSET),
            changedData(12L, 40L, 1, 1, now().plusDays(1), ORDER, 4L, UNSET),
            changedData(13L, 10L, 0, 0, now().plusDays(2), ITEM, -1L, UNSET),
            changedData(14L, 30L, 0, 0, now().plusDays(2), ITEM, -1L, UNSET),
            changedData(15L, 40L, 0, 0, now().plusDays(2), ITEM, -1L, UNSET),
            changedData(16L, 10L, 1, 1, now().plusDays(2), ORDER, -1L, UNSET),
            changedData(17L, 30L, 1, 1, now().plusDays(2), ORDER, -1L, UNSET),
            changedData(18L, 40L, 1, 1, now().plusDays(2), ORDER, 4L, UNSET)
        ));
    }

    @Test
    @DisplayName("Некорректный пересчет при последовательном удалении мультисервисного маппинга")
    @DatabaseSetup(
        value = "/repository/counting/before/multiservice_mapping.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/repository/counting/after/after_multiservice_mapping_removal_invalid.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void invalidSequentialMultiServiceMappingRemoval() {
        sequentialMultiServiceMappingAddition();

        RecalculationData data1 = service.recalculateForRemoving(List.of(
            versionedMapping(1L, 10L, 40L, 3L)
        ), false);
        RecalculationData data2 = service.recalculateForRemoving(List.of(
            versionedMapping(2L, 20L, 40L, 1L)
        ), false);

        softly.assertThat(data1.getExceptions()).isEmpty();
        softly.assertThat(data1.getCapacityValueCounterChangedData()).hasSameElementsAs(List.of(
            changedData(1L, 10L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(2L, 30L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(3L, 40L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(4L, 10L, 0, -1, now(), ORDER, -1L, UNSET),
            changedData(5L, 30L, 0, -1, now(), ORDER, -1L, UNSET),
            changedData(6L, 40L, 0, -1, now(), ORDER, 4L, UNSET),
            changedData(7L, 10L, 0, 0, now().plusDays(1), ITEM, -1L, UNSET),
            changedData(8L, 30L, 0, 0, now().plusDays(1), ITEM, -1L, UNSET),
            changedData(9L, 40L, 0, 0, now().plusDays(1), ITEM, -1L, UNSET),
            changedData(10L, 10L, 0, -1, now().plusDays(1), ORDER, -1L, UNSET),
            changedData(11L, 30L, 0, -1, now().plusDays(1), ORDER, -1L, UNSET),
            changedData(12L, 40L, 0, -1, now().plusDays(1), ORDER, 4L, UNSET),
            changedData(13L, 10L, 0, 0, now().plusDays(2), ITEM, -1L, UNSET),
            changedData(14L, 30L, 0, 0, now().plusDays(2), ITEM, -1L, UNSET),
            changedData(15L, 40L, 0, 0, now().plusDays(2), ITEM, -1L, UNSET),
            changedData(16L, 10L, 0, -1, now().plusDays(2), ORDER, -1L, UNSET),
            changedData(17L, 30L, 0, -1, now().plusDays(2), ORDER, -1L, UNSET),
            changedData(18L, 40L, 0, -1, now().plusDays(2), ORDER, 4L, UNSET)
        ));

        // should actually lead to no counters change
        softly.assertThat(data2.getExceptions()).isEmpty();
        softly.assertThat(data2.getCapacityValueCounterChangedData()).hasSameElementsAs(List.of(
            changedData(1L, 10L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(2L, 30L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(3L, 40L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(4L, 10L, -1, -1, now(), ORDER, -1L, UNSET),
            changedData(5L, 30L, -1, -1, now(), ORDER, -1L, UNSET),
            changedData(6L, 40L, -1, -1, now(), ORDER, 4L, UNSET),
            changedData(7L, 10L, 0, 0, now().plusDays(1), ITEM, -1L, UNSET),
            changedData(8L, 30L, 0, 0, now().plusDays(1), ITEM, -1L, UNSET),
            changedData(9L, 40L, 0, 0, now().plusDays(1), ITEM, -1L, UNSET),
            changedData(10L, 10L, -1, -1, now().plusDays(1), ORDER, -1L, UNSET),
            changedData(11L, 30L, -1, -1, now().plusDays(1), ORDER, -1L, UNSET),
            changedData(12L, 40L, -1, -1, now().plusDays(1), ORDER, 4L, UNSET),
            changedData(13L, 10L, 0, 0, now().plusDays(2), ITEM, -1L, UNSET),
            changedData(14L, 30L, 0, 0, now().plusDays(2), ITEM, -1L, UNSET),
            changedData(15L, 40L, 0, 0, now().plusDays(2), ITEM, -1L, UNSET),
            changedData(16L, 10L, -1, -1, now().plusDays(2), ORDER, -1L, UNSET),
            changedData(17L, 30L, -1, -1, now().plusDays(2), ORDER, -1L, UNSET),
            changedData(18L, 40L, -1, -1, now().plusDays(2), ORDER, 4L, UNSET)
        ));
    }

    @Test
    @DisplayName("Пересчет при одновременном удалении мультисервисного маппинга")
    @DatabaseSetup(
        value = "/repository/counting/before/multiservice_mapping.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/repository/counting/after/after_multiservice_mapping_removal.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void simultaneousMultiServiceMappingRemoval() {
        sequentialMultiServiceMappingAddition();
        RecalculationData data = service.recalculateForRemoving(List.of(
            versionedMapping(1L, 10L, 40L, 3L),
            versionedMapping(2L, 20L, 40L, 1L)
        ), false);

        softly.assertThat(data.getExceptions()).isEmpty();
        softly.assertThat(data.getCapacityValueCounterChangedData()).hasSameElementsAs(List.of(
            changedData(1L, 10L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(2L, 30L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(3L, 40L, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(4L, 10L, 0, -1, now(), ORDER, -1L, UNSET),
            changedData(5L, 30L, 0, -1, now(), ORDER, -1L, UNSET),
            changedData(6L, 40L, 0, -1, now(), ORDER, 4L, UNSET),
            changedData(7L, 10L, 0, 0, now().plusDays(1), ITEM, -1L, UNSET),
            changedData(8L, 30L, 0, 0, now().plusDays(1), ITEM, -1L, UNSET),
            changedData(9L, 40L, 0, 0, now().plusDays(1), ITEM, -1L, UNSET),
            changedData(10L, 10L, 0, -1, now().plusDays(1), ORDER, -1L, UNSET),
            changedData(11L, 30L, 0, -1, now().plusDays(1), ORDER, -1L, UNSET),
            changedData(12L, 40L, 0, -1, now().plusDays(1), ORDER, 4L, UNSET),
            changedData(13L, 10L, 0, 0, now().plusDays(2), ITEM, -1L, UNSET),
            changedData(14L, 30L, 0, 0, now().plusDays(2), ITEM, -1L, UNSET),
            changedData(15L, 40L, 0, 0, now().plusDays(2), ITEM, -1L, UNSET),
            changedData(16L, 10L, 0, -1, now().plusDays(2), ORDER, -1L, UNSET),
            changedData(17L, 30L, 0, -1, now().plusDays(2), ORDER, -1L, UNSET),
            changedData(18L, 40L, 0, -1, now().plusDays(2), ORDER, 4L, UNSET)
        ));
    }
}
