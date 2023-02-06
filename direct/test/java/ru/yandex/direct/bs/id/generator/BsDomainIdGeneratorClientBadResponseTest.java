package ru.yandex.direct.bs.id.generator;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.JUnitSoftAssertions;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.inside.passport.tvm2.TvmHeaders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.tvm.TvmService.BS_ID_GENERATOR_TEST;

class BsDomainIdGeneratorClientBadResponseTest {

    public JUnitSoftAssertions softAssertions = new JUnitSoftAssertions();

    private BsDomainIdGeneratorClient client;

    private static final String TVM_TICKET = "test_ticket";
    private static final Long START_DOMAIN_ID = 1356L;

    @BeforeEach
    public void before() throws IOException {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher());
        mockWebServer.start();

        var tvmIntegration = mock(TvmIntegration.class);
        when(tvmIntegration.getTicket(eq(BS_ID_GENERATOR_TEST))).thenReturn(TVM_TICKET);
        var config = new BsDomainIdGeneratorClientConfig.Builder()
                .withUrl("http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort())
                .withTvmId(BS_ID_GENERATOR_TEST.getId())
                .withRequestTimeout(Duration.ofSeconds(10))
                .withConnectTimeout(Duration.ofSeconds(10))
                .withRequestRetries(1)
                .build();
        client = new BsDomainIdGeneratorClient(config, new DefaultAsyncHttpClient(), tvmIntegration);
    }

    @Test
    void responseSizeNotEqualToRequestSizeTest() {
        softAssertions.assertThatThrownBy(() -> client.generate(List.of("ozon.ru", "yandex.ru", "mail.ru")))
                .isInstanceOf(BsDomainIdGeneratorException.class);
    }

    private Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                var domainId = new AtomicLong(START_DOMAIN_ID);
                softAssertions.assertThat(request.getMethod()).isEqualTo("POST");
                softAssertions.assertThat(request.getHeader("Content-type")).isEqualTo("application/json");
                softAssertions.assertThat(TvmHeaders.SERVICE_TICKET).isEqualTo(TVM_TICKET);
                softAssertions.assertThat(request.getPath()).isEqualTo("/domain");
                var body = request.getBody().readString(Charset.defaultCharset());
                BsDomainIdGeneratorRequest bsDomainRequest = JsonUtils.fromJson(body, BsDomainIdGeneratorRequest.class);
                // вернем id'шников на один меньше, чем доменов
                var domainIds = IntStream.of(bsDomainRequest.getDomains().size() - 1)
                        .mapToLong(i -> domainId.incrementAndGet())
                        .boxed()
                        .collect(Collectors.toList());
                var response = new BsDomainIdGeneratorResponse(domainIds);
                return new MockResponse()
                        .setBody(JsonUtils.toJson(response));
            }
        };
    }
}
