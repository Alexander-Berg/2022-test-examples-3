package ru.yandex.http.server.async;

public class BaseAsyncServerTest extends BaseAsyncServerTestBase {
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

