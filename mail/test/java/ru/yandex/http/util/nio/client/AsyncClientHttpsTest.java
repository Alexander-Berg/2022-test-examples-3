package ru.yandex.http.util.nio.client;

public class AsyncClientHttpsTest extends AsyncClientTestBase {
    @Override
    public boolean https() {
        return true;
    }

    @Override
    public boolean serverBc() {
        return false;
    }

    @Override
    public boolean clientBc() {
        return false;
    }
}

