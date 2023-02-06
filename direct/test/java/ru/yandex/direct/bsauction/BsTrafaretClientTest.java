package ru.yandex.direct.bsauction;

import java.util.IdentityHashMap;
import java.util.List;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.currency.CurrencyCode.USD;

/**
 * Тест для ручной проверки клиента к Трафаретным торгам {@link BsTrafaretClient}
 */
@Ignore("For manual smoke-test of BsTrafaretClient")
public class BsTrafaretClientTest {

    private AsyncHttpClient asyncHttpClient;
    ParallelFetcherFactory fetcherFactory;

    @Before
    public void setUp() throws Exception {
        DefaultAsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig
                .Builder()
                .setConnectTimeout(30 * 1000)
                .setReadTimeout(5 * 1000)
                .setMaxConnections(100)
                .build();
        asyncHttpClient = new DefaultAsyncHttpClient(config);
        fetcherFactory = new ParallelFetcherFactory(asyncHttpClient, new FetcherSettings());
    }

    @After
    public void tearDown() throws Exception {
        asyncHttpClient.close();
    }

    @Test
    public void smokeTest() {
        List<BsRequest<BasicBsRequestPhrase>> bsRequests = getBsRequests();

        BsTrafaretClient client = new BsTrafaretClient(fetcherFactory,
                "http://ppctest-proxy.ppc.yandex.ru:7088/rank/24", "bsrank.yandex.ru", () -> null);
        IdentityHashMap<BsRequest<BasicBsRequestPhrase>, BsResponse<BasicBsRequestPhrase, PositionalBsTrafaretResponsePhrase>> results =
                client.getAuctionResults(bsRequests);
        assertEquals(1, results.size());
    }

    @Test
    public void smokeTestForPositionCtrCorrection() {
        List<BsRequest<BasicBsRequestPhrase>> bsRequests = getBsRequests();

        BsTrafaretClient client = new BsTrafaretClient(fetcherFactory,
                "http://ppctest-proxy.ppc.yandex.ru:7088/rank/24", "bsrank.yandex.ru", () -> null);
        IdentityHashMap<BsRequest<BasicBsRequestPhrase>, BsResponse<BasicBsRequestPhrase, FullBsTrafaretResponsePhrase>>
                results = client.getAuctionResultsWithPositionCtrCorrection(bsRequests);
        assertEquals(1, results.size());
    }

    List<BsRequest<BasicBsRequestPhrase>> getBsRequests() {
        BsRequest<BasicBsRequestPhrase> request = new BsRequest<>();
        request.withOrderId(0)
                .withRegionIds(singletonList(0L))
                .withCurrency(USD.getCurrency())
                .withDomain("yandex.ru")
                .withTimetable(true)
                .withBannerHead("купить ноутбук")
                .withBannerBody("купить хороший ноутбук")
                .withPhrases(singletonList(
                        new BasicBsRequestPhrase()
                                .withText("купить ноутбук")
                                .withStat(BsRequestPhraseStat.getByForecast(100, 14, 10, 3))
                ));
        return singletonList(request);
    }
}
