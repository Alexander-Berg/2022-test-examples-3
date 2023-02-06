package ru.yandex.market.mbo.gwt.models.modelstorage;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.ImageType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class CommonModelPictureTest {

    private static final String XL_PICTURE = "XL-Picture";
    private static final String XL_PICTURE_WIDTH = ImageType.XL_PICTURE.getWidthParamName(XL_PICTURE);
    private static final String XL_PICTURE_HEIGHT = ImageType.XL_PICTURE.getHeightParamName(XL_PICTURE);

    private static final String IMAGE_URL = "IMAGE_URL";
    private static final String IMAGE_WIDTH_STR = "123";
    private static final int IMAGE_WIDTH = 123;
    private static final String IMAGE_HEIGHT_STR = "456";
    private static final int IMAGE_HEIGHT = 456;

    private CommonModel model;

    private Picture picture = new Picture();

    @Before
    public void startUp() {
        model = new CommonModel();

        picture.setXslName(XL_PICTURE);
        picture.setWidth(IMAGE_WIDTH);
        picture.setHeight(IMAGE_HEIGHT);
    }

    @Test
    public void testAddPictureIfPicturesListNotExists() {
        model.replacePicture(picture);
        assertEquals(1, model.getPictures().size());
        assertEquals(XL_PICTURE, model.getPictures().get(0).getXslName());
    }

    @Test
    public void testAddPictureIfPictureNotExists() {
        model.replacePicture(picture);
        assertEquals(1, model.getPictures().size());
        assertEquals(XL_PICTURE, model.getPictures().get(0).getXslName());
    }

    @Test
    public void testAddPictureIfPictureNotExistsAndOtherPictureExists() {
        Picture otherPicture = new Picture();
        otherPicture.setXslName("otherXslName");
        model.addPicture(otherPicture);

        model.replacePicture(picture);
        assertEquals(2, model.getPictures().size());
        assertEquals(XL_PICTURE, model.getPictures().get(1).getXslName());
    }

    @Test
    public void testAddPictureIfPictureExists() {
        Picture otherPicture = new Picture();
        otherPicture.setXslName(XL_PICTURE);
        model.addPicture(otherPicture);

        model.replacePicture(picture);
        assertEquals(1, model.getPictures().size());
        assertSame(picture, model.getPictures().get(0));
    }

    @Test
    public void testRemovePictureWithoutXslName() {
        Picture otherPicture = new Picture();
        otherPicture.setUrl(IMAGE_URL);
        otherPicture.setHeight(IMAGE_HEIGHT);
        otherPicture.setWidth(IMAGE_WIDTH);

        model.addPicture(otherPicture);
        assertEquals(1, model.getPictures().size());

        model.removePicture(new Picture(otherPicture));
        assertEquals(0, model.getPictures().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReplacePictureWithoutXslNameWillFail() {
        Picture otherPicture = new Picture();
        otherPicture.setUrl(IMAGE_URL);
        otherPicture.setHeight(IMAGE_HEIGHT);
        otherPicture.setWidth(IMAGE_WIDTH);

        model.replacePicture(otherPicture);
    }

    @Test
    public void testRemovePicture() {
        model.removePictureByXslName(picture.getXslName());
        assertEquals(0, model.getPictures().size());
    }
}
