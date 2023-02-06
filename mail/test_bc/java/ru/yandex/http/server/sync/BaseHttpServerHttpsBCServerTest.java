package ru.yandex.http.server.sync;

public class BaseHttpServerHttpsBCServerTest extends BaseHttpServerTestBase {
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

