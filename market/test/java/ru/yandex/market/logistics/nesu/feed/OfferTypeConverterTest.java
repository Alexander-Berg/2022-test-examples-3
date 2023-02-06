package ru.yandex.market.logistics.nesu.feed;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.enums.OfferType;
import ru.yandex.market.logistics.nesu.service.feed.parser.OfferTypeConverter;


class OfferTypeConverterTest extends AbstractTest {
    private final OfferTypeConverter converter = new OfferTypeConverter();

    @Test
    void convert() {
        softly.assertThat(converter.unmarshal("vendor.model")).isEqualTo(OfferType.VENDOR_MODEL);
        softly.assertThat(converter.unmarshal("artist.title")).isEqualTo(OfferType.ARTIST_TITLE);
        softly.assertThat(converter.unmarshal("book")).isEqualTo(OfferType.BOOK);
        softly.assertThat(converter.unmarshal("audiobook")).isEqualTo(OfferType.AUDIOBOOK);
        softly.assertThat(converter.unmarshal("tour")).isEqualTo(OfferType.TOUR);
        softly.assertThat(converter.unmarshal("unknown_offer_type")).isEqualTo(OfferType.OTHER);
        softly.assertThat(converter.unmarshal(null)).isEqualTo(OfferType.OTHER);
    }
}
