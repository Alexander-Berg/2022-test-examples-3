package ru.yandex.market.mcrm.http;

import java.util.List;

import io.netty.handler.codec.http.HttpMethod;

/**
 * @author apershukov
 */
public class HttpRequest {

    private final String basicUrl;
    private final Http http;

    private HttpRequest(String basicUrl, Http http) {
        this.basicUrl = basicUrl;
        this.http = http;
    }

    public static HttpRequest get(Service service, String url) {
        return get(service.getBaseUrl() + url);
    }

    public static HttpRequest get(String url) {
        return new HttpRequest(url, Http.get());
    }

    public static HttpRequest post(String url) {
        return new HttpRequest(url, Http.post());
    }

    public HttpRequest param(String name, String value) {
        http.queryParameter(name, value);
        return this;
    }

    public String getUrl() {
        return basicUrl;
    }

    public HttpMethod getMethod() {
        return http.getMethod();
    }

    public List<Http.NamedValue> getParams() {
        return http.getQueryParameters();
    }
}
