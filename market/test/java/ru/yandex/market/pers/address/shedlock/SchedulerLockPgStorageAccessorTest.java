package ru.yandex.market.pers.address.shedlock;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import net.javacrumbs.shedlock.core.LockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.address.util.BaseWebTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchedulerLockPgStorageAccessorTest extends BaseWebTest {


    public SchedulerLockPgStorageAccessor target;

    @Autowired
    public DataSource dataSource;

    @Autowired
    public JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        target = new SchedulerLockPgStorageAccessor(dataSource, jdbcTemplate, "shedlock");
    }

    @Test
    public void updateRecordSuccess() throws InterruptedException {
        final String nameProcess = "processResolvingGps";

        target.insertRecord(new LockConfiguration(nameProcess, Instant.now().plusSeconds(1),
                Instant.now().minusSeconds(20)));
        List<Timestamp> lockUntil = getLockUntil();
        TimeUnit.SECONDS.sleep(3);

        target.updateRecord(new LockConfiguration(nameProcess, Instant.now().plusSeconds(20)));

        List<Timestamp> lockUntilAfterUpdate = getLockUntil();

        assertEquals(1, lockUntil.size());
        assertEquals(1, lockUntilAfterUpdate.size());

        assertTrue(lockUntilAfterUpdate.get(0).after(lockUntil.get(0)));
    }

    @Test
    public void updateRecordNotChanged() throws InterruptedException {
        final String nameProcess = "processResolvingGps";

        target.insertRecord(new LockConfiguration(nameProcess, Instant.now().plusSeconds(100),
                Instant.now().minusSeconds(20)));
        List<Timestamp> lockUntil = getLockUntil();
        TimeUnit.SECONDS.sleep(2);

        target.updateRecord(new LockConfiguration(nameProcess, Instant.now().plusSeconds(100)));

        List<Timestamp> lockUntilAfterUpdate = getLockUntil();

        assertEquals(1, lockUntil.size());
        assertEquals(1, lockUntilAfterUpdate.size());

        assertEquals(lockUntil.get(0), lockUntilAfterUpdate.get(0));
    }

    @Test
    public void updateLockedRowsAfterApplicationRestart() {
        final String nameProcess = "processResolvingGps";

        long timeLockSec = 600L;
        target.insertRecord(new LockConfiguration(nameProcess, Instant.now().plusSeconds(timeLockSec),
                Instant.now().minusSeconds(20)));
        List<Timestamp> lockUntil = getLockUntil();
        target.updateLockedRowsAfterApplicationRestart();

        List<Timestamp> lockUntilAfterUpdate = getLockUntil();

        assertEquals(1, lockUntil.size());
        assertEquals(1, lockUntilAfterUpdate.size());
        assertTrue(lockUntilAfterUpdate.get(0).before(lockUntil.get(0)));

    }

    private List<Timestamp> getLockUntil() {
        return jdbcTemplate.query("SELECT lock_until FROM shedlock", (rs, num) -> rs.getTimestamp(
                "lock_until"));
    }

}