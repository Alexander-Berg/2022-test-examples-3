package ru.yandex.direct.juggler;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.http.entity.ContentType;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Request;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.direct.test.utils.MockedHttpWebServerRule;
import ru.yandex.direct.utils.FakeMonotonicClock;
import ru.yandex.direct.utils.MonotonicClock;
import ru.yandex.direct.utils.NanoTimeClock;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JugglerTest {

    @Rule
    public final MockedHttpWebServerRule mockedHTTPServer = new MockedHttpWebServerRule(ContentType.APPLICATION_JSON);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private AsyncHttpJugglerClient client;
    private AsyncHttpJugglerClient clientWithGateaway;

    @Before
    public void setup() {
        mockedHTTPServer.addPostResponsesFromConfig("ru/yandex/direct/libs/juggler/http_mock.conf");

        client = new AsyncHttpJugglerClient(mockedHTTPServer.getServerURL());
        clientWithGateaway = new AsyncHttpJugglerClient(mockedHTTPServer.getServerURL(), true);
    }

    @After
    public void tearDown() throws Exception {
        client.close();
        clientWithGateaway.close();
    }

    @Test
    public void testClient() {
        client.sendEvents(
                new JugglerEvent(
                        "host", "service", "instance", JugglerStatus.OK, "test"
                ),
                new JugglerEvent(
                        "host1", "service1", "instance1", JugglerStatus.CRIT, "test1"
                )
        );
    }

    @Test
    public void testClientGateaway() {
        clientWithGateaway.sendEvents(
                new JugglerEvent(
                        "host", "service", "instance", JugglerStatus.OK, "test"
                ),
                new JugglerEvent(
                        "host1", "service1", "instance1", JugglerStatus.CRIT, "test1"
                )
        );
    }

    @Test
    public void testClientBadRequest() {
        assertThatThrownBy(() -> client.sendEvents(
                new JugglerEvent(
                        "host", "bad", "instance", JugglerStatus.OK, "test"
                )
        )).isInstanceOf(JugglerClient.FailedEventsException.class);
    }

    @Test
    public void testBadReadTimeout() {
        thrown.expect(ArithmeticException.class);
        thrown.expectMessage("integer overflow");
        AsyncHttpClient defaultHttpClient = new DefaultAsyncHttpClient();
        try {
            new AsyncHttpJugglerClient("http://localhost:8999/", false, defaultHttpClient, Duration.ofDays(100));
        } finally {
            try {
                defaultHttpClient.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    @Test
    public void testAsyncSender() {
        MonotonicClock clock = new FakeMonotonicClock();
        StubJugglerClient client = new StubJugglerClient(Duration.ofMillis(100), "", clock);
        JugglerEvent[] events = new JugglerEvent[]{
                new JugglerEvent("h1", "s1", "", JugglerStatus.OK, "t1"),
                new JugglerEvent("h2", "s2", "", JugglerStatus.CRIT, "t2")
        };

        try (JugglerAsyncMultiSender sender = new JugglerAsyncMultiSender(
                Collections.singletonList(client),
                Duration.ofMillis(200),
                Duration.ofSeconds(10),
                100,
                clock
        )) {
            sender.sendEvent(events[0], Duration.ofMinutes(1));
            sender.sendEvent(events[1], Duration.ofMinutes(1));
        }

        Assert.assertEquals(Arrays.asList(events), client.getSentEvents());
    }

    @Test
    public void testAsyncSenderTimeout() throws InterruptedException {
        FakeMonotonicClock clock = new FakeMonotonicClock();
        StubJugglerClient client = new StubJugglerClient(Duration.ofSeconds(61), "", clock);
        JugglerEvent[] events = new JugglerEvent[]{
                new JugglerEvent("h1", "s1", "", JugglerStatus.OK, "t1"),
                new JugglerEvent("h2", "s2", "", JugglerStatus.CRIT, "t2")
        };

        try (
                JugglerAsyncMultiSender sender = new JugglerAsyncMultiSender(
                        Collections.singletonList(client),
                        Duration.ofMillis(200),
                        Duration.ofSeconds(10),
                        1,
                        clock
                );
                FakeMonotonicClock.ReferenceTimeline ignored = clock.referenceTimeline()
        ) {
            sender.sendEvent(events[0], Duration.ofMinutes(1));
            client.waitForEventsCount(1);
            sender.sendEvent(events[1], Duration.ofMinutes(1));
        }

        Assert.assertEquals(Collections.singletonList(events[0]), client.getSentEvents());
    }

    /**
     * Тест против "Consumer thread is unexpectedly stopped without an error"
     * https://st.yandex-team.ru/METR-43791#60a2798cb680cb681809cbad
     */
    @Test
    public void clientStillWorkIfAllEventsTimedOut() {
        var eventsTimeout = Duration.ofMillis(1500);
        var networkTimeout = eventsTimeout.multipliedBy(2);

        AsyncHttpClient asyncHttpClient = mock(AsyncHttpClient.class);
        var clock = new NanoTimeClock();
        System.err.println(Thread.currentThread());
        when(asyncHttpClient.executeRequest(any(Request.class)))
                .thenAnswer(invocation -> {
                    // эмулируем сетевые проблемы
                    clock.sleepUninterruptibly(networkTimeout);
                    throw new ExecutionException("test exception", new TimeoutException());
                });
        var client = new AsyncHttpJugglerClient(mockedHTTPServer.getServerURL(), true, asyncHttpClient);


        JugglerAsyncSender sender = new JugglerAsyncSender(client, Duration.ZERO, Duration.ZERO, 10, clock);
        // добавляем в очередь событие, которое повесит клиент
        sender.sendEvent(new JugglerEvent("t", "t1", "", JugglerStatus.WARN, "t"), eventsTimeout);
        // ждем немного, чтобы клиент успел подхватить и начать отправлять событие
        clock.sleepUninterruptibly(eventsTimeout.dividedBy(2));
        // добавляем событие, которое на следующей итерации — сломает клиент (т.к. будет уже протухшим)
        sender.sendEvent(new JugglerEvent("t", "t1", "", JugglerStatus.WARN, "test"), eventsTimeout);
        // ждем, пока клиент обработает первое и второе события
        clock.sleepUninterruptibly(networkTimeout.plusSeconds(5));
        assertThatCode(
                () -> sender.sendEvent(
                        new JugglerEvent("t", "t1", "", JugglerStatus.WARN, "test"),
                        eventsTimeout))
                .doesNotThrowAnyException();
    }

    @Test
    public void testAsyncSenderFailedEvent() {
        MonotonicClock clock = new FakeMonotonicClock();
        StubJugglerClient client = new StubJugglerClient(Duration.ofMillis(100), "t1", clock);
        JugglerEvent[] events = new JugglerEvent[]{
                new JugglerEvent("h1", "s1", "", JugglerStatus.OK, "t1"),
                new JugglerEvent("h2", "s2", "", JugglerStatus.CRIT, "t2")
        };

        try (JugglerAsyncMultiSender sender = new JugglerAsyncMultiSender(
                Collections.singletonList(client),
                Duration.ofMillis(200),
                Duration.ofSeconds(10),
                100,
                clock
        )) {
            sender.sendEvent(events[0], Duration.ofMinutes(1));
            sender.sendEvent(events[1], Duration.ofMinutes(1));
        }

        Assert.assertEquals(Collections.singletonList(events[1]), client.getSentEvents());
    }

    @Test
    public void testEventsQueue() throws InterruptedException {
        MonotonicClock clock = new FakeMonotonicClock();
        JugglerEventQueue queue = new JugglerEventQueue(clock);

        // Это событие будет перезатерто следующим
        queue.add(new JugglerEventWithDeadline(
                new JugglerEvent("h1", "", "", JugglerStatus.OK, "d000"),
                clock.getTime().plus(Duration.ofDays(1))
        ));

        // Это событие не будет добавлено (просрочено), и кроме этого, сотрет уже существующее для h1
        queue.add(new JugglerEventWithDeadline(
                new JugglerEvent("h1", "", "", JugglerStatus.OK, "d0"),
                clock.getTime().minus(Duration.ofNanos(1))
        ));

        queue.add(new JugglerEventWithDeadline(
                new JugglerEvent("h2", "", "", JugglerStatus.OK, "d1"),
                clock.getTime().plus(Duration.ofSeconds(1))
        ));

        queue.add(new JugglerEventWithDeadline(
                new JugglerEvent("h3", "", "", JugglerStatus.OK, "d2"),
                clock.getTime().plus(Duration.ofDays(1))
        ));

        // Шлем еще одно событие для h2, чтобы убедиться в правильной очередности.
        queue.add(new JugglerEventWithDeadline(
                new JugglerEvent("h2", "", "", JugglerStatus.OK, "d3"),
                clock.getTime().plus(Duration.ofSeconds(1))
        ));

        Assert.assertEquals(
                Arrays.asList("d3", "d2"),
                queue.stream().map(e -> e.getEvent().getDescription()).collect(Collectors.toList())
        );

        clock.sleep(Duration.ofMillis(1001));

        Assert.assertEquals(
                Collections.singletonList("d3"),
                queue.popTimedOutEvents().stream().map(e -> e.getEvent().getDescription()).collect(Collectors.toList())
        );
        Assert.assertEquals(Collections.emptyList(), queue.popTimedOutEvents());
        Assert.assertEquals(
                Collections.singletonList("d2"),
                queue.stream().map(e -> e.getEvent().getDescription()).collect(Collectors.toList())
        );
    }
}
