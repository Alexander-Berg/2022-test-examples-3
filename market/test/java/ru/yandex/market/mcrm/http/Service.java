package ru.yandex.market.mcrm.http;

/**
 * @author apershukov
 */
public enum Service {

    CRYPTA_API("external.crypta.url", "https://api.crypta.yandex.net");

    private final String urlParamName;
    private final String baseUrl;

    Service(String urlParamName, String baseUrl) {
        this.urlParamName = urlParamName;
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getUrlParamName() {
        return urlParamName;
    }
}
