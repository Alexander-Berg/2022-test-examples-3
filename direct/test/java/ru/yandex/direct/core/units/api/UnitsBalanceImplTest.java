package ru.yandex.direct.core.units.api;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static ru.yandex.direct.core.units.service.UnitsService.DEFAULT_LIMIT;

public class UnitsBalanceImplTest {

    private UnitsBalance testedUnitsBalance;

    @Before
    public void setup() {
        testedUnitsBalance = new UnitsBalanceImpl(1L, DEFAULT_LIMIT, DEFAULT_LIMIT, 0);
    }

    @Test
    public void withdraw_modifyBalance() {
        testedUnitsBalance.withdraw(100);
        assertThat(testedUnitsBalance.balance(), is(DEFAULT_LIMIT - 100));
    }

    @Test
    public void withdraw_balanceIsNeverNegative() {
        testedUnitsBalance.withdraw(2 * testedUnitsBalance.balance());
        assertThat(testedUnitsBalance.balance(), is(0));
    }

    @Test
    public void withdraw_modifySpent() {
        testedUnitsBalance.withdraw(100);
        assertThat(testedUnitsBalance.spent(), is(100));
    }

    @Test
    public void withdraw_modifySpentInCurrentRequest() {
        testedUnitsBalance.withdraw(100);
        assertThat(testedUnitsBalance.spentInCurrentRequest(), is(100));
    }

    @Test
    public void isAvailable_trueWhenBalanceNotLessThanAmount() {
        assertThat(testedUnitsBalance.isAvailable(DEFAULT_LIMIT), is(true));
    }

    @Test
    public void isAvailable_falseWhenBalanceLessThanAmount() {
        assertThat(testedUnitsBalance.isAvailable(DEFAULT_LIMIT + 1), is(false));
    }
}
