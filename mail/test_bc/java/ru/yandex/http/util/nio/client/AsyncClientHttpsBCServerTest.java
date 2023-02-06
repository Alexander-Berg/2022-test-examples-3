package ru.yandex.http.util.nio.client;

public class AsyncClientHttpsBCServerTest extends AsyncClientTestBase {
    @Override
    public boolean https() {
        return true;
    }

    @Override
    public boolean serverBc() {
        return true;
    }

    @Override
    public boolean clientBc() {
        return false;
    }
}

