package ru.yandex.market.checkout.stub.lock;

import java.util.HashSet;
import java.util.Set;

import ru.yandex.market.checkout.checkouter.lock.Lock;
import ru.yandex.market.checkout.checkouter.lock.LockStorage;
import ru.yandex.market.checkout.storage.EntityGroup;

public class LockStorageStub implements LockStorage {
    private final Set<String> storage = new HashSet<>();

    @Override
    public Lock initLock(EntityGroup<?> group) {
        return new LockStub(storage, group);
    }
}
