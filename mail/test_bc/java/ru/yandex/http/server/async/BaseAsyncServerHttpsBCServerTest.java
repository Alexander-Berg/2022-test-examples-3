package ru.yandex.http.server.async;

public class BaseAsyncServerHttpsBCServerTest extends BaseAsyncServerTestBase {
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

