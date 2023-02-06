package ru.yandex.market.api.partner.report;

import java.io.InputStream;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductOffersReportParserTest {
    ProductOffersReportParser parser = new ProductOffersReportParser();

    @Test
    public void testDifferentReportUrlsForDifferentPlaces() throws Exception {
        try (InputStream dataStream = getClass().getResourceAsStream("productOffers.json")) {
            parser.parse(dataStream);
            OfferSearchResponse response = parser.getResult();
            assertThat(response).isNotNull();
            assertThat(response.getSearchResponse()).isNotNull();
            assertThat(response.getSearchResponse().getResults()).isNotNull();
            assertThat(response.getSearchResponse().getResults()).hasSize(2);

            assertThat(response.getSearchResponse().getResults().get(0).getTitles().getRaw()).isEqualTo("Apple iPhone 12 Pro 128GB Тихоокеанский Синий");
            assertThat(response.getSearchResponse().getResults().get(1).getTitles().getRaw()).isEqualTo("Смартфон Apple iPhone 12 Pro 128GB Graphite (графитовый)");

            assertThat(response.getSearchResponse().getResults().get(0).getPrices().getValue()).isEqualTo(BigDecimal.valueOf(80380));
            assertThat(response.getSearchResponse().getResults().get(1).getPrices().getValue()).isEqualTo(BigDecimal.valueOf(81889));


            assertThat(response.getSearchResponse().getResults().get(0).getPrices().getDiscount().getPercent()).isEqualTo(8);
            assertThat(response.getSearchResponse().getResults().get(1).getPrices().getDiscount().getPercent()).isEqualTo(8);


            assertThat(response.getSearchResponse().getResults().get(0).getPrices().getDiscount().getOldMin()).isEqualTo(BigDecimal.valueOf(31840));
            assertThat(response.getSearchResponse().getResults().get(1).getPrices().getDiscount().getOldMin()).isEqualTo(BigDecimal.valueOf(31840));

            assertThat(response.getSearchResponse().getResults().get(0).getShop().getName()).isEqualTo("GyroShOp");
            assertThat(response.getSearchResponse().getResults().get(1).getShop().getName()).isEqualTo("WISHMASTER");

            assertThat(response.getSearchResponse().getResults().get(0).getShop().getQualityRating()).isEqualTo(5);
            assertThat(response.getSearchResponse().getResults().get(1).getShop().getQualityRating()).isEqualTo(5);

            assertThat(response.getSearchResponse().getResults().get(0).getDelivery().getInStock()).isFalse();
            assertThat(response.getSearchResponse().getResults().get(1).getDelivery().getInStock()).isTrue();

        }
    }
}
