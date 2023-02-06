package ru.yandex.market.core.report.client.model;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.core.report.parser.ReportResponseJsonParser;

/**
 * Тест на парсинг ответа репорта по плейсу
 * {@link ru.yandex.market.common.report.model.MarketReportPlace#PRICE_RECOMMENDER рекомендация цены}.
 *
 * @author fbokovikov
 */
public class PriceRecommendationsDtoParsingTest {

    private final static ReportResponseJsonParser<PriceRecommendationsDTO[]> PARSER =
            new ReportResponseJsonParser<>(PriceRecommendationsDTO[].class);
    private final InputStream RESPONSE = this.getClass().getResourceAsStream("prices.json");

    @Test
    public void testParsing() throws IOException {
        PARSER.parse(RESPONSE);
        ReflectionAssert.assertReflectionEquals(
                new PriceRecommendationsDTO[]{
                        new PriceRecommendationsDTO(
                                "100500",
                                ImmutableList.of(
                                        new PriceRecommendationsDTO.PriceRecommendationDTO(
                                                BigDecimal.valueOf(100L),
                                                BigDecimal.valueOf(500L)
                                        ),
                                        new PriceRecommendationsDTO.PriceRecommendationDTO(
                                                BigDecimal.valueOf(100L),
                                                BigDecimal.valueOf(500L)
                                        )
                                )
                        ),
                        new PriceRecommendationsDTO(
                                "100502",
                                ImmutableList.of(
                                        new PriceRecommendationsDTO.PriceRecommendationDTO(
                                                BigDecimal.valueOf(200L),
                                                BigDecimal.valueOf(400L)
                                        ),
                                        new PriceRecommendationsDTO.PriceRecommendationDTO(
                                                BigDecimal.valueOf(400L),
                                                BigDecimal.valueOf(800L)
                                        )
                                )
                        ),
                        new PriceRecommendationsDTO(
                                "100504",
                                ImmutableList.of(
                                        new PriceRecommendationsDTO.PriceRecommendationDTO(
                                                BigDecimal.valueOf(99995, 2),
                                                BigDecimal.valueOf(5, 1)
                                        ),
                                        new PriceRecommendationsDTO.PriceRecommendationDTO(
                                                BigDecimal.valueOf(199995, 2),
                                                BigDecimal.valueOf(15, 1)
                                        )
                                )
                        )
                },
                PARSER.getResult()
        );
        RESPONSE.close();
    }
}
