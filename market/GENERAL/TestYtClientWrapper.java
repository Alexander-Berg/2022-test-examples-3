package ru.yandex.market.ir.yt.util.tables;

import ru.yandex.market.mbo.yt.TestYt;
import ru.yandex.market.mbo.ytclient.TestApiServiceClient;
import ru.yandex.yt.ytclient.proxy.ApiServiceClient;

public class TestYtClientWrapper extends YtClientWrapper {

    private final ApiServiceClient ytClient;

    public TestYtClientWrapper(TestYt testYt) {
        this(new TestApiServiceClient(testYt));
    }

    public TestYtClientWrapper(ApiServiceClient ytClient) {
        this.ytClient = ytClient;
    }

    @Override
    public synchronized ApiServiceClient getClient() {
        return ytClient;
    }
}
