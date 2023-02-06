package ru.yandex.chemodan.util.test;

import org.apache.http.client.config.RequestConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.chemodan.boot.value.OverridableValuePrefix;
import ru.yandex.chemodan.mpfs.MpfsClientContextConfiguration;
import ru.yandex.chemodan.util.http.HttpClientConfigurator;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;

@ContextConfiguration
public class MpfsMockConfiguration {

    @Bean
    @OverridableValuePrefix("mpfs")
    @MpfsClientContextConfiguration.Mpfs
    public HttpClientConfigurator mpfsHttpClientConfigurator(HttpRecorderRule rule) {
        return new HttpClientConfigurator() {
            @Override
            public ApacheHttpClientUtils.Builder createBuilder() {
                return super.createBuilder().withRequestConfig(RequestConfig.custom().setProxy(rule.getHost()).build())
                        .withRoutePlanner(new HttpRecorderRule.CustomHttpsProxyRoutePlanner())
                        .withRequestRetry();
            }
        };
    }

    @Bean
    @OverridableValuePrefix("mpfs_long")
    @MpfsClientContextConfiguration.Mpfs
    public HttpClientConfigurator mpfsLongHttpClientConfigurator(HttpRecorderRule rule) {
        return new HttpClientConfigurator() {
            @Override
            public ApacheHttpClientUtils.Builder createBuilder() {
                return super.createBuilder().withRequestConfig(RequestConfig.custom().setProxy(rule.getHost()).build())
                        .withRoutePlanner(new HttpRecorderRule.CustomHttpsProxyRoutePlanner())
                        .withRequestRetry();
            }
        };
    }

    @Bean
    @OverridableValuePrefix("djfs")
    @Qualifier("djfs")
    @Primary
    public HttpClientConfigurator djfsHttpClientConfigurator(HttpRecorderRule rule) {
        return new HttpClientConfigurator() {
            @Override
            public ApacheHttpClientUtils.Builder createBuilder() {
                return super.createBuilder().withRequestConfig(RequestConfig.custom().setProxy(rule.getHost()).build())
                        .withRoutePlanner(new HttpRecorderRule.CustomHttpsProxyRoutePlanner())
                        .withRequestRetry();
            }
        };
    }
}
