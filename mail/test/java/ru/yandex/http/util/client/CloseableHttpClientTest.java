package ru.yandex.http.util.client;

public class CloseableHttpClientTest extends CloseableHttpClientTestBase {
    @Override
    protected boolean https() {
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

