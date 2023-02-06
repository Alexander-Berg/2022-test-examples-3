package ru.yandex.direct.intapi.client;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.RandomStringUtils;
import org.asynchttpclient.Request;
import org.junit.Before;
import org.junit.BeforeClass;
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
import ru.yandex.direct.intapi.client.model.request.NotificationRequest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.intapi.client.IntApiClient.NOTIFICATION_HANDLE;
import static ru.yandex.direct.utils.JsonUtils.fromJson;

/**
 * Тесты на методы NotificationClient
 */
public class NotificationClientMethodTest {
    private static final String TEST_INTAPI_URL = "http://intapi.direct.yandex.ru";
    private static final String TEST_NOTIFICATION_TYPE = "test_type";
    private static final String SUCCESS_RESULT = "";
    private static final int EXPECTED_REQUEST_PARAMS_COUNT = 3;

    private static NotificationRequest validRequest;

    private IntApiClient intApiClient;
    private Result<String> fetcherResponse;

    @Mock
    private ParallelFetcher<String> fetcher;

    @Captor
    private ArgumentCaptor<List<ParsableRequest<String>>> requestCaptor;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void initTestData() {
        Map<String, Object> data = new HashMap<>();
        data.put("someField", "someValue" + RandomStringUtils.random(5));
        data.put("some_field2", "some_value2" + RandomStringUtils.random(7));
        Map<String, Object> params = new HashMap<>();
        params.put("someField", "someValue" + RandomStringUtils.random(5));
        validRequest = new NotificationRequest(TEST_NOTIFICATION_TYPE, data, params);
    }

    @Before
    public void before() throws URISyntaxException {
        MockitoAnnotations.initMocks(this);

        ParallelFetcherFactory parallelFetcherFactory = mock(ParallelFetcherFactory.class);
        when(parallelFetcherFactory.getParallelFetcherWithMetricRegistry(any())).thenAnswer(fs -> fetcher);

        IntApiClientConfiguration configuration = new IntApiClientConfiguration(TEST_INTAPI_URL);
        intApiClient = new IntApiClient(configuration, parallelFetcherFactory, () -> null);

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
    public void checkSuccessAddNotification() throws TimeoutException, InterruptedException {
        fetcherResponse.setSuccess(SUCCESS_RESULT);
        setFetcherResponse();

        intApiClient
                .addNotification(validRequest.getNotificationType(), validRequest.getData(), validRequest.getOptions());
        verify(fetcher).execute(requestCaptor.capture());

        Request request = requestCaptor.getValue().get(0).getAHCRequest();
        assertThat("URI запроса соответствует ожидаемому", request.getUrl(),
                equalTo(TEST_INTAPI_URL + "/" + NOTIFICATION_HANDLE.getPath()));

        Map requestData = fromJson(request.getStringData(), Map.class);
        assertEquals(requestData.keySet().size(), EXPECTED_REQUEST_PARAMS_COUNT);
        assertEquals(requestData.get("name"), validRequest.getNotificationType());
        assertThat(requestData.get("vars"), beanDiffer(validRequest.getData()));
        assertThat(requestData.get("options"), beanDiffer(validRequest.getOptions()));
    }

    @Test
    public void checkExceptionAfterFetchNotEmptyResponse() throws TimeoutException, InterruptedException {
        fetcherResponse.setSuccess("not success result");
        setFetcherResponse();

        thrown.expect(IntApiClientException.class);
        thrown.expectMessage("Got unexpected response for Notification request");

        intApiClient
                .addNotification(validRequest.getNotificationType(), validRequest.getData(), validRequest.getOptions());
    }
}
