package ru.yandex.market.mbo.gwt.models.image;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.ImageType;
import ru.yandex.market.mbo.gwt.utils.XslNames;

/**
 * @author s-ermakov
 */
public class ImageTypeMatchesTest {

    private static final String XL_PICTURE = "XL-Picture";
    private static final String XL_PICTURE_X = "XLPictureSizeX";
    private static final String XL_PICTURE_Y = "XLPictureSizeY";
    private static final String XL_PICTURE_URL = "XLPictureUrl";
    private static final String XL_PICTURE_RAW_URL = "XLPictureOrig";

    private static final String XL_PICTURE_MDATA = "XL-Picture_mdata";
    private static final String XL_PICTURE_BROKEN = "XL_Picture";

    private static final String XL_PICTURE_2 = "XL-Picture_2";
    private static final String XL_PICTURE_X_2 = "XLPictureSizeX_2";
    private static final String XL_PICTURE_Y_2 = "XLPictureSizeY_2";
    private static final String XL_PICTURE_URL_2 = "XLPictureUrl_2";
    private static final String XL_PICTURE_RAW_URL_2 = "XLPictureOrig_2";

    private static final String XL_PICTURE_2_MDATA = "XL-Picture_2_mdata";
    private static final String XL_PICTURE_2_BROKEN = "XL_Picture_2";

    @Test
    public void testMatches() {
        Assert.assertTrue(ImageType.XL_PICTURE.matches(XL_PICTURE));
        Assert.assertTrue(!ImageType.XL_PICTURE.matches(XL_PICTURE_MDATA));
        Assert.assertTrue(!ImageType.XL_PICTURE.matches(XL_PICTURE_BROKEN));
        Assert.assertTrue(!ImageType.XL_PICTURE.matches(XslNames.PREVIEW));
        Assert.assertTrue(!ImageType.XL_PICTURE.matches(XL_PICTURE_URL));

        Assert.assertTrue(ImageType.XL_PICTURE.matches(XL_PICTURE_2));
        Assert.assertTrue(!ImageType.XL_PICTURE.matches(XL_PICTURE_2_MDATA));
        Assert.assertTrue(!ImageType.XL_PICTURE.matches(XL_PICTURE_2_BROKEN));
        Assert.assertTrue(!ImageType.XL_PICTURE.matches(XslNames.ANNOUNCE_DATE));
        Assert.assertTrue(!ImageType.XL_PICTURE.matches(XL_PICTURE_Y_2));

        Assert.assertTrue(!ImageType.UNKNOWN.matches(XL_PICTURE));
        Assert.assertTrue(!ImageType.UNKNOWN.matches(XL_PICTURE_2));
        Assert.assertTrue(ImageType.UNKNOWN.matches(XL_PICTURE_RAW_URL));
        Assert.assertTrue(ImageType.UNKNOWN.matches(XL_PICTURE_X_2));
        Assert.assertTrue(ImageType.UNKNOWN.matches(XL_PICTURE_2_BROKEN));
        Assert.assertTrue(ImageType.UNKNOWN.matches(XslNames.NAME));
        Assert.assertTrue(ImageType.UNKNOWN.matches(XslNames.PREVIEW));
        Assert.assertTrue(ImageType.UNKNOWN.matches(XslNames.NAME));
    }

    @Test
    public void testWidthMethods() {
        Assert.assertTrue(ImageType.XL_PICTURE.isWidthParam(XL_PICTURE_X));
        Assert.assertTrue(!ImageType.XL_PICTURE.isWidthParam(XL_PICTURE_URL));
        Assert.assertEquals(XL_PICTURE_X, ImageType.XL_PICTURE.getWidthParamName(XL_PICTURE));

        Assert.assertTrue(ImageType.XL_PICTURE.isWidthParam(XL_PICTURE_X_2));
        Assert.assertTrue(!ImageType.XL_PICTURE.isWidthParam(XL_PICTURE_RAW_URL_2));
        Assert.assertEquals(XL_PICTURE_X_2, ImageType.XL_PICTURE.getWidthParamName(XL_PICTURE_2));
    }

    @Test
    public void testHeightMethods() {
        Assert.assertTrue(ImageType.XL_PICTURE.isHeightParam(XL_PICTURE_Y));
        Assert.assertTrue(!ImageType.XL_PICTURE.isHeightParam(XL_PICTURE_URL));
        Assert.assertEquals(XL_PICTURE_Y, ImageType.XL_PICTURE.getHeightParamName(XL_PICTURE));

        Assert.assertTrue(ImageType.XL_PICTURE.isHeightParam(XL_PICTURE_Y_2));
        Assert.assertTrue(!ImageType.XL_PICTURE.isHeightParam(XL_PICTURE_RAW_URL_2));
        Assert.assertEquals(XL_PICTURE_Y_2, ImageType.XL_PICTURE.getHeightParamName(XL_PICTURE_2));
    }

    @Test
    public void testUrlMethods() {
        Assert.assertTrue(ImageType.XL_PICTURE.isUrlParamName(XL_PICTURE_URL));
        Assert.assertTrue(!ImageType.XL_PICTURE.isUrlParamName(XL_PICTURE_Y));
        Assert.assertEquals(XL_PICTURE_URL, ImageType.XL_PICTURE.getUrlParamName(XL_PICTURE));

        Assert.assertTrue(ImageType.XL_PICTURE.isUrlParamName(XL_PICTURE_URL_2));
        Assert.assertTrue(!ImageType.XL_PICTURE.isUrlParamName(XL_PICTURE_RAW_URL_2));
        Assert.assertEquals(XL_PICTURE_URL_2, ImageType.XL_PICTURE.getUrlParamName(XL_PICTURE_2));
    }

    @Test
    public void testRawUrlMethods() {
        Assert.assertTrue(ImageType.XL_PICTURE.isRawUrlParamName(XL_PICTURE_RAW_URL));
        Assert.assertTrue(!ImageType.XL_PICTURE.isRawUrlParamName(XL_PICTURE_Y));
        Assert.assertEquals(XL_PICTURE_RAW_URL, ImageType.XL_PICTURE.getRawUrlParamName(XL_PICTURE));

        Assert.assertTrue(ImageType.XL_PICTURE.isRawUrlParamName(XL_PICTURE_RAW_URL_2));
        Assert.assertTrue(!ImageType.XL_PICTURE.isRawUrlParamName(XL_PICTURE_2));
        Assert.assertEquals(XL_PICTURE_RAW_URL_2, ImageType.XL_PICTURE.getRawUrlParamName(XL_PICTURE_2));
    }

    @Test
    public void testGetIndex() {
        Assert.assertEquals(0, ImageType.XL_PICTURE.getIndex(XL_PICTURE));
        Assert.assertEquals(1, ImageType.XL_PICTURE.getIndex(XL_PICTURE_2));
    }
}
