package ru.yandex.market.billing.tasks;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Тесты для {@link ChangeTariffExecutor}.
 *
 * @author Vadim Lyalin
 */
public class ChangeTariffExecutorTest extends FunctionalTest {
    @Autowired
    private ChangeTariffExecutor changeTariffExecutor;

    @Test
    @DbUnitDataSet(before = "ChangeTariffExecutorTest.before.csv", after = "ChangeTariffExecutorTest.after.csv")
    void testChangeTariff() {
        changeTariffExecutor.doJob(null);
    }
}
