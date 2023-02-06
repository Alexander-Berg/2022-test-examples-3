package ru.yandex.market.api.util.httpclient.spi;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import io.netty.util.concurrent.Future;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.httpclient.HttpClientFactory;
import ru.yandex.market.api.util.httpclient.UriBuilderWrap;
import ru.yandex.market.http.HttpClient;
import ru.yandex.market.http.HttpResponse;
import ru.yandex.market.http.RequestPrototype;

public class HttpExpectationTest extends BaseTest {
    @Inject
    private HttpExpectations expectations;

    @Inject
    private HttpClientFactory httpClientFactory;

    @Test
    public void canHandleDuplicatedRequests() {
        // настройка
        expectations.configure(HttpRequestExpectationBuilder.localhost().get())
            .ok()
            .times(2);
        // действия
        HttpClient testHttpClient = createTestHttpClient();

        UriBuilderWrap uriBuilder = new UriBuilderWrap("http://localhost");
        URI uri = uriBuilder.build();

        Futures.waitAndGet(testHttpClient.doGetFull(uri, RequestPrototype.JSON));
        Futures.waitAndGet(testHttpClient.doGetFull(uri, RequestPrototype.JSON));
    }

    @Test(expected = UnmatchedHttpRequestException.class)
    public void singleSetup_andMultipleCalls_WaitError() {
        // настройка
        expectations.configure(HttpRequestExpectationBuilder.localhost().get())
            .ok()
            .emptyResponse();

        // действия
        HttpClient testHttpClient = createTestHttpClient();

        UriBuilderWrap uriBuilder = new UriBuilderWrap("http://localhost");
        URI uri = uriBuilder.build();

        Futures.waitAndGet(testHttpClient.doGetFull(uri, RequestPrototype.JSON));
        Futures.waitAndGet(testHttpClient.doGetFull(uri, RequestPrototype.JSON));
    }

    @Test()
    public void unmatchedRequest_WaitException() {
        // настройка
        expectations.configure(HttpRequestExpectationBuilder.localhost().get())
            .ok().emptyResponse();
        // действия теста
        try {
            expectations.verify();
            Assert.fail("no exception");
        } catch (UnmatchedHttpExpectationExistsException e) {

        } catch (Throwable t) {
            Assert.fail(String.format("unknown exception: %s", t));
        } finally {
            // это необходимо т.к. verify вызовется в tearDown теста и выбросит исключение, от чего тест упадет
            expectations.reset();
        }
    }

    @Test
    public void setupHttpResponseBody_FromFile_RelativeToTestClass() {
        // настройка
        expectations.configure(HttpRequestExpectationBuilder.localhost().get())
            .ok()
            .body("test-response.txt");
        String expected = getExpectedBody("test-response.txt");

        // действия
        HttpClient testHttpClient = createTestHttpClient();
        UriBuilderWrap uriBuilder = new UriBuilderWrap("http://localhost");

        Future<HttpResponse> future = testHttpClient.doGetFull(uriBuilder.build(), RequestPrototype.JSON);
        HttpResponse response = Futures.waitAndGet(future);
        String actual = new String(response.getBody(), StandardCharsets.UTF_8);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void setupHttpResponseBody_FromFile_BasedOnFullPath() {
        // настройка
        String resourceFullPath = "/ru/yandex/market/api/util/httpclient/spi/test-response.txt";
        expectations.configure(HttpRequestExpectationBuilder.localhost().get())
            .ok()
            .body(resourceFullPath);
        String expected = getExpectedBody(resourceFullPath);

        // действия
        HttpClient testHttpClient = createTestHttpClient();
        UriBuilderWrap uriBuilder = new UriBuilderWrap("http://localhost");

        Future<HttpResponse> future = testHttpClient.doGetFull(uriBuilder.build(), RequestPrototype.JSON);
        HttpResponse response = Futures.waitAndGet(future);
        String actual = new String(response.getBody(), StandardCharsets.UTF_8);
        Assert.assertEquals(expected, actual);
    }


    private String getExpectedBody(String resource) {
        ClassPathResource source = new ClassPathResource(resource, this.getClass());
        try {
            byte[] data = IOUtils.readFully(source.getInputStream(), (int) source.contentLength());
            return new String(data, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpClient create(String configuration) {
        return httpClientFactory.create(configuration).build();
    }

    private HttpClient createTestHttpClient() {
        return create("TestHttpClient");
    }

}
