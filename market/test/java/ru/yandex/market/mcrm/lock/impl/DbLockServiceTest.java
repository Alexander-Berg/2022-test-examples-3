package ru.yandex.market.mcrm.lock.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.mcrm.lock.DbLockServiceTestConfig;
import ru.yandex.market.mcrm.lock.LockService;
import ru.yandex.market.mcrm.lock.Result;
import ru.yandex.market.mcrm.tx.TxService;

@ContextConfiguration(classes = DbLockServiceTestConfig.class)
@TestPropertySource("classpath:/ru/yandex/market/mcrm/lock/test.properties")
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DbLockServiceTest {

    @Inject
    DbLockService lockService;

    @Inject
    TxService txService;

    @Inject
    DataSource dataSource;

    /**
     * Проверяем, что нельзя взять лок повторно
     */
    @Test
    public void tryDoubleLock() {
        String key = Randoms.string();
        String methodResult = Randoms.string();

        Result<Result<String>> result = lockService.doInTryLock(key,
                () -> lockService.doInTryLock(key, () -> methodResult));

        Result<String> innerResult = result.value();
        Assertions.assertTrue(result.hasResult(), "Должны получить блокировку, т.к. key ранее не блокировался");
        Assertions.assertFalse(innerResult.hasResult(), "Недолжны получить блокировку, т.к. key заблокирован ранее");
    }

    /**
     * Простой сценарий получения лока. Лок должны получить успешно т.к. в базе не должно быть локов с таким ключем.
     */
    @Test
    public void tryLock() {
        String key = Randoms.string();
        String methodResult = Randoms.string();

        Result<String> result = lockService.doInTryLock(key, () -> methodResult);

        Assertions.assertTrue(result.hasResult());
        Assertions.assertEquals(methodResult, result.value());
    }

    /**
     * Проверяем сценарий, что после освобождения лока он может браться повторно
     */
    @Test
    public void trySequenceLock() {
        String key = Randoms.string();

        lockService.doInTryLock(key, () -> null);
        Result<String> result = lockService.doInTryLock(key, () -> null);

        Assertions.assertTrue(result.hasResult());
    }

    /**
     * Проверяем, что во время фонового процесса актуализации локов не возникает {@link Exception ошибок}
     */
    @Test
    public void actualizeLocks() {
        String key = Randoms.string();

        lockService.doInTryLock(key, () -> {
            lockService.actualizeLocks();
            return null;
        });
    }

    /**
     * Проверяем, что метод берет блокировку по ключу
     */
    @Test
    public void lock() throws Exception {
        String key = Randoms.string();

        LockService.SimpleLock lock = lockService.lock(key, 1, TimeUnit.SECONDS);

        // Проверка утверждений
        Result<Object> result = lockService.doInTryLock(key, () -> null);
        Assertions.assertFalse(result.hasResult(), "Не должны получить блокировку т.к. key уже заблокирован");

        // Очистка
        lock.close();
    }

    /**
     * Проверяем, что метод не берет блокировку по заблокированному ключу
     */
    @Test
    public void lockLocked() {
        Assertions.assertThrows(TimeoutException.class, () -> {
            String key = Randoms.string();
            lockService.lock(key, 1, TimeUnit.SECONDS);

            // Вызов системы
            lockService.lock(key, 500, TimeUnit.MILLISECONDS);

            // Очистка
        });
    }

    /**
     * Проверяем, что метод ждет возможности взятия блокировки по заблокированному ключу и берет ее после освобождения
     * ранее установленной блокировки.
     */
    @Test
    public void lockLockedTimeout() throws Exception {
        String key = Randoms.string();

        LockService.SimpleLock preLock = lockService.lock(key, 1, TimeUnit.SECONDS);
        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (Exception ignored) {
            }
            preLock.close();
        }).start();

        // Вызов системы
        lockService.lock(key, 1, TimeUnit.SECONDS);

        // Проверка утверждений
        // ожидаем отсутствия исключения TimeoutException
        // т.к. preLock должен освободиться до истечения таймаута ожидания блокировки
    }

    /**
     * Проверяем, что метод вернет результат выполнения action в случае успешного звятия блокировки
     */
    @Test
    public void doInLock() throws Exception {
        String key = Randoms.string();
        String value = Randoms.string();

        String result = lockService.doInLock(key, 1, TimeUnit.SECONDS, () -> value);

        Assertions.assertEquals(value,
                result, "Должны взять блокировку, вызвать action, и вернуть результат выполнения action");
    }

    @Test
    public void runInLock() throws Exception {
        String key = Randoms.string();

        AtomicBoolean result = new AtomicBoolean(false);
        lockService.runInLock(key, 1, TimeUnit.SECONDS, () -> result.set(true));

        Assertions.assertTrue(result.get(), "Должны получить блокировку и вызвать метод");
    }

    /**
     * Проверяем, что в для заблокированного ключа выбрасываем TimeoutException
     */
    @Test
    public void doInLockLocked() {
        Assertions.assertThrows(TimeoutException.class, () -> {
            String key = Randoms.string();
            String value = Randoms.string();
            lockService.lock(key, 10, TimeUnit.SECONDS);

            // вызов системы
            lockService.doInLock(key, 1, TimeUnit.SECONDS, () -> value);
        });
    }

    /**
     * Проверяем, что отпускаем блокировку после исключения
     */
    @Test
    public void doInLockWithException() {
        String key = Randoms.string();

        // вызов системы
        try {
            lockService.doInLock(key, 1, TimeUnit.SECONDS, () -> {
                throw new RuntimeException();
            });
        } catch (Exception ignored) {
        }

        // проверка утверждений
        Result<Object> result = lockService.doInTryLock(key, () -> null);
        Assertions.assertTrue(result.hasResult(), "Должны получить результат т.к. блокировка должна была быть " +
                "освобождена");
    }

    @Test
    public void tryAddLocks() throws Exception {
        String key1 = Randoms.string();
        String key2 = Randoms.string();

        List<String> keys = Arrays.asList(key1, key2);
        LockService.SimpleLock lock = lockService.lock(keys, 1, TimeUnit.SECONDS);

        // проверка утверждений
        Result<Object> result1 = lockService.doInTryLock(key1, () -> null);
        Assertions.assertFalse(result1.hasResult(), "Не должны получить блокировку т.к. key1 уже заблокирован");

        Result<Object> result2 = lockService.doInTryLock(key2, () -> null);
        Assertions.assertFalse(result2.hasResult(), "Не должны получить блокировку т.к. key2 уже заблокирован");

        // очистка системы
        lock.close();
    }

    @Test
    public void tryAddLocks_locked() throws Exception {
        String key1 = Randoms.string();
        String key2 = Randoms.string();

        lockService.lock(key1, 1, TimeUnit.SECONDS);

        // вызов системы
        boolean timeout = false;
        List<String> keys = Arrays.asList(key1, key2);
        try {
            lockService.lock(keys, 1, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            timeout = true;
        }
        Assertions.assertTrue(
                timeout, "Должны получить timeout во время блокировки т.к. ранее заблокировали один из ключей");
    }

    @Test
    public void tryAddLocks_freeAfterException() throws Exception {
        String key1 = Randoms.string();
        String key2 = Randoms.string();

        lockService.lock(key1, 1, TimeUnit.SECONDS);

        // вызов системы
        List<String> keys = Arrays.asList(key1, key2);
        try {
            lockService.lock(keys, 1, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
        }


        Result<Object> result2 = lockService.doInTryLock(key2, () -> null);
        Assertions.assertTrue(
                result2.hasResult(), "Должны получить блокировку т.к. key2 должен быть разблокирован после исключения");
    }

    @Test
    public void tryLock_partial() {
        String key1 = Randoms.string();
        String key2 = Randoms.string();

        lockService.tryLock(key1);

        // вызов системы
        Map<String, LockService.SimpleLock> result = lockService.tryLock(Lists.newArrayList(key1, key2));

        Assertions.assertEquals(1, result.size(), "Должны взять блокировку только на key2");
        Assertions.assertTrue(result.containsKey(key2), "Должны взять блокировку только на key2");
    }

    @Test
    public void orphanedLocks() throws Exception {
        String key = Randoms.string();

        LockService.SimpleLock lock = lockService.lock(key, 1, TimeUnit.SECONDS);
        Assertions.assertTrue(lock.isActual(), "Блокировка должна быть взята на инстансе");

        txService.runInNewTx(() -> {
            try {
                // блокируем запись о блокировке на уровне БД
                dataSource.getConnection().createStatement()
                        .execute("UPDATE lock_service_locks SET last_activity = NOW()");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // должны отпустить блокировку на уровне сервиса, но не на уровне БД
            // это должно произойти т.к. мы заблокировали запись в текущей транзакции, а освобождение блокировки
            // происходит в новой транзакции.
            lock.close();

            Assertions.assertFalse(lock.isActual(), "На уровне сервиса блокировка уже освобождена");
            Assertions.assertTrue(
                    lockService.isInstanceOrphaned(key), "Блокировка должна не быть освобожденной на уровне БД");
        });

        lockService.actualizeOrphanedLocks();

        Assertions.assertFalse(lockService.isLockedByInstance(Set.of(key)));
        Assertions.assertFalse(lockService.isInstanceOrphaned(key));
    }

    @Test
    public void testHasLocks() throws Exception {
        Set<String> keys = Set.of(Randoms.string(), Randoms.string());
        lockService.lock(keys, 1, TimeUnit.SECONDS);
        boolean isLocked = lockService.isLockedByInstance(keys);
        Assertions.assertTrue(isLocked, "All keys must be locked");
    }
}
