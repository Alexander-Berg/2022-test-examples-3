package ru.yandex.market.yql_test.proxy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import ru.yandex.misc.web.servlet.mock.DelegatingServletInputStream;

public class RepeatableReadRequest extends HttpServletRequestWrapper {

    private static final int CHUNK = 1024;

    private final DelegatingServletInputStream inputStream;
    private final byte[] body;

    public RepeatableReadRequest(HttpServletRequest request) {
        super(request);
        if (request.getContentLength() > 0) {
            try {
                body = new byte[request.getContentLength()];
                ServletInputStream is = request.getInputStream();
                int read = 0;
                while (read < request.getContentLength()) {
                    read += is.read(body, read, Math.min(read + CHUNK, body.length));
                }
                inputStream = new DelegatingServletInputStream(new ByteArrayInputStream(body));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            body = null;
            inputStream = null;
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return inputStream != null ? inputStream : super.getInputStream();
    }

    public Optional<byte[]> getBody() {
        return Optional.ofNullable(body);
    }
}
