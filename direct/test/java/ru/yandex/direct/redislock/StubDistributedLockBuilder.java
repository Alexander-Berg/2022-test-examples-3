package ru.yandex.direct.redislock;

public class StubDistributedLockBuilder implements DistributedLockBuilder {
    @Override
    public DistributedLock createLock(String lockKey) {
        return new StubDistributedLock();
    }

    @Override
    public DistributedLock createLock(String lockKey, int maxLocks) {
        return new StubDistributedLock();
    }
}
