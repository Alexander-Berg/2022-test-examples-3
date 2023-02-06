package ru.yandex.direct.pokazometer;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcher;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.asynchttp.Result;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PokazometerClientTest {
    private static final Long TEST_KEYWORD_ID = 12345L;

    @Test
    @SuppressWarnings("unchecked")
    public void parseTest() throws InterruptedException, TimeoutException {
        long id = 1L;
        ParallelFetcher<String> fetcher = mock(ParallelFetcher.class);
        Result<String> fetcherResult = new Result<>(id);
        fetcherResult.setSuccess(
                "{\"id\": 42,\"jsonrpc\": \"2.0\",\"result\": {\"distribution\": [[{\"cost\": 100,\"count\": 1},{\"cost\""
                        + ": 200,\"count\": 1000}]],\"ref_days\": 7}}");

        when(fetcher.execute(anyList())).thenReturn(ImmutableMap.of(1L, fetcherResult));

        ParallelFetcherFactory fetcherFactoryMock = mock(ParallelFetcherFactory.class);
        when(fetcherFactoryMock.<String>getParallelFetcherWithMetricRegistry(any())).thenReturn(fetcher);

        PokazometerClient client = new PokazometerClient(fetcherFactoryMock, "http://test.yandex.ru/test");

        IdentityHashMap<GroupRequest, GroupResponse> result = client.get(singletonList(new GroupRequest(
                singletonList(new PhraseRequest("носки", 150L)),
                emptyList())));
        PhraseResponse phrase = result.values().iterator().next().getPhrases().get(0);
        assertEquals(50, phrase.getContextCoverage().intValue());
        assertEquals(120, phrase.getPriceByCoverage(PhraseResponse.Coverage.LOW).intValue());
        assertEquals(150, phrase.getPriceByCoverage(PhraseResponse.Coverage.MEDIUM).intValue());
        assertEquals(200, phrase.getPriceByCoverage(PhraseResponse.Coverage.HIGH).intValue());
    }

    @Test
    @Ignore
    public void smokeTest() {
        PokazometerClient client = new PokazometerClient(new ParallelFetcherFactory(null, new FetcherSettings()),
                "http://whale-test.yandex.ru");
        IdentityHashMap<GroupRequest, GroupResponse> results = client.get(
                singletonList(new GroupRequest(
                        asList(new PhraseRequest("носки", 1000000L),
                                new PhraseRequest("брюки", 1000000L)),
                        asList(1L, 2L, 3L))));
        assertEquals(2, results.values().iterator().next().getPhrases().size());
    }

    @Test
    @Ignore
    public void emptyResponseTest() {
        PokazometerClient client = new PokazometerClient(new ParallelFetcherFactory(null, new FetcherSettings()),
                "http://whale-test.yandex.ru");
        IdentityHashMap<GroupRequest, GroupResponse> results = client.get(
                singletonList(new GroupRequest(
                        asList(new PhraseRequest("????", 1000000L),
                                new PhraseRequest("брюки", 10000000L)),
                        asList(1L, 2L, 3L))));
        assertEquals(2, results.values().iterator().next().getPhrases().size());
    }

    @Test
    @Ignore
    public void keywordIdTest() {
        PokazometerClient client = new PokazometerClient(new ParallelFetcherFactory(null, new FetcherSettings()),
                "http://whale-test.yandex.ru");
        GroupRequest request = new GroupRequest(
                asList(new PhraseRequest("????", 1000000L, TEST_KEYWORD_ID),
                        new PhraseRequest("брюки", 10000000L, TEST_KEYWORD_ID + 1)),
                asList(1L, 2L, 3L));
        IdentityHashMap<GroupRequest, GroupResponse> results = client.get(singletonList(request));

        assertEquals(Arrays.asList(TEST_KEYWORD_ID, TEST_KEYWORD_ID + 1),
                results.get(request).getPhrases().stream().map(p -> p.getKeywordId()).collect(Collectors.toList()));
    }
}
