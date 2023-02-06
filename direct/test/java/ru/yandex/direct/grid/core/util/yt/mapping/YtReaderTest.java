package ru.yandex.direct.grid.core.util.yt.mapping;

import java.math.BigDecimal;

import org.jooq.types.ULong;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.grid.core.entity.offer.model.GdiOffer;
import ru.yandex.direct.grid.core.entity.offer.model.GdiOfferId;
import ru.yandex.direct.grid.schema.yt.tables.OfferattributesBs;
import ru.yandex.direct.grid.schema.yt.tables.OfferstatBs;
import ru.yandex.direct.jooqmapper.read.JooqReaderWithSupplierBuilder;
import ru.yandex.direct.ytwrapper.dynamic.dsl.YtMappingUtils;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.schema.yt.Tables.OFFERATTRIBUTES_BS;
import static ru.yandex.direct.grid.schema.yt.Tables.OFFERSTAT_BS;
import static ru.yandex.direct.jooqmapper.read.ReaderBuilders.fromField;

public class YtReaderTest {
    private static final OfferstatBs OFFER_STAT = OFFERSTAT_BS.as("OS");
    private static final OfferattributesBs OFFER_ATTRIBUTES = OFFERATTRIBUTES_BS.as("OA");

    private YtReader<GdiOffer> offerReader;

    @Before
    public void before() {
        offerReader = new YtReader<>(JooqReaderWithSupplierBuilder.builder(GdiOffer::new)
                .readProperty(GdiOffer.ID, JooqReaderWithSupplierBuilder.builder(GdiOfferId::new)
                        .readProperty(GdiOfferId.BUSINESS_ID, fromField(OFFER_STAT.BUSINESS_ID))
                        .readProperty(GdiOfferId.SHOP_ID, fromField(OFFER_STAT.SHOP_ID))
                        .readProperty(GdiOfferId.OFFER_YABS_ID, fromField(OFFER_STAT.OFFER_YABS_ID)
                                .by(ULong::longValue))
                        .build())
                .readPropertyForFirst(GdiOffer.NAME, fromField(OFFER_ATTRIBUTES.NAME))
                .readPropertyForFirst(GdiOffer.URL, fromField(OFFER_ATTRIBUTES.URL))
                .readPropertyForFirst(GdiOffer.IMAGE_URL, fromField(OFFER_ATTRIBUTES.PICTURE_URL))
                .readPropertyForFirst(GdiOffer.PRICE, fromField(OFFER_ATTRIBUTES.PRICE).by(YtMappingUtils::fromMicros))
                .readPropertyForFirst(GdiOffer.CURRENCY_ISO_CODE, fromField(OFFER_ATTRIBUTES.CURRENCY_NAME))
                .build());
    }

    @Test
    public void testYtReader() {
        YTreeMapNode node = YTree.builder()
                .beginMap()
                .key("OS_BusinessId").value(111L)
                .key("OS_ShopId").value(123L)
                .key("OS_OfferYabsId").value(31L)
                .key("OA_Name").value("An Offer")
                .key("OA_Url").value("https://example.com")
                .key("OA_PictureUrl").value("https://example.com/picture.jpg")
                .key("OA_Price").value(321670000L)
                .key("OA_CurrencyName").value("RUR")
                .buildMap();

        GdiOffer offer = offerReader.fromYTreeRow(node);

        GdiOffer expected = new GdiOffer()
                .withId(new GdiOfferId()
                        .withBusinessId(111L)
                        .withShopId(123L)
                        .withOfferYabsId(31L))
                .withName("An Offer")
                .withUrl("https://example.com")
                .withImageUrl("https://example.com/picture.jpg")
                .withPrice(BigDecimal.valueOf(321.67))
                .withCurrencyIsoCode("RUR");
        assertThat(offer).isEqualTo(expected);
    }
}
