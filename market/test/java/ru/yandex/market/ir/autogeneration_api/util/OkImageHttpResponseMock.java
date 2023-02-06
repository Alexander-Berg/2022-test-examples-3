package ru.yandex.market.ir.autogeneration_api.util;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

public class OkImageHttpResponseMock implements CloseableHttpResponse {

    private static final byte[] IMAGE_BYTES;

    static {
        BufferedImage img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "jpg", baos);
        } catch (IOException e) {
            throw new RuntimeException("Error write image", e);
        }
        IMAGE_BYTES = baos.toByteArray();
    }


    @Override
    public void close() {
    }

    @Override
    public StatusLine getStatusLine() {
        return new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK");
    }

    @Override
    public void setStatusLine(StatusLine statusline) {
    }

    @Override
    public void setStatusLine(ProtocolVersion ver, int code) {
    }

    @Override
    public void setStatusLine(ProtocolVersion ver, int code, String reason) {
    }

    @Override
    public void setStatusCode(int code) throws IllegalStateException {
    }

    @Override
    public void setReasonPhrase(String reason) throws IllegalStateException {
    }

    @Override
    public HttpEntity getEntity() {
        return new ByteArrayEntity(IMAGE_BYTES);
    }

    @Override
    public void setEntity(HttpEntity entity) {
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public void setLocale(Locale loc) {
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
        return null;
    }

    @Override
    public boolean containsHeader(String name) {
        return false;
    }

    @Override
    public Header[] getHeaders(String name) {
        return new Header[0];
    }

    @Override
    public Header getFirstHeader(String name) {
        return null;
    }

    @Override
    public Header getLastHeader(String name) {
        return null;
    }

    @Override
    public Header[] getAllHeaders() {
        return new Header[0];
    }

    @Override
    public void addHeader(Header header) {
    }

    @Override
    public void addHeader(String name, String value) {
    }

    @Override
    public void setHeader(Header header) {
    }

    @Override
    public void setHeader(String name, String value) {
    }

    @Override
    public void setHeaders(Header[] headers) {
    }

    @Override
    public void removeHeader(Header header) {
    }

    @Override
    public void removeHeaders(String name) {
    }

    @Override
    public HeaderIterator headerIterator() {
        return null;
    }

    @Override
    public HeaderIterator headerIterator(String name) {
        return null;
    }

    @Override
    public HttpParams getParams() {
        return null;
    }

    @Override
    public void setParams(HttpParams params) {
    }
}
