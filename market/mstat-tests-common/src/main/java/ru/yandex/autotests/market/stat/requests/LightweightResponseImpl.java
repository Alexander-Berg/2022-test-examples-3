package ru.yandex.autotests.market.stat.requests;

import java.io.InputStream;
import java.net.HttpCookie;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by entarrion on 05.05.16.
 */
public class LightweightResponseImpl implements LightweightResponse {
    private static final Pattern CHARSET_REGEXP = Pattern.compile("charset=([^\\s]+)");
    private InputStream body;
    private List<RequestParam> headers;
    private List<HttpCookie> cookies;
    private String contentType;
    private int statusCode;
    private String charset;

    public LightweightResponseImpl(InputStream body, List<RequestParam> headers, List<HttpCookie> cookies, String contentType, int statusCode) {
        this.body = body;
        this.headers = headers;
        this.cookies = cookies;
        this.contentType = contentType;
        this.statusCode = statusCode;
        Matcher matcher = CHARSET_REGEXP.matcher(Objects.nonNull(contentType) ? contentType : "");
        this.charset = matcher.find() ? matcher.group(1) : "utf-8";
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public List<RequestParam> getHeaders() {
        return headers;
    }

    @Override
    public List<HttpCookie> getCookies() {
        return cookies;
    }

    @Override
    public InputStream bodyAsInputStream() {
        return body;
    }

    @Override
    public String getCharset() {
        return charset;
    }
}