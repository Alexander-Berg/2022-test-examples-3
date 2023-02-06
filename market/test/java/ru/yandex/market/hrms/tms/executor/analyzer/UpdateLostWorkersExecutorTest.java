package ru.yandex.market.hrms.tms.executor.analyzer;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.tms.AbstractTmsTest;

/**
 * {@link UpdateLostWorkersExecutor}
 */
@DbUnitDataSet(before = "AnalyzeLostWorkersExecutorTest.before.csv")
public class UpdateLostWorkersExecutorTest extends AbstractTmsTest {

    @Autowired
    private UpdateLostWorkersExecutor updateLostWorkersExecutor;

    @Test
    @DisplayName("Поиск потеряшек (которые уже давно не ходят в офис)")
    @DbUnitDataSet(after = "AnalyzeLostWorkersExecutorTest.after.csv")
    void test() {
        mockClock(LocalDateTime.of(2021, 3, 20, 15, 0));
        updateLostWorkersExecutor.doRealJob(null);
    }
}
