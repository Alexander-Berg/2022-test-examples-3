package ru.yandex.market.jmf.lock.test.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import ru.yandex.market.jmf.lock.impl.LockDao;

@Primary
@Component
@Profile("inMemoryLockService")
public class InMemoryLockDao implements LockDao {
    private final Map<String, LockInfo> locks = new ConcurrentHashMap<>();

    @Override
    public synchronized Set<String> tryAddLocks(Collection<String> keys, String instanceKey) {
        var result = new HashSet<String>();
        for (String key : keys) {
            var lock = locks.computeIfAbsent(key, k -> new LockInfo(new ReentrantLock(), LocalDateTime.now(),
                    instanceKey));
            if (lock.tryLock(instanceKey)) {
                result.add(key);
            }
        }
        return result;
    }

    @Override
    public synchronized Set<String> freeLocks(Collection<String> keys, String instanceKey) {
        var result = new HashSet<String>();
        for (String key : keys) {
            var lock = locks.get(key);
            if (lock.tryUnlock(instanceKey)) {
                result.add(key);
                locks.remove(key);
            }
        }
        return result;
    }

    @Override
    public synchronized void updateLocks(Set<String> keys, String instanceKey) {
        for (String key : keys) {
            locks.computeIfPresent(key, (k, l) -> {
                if (l.instanceKey().equals(instanceKey)) {
                    return new LockInfo(l.lock(), LocalDateTime.now(), instanceKey);
                }
                return l;
            });
        }
    }

    @Override
    public void deleteOrphanedLocks(long orphanedPeriod) {
        for (String key : locks.keySet()) {
            var lock = locks.get(key);
            if (lock.lastActivity().isBefore(LocalDateTime.now().minus(Duration.ofMillis(orphanedPeriod)))) {
                locks.remove(key, lock);
            }
        }
    }

    @Override
    public synchronized Set<String> getRetainedLocks(Collection<String> keys, String instanceKey) {
        var result = new HashSet<String>();
        for (String key : keys) {
            var lock = locks.get(key);
            if (lock.instanceKey().equals(instanceKey)) {
                result.add(key);
            }
        }
        return result;
    }

    private static record LockInfo(
            Lock lock,
            LocalDateTime lastActivity,
            String instanceKey
    ) {
        public synchronized boolean tryLock(String instanceKey) {
            if (!instanceKey.equals(instanceKey())) {
                return false;
            }
            return lock.tryLock();
        }

        public synchronized boolean tryUnlock(String instanceKey) {
            if (!instanceKey.equals(instanceKey())) {
                return false;
            }
            lock.unlock();
            return true;
        }
    }
}
