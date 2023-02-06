package ru.yandex.http.util.client;

public class CloseableHttpClientHttpsBCServerTest
    extends CloseableHttpClientTestBase
{
    @Override
    protected boolean https() {
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

