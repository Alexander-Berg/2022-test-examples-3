package ru.yandex.market.hrms.tms.manager.schedule;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.test.configurer.OebsApiConfigurer;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.manager.ScheduleSyncManager;

@DbUnitDataSet(before = "ScheduleSyncManagerTest.before.csv")
public class ScheduleSyncManagerTest extends AbstractTmsTest {
    @Autowired
    private ScheduleSyncManager scheduleSyncManager;
    @Autowired
    private OebsApiConfigurer oebsApiConfigurer;

    @DbUnitDataSet(after = "ScheduleSyncManagerTest.after.csv")
    @Test
    void shouldRunSyncManager() {
        scheduleSyncManager.syncSchedules();
    }

    @DbUnitDataSet(after = "ScheduleSyncManagerTest.after.csv")
    @Test
    void shouldUpdateSchedules() {
        scheduleSyncManager.syncSchedules();
        scheduleSyncManager.syncSchedules();
    }

    @DbUnitDataSet(after = "ScheduleSyncManagerTest.after_delete.csv")
    @Test
    void shouldDeleteSchedules() {
        scheduleSyncManager.syncSchedules();

        oebsApiConfigurer.mockGetDepartmentSchedules("/results/oebs/get_schedules_empty_response.json");
        scheduleSyncManager.syncSchedules();
    }

    @DbUnitDataSet(after = "ScheduleSyncManagerTest.after_24hours.csv")
    @Test
    void shouldUpdateScheduleWithStartTime24HoursPlus() {
        oebsApiConfigurer.mockGetDepartmentSchedules("/results/oebs/get_schedules_24_hours_plus.json");
        scheduleSyncManager.syncSchedules();
    }


    @DbUnitDataSet(
            before = "ScheduleSyncManagerTest.disableSync.before.csv",
            after = "ScheduleSyncManagerTest.disableSync.after.csv")
    @Test
    void disableSync() {
        oebsApiConfigurer.mockGetDepartmentSchedules("/results/oebs/get_schedules_24_hours_plus.json");
        scheduleSyncManager.syncSchedules();
    }


    @DbUnitDataSet(
            after = "ScheduleSyncManagerTest.oebsResponseWithInvalidIntervals.after.csv")
    @Test
    void shouldNotFailWhenOebsResponseHasInvalidIntervals() {
        oebsApiConfigurer.mockGetDepartmentSchedules("/results/oebs/get_schedules_with_invalid_intervals.json");
        scheduleSyncManager.syncSchedules();
    }
}
