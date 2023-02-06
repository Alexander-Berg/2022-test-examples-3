package ru.yandex.market.vendors.analytics.tms.jobs.widget;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;

/**
 * Функциональный тест для джобы {@link CorrectWidgetPositionExecutor}.
 *
 * @author ogonek
 */
@DbUnitDataSet(before = "CorrectWidgetPosition.before.csv")
public class CorrectWidgetPositionExecutorTest extends FunctionalTest {

    @Autowired
    private CorrectWidgetPositionExecutor correctWidgetPositionExecutor;

    @Test
    @DbUnitDataSet(after = "CorrectWidgetPosition.after.csv")
    void correctWidgetPosition() {
        correctWidgetPositionExecutor.doJob(null);
    }
}
