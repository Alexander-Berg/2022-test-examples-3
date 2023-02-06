package ru.yandex.market.api.partner.controllers.auction.recommendation.top.parser;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.api.partner.controllers.auction.MarketSearchTopQueriesRecommendationParser;
import ru.yandex.market.api.partner.controllers.auction.model.ReportOfferTopRecommendations;
import ru.yandex.market.api.partner.controllers.auction.model.TopRecommendationType;
import ru.yandex.market.core.AbstractParserTest;

/**
 * Тест парсера {@link MarketSearchTopQueriesRecommendationParser}
 *
 * @author chmilevfa@yandex-team.ru
 * @since 06.06.17
 */
class ReportTopQueriesRecommendationParserTest extends AbstractParserTest {

    /**
     * Проверка корректности обработки ответа с пустым списком оферов/рекомендаций
     */
    @Test
    void emptySearchResultTest() throws IOException {
        try (InputStream input = getContentStream("empty-search-results.xml")) {
            MarketSearchTopQueriesRecommendationParser parser = new MarketSearchTopQueriesRecommendationParser();
            parser.parse(input);
            ReportOfferTopRecommendations recommendations = parser.getRecommendations();
            assertNull(recommendations);
        }
    }

    /**
     * Проверка корректности обработки ответа с полными и корректными данными
     */
    @Test
    void offerWithRecommendationsTest() throws IOException {
        ReportOfferTopRecommendations expected = new ReportOfferTopRecommendations();
        //Заполняем информацию по оферу
        expected.setName("Телефон Apple iPhone SE 16GB 1723 (Розовое Золото) RU/A");
        expected.setUrl("https://store77.net/apple_iphone_se/telefon_apple_iphone_se_16gb_1723_rozovoe_zoloto_ru_a/");
        expected.setHyperId(13584121);
        expected.setHyperCategoryId(91491);
        expected.setWareMd5("Z-QaNRoRSKdx8QgKHd5tWQ");
        expected.setPrice(new BigDecimal(21290));
        expected.setPriceCurrency(Currency.RUR);
        expected.setBid(30);
        expected.setMinBid(16);

        //Заполняем данные по рекомендациям
        ReportOfferTopRecommendations.TopQueries topQueries1 = new ReportOfferTopRecommendations.TopQueries();
        topQueries1.setQuery("Apple iPhone SE 16");
        topQueries1.setType(TopRecommendationType.TOP_ALL);
        topQueries1.setAverageOfferPosition(59);
        topQueries1.setOfferShowCount(3192);
        topQueries1.setQueryShowCount(31);
        topQueries1.setModelCount(0);
        topQueries1.setQueryRecommendations(new ArrayList<>(Arrays.asList(
                new ReportOfferTopRecommendations.TopQueries.Recommendations(1, 456, 0),
                new ReportOfferTopRecommendations.TopQueries.Recommendations(2, 756, 0))
        ));

        ReportOfferTopRecommendations.TopQueries topQueries2 = new ReportOfferTopRecommendations.TopQueries();
        topQueries2.setQuery("iphone se");
        topQueries2.setType(TopRecommendationType.TOP_OFFER);
        topQueries2.setAverageOfferPosition(31);
        topQueries2.setOfferShowCount(30675);
        topQueries2.setQueryShowCount(20);
        topQueries2.setModelCount(2);
        topQueries2.setQueryRecommendations(new ArrayList<>(Arrays.asList(
                new ReportOfferTopRecommendations.TopQueries.Recommendations(1, 296, 0),
                new ReportOfferTopRecommendations.TopQueries.Recommendations(2, 396, 0))
        ));

        expected.setTopRecommendations(new ArrayList<>(Arrays.asList(topQueries1, topQueries2)));

        try (InputStream input = getContentStream("recommendations-ok.xml")) {
            MarketSearchTopQueriesRecommendationParser parser = new MarketSearchTopQueriesRecommendationParser();
            parser.parse(input);
            ReportOfferTopRecommendations actual = parser.getRecommendations();
            ReflectionAssert.assertLenientEquals(expected, actual);
        }
    }
}
