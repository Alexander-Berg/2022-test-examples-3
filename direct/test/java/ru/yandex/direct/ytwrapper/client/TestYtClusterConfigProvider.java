package ru.yandex.direct.ytwrapper.client;

import ru.yandex.direct.ytwrapper.model.YtCluster;

import static com.google.common.base.Preconditions.checkState;

public class TestYtClusterConfigProvider implements YtClusterConfigProvider {
    @Override
    public YtClusterConfig get(YtCluster cluster) {
        checkState(cluster == YtCluster.YT_LOCAL);

        String ytProxy = System.getenv("YT_PROXY");

        return new YtClusterConfig() {
            @Override
            public String getProxy() {
                return ytProxy;
            }

            @Override
            public String getToken() {
                return "anyToken";
            }

            @Override
            public String getYqlToken() {
                return "anyToken";
            }

            @Override
            public String getHome() {
                return "//home/direct";
            }

            @Override
            public String getUser() {
                return "root";
            }

            @Override
            public YtCluster getCluster() {
                return YtCluster.YT_LOCAL;
            }
        };
    }
}
