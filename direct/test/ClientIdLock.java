package ru.yandex.autotests.directapi.test;

import ru.yandex.autotests.directapi.HazelcastLock;

public final class ClientIdLock extends HazelcastLock {
    public ClientIdLock(String lockName) {
        super(lockName);
    }
}
