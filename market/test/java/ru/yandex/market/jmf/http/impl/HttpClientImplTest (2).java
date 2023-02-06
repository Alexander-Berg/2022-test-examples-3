package ru.yandex.market.jmf.http.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.asynchttpclient.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.crm.util.Futures;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.http.Http;
import ru.yandex.market.jmf.http.HttpClient;
import ru.yandex.market.jmf.http.HttpResponse;
import ru.yandex.market.jmf.http.test.HttpClientFactoryTestConfiguration;
import ru.yandex.market.jmf.http.test.ResponseBuilder;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = HttpClientFactoryTestConfiguration.class)
@TestPropertySource("classpath:/ru/yandex/market/jmf/http/internal/external.properties")
public class HttpClientImplTest {

    @Inject
    HttpClientFactoryImpl factory;

    @Test
    public void check200() {
        byte[] responseBody = Randoms.bytea();

        HttpClient client = getClient(request -> createResponse(200, responseBody));

        Http request = Http.get();
        HttpResponse response = client.execute(request);

        // проверка утверждений
        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.getHttpStatus().value());
        Assertions.assertArrayEquals(responseBody, response.getBodyAsBytes());
    }

    @Test
    public void check500() {
        byte[] responseBody = Randoms.bytea();

        HttpClient client = getClient(request -> createResponse(500, responseBody));

        Http request = Http.get();
        HttpResponse response = client.execute(request);

        // проверка утверждений
        Assertions.assertNotNull(response);
        Assertions.assertEquals(500, response.getHttpStatus().value());
        Assertions.assertArrayEquals(responseBody, response.getBodyAsBytes());
    }

    @Test
    public void checkException() {
        HttpClient client = getClient(request -> Futures.newFailedFuture(new RuntimeException()));

        Http request = Http.get();
        Assertions.assertThrows(RuntimeException.class, () -> client.execute(request));
    }

    /**
     * Проверяем заполненность заголовка, в котором передается уникальный идентификатор запроса.
     */
    @Test
    public void checkRequest_xMarketReqId() {
        Http httpReq = Http.get();
        Request result = doRequest(httpReq);

        // проверка утверждений
        Assertions.assertNotNull(result);
        String actualHeader = result.getHeaders().get("X-Market-Req-ID");
        Assertions.assertNotNull(actualHeader);
    }

    /**
     * Проверяем заполненность заголовка, в котором передается уникальный идентификатор запроса.
     */
    @Test
    public void checkRequest_url() {
        String path = Randoms.string();
        String parameterName = Randoms.string();
        String parameterValue = Randoms.string();

        Http httpReq = Http.get().path(path).queryParameter(parameterName, parameterValue);
        Request result = doRequest(httpReq);

        // проверка утверждений
        Assertions.assertNotNull(result);
        String actualHeader = result.getUrl();
        // домен берется из настроек external.properties
        Assertions.assertEquals("http://example.com/service2/" + path + "?" + parameterName + "=" + parameterValue,
                actualHeader);
    }

    private Request doRequest(Http httpReq) {
        byte[] responseBody = Randoms.bytea();
        AtomicReference<Request> r = new AtomicReference<>();

        HttpClient client = getClient(request -> {
            r.set(request);
            return createResponse(200, responseBody);
        });

        client.execute(httpReq);
        return r.get();
    }

    private CompletableFuture<HttpResponse> createResponse(int statusCode, byte[] responseBody) {
        return Futures.newSucceededFuture(ResponseBuilder.newBuilder()
                .code(statusCode)
                .body(responseBody)
                .build());
    }

    private HttpClient getClient(HttpClientImpl.Executor executor) {
        HttpClientConfiguration configuration = factory.getConfiguration("service2");
        return new HttpClientImpl(configuration, executor, null);
    }
}
