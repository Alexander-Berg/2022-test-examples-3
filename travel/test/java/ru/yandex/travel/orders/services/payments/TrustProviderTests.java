package ru.yandex.travel.orders.services.payments;

import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.opentracing.util.GlobalTracer;
import org.assertj.core.api.Assertions;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.travel.commons.http.apiclient.HttpApiRetryableException;
import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

public class TrustProviderTests {
    private static final Logger logger = LoggerFactory.getLogger(TrustProviderTests.class);

    private AsyncHttpClientWrapper clientWrapper;
    private TrustClient subject;

    @Rule
    public WireMockRule wireMockRule =
            new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());
    private AsyncHttpClient testClient;

    @Before
    public void setUp() {
        testClient = Dsl.asyncHttpClient(Dsl.config()
                .setThreadFactory(new ThreadFactoryBuilder().setDaemon(true).build())
                .build());
        clientWrapper = new AsyncHttpClientWrapper(
                testClient, logger, "testDestination", GlobalTracer.get(),
                Arrays.stream(DefaultTrustClient.Method.values()).map(Enum::name).collect(Collectors.toSet())
        );
        TrustConnectionProperties connectionProperties = new TrustConnectionProperties();
        connectionProperties.setBaseUrl("http://localhost:" + wireMockRule.port());
        connectionProperties.setHttpReadTimeout(Duration.ofSeconds(1));
        connectionProperties.setHttpRequestTimeout(Duration.ofSeconds(1));
        subject = new DefaultTrustClient(clientWrapper, connectionProperties, "test_token");
    }

    @After
    public void tearDown() throws Exception {
        testClient.close();
    }

    @Test
    public void testCreated() {
        stubFor(get(urlPathMatching(".*")).willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));
        Assertions.assertThatExceptionOfType(HttpApiRetryableException.class).isThrownBy(
                () -> subject.getBasketStatus("trust_purchase_token", new TrustUserInfo("uid", "127.0.0.1"))
        );

    }
}
