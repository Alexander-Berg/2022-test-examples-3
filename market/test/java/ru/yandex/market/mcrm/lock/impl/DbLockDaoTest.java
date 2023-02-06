package ru.yandex.market.mcrm.lock.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.postgresql.util.PSQLException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.mcrm.lock.DbLockServiceTestConfig;
import ru.yandex.market.mcrm.tx.TxService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration(classes = DbLockServiceTestConfig.class)
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource("classpath:/ru/yandex/market/mcrm/lock/test.properties")
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
        AtomicReference<Exception> exception = new AtomicReference<>();
        txService.runInNewTx(() -> {
            try {
                firstLocks.addAll(dao.tryAddLocks0(keys, instance));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            txService.runInNewTx(() -> {
                try {
                    dao.tryAddLocks0(keys, instance);
                } catch (Exception e) {
                    exception.set(e);
                }
            });
        });

        assertTrue(firstLocks.contains(key));

        var e = exception.get();
        assertNotNull(e);

        var cause = e.getCause();
        assertTrue(cause instanceof PSQLException);

        var sqlException = (PSQLException) cause;
        // см. https://postgrespro.ru/docs/postgrespro/9.5/errcodes-appendix
        assertEquals("55P03", sqlException.getSQLState(), "Должны получить ошибку блокировки");
    }
}
