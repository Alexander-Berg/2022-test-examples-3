package ru.yandex.autotests.market.stat.requests;

import java.io.InputStream;
import java.net.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by entarrion on 22.11.16.
 */
public interface RequestData {
    Method getMethod();

    Integer getTimeout();

    Scheme getScheme();

    String getUserInfo();

    String getHost();

    Integer getPort();

    String getPath();

    String getFragment();

    List<HttpCookie> getCookies();

    List<RequestParam> getHeaders();

    List<RequestParam> getParams();

    Supplier<InputStream> getBody();

    boolean isIgnoreSSL();

    default URL asUrl() {
        try {
            String tScheme = Objects.nonNull(getScheme()) ? getScheme().name().toLowerCase() : Scheme.HTTP.name().toLowerCase();
            if (Objects.isNull(getHost()) || getHost().trim().isEmpty()) {
                throw new IllegalStateException("Host must not be empty and be null");
            } else if (getHost().contains("/") || getHost().contains(":")) {
                throw new IllegalStateException("Wrong host [" + getHost() + "]");
            }
            String tHost = getHost().trim();
            Integer tPort = (Objects.isNull(getPort()) || getPort() < 0) ? -1 : getPort();
            String tPath = Objects.nonNull(getPath()) ? (getPath().startsWith("/") ? getPath() : "/" + getPath()) : "/";
            tPath = tPath.contains("?") ? tPath.substring(0, tPath.indexOf("?")) : tPath;
            String tQuery = RequestUtils.formatQueryParams(
                    getParams().stream().filter(RequestParam::isUrlParam).collect(Collectors.toList()));
            tQuery = tQuery.trim().isEmpty() ? null : tQuery.trim();
            return new URL(new URI(tScheme, getUserInfo(), tHost, tPort, tPath, tQuery, getFragment()).toString());
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    enum Method {GET, PUT, POST, DELETE, HEAD, TRACE, OPTIONS, PATCH}

    enum Scheme {HTTP, HTTPS}
}