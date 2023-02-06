package ru.yandex.market.vendors.analytics.core.service.ga;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import one.util.streamex.IntStreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.FunctionalTest;
import ru.yandex.market.vendors.analytics.core.jpa.entity.ga.GaLoadTask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author antipov93.
 */
class GaLoadTaskServiceTest extends FunctionalTest {

    @Autowired
    private GaLoadTaskService gaLoadTaskService;

    @Test
    @DisplayName("Нет активных заявок с ga")
    void tasksToProcessNoApplications() {
        List<GaLoadTask> actual = gaLoadTaskService.tasksToProcess();
        assertTrue(actual.isEmpty());
    }

    @Test
    @DisplayName("Первая загрузка для счётчика")
    @DbUnitDataSet(before = "GaLoadTaskServiceTest.tasksToProcessFirstRun.before.csv")
    void tasksToProcessFirstRun() {
        Set<GaLoadTask> actual = new HashSet<>(gaLoadTaskService.tasksToProcess());

        long userId = 10;
        long viewId = 1001;
        var now = LocalDate.now();
        Set<GaLoadTask> expected = IntStreamEx.range(0, 5)
                .map(diff -> diff + 1)
                .mapToObj(now::minusDays)
                .map(date -> new GaLoadTask(userId, viewId, date))
                .toSet();

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("")
    @DbUnitDataSet(before = "GaLoadTaskServiceTest.tasksToProcessRegularRun.before.csv")
    void tasksToProcessAllFinished() {
        var actual = gaLoadTaskService.tasksToProcess();
        assertTrue(actual.isEmpty());
    }
}