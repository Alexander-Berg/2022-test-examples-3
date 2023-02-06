package ru.yandex.market.jmf.http.test.impl;

import java.util.List;

import io.netty.handler.codec.http.HttpMethod;

import ru.yandex.market.jmf.http.Http;

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

    public static HttpRequest get(String url) {
        return new HttpRequest(url, Http.get());
    }

    public static HttpRequest post(String url) {
        return new HttpRequest(url, Http.post());
    }

    public static HttpRequest patch(String url) {
        return new HttpRequest(url, Http.patch());
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
