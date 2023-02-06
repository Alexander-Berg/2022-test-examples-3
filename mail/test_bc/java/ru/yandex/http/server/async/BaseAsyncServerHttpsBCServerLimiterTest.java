package ru.yandex.http.server.async;

public class BaseAsyncServerHttpsBCServerLimiterTest
    extends BaseAsyncServerLimiterTestBase
{
    @Override
    protected boolean https() {
        return true;
    }

    @Override
    protected boolean serverBc() {
        return true;
    }

    @Override
    protected boolean clientBc() {
        return false;
    }
}

