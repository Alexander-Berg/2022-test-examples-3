package ru.yandex.autotests.direct.cmd.steps.base;

import org.apache.http.impl.client.CloseableHttpClient;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.httpclientlite.utils.client.HttpClientBuilderUtils;
import ru.yandex.autotests.httpclientlite.utils.client.HttpClientConfig;
import ru.yandex.autotests.httpclientlite.context.ConnectionContext;

public class DirectStepsContext {

    private DirectTestRunProperties properties;
    private ConnectionContext connectionContext;
    private HttpClientConfig httpClientConfig = new HttpClientConfig();
    private AuthConfig authConfig = new AuthConfig();
    private CloseableHttpClient httpClient;

    public DirectStepsContext() {
    }

    public DirectStepsContext useConnectionContext(ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
        return this;
    }

    public DirectTestRunProperties getProperties() {
        return properties;
    }

    public DirectStepsContext withProperties(DirectTestRunProperties properties) {
        this.properties = properties;
        return this;
    }

    public DirectStepsContext useHttpClientConfig(HttpClientConfig httpClientConfig) {
        this.httpClientConfig = httpClientConfig;
        return this;
    }

    public DirectStepsContext useAuthConfig(AuthConfig authConfig) {
        this.authConfig = authConfig;
        return this;
    }

    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    public HttpClientConfig getHttpClientConfig() {
        return httpClientConfig;
    }

    public CloseableHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = HttpClientBuilderUtils.defaultTestConfiguration(httpClientConfig).build();
        }
        return httpClient;
    }

    public AuthConfig getAuthConfig() {
        return authConfig;
    }

    public String getBaseUrl() {
        return connectionContext.getScheme() + "://" + connectionContext.getHost();
    }
}
