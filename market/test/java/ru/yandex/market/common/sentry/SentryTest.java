package ru.yandex.market.common.sentry;


import com.github.tomakehurst.wiremock.WireMockServer;
import io.sentry.Sentry;
import io.sentry.SentryClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import ru.yandex.common.util.URLUtils;
import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.common.test.mockito.MockitoTestExecutionListener;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringJUnitConfig(classes = SentryTestConfig.class)
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        MockitoTestExecutionListener.class
})

@ActiveProfiles("functionalTest")
public class SentryTest {
    @Value("${sentry.token}")
    private String token;

    @Value("${sentry.enable:false}")
    private boolean isEnabled;

    @Value("${sentry.dsn}")
    private String datasourceHostName;

    @Value("${sentry.async:true}")
    private boolean isAsync;

    @Value("${sentry.messagelength:100000}")
    private int messageLength;

    @Value("${sentry.timeout:20000}")
    private int timeout;

    @Value("${sentry.scheme:https}")
    private String scheme;

    private static final String EXPECTED_TOKEN = "mock";

    @Autowired
    SentryInitializer sentry;

    @Test
    @DisplayName("Проверка того что url создается корректно, подгружая данные из пропертей")
    void buildUrlTest() {
        Assertions.assertTrue(sentry.getUrl().contains(scheme + "://" + token + "@" + datasourceHostName));
    }

    @Test
    @DisplayName("Проверка того что builder устанавливает правильные настройки для соединения для Sentry")
    void sentryCreateSetupTest() {
        WireMockServer wireMockServer = new WireMockServer(options()
                .dynamicPort()
        );
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
        SentryInitializer sentryInitializer = new SentryInitializer.Builder()
                .setScheme(URLUtils.SCHEME_HTTP)
                .setToken(EXPECTED_TOKEN)
                .setEnabled(true)
                .setDsn("localhost:" + wireMockServer.port() + "/111")
                .setAsync(false)
                .setMessageLength(100000)
                .setTimeout(20000)
                .setEnvironmentType(EnvironmentType.TESTING)
                .build();


        sentryInitializer.initializeSentry();
        SentryClient sentryClient = Sentry.getStoredClient();
        sentryClient.sendMessage("msg");

        verify(postRequestedFor(urlEqualTo("/api/111/store/"))
                .withHeader("X-Sentry-Auth", containing("sentry_key=" + EXPECTED_TOKEN))
                .withRequestBody(containing("\"message\":\"msg\"")));
    }

}
