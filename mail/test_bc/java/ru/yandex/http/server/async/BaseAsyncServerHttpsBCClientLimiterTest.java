package ru.yandex.http.server.async;

public class BaseAsyncServerHttpsBCClientLimiterTest
    extends BaseAsyncServerLimiterTestBase
{
    @Override
    protected boolean https() {
        return true;
    }

    @Override
    protected boolean serverBc() {
        return false;
    }

    @Override
    protected boolean clientBc() {
        return true;
    }
}

