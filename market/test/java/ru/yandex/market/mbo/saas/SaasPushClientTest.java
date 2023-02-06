package ru.yandex.market.mbo.saas;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.http.nio.reactor.IOReactorException;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.TimeToLive;
import org.mockserver.matchers.Times;
import org.mockserver.model.Delay;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.search.saas.RTYServer;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

/**
 * @author apluhin
 * @created 12/9/21
 */
@RunWith(SpringRunner.class)
public class SaasPushClientTest {

    private static ClientAndServer clientAndServer;
    private SaasPushClient saasPushClient;

    @BeforeClass
    public static void beforeClass() throws Exception {
        clientAndServer = startClientAndServer(45132);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        clientAndServer.stop();
    }

    @Before
    public void setUp() throws Exception {
        clientAndServer.reset();
        saasPushClient = new SaasPushClient(
                "localhost",
                45132,
                "/test",
                5,
                30_000,
                1024);
    }

    @After
    public void tearDown() throws Exception {
        clientAndServer.reset();
    }

    @Test
    public void testCorrectHandleAllSuccessRepsponse() throws InterruptedException {
        List<SaasLogbrokerEvent> saasLogbrokerEvents = generateEvents(100);
        clientAndServer.when(HttpRequest.request("/test")).respond(HttpResponse.response().withStatusCode(200));
        saasPushClient.publishEvents(saasLogbrokerEvents);
    }

    @Test(expected = RuntimeException.class)
    public void testCorrectHandleRequestWithFailedResponse() throws InterruptedException {
        List<SaasLogbrokerEvent> saasLogbrokerEvents = generateEvents(150);
        clientAndServer.when(HttpRequest.request("/test"), Times.exactly(100), TimeToLive.unlimited(), 100)
                .respond(HttpResponse.response().withStatusCode(200));
        saasPushClient.publishEvents(saasLogbrokerEvents);
    }

    @Test(expected = RuntimeException.class, timeout = 30_000)
    public void testLivelock() throws InterruptedException, IOReactorException {
        SaasPushClient client = new SaasPushClient(
                "localhost",
                45132,
                "/test",
                5,
                10_000,
                1024);

        List<SaasLogbrokerEvent> saasLogbrokerEvents = generateEvents(100);
        clientAndServer.reset();
        clientAndServer.when(HttpRequest.request("/test"))
                .respond(HttpResponse.response().withStatusCode(200)
                        .withDelay(Delay.delay(TimeUnit.MINUTES, 1)));
        client.publishEvents(saasLogbrokerEvents);
    }

    @Test
    public void testLimitBandwidth() throws InterruptedException, IOReactorException {

        saasPushClient = new SaasPushClient(
                "localhost",
                45132,
                "/test",
                5,
                30_000,
                1024);

        //rate limiter skip first acquire without timeout
        //bandwidth 1024kb/s, message size ~ 1024kb
        List<SaasLogbrokerEvent> saasLogbrokerEvents = generateLargeEvents(7);
        clientAndServer.reset();
        clientAndServer.when(HttpRequest.request("/test"))
                .respond(HttpResponse.response().withStatusCode(200));

        long start = System.currentTimeMillis();
        saasPushClient.publishEvents(saasLogbrokerEvents);
        Assertions.assertThat(System.currentTimeMillis() - start)
                .isGreaterThanOrEqualTo(5000L);
    }

    private List<SaasLogbrokerEvent> generateEvents(int count) {
        return IntStream.range(0, count)
                .boxed()
                .map(it -> new SaasLogbrokerEvent(
                        RTYServer.TMessage.newBuilder()
                                .setMessageType(RTYServer.TMessage.TMessageType.MODIFY_DOCUMENT)
                                .setDocument(
                                        RTYServer.TMessage.TDocument.newBuilder().setUrl(String.valueOf(it)).setBody(String.valueOf(it)).build()
                                ).build()
                )).collect(Collectors.toList());
    }

    private List<SaasLogbrokerEvent> generateLargeEvents(int count) throws IOReactorException {
        byte[] bytes = new byte[1024 * 1024];
        return IntStream.range(0, count)
                .boxed()
                .map(it -> new SaasLogbrokerEvent(
                        RTYServer.TMessage.newBuilder()
                                .setMessageType(RTYServer.TMessage.TMessageType.MODIFY_DOCUMENT)
                                .setDocument(
                                        RTYServer.TMessage.TDocument.newBuilder()
                                                .setUrl(String.valueOf(it)).setBody(new String(bytes)).build()
                                ).build()
                )).collect(Collectors.toList());
    }
}
