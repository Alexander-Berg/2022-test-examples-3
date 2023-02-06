package ru.yandex.market.vendors.analytics.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.market.vendors.analytics.platform.yt.YtMetaTree;
import ru.yandex.market.vendors.analytics.platform.yt.YtMetaTreeConfig;

import static org.mockito.Mockito.mock;

@Configuration
public class YtMetaTreeMockConfig {

    @Bean
    public YtMetaTreeConfig ytMetaTreeConfig() {
        return mock(YtMetaTreeConfig.class);
    }

    @Bean
    public YtMetaTree ytMetaTree() {
        return mock(YtMetaTree.class);
    }
}

