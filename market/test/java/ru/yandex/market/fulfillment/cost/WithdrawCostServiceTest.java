package ru.yandex.market.fulfillment.cost;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
/**
 * Тесты для {@link WithdrawCostService}.
 *
 * @author vbudnev
 */
class WithdrawCostServiceTest extends FunctionalTest {

    private static final LocalDate DATE_2019_03_14 = LocalDate.of(2019, 3, 14);
    @Autowired
    private WithdrawCostService withdrawCostService;

    @DbUnitDataSet(
            before = "db/WithdrawCostServiceTest.before.csv",
            after = "db/WithdrawCostServiceTest.after.csv"
    )
    @Test
    void test_calculateWithdrawCost() {
        withdrawCostService.calculateWithdrawCost(DATE_2019_03_14);
    }

}
