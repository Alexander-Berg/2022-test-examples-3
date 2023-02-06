package ru.yandex.market.jmf.module.automation.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.inject.Named;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.crm.util.Exceptions;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.http.HttpHeaderNames;
import ru.yandex.market.jmf.module.def.AllowedOutgoingTvmService;
import ru.yandex.market.jmf.tvm.support.TvmService;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.request.trace.RequestTraceUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.jmf.module.http.support.ModuleHttpSupportConfiguration.HTTP_SUPPORT_REST_TEMPLATE;
import static ru.yandex.market.jmf.module.http.support.ModuleHttpSupportConfiguration.HTTP_SUPPORT_REST_TEMPLATE_WITH_PROCESS_REDIRECT;

public class HttpRequestRuleActionTest extends AbstractAutomationRuleTest {
    private final RestTemplate restTemplate;
    private final TvmService tvmService;
    private final RestTemplate restTemplateWithProcessRedirect;

    public HttpRequestRuleActionTest(
            @Named(HTTP_SUPPORT_REST_TEMPLATE) RestTemplate restTemplate,
            @Named(HTTP_SUPPORT_REST_TEMPLATE_WITH_PROCESS_REDIRECT) RestTemplate restTemplatewithProcessRedirect,
            TvmService tvmService
    ) {
        this.restTemplate = restTemplate;
        this.tvmService = tvmService;
        this.restTemplateWithProcessRedirect = restTemplatewithProcessRedirect;
    }

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        when(tvmService.getTicket(any(String.class))).thenReturn("test");
    }

    @AfterEach
    @Override
    public void tearDown() {
        super.tearDown();
        reset(restTemplate);
        reset(tvmService);
    }

    @Test
    public void explicitWithRedirectHttpRequest() throws IOException {
        InputStream responseBodyStream = new ByteArrayInputStream(
                "{\"data\": {\"key\": \"value\"}, \"array\": [{\"key\": \"value1\"},{\"key\": \"value2\"}]}"
                        .getBytes(StandardCharsets.UTF_8)
        );
        var httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
        httpHeaders.add(HttpHeaders.LOCATION, "http://example2.com");
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getRawStatusCode()).thenReturn(301);
        when(response.getBody()).thenReturn(responseBodyStream);
        when(response.getHeaders()).thenReturn(httpHeaders);
        prepareMocks("http://example.com", HttpMethod.GET, response);

        var httpHeaders2 = new HttpHeaders();
        httpHeaders2.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
        ClientHttpResponse response2 = mock(ClientHttpResponse.class);
        when(response2.getRawStatusCode()).thenReturn(200);
        when(response2.getBody()).thenReturn(responseBodyStream);
        when(response2.getHeaders()).thenReturn(httpHeaders2);
        prepareMocks("http://example2.com", HttpMethod.GET, response2);

        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event" +
                "/explicitWithRedirectHttpRequestAction.json");
        startTrigger(entity1, entity2);

        verify(restTemplateWithProcessRedirect, times(1)).execute(
                eq("http://example.com"),
                eq(HttpMethod.GET),
                any(),
                any()
        );

        verify(restTemplateWithProcessRedirect, times(0)).execute(
                eq("http://example2.com"),
                eq(HttpMethod.GET),
                any(),
                any()
        );
    }

    @Test
    public void explicitWithoutRedirectHttpRequest() throws IOException {
        InputStream responseBodyStream = new ByteArrayInputStream(
                "{\"data\": {\"key\": \"value\"}, \"array\": [{\"key\": \"value1\"},{\"key\": \"value2\"}]}"
                        .getBytes(StandardCharsets.UTF_8)
        );
        var httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
        httpHeaders.add(HttpHeaders.LOCATION, "http://example2.com");
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getRawStatusCode()).thenReturn(301);
        when(response.getBody()).thenReturn(responseBodyStream);
        when(response.getHeaders()).thenReturn(httpHeaders);
        prepareMocks("http://example.com", HttpMethod.GET, response);

        var httpHeaders2 = new HttpHeaders();
        httpHeaders2.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
        ClientHttpResponse response2 = mock(ClientHttpResponse.class);
        when(response2.getRawStatusCode()).thenReturn(200);
        when(response2.getBody()).thenReturn(responseBodyStream);
        when(response2.getHeaders()).thenReturn(httpHeaders2);
        prepareMocks("http://example2.com", HttpMethod.GET, response2);

        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event" +
                "/explicitWithoutRedirectHttpRequestAction" +
                ".json");
        startTrigger(entity1, entity2);

        verify(restTemplate, times(1)).execute(
                eq("http://example.com"),
                eq(HttpMethod.GET),
                any(),
                any()
        );

        verify(restTemplate, times(0)).execute(
                eq("http://example2.com"),
                eq(HttpMethod.GET),
                any(),
                any()
        );
    }

    @Test
    public void explicitNoAuthHttpRequest() throws IOException {
        ClientHttpRequest request = prepareMocks("https://example.com", HttpMethod.POST);
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/explicitNoAuthHttpRequestAction" +
                ".json");
        startTrigger(entity1, entity2);

        verify(restTemplate, times(1)).execute(
                eq("https://example.com"),
                eq(HttpMethod.POST),
                any(),
                any()
        );

        verify(request.getBody(), times(1)).write(
                ArgumentMatchers.argThat(argument -> Arrays.mismatch(
                        argument,
                        "{\"my\":\"body\",\"number\":124}".getBytes(StandardCharsets.UTF_8)
                ) == 26),
                eq(0),
                eq(26)
        );

        checkHeaders(
                request,
                Map.of("X-Some-Header", "value"),
                MediaType.APPLICATION_JSON,
                1
        );
    }

    @Test
    public void testJsonBodyTypeHttpRequest() throws IOException {
        ClientHttpRequest request = prepareMocks("https://example.com", HttpMethod.POST);
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/jsonBodyTypeHttpRequestAction.json");
        startTrigger(entity1, entity2);

        verify(restTemplate, times(1)).execute(
                eq("https://example.com"),
                eq(HttpMethod.POST),
                any(),
                any()
        );

        verifyBodyWritten(request, "{\"my\":\"body\",\"number\":124}");

        checkHeaders(
                request,
                Map.of("X-Some-Header", "value"),
                MediaType.APPLICATION_JSON,
                1
        );
    }

    @Test
    public void testUrlEncodedBodyTypeHttpRequest() throws IOException {
        ClientHttpRequest request = prepareMocks("https://example.com", HttpMethod.POST);
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/urlEncodedBodyTypeHttpRequestAction" +
                ".json");
        startTrigger(entity1, entity2);

        verify(restTemplate, times(1)).execute(
                eq("https://example.com"),
                eq(HttpMethod.POST),
                any(),
                any()
        );

        verifyBodyWritten(request, "key=value&encoded%26Key=value&encodedValue=val%26ue");

        checkHeaders(
                request,
                Map.of("X-Some-Header", "value"),
                MediaType.APPLICATION_FORM_URLENCODED,
                1
        );
    }

    @Test
    public void testUrlEncodedBodyTypeAndUrlEncodedAuthorizationHttpRequest() throws IOException {
        ClientHttpRequest request = prepareMocks("https://example.com", HttpMethod.POST);
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event" +
                "/urlEncodedBodyTypeAndUrlEncodedAuthorizationHttpRequestAction.json");
        startTrigger(entity1, entity2);

        verify(restTemplate, times(1)).execute(
                eq("https://example.com"),
                eq(HttpMethod.POST),
                any(),
                any()
        );

        verifyBodyWritten(request, "auth=psswd");

        checkHeaders(
                request,
                Map.of("X-Some-Header", "value"),
                MediaType.APPLICATION_FORM_URLENCODED,
                1
        );
    }

    @Test
    public void implicitNoAuthHttpRequest() throws IOException {
        ClientHttpRequest request = prepareMocks("https://example.com", HttpMethod.OPTIONS);
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/implicitNoAuthHttpRequestAction" +
                ".json");
        startTrigger(entity1, entity2);

        verify(restTemplate, times(1)).execute(
                eq("https://example.com"),
                eq(HttpMethod.OPTIONS),
                any(),
                any()
        );

        verifyNoInteractions(request.getBody());

        checkHeaders(request, Map.of());
    }

    @Test
    public void storeJsonVariables() throws IOException {
        InputStream responseBodyStream = new ByteArrayInputStream(
                "{\"data\": {\"key\": \"value\"}, \"array\": [{\"key\": \"value1\"},{\"key\": \"value2\"}]}"
                        .getBytes(StandardCharsets.UTF_8)
        );
        var httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getRawStatusCode()).thenReturn(200);
        when(response.getBody()).thenReturn(responseBodyStream);
        when(response.getHeaders()).thenReturn(httpHeaders);
        prepareMocks("https://example.com", HttpMethod.GET, response);
        ClientHttpRequest request = prepareMocks("https://example.com/test", HttpMethod.POST);
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/storeJsonVariablesHttpRequestAction" +
                ".json");
        startTrigger(entity1, entity2);

        Assertions.assertEquals((Object) 200L, entity1.getAttribute("responseStatus"));
        Assertions.assertEquals("value", entity1.getAttribute("dataKey"));
        Assertions.assertEquals(List.of("value1", "value2"), entity1.getAttribute("arrayKeys"));

        verify(restTemplate, times(1)).execute(
                eq("https://example.com/test"),
                eq(HttpMethod.POST),
                any(),
                any()
        );

        var body = "{\"responseStatus\": 200, \"dataKey\": \"value\", \"arrayKeys\": [\"value1\",\"value2\"]}";
        verifyBodyWritten(request, body);

        checkHeaders(request, Map.of(), MediaType.APPLICATION_JSON, 1);
    }

    private void verifyBodyWritten(ClientHttpRequest request, String body) throws IOException {
        var bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        verify(request.getBody(), times(1)).write(
                ArgumentMatchers.argThat(argument -> Arrays.mismatch(
                        argument,
                        bodyBytes
                ) == bodyBytes.length),
                eq(0),
                eq(bodyBytes.length)
        );
    }

    @Test
    public void storeInvalidHeaderJsonVariables() throws IOException {
        InputStream responseBodyStream = new ByteArrayInputStream(
                "{\"data\": {\"key\": \"value\"}, \"array\": [{\"key\": \"value1\"},{\"key\": \"value2\"}]}"
                        .getBytes(StandardCharsets.UTF_8)
        );
        var httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getRawStatusCode()).thenReturn(200);
        when(response.getBody()).thenReturn(responseBodyStream);
        when(response.getHeaders()).thenReturn(httpHeaders);
        prepareMocks("https://example.com", HttpMethod.GET, response);
        ClientHttpRequest request = prepareMocks("https://example.com/test", HttpMethod.POST);
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/storeJsonVariablesHttpRequestAction" +
                ".json");
        startTrigger(entity1, entity2);

        Assertions.assertEquals((Object) 200L, entity1.getAttribute("responseStatus"));
        Assertions.assertNull(entity1.getAttribute("dataKey"));
        Assertions.assertTrue(((Collection<?>) entity1.getAttribute("arrayKeys")).isEmpty());

        verify(restTemplate, times(1)).execute(
                eq("https://example.com/test"),
                eq(HttpMethod.POST),
                any(),
                any()
        );

        var body = "{\"responseStatus\": 200, \"dataKey\": null, \"arrayKeys\": []}";
        verifyBodyWritten(request, body);

        checkHeaders(request, Map.of(), MediaType.APPLICATION_JSON, 1);
    }

    @Test
    public void storeTextVariables() throws IOException {
        InputStream responseBodyStream = new ByteArrayInputStream(
                "Just text here".getBytes(StandardCharsets.UTF_8)
        );
        var httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getRawStatusCode()).thenReturn(200);
        when(response.getBody()).thenReturn(responseBodyStream);
        when(response.getHeaders()).thenReturn(httpHeaders);
        prepareMocks("https://example.com", HttpMethod.GET, response);
        ClientHttpRequest request = prepareMocks("https://example.com/test", HttpMethod.POST);
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/storeTextVariablesHttpRequestAction" +
                ".json");
        startTrigger(entity1, entity2);

        Assertions.assertEquals((Object) 200L, entity1.getAttribute("responseStatus"));
        Assertions.assertEquals("Just text here", entity1.getAttribute("dataKey"));
        Assertions.assertEquals(List.of("null"), entity1.getAttribute("arrayKeys"));

        verify(restTemplate, times(1)).execute(
                eq("https://example.com/test"),
                eq(HttpMethod.POST),
                any(),
                any()
        );

        var body = "{\"responseStatus\": 200, \"dataKey\": \"Just text here\", \"arrayKeys\": null}";
        verifyBodyWritten(request, body);

        checkHeaders(request, Map.of(), MediaType.APPLICATION_JSON, 1);
    }

    @Test
    public void basicAuthHttpRequest() throws IOException {
        ClientHttpRequest request = prepareMocks("https://example.com", HttpMethod.POST);
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/basicAuthHttpRequestAction.json");
        startTrigger(entity1, entity2);

        verify(restTemplate, times(1)).execute(
                eq("https://example.com"),
                eq(HttpMethod.POST),
                any(),
                any()
        );

        verify(request.getBody(), times(1)).write(
                ArgumentMatchers.argThat(argument -> Arrays.mismatch(
                        argument,
                        "{\"my\":\"body\",\"number\":124}".getBytes(StandardCharsets.UTF_8)
                ) == 26),
                eq(0),
                eq(26)
        );

        checkHeaders(
                request,
                Map.of(
                        "X-Some-Header", "value",
                        HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwc3N3ZA=="
                ),
                MediaType.APPLICATION_JSON,
                1
        );
    }

    @Test
    public void tokenAndBasicAuthHttpRequest() throws IOException {
        ClientHttpRequest request = prepareMocks("https://example.com", HttpMethod.POST);
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/tokenAndBasicAuthHttpRequestAction" +
                ".json");
        startTrigger(entity1, entity2);

        verify(restTemplate, times(1)).execute(
                eq("https://example.com"),
                eq(HttpMethod.POST),
                any(),
                any()
        );

        verify(request.getBody(), times(1)).write(
                ArgumentMatchers.argThat(argument -> Arrays.mismatch(
                        argument,
                        "{\"my\":\"body\",\"number\":124}".getBytes(StandardCharsets.UTF_8)
                ) == 26),
                eq(0),
                eq(26)
        );

        checkHeaders(
                request,
                Map.of(
                        "X-Some-Header", "value",
                        HttpHeaders.AUTHORIZATION, "AccessToken token",
                        "X-User-Authorization", "Basic dXNlcjpwc3N3ZA=="
                ),
                MediaType.APPLICATION_JSON,
                1
        );
    }

    @Test
    public void oAuthAuthHttpRequest() throws IOException {
        ClientHttpRequest request = prepareMocks("https://example.com", HttpMethod.POST);
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/oAuthAuthHttpRequestAction.json");
        startTrigger(entity1, entity2);

        verify(restTemplate, times(1)).execute(
                eq("https://example.com"),
                eq(HttpMethod.POST),
                any(),
                any()
        );

        verify(request.getBody(), times(1)).write(
                ArgumentMatchers.argThat(argument -> Arrays.mismatch(
                        argument,
                        "{\"my\":\"body\",\"number\":124}".getBytes(StandardCharsets.UTF_8)
                ) == 26),
                eq(0),
                eq(26)
        );

        checkHeaders(
                request,
                Map.of(
                        "X-Some-Header", "value",
                        HttpHeaders.AUTHORIZATION, "Bearer psswd"
                ),
                MediaType.APPLICATION_JSON,
                1
        );
    }

    @Test
    public void httpRequestHeaderValueTemplate() throws IOException {
        ClientHttpRequest request = prepareMocks("https://example.com", HttpMethod.GET);
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event" +
                "/templateHeaderValueHttpRequestAction.json");
        startTrigger(entity1, entity2);

        verify(restTemplate, times(1)).execute(
                eq("https://example.com"),
                eq(HttpMethod.GET),
                any(),
                any()
        );

        verifyNoInteractions(request.getBody());
        checkHeaders(request, Map.of("X-Some-Template-Header", entity1.getGid()));
    }

    @Test
    public void httpRequestUrlTemplate() throws IOException {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        ClientHttpRequest request = prepareMocks("https://example.com/" + entity2.getGid(), HttpMethod.GET);
        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/templateUrlHttpRequestAction.json");
        startTrigger(entity1, entity2);

        verify(restTemplate, only()).execute(
                eq("https://example.com/" + entity1.getGid()),
                eq(HttpMethod.GET),
                any(),
                any()
        );

        verifyNoInteractions(request.getBody());

        verifyNoInteractions(request.getHeaders());
    }

    @Test
    public void httpRequestUriVariables() throws IOException {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        ClientHttpRequest request = prepareMocks("https://example.com/" + entity2.getGid(), HttpMethod.GET);
        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/httpRequestUriVariables.json");
        startTrigger(entity1, entity2);

        verify(restTemplate, only()).execute(
                eq("https://example.com/" + entity1.getGid() +
                        "/test%25%2F?param=%D0%9F%D1%83%D1%82%D1%8C%3A%20%2F%2Fhome%2Ftest"),
                eq(HttpMethod.GET),
                any(),
                any()
        );

        verifyNoInteractions(request.getBody());
        verifyNoInteractions(request.getHeaders());
    }

    @Test
    public void tvmAuthHttpRequest() throws IOException {
        ClientHttpRequest request = prepareMocks("https://example.com", HttpMethod.POST);
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/tvmAuthHttpRequestAction.json");
        startTrigger(entity1, entity2);

        verify(restTemplate, times(1)).execute(
                eq("https://example.com"),
                eq(HttpMethod.POST),
                any(),
                any()
        );

        verify(request.getBody(), times(1)).write(
                ArgumentMatchers.argThat(argument -> Arrays.mismatch(
                        argument,
                        "{\"my\":\"body\",\"number\":124}".getBytes(StandardCharsets.UTF_8)
                ) == 26),
                eq(0),
                eq(26)
        );

        checkHeaders(
                request,
                Map.of(
                        "X-Some-Header", "value",
                        HttpHeaderNames.TVM_SERVICE_TICKET, "test"
                ),
                MediaType.APPLICATION_JSON,
                1
        );
    }

    @Test
    public void safetyTvmAuthHttpRequest() throws IOException {
        ClientHttpRequest request = prepareMocks("http://localhost.com", HttpMethod.POST);
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        bcpService.create(AllowedOutgoingTvmService.FQN, ImmutableMap.of(
                "title", "LOCALHOST",
                "url", "http://localhost.com",
                "clientId", 123));

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/safetyTvmAuthHttpRequestAction.json");
        startTrigger(entity1, entity2);

        verify(restTemplate, times(1)).execute(
                eq("http://localhost.com"),
                eq(HttpMethod.POST),
                any(),
                any()
        );

        verify(request.getBody(), times(1)).write(
                ArgumentMatchers.argThat(argument -> Arrays.mismatch(
                        argument,
                        "{\"my\":\"body\",\"number\":124}".getBytes(StandardCharsets.UTF_8)
                ) == 26),
                eq(0),
                eq(26)
        );

        checkHeaders(
                request,
                Map.of(
                        "X-Some-Header", "value",
                        HttpHeaderNames.TVM_SERVICE_TICKET, "test"
                ),
                MediaType.APPLICATION_JSON,
                1
        );
    }

    @Test
    public void incorrectSafetyTvmAuthHttpRequest() throws IOException {
        ClientHttpRequest request = prepareMocks("http://localhost.com", HttpMethod.POST);
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        var tvmService = bcpService.create(AllowedOutgoingTvmService.FQN, ImmutableMap.of(
                "title", "LOCALHOST",
                "url", "http://localhost",
                "clientId", 123));

        Assertions.assertThrows(ValidationException.class, () -> {
            createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/safetyTvmAuthHttpRequestAction" +
                    ".json");
        });


        bcpService.edit(tvmService, Maps.of(
                "title", "LOCALHOST2",
                "url", "http://localhost2",
                "clientId", 123));

        startTrigger(entity1, entity2);

        verify(restTemplate, times(0)).execute(
                eq("http://localhost.com"),
                eq(HttpMethod.POST),
                any(),
                any()
        );

        verify(request.getBody(), times(0)).write(
                ArgumentMatchers.argThat(argument -> Arrays.mismatch(
                        argument,
                        "{\"my\":\"body\",\"number\":124}".getBytes(StandardCharsets.UTF_8)
                ) == 26),
                eq(0),
                eq(26)
        );

        var headers = request.getHeaders();
        verify(headers, times(0)).add("X-Some-Header", "value");
        verify(headers, times(0)).add(HttpHeaderNames.TVM_SERVICE_TICKET, "test");
        verify(headers, times(0)).setContentType(MediaType.APPLICATION_JSON);
        verifyNoMoreInteractions(headers);
    }

    @Test
    public void testThatRequestIsRetriedUntilSucceed() throws IOException {
        var numberOfSuccessfulAttempt = 3;
        var request = prepareMocks("https://example.com", HttpMethod.POST, (i) -> {
            if (i != numberOfSuccessfulAttempt) {
                throw new RuntimeException();
            }
            return null;
        });
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/tvmAuthHttpRequestAction.json");
        startTrigger(entity1, entity2);

        verify(restTemplate, times(numberOfSuccessfulAttempt)).execute(
                eq("https://example.com"),
                eq(HttpMethod.POST),
                any(),
                any()
        );

        verify(request.getBody(), times(numberOfSuccessfulAttempt)).write(
                ArgumentMatchers.argThat(argument -> Arrays.mismatch(
                        argument,
                        "{\"my\":\"body\",\"number\":124}".getBytes(StandardCharsets.UTF_8)
                ) == 26),
                eq(0),
                eq(26)
        );

        checkHeaders(
                request,
                Map.of(
                        "X-Some-Header", "value",
                        HttpHeaderNames.TVM_SERVICE_TICKET, "test"
                ),
                MediaType.APPLICATION_JSON,
                numberOfSuccessfulAttempt
        );
    }

    @Test
    public void testThatRequestIsRetriedUntilMaxAttemptsReached() throws IOException {
        var request = prepareMocks("https://example.com", HttpMethod.POST, i -> {
            throw new RuntimeException();
        });
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/tvmAuthHttpRequestAction.json");
        startTrigger(entity1, entity2);

        var maxAttemptsCount = 5;
        verify(restTemplate, times(maxAttemptsCount)).execute(
                eq("https://example.com"),
                eq(HttpMethod.POST),
                any(),
                any()
        );

        verify(request.getBody(), times(maxAttemptsCount)).write(
                ArgumentMatchers.argThat(argument -> Arrays.mismatch(
                        argument,
                        "{\"my\":\"body\",\"number\":124}".getBytes(StandardCharsets.UTF_8)
                ) == 26),
                eq(0),
                eq(26)
        );

        checkHeaders(
                request,
                Map.of(
                        "X-Some-Header", "value",
                        HttpHeaderNames.TVM_SERVICE_TICKET, "test"
                ),
                MediaType.APPLICATION_JSON,
                maxAttemptsCount
        );
    }

    @Nonnull
    private ClientHttpRequest prepareMocks(
            String url, HttpMethod method,
            Exceptions.TrashFunction<Integer, Object> restTemplateExecuteResultFunction
    ) throws IOException {
        var request = mock(ClientHttpRequest.class);
        OutputStream outputStream = mock(OutputStream.class);
        when(request.getBody()).thenReturn(outputStream);
        var headers = mock(HttpHeaders.class);
        when(request.getHeaders()).thenReturn(headers);
        var invocationCount = new AtomicInteger(0);
        when(restTemplate.execute(
                eq(url),
                eq(method),
                any(),
                any()
        )).then(invocation -> {
            invocation.getArgument(2, RequestCallback.class).doWithRequest(request);
            return restTemplateExecuteResultFunction.apply(invocationCount.incrementAndGet());
        });
        return request;
    }

    @Nonnull
    private ClientHttpRequest prepareMocks(
            String url, HttpMethod method,
            ClientHttpResponse response
    ) throws IOException {
        var request = mock(ClientHttpRequest.class);
        OutputStream outputStream = mock(OutputStream.class);
        when(request.getBody()).thenReturn(outputStream);
        var headers = mock(HttpHeaders.class);
        when(request.getHeaders()).thenReturn(headers);
        var invocationCount = new AtomicInteger(0);
        when(restTemplate.execute(
                eq(url),
                eq(method),
                any(),
                any()
        )).then(invocation -> {
            invocation.getArgument(2, RequestCallback.class).doWithRequest(request);
            return invocation.getArgument(3, ResponseExtractor.class).extractData(response);
        });
        return request;
    }

    @Nonnull
    private ClientHttpRequest prepareMocks(String s, HttpMethod post) throws IOException {
        return prepareMocks(s, post, ignored -> null);
    }

    private void checkHeaders(ClientHttpRequest request, Map<String, String> expectedHeaders) {
        checkHeaders(request, expectedHeaders, null, 1);
    }

    private void checkHeaders(ClientHttpRequest request,
                              Map<String, String> expectedHeaders,
                              MediaType contentType,
                              int numberOfSuccessfulAttempt) {
        var headers = request.getHeaders();
        expectedHeaders.forEach((k, v) -> verify(headers, times(numberOfSuccessfulAttempt)).add(k, v));
        verify(headers, times(numberOfSuccessfulAttempt)).add(eq(RequestTraceUtil.REQUEST_ID_HEADER), anyString());
        if (null != contentType) {
            verify(headers, times(numberOfSuccessfulAttempt)).setContentType(contentType);
        }
        verifyNoMoreInteractions(headers);
    }
}
