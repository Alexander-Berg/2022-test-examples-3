package ru.yandex.travel.train.partners.im;

import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.opentracing.mock.MockTracer;
import org.asynchttpclient.Dsl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.testing.misc.TestResources;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

public class TestReservationCancel {
    private static final Logger logger = LoggerFactory.getLogger(TestReservationConfirm.class);
    private ImClient client;

    @Rule
    public WireMockRule wireMockRule
            = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Before
    public void setUp() {
        var testClient = Dsl.asyncHttpClient(Dsl.config()
                .setThreadPoolName("expediaAhcPool")
                .setThreadFactory(new ThreadFactoryBuilder().setDaemon(true).build())
                .build());
        var clientWrapper = new AsyncHttpClientWrapper(
                testClient, logger, "testDestination", new MockTracer(),
                Arrays.stream(DefaultImClient.Method.values()).map(Enum::name).collect(Collectors.toSet())
        );
        client = new DefaultImClient(clientWrapper, "pos", Duration.ofSeconds(10),
                String.format("http://localhost:%d/", wireMockRule.port()),"ya", "***");
    }

    @Test
    public void testReservationConfirm(){
        stubFor(post(anyUrl()).willReturn(aResponse().withBody("{}")));

        client.reservationCancel(1111100);

        verify(postRequestedFor(urlPathEqualTo("/Order/V1/Reservation/Cancel")).withRequestBody(equalToJson(
                TestResources.readResource("ReservationCancelRequest.json")
        )));
    }
}
