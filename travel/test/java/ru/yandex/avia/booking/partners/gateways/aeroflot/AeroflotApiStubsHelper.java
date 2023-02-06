package ru.yandex.avia.booking.partners.gateways.aeroflot;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.opentracing.mock.MockTracer;
import org.asynchttpclient.Dsl;
import org.slf4j.LoggerFactory;

import ru.yandex.avia.booking.partners.gateways.model.booking.ClientInfo;
import ru.yandex.avia.booking.tests.wiremock.Wiremock;
import ru.yandex.avia.booking.tests.wiremock.matching.WiremockStartsWithPattern;
import ru.yandex.travel.commons.jackson.MoneySerializersModule;
import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static ru.yandex.travel.testing.misc.TestResources.readResource;

class AeroflotApiStubsHelper {
    public static final String SINGLE_COMPLEX_OFFER_ID = "2ADT.1CHD-" +
            "SVO.202010021540.VVO.SU.1700.A.ABSLR-VVO.202010031840.KHV.SU.5602.M.ABSLR-" +
            "KHV.202010121915.VVO.SU.5601.M.ABSLR-VVO.202010130840.SVO.SU.1701.A.ABSLR";
    public static final String MULTIPLE_OFFERS_MAIN_ID = "1ADT-VKO.202012020830.LED.SU.6012.N.NCOR";
    public static final String MULTIPLE_OFFERS_ANOTHER_ID = "1ADT-VKO.202012020830.LED.SU.6012.N.NFOR";

    static final ObjectMapper defaultMapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerModule(new MoneySerializersModule())
            .registerModule(new JavaTimeModule());

    static AeroflotGateway defaultGateway(String bookingUrl) {
        return defaultGateway(bookingUrl, defaultAhcClient());
    }

    static AeroflotGateway defaultGateway(String bookingUrl, AsyncHttpClientWrapper ahcClientWrapper) {
        AeroflotProviderProperties config = AeroflotProviderProperties.builder()
                .authToken("integrationTestAuthToken")
                .bookingUrl(bookingUrl)
                .contentType("integrationTestContentType")
                .userName("ya_avia_test_agency")
                .userAgent("ya_avia_test_user_agent")
                .readTimeout(Duration.ofSeconds(3))
                .enableTestingScenarios(false)
                .build();
        return new AeroflotGateway(config, ahcClientWrapper, Clock.systemUTC());
    }

    static void stubRequest(@Wiremock WireMockServer wmServer, String bodyStart, String responseResource) {
        stubRequest(wmServer, readResource("__files/" + responseResource), new WiremockStartsWithPattern(bodyStart));
    }

    static void stubRequest(@Wiremock WireMockServer wmServer, String responseBody) {
        stubRequest(wmServer, responseBody, List.of());
    }

    static void stubRequest(@Wiremock WireMockServer wmServer, String responseBody, ContentPattern<?> pattern) {
        stubRequest(wmServer, responseBody, List.of(pattern));
    }

    static void stubRequest(@Wiremock WireMockServer wmServer, String responseBody, List<ContentPattern<?>> patterns) {
        MappingBuilder mb = post(urlEqualTo("/"))
                .willReturn(aResponse().withStatus(200).withBody(responseBody));
        if (patterns != null) {
            for (ContentPattern<?> pattern : patterns) {
                mb.withRequestBody(pattern);
            }
        }
        wmServer.stubFor(mb);
    }

    static AsyncHttpClientWrapper defaultAhcClient() {
        return new AsyncHttpClientWrapper(Dsl.asyncHttpClient(Dsl.config()
                .setThreadFactory(new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("aeroflotTestsAhcPool-%s")
                        .build())
                .setIoThreadsCount(1)
                .setMaxConnections(10)
                .setPooledConnectionIdleTimeout(100)
                // Check Cipher Suites: https://www.ssllabs.com/ssltest/analyze.html?d=ndc-search.aeroflot.io&latest
                // (Triggers SEC audit: nmap --script ssl-enum-ciphers -p 443 ndc-search.aeroflot.io)
                .setEnabledCipherSuites(new String[]{
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                        "TLS_RSA_WITH_AES_256_GCM_SHA384"
                })
                .build()),
                LoggerFactory.getLogger("test"),
                "test",
                new MockTracer(),
                null);
    }

    static JsonNode loadSampleTdRequestNdcV3SingleTariff() {
        return loadSampleTdRequest("aeroflot/v3/air_shopping_rs_v3_sample.xml", SINGLE_COMPLEX_OFFER_ID);
    }

    static JsonNode loadSampleTdRequestNdcV3MultipleTariffs() {
        return loadSampleTdRequest("aeroflot/v3/air_shopping_rs_v3_sample_large.xml", MULTIPLE_OFFERS_MAIN_ID);
    }

    static JsonNode loadSampleTdRequest(String airShoppingFile, String offerId) {
        try {
            JsonNode req = defaultMapper.readTree(readResource(
                    "incoming_requests/aeroflot/check_availability_td_request.json"));
            ObjectNode bookingInfo = ((ObjectNode) req.at("/order_data/booking_info"))
                    .put("AirShoppingRS", readResource(airShoppingFile));
            if (offerId != null) {
                bookingInfo.put("OfferId", offerId);
            }
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static ClientInfo testClientInfo() {
        return ClientInfo.builder().email("some@example.com").phone("791...")
                .userIp("127.0.0.1").userAgent("agent").build();
    }
}
