package ru.yandex.chemodan.app.docviewer.dao.ydb;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.function.Function;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.chemodan.ydb.dao.pojo.YdbTestUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class YdbCleanerTaskDaoTest {
    @Test
    public void writeReadTest() {
        doWithEmptyTable(dao -> {
            String id = "cleaning id";
            String hostA = "hostA";
            String hostB = "hostB";

            Assert.isTrue(dao.saveAttempt(id, Instant.now(), hostA));
            Assert.isFalse(dao.saveAttempt(id, Instant.now(), hostB));
            dao.deleteById(id);
            Assert.isTrue(dao.saveAttempt(id, Instant.now(), hostB));
            dao.deleteById(id);

            AtomicInteger obtainedLock = new AtomicInteger();
            AtomicInteger failedLock = new AtomicInteger();

            Function<String, Boolean> tryToGetLock = host -> {
                boolean obtained = dao.saveAttempt(id, Instant.now(), host);
                if (obtained) {
                    obtainedLock.incrementAndGet();
                } else {
                    failedLock.incrementAndGet();
                }
                return obtained;
            };

            CompletableFuture.allOf(
                    CompletableFuture.supplyAsync(() -> tryToGetLock.apply(hostA)),
                    CompletableFuture.supplyAsync(() -> tryToGetLock.apply(hostB))
            ).join();

            Assert.equals(1, obtainedLock.get(), "not enough successful successful attempts");
            Assert.equals(1, failedLock.get(), "not enough successful failed attempts");
        });
    }

    private void doWithEmptyTable(Function1V<YdbCleanerTaskDao> action) {
        YdbTestUtils.doWithTable(YdbCleanerTaskDao::new, dao -> action.apply((YdbCleanerTaskDao) dao));
    }
}
