package ru.yandex.market.hrms.core.domain.analytics;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;

public class CalendarDataHistoryServiceTest extends AbstractCoreTest {
    @Autowired
    private CalendarDataHistoryService calendarDataHistoryService;

    @Test
    @DbUnitDataSet(schema = "public",
            before = "CalendarDataHistoryServiceTest.CheckTempAbsenceBeforeDraftAbsence.before.csv")
    void checkTempAbsenceBeforeDraftAbsence() {
        mockClock(LocalDateTime.of(2021, 10, 28, 20, 45));
        String result = calendarDataHistoryService.checkTempAbsenceBeforeDraftAbsence();
        Assertions.assertEquals("2;employee_id=1 on 2021-10-28", result);
    }

}
