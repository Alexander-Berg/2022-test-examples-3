package ru.yandex.autotests.market.stat.requests;

import java.io.InputStream;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by entarrion on 22.11.16.
 */
public abstract class AbstractLightweightRequest<T extends AbstractLightweightRequest> implements LightweightRequest {
    protected Method method;
    protected Integer timeout;
    protected Scheme scheme;
    protected String userInfo;
    protected String host;
    protected Integer port;
    protected String path;
    protected String fragment;
    protected List<HttpCookie> cookies;
    protected List<RequestParam> headers;
    protected List<RequestParam> params;
    protected Supplier<InputStream> body;
    protected boolean ignoreSSL;

    public AbstractLightweightRequest() {
        method = Method.GET;
        timeout = 60 * 1000;
        scheme = Scheme.HTTP;
        userInfo = null;
        host = "localhost";
        port = null;
        path = null;
        fragment = null;
        params = new ArrayList<>();
        headers = new ArrayList<>();
        cookies = new ArrayList<>();
        body = null;
        ignoreSSL = false;
    }

    public AbstractLightweightRequest(String url) {
        this();
        try {
            URL parseUrl = new URL(url);
            scheme = parseUrl.getProtocol().toLowerCase().trim().equals(Scheme.HTTPS.name().toLowerCase()) ?
                    Scheme.HTTPS : Scheme.HTTP;
            userInfo = parseUrl.getUserInfo();
            host = parseUrl.getHost();
            port = parseUrl.getPort();
            path = parseUrl.getPath();
            params = RequestUtils.parseQueryParams(parseUrl.getQuery());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public AbstractLightweightRequest(AbstractLightweightRequest<T> other) {
        method = other.getMethod();
        timeout = other.getTimeout();
        scheme = other.getScheme();
        userInfo = other.getUserInfo();
        host = other.getHost();
        port = other.getPort();
        path = other.getPath();
        fragment = other.getFragment();
        cookies = other.getCookies().stream().map(it -> (HttpCookie) it.clone()).collect(Collectors.toList());
        headers = other.getHeaders().stream().map(RequestParam::new).collect(Collectors.toList());
        params = other.getParams().stream().map(RequestParam::new).collect(Collectors.toList());
        body = other.getBody();
        ignoreSSL = other.isIgnoreSSL();
    }

    protected abstract T copy(T original);

    protected T copyAndModify(Consumer<T> modifier) {
        T result = copy((T) this);
        modifier.accept(result);
        return result;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Integer getTimeout() {
        return timeout;
    }

    @Override
    public Scheme getScheme() {
        return scheme;
    }

    @Override
    public String getUserInfo() {
        return userInfo;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public Integer getPort() {
        return port;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getFragment() {
        return fragment;
    }

    @Override
    public List<HttpCookie> getCookies() {
        return cookies;
    }

    @Override
    public List<RequestParam> getHeaders() {
        return headers;
    }

    @Override
    public List<RequestParam> getParams() {
        return params;
    }

    @Override
    public Supplier<InputStream> getBody() {
        return body;
    }

    @Override
    public boolean isIgnoreSSL() {
        return ignoreSSL;
    }

    @Override
    public LightweightResponse send() {
        return SenderUtils.send(this);
    }

    @Override
    public LightweightRequest withMethod(Method method) {
        return copyAndModify(t -> t.method = method);
    }

    @Override
    public LightweightRequest withScheme(Scheme scheme) {
        return copyAndModify(t -> t.scheme = scheme);
    }

    @Override
    public LightweightRequest withUserInfo(String userInfo) {
        return copyAndModify(t -> t.userInfo = userInfo);
    }

    @Override
    public LightweightRequest withHost(String host) {
        return copyAndModify(t -> t.host = host);
    }

    @Override
    public LightweightRequest withPort(Integer port) {
        return copyAndModify(t -> t.port = port);
    }

    @Override
    public LightweightRequest withPath(String path) {
        return copyAndModify(t -> t.path = path);
    }

    @Override
    public LightweightRequest withFragment(String fragment) {
        return copyAndModify(t -> t.fragment = fragment);
    }

    @Override
    public LightweightRequest withReadTimeout(Integer timeout) {
        return copyAndModify(t -> t.timeout = timeout);
    }

    @Override
    public LightweightRequest withCookie(HttpCookie cookie) {
        return Objects.isNull(cookie) ? this : copyAndModify(t -> t.getCookies().add(cookie));
    }

    @Override
    public LightweightRequest withCookie(String name, String value) {
        return Objects.isNull(name) ? this : withCookie(new HttpCookie(name, value));
    }

    @Override
    public LightweightRequest withParam(RequestParam param) {
        return Objects.isNull(param) || Objects.isNull(param.getKey()) ? this : copyAndModify(t -> t.getParams().add(param));
    }

    @Override
    public LightweightRequest withHeader(RequestParam header) {
        return Objects.isNull(header) || Objects.isNull(header.getKey()) || Objects.isNull(header.getValue()) ? this : copyAndModify(t -> t.getHeaders().add(header));
    }

    @Override
    public LightweightRequest withBody(InputStream body) {
        return copyAndModify(t -> t.body = () -> body);
    }

    @Override
    public LightweightRequest withIgnoreSSL(boolean isIgnoreSSL) {
        return copyAndModify(t -> t.ignoreSSL = ignoreSSL);
    }

    @Override
    public String toString() {
        try {
            return asUrl().toString();
        } catch (IllegalStateException e) {
            return getClass().getName();
        }
    }
}
