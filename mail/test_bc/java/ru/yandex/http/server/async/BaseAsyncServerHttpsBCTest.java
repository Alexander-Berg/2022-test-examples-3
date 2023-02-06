package ru.yandex.http.server.async;

public class BaseAsyncServerHttpsBCTest extends BaseAsyncServerTestBase {
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
        return true;
    }
}

