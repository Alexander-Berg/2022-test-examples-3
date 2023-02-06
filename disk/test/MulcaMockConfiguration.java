package ru.yandex.chemodan.util.test;

import java.util.function.UnaryOperator;

import org.apache.http.client.config.RequestConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.chemodan.mulca.MulcaClientContextConfiguration.Mulca;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;

@ContextConfiguration
public class MulcaMockConfiguration {

    @Bean
    @Mulca
    public UnaryOperator<ApacheHttpClientUtils.Builder> mulcaProxyInterceptor(HttpRecorderRule rule) {
        return builder -> builder.withRequestConfig(RequestConfig.custom().setProxy(rule.getHost()).build());
    }

}
