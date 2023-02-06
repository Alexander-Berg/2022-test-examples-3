package ru.yandex.direct.asynchttp;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import fi.iki.elonen.NanoHTTPD;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.assertj.core.api.LongAssert;
import org.assertj.core.api.SoftAssertions;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.monlib.metrics.registry.MetricRegistry;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static java.util.stream.Collectors.toList;
import static org.asynchttpclient.util.HttpConstants.ResponseStatusCodes.OK_200;

@RunWith(Parameterized.class)
@Ignore("Для ручного запуска")
public class FetcherTest {
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(FetcherTest.class);
    private static NanoHTTPD nanoHTTPD;
    private static AsyncHttpClient ahc;

    @Parameterized.Parameter
    public boolean collectMetrics;

    @Parameterized.Parameters
    public static Collection<Boolean> collectMetricsParameters() {
        return Arrays.asList(false, true);
    }

    @BeforeClass
    public static void setup() throws Exception {
        nanoHTTPD = new MyNanoHTTPD(20);
        nanoHTTPD.start();
        logger.info("NanoHTTPServer starting");

        ahc = new DefaultAsyncHttpClient();

        do {
            Thread.sleep(1000L);
        } while (!nanoHTTPD.isAlive());
        logger.info("NanoHTTPServer started");

        // делаем один запрос, чтобы всё прогреть
        ahc.executeRequest(new RequestBuilder()
                .setUrl("http://localhost:" + nanoHTTPD.getListeningPort())
                .addQueryParam(MyNanoHTTPD.PARAM_REQUEST_ID, "warmup")
                .build()
        ).get(10, TimeUnit.SECONDS);
    }

    @Before
    public void before() {
        ((MyNanoHTTPD) nanoHTTPD).resetUniqueRequestCounter();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        ahc.close();
        nanoHTTPD.stop();
    }

