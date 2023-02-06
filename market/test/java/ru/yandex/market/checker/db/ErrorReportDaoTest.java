package ru.yandex.market.checker.db;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checker.FunctionalTest;
import ru.yandex.market.checker.yql.model.ErrorReport;
import ru.yandex.market.checker.yql.model.ErrorReportRow;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checker.yql.model.MismatchType.FIRST_ABSENT;
import static ru.yandex.market.checker.yql.model.MismatchType.SECOND_ABSENT;
import static ru.yandex.market.checker.yql.model.MismatchType.VALUE_MISMATCH;

@ParametersAreNonnullByDefault
public class ErrorReportDaoTest extends FunctionalTest {

    @Autowired
    private ErrorReportDao errorReportDao;

    @Test
    @DbUnitDataSet(
            before = "error_report.before.csv",
            after = "error_report.after.csv")
    void test_shouldSaveErrorReport() {
        ErrorReport errorReport = new ErrorReport(1, "QUEUE-1", "someId",
                List.of(
                        new ErrorReportRow("count", FIRST_ABSENT, 10L),
                        new ErrorReportRow("fact_count", SECOND_ABSENT, 10L),
                        new ErrorReportRow("defect_count",VALUE_MISMATCH, 1310L)
        ));
        errorReportDao.saveErrorReports(List.of(errorReport));
    }

    @Test
    @DbUnitDataSet(before = "error_report.after.csv")
    @DisplayName("Получаем отчет о запуске из БД")
    void test_shouldRetrieveErrorReport_WhenTaskId_Given() {
        ErrorReport errorReport = errorReportDao.getErrorReport(1);
        assertNotNull(errorReport);
        assertEquals(errorReport.getErrorData().size(), 3);
    }

    @Test
    @DbUnitDataSet(before = "error_report.after.csv")
    @DisplayName("Пустой отчет не падает")
    void test_shouldRetrieveEmptyReport_whenWrongIdGiven() {
        ErrorReport errorReport = errorReportDao.getErrorReport(1123);
        assertNull(errorReport);
    }
}
