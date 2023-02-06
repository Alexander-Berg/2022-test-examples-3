package ru.yandex.market.hrms.tms.manager.analytics;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.manager.CalendarDataSyncManager;

@DbUnitDataSet(before = {"CalendarDataSyncManagerTest.schedule.csv", "CalendarDataSyncManagerTest.before.csv"},
        nonRestartedSequences = {"s_calendar_data"})
public class CalendarDataSyncManagerTest extends AbstractTmsTest {

    @Autowired
    private CalendarDataSyncManager calendarDataSyncManager;

    @Test
    @DisplayName("Стандартная проверка статистики, все данные полученны и корректны")
    @DbUnitDataSet(before = "CalendarDataSyncManagerTest.users_default.csv",
            after = "CalendarDataSyncManagerTest.after.csv")
    public void syncNewCalendarData() {
        mockClock(LocalDateTime.parse("2021-12-27T14:00:00"));
        calendarDataSyncManager.syncCalendarData();
    }

    @Test
    @DisplayName("При нескольких назначениях не основные игнорируются")
    @DbUnitDataSet(before = {"CalendarDataSyncManagerTest.users_default.csv",
            "CalendarDataSyncManagerTest.SeveralAssignment.before.csv"},
            after = "CalendarDataSyncManagerTest.after.csv")
    public void syncCalendarWithSeveralAssignmentEmployees() {
        mockClock(LocalDateTime.parse("2021-12-27T14:00:00"));
        calendarDataSyncManager.syncCalendarData();
    }

    @Test
    @DisplayName("Смена домена среди месяца")
    @DbUnitDataSet(before = {"CalendarDataSyncManagerTest.users_default.csv",
            "CalendarDataSyncManagerTest.TransferInMid.before.csv"},
            after = {"CalendarDataSyncManagerTest.after.csv", "CalendarDataSyncManagerTest.TransferInMid.after.csv"})
    public void syncCalendarWithTransferredInTheMiddleOfMonth() {
        mockClock(LocalDateTime.parse("2021-12-27T14:00:00"));
        calendarDataSyncManager.syncCalendarData();
    }

    @Test
    @DisplayName("Смена домена в начале месяца")
    @DbUnitDataSet(before = "CalendarDataSyncManagerTest.TransfeAtStart.before.csv",
            after = "CalendarDataSyncManagerTest.TransfeAtStart.after.csv")
    public void syncCalendarWithTransferredAtStartOfMonth() {
        mockClock(LocalDateTime.parse("2021-12-27T14:00:00"));
        calendarDataSyncManager.syncCalendarData();
    }

    @Test
    @DisplayName("Смена домена среди месяца с удалением старых записей")
    @DbUnitDataSet(before = "CalendarDataSyncManagerTest.TransferInMidWithDeletionFirst.before.csv.csv",
            after =  "CalendarDataSyncManagerTest.TransferInMidWithDeletionFirst.after.csv")
    public void syncCalendarWithTransferredInTheMiddleOfMonthWithDeletionFirst() {
        mockClock(LocalDateTime.parse("2021-12-27T14:00:00"));
        calendarDataSyncManager.syncCalendarData();
    }

    @Test
    @DisplayName("Смена домена среди месяца с удалением старых записей")
    @DbUnitDataSet(before = "CalendarDataSyncManagerTest.TransferInMidWithDeletionSecond.before.csv.csv",
            after =  "CalendarDataSyncManagerTest.TransferInMidWithDeletionSecond.after.csv")
    public void syncCalendarWithTransferredInTheMiddleOfMonthWithDeletionSecond() {
        mockClock(LocalDateTime.parse("2021-12-27T14:00:00"));
        calendarDataSyncManager.syncCalendarData();
    }
}
