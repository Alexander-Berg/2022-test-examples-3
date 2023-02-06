package ru.yandex.http.server.sync;

public class BaseHttpServerLimiterTest extends BaseHttpServerLimiterTestBase {
    @Override
    protected boolean https() {
        return false;
    }

    @Override
    protected boolean serverBc() {
        return false;
    }

    @Override
    protected boolean clientBc() {
        return false;
    }
}

