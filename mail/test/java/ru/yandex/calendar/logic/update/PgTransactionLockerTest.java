package ru.yandex.calendar.logic.update;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.concurrent.CountDownLatches;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.thread.ThreadLocalTimeout;
import ru.yandex.misc.thread.ThreadUtils;

/**
 * @author dbrylev
 */
public class PgTransactionLockerTest extends AbstractConfTest {

    @Autowired
    private LockTransactionManager lockTransactionManager;

    @Test(timeout = 5000)
    public void timeout() {
        ListF<LockResource> resources = Cf.list(LockResource.todoUser(PassportUid.cons(1)));

        CountDownLatch lockedLatch = new CountDownLatch(1);

        CompletableFuture.runAsync(() ->
                lockTransactionManager.lockAndDoInTransaction(resources, () -> {
                    lockedLatch.countDown();
                    ThreadUtils.sleep(Duration.standardSeconds(5));
                }));

        CountDownLatches.await(lockedLatch);

        ThreadLocalTimeout.Handle tlt = ThreadLocalTimeout.push(Duration.standardSeconds(2));
        try {
            Assert.assertThrows(
                    () -> lockTransactionManager.lockAndDoInTransaction(resources, () -> null),
                    DataAccessResourceFailureException.class);

        } finally {
            tlt.popSafely();
        }
    }

    private boolean enabled;

    @Before
    public void setup() {
        enabled = ThreadLocalTimeout.isEnabled();
        ThreadLocalTimeout.setEnabled(true);
    }

    @After
    public void teardown() {
        ThreadLocalTimeout.setEnabled(enabled);
    }
}
