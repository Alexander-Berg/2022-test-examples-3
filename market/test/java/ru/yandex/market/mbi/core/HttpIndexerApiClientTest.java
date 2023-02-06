package ru.yandex.market.mbi.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.retry.support.RetryTemplate;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.common.excel.MarketTemplate;
import ru.yandex.market.common.test.matcher.HttpGetMatcher;
import ru.yandex.market.core.feed.model.FeedFileType;
import ru.yandex.market.mbi.core.feed.FeedParsingRequest;
import ru.yandex.market.mbi.core.feed.FeedParsingResult;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

/**
 * Тест на логику работы {@link HttpIndexerApiClient}.
 *
 * @author fbokovikov
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpIndexerApiClientTest {

    private static final String INDEXER_API_ACTIVE_URL = "http://active.indexer.market.yandex.net:29334/";
    private static final String INDEXER_API_URL = "http://indexer.market.yandex.net:29334/";
    private static final String FEED_URL = "https://market-mbi-dev.s3.mdst.yandex" +
            ".net/upload-feed/576/upload-feed-693576";

    private final String FEED_CHECKER_RESPONSE =
            IOUtils.toString(this.getClass().getResourceAsStream("feedCheckerResponse.txt"));

    private final String FEED_CHECKER_RESPONSE_WITH_FEED_FILE_TYPE =
            IOUtils.toString(this.getClass().getResourceAsStream("feedCheckerResponseWithFeedFileType.txt"));

    @Mock
    private HttpClient mockHttpClient;

    private HttpIndexerApiClient httpIndexerApiClient;

    private RetryTemplate retryTemplate = new RetryTemplate();

    public HttpIndexerApiClientTest() throws IOException {
    }

    private static String buildExpectedFeedCheckUrl() {
        try {
            URIBuilder uriBuilder = new URIBuilder(INDEXER_API_ACTIVE_URL + "v1/check/feed");
            uriBuilder.addParameter("url", FEED_URL);
            uriBuilder.addParameter("tolerant", null);
            uriBuilder.addParameter("alcohol", CpaCpcStatus.NO.name());
            return uriBuilder.toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void initMock() {
        httpIndexerApiClient = new HttpIndexerApiClient(
                mockHttpClient,
                INDEXER_API_URL,
                INDEXER_API_ACTIVE_URL,
                retryTemplate);
    }

    /**
     * Тест на {@link HttpIndexerApiClient#parseFeed(FeedParsingRequest) интеграцию с фидчекером}
     */
    @Test
    public void testFeedCheckRequest() throws IOException {
        testFeedCheckRequest(FEED_CHECKER_RESPONSE, null);
        testFeedCheckRequest(FEED_CHECKER_RESPONSE_WITH_FEED_FILE_TYPE, FeedFileType.XLS);
    }

    private void testFeedCheckRequest(String feedCheckerResponse, FeedFileType feedFileType) throws IOException {
        HttpResponse response = mockOkResponse(feedCheckerResponse);
        when(mockHttpClient.execute(argThat(new HttpGetMatcher(buildExpectedFeedCheckUrl(), "GET"))))
                .thenReturn(response);
        FeedParsingResult feedParsingResult = httpIndexerApiClient.parseFeed(buildFeedParsingRequest());
        String expectedLog = Stream.of(feedCheckerResponse.split("\n"))
                .filter(s -> !s.contains("X-MARKET-TEMPLATE"))
                .filter(s -> !s.contains("X-FEED-FILE-TYPE"))
                .filter(s -> !s.contains("X-RETURN-CODE"))
                .collect(Collectors.joining("\n")) + "\n";
        FeedParsingResult expected = new FeedParsingResult(expectedLog, 0, MarketTemplate.COMMON, feedFileType);
        ReflectionAssert.assertReflectionEquals(expected, feedParsingResult);
    }

    private FeedParsingRequest buildFeedParsingRequest() {
        FeedParsingRequest feedParsingRequest = new FeedParsingRequest();
        feedParsingRequest.setFeedUrl("https://market-mbi-dev.s3.mdst.yandex.net/upload-feed/576/upload-feed-693576");
        feedParsingRequest.setTolerant(true);
        feedParsingRequest.setAlcoholStatus(CpaCpcStatus.NO);
        return feedParsingRequest;
    }

    private HttpResponse mockOkResponse(byte[] body) {
        HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 2), 200, "");
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(body));
        response.setEntity(entity);
        return response;
    }

    private HttpResponse mockOkResponse(String body) {
        return mockOkResponse(body.getBytes(StandardCharsets.UTF_8));
    }
}
