package ru.yandex.market.yql_test.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import ru.yandex.market.yql_test.service.YqlProxyServerService;

@ConfigurationProperties
public class TestYqlDataSourcePropertiesConfig {

    private final String token;
    private final String cluster;
    private final YqlProxyServerService proxyServerService;

    public TestYqlDataSourcePropertiesConfig(
            @Value("${yql.datasource.token}") String token,
            @Value("${yql.datasource.cluster}") String cluster,
            YqlProxyServerService proxyServerService
    ) {
        this.token = token;
        this.cluster = cluster;
        this.proxyServerService = proxyServerService;
    }

    public String getUrl() {
        if (!proxyServerService.isStarted()) {
            throw new IllegalStateException("YqlProxyServer is not started");
        }
        return String.format("jdbc:yql://localhost:%d/%s", proxyServerService.getProxyPort(), cluster);
    }

    public String getToken() {
        return token;
    }
}
