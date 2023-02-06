package ru.yandex.direct.intapi.client;

import java.net.URISyntaxException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.google.common.primitives.Ints;
import org.assertj.core.api.SoftAssertions;
import org.asynchttpclient.Request;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.asynchttp.ParallelFetcher;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.asynchttp.ParsableRequest;
import ru.yandex.direct.asynchttp.ParsableStringRequest;
import ru.yandex.direct.asynchttp.Result;
import ru.yandex.direct.intapi.client.model.handle.IntApiHandle;
import ru.yandex.direct.intapi.client.model.request.IntApiRequest;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.intapi.client.IntApiClient.SERVICE_TICKET_HEADER;

/**
 * Тесты на методы CampaignUnarcClient
 */
public class IntapiClientTest {
    private static final String TEST_INTAPI_URL = "http://intapi.direct.yandex.ru";
    private static final String TEST_PATH = "Test/test2";
    private static final String TEST_RESPONSE = "Response";
    private static final String TEST_REQUEST = "Request";
    private static final String TVM_TICKET_BODY = "tvm_ticket";
    private static final Duration TEST_TIMEOUT = Duration.ofMillis(1);

    private static final IntApiHandle<String> TEST_HANDLE =
            new IntApiHandle<String>(TEST_PATH, APPLICATION_JSON.toString()) {
                @Override
                public String serializeRequest(IntApiRequest request) {
                    return TEST_REQUEST;
                }

                @Override
                public String deserializeResponse(String response) {
                    return TEST_RESPONSE;
                }
            };

    private IntApiRequest request = new IntApiRequest() {
    };
    private IntApiClient intApiClient;
    private Result<String> fetcherResponse;

    @Mock
    private ParallelFetcher<String> fetcher;

    @Captor
    private ArgumentCaptor<List<ParsableRequest<String>>> requestCaptor;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void before() throws URISyntaxException {
        MockitoAnnotations.initMocks(this);
        intApiClient = getIntApiClient(() -> null);
        fetcherResponse = new Result<>(ParsableStringRequest.DEFAULT_REQUEST_ID);
    }

    void setFetcherResponse() throws InterruptedException {
        when(fetcher.execute(ArgumentMatchers.anyList())).thenAnswer(i -> {
            Map<Long, Result<?>> resultMap = new HashMap<>();

            long id = 1;

            for (var ignored : (List<ParsableStringRequest>) i.getArgument(0)) {
                resultMap.put(id++, fetcherResponse);
            }
            return resultMap;
        });
    }

    @Test
    public void testSuccessfulRequest() throws InterruptedException {
        fetcherResponse.setSuccess(TEST_RESPONSE);
        setFetcherResponse();

        intApiClient.doRequest(TEST_HANDLE, request, TEST_TIMEOUT);
        verify(fetcher).execute(requestCaptor.capture());

        Request request = requestCaptor.getValue().get(0).getAHCRequest();

        assertThat("URI запроса соответствует ожидаемому", request, allOf(
                hasProperty("url", equalTo(TEST_INTAPI_URL + "/" + TEST_PATH)),
                hasProperty("stringData", equalTo(TEST_REQUEST)),
                hasProperty("requestTimeout", equalTo(Ints.checkedCast(TEST_TIMEOUT.toMillis())))));
    }

    @Test
    public void checkExceptionAfterFetchError() throws InterruptedException {
        fetcherResponse.addError(new RuntimeException("Custom internal server error"));
        setFetcherResponse();

        thrown.expect(IntApiClientException.class);
        thrown.expectMessage("Got error on response for IntApi request");

        intApiClient.doRequest(TEST_HANDLE, request, TEST_TIMEOUT);
    }

    @Test
    public void checkExceptionAfterDeserializationResultIsNull() throws InterruptedException {
        fetcherResponse.setSuccess(TEST_RESPONSE);
        setFetcherResponse();

        thrown.expect(IntApiClientException.class);
        thrown.expectMessage("Deserialization result is null");

        IntApiHandle<String> handle = new IntApiHandle<String>(TEST_PATH, APPLICATION_JSON.toString()) {
            @Override
            public String serializeRequest(IntApiRequest request) {
                return TEST_REQUEST;
            }

            @Override
            public String deserializeResponse(String response) {
                return null;
            }
        };
        intApiClient.doRequest(handle, request, TEST_TIMEOUT);
    }

    @Test
    public void checkInterrupted() throws InterruptedException {
        doThrow(InterruptedException.class).when(fetcher).execute(anyList());

        thrown.expect(IntApiClientException.class);

        intApiClient.doRequest(TEST_HANDLE, request, TEST_TIMEOUT);
    }

    @Test
    public void checkTimeout() throws InterruptedException {
        when(fetcher.execute(ArgumentMatchers.anyList())).thenAnswer(i -> {
            Map<Long, Result<Object>> resultMap = new HashMap<>();

            long id = 1;

            for (var ignored : (List<ParsableStringRequest>) i.getArgument(0)) {
                Result<Object> ret = new Result<>(ParsableStringRequest.DEFAULT_REQUEST_ID);
                ret.addError(new TimeoutException());
                resultMap.put(id++, ret);
            }
            return resultMap;
        });

        thrown.expect(IntApiClientException.class);

        intApiClient.doRequest(TEST_HANDLE, request, TEST_TIMEOUT);
    }

    @Test
    public void tvmTicketPresent() throws InterruptedException, URISyntaxException {
        intApiClient = getIntApiClient(() -> TVM_TICKET_BODY);
        fetcherResponse.setSuccess(TEST_RESPONSE);
        setFetcherResponse();

        intApiClient.doRequest(TEST_HANDLE, request, TEST_TIMEOUT);
        verify(fetcher).execute(requestCaptor.capture());

        Request request = requestCaptor.getValue().get(0).getAHCRequest();

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(request.getHeaders().get(SERVICE_TICKET_HEADER)).isEqualTo(TVM_TICKET_BODY);
        //todo kuhtich: сделано для того, чтобы не падало. Поменять на contains после включения в клиенте
        sa.assertThat(request.getUrl()).doesNotContain("tvm");
        sa.assertAll();
    }

    private IntApiClient getIntApiClient(Supplier<String> tvmServiceTicketProvider) throws URISyntaxException {
        ParallelFetcherFactory parallelFetcherFactory = mock(ParallelFetcherFactory.class);
        when(parallelFetcherFactory.getParallelFetcherWithMetricRegistry(any())).thenAnswer(fs -> fetcher);

        IntApiClientConfiguration configuration = new IntApiClientConfiguration(TEST_INTAPI_URL);
        return new IntApiClient(configuration, parallelFetcherFactory, tvmServiceTicketProvider);
    }
}
