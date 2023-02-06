package ru.yandex.market.logistics.management.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;

class ScheduleRepositoryTest extends AbstractContextualAspectValidationTest {

    @Autowired
    ScheduleRepository scheduleRepository;

    @Test
    @DatabaseSetup("/data/repository/schedule/before/clean_unused_schedules.xml")
    @ExpectedDatabase(
        value = "/data/repository/schedule/after/clean_unused_schedules.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cleanUnusedSchedules_shouldDeleteAllUnused() {
        scheduleRepository.cleanUnusedSchedules(1L, Long.MAX_VALUE);
    }

    @Test
    @DatabaseSetup("/data/repository/schedule/before/clean_unused_schedules.xml")
    @ExpectedDatabase(
        value = "/data/repository/schedule/after/clean_unused_schedules_1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cleanUnusedSchedules_shouldDeleteFirstUnused() {
        scheduleRepository.cleanUnusedSchedules(1L, 1L);
    }

    @Test
    @DatabaseSetup("/data/repository/schedule/before/clean_unused_schedules.xml")
    @ExpectedDatabase(
        value = "/data/repository/schedule/after/clean_unused_schedules_3.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cleanUnusedSchedules_shouldDeleteThreeUnused() {
        scheduleRepository.cleanUnusedSchedules(1L, 1001L);
    }
}
