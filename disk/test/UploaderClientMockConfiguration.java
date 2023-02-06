package ru.yandex.chemodan.util.test;

import java.util.function.UnaryOperator;

import org.apache.http.client.config.RequestConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;

@ContextConfiguration
public class UploaderClientMockConfiguration {

    @Bean
    @Qualifier("uploaderClient")
    public UnaryOperator<ApacheHttpClientUtils.Builder> uploaderClientProxyInterceptor(HttpRecorderRule rule) {
        return builder -> builder.withRequestConfig(RequestConfig.custom().setProxy(rule.getHost()).build()).withRequestRetry();
    }

}
