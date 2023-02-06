package ru.yandex.market.rg.asyncreport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.rg.config.FunctionalTest;

/**
 * Тесты для {@link ReturnToPendingReportsExecutor}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class ReturnToPendingReportsExecutorTest extends FunctionalTest {

    @Autowired
    private ReturnToPendingReportsExecutor<ReportsType> returnToPendingReportsExecutor;

    @Test
    @DisplayName("Отчёты в статусе PROCESSING должны стать доступными для новых воркеров (перейдут обратно в PENDING)")
    @DbUnitDataSet(
            before = "ReturnToPendingReportsExecutorTest.shouldReturnToPendingAt.before.csv",
            after = "ReturnToPendingReportsExecutorTest.shouldReturnToPendingAt.after.csv"
    )
    void shouldReturnToPendingAt() {
        returnToPendingReportsExecutor.returnToPendingReports();
    }
}
