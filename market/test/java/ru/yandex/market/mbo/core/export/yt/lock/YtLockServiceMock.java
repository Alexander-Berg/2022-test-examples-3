package ru.yandex.market.mbo.core.export.yt.lock;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.transactions.Transaction;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

/**
 * @author amaslak
 */
public class YtLockServiceMock implements YtLockService {

    private final YPath base = YPath.simple("//tmp/mbo/locks");

    private final ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T doWithLock(YPath path, BiFunction<Transaction, YPath, T> job) {
        final Lock lock = locks.computeIfAbsent(path.toString(), (name) -> new ReentrantLock());
        boolean locked = lock.tryLock();
        if (!locked) {
            throw new RuntimeException("Failed to aquire lock for " + path);
        }
        try {
            return job.apply(null, path);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <T> T doWithLock(String lockName, BiFunction<Transaction, YPath, T> job) {
        YPath path = base.child(lockName);
        return doWithLock(path, job);
    }
}
