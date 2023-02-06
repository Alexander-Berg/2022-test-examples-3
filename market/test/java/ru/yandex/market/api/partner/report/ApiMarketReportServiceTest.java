package ru.yandex.market.api.partner.report;

import java.util.Map;

import javax.validation.constraints.NotNull;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.common.parser.InputStreamParser;
import ru.yandex.market.common.parser.Parsers;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.MarketSearchRequest;
import ru.yandex.market.common.report.model.SearchType;
import ru.yandex.market.common.util.AsyncRetryHttpClient;

public class ApiMarketReportServiceTest {

    // Сервер не поднимаем, этот URL нужен только чтобы сделать запрос
    private final static String BASE_URL = "http://localhost:5555";
    private ApiMarketReportService marketReportService;
    private AsyncRetryHttpClient commonMarketSearchClient;

    // В тестинге используются одинаковые URL для разных SearchType
    // Сделал более наглядно
    private final static Map<SearchType, String> TYPE_TO_URL_MAPPING = Map.of(
            SearchType.MARKET_LOW_LATENCY, BASE_URL + "/low_latency_url",
            SearchType.MARKET, BASE_URL + "/market_url",
            SearchType.MAXCPMS, BASE_URL + "/maxcpms_url",
            SearchType.PARALLEL, BASE_URL + "/parallel_url"
    );

    @BeforeEach
    public void setUp() throws Exception {
        initReportService();
    }

    @Test
    public void testDifferentReportUrlsForDifferentPlaces() throws Exception {
        Map<MarketReportPlace, SearchType> placesToTypes = Map.of(

                // Эти правила определены в MarketServiceSupportWithLowLatencyPlaces
                MarketReportPlace.PRIME, SearchType.MARKET_LOW_LATENCY,
                MarketReportPlace.MIPRIME, SearchType.MARKET_LOW_LATENCY,
                MarketReportPlace.PARTNER_OFFER_COUNTS, SearchType.MARKET_LOW_LATENCY,
                MarketReportPlace.PARTNER_MODEL_OFFERS, SearchType.MARKET_LOW_LATENCY,
                MarketReportPlace.PRODUCT_OFFERS, SearchType.MARKET_LOW_LATENCY,

                // Эти правила определены в DefaultMarketServiceSupport
                MarketReportPlace.CHECK_OFFERS, SearchType.MARKET_LOW_LATENCY,
                MarketReportPlace.MAXCPMS, SearchType.MAXCPMS,
                MarketReportPlace.PARALLEL, SearchType.PARALLEL
        );

        for (Map.Entry<MarketReportPlace, SearchType> entry : placesToTypes.entrySet()) {
            assertReportRequest(entry.getKey(), getParserStub(), entry.getValue());
        }
    }

    private void assertReportRequest(MarketReportPlace place,
                                     InputStreamParser parser,
                                     SearchType searchType) throws Exception {

        AsyncRetryHttpClient searchClient = Mockito.spy(commonMarketSearchClient);
        marketReportService.setAsyncRetryClient(searchClient);
        marketReportService.executeSearchAndParse(new MarketSearchRequest(place), Parsers.itself(parser));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(searchClient).execute(urlCaptor.capture(), Mockito.any(), Mockito.any());

        Assertions.assertTrue(urlCaptor.getValue().startsWith(TYPE_TO_URL_MAPPING.get(searchType) + "?"));
    }

    @NotNull
    private InputStreamParser getParserStub() {
        return in -> {
            // для этих тестов ничего не делает
        };
    }

    private void initReportService() throws Exception {
        marketReportService = new ApiMarketReportService();
        marketReportService.setUrls(TYPE_TO_URL_MAPPING);
        commonMarketSearchClient = new AsyncRetryHttpClient();
        commonMarketSearchClient.setMaxConnectionPerRoute(1);
        commonMarketSearchClient.setMaxRetryCount(1);
        commonMarketSearchClient.setMaxConnectionTotal(1);
        commonMarketSearchClient.afterPropertiesSet();
    }
}
