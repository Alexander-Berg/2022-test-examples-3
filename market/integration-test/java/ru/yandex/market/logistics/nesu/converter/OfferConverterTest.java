package ru.yandex.market.logistics.nesu.converter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.enums.VatRate;
import ru.yandex.market.logistics.nesu.model.dto.OfferDto;

import static ru.yandex.market.logistics.nesu.model.ModelFactory.EXTERNAL_ID;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.OFFER_ID;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.OFFER_NAME;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.PRICE;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.offer;

class OfferConverterTest extends AbstractContextualTest {
    @Autowired
    private OfferConverter offerConverter;

    @Test
    void convertOfferWithSomeNullFields() {
        OfferDto expected = new OfferDto(
            OFFER_ID,
            EXTERNAL_ID,
            OFFER_NAME,
            null,
            PRICE,
            null
        );
        OfferDto actual = offerConverter.toApi(offer());
        assertThatModelEquals(expected, actual);
    }

    @Test
    void convertFeedWithAllFieldsSet() {
        OfferDto expected = new OfferDto(
            OFFER_ID,
            EXTERNAL_ID,
            OFFER_NAME,
            "test-article",
            PRICE,
            VatRate.VAT_20
        );
        OfferDto actual = offerConverter.toApi(offer()
            .setVatRate(VatRate.VAT_20)
            .setArticle("test-article"));
        assertThatModelEquals(expected, actual);
    }
}
