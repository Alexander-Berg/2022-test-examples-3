package ru.yandex.market.checkout.checkouter.test.config.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.checkout.storage.EntityGroup;
import ru.yandex.market.checkout.storage.err.StorageException;
import ru.yandex.market.checkout.storage.impl.LockCallback;
import ru.yandex.market.checkout.storage.impl.LockService;
import ru.yandex.market.checkout.storage.impl.SizeReporter;

public class LocalLockService implements LockService, SizeReporter {

    private static final Logger LOG = LoggerFactory.getLogger(LocalLockService.class);

    private final List<String> entityGroups;

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public LocalLockService(List<String> entityGroups) {
        this.entityGroups = entityGroups;
    }

    @Override
    public <T, K extends Comparable<K>> T lockEntityGroup(EntityGroup<K> entityGroup, long waitTimeout,
                                                          LockCallback<T> callback) throws StorageException,
            TimeoutException {
        if (!entityGroups.contains(entityGroup.name())) {
            throw new IllegalArgumentException("Unsupported entityGroup: " + entityGroup.name());
        }

        String path = buildLockPath(entityGroup);
        ReentrantLock lock = locks.computeIfAbsent(path, p -> new ReentrantLock(true));

        if (waitTimeout == NO_TIMEOUT) {
            return lockEntityGroupNoTimeout(lock, callback);
        } else {
            return lockEntityGroupWithTimeout(path, lock, waitTimeout, callback);
        }
    }

    private String buildLockPath(EntityGroup<?> group) {
        return group.name() + group.stringKey();
    }

    private <T> T lockEntityGroupNoTimeout(ReentrantLock lock, LockCallback<T> callback) throws StorageException {
        lock.lock();
        try {
            return callback.doLocked(null);
        } finally {
            lock.unlock();
        }
    }

    private <T> T lockEntityGroupWithTimeout(String lockPath, ReentrantLock lock, long waitTimeout,
                                             LockCallback<T> callback) throws StorageException, TimeoutException {
        long start = System.currentTimeMillis();
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTimeout, TimeUnit.MILLISECONDS);
            if (acquired) {
                LOG.info("Acquiring lock {} took {} ms", lockPath, System.currentTimeMillis() - start);
                return callback.doLocked(null);
            } else {
                throw new TimeoutException(String.format("Timeout has occurred while waiting for lock '%s': %d ms",
                        lockPath, System.currentTimeMillis() - start));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StorageException("Thread was interrupted");
        } finally {
            long end = System.currentTimeMillis();
            if (acquired) {
                lock.unlock();
                LOG.info("Unlocking lock {} took {} ms", lockPath, System.currentTimeMillis() - end);
            }
        }
    }

    @Override
    public Map<String, Integer> getSizeByEntityGroup() {
        return entityGroups.stream()
                .collect(Collectors.toMap(Function.identity(), (key) -> locks.size()));

    }

    public void expireUnusedOlderThan() {
        // заглушка для тасок
    }
}
