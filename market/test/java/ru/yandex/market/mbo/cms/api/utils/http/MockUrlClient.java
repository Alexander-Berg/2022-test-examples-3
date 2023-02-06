package ru.yandex.market.mbo.cms.api.utils.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import ru.yandex.market.mbo.cms.core.utils.http.UrlClient;

/**
 * @author ayratgdl
 * @date 21.03.18
 */
public class MockUrlClient extends UrlClient {
    private HashMap<URL, byte[]> responses = new HashMap<>();
    private byte[] defaultResponse;

    public void addResponse(URL url, byte[] response) {
        responses.put(url, response);
    }

    public void setDefaultResponse(byte[] response) {
        defaultResponse = response;
    }

    @Override
    public URLConnection openConnection(URL url) throws IOException {
        if (responses.containsKey(url)) {
            return new FixedURLConnection(responses.get(url));
        } else if (defaultResponse != null) {
            return new FixedURLConnection(defaultResponse);
        } else {
            return new IOExceptionUrlConnection();
        }
    }

    private static class FixedURLConnection extends URLConnection {
        private byte[] response;

        protected FixedURLConnection(byte[] response) {
            super(null);
            this.response = response;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(response);
        }

        @Override
        public void connect() throws IOException {
        }
    }

    private static class IOExceptionUrlConnection extends URLConnection {
        protected IOExceptionUrlConnection() {
            super(null);
        }

        @Override
        public void connect() throws IOException {
            throw new IOException();
        }
    }

}
