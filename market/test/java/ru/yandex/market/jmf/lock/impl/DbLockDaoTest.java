package ru.yandex.market.jmf.lock.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.postgresql.util.PSQLException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.lock.LockServiceTestConfiguration;
import ru.yandex.market.jmf.tx.TxService;

@ContextConfiguration(classes = LockServiceTestConfiguration.class)
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DbLockDaoTest {

    @Inject
    DbLockDao dao;
    @Inject
    TxService txService;

    /**
     * Проверяем, что нельзя взять лок повторно
     */
    @Test
    public void tryDoubleLock() {
        String key = Randoms.string();
        String instance = Randoms.string();
        List<String> keys = Collections.singletonList(key);

        Collection<String> firstLocks = new ArrayList<>();
        AtomicReference<SQLException> exception = new AtomicReference<>();
        txService.runInNewTx(() -> {
            try {
                firstLocks.addAll(dao.tryAddLocks0(keys, instance));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            txService.runInNewTx(() -> {
                try {
                    dao.tryAddLocks0(keys, instance);
                } catch (SQLException e) {
                    exception.set(e);
                }
            });
        });

        Assertions.assertTrue(firstLocks.contains(key));

        SQLException e = exception.get();
        Assertions.assertTrue(e instanceof PSQLException);
        // см. https://postgrespro.ru/docs/postgrespro/9.5/errcodes-appendix
        Assertions.assertEquals("55P03", e.getSQLState(), "Должны получить ошибку блокировки");
    }
}
