package ru.yandex.autotests.directapi.test.units;

import ru.yandex.autotests.directapi.HazelcastLock;
import ru.yandex.autotests.directapi.HazelcastLockNames;

public final class UnitsLock extends HazelcastLock {
    public static final UnitsLock INSTANCE = new UnitsLock();

    private UnitsLock() {
        super(HazelcastLockNames.UNITS);
    }
}
