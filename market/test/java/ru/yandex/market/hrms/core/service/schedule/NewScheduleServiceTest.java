package ru.yandex.market.hrms.core.service.schedule;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.domain.new_schedule.NewScheduleService;
import ru.yandex.market.hrms.core.domain.new_schedule.repo.NewScheduleEntity;

@RequiredArgsConstructor
public class NewScheduleServiceTest extends AbstractNewScheduleTest {

    @Autowired
    NewScheduleService newScheduleService;

    @Test
    @DbUnitDataSet(before = "NewScheduleServiceTest.UpdateNewMonth.before.csv",
            after = "NewScheduleServiceTest.UpdateNewMonth.after.csv")
    public void shouldUpdateScheduleAndAddNewScheduleDays() {
        Set<NewScheduleEntity> received = mapFromFile("schedule_337_4.json");
        newScheduleService.processSchedules(received, YearMonth.of(2021, 11));
    }

    @Test
    @DbUnitDataSet(before = "NewScheduleServiceTest.DisableSyncUntil.before.csv",
            after = "NewScheduleServiceTest.DisableSyncUntil.before.csv")
    public void shouldNotChangeScheduleBeforeDisableSyncUntilDate() {
        mockClock(LocalDate.of(2021, 10, 1));
        Set<NewScheduleEntity> received = mapFromFile("schedule_8648_4.json");
        newScheduleService.processSchedules(received, YearMonth.of(2021, 11));
    }

    @Test
    @DbUnitDataSet(before = "NewScheduleServiceTest.DisableSyncUntil.before.csv",
            after = "NewScheduleServiceTest.DisableSyncUntil.before.csv")
    public void shouldNotChangeScheduleAtDisableSyncUntilDate() {
        mockClock(LocalDate.of(2021, 11, 1));
        Set<NewScheduleEntity> received = mapFromFile("schedule_8648_4.json");
        newScheduleService.processSchedules(received, YearMonth.of(2021, 11));
    }

    @Test
    @DbUnitDataSet(before = "NewScheduleServiceTest.DisableSyncUntil.before.csv",
            after = "NewScheduleServiceTest.DisableSyncUntil.after.csv")
    public void shouldChangeScheduleAfterDisableSyncUntilDate() {
        mockClock(LocalDate.of(2021, 11, 2));
        Set<NewScheduleEntity> received = mapFromFile("schedule_8648_4.json");
        newScheduleService.processSchedules(received, YearMonth.of(2021, 11));
    }

    @Test
    @DbUnitDataSet(before = "NewScheduleServiceTest.RemoveDisappearedSchedule.before.csv",
            after = "NewScheduleServiceTest.RemoveDisappearedSchedule.after.csv")
    public void shouldRemoveSchedule() {
        mockClock(LocalDate.of(2021, 11, 1));
        Set<NewScheduleEntity> received = mapFromFile("schedule_empty.json");
        newScheduleService.processSchedules(received, YearMonth.of(2021, 11));
    }

    @Test
    @DbUnitDataSet(before = "NewScheduleServiceTest.RestoreRemovedSchedule.before.csv",
            after = "NewScheduleServiceTest.RestoreRemovedSchedule.after.csv")
    public void shouldRestoreRemovedSchedule() {
        mockClock(LocalDate.of(2021, 11, 1));
        Set<NewScheduleEntity> received = mapFromFile("schedule_337_4.json");
        newScheduleService.processSchedules(received, YearMonth.of(2021, 11));
    }

}
