package ru.yandex.market.hrms.tms.executor.analytics;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.domain.analytics.repo.CalendarDataRepo;
import ru.yandex.market.hrms.tms.AbstractTmsTest;

@DbUnitDataSet(before = {
        "CalendarDataHistoryExecutorTest.before.csv"
})
class CalendarDataHistoryExecutorTest extends AbstractTmsTest {

    @Autowired
    private CalendarDataRepo calendarDataRepo;
    @Autowired
    private CalendarDataHistoryExecutor executor;

    @Test
    @DbUnitDataSet(after = "CalendarDataHistoryExecutorTest.newValues.after.csv")
    public void addNewItems() {
        mockClock(LocalDateTime.parse("2021-11-11T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        executor.executeJob(null);
    }

    @Test
    @DbUnitDataSet(after = "CalendarDataHistoryExecutorTest.changedOnly.after.csv")
    public void storeOnlyChanges() {
        mockClock(LocalDateTime.parse("2021-11-11T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        executor.executeJob(null);

        mockClock(LocalDateTime.parse("2021-11-11T00:15:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        var startDate = LocalDate.parse("2021-11-04", DateTimeFormatter.ISO_LOCAL_DATE);
        var endDate = LocalDate.parse("2021-11-08", DateTimeFormatter.ISO_LOCAL_DATE);
        startDate.datesUntil(endDate)
                .forEach(ld-> calendarDataRepo.updateOne(1L, ld, 16L));
        executor.executeJob(null);

        mockClock(LocalDateTime.parse("2021-11-11T00:30:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        calendarDataRepo.updateOne(1L, startDate, 27);
        executor.executeJob(null);

        mockClock(LocalDateTime.parse("2021-11-11T00:45:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        endDate = startDate.plusDays(2);
        startDate.datesUntil(endDate)
                .forEach(ld -> calendarDataRepo.updateOne(1L, ld, 16L));
        executor.executeJob(null);
    }

}
