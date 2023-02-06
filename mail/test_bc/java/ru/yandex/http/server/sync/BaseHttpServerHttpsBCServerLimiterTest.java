package ru.yandex.http.server.sync;

public class BaseHttpServerHttpsBCServerLimiterTest
    extends BaseHttpServerLimiterTestBase
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

