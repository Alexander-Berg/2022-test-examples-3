package ru.yandex.market.hrms.core.domain.analytics;

import java.time.LocalDate;
import java.util.Optional;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.report.events.EmployeeEvent;
import ru.yandex.market.hrms.core.service.util.HrmsCollectionUtils;

import static ru.yandex.market.hrms.core.service.analyzer.ActivityLogSource.HRMS_NPO;
import static ru.yandex.market.hrms.core.service.analyzer.ActivityLogSource.SC;
import static ru.yandex.market.hrms.core.service.analyzer.ActivityLogSource.TIMEX;
import static ru.yandex.market.hrms.core.service.analyzer.ActivityLogSource.WMS;

class EmployeeEventsLoaderTest extends AbstractCoreTest {

    @Autowired
    private EmployeeEventsLoader eventsLoader;

    @Test
    @DbUnitDataSet(before = "EmployeeEventsLoaderTest.before.csv")
    void loadLogs() {
        var all = eventsLoader.loadByDates(LocalDate.MIN, LocalDate.MAX);
        var grouped = HrmsCollectionUtils.groupBy(all,
                x -> Optional.ofNullable(x.logSource()).map(Enum::name).orElse(""));

        Assertions.assertEquals(3, grouped.get(HRMS_NPO.name()).size(), "length of received npo");
        Assertions.assertEquals(60, grouped.get(SC.name()).size(), "length of received sc");
        Assertions.assertEquals(60, grouped.get(WMS.name()).size(), "length of received wms");
        Assertions.assertEquals(20, grouped.get(TIMEX.name()).size(), "length of received timex");

        var byEmployees = StreamEx.of(all).groupingBy(EmployeeEvent::employeeId);
        Assertions.assertEquals(12, byEmployees.keySet().size(), "analyzed employees");
    }
}