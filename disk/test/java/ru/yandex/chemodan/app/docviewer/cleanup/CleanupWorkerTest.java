package ru.yandex.chemodan.app.docviewer.cleanup;

import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.DocviewerSpringTestBase;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class CleanupWorkerTest extends DocviewerSpringTestBase {

    @Autowired
    private CleanupWorker cleanupWorker;

    @Test
    public void testLock() {
        String lock = "test_lock";
        cleanupWorker.releaseLock(lock);
        Assert.isTrue(cleanupWorker.getLock(lock, new Instant()));
        Assert.isFalse(cleanupWorker.getLock(lock, new Instant()));
        cleanupWorker.releaseLock(lock);
    }
}
