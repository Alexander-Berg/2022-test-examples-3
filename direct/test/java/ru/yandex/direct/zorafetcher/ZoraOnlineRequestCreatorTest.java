package ru.yandex.direct.zorafetcher;

import java.time.Duration;
import java.util.Objects;

import org.assertj.core.api.SoftAssertions;
import org.asynchttpclient.Request;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.tracing.util.TraceUtil;

import static org.assertj.core.api.Assertions.assertThat;

public class ZoraOnlineRequestCreatorTest {

    private static final String TVM_TICKET = "ticket";
    private static final String ZORA_HOST = "zora";
    private static final int ZORA_PORT = 8166;
    private static final String ZORA_SOURCE_NAME = "some source name";

    private static final String URL_WITH_HTTP = "http://url";
    private static final String URL_WITH_HTTPS = "https://url";
    private static final String URL_WITH_HTTPS_AND_TRAILING_SPACES = " https://url ";
    private static final String URL_WITH_HTTPS_IN_UPPERCASE = "HTTPS://url ";

    private static final String TRACE_HEADER_VALUE = "trace-header-value";
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(5);

    private ZoraOnlineRequestCreator zoraOnlineRequestCreator;

    @Before
    public void before() {
        zoraOnlineRequestCreator = new ZoraOnlineRequestCreator(ZORA_HOST, ZORA_PORT, ZORA_SOURCE_NAME);
    }

    @Test
    public void createGetRequest_UrlWithHttp_IsPassedUnchanged() {
        Request request = zoraOnlineRequestCreator.createGetRequest(URL_WITH_HTTP, TRACE_HEADER_VALUE, RESPONSE_TIMEOUT,
                TVM_TICKET, true, null);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(request).hasFieldOrPropertyWithValue("url", URL_WITH_HTTP);
            softly.assertThat(httpsHeaderIsSet(request)).as("https header is not set").isFalse();
        });
    }

    @Test
    public void createGetRequest_UrlWithHttps_IsPassedAsHttpWithHttpsHeader() {
        Request request = zoraOnlineRequestCreator.createGetRequest(URL_WITH_HTTPS, TRACE_HEADER_VALUE, RESPONSE_TIMEOUT,
                TVM_TICKET, true, null);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(request).hasFieldOrPropertyWithValue("url", URL_WITH_HTTP);
            softly.assertThat(httpsHeaderIsSet(request)).as("https header is set").isTrue();
        });
    }

    @Test
    public void createGetRequest_UrlWithHttpsAndTrailingSpaces_IsPassedAsHttpWithHttpsHeader() {
        Request request = zoraOnlineRequestCreator.createGetRequest(URL_WITH_HTTPS_AND_TRAILING_SPACES, TRACE_HEADER_VALUE,
                RESPONSE_TIMEOUT, TVM_TICKET, true, null);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(request).hasFieldOrPropertyWithValue("url", URL_WITH_HTTP);
            softly.assertThat(httpsHeaderIsSet(request)).as("https header is set").isTrue();
        });
    }

    @Test
    public void createGetRequest_UrlWithHttpsInUppercase_IsPassedAsHttpWithHttpsHeader() {
        Request request = zoraOnlineRequestCreator.createGetRequest(URL_WITH_HTTPS_IN_UPPERCASE, TRACE_HEADER_VALUE,
                RESPONSE_TIMEOUT, TVM_TICKET, true, null);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(request).hasFieldOrPropertyWithValue("url", URL_WITH_HTTP);
            softly.assertThat(httpsHeaderIsSet(request)).as("https header is set").isTrue();
        });
    }

    @Test
    public void createGetRequest_CheckTraceHeaderValue() {
        Request request = zoraOnlineRequestCreator.createGetRequest(URL_WITH_HTTP, TRACE_HEADER_VALUE, RESPONSE_TIMEOUT,
                TVM_TICKET, true, null);
        assertThat(request.getHeaders().get(TraceUtil.X_YANDEX_TRACE))
                .as("Указан правильный заголовок трассировки")
                .isEqualTo(TRACE_HEADER_VALUE);
    }

    @Test
    public void createGetRequest_CheckTimeoutHeaderValue() {
        Request request = zoraOnlineRequestCreator.createGetRequest(URL_WITH_HTTP, TRACE_HEADER_VALUE, RESPONSE_TIMEOUT,
                TVM_TICKET, true, null);
        assertThat(request.getHeaders().get(ZoraOnlineRequestCreator.X_YANDEX_RESPONSE_TIMEOUT))
                .as("Указан правильный заголовок")
                .isEqualTo(String.valueOf(RESPONSE_TIMEOUT.getSeconds()));
    }

    @Test
    public void createGetRequest_CheckRequestTimeoutValue() {
        Request request = zoraOnlineRequestCreator.createGetRequest(URL_WITH_HTTP, TRACE_HEADER_VALUE, RESPONSE_TIMEOUT,
                TVM_TICKET, true, null);
        assertThat(request.getRequestTimeout())
                .as("Указан правильный таймаут запроса в zora")
                .isEqualTo((int) RESPONSE_TIMEOUT.plusSeconds(1).toMillis());
    }

    @Test
    public void createGetRequest_CheckOtherHeaderIsSet() {
        Request request = zoraOnlineRequestCreator.createGetRequest(URL_WITH_HTTP, TRACE_HEADER_VALUE, RESPONSE_TIMEOUT,
                TVM_TICKET, true, null);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(request.getHeaders().get(ZoraOnlineRequestCreator.X_YANDEX_SOURCENAME))
                    .isEqualTo(ZORA_SOURCE_NAME);
            softly.assertThat(request.getHeaders().get(ZoraOnlineRequestCreator.X_YANDEX_REQUESTTYPE))
                    .isEqualTo(ZoraOnlineRequestCreator.REQUEST_TYPE_USERPROXY);
            softly.assertThat(request.getHeaders().get(ZoraOnlineRequestCreator.X_YANDEX_REDIRS))
                    .isEqualTo("1");
        });
    }

    @Test
    public void createGetRequest_CheckTvmTicketHeaderValue() {
        Request request = zoraOnlineRequestCreator.createGetRequest(URL_WITH_HTTP, TRACE_HEADER_VALUE, RESPONSE_TIMEOUT,
                TVM_TICKET, true, null);
        assertThat(request.getHeaders().get(ZoraOnlineRequestCreator.X_YANDEX_SERVICE_TICKET))
                .as("Указан правильный заголовок для tvm тикета")
                .isEqualTo(TVM_TICKET);
    }

    private boolean httpsHeaderIsSet(Request req) {
        return Objects.equals(req.getHeaders().get(ZoraOnlineRequestCreator.X_YANDEX_USE_HTTPS), "1");
    }
}
