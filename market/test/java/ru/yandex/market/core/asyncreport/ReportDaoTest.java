package ru.yandex.market.core.asyncreport;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.asyncreport.model.ReportGenerationInfo;
import ru.yandex.market.core.asyncreport.model.ReportsType;

/**
 * Тесты для {@link ReportsDao}
 */
@DbUnitDataSet(before = "AsyncReportDao.testDescriptionLength.before.csv")
class ReportDaoTest extends FunctionalTest {
    @Autowired
    private ReportsDao<ReportsType> reportsDao;

    @ParameterizedTest
    @ValueSource(ints = {1, 100, 4000, 4001, 10000})
    void testDescriptionLength(int descriptionLength) {
        String bigString = "a".repeat(descriptionLength);
        reportsDao.updateReportState(
                "my_report_id",
                ReportState.PENDING,
                ReportGenerationInfo.builder()
                        .setDescription(bigString)
                        .build(),
                Instant.parse("2021-01-12T10:00:00Z")
        );
    }

    @Test
    void testReportInfosWithFilterWithEmptyIds() {
        reportsDao.getReportInfosWithFilter(Collections.emptyList(), Collections.emptyList(),
                Timestamp.valueOf("2021-01-12 10:00:00"), 5, 5);
    }

    @Test
    void markOldestAboveLimitReportsToDelete() {
        reportsDao.markOldestAboveLimitReportsToDelete(
                1234L,
                ReportsType.ASSORTMENT,
                0,
                false
        );
    }

    @Test
    void markOutOfTtlReportsToDelete() {
        reportsDao.markOutOfTtlReportsToDelete(3);
    }

    @Test
    void markOutOfTtlEmptyReportsToDelete() {
        reportsDao.markOutOfTtlEmptyReportsToDelete(3);
    }
}
