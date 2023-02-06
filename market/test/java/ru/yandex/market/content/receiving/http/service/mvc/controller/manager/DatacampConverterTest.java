package ru.yandex.market.content.receiving.http.service.mvc.controller.manager;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampExplanation;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMarketContent;
import Market.DataCamp.DataCampOfferPictures;
import org.junit.Test;

import ru.yandex.market.ir.http.OfferContentProcessing;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.partner.content.SourceController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DatacampConverterTest {


    private DatacampConverter converter = new DatacampConverter(mock(SourceController.class));

    @Test
    public void testRemoveThumbnails() {
        DataCampOffer.Offer offer = initOffer();
        Timestamp now = Timestamp.from(Instant.now());
        List<DatacampOffer> jooqOffers = converter.convertToJooqPojo(Collections.singletonList(offer), now, now, false);

        assertThat(jooqOffers).hasSize(1);
        DatacampOffer datacampOffer = jooqOffers.get(0);
        Map<String, DataCampOfferPictures.MarketPicture> actualMap =
                datacampOffer.getData().getPictures().getPartner().getActualMap();
        assertThat(actualMap).hasSize(2);
        actualMap.forEach((s, marketPicture) -> {
            assertThat(marketPicture.getId()).isNotNull();
            assertThat(marketPicture.getOriginal()).isNotNull();
            assertThat(marketPicture.getThumbnailsList()).isEmpty();
        });
    }


    @Test
    public void testRemoveProcessingResponse() {
        DataCampOffer.Offer offer = initOffer();
        Timestamp now = Timestamp.from(Instant.now());
        List<DatacampOffer> jooqOffers = converter.convertToJooqPojo(Collections.singletonList(offer), now, now, false);

        assertThat(jooqOffers).hasSize(1);
        DatacampOffer datacampOffer = jooqOffers.get(0);
        assertThat(datacampOffer.getData().getContent().getPartner().getMarketSpecificContent()
                .getProcessingResponse().getItemsList()).hasSize(0);
    }

    @Test
    public void testEmptyOffer() {
        DataCampOffer.Offer offer = DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setBusinessId(1)
                        .setOfferId("1")
                        .build())
                .build();
        Timestamp now = Timestamp.from(Instant.now());
        List<DatacampOffer> jooqOffers = converter.convertToJooqPojo(Collections.singletonList(offer), now, now, false);

        assertThat(jooqOffers).hasSize(1);
        DatacampOffer datacampOffer = jooqOffers.get(0);
        assertThat(datacampOffer.getData().getContent().getPartner().hasMarketSpecificContent()).isFalse();
    }

    @Test
    public void testConvertOfferToJooq() {
        OfferContentProcessing.OfferContentProcessingRequest.OfferWithFlags offer =
                OfferContentProcessing.OfferContentProcessingRequest.OfferWithFlags.newBuilder()
                .setOffer(
                        DataCampOffer.Offer.newBuilder()
                                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                        .setBusinessId(1)
                                        .setOfferId("1")
                                        .build()
                                )
                )
                .setAllowFastSkuCreation(true)
                .build();
        Timestamp now = Timestamp.from(Instant.now());
        List<DatacampOffer> jooqOffers = converter.convertToJooqPojoNew(Collections.singletonList(offer), now, now);

        assertThat(jooqOffers).hasSize(1);
        DatacampOffer datacampOffer = jooqOffers.get(0);
        assertThat(datacampOffer.getData().getContent().getPartner().hasMarketSpecificContent()).isFalse();
        assertThat(datacampOffer.getIsAllowFastSkuCreation()).isTrue();
    }

    private DataCampOffer.Offer initOffer() {
        return DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setBusinessId(1)
                        .setOfferId("1")
                        .build())
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                                .setMarketSpecificContent(DataCampOfferMarketContent.MarketSpecificContent.newBuilder()
                                        .setProcessingResponse(DataCampOfferMarketContent.MarketContentProcessing.newBuilder()
                                                .addItems(DataCampOfferMarketContent.MarketContentProcessing.Item.newBuilder()
                                                        .setMessage(DataCampExplanation.Explanation.newBuilder().setCode("code")
                                                                .setText("text")
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .setPictures(DataCampOfferPictures.OfferPictures.newBuilder()
                        .setPartner(DataCampOfferPictures.PartnerPictures.newBuilder()
                                .putActual("img1", DataCampOfferPictures.MarketPicture.newBuilder()
                                        .setId("id1")
                                        .setOriginal(DataCampOfferPictures.MarketPicture.Picture.newBuilder()
                                                .setUrl("url1"))
                                        .addThumbnails(DataCampOfferPictures.MarketPicture.Picture.newBuilder()
                                                .setUrl("thumbnails-url1"))
                                        .addThumbnails(DataCampOfferPictures.MarketPicture.Picture.newBuilder()
                                                .setUrl("thumbnails-url2"))
                                        .build())
                                .putActual("img2", DataCampOfferPictures.MarketPicture.newBuilder()
                                        .setId("id2")
                                        .setOriginal(DataCampOfferPictures.MarketPicture.Picture.newBuilder()
                                                .setUrl("url2"))
                                        .build())
                                .build())
                        .build())
                .build();
    }
}
