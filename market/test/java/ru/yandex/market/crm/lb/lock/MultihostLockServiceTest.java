package ru.yandex.market.crm.lb.lock;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;

import ru.yandex.market.crm.lb.test.TestDataSourceConfig;
import ru.yandex.market.mcrm.db.Constants;
import ru.yandex.market.mcrm.db.test.DbTestTool;
import ru.yandex.market.mcrm.utils.background.BackgroundService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author apershukov
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestDataSourceConfig.class)
class MultihostLockServiceTest {

    private static final String LOCK_KEY = "lock_key";

    @Inject
    private DbTestTool dbTestTool;

    @Inject
    @Named(Constants.DEFAULT_JDBC_TEMPLATE)
    private JdbcTemplate jdbcTemplate;

    @Inject
    @Named(Constants.DEFAULT_TRANSACTION_MANAGER)
    private PlatformTransactionManager txManager;

    @AfterEach
    public void tearDown() {
        dbTestTool.clearDatabase();
    }

    /**
     * Если инстанс захватил лок, он считается удерживающим его
     */
    @Test
    void testAfterSingleHostLocksKeyItIsLocked() {
        var service = createService("sas", "host.yandex.net");
        assertTrue(service.tryLock(LOCK_KEY));
        assertTrue(service.isLocked(LOCK_KEY));
    }

    /**
     * Если лок удерживается инстансом одного ДЦ, он не может быть захвачен инстансом другого
     */
    @Test
    void testIfKeyIsLockedInOneDcItCannotBeLockedInAnother() {
        var service1 = createService("sas", "host1.yandex.net");
        var service2 = createService("vla", "host2.yandex.net");

        assertTrue(service1.tryLock(LOCK_KEY));
        assertFalse(service2.tryLock(LOCK_KEY), "Lock has been locked multiple times");
    }

    /**
     * Если лок удерживается инстансом одного ДЦ он может так же быть захвачен другим инстансом
     * того же ДЦ
     */
    @Test
    void testIfKeyIsLockedOnOneDcItCanBeLockedAgainInSameDc() {
        var service1 = createService("sas", "host1.yandex.net");
        var service2 = createService("sas", "host2.yandex.net");

        assertTrue(service1.tryLock(LOCK_KEY));
        assertTrue(service2.tryLock(LOCK_KEY), "Lock cannot be taken in same dc");
    }

    /**
     * В случае если лок был освобожден единственным инстансом в ДЦ он может быть захвачен инстансом
     * в другом ДЦ
     */
    @Test
    void testIfLockIsReleasedItCanBeTakenByAnotherInstance() {
        var service1 = createService("sas", "host1.yandex.net");
        var service2 = createService("vla", "host2.yandex.net");

        service1.tryLock(LOCK_KEY);
        service1.release(LOCK_KEY);

        assertTrue(service2.tryLock(LOCK_KEY), "Lock is still unavailable after release");
    }

    /**
     * В случае если лок удерживался двумя разными инстансами в одном ДЦ и был отпущен одним из них,
     * его все равно нельзя захватить инстансом из другого ДЦ
     */
    @Test
    void testIfLockIsNotReleasedByAllInstancesInDcItCannotBeTakenInAnother() {
        var service1 = createService("sas", "host1.yandex.net");
        var service2 = createService("sas", "host2.yandex.net");
        var service3 = createService("vla", "host3.yandex.net");

        service1.tryLock(LOCK_KEY);
        service2.tryLock(LOCK_KEY);
        service1.release(LOCK_KEY);

        assertFalse(service3.tryLock(LOCK_KEY), "Lock is taken");
    }

    /**
     * Если инстанс не брал конкретный лок, не считается что он его удеживает, даже если
     * этот лок удерживается другим инстансом в том же ДЦ.
     */
    @Test
    void testIfLockHasNotBeenTakenByInstanceItIsNotConsideredLockedByIt() {
        var service1 = createService("sas", "host1.yandex.net");
        var service2 = createService("sas", "host2.yandex.net");

        service1.tryLock(LOCK_KEY);

        assertFalse(service2.isLocked(LOCK_KEY), "Instance has not taken lock");
    }

