package ru.yandex.direct.intapi.client;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.Assertions;
import org.asynchttpclient.Request;
import org.hamcrest.MatcherAssert;
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
import ru.yandex.direct.intapi.client.model.request.CalculateCampaignStatusModerateRequest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.intapi.client.IntApiClient.CALCULATE_CAMPAIGN_STATUS;
import static ru.yandex.direct.utils.JsonUtils.fromJson;
import static ru.yandex.direct.utils.JsonUtils.getObjectMapper;

/**
 * Тесты на методы CalculateCampaignStatusModerate
 */
public class CalculateCampaignStatusClientMethodTest {
    private static final String TEST_INTAPI_URL = "http://intapi.direct.yandex.ru";
    private static final int BIG_ENOUGH_PRIME_NUMBER = 2999;

    private static CalculateCampaignStatusModerateRequest validRequest;

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
        Long cid = RandomUtils.nextLong(1, 2147483647);
        validRequest = new CalculateCampaignStatusModerateRequest(List.of(cid));
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
    public void checkBigRequestData() throws InterruptedException {
        fetcherResponse.setSuccess(IntApiClient.CAMPAIGN_UNARC_SUCCESS_RESULT);
        setFetcherResponse();

        List<Long> cids = LongStream.range(1, BIG_ENOUGH_PRIME_NUMBER).boxed().collect(Collectors.toList());

        intApiClient.calculateCampaignStatusModerate(cids);
        verify(fetcher).execute(requestCaptor.capture());

        List<ParsableRequest<String>> requests = requestCaptor.getValue();

        List<Long> actuallyRequestedCids = new ArrayList<>();

        Assertions.assertThat(requests.size()).isGreaterThan(2);

        for (var r : requests) {
            String requestData = r.getAHCRequest().getStringData();

            MatcherAssert.assertThat("URI запроса соответствует ожидаемому", r.getAHCRequest().getUrl(),
                    equalTo(TEST_INTAPI_URL + "/" + CALCULATE_CAMPAIGN_STATUS.getPath()));

            CalculateCampaignStatusModerateRequest request = fromJson(requestData,
                    CalculateCampaignStatusModerateRequest.class);
            actuallyRequestedCids.addAll(request.getCids());
        }

        Assertions.assertThat(actuallyRequestedCids).containsExactlyElementsOf(cids);
    }

    @Test
    public void checkRequestData() throws InterruptedException {
        fetcherResponse.setSuccess(IntApiClient.CAMPAIGN_UNARC_SUCCESS_RESULT);
        setFetcherResponse();

        intApiClient.calculateCampaignStatusModerate(validRequest.getCids());
        verify(fetcher).execute(requestCaptor.capture());

        Request request = requestCaptor.getValue().get(0).getAHCRequest();

        String requestData = request.getStringData();
        MatcherAssert.assertThat("URI запроса соответствует ожидаемому", request.getUrl(),
                equalTo(TEST_INTAPI_URL + "/" + CALCULATE_CAMPAIGN_STATUS.getPath()));

        assertThat("запрос соответствует ожидаемому",
                fromJson(requestData, CalculateCampaignStatusModerateRequest.class),
                beanDiffer(getObjectMapper().convertValue(validRequest, CalculateCampaignStatusModerateRequest.class)));
    }

    @Test
    public void checkExceptionAfterFetchNotEmptyResponse() throws TimeoutException, InterruptedException {
        fetcherResponse.setSuccess("SOME_RESULT");
        setFetcherResponse();

        thrown.expect(IntApiClientException.class);
        thrown.expectMessage("Got unexpected response for CalculateCampaignStatusModerate request");

        intApiClient.calculateCampaignStatusModerate(validRequest.getCids());
    }

    @Test
    public void checkSuccessAddNotification() throws TimeoutException, InterruptedException {
        fetcherResponse.setSuccess(IntApiClient.CAMPAIGN_UNARC_SUCCESS_RESULT);
        setFetcherResponse();

        intApiClient.calculateCampaignStatusModerate(validRequest.getCids());
        verify(fetcher).execute(anyList());
    }
}
