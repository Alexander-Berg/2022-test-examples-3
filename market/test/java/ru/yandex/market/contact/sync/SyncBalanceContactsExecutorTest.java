package ru.yandex.market.contact.sync;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

/**
 * Тесты для синхронизации с балансом {@link SyncContactsJobsConfig#syncBalanceContactsExecutor()}.
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "SyncBalanceContactsExecutorTest.csv")
public class SyncBalanceContactsExecutorTest extends FunctionalTest {
    @Autowired
    private SyncPassportContactsJob syncBalanceContactsExecutor;

    /**
     * Проверяет, что
     * <ul>
     *     <li>Не удаляется линк с бизнесом</li>
     *     <li>Добавляется линк с кампанией клиента контакта без роли</li>
     * </ul>
     */
    @Test
    @DbUnitDataSet(after = "SyncBalanceContactsExecutorTest.after.csv")
    void testJob() {
        syncBalanceContactsExecutor.doJob(null);
    }
}
