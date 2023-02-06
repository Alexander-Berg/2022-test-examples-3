package ru.yandex.market.yql_test.proxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.util.Callback;

public class YTResponseWaitingServlet extends ProxyServlet {

    protected class Listener extends ProxyResponseListener {

        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final Consumer<YqlResponseWrapper> responseConsumer;
        private byte[] cachedResponse = {};

        protected Listener(HttpServletRequest request,
                           HttpServletResponse response,
                           Consumer<YqlResponseWrapper> responseConsumer
        ) {
            super(request, response);
            this.request = request;
            this.response = response;
            this.responseConsumer = responseConsumer;
        }

        @Override
        public void onContent(Response proxyResponse, ByteBuffer content, Callback callback) {
            byte[] buffer;
            int offset;
            int length = content.remaining();
            if (content.hasArray()) {
                buffer = content.array();
                offset = content.arrayOffset();
            } else {
                buffer = new byte[length];
                content.get(buffer);

                offset = 0;
            }

            onResponseContent(request, response, proxyResponse, buffer, offset, length, new Callback.Nested(callback) {
                @Override
                public void failed(Throwable x) {
                    super.failed(x);
                    proxyResponse.abort(x);
                }
            });
            addBuffer(buffer);
        }

        @Override
        public void onComplete(Result result) {
            super.onComplete(result);
            Response resp = result.getResponse();
            if (200 <= resp.getStatus() && resp.getStatus() < 400) {
                try {
                    byte[] unzipped = unzip(cachedResponse);
                    responseConsumer.accept(YqlResponseExtractor.extractResponse(unzipped));
                } catch (IOException e) {
                    throw new RuntimeException("Unable to unzip cached response", e);
                }
            }
        }

        private void addBuffer(byte[] buffer) {
            byte[] c = new byte[cachedResponse.length + buffer.length];
            System.arraycopy(cachedResponse, 0, c, 0, cachedResponse.length);
            System.arraycopy(buffer, 0, c, cachedResponse.length, buffer.length);
            cachedResponse = c;
        }

        private byte[] unzip(byte[] source) throws IOException {
            final GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(source));
            return toByteArray(gzipInputStream);
        }

        private byte[] toByteArray(InputStream in) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            copy(in, out);
            return out.toByteArray();
        }

        private void copy(InputStream from, OutputStream to) throws IOException {
            byte[] buf = new byte[0x1000];
            while (true) {
                int r = from.read(buf);
                if (r == -1) {
                    break;
                }
                to.write(buf, 0, r);
            }
        }
    }
}
