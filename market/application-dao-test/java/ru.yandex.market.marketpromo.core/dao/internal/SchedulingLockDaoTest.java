package ru.yandex.market.marketpromo.core.dao.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.scheduling.model.LockingSession;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class SchedulingLockDaoTest extends ServiceTestBase {

    private static final String NAME = "some name";

    @Autowired
    private SchedulingLockDao schedulingLockDao;

    @Test
    void shouldSaveLockOnFirstAcquiring() {
        LockingSession session = schedulingLockDao.acquireLockSession(NAME, clock.dateTime().plusDays(3));

        assertThat(session, notNullValue());
    }

    @Test
    void shouldBeNullOnSecondAcquiring() {
        LockingSession session = schedulingLockDao.acquireLockSession(NAME, clock.dateTime().plusDays(3));

        assertThat(session, notNullValue());

        session = schedulingLockDao.acquireLockSession(NAME, clock.dateTime().plusDays(3));

        assertThat(session, nullValue());
    }

    @Test
    void shouldGetAcquiredLockingSession() {
        LockingSession session = schedulingLockDao.acquireLockSession(NAME, clock.dateTime().plusDays(3));

        assertThat(session, notNullValue());

        assertThat(schedulingLockDao.get(NAME).isEmpty(), is(false));
    }

    @Test
    void shouldDestroyAcquiredSession() {
        LockingSession session = schedulingLockDao.acquireLockSession(NAME, clock.dateTime().plusDays(3));

        assertThat(session, notNullValue());

        schedulingLockDao.destroySession(session);

        assertThat(schedulingLockDao.get(session.getId()).isEmpty(), is(true));
    }
}
