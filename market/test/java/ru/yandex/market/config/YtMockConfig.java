package ru.yandex.market.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.billing.util.yt.YtCluster;
import ru.yandex.market.billing.util.yt.YtTemplate;

/**
 * Конфиг для мока ытя
 */
@Configuration
public class YtMockConfig {

    @Bean(name = {"yt", "hahnYt"})
    public Yt yt() {
        return Mockito.mock(Yt.class);
    }

    @Bean
    public YtCluster ytClusterMock() {
        return Mockito.mock(YtCluster.class);
    }

    @Bean
    public YtTemplate ytTemplate() {
        return new YtTemplate(ytClusterMock());
    }
}
