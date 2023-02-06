package ru.yandex.market.hrms.core.domain.employee;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Table;
import one.util.streamex.EntryStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.domain.HROperationTypeRepo;
import ru.yandex.market.hrms.core.domain.employee.calendar.CalendarService;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeAssignmentEntity;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeAssignmentRepo;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeHROperationDetail;
import ru.yandex.market.hrms.core.domain.employee.repo.HROperationType;
import ru.yandex.market.hrms.core.domain.property.repo.GapPropertyRepo;
import ru.yandex.market.hrms.core.domain.util.MergedEntity;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

class CalendarServiceTest extends AbstractCoreTest {
    private static Map<Long, HROperationType> hrOperationTypeMap = Map.of();

    @Autowired
    private HROperationTypeRepo hrOperationTypeRepo;

    @Autowired
    private EmployeeAssignmentRepo employeeAssignmentRepo;

    @Autowired
    private CalendarService calendarService;

    @Autowired
    private GapPropertyRepo gapPropertyRepo;

    @BeforeEach
    public void init() {
        hrOperationTypeMap = hrOperationTypeRepo.findAllMapByNaturalId(HROperationType::getId);
    }

    @Test
    @DbUnitDataSet(before = "CalendarServiceTest.before.csv")
    public void test1() {
        LocalDateInterval interval = new LocalDateInterval(
                LocalDate.of(2021, 4, 28),
                LocalDate.of(2021, 5, 4)
        );

        List<EmployeeAssignmentEntity> assignments = employeeAssignmentRepo.findAll();
        Table<Long, LocalDate, List<MergedEntity<EmployeeHROperationDetail>>> result =
                calendarService.loadEmployeeHrOperations(assignments, interval, hrOperationTypeMap);

        var actual = DateTimeUtil.asStream(interval)
                .mapToEntry(result.columnMap()::get)
                .nonNullValues()
                .mapValues(Map::keySet)
                .toSortedMap();

        var expected = EntryStream.of(
                LocalDate.of(2021, 4, 28), 3L,
                LocalDate.of(2021, 4, 29), 3L,
                LocalDate.of(2021, 4, 30), 4L,
                LocalDate.of(2021, 5, 1), 5L,
                LocalDate.of(2021, 5, 2), 5L,
                LocalDate.of(2021, 5, 3), 5L
        ).mapValues(Set::of).toSortedMap();

        Assertions.assertEquals(expected, actual);

    }
}
