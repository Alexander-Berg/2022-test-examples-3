package ru.yandex.market.failover.ping;

import ru.yandex.market.common.ping.PingChecker;

public abstract class PingerCheckTestCase<T extends PingChecker> {
    protected T pingerCheck;

    public abstract void testOk();
}
