package ru.yandex.autotests.directapi.test.error53.impl;

import ru.yandex.autotests.directapi.HazelcastLock;
import ru.yandex.autotests.directapi.HazelcastLockNames;

public final class Error53Lock extends HazelcastLock {
    public static final Error53Lock INSTANCE = new Error53Lock();

    private Error53Lock() {
        super(HazelcastLockNames.ERROR53);
    }
}
