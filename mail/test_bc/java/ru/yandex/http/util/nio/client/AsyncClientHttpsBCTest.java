package ru.yandex.http.util.nio.client;

public class AsyncClientHttpsBCTest extends AsyncClientTestBase {
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
        return true;
    }
}

