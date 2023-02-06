package ru.yandex.market.api.listener.expectations;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.listener.Listener;
import ru.yandex.market.api.listener.domain.HttpStatus;
import ru.yandex.market.api.util.Urls;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

public class HttpExpectationTest {
    private static final int PORT = 13666;

    @Test
    public void canHandleDuplicatedRequests() throws IOException {
        HttpExpectations httpExpectations = config(HttpRequestExpectationBuilder.localhost().get(),
            configurer -> configurer.ok().times(2));

        Listener listener = listener(httpExpectations);
        HttpClient testHttpClient = createTestHttpClient();

        try {
            listener.start();
            URI uri = Urls.toUri("http://localhost:" + PORT);

            assertMatch(testHttpClient.execute(new HttpGet(uri)));
            assertMatch(testHttpClient.execute(new HttpGet(uri)));
        } finally {
            listener.stop();
        }
    }

    @Test
    public void singleSetup_andMultipleCalls_WaitError() throws IOException {
        HttpExpectations httpExpectations = config(HttpRequestExpectationBuilder.localhost().get(),
            configurer -> configurer.ok().emptyResponse());

        Listener listener = listener(httpExpectations);
        HttpClient testHttpClient = createTestHttpClient();

        try {
            listener.start();

            URI uri = Urls.toUri("http://localhost:" + PORT);
            assertMatch(testHttpClient.execute(new HttpGet(uri)));
            assertMistmatch(testHttpClient.execute(new HttpGet(uri)));
        } finally {
            listener.stop();
        }
    }

    @Test
    public void unmatchedRequest_WaitException() throws IOException {
        HttpExpectations httpExpectations = config(HttpRequestExpectationBuilder.localhost().get(),
            configurer -> configurer.ok().emptyResponse());

        Listener listener = listener(httpExpectations);

        try {
            listener.start();
            httpExpectations.verify();
            Assert.fail("no exception");
        } catch (UnmatchedHttpExpectationExistsException e) {

        } catch (Throwable t) {
            Assert.fail(String.format("unknown exception: %s", t));
        } finally {
            // это необходимо т.к. verify вызовется в tearDown теста и выбросит исключение, от чего тест упадет
            httpExpectations.reset();

            listener.stop();
        }
    }

    @Test
    public void setupHttpResponseBody_FromFile_RelativeToTestClass() throws IOException {
        System.out.printf("uri: '%s'\n", getClass().getResource("test-response.txt").toString());

        HttpExpectations httpExpectations = config(HttpRequestExpectationBuilder.localhost().get(),
            configurer -> configurer.ok().body("test-response.txt"));

        Listener listener = listener(httpExpectations);

        String expected = getExpectedBody("test-response.txt");
        HttpClient testHttpClient = createTestHttpClient();

        try {
            listener.start();
            URI uri = Urls.toUri("http://localhost:" + PORT);

            HttpResponse response = testHttpClient.execute(new HttpGet(uri));
            assertMatch(response);
            String actual = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            assertEquals(expected, actual);
        } finally {
            listener.stop();
        }
    }

    @Test
    public void setupHttpResponseBody_FromFile_BasedOnFullPath() throws IOException {
        String resourceFullPath = "/ru/yandex/market/api/listener/expectations/test-response.txt";
        String expected = getExpectedBody(resourceFullPath);

        HttpExpectations httpExpectations = config(HttpRequestExpectationBuilder.localhost().get(),
            configurer -> configurer.ok().body(resourceFullPath));

        Listener listener = listener(httpExpectations);

        HttpClient testHttpClient = createTestHttpClient();
        URI uri = Urls.toUri("http://localhost:" + PORT);

        try {
            listener.start();

            HttpResponse response = testHttpClient.execute(new HttpGet(uri));
            assertMatch(response);
            String actual = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            assertEquals(expected, actual);
        } finally {
            listener.stop();
        }
    }


    private String getExpectedBody(String resource) {
        return new String(ResourceHelpers.getResource(resource), StandardCharsets.UTF_8);
    }

    private HttpClient createTestHttpClient() {
        return HttpClientBuilder.create().build();
    }

    private Listener listener(HttpExpectations httpExpectations) {
        return Listener.listener(httpExpectations, PORT);
    }

    private static HttpExpectations config(HttpRequestExpectationBuilder request,
                                                      Consumer<HttpResponseConfigurer> response) {
        HttpExpectations httpExpectations = new HttpExpectations();
        response.accept(httpExpectations.configure(request));
        return httpExpectations;
    }

    private static void assertMatch(HttpResponse response) {
        assertEquals(HttpStatus.OK.value(), response.getStatusLine().getStatusCode());
    }

    private static void assertMistmatch(HttpResponse response) {
        assertEquals(HttpStatus.NOT_IMPLEMENTED.value(), response.getStatusLine().getStatusCode());
    }
}
