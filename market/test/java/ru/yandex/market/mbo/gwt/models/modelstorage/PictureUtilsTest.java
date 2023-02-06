package ru.yandex.market.mbo.gwt.models.modelstorage;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.gwt.models.modelstorage.models.PictureUtils;

/**
 * @author s-ermakov
 */
public class PictureUtilsTest {

    @Test
    public void testDoubleConversion() {
        testDoubleConversion(null);
        testDoubleConversion(PictureBuilder.newBuilder().setUrl("//test.ru").build());
        testDoubleConversion(PictureBuilder.newBuilder().setUrlOrig("//test.ru").build());
        testDoubleConversion(PictureBuilder.newBuilder().setUrlSource("//test.ru").build());
        testDoubleConversion(PictureBuilder.newBuilder().setHeight(1).build());
        testDoubleConversion(PictureBuilder.newBuilder().setWidth(1).build());
        testDoubleConversion(PictureBuilder.newBuilder()
            .setUrl("//test.ru")
            .setUrlOrig("//test1.ru")
            .setUrlSource("//test2.ru")
            .setHeight(2)
            .setWidth(1)
            .build());
    }

    private static void testDoubleConversion(Picture picture) {
        String serializedStr = PictureUtils.marshall(picture);
        Picture deserializedPicture = PictureUtils.unmarshall(serializedStr);
        assertEquals(picture, deserializedPicture);
    }

    private static void assertEquals(Picture expected, Picture actual) {
        if (expected == actual) {
            return;
        }

        Assert.assertEquals(expected.getUrl(), actual.getUrl());
        Assert.assertEquals(expected.getUrlOrig(), actual.getUrlOrig());
        Assert.assertEquals(expected.getUrlSource(), actual.getUrlSource());
        Assert.assertEquals(expected.getWidth(), actual.getWidth());
        Assert.assertEquals(expected.getHeight(), actual.getHeight());
    }
}
