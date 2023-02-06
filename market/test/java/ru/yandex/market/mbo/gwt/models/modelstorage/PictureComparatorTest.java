package ru.yandex.market.mbo.gwt.models.modelstorage;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.gwt.models.modelstorage.models.PictureComparator;
import ru.yandex.market.mbo.gwt.models.modelstorage.models.XslNameComparator;

/**
 * Test for PictureComparator and related XslNameComparator.
 *
 * @author dmserebr
 */
public class PictureComparatorTest {
    @Test
    public void testXslNameComparison() {
        XslNameComparator xslNameComparator = XslNameComparator.INSTANCE;
        Assert.assertEquals(-1, xslNameComparator.compare("XL-Picture", "XL-Picture_2"));
        Assert.assertEquals(-1, xslNameComparator.compare("XL-Picture_3", "XL-Picture_7"));
        Assert.assertEquals(-1, xslNameComparator.compare("XL-Picture", "XL-Picture_10"));
        Assert.assertEquals(-1, xslNameComparator.compare("XL-Picture_2", "XL-Picture_10"));
        Assert.assertEquals(1, xslNameComparator.compare("XL-Picture_11", "XL-Picture_3"));
        Assert.assertEquals(1, xslNameComparator.compare("XL-Picture_20180214", "XL-Picture_20180213"));
        Assert.assertEquals(0, xslNameComparator.compare("XL-Picture_3", "XL-Picture_3"));
        Assert.assertEquals(1, xslNameComparator.compare("", "XL-Picture_5"));
        Assert.assertEquals(0, xslNameComparator.compare("", null));
        Assert.assertEquals(-1, xslNameComparator.compare(
            "XL-Picture", "these aren't the droids you're looking for"));
        Assert.assertEquals(1, xslNameComparator.compare("$#@F*^", "XL-Picture"));
    }

    @Test
    public void testPictureComparison() {
        PictureComparator pictureComparator = PictureComparator.INSTANCE;
        Assert.assertEquals(-1, pictureComparator.compare(
            PictureBuilder.newBuilder().setXslName("XL-Picture_1").build(),
            PictureBuilder.newBuilder().setXslName("XL-Picture_2").build()
        ));
        Assert.assertEquals(1, pictureComparator.compare(
            PictureBuilder.newBuilder().setXslName("XL-Picture_10").build(),
            PictureBuilder.newBuilder().setXslName("XL-Picture_2").build()
        ));
        Assert.assertEquals(0, pictureComparator.compare(
            PictureBuilder.newBuilder().setXslName("XL-Picture_5").setUrl("url").build(),
            PictureBuilder.newBuilder().setXslName("XL-Picture_5").setUrlOrig("urlOrig").build()
        ));
        Assert.assertEquals(1, pictureComparator.compare(
            PictureBuilder.newBuilder().build(),
            PictureBuilder.newBuilder().setXslName("XL-Picture").build()
        ));
        Assert.assertEquals(0, pictureComparator.compare(
            PictureBuilder.newBuilder().setUrl("http://url2.com").build(),
            PictureBuilder.newBuilder().setUrl("http://url1.com").build()
        ));
        Assert.assertEquals(-1, pictureComparator.compare(
                PictureBuilder.newBuilder().setXslName("XL-Picture").setUrl("http://url2.com").build(),
                PictureBuilder.newBuilder().setUrl("http://url123.com").build()
        ));
        Assert.assertEquals(-1, pictureComparator.compare(
                PictureBuilder.newBuilder().setXslName("XL-Picture").setUrl("http://url2.com").build(),
                PictureBuilder.newBuilder().setXslName("").setUrl("http://url123.com").build()
        ));

        try {
            pictureComparator.compare(PictureBuilder.newBuilder().setXslName("XL-Picture").build(), null);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("PictureComparator.compare() - picture is null!", ex.getMessage());
        }
        try {
            pictureComparator.compare(null, PictureBuilder.newBuilder().setXslName("XL-Picture_10").build());
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("PictureComparator.compare() - picture is null!", ex.getMessage());
        }

        Assert.assertEquals(-1, pictureComparator.compare(
            PictureBuilder.newBuilder().setXslName("XL-Picture").build(),
            PictureBuilder.newBuilder().setXslName("www.howmanypeopleareinspacerightnow.com").build()
        ));
    }
}
