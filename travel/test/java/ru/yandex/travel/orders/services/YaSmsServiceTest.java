package ru.yandex.travel.orders.services;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.opentracing.util.GlobalTracer;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class YaSmsServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(YaSmsServiceTest.class);
    public static final String PHONE_NUMBER = "89119110000";

    private YaSmsService service;
    private YaSmsConfigurationProperties configurationProperties;
    private AsyncHttpClientWrapper clientWrapper;
    private AsyncHttpClient testClient;

    @Rule
    public WireMockRule wireMockRule
            = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Before
    public void setUp() {
        configurationProperties = new YaSmsConfigurationProperties();
        configurationProperties.setBaseUrl(String.format("http://localhost:%d", wireMockRule.port()));
        configurationProperties.setTimeout(Duration.ofSeconds(10));
        configurationProperties.setSender("travel");
        testClient = Dsl.asyncHttpClient(
                Dsl.config().setThreadFactory(new ThreadFactoryBuilder().setDaemon(true).build()
                ).build());
        clientWrapper = new AsyncHttpClientWrapper(
                testClient, logger, "testDestination", GlobalTracer.get(),
                Set.of("/sendsms")
        );
        service = new YaSmsService(configurationProperties, clientWrapper, null);
    }

    @After
    public void tearDown() throws Exception {
        testClient.close();
    }

    @Test
    public void testSendSms() {
        stubFor(get(anyUrl()).willReturn(aResponse()
                .withBody("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<doc>\n" +
                        "    <message-sent id=\"msg-id\" />\n" +
                        "</doc>\n" +
                        "            ")));
        var result = service.sendSms("tchik-tchirik", PHONE_NUMBER);

        verify(getRequestedFor(urlPathEqualTo("/sendsms"))
                .withQueryParam("text", equalTo("tchik-tchirik"))
                .withQueryParam("phone", equalTo(PHONE_NUMBER))
                .withQueryParam("sender", equalTo("travel")));
        assertThat(result).isEqualTo("msg-id");
    }

    @Test
    public void testBlackList() {
        configurationProperties.setIgnore(new YaSmsConfigurationProperties.IgnoreConfig(
                YaSmsConfigurationProperties.IgnoreMode.BLACK_LIST,
                List.of(PHONE_NUMBER)
        ));

        var result = service.sendSms("tchik-tchirik", PHONE_NUMBER);

        verify(exactly(0), getRequestedFor(urlPathEqualTo("/sendsms")));
        assertThat(result).isNull();
    }

    @Test
    public void testWhiteList() {
        configurationProperties.setIgnore(new YaSmsConfigurationProperties.IgnoreConfig(
                YaSmsConfigurationProperties.IgnoreMode.WHITE_LIST,
                List.of()
        ));

        var result = service.sendSms("tchik-tchirik", PHONE_NUMBER);

        verify(exactly(0), getRequestedFor(urlPathEqualTo("/sendsms")));
        assertThat(result).isNull();
    }

    @Test
    public void testSendSmsError() {
        stubFor(get(anyUrl()).willReturn(aResponse()
                .withBody("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<doc>\n" +
                        "    <errorcode>9999</errorcode><error>testyasmserror</error>\n" +
                        "</doc>\n ")));

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> service.sendSms("tchik-tchirik", PHONE_NUMBER))
                .withMessageContaining("Code 9999")
                .withMessageContaining("testyasmserror");
    }
}
