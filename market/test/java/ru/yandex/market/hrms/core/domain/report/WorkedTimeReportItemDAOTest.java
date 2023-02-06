package ru.yandex.market.hrms.core.domain.report;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.hrms.core.AbstractCoreTest;

class WorkedTimeReportItemDAOTest extends AbstractCoreTest {

    @Autowired
    private WorkedTimeReportItemDAO reportItemDAO;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void upsert() {
        var date = LocalDate.parse("1970-01-01");
        var n = 5;
        var items = IntStreamEx.range(n)
                .mapToObj(date::plusDays)
                .map(this::getTestReportItem)
                .toMutableList();
        reportItemDAO.upsert(items.subList(0, n - 1));
        var m1 = getCtidByDateMap();

        var changedDate = date.plusDays(1);
        items.set(1, getTestReportItem(changedDate, Instant.MIN));
        reportItemDAO.upsert(items.subList(1, n));
        var m2 = getCtidByDateMap();

        Assertions.assertNotEquals(m1.get(changedDate), m2.get(changedDate));
        StreamEx.of(m1.keySet())
                .removeBy(Function.identity(), changedDate)
                .forEach(k -> Assertions.assertEquals(m1.get(k), m2.get(k)));
    }

    private Map<LocalDate, String> getCtidByDateMap() {
        var results = jdbcTemplate.query(
                "select date, ctid from report_worked_time",
                (rs, rowNum) -> Pair.of(rs.getString(1), rs.getString(2)));
        return StreamEx.of(results).mapToEntry(Pair::getFirst, Pair::getSecond).mapKeys(LocalDate::parse).toMap();
    }

    private WorkedTimeReportItemEntity getTestReportItem(LocalDate date) {
        return getTestReportItem(date, null);
    }

    private WorkedTimeReportItemEntity getTestReportItem(LocalDate date, @Nullable Instant timexInstant) {
        var reportData = new WorkedTimeReportData(Instant.MIN, Instant.MIN, Duration.ZERO,
                timexInstant, null, null, null, null, null, null, null, false, false, false);
        return WorkedTimeReportItemEntity.builder()
                .date(date)
                .domainId(1L)
                .employeeId(1L)
                .content(reportData)
                .build();
    }
}