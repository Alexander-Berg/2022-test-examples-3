package ru.yandex.market.pricelabs;

import java.net.HttpURLConnection;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import okhttp3.mockwebserver.RecordedRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.MimeTypeUtils;

import ru.yandex.market.pricelabs.apis.ApiConst;
import ru.yandex.market.pricelabs.misc.TimeSource;
import ru.yandex.market.pricelabs.misc.TimingUtils;

public class CoreConfigurationForTests {

    @Configuration
    public static class Basic {

        /**
         *
         * @return Часы, в которых можно поменять текущее время через методы {@link TimingUtils}
         */
        @Bean
        public TimeSource clock() {
            return TimingUtils.timeSource();
        }

        public static CleanableQueueDispatcher getAgnosticDispatcher() {
            return new CleanableQueueDispatcher();
        }

        /**
         *
         * @return Тестовый сервер, который будет ловить все запросы в Retrofit
         */
        public static MockWebServer mockWebServer() {
            return new MockWebServer();
        }

    }

    public static class CleanableQueueDispatcher extends QueueDispatcher {
        private static final Logger logger = Logger.getLogger(CleanableQueueDispatcher.class.getName());

        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            // To permit interactive/browser testing, ignore requests for favicons.
            String requestLine = request.getRequestLine();
            if (requestLine != null && requestLine.equals("GET /favicon.ico HTTP/1.1")) {
                logger.info("served " + requestLine);
                return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
            }

            if (responseQueue.peek() == null) {
                @Nullable var acceptResponse = buildResponse(request.getHeader("Accept"));
                if (acceptResponse != null) {
                    return acceptResponse;
                }

                @Nullable var contentTypeResponse = buildResponse(request.getHeader("Content-Type"));
                if (contentTypeResponse != null) {
                    return contentTypeResponse;
                }

                return jsonResponse();
            }

            return super.dispatch(request);
        }

        @Nullable
        private MockResponse buildResponse(@Nullable String header) {
            if (MimeTypeUtils.APPLICATION_JSON_VALUE.equals(header)) {
                return jsonResponse();
            } else if (MimeTypeUtils.APPLICATION_XML_VALUE.equals(header)) {
                return new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", MimeTypeUtils.APPLICATION_XML_VALUE)
                        .setBody("<?xml version=\"1.0\" encoding=\"utf-8\"?><body/>");
            } else if (MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE.equals(header)) {
                return new MockResponse().setResponseCode(200);
            } else if (ApiConst.MIME_PROTOBUF.equals(header)) {
                return new MockResponse().setResponseCode(200);
            } else {
                return null;
            }
        }

        private MockResponse jsonResponse() {
            return new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", MimeTypeUtils.APPLICATION_JSON_VALUE)
                    .setBody("{}");
        }

        public void clean() {
            responseQueue.clear();
        }
    }
}
