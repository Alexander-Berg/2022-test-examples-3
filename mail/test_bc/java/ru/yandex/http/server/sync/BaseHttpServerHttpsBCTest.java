package ru.yandex.http.server.sync;

public class BaseHttpServerHttpsBCTest extends BaseHttpServerTestBase {
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

