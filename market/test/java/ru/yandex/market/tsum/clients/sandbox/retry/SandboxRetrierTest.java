package ru.yandex.market.tsum.clients.sandbox.retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import io.netty.handler.codec.http.HttpMethod;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.market.request.netty.JsonNettyHttpClient;
import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.request.netty.WrongStatusCodeException;
import ru.yandex.market.request.netty.auth.OAuthProvider;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.tsum.clients.sandbox.SandboxTask;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.junit.Assert.assertThat;

@ParametersAreNonnullByDefault
public class SandboxRetrierTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    public static final int NOT_FOUND = 404;
    public static final int TOO_MANY_REQUESTS = 429;
    private static final Set<Integer> RETRYABLE_ERRORS = Collections.singleton(TOO_MANY_REQUESTS);
    private static final int[] RETRYABLE_ERRORS_RETRY_DELAYS = {1, 2, 5, 10, 30, 60, 120, 300};
    private static final int RETRYABLE_ERRORS_MAX_ATTEMPTS = 20;
    private static final int TOO_MANY_REQUESTS_SUCCESSFUL_ATTEMPTS_BEFORE_SUCCESS = 7;
    private static final List<Duration> TOO_MANY_REQUESTS_SUCCESSFUL_SLEEP_DURATIONS =
        Stream.of(1, 2, 5, 10, 30, 60, 120)
            .map(Duration::ofSeconds)
            .collect(Collectors.toList());
    private static final List<Duration> TOO_MANY_REQUESTS_UNSUCCESSFUL_SLEEP_DURATIONS;

    static {
        Stream<Integer> seconds = Stream.concat(
            // здесь 7 значений:
            Stream.of(1, 2, 5, 10, 30, 60, 120),

            // с этими 12 получается 19: оно делает 19 попыток, после каждой спит,
            // а когда проваливается двадцатая, немедленно порождает исключение, ещё раз не спит
            Stream.iterate(300, n -> n).limit(12)
        );

        TOO_MANY_REQUESTS_UNSUCCESSFUL_SLEEP_DURATIONS = seconds.map(Duration::ofSeconds)
            .collect(Collectors.toList());
    }

    private static final long TASK_ID = 9876543210L;

    private SandboxRetrier retrier;
    private List<Duration> retryDelays;

    @Before
    public void setUp() throws Exception {
        retryDelays = new ArrayList<>();
        retrier = new SandboxRetrier(
            () -> new SandboxRetryPolicy(RETRYABLE_ERRORS, RETRYABLE_ERRORS_RETRY_DELAYS,
                RETRYABLE_ERRORS_MAX_ATTEMPTS),
            () -> duration -> retryDelays.add(duration));
    }

    @Test
    public void retriesIfTooManyRequests() {
        String scenario = "retriesIfTooManyRequests";

        wireMockRule.stubFor(get(urlPathEqualTo("/api/v1.0/task/" + TASK_ID))
            .inScenario(scenario)
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse().withStatus(TOO_MANY_REQUESTS).withBody("too many requests"))
            .willSetStateTo("After attempt 1"));

        for (int i = 1; i <= TOO_MANY_REQUESTS_SUCCESSFUL_ATTEMPTS_BEFORE_SUCCESS; i++) {
            wireMockRule.stubFor(get(urlPathEqualTo("/api/v1.0/task/" + TASK_ID))
                .inScenario(scenario)
                .whenScenarioStateIs("After attempt " + i)
                .willReturn(aResponse().withStatus(TOO_MANY_REQUESTS).withBody("too many requests"))
                .willSetStateTo("After attempt " + (i + 1)));
        }

        wireMockRule.stubFor(get(urlPathEqualTo("/api/v1.0/task/" + TASK_ID))
            .inScenario(scenario)
            .whenScenarioStateIs("After attempt " + TOO_MANY_REQUESTS_SUCCESSFUL_ATTEMPTS_BEFORE_SUCCESS)
            .willReturn(ok(String.format("{\"items\": [%s]}", getTaskJson())))
            .willSetStateTo("DONE"));

        wireMockRule.stubFor(get(urlPathEqualTo("/api/v1.0/task/" + TASK_ID + "/audit"))
            .inScenario(scenario)
            .willReturn(ok("[]")));

        requestSandboxTaskWithRetries();

        assertThat(retryDelays, Matchers.equalTo(TOO_MANY_REQUESTS_SUCCESSFUL_SLEEP_DURATIONS));
    }

    @Test
    public void failsIfTooManyRequestsForTooLong() {
        wireMockRule.stubFor(get(urlPathEqualTo("/api/v1.0/task/" + TASK_ID))
            .willReturn(aResponse().withStatus(TOO_MANY_REQUESTS).withBody("too many requests")));

        try {
            requestSandboxTaskWithRetries();
            Assert.fail("pollTask should have thrown an exception");
        } catch (TooManyRetriesException e) {
            assertThat(e.getCause(), Matchers.instanceOf(WrongStatusCodeException.class));
            assertThat(((WrongStatusCodeException) e.getCause()).getHttpCode(), Matchers.equalTo(TOO_MANY_REQUESTS));
        }

        assertThat(retryDelays, Matchers.equalTo(TOO_MANY_REQUESTS_UNSUCCESSFUL_SLEEP_DURATIONS));
    }

    @Test
    public void failsIfNonRetryableResponseCode() {
        wireMockRule.stubFor(get(urlPathEqualTo("/api/v1.0/task/" + TASK_ID))
            .willReturn(aResponse().withStatus(NOT_FOUND).withBody("too many requests")));

        try {
            requestSandboxTaskWithRetries();
            Assert.fail("pollTask should have thrown an exception");
        } catch (WrongStatusCodeException e) {
            assertThat(e.getHttpCode(), Matchers.equalTo(NOT_FOUND));
        }

        assertThat(retryDelays, Matchers.empty());
    }

    private static String getTaskJson() {
        return String.format("{\"id\": %d, \"status\": \"SUCCESS\"}", TASK_ID);
    }

    private SandboxTask requestSandboxTask() {
        JsonNettyHttpClient httpClient = new JsonNettyHttpClient(wireMockRule.url("/api/v1.0"), new OAuthProvider(
            "TOKEN"),
            Module.SANDBOX, (NettyHttpClientContext) null, new Gson());
        return httpClient.executeRequest(HttpMethod.GET, "/task/" + TASK_ID, null, SandboxTask.class);
    }

    private void requestSandboxTaskWithRetries() {
        retrier.tryMultipleTimes(this::requestSandboxTask);
    }
}
