package ru.yandex.market.mcrm.http;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import org.asynchttpclient.Response;
import org.asynchttpclient.uri.Uri;

/**
 * @author apershukov
 */
public class ResponseMock implements Response {

    private final int statusCode;
    private final byte[] body;

    public ResponseMock(byte[] body) {
        this(200, body);
    }

    public ResponseMock(int statusCode, byte[] body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getStatusText() {
        return null;
    }

    @Override
    public byte[] getResponseBodyAsBytes() {
        return body;
    }

    @Override
    public ByteBuffer getResponseBodyAsByteBuffer() {
        return ByteBuffer.wrap(body);
    }

    @Override
    public InputStream getResponseBodyAsStream() {
        return new ByteArrayInputStream(body);
    }

    @Override
    public String getResponseBody(Charset charset) {
        return new String(body, charset);
    }

    @Override
    public String getResponseBody() {
        return getResponseBody(StandardCharsets.UTF_8);
    }

    @Override
    public Uri getUri() {
        return null;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public HttpHeaders getHeaders() {
        return null;
    }

    @Override
    public boolean isRedirected() {
        return false;
    }

    @Override
    public String getHeader(CharSequence name) {
        return null;
    }

    @Override
    public List<String> getHeaders(CharSequence name) {
        return null;
    }

    @Override
    public List<Cookie> getCookies() {
        return null;
    }

    @Override
    public boolean hasResponseStatus() {
        return false;
    }

    @Override
    public boolean hasResponseHeaders() {
        return false;
    }

    @Override
    public boolean hasResponseBody() {
        return false;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public SocketAddress getLocalAddress() {
        return null;
    }
}
