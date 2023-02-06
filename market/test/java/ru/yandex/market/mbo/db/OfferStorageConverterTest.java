package ru.yandex.market.mbo.db;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.http.OffersStorage;

import java.util.Arrays;

public class OfferStorageConverterTest {

    @Test
    public void testPictureMeta() {
        OfferStorageConverter converter = new OfferStorageConverter();

        OffersStorage.GenerationDataOffer generationDataOffer = GenerationDataOfferMock.createOffer();
        OffersStorage.PictureMeta expectedPictureMeta = generationDataOffer.getPictureMeta();

        Offer convertedOffer = converter.convert(generationDataOffer);

        Assert.assertEquals(expectedPictureMeta, convertedOffer.getPictureMeta());
        Assert.assertTrue(Arrays.equals(
            expectedPictureMeta.toByteArray(),
            convertedOffer.getOfferData().getPictureMetaProto()
        ));
    }
}
