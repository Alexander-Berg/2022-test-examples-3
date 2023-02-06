package ru.yandex.market.api.listener;

import com.google.common.collect.ImmutableMap;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import ru.yandex.market.api.listener.domain.HttpStatus;
import ru.yandex.market.api.listener.expectations.HttpExpectations;
import ru.yandex.market.api.listener.expectations.HttpRequestExpectationBuilder;
import ru.yandex.market.api.listener.expectations.HttpResponseConfigurer;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.api.listener.ListenerTestHelper.*;
import static ru.yandex.market.api.util.Exceptions.wrapRuntimeException;

/**
 * @author dimkarp93
 */
public class ListenerTest {
    private static final String HOST = "localhost";
    private static final int PORT = 13666;

    private static final String REQUEST_PATH = "/match";
    private static final String REQUEST_QUERY_PARAM_NAME = "q";
    private static final String REQUEST_QUERY_PARAM_VALUE = "qwerty";

    private static final String REQUEST_POST_BODY = "hello";

    private static final String RESPONSE_GET_BODY = "It works!";
    private static final String RESPONSE_POST_BODY = "world!";

    private static final URI URI = uri(HOST,
        PORT,
        REQUEST_PATH,
        ImmutableMap
            .<String, String>builder()
            .put(REQUEST_QUERY_PARAM_NAME, REQUEST_QUERY_PARAM_VALUE)
            .build()
    );

    private static final HttpRequestExpectationBuilder GET_REQUEST_CONFIG = HttpRequestExpectationBuilder.localhost()
        .get()
        .serverMethod(REQUEST_PATH)
        .param(REQUEST_QUERY_PARAM_NAME, REQUEST_QUERY_PARAM_VALUE);

    private static final Consumer<HttpResponseConfigurer> GET_RESPONSE_CONFIG = configurer -> configurer.ok()
        .body(bytes(RESPONSE_GET_BODY));

    private static final HttpRequestExpectationBuilder POST_REQUEST_CONFIG = HttpRequestExpectationBuilder.localhost()
        .post()
        .serverMethod(REQUEST_PATH)
        .param(REQUEST_QUERY_PARAM_NAME, REQUEST_QUERY_PARAM_VALUE)
        .body(b -> Arrays.equals(bytes(REQUEST_POST_BODY), b), "body matched");

    private static final Consumer<HttpResponseConfigurer> POST_RESPONSE_CONFIG = configurer -> configurer.ok()
        .body(bytes(RESPONSE_POST_BODY));

    @Test
    public void listenerGetMatch() throws IOException {
        Listener listener = listener(config(GET_REQUEST_CONFIG, GET_RESPONSE_CONFIG));

        try {
            listener.start();

            execute(get(URI), response -> assertMatch(RESPONSE_GET_BODY, response));
        } finally {
            listener.stop();
        }
    }

    @Test
    public void listenerGetMismatchByPath() throws IOException {
        Listener listener = listener(config(GET_REQUEST_CONFIG, GET_RESPONSE_CONFIG));

        try {
            listener.start();

            URI uri = uri(HOST,
                PORT,
                "/no" + REQUEST_PATH,
                ImmutableMap
                    .<String, String>builder()
                    .put(REQUEST_QUERY_PARAM_NAME, REQUEST_QUERY_PARAM_VALUE)
                    .build()
            );

            execute(get(uri), ListenerTest::assertMismatch);

        } finally {
            listener.stop();
        }
    }


    @Test
    public void listenerGetMismatchByQuery() throws IOException {
        Listener listener = listener(config(GET_REQUEST_CONFIG, GET_RESPONSE_CONFIG));

        try {
            listener.start();

            URI uri = uri(HOST, PORT, REQUEST_PATH,
                ImmutableMap
                    .<String, String>builder()
                    .put(REQUEST_QUERY_PARAM_NAME, "no" + REQUEST_QUERY_PARAM_VALUE)
                    .build());

            execute(get(uri), ListenerTest::assertMismatch);

        } finally {
            listener.stop();
        }
    }


    @Test
    public void listenerPostMatch() throws IOException {
        Listener listener = listener(config(POST_REQUEST_CONFIG, POST_RESPONSE_CONFIG));

        try {
            listener.start();

            execute(post(URI, REQUEST_POST_BODY),
                response -> assertMatch(RESPONSE_POST_BODY, response));
        } finally {
            listener.stop();
        }

    }

    @Test
    public void listenerPostMismatchByPath() throws IOException {
        Listener listener = listener(config(POST_REQUEST_CONFIG, POST_RESPONSE_CONFIG));

        try {
            listener.start();

            URI uri = uri(HOST,
                PORT,
                "/no" + REQUEST_PATH,
                ImmutableMap
                    .<String, String>builder()
                    .put(REQUEST_QUERY_PARAM_NAME, REQUEST_QUERY_PARAM_VALUE)
                    .build()
            );

            execute(post(uri, REQUEST_POST_BODY), ListenerTest::assertMismatch);

        } finally {
            listener.stop();
        }
    }

    @Test
    public void listenerPostMismatchByQuery() throws IOException {
        Listener listener = listener(config(POST_REQUEST_CONFIG, POST_RESPONSE_CONFIG));

        try {
            listener.start();

            URI uri = uri(HOST,
                PORT,
                REQUEST_PATH,
                ImmutableMap
                    .<String, String>builder()
                    .put(REQUEST_QUERY_PARAM_NAME, "no" + REQUEST_QUERY_PARAM_VALUE)
                    .build()
            );

            execute(post(uri, REQUEST_POST_BODY), ListenerTest::assertMismatch);
        } finally {
            listener.stop();
        }
    }

    @Test
    public void listenerPostMismatchByBody() throws IOException {
        Listener listener = listener(config(POST_REQUEST_CONFIG, POST_RESPONSE_CONFIG));

        try {
            listener.start();

            execute(post(URI, "no" + REQUEST_POST_BODY), ListenerTest::assertMismatch);
        } finally {
            listener.stop();
        }
    }


    private static Listener listener(HttpExpectations httpExpectations) {
        return Listener.listener(httpExpectations, PORT);
    }


    private static void execute(HttpRequestBase request, Consumer<HttpResponse> processor) {
        try (CloseableHttpClient client = HttpClientBuilder.create()
            .build()) {

            processor.accept(client.execute(request));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void assertMatch(String responseBody, HttpResponse response) {
        wrapRuntimeException(() -> {
            int status = response.getStatusLine().getStatusCode();
            assertEquals(HttpStatus.OK.value(), status);

            String body = EntityUtils.toString(response.getEntity());
            assertEquals(responseBody, body);
        });
    }

    private static void assertMismatch(HttpResponse response) {
        int status = response.getStatusLine().getStatusCode();
        assertEquals(HttpStatus.NOT_IMPLEMENTED.value(), status);
    }


    private static HttpExpectations config(HttpRequestExpectationBuilder request,
                                           Consumer<HttpResponseConfigurer> response) {
        HttpExpectations httpExpectations = new HttpExpectations();
        response.accept(httpExpectations.configure(request));
        return httpExpectations;
    }


}
