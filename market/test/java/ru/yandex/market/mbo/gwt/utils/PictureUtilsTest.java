package ru.yandex.market.mbo.gwt.utils;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.ImageType;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Arrays;
import java.util.Collections;

/**
 * Tests of {@link PictureUtils}.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class PictureUtilsTest {

    private CommonModel model;

    @Before
    public void setUp() throws Exception {
        model = CommonModelBuilder.newBuilder(1, 1, 1)
            .pictureParam("XL-Picture", "url2", 2, 2, "src2", null)
            .picture("XL-Picture", "url2", 2, 2, null, null)
            .picture("other_url", 1, 1, null, null)
            .getModel();

        Assertions.assertThat(model.getParameterValues()).hasSize(5);
        Assertions.assertThat(model.getPictures()).hasSize(2);
    }

    @Test
    public void testRemoveImageParamsAndPictures() {
        Picture xslPicture = model.getPicture("XL-Picture");
        Picture pictureWithoutXslName = model.getPictures().stream()
            .filter(p -> p.getXslName() == null).findFirst().get();
        PictureUtils.removeImageParamsAndPictures(model, Arrays.asList(xslPicture, pictureWithoutXslName));

        Assertions.assertThat(model.getParameterValues()).hasSize(1);
        Assertions.assertThat(model.getParameterValues().stream().map(ParameterValues::getXslName))
            .allMatch(xslName -> xslName.equals(XslNames.VENDOR));

        Assertions.assertThat(model.getPictures()).isEmpty();
    }

    @Test
    public void testRemoveImageParamsAndPicturesByXslNames() {
        PictureUtils.removeImageParamsAndPicturesByXslNames(model, Collections.singletonList("XL-Picture"));

        Assertions.assertThat(model.getParameterValues()).hasSize(1);
        Assertions.assertThat(model.getParameterValues().stream().map(ParameterValues::getXslName))
            .allMatch(xslName -> xslName.equals(XslNames.VENDOR));

        Assertions.assertThat(model.getPictures()).hasSize(1);
        Assertions.assertThat(model.getPictures().stream().map(Picture::getUrl))
            .containsExactlyInAnyOrder("other_url");
    }

    @Test
    public void testRemoveAllImageParamsAndPictures() {
        PictureUtils.removeAllImageParamsAndPictures(model);

        Assertions.assertThat(model.getParameterValues()).hasSize(1);
        Assertions.assertThat(model.getParameterValues().stream().map(ParameterValues::getXslName))
            .containsExactlyInAnyOrder(XslNames.VENDOR);
        Assertions.assertThat(model.getPictures()).isEmpty();
    }

    @Test
    public void testGetXLPictureNameByIndex() {
        Assertions.assertThat(PictureUtils.getXLPictureNameByIndex(
            ImageType.XL_PICTURE.getIndex(XslNames.XL_PICTURE))).isEqualTo(XslNames.XL_PICTURE);

        Assertions.assertThat(PictureUtils.getXLPictureNameByIndex(
            ImageType.XL_PICTURE.getIndex("XL-Picture_2"))).isEqualTo("XL-Picture_2");

        Assertions.assertThat(PictureUtils.getXLPictureNameByIndex(
            ImageType.XL_PICTURE.getIndex("XL-Picture_27"))).isEqualTo("XL-Picture_27");
    }
}
