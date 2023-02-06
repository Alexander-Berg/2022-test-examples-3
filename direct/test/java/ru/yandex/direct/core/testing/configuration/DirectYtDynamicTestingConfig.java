package ru.yandex.direct.core.testing.configuration;

import java.util.Collection;
import java.util.List;

import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.ytcomponents.config.DirectYtDynamicConfig;
import ru.yandex.direct.ytwrapper.model.YtCluster;

public class DirectYtDynamicTestingConfig extends DirectYtDynamicConfig {
    public DirectYtDynamicTestingConfig(DirectConfig config) {
        super(config);
    }

    @Override
    protected Collection<YtCluster> getClustersByKey(String confKey) {
        return List.of(YtCluster.YT_LOCAL);
    }
}
