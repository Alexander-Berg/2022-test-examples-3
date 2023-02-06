package ru.yandex.market.mbo.gwt.models.image;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.ImageType;

import static org.junit.Assert.assertEquals;

/**
 * @author s-ermakov
 */
public class ImageTypeIndexTest {

    @Test
    public void testXlPictureXslName() throws Exception {
        String xslName = "XL-Picture";
        int index = ImageType.getImageType(xslName).getIndex(xslName);
        assertEquals(0, index);
    }

    @Test
    public void testXlPicture2XslName() throws Exception {
        String xslName = "XL-Picture_2";
        int index = ImageType.getImageType(xslName).getIndex(xslName);
        assertEquals(1, index);
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testXlPicture42XslName() throws Exception {
        String xslName = "XL-Picture_42";
        int index = ImageType.getImageType(xslName).getIndex(xslName);
        assertEquals(41, index);
    }

    @Test
    public void testInvalidXslName() throws Exception {
        String xslName = "Invalid_xsl_name";
        int index = ImageType.getImageType(xslName).getIndex(xslName);
        assertEquals(0, index);
    }
}
