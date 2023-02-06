package ru.yandex.market.common.util;

import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpStatusCode;
import org.mockserver.verify.VerificationTimes;

import ru.yandex.market.common.parser.LiteInputStreamParser;
import ru.yandex.market.common.report.model.ReportException;

import static org.junit.Assert.assertEquals;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class AsyncRetryHttpClientTest {

    private ClientAndServer mockServer;
    private String baseUrl;

    private AsyncRetryHttpClient client;

    @Before
    public void startMockServer() throws Exception {
        mockServer = startClientAndServer(0);
        baseUrl = "http://localhost:" + mockServer.getLocalPort();

        client = new AsyncRetryHttpClient();

        client.setMaxConnectionTotal(10);
        client.setMaxConnectionPerRoute(10);

        client.afterPropertiesSet();
    }

    @After
    public void stopMockServer() {
        mockServer.stop();
    }

    /**
     * Проверка корректности обработки ответа 500 и работоспособности ретраев (1 запрос + 3 ретрая).
     */
    @Test
    public void test500Response() throws Exception {
        mockStatusCode("/500", HttpStatusCode.INTERNAL_SERVER_ERROR_500);
        assertResponseException("/500", ReportException.class);
        mockServer.verify(request().withPath("/500"), VerificationTimes.exactly(4));
    }

    /**
     * Проверка корректности обработки ответа 503 и работоспособности ретраев (1 запрос + 3 ретрая).
     */
    @Test
    public void test503Response() throws Exception {
        mockStatusCode("/503", HttpStatusCode.SERVICE_UNAVAILABLE_503);
        assertResponseException("/503", ReportException.class);
        mockServer.verify(request().withPath("/503"), VerificationTimes.exactly(4));
    }

    /**
     * Проверка корректности обработки ответа 504 и работоспособности ретраев (1 запрос + 3 ретрая).
     */
    @Test
    public void test504Response() throws Exception {
        mockStatusCode("/504", HttpStatusCode.GATEWAY_TIMEOUT_504);
        assertResponseException("/504", ReportException.class);
        mockServer.verify(request().withPath("/504"), VerificationTimes.exactly(4));
    }

    /**
     * Проверка корректности обработки ответа 400 - ретраев на такой код ответа нет.
     */
    @Test
    public void test400Response() throws Exception {
        mockStatusCode("/400", HttpStatusCode.BAD_REQUEST_400);
        assertResponseException("/400", ReportException.class);
        mockServer.verify(request().withPath("/400"), VerificationTimes.exactly(1));
    }

    /**
     * Проверка корректности обработки таймаута и работоспособности ретраев.
     */
    @Test
    public void testReadTimeout() throws Exception {
        client.setRetryOnErrors(Collections.singletonList(SocketTimeoutException.class));
        client.afterPropertiesSet();

        mockServer
                .when(request().withPath("/200-timeout"))
                .respond(response()
                        .withDelay(TimeUnit.MILLISECONDS, 1000)
                        .withStatusCode(HttpStatusCode.OK_200.code())
                );

        Throwable throwable = client.execute(baseUrl + "/200-timeout", new Timeouts(100, null), new StubParser())
                .handle((r, ex) -> ex)
                .get();

        assertEquals(ReportException.class, throwable.getClass());

        mockServer.verify(request().withPath("/200-timeout"), VerificationTimes.exactly(4));
    }

    private static class StubParser implements LiteInputStreamParser<Void> {
        @Override
        public Void parse(InputStream inputStream) {
            return null;
        }
    }

    private void mockStatusCode(String uri, HttpStatusCode code) {
        mockServer
                .when(request().withPath(uri))
                .respond(response().withStatusCode(code.code()));
    }

    private void assertResponseException(String uri, Class<? extends Throwable> expectedEx) throws Exception {
        Throwable throwable = client.execute(baseUrl + uri, new Timeouts(), new StubParser())
                .handle((r, ex) -> ex)
                .get();

        assertEquals(expectedEx, throwable.getClass());
    }
}
