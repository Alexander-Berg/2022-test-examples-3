package ru.yandex.market.common.idx.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.io.IOUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.common.idx.Cluster;
import ru.yandex.market.common.idx.FeedParsingRequest;
import ru.yandex.market.common.idx.FeedParsingResult;
import ru.yandex.market.common.idx.IndexerApiException;
import ru.yandex.market.common.idx.model.Dimensions;
import ru.yandex.market.common.idx.model.FeedOfferId;
import ru.yandex.market.common.idx.model.Stock;
import ru.yandex.market.common.idx.model.SupplierFeedOfferIds;
import ru.yandex.market.common.util.AsyncRetryHttpClient;
import ru.yandex.market.request.trace.Module;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Тест на логику работы {@link HttpIndexerApiClient}.
 *
 * @author fbokovikov
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpIndexerApiClientTest {
    private HttpIndexerApiClient httpIndexerApiClient;

    private HttpIndexerApiClient httpIndexerApiClientAsync;

    @Rule
    public WireMockRule wireMockServer;

    @Before
    public void initMock() throws Exception {
        mockServer();
        String indexerApiUrl = "http://localhost:" + wireMockServer.port();
        httpIndexerApiClient = new HttpIndexerApiClient(HttpClientBuilder.create().build(),
                indexerApiUrl,
                indexerApiUrl,
                null);
        AsyncRetryHttpClient httpClient = new AsyncRetryHttpClient();
        httpClient.setTargetModule(Module.MARKET_INDEXER);
        httpClient.setMaxConnectionTotal(3);
        httpClient.setMaxConnectionPerRoute(3);
        httpClient.setMaxRetryCount(3);
        httpClient.afterPropertiesSet();

        httpIndexerApiClientAsync = new HttpIndexerApiClient(HttpClientBuilder.create().build(),
                indexerApiUrl,
                indexerApiUrl,
                new RetryPolicy().withMaxRetries(2), httpClient);

    }

    @After
    public void destroy() {
        wireMockServer.stop();
    }


    /**
     * Тест на {@link HttpIndexerApiClient#parseFeed(ru.yandex.market.common.idx.FeedParsingRequest) интеграцию с
     * фидчекером}
     */
    @Test
    public void testClient() throws IOException {
        String responseFeed = IOUtils.toString(this.getClass().getResourceAsStream("feedchecker.log"));
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/v1/check/feed"))
                .willReturn(aResponse().withBody(responseFeed)));

        String response = IOUtils.toString(this.getClass().getResourceAsStream("feedchecker.log"));
        //when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(buildHttpResponse(200, response));
        FeedParsingRequest request = FeedParsingRequest.Builder.withFeedUrl("http://test.ru").build();

        FeedParsingResult feedParsingResult = httpIndexerApiClient.parseFeed(request);
        String expectedLog = Stream.of(response.split("\n"))
                .filter(s -> !s.contains("X-MARKET-TEMPLATE") && !s.contains("X-RETURN-CODE"))
                .reduce((s1, s2) -> s1 + "\n" + s2).orElse("") + "\n";
        ReflectionAssert.assertReflectionEquals(
                new FeedParsingResult(expectedLog, 0, "COMMON"),
                feedParsingResult
        );
    }

    @Test
    public void master() throws IOException {
        String responseMaster = IOUtils.toString(this.getClass().getResourceAsStream("master.json"));
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/v1/master"))
                .willReturn(aResponse().withBody(responseMaster)));
        Cluster cluster = httpIndexerApiClient.master();
        assertEquals(Cluster.STRATOCASTER, cluster);
    }

    @Test
    public void masterRetry() throws IOException {
        String responseMaster = IOUtils.toString(this.getClass().getResourceAsStream("master.json"));
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/v1/master"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("{}")));

        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/v1/master"))
                .willReturn(aResponse().withBody(responseMaster)));

        Cluster cluster = httpIndexerApiClient.master();
        assertEquals(Cluster.STRATOCASTER, cluster);
    }

    @Test(expected = IndexerApiException.class)
    public void masterRetryError() {
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/v1/master"))
                .willReturn(aResponse().withStatus(500).withBody("{}")));
        httpIndexerApiClient.master();
    }


    @Test
    public void stocks() {
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/v1/stocks"))
                .willReturn(aResponse().withBody(
                        /*language=json*/
                        "[{\"feed\":1,\"count\":5,\"timestamp\":1565709610156," +
                                "\"offer\":\"123\"}]")));

        List<Stock> response = httpIndexerApiClient.getStocks(
                new SupplierFeedOfferIds(101L, 145L, singletonList(new FeedOfferId(1, "123"))));
        wireMockServer.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/v1/stocks?offer=1-123&shop_id=101" +
                "&warehouse_id=145")));
        assertNotNull("Nonnull response", response);
        assertEquals("Only one record in response", 1, response.size());
        assertEquals(new Stock(1L, "123", 1565709610156L, 5), response.get(0));
    }

    @Test
    public void stocksAsync() {
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/v1/stocks"))
                .willReturn(aResponse().withBody(
                        /*language=json*/
                        "[{\"feed\":1,\"count\":5,\"timestamp\":1565709610156," +
                                "\"offer\":\"123\"}]")));

        List<Stock> response = httpIndexerApiClientAsync.getStocks(
                new SupplierFeedOfferIds(101L, 145L, singletonList(new FeedOfferId(1, "123"))));
        wireMockServer.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/v1/stocks?offer=1-123&shop_id=101" +
                "&warehouse_id=145")));
        assertNotNull("Nonnull response", response);
        assertEquals("Only one record in response", 1, response.size());
        assertEquals(new Stock(1L, "123", 1565709610156L, 5), response.get(0));
    }

    @Test
    public void stocksAsyncBatch() {
        final int indexerBatchSize = 50;
        final int pages = 5;
        final int count = indexerBatchSize * pages;
        List<FeedOfferId> feedOfferIds = new ArrayList<>(count);

        for (int i = 0; i < pages; i++) {
            StringBuilder responseBuilder = new StringBuilder();
            StringBuilder requestBuilder = new StringBuilder();
            responseBuilder.append("[");
            String separator = "";

            for (int j = 0; j < indexerBatchSize; j++) {
                String offerid = Integer.toString(i * indexerBatchSize + j);
                responseBuilder.append(separator);
                responseBuilder.append("{\"feed\":1,\"count\":5,\"timestamp\":1565709610156,\"offer\":\"")
                        .append(offerid)
                        .append("\"}");
                separator = ",\n";
                feedOfferIds.add(new FeedOfferId(1, offerid));
                requestBuilder.append("offer=1-").append(offerid).append("&");
            }
            String request = requestBuilder.append("shop_id=101&warehouse_id=145").toString();
            String response = responseBuilder.append("]").toString();
            wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/v1/stocks?" + request))
                    .willReturn(aResponse().withBody(response))
            );
        }
        List<Stock> response = httpIndexerApiClientAsync.getStocks(
                new SupplierFeedOfferIds(101L, 145L, feedOfferIds));
        wireMockServer.verify(pages, WireMock.getRequestedFor(WireMock.urlPathEqualTo("/v1/stocks")));
        assertNotNull("Nonnull response", response);
        assertEquals(count, response.size());
        int[] offers = response.stream().map(Stock::getOffer).mapToInt(Integer::parseInt).distinct().toArray();
        assertEquals("Not found all offers", count, offers.length);
        assertTrue("Find offers non in query", IntStream.of(offers).allMatch(v -> v >= 0 && v < count));
    }

    @Test
    public void dimensions() {
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/v1/dimensions"))
                .willReturn(aResponse().withBody(
                        /*language=json*/
                        "[{\"feed_id\":1,\"height\":0.6,\"length\":3.6,\"offer_id\":\"123\",\"weight_gross\":3.48," +
                                "\"width\":2.7}]"))
        );

        List<Dimensions> response = httpIndexerApiClient
                .getDimensions(new SupplierFeedOfferIds(101L, 145L, singletonList(new FeedOfferId(1, "123"))));
        wireMockServer.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/v1/dimensions?offer=1-123&shop_id=101" +
                "&warehouse_id=145")));
        assertNotNull("Nonnull response", response);
        assertEquals("Only one record in response", 1, response.size());
        assertEquals(new Dimensions(1, "123",
                        BigDecimal.valueOf(348, 2),
                        BigDecimal.valueOf(6, 1),
                        BigDecimal.valueOf(36, 1),
                        BigDecimal.valueOf(27, 1)),
                response.get(0));
    }

    @Test
    public void dimensionsAsync() {
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/v1/dimensions"))
                .willReturn(aResponse().withBody(
                        /*language=json*/
                        "[{\"feed_id\":1,\"height\":0.6,\"length\":3.6,\"offer_id\":\"123\",\"weight_gross\":3.48," +
                                "\"width\":2.7}]"))
        );

        List<Dimensions> response = httpIndexerApiClient
                .getDimensions(new SupplierFeedOfferIds(101L, 145L, singletonList(new FeedOfferId(1, "123"))));
        wireMockServer.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/v1/dimensions?offer=1-123&shop_id=101" +
                "&warehouse_id=145")));
        assertNotNull("Nonnull response", response);
        assertEquals("Only one record in response", 1, response.size());
        assertEquals(new Dimensions(1, "123",
                        BigDecimal.valueOf(348, 2),
                        BigDecimal.valueOf(6, 1),
                        BigDecimal.valueOf(36, 1),
                        BigDecimal.valueOf(27, 1)),
                response.get(0));
    }

    @Test
    public void dimensionsAsyncBatch() {
        final int indexerBatchSize = 50;
        final int pages = 5;
        final int count = indexerBatchSize * pages;
        List<FeedOfferId> feedOfferIds = new ArrayList<>(count);

        for (int i = 0; i < pages; i++) {
            StringBuilder responseBuilder = new StringBuilder();
            StringBuilder requestBuilder = new StringBuilder();
            responseBuilder.append("[");
            String separator = "";

            for (int j = 0; j < indexerBatchSize; j++) {
                String offerid = Integer.toString(i * indexerBatchSize + j);
                responseBuilder.append(separator);
                responseBuilder.append("{\"feed_id\":1,\"height\":0.6,\"length\":3.6,\"offer_id\":\"")
                        .append(offerid)
                        .append("\",\"weight_gross\":3.48,\"width\":2.7}");
                separator = ",\n";
                feedOfferIds.add(new FeedOfferId(1, offerid));
                requestBuilder.append("offer=1-").append(offerid).append("&");
            }
            String request = requestBuilder.append("shop_id=101&warehouse_id=145").toString();
            String response = responseBuilder.append("]").toString();
            wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/v1/dimensions?" + request))
                    .willReturn(aResponse().withBody(response))
            );
        }
        List<Dimensions> response = httpIndexerApiClientAsync
                .getDimensions(new SupplierFeedOfferIds(101L, 145L, feedOfferIds));
        wireMockServer.verify(pages, WireMock.getRequestedFor(WireMock.urlPathEqualTo("/v1/dimensions")));
        assertNotNull("Nonnull response", response);
        assertEquals(count, response.size());
        int[] offers = response.stream().map(Dimensions::getOfferId).mapToInt(Integer::parseInt).distinct().toArray();
        assertEquals("Not found all offers", count, offers.length);
        assertTrue("Find offers non in query", IntStream.of(offers).allMatch(v -> v >= 0 && v < count));
    }

    private void mockServer() {
        WireMockConfiguration configuration = new WireMockConfiguration().dynamicPort();
        wireMockServer = new WireMockRule(configuration.dynamicHttpsPort());
        wireMockServer.start();
    }
}
