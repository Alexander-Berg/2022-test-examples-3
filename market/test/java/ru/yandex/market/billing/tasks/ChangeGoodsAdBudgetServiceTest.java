package ru.yandex.market.billing.tasks;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Тестируем {@link ChangeGoodsAdBudgetService}.
 *
 * @author samodurov-d@yandex-team.ru
 */
class ChangeGoodsAdBudgetServiceTest extends FunctionalTest {

    @Autowired
    private ChangeGoodsAdBudgetService service;

    @Test
    @DbUnitDataSet(before = "ChangeGoodsAdBudgetServiceTest.before.csv",
            after = "ChangeGoodsAdBudgetServiceTest.after.csv")
    void testChangeGoodsAdBudgetService() {
        service.updateGoodsAdBudget();
    }
}
