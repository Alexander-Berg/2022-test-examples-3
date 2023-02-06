package ru.yandex.market.global.index.domain.offer;

import Market.DataCamp.DataCampOfferPictures;
import Market.DataCamp.DataCampUnitedOffer;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.index.BaseLocalTest;
import ru.yandex.market.global.index.domain.cleanweb.CleanWebSender;

@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CleanWebSenderLocalTest extends BaseLocalTest {
    private final CleanWebSender cleanWebSender;

    @Test
    public void test() {
        DataCampUnitedOffer.UnitedOffer.Builder offer = DataCampUnitedOffer.UnitedOffer.newBuilder();

        offer.getBasicBuilder().getIdentifiersBuilder().setBusinessId(1);
        offer.getBasicBuilder().getIdentifiersBuilder().setOfferId("testofferId123");

        offer.getBasicBuilder().getContentBuilder().getPartnerBuilder().getOriginalBuilder().getNameBuilder()
                .setValue("Test offer name");

        offer.getBasicBuilder().getContentBuilder().getPartnerBuilder().getOriginalBuilder().getDescriptionBuilder()
                .setValue("Test offer description");

        offer.getBasicBuilder().getPicturesBuilder().getPartnerBuilder().putActual("source_picture_url",
                DataCampOfferPictures.MarketPicture.newBuilder()
                        .setOriginal(DataCampOfferPictures.MarketPicture.Picture.newBuilder()
                                .setUrl("//avatars.mds.yandex.net/get-marketpic/1848728/picb0e32999c38e67fe6ec349c98efc5f2c/orig")
                                .build()
                        ).build()
        );
        cleanWebSender.send(offer.build());
    }
}
