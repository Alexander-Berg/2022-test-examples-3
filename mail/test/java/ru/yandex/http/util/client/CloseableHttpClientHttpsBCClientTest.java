package ru.yandex.http.util.client;

public class CloseableHttpClientHttpsBCClientTest
    extends CloseableHttpClientTestBase
{
    @Override
    protected boolean https() {
        return true;
    }

    @Override
    public boolean serverBc() {
        return false;
    }

    @Override
    public boolean clientBc() {
        return true;
    }
}

