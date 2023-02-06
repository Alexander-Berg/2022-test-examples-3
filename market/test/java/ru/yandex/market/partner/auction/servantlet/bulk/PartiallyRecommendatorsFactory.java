package ru.yandex.market.partner.auction.servantlet.bulk;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;
import org.xml.sax.SAXException;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.RecommendationType;
import ru.yandex.market.common.report.model.SearchResults;
import ru.yandex.market.common.report.model.json.converter.PrimeResultToFoundOffersConverter;
import ru.yandex.market.common.report.parser.json.PrimeSearchResultParser;
import ru.yandex.market.common.report.parser.xml.GeneralMarketReportXmlParserFactory;
import ru.yandex.market.core.auction.dto.report.ReportRecommendationsAnswerDto;
import ru.yandex.market.core.auction.marketreport.AuctionMarketSearchMarketReportXmlParser;
import ru.yandex.market.core.auction.marketreport.AuctionMarketSearchMarketReportXmlParserTest;
import ru.yandex.market.core.auction.recommend.BidRecommendationRequest;
import ru.yandex.market.core.auction.recommend.BidRecommendator;
import ru.yandex.market.core.auction.recommend.BidRecommendatorImpl;
import ru.yandex.market.core.auction.recommend.ParallelSearchBidRecommendator;
import ru.yandex.market.core.auction.recommend.ParallelSearchRecommendationParser;
import ru.yandex.market.core.auction.recommend.ReportMarketSearchBidRecommendator;
import ru.yandex.market.core.report.parser.ReportResponseXmlParser;
import ru.yandex.market.mbi.report.MarketSearchService;
import ru.yandex.market.partner.auction.servantlet.AuctionServantletMockBase;

import static org.hamcrest.CoreMatchers.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.auction.matchers.MarketSearchRequestMatchers.hasClient;
import static ru.yandex.market.core.auction.matchers.MarketSearchRequestMatchers.hasPlace;
import static ru.yandex.market.core.auction.matchers.MarketSearchRequestMatchers.hasQuery;
import static ru.yandex.market.core.auction.matchers.MarketSearchRequestMatchers.hasRecommendationType;
import static ru.yandex.market.partner.auction.matchers.ReportRecommendationSearchRequestFeatureMatchers.hasRecommendationType;

/**
 * Вспомогательная фактори, которая строит почти продовский вариант реализаций {@link BidRecommendator}, для тестов,
 * позволяя прогнать весь сценарий обработки ответа репорта представленного в виде файлика.
 * С точки зрения пользователя результат обработки будет отдаваться в ответе метода {@link BidRecommendator#calculate(BidRecommendationRequest)}.
 * <p>
 * PS важное отличие в том, что конфиг для парсеров берется не из прода, а из тестов.
 *
 * @author vbudnev
 */
public class PartiallyRecommendatorsFactory {

    public static BidRecommendatorImpl buildCardRecommendator(
            InputStream streamContainingReportAnswer
    ) throws IOException {
        final AsyncMarketReportService marketReportService = Mockito.mock(AsyncMarketReportService.class);
        mockAsyncServiceForCard(streamContainingReportAnswer, marketReportService);
        return new BidRecommendatorImpl(marketReportService);
    }

    public static void mockAsyncServiceForCard(
            InputStream streamContainingReportAnswer,
            AsyncMarketReportService marketReportService
    ) throws IOException {

        final ReportResponseXmlParser<ReportRecommendationsAnswerDto> parser
                = new ReportResponseXmlParser<>(ReportRecommendationsAnswerDto.class);

        parser.parse(streamContainingReportAnswer);

        doReturn(CompletableFuture.completedFuture(parser.getResult()))
                .when(marketReportService)
                .async(
                        MockitoHamcrest.argThat(
                                allOf(
                                        hasPlace(MarketReportPlace.BIDS_RECOMMENDER),
                                        hasRecommendationType(EnumSet.of(RecommendationType.CARD))
                                )
                        ),
                        any()
                );
    }

    public static void mockAsyncServiceForPrime(
            InputStream streamContainingReportAnswer,
            AsyncMarketReportService marketReportService,
            String query
    ) throws IOException {

        final PrimeResultToFoundOffersConverter converter = new PrimeResultToFoundOffersConverter(1, 1);
        final PrimeSearchResultParser<Pair<SearchResults, List<FoundOffer>>> parser
                = new PrimeSearchResultParser<>(converter);

        doReturn(CompletableFuture.completedFuture(parser.parse(streamContainingReportAnswer)))
                .when(marketReportService)
                .async(
                        MockitoHamcrest.argThat(
                                allOf(
                                        hasPlace(MarketReportPlace.PRIME),
                                        hasQuery(query),
                                        hasClient(AuctionServantletMockBase.PARTNER_INTERFACE_CLIENT)
                                )
                        ),
                        any()
                );
    }


    /**
     * @param streamContainingReportAnswer - стрим содержащий ответ ответ репорта. Удобно здесь подкладывать на базе
     *                                     ресурса файла в тесте.
     * @return рекомендатор для рекомендаций на поиске маркета
     * @throws IOException
     * @throws SAXException
     */
    public static BidRecommendator buildMarketSearchRecommendator(
            InputStream streamContainingReportAnswer
    ) throws IOException, SAXException {

        //настройки парсера берутся из тестовых настроек
        AuctionMarketSearchMarketReportXmlParser parser = new AuctionMarketSearchMarketReportXmlParser(
                AuctionMarketSearchMarketReportXmlParserTest.createMinimalTestSettings()
        );

        //устанавливаем для парсера содержимое ответа репорта
        parser.parseXmlStream(streamContainingReportAnswer);

        GeneralMarketReportXmlParserFactory parserFactory = Mockito.mock(GeneralMarketReportXmlParserFactory.class);
        when(parserFactory.newParser())
                .thenReturn(parser);

        AsyncMarketReportService marketReportService = Mockito.mock(AsyncMarketReportService.class);
        doReturn(CompletableFuture.completedFuture(parser)).
                when(marketReportService)
                .async(
                        MockitoHamcrest.argThat(
                                allOf(
                                        hasPlace(MarketReportPlace.BIDS_RECOMMENDER),
                                        hasRecommendationType(EnumSet.of(RecommendationType.MARKET_SEARCH))
                                )
                        ),
                        any()
                );

        ReportMarketSearchBidRecommendator recommendator
                = new ReportMarketSearchBidRecommendator(parserFactory, marketReportService);

        return recommendator;
    }

    /**
     * @param streamContainingReportAnswer - стрим содержащий ответ ответ репорта. Удобно здесь подкладывать на базе
     *                                     ресурса файла в тесте.
     * @return рекомендатор для рекомендаций на параллельном поиске
     * @throws IOException
     * @throws SAXException
     */
    public static BidRecommendator buildParallelSearchRecommendator(
            InputStream streamContainingReportAnswer
    ) throws IOException, SAXException {
        ParallelSearchRecommendationParser parser = new ParallelSearchRecommendationParser();
        //устанавливаем для парсера содержимое ответа репорта
        parser.parseXmlStream(streamContainingReportAnswer);

        MarketSearchService marketSearchService = Mockito.mock(MarketSearchService.class);
        doReturn(CompletableFuture.completedFuture(parser))
                .when(marketSearchService)
                .executeAsync(
                        MockitoHamcrest.argThat(
                                hasRecommendationType(RecommendationType.SEARCH)
                        ),
                        any(ParallelSearchRecommendationParser.class)
                );

        return new ParallelSearchBidRecommendator(marketSearchService, 1);
    }


}
