package ru.yandex.replenishment.autoorder.integration.test.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "yql.datasource")
public class YqlDataSourcePropertiesConfig {

    private String url;
    private String token;
    private YtCluster cluster;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public YtCluster getCluster() {
        return cluster;
    }

    public void setCluster(YtCluster cluster) {
        this.cluster = cluster;
    }
}
