package ru.yandex.autotests.market.stat.requests;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpCookie;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by entarrion on 05.05.16.
 */
public interface LightweightRequest extends RequestData, RequestSender {
    LightweightRequest withMethod(final Method method);

    LightweightRequest withScheme(final Scheme scheme);

    LightweightRequest withUserInfo(final String userInfo);

    LightweightRequest withHost(final String host);

    LightweightRequest withPort(final Integer port);

    LightweightRequest withPath(final String path);

    LightweightRequest withFragment(final String fragment);

    LightweightRequest withReadTimeout(final Integer timeout);

    LightweightRequest withCookie(HttpCookie cookie);

    LightweightRequest withCookie(String name, String value);

    LightweightRequest withParam(RequestParam param);

    default LightweightRequest withParam(String key, String value, boolean isUrlParam) {
        return withParam(new RequestParam(key, value, isUrlParam));
    }

    default LightweightRequest withUrlParam(String key, String value) {
        return withParam(key, value, true);
    }

    default LightweightRequest withNoUrlParam(String key, String value) {
        return withParam(key, value, false);
    }

    default LightweightRequest withParams(List<RequestParam> params) {
        LightweightRequest result = this;
        for (RequestParam param : params) {
            result = withParam(param);
        }
        return result;
    }

    default LightweightRequest withQuery(String query) {
        return withParams(RequestUtils.parseQueryParams(query));
    }

    LightweightRequest withHeader(RequestParam header);

    default LightweightRequest withHeader(String headerName, String headerValue) {
        return withHeader(new RequestParam(headerName, headerValue));
    }

    default LightweightRequest withHeaders(List<RequestParam> headers) {
        LightweightRequest result = this;
        for (RequestParam header : headers) {
            result = result.withHeader(header);
        }
        return result;
    }

    LightweightRequest withBody(final InputStream body);

    default LightweightRequest withBody(final byte[] body) {
        return withBody(new ByteArrayInputStream(body));
    }

    default LightweightRequest withBody(final String body) {
        return withBody(body.getBytes(StandardCharsets.UTF_8));
    }

    LightweightRequest withIgnoreSSL(final boolean isIgnoreSSL);
}
