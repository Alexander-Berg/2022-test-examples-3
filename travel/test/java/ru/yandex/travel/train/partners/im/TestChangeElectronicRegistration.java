package ru.yandex.travel.train.partners.im;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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
import ru.yandex.travel.train.partners.im.model.ElectronicRegistrationRequest;
import ru.yandex.travel.train.partners.im.model.ImBlankStatus;
import ru.yandex.travel.train.partners.im.model.PendingElectronicRegistration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

public class TestChangeElectronicRegistration {
    private static final Logger logger = LoggerFactory.getLogger(TestReservationConfirm.class);
    private ImClient client;

    @Rule
    public WireMockRule wireMockRule
            = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Before
    public void setUp() {
        var testClient = Dsl.asyncHttpClient(Dsl.config()
                .setThreadPoolName("imAhcPool")
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
    public void testChangeElectronicRegistration() {
        stubFor(post(anyUrl()).willReturn(aResponse()
                .withBody(TestResources.readResource("ElectronicRegistrationResponse.json"))));

        var request = new ElectronicRegistrationRequest();
        request.setOrderItemId(52159);
        request.setOrderItemBlankIds(List.of(51946));
        request.setSet(true);
        request.setSendNotification(false);

        var response = client.changeElectronicRegistration(request);

        verify(postRequestedFor(urlPathEqualTo("/Railway/V1/Reservation/ElectronicRegistration"))
                .withRequestBody(equalToJson(TestResources.readResource("ElectronicRegistrationRequest.json")
        )));

        assertThat(response.getExpirationElectronicRegistrationDateTime()).isEqualTo(
                LocalDateTime.of(2019, 8, 29, 5, 20));
        assertThat(response.getBlanks().size()).isEqualTo(1);

        var blank = response.getBlanks().get(0);
        assertThat(blank.getOrderItemBlankId()).isEqualTo(51946);
        assertThat(blank.getNumber()).isEqualTo("71234567890000");
        assertThat(blank.getBlankStatus()).isEqualTo(ImBlankStatus.REMOTE_CHECK_IN);
        assertThat(blank.getPendingElectronicRegistration()).isEqualTo(PendingElectronicRegistration.TO_CANCEL);
        assertThat(blank.getSignSequence()).isNull();
    }
}
