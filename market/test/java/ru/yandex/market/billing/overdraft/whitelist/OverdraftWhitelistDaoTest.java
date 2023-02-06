package ru.yandex.market.billing.overdraft.whitelist;

import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Тесты для {@link OverdraftWhitelistDao}.
 *
 * @author vbudnev
 */
class OverdraftWhitelistDaoTest extends FunctionalTest {

    @Autowired
    private OverdraftWhitelistDao overdraftWhitelistDao;

    @DbUnitDataSet(before = "db/OverdraftWhitelistDaoTest.before.csv")
    @Test
    void test_getWhiteListedClients() {
        assertThat(
                overdraftWhitelistDao.getWhiteListedClients(),
                Matchers.containsInAnyOrder(7L, 5L, 1L)
        );
    }

    @DbUnitDataSet(
            before = "db/OverdraftWhitelistDaoTest.before.csv",
            after = "db/OverdraftWhitelistDaoTest.addClient.after.csv"
    )
    @Test
    void test_addClient() {
        overdraftWhitelistDao.addClient(ImmutableSet.of(8L, 9L));
    }

}
