package ru.yandex.market.checkout.stub.lock;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.checkout.checkouter.lock.Lock;
import ru.yandex.market.checkout.checkouter.lock.RetryableLockException;
import ru.yandex.market.checkout.storage.EntityGroup;

public class LockStub implements Lock {
    private static final Logger LOG = LoggerFactory.getLogger(LockStub.class);

    private final Set<String> storage;
    private final String path;

    public LockStub(Set<String> storage, EntityGroup<?> group) {
        this.storage = storage;
        this.path = group.name() + group.stringKey();
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void acquire() throws RetryableLockException {
        synchronized (storage) {
            LOG.info("acquireLock for {}", path);
            if (storage.contains(path)) {
                LOG.info("already locked");
                throw new RetryableLockException(null);
            }
            storage.add(path);

            LOG.info("successfully locked");
        }
    }

    @Override
    public void release() {
        storage.remove(path);
    }

    @Override
    public String toString() {
        return "LockStub{" +
                "path='" + path + '\'' +
                '}';
    }
}