    /**
     * Актуализатор времени активности устанавливает текущее время для держателя лока
     */
    @Test
    void testLastActivityTimestampUpdates() {
        insertLock(LOCK_KEY);

        var host1 = "host1.yandex.net";
        var host2 = "host2.yandex.net";

        var lastActivityTime = LocalDateTime.now().minusHours(1)
                .truncatedTo(ChronoUnit.SECONDS);

        insertLockHolder(host1, lastActivityTime);
        insertLockHolder(host2, lastActivityTime);

        var service = createService("sas", host1);
        service.actualizeHolders();

        var time1 = getActivityTime(host1);
        assertTrue(time1.isAfter(lastActivityTime));

        var time2 = getActivityTime(host2);
        assertEquals(time2, lastActivityTime);
    }

    /**
     * Удерживающие локи, которые долго не обновлялись удаляются
     */
    @Test
    void testOutdatedHoldersAreDeleted() {
        insertLock(LOCK_KEY);

        var host1 = "host1.yandex.net";
        var host2 = "host2.yandex.net";

        insertLockHolder(host1, LocalDateTime.now().minusHours(1));
        insertLockHolder(host2, LocalDateTime.now());

        var service = createService("sas", host1);
        service.deleteOrphaned();

        assertFalse(holderExists(host1));
        assertTrue(holderExists(host2));
    }

    /**
     * Локи которые остались без инстансов которые их держат удаляются
     */
    @Test
    void testLocksWithoutHoldersAreDeleted() {
        insertLock(LOCK_KEY);

        var orphanedLockKey = "orphaned_lock";
        insertLock(orphanedLockKey);

        insertLockHolder("host1.yandex.net", LocalDateTime.now());

        var service = createService("sas", "host2.yandex.net");
        service.deleteOrphaned();

        assertTrue(lockExists(LOCK_KEY));
        assertFalse(lockExists(orphanedLockKey));
    }

    private boolean lockExists(String lockKey) {
        var result = jdbcTemplate.queryForObject(
                "SELECT EXISTS(\n" +
                "    SELECT 1 FROM multihost_lock__locks \n" +
                "    WHERE \n" +
                "          lock_key = ?\n" +
                ")",
                Boolean.class,
                lockKey
        );

        return Boolean.TRUE.equals(result);
    }

    private void insertLock(String lockKey) {
        jdbcTemplate.update(
                "INSERT INTO multihost_lock__locks (lock_key, holding_dc)\n" +
                "VALUES (?, ?)",
                lockKey, "sas"
        );
    }

    private void insertLockHolder(String hostKey, LocalDateTime lastActivity) {
        jdbcTemplate.update(
                "INSERT INTO multihost_lock__holders (lock_key, host_key, last_activity)\n" +
                "VALUES (?, ?, ?)",
                LOCK_KEY,
                hostKey,
                lastActivity
        );
    }

    private boolean holderExists(String hostKey) {
        var result = jdbcTemplate.queryForObject(
                "SELECT EXISTS(\n" +
                "    SELECT 1 FROM multihost_lock__holders\n" +
                "    WHERE\n" +
                "        lock_key = ? AND host_key = ?\n" +
                ")",
                Boolean.class,
                LOCK_KEY, hostKey
        );

        return Boolean.TRUE.equals(result);
    }

    private LocalDateTime getActivityTime(String hostKey) {
        var time = jdbcTemplate.queryForObject(
                "SELECT last_activity FROM multihost_lock__holders\n" +
                "WHERE\n" +
                "    lock_key = ? AND host_key = ?",
                LocalDateTime.class,
                LOCK_KEY,
                hostKey
        );

        assertNotNull(time);
        return time.truncatedTo(ChronoUnit.SECONDS);
    }

    private MultihostLockService createService(String dc, String hostname) {
        return new MultihostLockService(jdbcTemplate, mock(BackgroundService.class), txManager, dc, hostname);
    }
}
