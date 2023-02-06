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
import ru.yandex.market.logistics.cs.domain.jdbc.RecalculationChangedData;
import ru.yandex.market.logistics.cs.domain.jdbc.RecalculationData;

import static ru.yandex.market.logistics.cs.domain.enumeration.DayOffType.UNSET;
import static ru.yandex.market.logistics.cs.domain.enumeration.UnitType.ITEM;
import static ru.yandex.market.logistics.cs.domain.enumeration.UnitType.ORDER;

/**
 * 20 service
 * now: +1 -1 (order u item)
 * now - 10d: +1 -2
 * now + 60d: +1
 * <p>
 * 30 service
 * now: +2 -1 (order u item)
 * now - 10: +1 -1
 * <p>
 * В тестах с удалением и добавлением
 * для обоих сервисов скипается по одному + в now+60d из-за версии 6.
 * это имитация случая, когда сразу после транзакции с мержем деревьев прилетает новый счетчик.
 */
@DisplayName("Сервис по пересчету капасити у счетчиков")
@DatabaseSetup("/repository/counting/before/base_recounting.xml")
class CapacityValueCounterRecountingTest extends AbstractRecountingTest {

    @Autowired
    private CapacityValueCounterService service;

    @Test
    @DisplayName("Пересчет после добавления связки")
    @DatabaseSetup(
        value = "/repository/counting/before/without_value_counters.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/repository/counting/after/after_adding.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void recountAfterAddition() {
        List<RecalculationChangedData> expected = List.of(
            changedData(21L, CAPACITY_ID2, 101, 1, now(), ORDER, 1000L, UNSET),
            changedData(22L, CAPACITY_ID2, 99, -1, startDate(), ORDER, 1000L, UNSET),
            changedData(23L, CAPACITY_ID2, 101, 1, finishDate(), ORDER, 1000L, UNSET),
            changedData(25L, CAPACITY_ID2, 100, 0, now(), ITEM, 1000L, UNSET),
            changedData(1L, CAPACITY_ID1, 0, 0, startDate(), ITEM, -1L, UNSET),
            changedData(2L, CAPACITY_ID2, 0, 0, startDate(), ITEM, 1000L, UNSET),
            changedData(3L, CAPACITY_ID1, -1, -1, startDate(), ORDER, -1L, UNSET),
            changedData(4L, CAPACITY_ID1, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(5L, CAPACITY_ID1, 1, 1, now(), ORDER, -1L, UNSET),
            changedData(6L, CAPACITY_ID1, 0, 0, now().plusDays(1), ITEM, -1L, UNSET),
            changedData(7L, CAPACITY_ID2, 0, 0, now().plusDays(1), ITEM, 1000L, UNSET),
            changedData(8L, CAPACITY_ID1, 1, 1, now().plusDays(1), ORDER, -1L, UNSET),
            changedData(9L, CAPACITY_ID2, 1, 1, now().plusDays(1), ORDER, 1000L, UNSET),
            changedData(10L, CAPACITY_ID1, 0, 0, finishDate(), ITEM, -1L, UNSET),
            changedData(11L, CAPACITY_ID2, 0, 0, finishDate(), ITEM, 1000L, UNSET),
            changedData(12L, CAPACITY_ID1, 1, 1, finishDate(), ORDER, -1L, UNSET)
        );
        RecalculationData recalculationData = service.recalculateForAdding(
            List.of(
                versionedMapping(1L, SERVICE_ID20, CAPACITY_ID2, 6L),
                versionedMapping(2L, SERVICE_ID30, CAPACITY_ID2, 6L)
            ),
            false
        );

        softly.assertThat(recalculationData.getExceptions()).isEmpty();
        softly.assertThat(recalculationData.getCapacityValueCounterChangedData()).hasSameElementsAs(expected);
    }

    @Test
    @DisplayName("Пересчет после удаления связки")
    @ExpectedDatabase(
        value = "/repository/counting/after/after_deleting.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void recountAfterDeletion() {
        List<RecalculationChangedData> expected = List.of(
            changedData(31L, CAPACITY_ID3, 99, -1, now(), ORDER, 1000L, UNSET),
            changedData(32L, CAPACITY_ID3, 100, 0, startDate(), ORDER, 1000L, UNSET),
            changedData(35L, CAPACITY_ID3, 100, 0, now(), ITEM, 1000L, UNSET)
        );
        RecalculationData recalculationData = service.recalculateForRemoving(List.of(
            versionedMapping(2L, SERVICE_ID30, CAPACITY_ID3, 6L)
        ), false);

        softly.assertThat(recalculationData.getExceptions()).isEmpty();
        softly.assertThat(recalculationData.getCapacityValueCounterChangedData()).hasSameElementsAs(expected);
    }

    @Test
    @DisplayName("Пересчет после обновления связки")
    @ExpectedDatabase(
        value = "/repository/counting/after/after_updating.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void recountAfterUpdate() {
        List<RecalculationChangedData> expected = List.of(
            // Удаленные
            changedData(31L, CAPACITY_ID3, 100, 0, now(), ORDER, 1000L, UNSET),
            changedData(32L, CAPACITY_ID3, 101, 1, startDate(), ORDER, 1000L, UNSET),
            changedData(33L, CAPACITY_ID3, 99, -1, finishDate(), ORDER, 1000L, UNSET),
            // добавленные
            changedData(21L, CAPACITY_ID2, 100, 0, now(), ORDER, 1000L, UNSET),
            changedData(22L, CAPACITY_ID2, 99, -1, startDate(), ORDER, 1000L, UNSET),
            changedData(23L, CAPACITY_ID2, 101, 1, finishDate(), ORDER, 1000L, UNSET),
            changedData(5L, CAPACITY_ID1, 0, 0, now(), ORDER, -1L, UNSET),
            changedData(3L, CAPACITY_ID1, -1, -1, startDate(), ORDER, -1L, UNSET),
            changedData(8L, CAPACITY_ID1, 1, 1, finishDate(), ORDER, -1L, UNSET),

            changedData(1L, CAPACITY_ID1, 0, 0, startDate(), ITEM, -1L, UNSET),
            changedData(4L, CAPACITY_ID1, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(6L, CAPACITY_ID1, 0, 0, finishDate(), ITEM, -1L, UNSET),
            changedData(2L, CAPACITY_ID2, 0, 0, startDate(), ITEM, 1000L, UNSET),
            changedData(7L, CAPACITY_ID2, 0, 0, finishDate(), ITEM, 1000L, UNSET),
            changedData(25L, CAPACITY_ID2, 100, 0, now(), ITEM, 1000L, UNSET),
            changedData(35L, CAPACITY_ID3, 100, 0, now(), ITEM, 1000L, UNSET)
        );
        RecalculationData recalculationData = service.recalculateForUpdating(
            List.of(versionedMapping(1L, SERVICE_ID20, CAPACITY_ID3, 6L)),
            List.of(versionedMapping(1L, SERVICE_ID20, CAPACITY_ID2, 6L))
        );

        softly.assertThat(recalculationData.getExceptions()).isEmpty();
        softly.assertThat(recalculationData.getCapacityValueCounterChangedData()).hasSameElementsAs(expected);
    }

    /**
     * 1 service_counter with id=36
     * 2 capacity_value_counter with different unit_type.
     */
    @Test
    @DisplayName("Учитываются только сервисные счетчики с версией меньше текущей")
    @ExpectedDatabase(
        value = "/repository/counting/after/version_below_2.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void usesServiceCountersWithVersionBelowCurrent() {
        List<RecalculationChangedData> expected = List.of(
            changedData(31L, CAPACITY_ID3, 99, -1, now(), ORDER, 1000L, UNSET),
            changedData(35L, CAPACITY_ID3, 100, 0, now(), ITEM, 1000L, UNSET),
            changedData(1L, CAPACITY_ID1, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(2L, CAPACITY_ID1, -1, -1, now(), ORDER, -1L, UNSET)
        );

        RecalculationData recalculationData = service.recalculateForAdding(List.of(
            versionedMapping(1L, SERVICE_ID20, CAPACITY_ID3, 2L)
        ), false);
        softly.assertThat(recalculationData.getExceptions()).isEmpty();
        softly.assertThat(recalculationData.getCapacityValueCounterChangedData())
            .hasSameElementsAs(expected);
    }

    @Test
    @DisplayName("При добавлении маппинга, добавляется только уникальный счетчик (есть еще сервис на этот капасити)")
    @DatabaseSetup(
        value = "/repository/counting/before/with_unique_counters.xml",
        type = DatabaseOperation.INSERT
    )
    void addUnique() {
        List<RecalculationChangedData> expected = List.of(
            changedData(31L, CAPACITY_ID3, 102, 2, now(), ORDER, 1000L, UNSET),
            changedData(32L, CAPACITY_ID3, 101, 1, startDate(), ORDER, 1000L, UNSET),
            changedData(33L, CAPACITY_ID3, 101, 1, finishDate(), ORDER, 1000L, UNSET),
            changedData(35L, CAPACITY_ID3, 100, 0, now(), ITEM, 1000L, UNSET),
            changedData(1L, CAPACITY_ID1, 0, 0, startDate(), ITEM, -1L, UNSET),
            changedData(2L, CAPACITY_ID3, 0, 0, startDate(), ITEM, 1000L, UNSET),
            changedData(3L, CAPACITY_ID1, 1, 1, startDate(), ORDER, -1L, UNSET),
            changedData(4L, CAPACITY_ID1, 0, 0, now(), ITEM, -1L, UNSET),
            changedData(5L, CAPACITY_ID1, 2, 2, now(), ORDER, -1L, UNSET),
            changedData(6L, CAPACITY_ID1, 0, 0, finishDate(), ITEM, -1L, UNSET),
            changedData(7L, CAPACITY_ID3, 0, 0, finishDate(), ITEM, 1000L, UNSET),
            changedData(8L, CAPACITY_ID1, 1, 1, finishDate(), ORDER, -1L, UNSET)
        );

        RecalculationData recalculationData = service.recalculateForAdding(
            List.of(versionedMapping(1L, SERVICE_ID50, CAPACITY_ID3, 2L)),
            true
        );
        softly.assertThat(recalculationData.getExceptions()).isEmpty();
        softly.assertThat(recalculationData.getCapacityValueCounterChangedData()).hasSameElementsAs(expected);
    }

    @Test
    @DisplayName("При удалении маппинга, удаляется только уникальный счетчик (есть еще сервис на этот капасити)")
    @DatabaseSetup(
        value = "/repository/counting/before/with_unique_counters.xml",
        type = DatabaseOperation.INSERT
    )
    void deleteUnique() {
        List<RecalculationChangedData> expected = List.of(
            changedData(31L, CAPACITY_ID3, 98, -2, now(), ORDER, 1000L, UNSET),
            changedData(32L, CAPACITY_ID3, 99, -1, startDate(), ORDER, 1000L, UNSET),
            changedData(33L, CAPACITY_ID3, 99, -1, finishDate(), ORDER, 1000L, UNSET),
            changedData(35L, CAPACITY_ID3, 100, 0, now(), ITEM, 1000L, UNSET)
        );

        RecalculationData recalculationData = service.recalculateForRemoving(
            List.of(versionedMapping(1L, SERVICE_ID50, CAPACITY_ID3, 2L)),
            true
        );
        softly.assertThat(recalculationData.getExceptions()).isEmpty();
        softly.assertThat(recalculationData.getCapacityValueCounterChangedData()).hasSameElementsAs(expected);
    }

    @Test
    @DisplayName("Пересчет по удаленному капасити приводит к результату с ошибкой")
    //TODO DELIVERY-32021
    void recountForDeletedCapacity() {
        RecalculationData recalculationData = service.recalculateForRemoving(
            List.of(versionedMapping(183833L, 6302694L, 190161L, 2L)),
            false
        );
        softly.assertThat(recalculationData.getExceptions()).hasSize(1);
    }

    @Test
    @DisplayName("Множественные сервисы на 1 капасити с 1 лог. точки, корректно пересчитываются (уникально)")
    @DatabaseSetup(
        value = "/repository/counting/before/add_multiple_services.xml",
        type = DatabaseOperation.INSERT
    )
    void uniqueAddMultipleServices() {
        List<RecalculationChangedData> expected = List.of(
            changedData(21L, CAPACITY_ID2, 150, 50, now(), ORDER, 1000L, UNSET),
            changedData(25L, CAPACITY_ID2, 100, 0, now(), ITEM, 1000L, UNSET),
            changedData(2L, CAPACITY_ID1, 50, 50, now(), ORDER, -1L, UNSET),
            changedData(1L, CAPACITY_ID1, 0, 0, now(), ITEM, -1L, UNSET)
        );

        RecalculationData recalculationData = service.recalculateForAdding(
            List.of(
                versionedMapping(123L, 21L, CAPACITY_ID2, 2L),
                versionedMapping(124L, 22L, CAPACITY_ID2, 3L),
                versionedMapping(125L, 23L, CAPACITY_ID2, 4L)
            ),
            true
        );
        softly.assertThat(recalculationData.getExceptions()).isEmpty();
        softly.assertThat(recalculationData.getCapacityValueCounterChangedData()).hasSameElementsAs(expected);
    }

    @Test
    @DisplayName("Отвязываем сервис 1 от капасити 1 после нескольких изменений роута")
    @DatabaseSetup(
        value = "/repository/counting/before/remove_service_after_several_route_changes.xml"
    )
    public void removeServiceAfterSeveralRouteChanges() {
        RecalculationData recalculationData = service.recalculateForRemoving(
            List.of(
                versionedMapping(123L, 1L, 1L, 2L)
            ),
            true
        );
        List<RecalculationChangedData> expected = List.of(
            changedData(101L, 1L, 0, -5, now(), ORDER, 100L, UNSET),
            changedData(102L, 1L, 0, -50, now(), ITEM, 100L, UNSET)
        );
        softly.assertThat(recalculationData.getExceptions()).isEmpty();
        softly.assertThat(recalculationData.getCapacityValueCounterChangedData()).hasSameElementsAs(expected);
    }


    @Test
    @DisplayName("Привязываем сервис 1 к капасити 2 после того, как отвязали его от капасити 1")
    @DatabaseSetup(
        value = "/repository/counting/before/add_service_after_several_route_changes.xml"
    )
    public void addServiceAfterSeveralRouteChanges() {
        RecalculationData recalculationData = service.recalculateForAdding(
            List.of(
                versionedMapping(123L, 1L, 2L, 2L)
            ),
            true
        );
        List<RecalculationChangedData> expected = List.of(
            changedData(103L, 2L, 5, 5, now(), ORDER, 100L, UNSET),
            changedData(104L, 2L, 50, 50, now(), ITEM, 100L, UNSET)
        );
        softly.assertThat(recalculationData.getExceptions()).isEmpty();
        softly.assertThat(recalculationData.getCapacityValueCounterChangedData()).hasSameElementsAs(expected);
    }
}
