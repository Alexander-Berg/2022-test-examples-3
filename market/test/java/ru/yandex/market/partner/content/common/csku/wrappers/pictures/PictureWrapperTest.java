package ru.yandex.market.partner.content.common.csku.wrappers.pictures;

import java.util.EnumSet;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.cleanweb.client.CWImageResult;
import ru.yandex.market.cleanweb.client.ImageVerdict;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.ModificationSource;

import static org.junit.Assert.assertEquals;

public class PictureWrapperTest {

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    private final CWImageResult resultWithText = Mockito.mock(CWImageResult.class);
    private final CWImageResult resultWithoutText = Mockito.mock(CWImageResult.class);

    @Before
    public void setUp() {
        Mockito.when(resultWithText.getTotalVerdict())
                .thenReturn(EnumSet.of(ImageVerdict.IS_GOOD, ImageVerdict.NOT_EROTICA, ImageVerdict.HAS_TEXT));
        Mockito.when(resultWithoutText.getTotalVerdict())
                .thenReturn(EnumSet.of(ImageVerdict.IS_GOOD, ImageVerdict.NOT_EROTICA));
    }

    //true true true true
    @Test
    public void useSkuPictureIfAllTrueSameResolution() {
        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithoutText);
        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithoutText);
        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(skuWrapper.getPicture(), resultWrapper.getPicture());
    }

    //false false false false
    @Test
    public void useSkuPictureIfAllFalseSameResolution() {
        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, false), resultWithText);
        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, false), resultWithText);
        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(skuWrapper.getPicture(), resultWrapper.getPicture());
    }

    //true true true false
    @Test
    public void skuWithoutTextWhiteOfferWithoutTextNotWhite() {
        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithoutText);
        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, false), resultWithoutText);
        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(skuWrapper.getPicture(), resultWrapper.getPicture());
    }

    //true true false true
    @Test
    public void skuWithoutTextWhiteOfferWithTextWhite() {
        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithoutText);
        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithText);
        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(skuWrapper.getPicture(), resultWrapper.getPicture());
    }

    //true true false false
    @Test
    public void skuWithoutTextWhiteOfferWithTextNotWhite() {
        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithoutText);
        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, false), resultWithText);
        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(skuWrapper.getPicture(), resultWrapper.getPicture());
    }

    //true false true true
    @Test
    public void skuWithoutTextNotWhiteOfferWithoutTextWhite() {
        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, false), resultWithoutText);
        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithoutText);
        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(offerWrapper.getPicture(), resultWrapper.getPicture());
    }

    //true false true false
    @Test
    public void skuWithoutTextNotWhiteOfferWithoutTextNotWhite() {
        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, false), resultWithoutText);
        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, false), resultWithoutText);
        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(skuWrapper.getPicture(), resultWrapper.getPicture());
    }

    //true false false true
    @Test
    public void skuWithoutTextNotWhiteOfferWithTextWhite() {
        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, false), resultWithoutText);
        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithText);
        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(skuWrapper.getPicture(), resultWrapper.getPicture());
    }

    //true false false false
    @Test
    public void skuWithoutTextNotWhiteOfferWithTextNotWhite() {
        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, false), resultWithoutText);
        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, false), resultWithText);
        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(skuWrapper.getPicture(), resultWrapper.getPicture());
    }

    //false true true true
    @Test
    public void skuWithTextWhiteOfferWithoutTextWhite() {
        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithText);
        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithoutText);
        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(offerWrapper.getPicture(), resultWrapper.getPicture());
    }

    //false true true false
    @Test
    public void skuWithTextWhiteOfferWithoutTextNotWhite() {
        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithText);
        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, false), resultWithoutText);
        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(offerWrapper.getPicture(), resultWrapper.getPicture());
    }

    //false true false true
    @Test
    public void skuWithTextWhiteOfferWithTextWhite() {
        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithText);
        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithText);
        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(skuWrapper.getPicture(), resultWrapper.getPicture());
    }

    //false true false false
    @Test
    public void skuWithTextWhiteOfferWithTextNotWhite() {
        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithText);
        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, false), resultWithText);
        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(skuWrapper.getPicture(), resultWrapper.getPicture());
    }

    //false false true true
    @Test
    public void skuWithTextNotWhiteOfferWithoutTextWhite() {
        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, false), resultWithText);
        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithoutText);
        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(offerWrapper.getPicture(), resultWrapper.getPicture());
    }

    //false false true false
    @Test
    public void skuWithTextNOtWhiteOfferWithoutTextNotWhite() {
        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, false), resultWithText);
        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, false), resultWithoutText);
        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(offerWrapper.getPicture(), resultWrapper.getPicture());
    }

    //false false false true
    @Test
    public void skuWithTextNotWhiteOfferWithTextWhite() {
        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, false), resultWithText);
        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithText);
        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(offerWrapper.getPicture(), resultWrapper.getPicture());
    }

    @Test
    public void allSameUnknownSkuSize() {
        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithoutText);

        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPicture(0, 0, true), resultWithoutText);
        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(skuWrapper.getPicture(), resultWrapper.getPicture());

        skuWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, 0, true), resultWithoutText);
        resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(skuWrapper.getPicture(), resultWrapper.getPicture());

        skuWrapper = PictureWrapper.forSku(
                buildPicture(0, HEIGHT, true), resultWithoutText);
        resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(skuWrapper.getPicture(), resultWrapper.getPicture());
    }

    @Test
    public void allSameEqualSize() {
        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithoutText);
        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithoutText);
        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(skuWrapper.getPicture(), resultWrapper.getPicture());
    }

    @Test
    public void allSameOfferSizeBetter() {
        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithoutText);

        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT + 5, true), resultWithoutText);
        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(offerWrapper.getPicture(), resultWrapper.getPicture());

        offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH + 5, HEIGHT, true), resultWithoutText);
        resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(offerWrapper.getPicture(), resultWrapper.getPicture());

        offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH + 5, HEIGHT + 5, true), resultWithoutText);
        resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(offerWrapper.getPicture(), resultWrapper.getPicture());

        offerWrapper = PictureWrapper.forSku(
                buildPicture((int) (WIDTH * 0.81), (int) (HEIGHT * 1.2), true), resultWithoutText);
        resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(skuWrapper.getPicture(), resultWrapper.getPicture());

        offerWrapper = PictureWrapper.forSku(
                buildPicture((int) (WIDTH * 0.85), (int) (HEIGHT * 1.2), true), resultWithoutText);
        resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(offerWrapper.getPicture(), resultWrapper.getPicture());
    }

    @Test
    public void operatorPictureHasHighestPriority() {
        PictureWrapper skuWrapper = PictureWrapper.forSku(
                buildPictureWithValueSource(WIDTH - 1, HEIGHT - 1, false,
                        ModificationSource.OPERATOR_FILLED),
                resultWithText);
        PictureWrapper offerWrapper = PictureWrapper.forSku(
                buildPicture(WIDTH, HEIGHT, true), resultWithoutText);

        PictureWrapper resultWrapper = PictureWrapper.chooseTheBest(skuWrapper, offerWrapper);
        assertEquals(skuWrapper.getPicture(), resultWrapper.getPicture());
    }

    @NotNull
    private ModelStorage.Picture buildPicture(int width, int height, boolean isWhiteBackground) {
        return buildPictureWithValueSource(width, height, isWhiteBackground,
                ModificationSource.VENDOR_OFFICE);
    }

    @NotNull
    private ModelStorage.Picture buildPictureWithValueSource(int width, int height, boolean isWhiteBackground,
                                                             ModificationSource valueSource) {
        return ModelStorage.Picture.newBuilder()
                .setWidth(width)
                .setHeight(height)
                .setIsWhiteBackground(isWhiteBackground)
                .setValueSource(valueSource)
                .build();
    }
}
