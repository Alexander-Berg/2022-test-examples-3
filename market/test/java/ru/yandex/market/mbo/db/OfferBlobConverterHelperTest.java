package ru.yandex.market.mbo.db;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.http.OffersStorage;

import java.io.IOException;

public class OfferBlobConverterHelperTest {

    @Test
    public void testByteArrayToPictureMeta() throws IOException {
        OffersStorage.GenerationDataOffer generationDataOffer = GenerationDataOfferMock.createOffer();

        OffersStorage.PictureMeta pictureMeta = OfferBlobConverterHelper.byteArrayToPictureMeta(
            generationDataOffer.getPictureMeta().toByteArray()
        );

        Assert.assertEquals(generationDataOffer.getPictureMeta(), pictureMeta);
    }

    @Test
    public void testByteArrayToPictureMetaIfNull() {
        OffersStorage.PictureMeta convertedPictureMeta = OfferBlobConverterHelper.byteArrayToPictureMeta(null);
        Assert.assertNull(convertedPictureMeta);
    }

    @Test
    public void testByteArrayToPictureMetaIfEmpty() {
        byte[] bytes = new byte[0];
        OffersStorage.PictureMeta convertedPictureMeta = OfferBlobConverterHelper.byteArrayToPictureMeta(bytes);
        Assert.assertNull(convertedPictureMeta);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testByteArrayToPictureMetaIfWrong() {
        OfferBlobConverterHelper.byteArrayToPictureMeta("wrong".getBytes());
    }
}
