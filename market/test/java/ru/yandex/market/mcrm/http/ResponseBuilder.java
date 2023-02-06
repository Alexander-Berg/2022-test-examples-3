package ru.yandex.market.mcrm.http;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * @author apershukov
 */
public class ResponseBuilder {

    private int code = 200;
    private byte[] body = new byte[0];

    public static ResponseBuilder newBuilder() {
        return new ResponseBuilder();
    }

    public ResponseBuilder code(int code) {
        this.code = code;
        return this;
    }

    public ResponseBuilder body(InputStream is) {
        try {
            body = IOUtils.toByteArray(is);
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseBuilder body(String string) {
        body = string == null ? new byte[0] : string.getBytes();
        return this;
    }

    public ResponseBuilder body(byte[] bytes) {
        body = bytes;
        return this;
    }

    public HttpResponse build() {
        return new HttpResponse(
                new ResponseMock(code, body)
        );
    }
}
