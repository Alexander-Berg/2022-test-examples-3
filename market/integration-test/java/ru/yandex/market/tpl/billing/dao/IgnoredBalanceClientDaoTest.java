package ru.yandex.market.tpl.billing.dao;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

/**
 * Тесты для {@link IgnoredBalanceClientDao}
 */
public class IgnoredBalanceClientDaoTest extends AbstractFunctionalTest {

    @Autowired
    private IgnoredBalanceClientDao ignoredBalanceClientDao;

    @Test
    @DbUnitDataSet(before = "/database/dao/ignoredBalanceClientDaoTest/before/getAll.csv")
    void testGetAll() {
        Set<Long> ignoredClients = ignoredBalanceClientDao.getAll();
        assertThat(ignoredClients, hasSize(3));
        assertThat(ignoredClients, containsInAnyOrder(1L, 2L, 3L));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/dao/ignoredBalanceClientDaoTest/before/addClients.csv",
            after = "/database/dao/ignoredBalanceClientDaoTest/after/addClients.csv")
    void testAddClients() {
        ignoredBalanceClientDao.add(2L); // дубликат, ничего не поменяется
        ignoredBalanceClientDao.add(5L); // новый

    }

    @Test
    @DbUnitDataSet(
            before = "/database/dao/ignoredBalanceClientDaoTest/before/removeClients.csv",
            after = "/database/dao/ignoredBalanceClientDaoTest/after/removeClients.csv")
    void testRemoveClients() {
        ignoredBalanceClientDao.remove(4L); // такого нет, все ок
        ignoredBalanceClientDao.remove(2L); // удалится

    }
}