    @Test
    public void testSoftTimeout() throws InterruptedException {
        FetcherSettings settings = new FetcherSettings()
                .withRequestTimeout(Duration.ofMillis(2000))
                .withSoftTimeout(Duration.ofMillis(300))
                .withRequestRetries(2)
                .withGlobalTimeout(Duration.ofSeconds(20))
                .withParallel(10)
                .withMetricRegistry(collectMetrics ? new MetricRegistry() : null);

        int totalRequestCount = 5;
        List<ParsableRequest<String>> requests = IntStream.range(0, totalRequestCount)
                .mapToObj(i -> new ParsableStringRequest(i, new RequestBuilder()
                        .setUrl("http://localhost:" + nanoHTTPD.getListeningPort())
                        .addQueryParam(MyNanoHTTPD.PARAM_RESPONSE_TIME,
                                Long.toString(Duration.ofSeconds(1L).toMillis()))
                        .addQueryParam(MyNanoHTTPD.PARAM_RESPONSE_TEXT, String.format("Response %03d", i))
                        .addQueryParam(MyNanoHTTPD.PARAM_REQUEST_ID, String.format("Request %03d", i))
                        .addQueryParam(MyNanoHTTPD.PARAM_FAST_RESPONSE_ORDER_NUMBER, Integer.toString(2))
                        .build()))
                .collect(toList());
        /*
        softTimeout в 300мс -- ожидается, что перезапросы из-за softTimeout'а выполнятся быстрее первых вызовов.
         */
        Duration executeDuration;
        Map<Long, Result<String>> execute;
        ParallelFetcherMetrics metrics;
        try (ParallelFetcher<String> fetcher = new ParallelFetcher<>(settings, ahc)) {
            long startTime = System.nanoTime();
            logger.info("Execute");
            execute = fetcher.execute(requests);
            executeDuration = Duration.ofNanos(System.nanoTime() - startTime);
            metrics = fetcher.getMetrics();
        }

        EntryStream.of(execute)
                .forKeyValue((id, result) -> System.out.println(String.format("Request %03d: %s", id, result)));

        logger.info("Execution time: {}ms", executeDuration.toMillis());
        Duration limitDuration = Duration.ofMillis(2000);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(executeDuration).isLessThan(limitDuration);
            assertMetrics(softly, metrics, new ExpectedMetrics()
                    .withRequests(5 * 2) // каждый из запросов отправили дважды
                    .withSuccesses(5) // переотправленные запросы выполнились
                    .withFailures(0) // все запросы завершились либо успехом, либо абортом
                    .withAborted(5) // первые отправленные заабортили
                    .withSoftRetries(5)); // кол-во переотправок по софт-таймауту == кол-во запросов
        });
    }

    @Test
    public void testMultipleRequests() throws InterruptedException {
        FetcherSettings settings = new FetcherSettings()
                .withConnectTimeout(Duration.ofMillis(200))
                .withRequestTimeout(Duration.ofMillis(500))
                .withRequestRetries(2)
                .withGlobalTimeout(Duration.ofSeconds(20))
                .withParallel(10)
                .withMetricRegistry(collectMetrics ? new MetricRegistry() : null);
        // половина запросов -- быстрые
        Duration smallRequestDuration = Duration.ofMillis(100);
        // половина -- медленные при первой попытке
        Duration bigRequestDuration = Duration.ofMillis(1000);

        int totalRequestCount = 20;
        List<ParsableRequest<String>> requests = IntStream.range(0, totalRequestCount)
                .mapToObj(i -> new ParsableStringRequest(i, new RequestBuilder()
                        .setUrl("http://localhost:" + nanoHTTPD.getListeningPort())
                        .addQueryParam(MyNanoHTTPD.PARAM_RESPONSE_TIME,
                                Long.toString(i < totalRequestCount / 2
                                        ? smallRequestDuration.toMillis()
                                        : bigRequestDuration.toMillis()))
                        .addQueryParam(MyNanoHTTPD.PARAM_RESPONSE_TEXT, String.format("Response %03d", i))
                        .addQueryParam(MyNanoHTTPD.PARAM_REQUEST_ID, String.format("Request %03d", i))
                        .addQueryParam(MyNanoHTTPD.PARAM_FAST_RESPONSE_ORDER_NUMBER, Integer.toString(2))
                        .build()))
                .collect(toList());

        /*
        Отправляем 10 запросов на 100мс и 10, которые сначала отвалятся по таймауту за 500мс,
        а затем выполнятся так быстро, как сможет NanoHTTPD. Итого серверного времени потребуется
        примерно 100 * 10 + 500 * 10 = 6000мс.
        Запросы шлём в 10 потоков, поэтому ожидаемые затраты реального времени: 600мс.
        Но в худшем случае, особенно в Sandbox, нам могут дать только один поток выполнения,
        потому закладываем 6000мс с небольшой дельтой.
         */
        Map<Long, Result<String>> execute;
        Duration executeDuration;
        ParallelFetcherMetrics metrics;
        try (ParallelFetcher<String> fetcher = new ParallelFetcher<>(settings, ahc)) {
            long startTime = System.nanoTime();
            execute = fetcher.execute(requests);
            executeDuration = Duration.ofNanos(System.nanoTime() - startTime);
            metrics = fetcher.getMetrics();
        }

        EntryStream.of(execute)
                .forKeyValue((id, result) -> System.out.printf("Request %03d: %s%n", id, result));

        logger.info("Execution time: {}ms", executeDuration.toMillis());
        Duration limitDuration = Duration.ofMillis(6050);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(executeDuration).isLessThan(limitDuration);
            assertMetrics(softly, metrics, new ExpectedMetrics()
                    .withRequests(30) // половину запросов отправили дважды
                    .withSuccesses(20) // первая половина с первой попытки отправилась, вторая - со второй
                    .withFailures(10) // первая попытка второй половины запросов таймаутит
                    .withRetriesOnFailure(10)); // переотправили вторую половину запросов из-за таймаута
        });
    }

    // softRetry - имеют минимальный редирект, они должны выполняться только если есть свободные слоты, и не
    // использовать
    // globalRetriesLimit понапрасну
    @Test
    public void softRetriesCountedOnlyWhenFreeSlotsExist() throws InterruptedException {
        // делаем 4 долгих запроса (дольше softTimeout-а) с параллельностью 2
        // soft-timeout-retry-ев должно быть не больше 2-х

        FetcherSettings settings = new FetcherSettings()
                .withConnectTimeout(Duration.ofMillis(500))
                .withRequestTimeout(Duration.ofMillis(5_000))
                .withSoftTimeout(Duration.ofMillis(1_000))
                .withRequestRetries(2)
                .withGlobalTimeout(Duration.ofSeconds(20))
                .withParallel(2)
                .withMetricRegistry(collectMetrics ? new MetricRegistry() : null);

        List<ParsableRequest<String>> requests = IntStream.range(0, 4)
                .mapToObj(i -> new ParsableStringRequest(i, new RequestBuilder()
                        .setUrl("http://localhost:" + nanoHTTPD.getListeningPort())
                        .addQueryParam(MyNanoHTTPD.PARAM_RESPONSE_TIME, Long.toString(Duration.ofSeconds(2).toMillis()))
                        .addQueryParam(MyNanoHTTPD.PARAM_RESPONSE_TEXT, String.format("Response %03d", i))
                        .addQueryParam(MyNanoHTTPD.PARAM_REQUEST_ID, String.format("Request %03d", i))
                        .addQueryParam(MyNanoHTTPD.PARAM_FAST_RESPONSE_ORDER_NUMBER, Integer.toString(2))
                        .build()))
                .collect(toList());

        try (ParallelFetcher<String> fetcher = new ParallelFetcher<>(settings, ahc)) {
            Map<Long, Result<String>> result = fetcher.execute(requests);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.values()).allMatch(r -> r.getSuccess() != null);
                softly.assertThat(fetcher.getGlobalRetries()).isLessThanOrEqualTo(2);
                assertMetrics(softly, fetcher.getMetrics(), new ExpectedMetrics(false)
                        // кол-во запросов: 4 основых + повторные запросы
                        .withRequests(4 + fetcher.getGlobalRetries())
                        // повторные запросы могут отработать быстрее, чем выполнится их аборт
                        .withSuccesses(4 + fetcher.getGlobalRetries())
                        // если повторные запросы успели заабортить, то они aborted
                        .withAborted(fetcher.getGlobalRetries())
                        .withSoftRetries(fetcher.getGlobalRetries()));
            });
        }
    }

    // мы не должны жрать слишком много cpu (крутиться в spin-loop-e)
    @Test
    public void cpuConsumeTest() throws Exception {
        FetcherSettings settings = new FetcherSettings()
                .withConnectTimeout(Duration.ofMillis(500))
                .withRequestTimeout(Duration.ofMillis(5_000))
                .withSoftTimeout(Duration.ofMillis(1_000))
                .withRequestRetries(2)
                .withGlobalTimeout(Duration.ofSeconds(20))
                .withParallel(2);

        List<ParsableRequest<String>> requests = IntStream.range(0, 4)
                .mapToObj(i -> new ParsableStringRequest(i, new RequestBuilder()
                        .setUrl("http://localhost:" + nanoHTTPD.getListeningPort())
                        .addQueryParam(MyNanoHTTPD.PARAM_RESPONSE_TIME, Long.toString(Duration.ofSeconds(2).toMillis()))
                        .addQueryParam(MyNanoHTTPD.PARAM_RESPONSE_TEXT, String.format("Response %03d", i))
                        .addQueryParam(MyNanoHTTPD.PARAM_REQUEST_ID, String.format("Request %03d", i))
                        .addQueryParam(MyNanoHTTPD.PARAM_FAST_RESPONSE_ORDER_NUMBER, Integer.toString(2))
                        .build()))
                .collect(toList());

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long startNanos = threadMXBean.getCurrentThreadCpuTime();
        try (ParallelFetcher<String> fetcher = new ParallelFetcher<>(settings, ahc)) {
            Map<Long, Result<String>> result = fetcher.execute(requests);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.values()).allMatch(r -> r.getSuccess() != null);
                long endNanos = threadMXBean.getCurrentThreadCpuTime();
                softly.assertThat(endNanos - startNanos).isLessThanOrEqualTo(Duration.ofMillis(50).toNanos());
            });
        }
    }

    @Test
    public void failFastTest() throws Exception {
        /*
        запускаем 4 запроса без ретраев в 1 поток;
        все запросы падают по timeOut'у через 1 секунду;
        без failFast будет выполнено 4 запроса;
        с failFast будет выполнен 1 запрос;
         */
        FetcherSettings settings = new FetcherSettings()
                .withConnectTimeout(Duration.ofMillis(500))
                .withRequestTimeout(Duration.ofMillis(1_000))
                .withRequestRetries(0)
                .withGlobalTimeout(Duration.ofSeconds(20))
                .withParallel(1)
                .withFailFast(true)
                .withMetricRegistry(collectMetrics ? new MetricRegistry() : null);


        List<ParsableRequest<String>> requests = IntStream.range(0, 4)
                .mapToObj(i -> new ParsableStringRequest(i, new RequestBuilder()
                        .setUrl("http://localhost:" + nanoHTTPD.getListeningPort())
                        .addQueryParam(MyNanoHTTPD.PARAM_RESPONSE_TIME, Long.toString(Duration.ofSeconds(2).toMillis()))
                        .addQueryParam(MyNanoHTTPD.PARAM_RESPONSE_TEXT, String.format("Response %03d", i))
                        .addQueryParam(MyNanoHTTPD.PARAM_REQUEST_ID, String.format("Request %03d", i))
                        .addQueryParam(MyNanoHTTPD.PARAM_FAST_RESPONSE_ORDER_NUMBER, Integer.toString(10))
                        .build()))
                .collect(toList());

        Duration executeDuration;
        Map<Long, Result<String>> execute;
        ParallelFetcherMetrics metrics;
        try (ParallelFetcher<String> fetcher = new ParallelFetcher<>(settings, ahc)) {
            long startTime = System.nanoTime();
            execute = fetcher.execute(requests);
            executeDuration = Duration.ofNanos(System.nanoTime() - startTime);
            metrics = fetcher.getMetrics();
        }

        EntryStream.of(execute)
                .forKeyValue((id, result) -> logger.info(String.format("Request %03d: %s", id, result)));

        logger.info("Execution time: {}ms", executeDuration.toMillis());

        int totalErrorsCount = StreamEx.ofValues(execute)
                .map(Result::getErrors)
                .mapToInt(Collection::size)
                .sum();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(totalErrorsCount).isEqualTo(4);
            Duration limitDuration = Duration.ofMillis(2000);
            softly.assertThat(executeDuration).isLessThan(limitDuration);
            // один запрос, который упал по таймауту, остальные не выполнились из-за failFast
            assertMetrics(softly, metrics, new ExpectedMetrics().withRequests(1).withFailures(1));
        });
    }

    @Test
    public void errorCodeTest() throws Exception {
        try (ParallelFetcher<String> fetcher = new ParallelFetcher<>(new FetcherSettings()
                .withMetricRegistry(collectMetrics ? new MetricRegistry() : null), ahc)) {
            Result<String> result = fetcher.execute(new ParsableStringRequest(new RequestBuilder()
                    .setUrl("http://localhost:" + nanoHTTPD.getListeningPort())
                    .addQueryParam(MyNanoHTTPD.PARAM_RESPONSE_CODE, "FORBIDDEN")
                    .addQueryParam(MyNanoHTTPD.PARAM_REQUEST_ID, "Request with error")
                    .build()));
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getSuccess()).isNull();
                softly.assertThat(result.getErrors()).hasSize(1);
                // один запрос с forbidden ответом
                assertMetrics(softly, fetcher.getMetrics(), new ExpectedMetrics().withRequests(1).withFailures(1));
            });
        }
    }

    @Test
    public void nonOkAnswersPropagation() throws Exception {
        String expectedMsg = "test error message";
        try (ParallelFetcher<String> fetcher = new ParallelFetcher<>(new FetcherSettings()
                .withMetricRegistry(collectMetrics ? new MetricRegistry() : null), ahc)) {
            Set<Integer> successResponseCodes = ImmutableSet.of(OK_200, INTERNAL_SERVER_ERROR.code());

            Result<String> result = fetcher.execute(new ParsableStringRequest(new RequestBuilder()
                    .setUrl("http://localhost:" + nanoHTTPD.getListeningPort())
                    .addQueryParam(MyNanoHTTPD.PARAM_RESPONSE_CODE, "INTERNAL_ERROR")
                    .addQueryParam(MyNanoHTTPD.PARAM_RESPONSE_TEXT, expectedMsg)
                    .addQueryParam(MyNanoHTTPD.PARAM_REQUEST_ID, "Request with error")
                    .build()) {
                @Override
                public boolean isParsableResponse(Response response) {
                    return response.hasResponseStatus() && successResponseCodes.contains(response.getStatusCode());
                }
            });
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getSuccess()).isEqualTo(expectedMsg);
                softly.assertThat(result.getErrors()).isNull();
                assertMetrics(softly, fetcher.getMetrics(), new ExpectedMetrics().withRequests(1).withSuccesses(1));
            });
        }
    }

    private void assertMetrics(SoftAssertions softly, ParallelFetcherMetrics actualMetrics,
                               ExpectedMetrics expectedMetrics) {
        LongAssert requestsAssert = softly.assertThat(actualMetrics.getRequests())
                .as("Number of requests");
        LongAssert successesAssert = softly.assertThat(actualMetrics.getSuccesses())
                .as("Number of successful requests");
        LongAssert failuresAssert = softly.assertThat(actualMetrics.getFailures())
                .as("Number of failed requests");
        LongAssert abortedAssert = softly.assertThat(actualMetrics.getAborted())
                .as("Number of aborted requests");
        LongAssert softRetriesAssert = softly.assertThat(actualMetrics.getSoftRetries())
                .as("Number of soft retries");
        LongAssert retriesOnFailureAssert = softly.assertThat(actualMetrics.getRetriesOnFailure())
                .as("Number of retries on failure");
        if (collectMetrics) {
            requestsAssert.isEqualTo(expectedMetrics.requests);
            softRetriesAssert.isEqualTo(expectedMetrics.softRetries);
            retriesOnFailureAssert.isEqualTo(expectedMetrics.retriesOnFailure);
            if (expectedMetrics.strictCompareSuccessesAndFailures) {
                successesAssert.isEqualTo(expectedMetrics.successes);
                failuresAssert.isEqualTo(expectedMetrics.failures);
                abortedAssert.isEqualTo(expectedMetrics.aborted);
            } else {
                successesAssert.isLessThanOrEqualTo(expectedMetrics.successes);
                failuresAssert.isLessThanOrEqualTo(expectedMetrics.failures);
                abortedAssert.isLessThanOrEqualTo(expectedMetrics.aborted);
            }
        } else {
            requestsAssert.isNull();
            successesAssert.isNull();
            failuresAssert.isNull();
            abortedAssert.isNull();
            softRetriesAssert.isNull();
            retriesOnFailureAssert.isNull();
        }
    }

    private static class ExpectedMetrics {
        private int requests;
        private int successes;
        private int failures;
        private int aborted;
        private int softRetries;
        private int retriesOnFailure;
        private boolean strictCompareSuccessesAndFailures;

        public ExpectedMetrics() {
            this.strictCompareSuccessesAndFailures = true;
        }

        public ExpectedMetrics(boolean strictCompareSuccessesAndFailures) {
            this.strictCompareSuccessesAndFailures = strictCompareSuccessesAndFailures;
        }

        public ExpectedMetrics withRequests(int requests) {
            this.requests = requests;
            return this;
        }

        public ExpectedMetrics withSuccesses(int successes) {
            this.successes = successes;
            return this;
        }

        public ExpectedMetrics withFailures(int failures) {
            this.failures = failures;
            return this;
        }

        public ExpectedMetrics withAborted(int aborted) {
            this.aborted = aborted;
            return this;
        }

        public ExpectedMetrics withSoftRetries(int softRetries) {
            this.softRetries = softRetries;
            return this;
        }

        public ExpectedMetrics withRetriesOnFailure(int retriesOnFailure) {
            this.retriesOnFailure = retriesOnFailure;
            return this;
        }
    }
}
