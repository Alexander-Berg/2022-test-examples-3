package ru.yandex.market.hrms.tms.manager.sc;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.domain.overtime.EmployeeOvertimeDetectionService;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

@DbUnitDataSet(schema = "public", before = "ScEmployeeOvertimeDetectionTest.before.csv")
public class ScEmployeeOvertimeDetectionTest extends AbstractTmsTest {

    @Autowired
    private EmployeeOvertimeDetectionService employeeOvertimeDetector;
    @Autowired
    private Clock clock;

    @Test
    @DbUnitDataSet(schema = "public", after = "ScEmployeeOvertimeDetectionTest.after.csv")
    void detectEmployeeOvertimes() {
        mockClock(LocalDateTime.of(2021, 11, 21, 21, 0));
        LocalDate today = DateTimeUtil.toLocalDateTime(clock.instant()).toLocalDate();
        employeeOvertimeDetector.detectEmployeeOvertimes(
                44L,
                new LocalDateInterval(today.minusDays(1), today));
    }
}
