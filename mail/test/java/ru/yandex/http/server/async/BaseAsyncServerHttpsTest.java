package ru.yandex.http.server.async;

public class BaseAsyncServerHttpsTest extends BaseAsyncServerTestBase {
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
        return false;
    }
}

