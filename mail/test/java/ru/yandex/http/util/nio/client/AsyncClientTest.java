package ru.yandex.http.util.nio.client;

public class AsyncClientTest extends AsyncClientTestBase {
    @Override
    public boolean https() {
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

