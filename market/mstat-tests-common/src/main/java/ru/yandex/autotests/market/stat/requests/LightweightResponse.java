package ru.yandex.autotests.market.stat.requests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.util.List;

/**
 * Created by entarrion on 05.05.16.
 */
public interface LightweightResponse {
    String getContentType();

    int getStatusCode();

    List<RequestParam> getHeaders();

    List<HttpCookie> getCookies();

    InputStream bodyAsInputStream();

    String getCharset();

    default byte[] bodyAsByte() {
        try (InputStream in = bodyAsInputStream(); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            if (in != null) {
                byte[] buffer = new byte[1024];
                for (int len; (len = in.read(buffer)) != -1; )
                    os.write(buffer, 0, len);
                os.flush();
            }
            return os.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    default String bodyAsString() {
        return bodyAsString(getCharset());
    }

    default String bodyAsString(String charset) {
        final byte[] result = bodyAsByte();
        try {
            return new String(result, 0, result.length, charset);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Can't create a string encoding " + charset, e);
        }
    }
}