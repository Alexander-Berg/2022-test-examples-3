package ru.yandex.direct.redislock;

public class StubDistributedLock implements DistributedLock {
    private boolean locked;

    @Override
    public boolean lock() throws DistributedLockException {
        locked = true;
        return true;
    }

    @Override
    public boolean tryLock() throws DistributedLockException {
        locked = true;
        return true;
    }

    @Override
    public boolean unlock() throws DistributedLockException {
        locked = false;
        return true;
    }

    @Override
    public void unlockByEntry() throws DistributedLockException {
    }

    @Override
    public boolean isLocked() {
        return locked;
    }
}
