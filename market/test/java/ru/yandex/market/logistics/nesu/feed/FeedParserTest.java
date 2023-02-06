package ru.yandex.market.logistics.nesu.feed;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.enums.VatRate;
import ru.yandex.market.logistics.nesu.exception.feed.FeedConstraintViolationsException;
import ru.yandex.market.logistics.nesu.model.entity.Offer;
import ru.yandex.market.logistics.nesu.service.feed.parser.FeedParser;

import static java.lang.ClassLoader.getSystemResourceAsStream;

class FeedParserTest extends AbstractTest {

    private static final Offer VENDOR_MODEL_OFFER = new Offer()
        .setExternalId("9012")
        .setVatRate(VatRate.VAT_20)
        .setPrice(BigDecimal.valueOf(8990))
        .setName("Мороженица Brand 3811");

    private static final Offer ARTIST_TITLE_OFFER = new Offer()
        .setExternalId("12345")
        .setPrice(BigDecimal.valueOf(450))
        .setName("Pink Floyd Dark Side Of The Moon, Platinum Disc");

    private static final Offer BOOK_OFFER = new Offer()
        .setExternalId("12342")
        .setName("Все не так. В 2 томах. Том 1")
        .setPrice(BigDecimal.valueOf(80))
        .setName("Александра Маринина Все не так. В 2 томах. Том 1");

    private static final Offer AUDIO_BOOK_OFFER = new Offer()
        .setExternalId("12342")
        .setPrice(BigDecimal.valueOf(200))
        .setVatRate(VatRate.NO_VAT)
        .setName("Владимир Кунин Иванов и Рабинович, или \"Ай гоу ту Хайфа!\"");

    private static final Offer TOUR_OFFER = new Offer()
        .setExternalId("12341")
        .setPrice(BigDecimal.valueOf(24129))
        .setName("Египет Хургада Hilton 7 д.");

    private static final Offer OTHER_OFFER = new Offer()
        .setExternalId("12346")
        .setArticle("A1234567B")
        .setVatRate(VatRate.VAT_20)
        .setPrice(BigDecimal.valueOf(1490))
        .setName("Вафельница First FA-5300");

    @Test
    @DisplayName("Парсинг валидного yml файла фида")
    void parseGoodFeed() throws Exception {
        InputStream inputStream = getSystemResourceAsStream("feed/good-feed.xml");
        FeedParser feedParser = new FeedParser();
        List<Offer> actualOffers = feedParser.parse(inputStream);
        List<Offer> expectedOffers = List.of(
            VENDOR_MODEL_OFFER,
            ARTIST_TITLE_OFFER,
            BOOK_OFFER,
            AUDIO_BOOK_OFFER,
            TOUR_OFFER,
            OTHER_OFFER
        );
        softly.assertThat(actualOffers)
            .usingFieldByFieldElementComparator()
            .isEqualTo(expectedOffers);
    }

    @Test
    @DisplayName("Парсинг невалидного фида. У оффера отсутствуют обязательные параметры")
    void parseBadFeed() {
        InputStream inputStream = getSystemResourceAsStream("feed/bad-feed.xml");
        FeedParser feedParser = new FeedParser();
        softly.assertThatThrownBy(() -> feedParser.parse(inputStream))
            .isInstanceOf(FeedConstraintViolationsException.class);
    }
}
