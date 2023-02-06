package ru.yandex.market.loyalty.core.dao;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.accounting.AccountDao;
import ru.yandex.market.loyalty.core.model.accounting.AccountMatter;
import ru.yandex.market.loyalty.core.model.accounting.AccountType;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 14.06.17
 */
public class AccountDaoTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private AccountDao accountDao;

    @Override
    protected boolean shouldCheckConsistence() {
        //для консистенции необходимо создать транзакции на пополнение счёта
        return false;
    }

    @Test
    public void add() {
        long id = accountDao.createAccount(AccountType.ACTIVE, AccountMatter.MONEY, null, false);
        assertEquals(0, accountDao.getBalance(id).longValueExact());
    }

    @Test
    public void bulkAndSingleGetAccount() {
        long id = accountDao.createAccount(AccountType.ACTIVE, AccountMatter.MONEY, null, false);
        assertThat(
                accountDao.getAccounts(Collections.singleton(id)).values(),
                contains(samePropertyValuesAs(accountDao.getAccount(id)))
        );
    }

    @Test
    public void getTechnicalAccountId() {
        Arrays.stream(AccountMatter.values()).forEach(value ->
                assertTrue(accountDao.getTechnicalAccountId(value) >= 0)
        );
    }
}
