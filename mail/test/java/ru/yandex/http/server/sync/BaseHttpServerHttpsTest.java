package ru.yandex.http.server.sync;

public class BaseHttpServerHttpsTest extends BaseHttpServerTestBase {
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

