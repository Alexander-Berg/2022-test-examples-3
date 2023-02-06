package ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors;

import java.util.Arrays;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.image.ModelImageSyncService;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.utils.XslNames;

public class ModelPicturePreprocessorTest extends BasePreprocessorTest {

    public static final long MODEL_ID_1 = 1L;
    public static final long SKU_ID_1 = 1000L;

    public static final String XL_PICTURE_1 = XslNames.XL_PICTURE + "_1";
    public static final String XL_PICTURE_2 = XslNames.XL_PICTURE + "_2";
    public static final String XL_PICTURE_3 = XslNames.XL_PICTURE + "_3";

    public static final String URL_1 = "http://url.dot.com/pic1.jpg";
    public static final String URL_2 = "http://url.dot.com/pic2.jpg";
    public static final String URL_3 = "http://url.dot.com/pic3.jpg";

    private ModelPicturePreprocessor preprocessor;

    @Before
    public void before() {
        super.before();
        ModelImageSyncService imageSyncService = Mockito.mock(ModelImageSyncService.class);
        preprocessor = new ModelPicturePreprocessor(imageSyncService);
    }

    @Test
    public void testPicturesMovedInsideModel() {
        Picture pictureBefore1 = picture(URL_1, XL_PICTURE_1);
        Picture pictureBefore2 = picture(URL_2, XL_PICTURE_2);
        Picture pictureBefore3 = picture(URL_3, XL_PICTURE_3);

        CommonModel modelBefore = model(MODEL_ID_1);
        modelBefore.setPictures(Arrays.asList(pictureBefore1, pictureBefore2, pictureBefore3));

        // swap picture1 and picture2, leave picture3 untouched
        Picture pictureAfter1 = picture(URL_2, XL_PICTURE_1);
        Picture pictureAfter2 = picture(URL_1, XL_PICTURE_2);
        Picture pictureAfter3 = picture(URL_3, XL_PICTURE_3);

        CommonModel modelAfter = new CommonModel(modelBefore);
        modelAfter.setPictures(Arrays.asList(pictureAfter2, pictureAfter1, pictureAfter3));

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(modelAfter),
            ImmutableList.of(modelBefore)
        );

        preprocessor.preprocess(modelSaveGroup, modelSaveContext);

        CommonModel modelAfterProcessed = modelSaveGroup.getById(modelAfter.getId());
        Assertions.assertThat(modelAfterProcessed.getPictures())
            .extracting(Picture::getModificationSource)
            .containsExactly(
                ModificationSource.OPERATOR_COPIED, // picture2
                ModificationSource.OPERATOR_COPIED, // picture1
                ModificationSource.OPERATOR_FILLED // picture3
            );
    }

    @Test
    public void testPicturesMovedFromModelToSku() {
        Picture modelPictureBefore1 = picture(URL_1, XL_PICTURE_1);
        Picture modelPictureBefore2 = picture(URL_2, XL_PICTURE_2);
        Picture skuPictureBefore1 = picture(URL_3, XL_PICTURE_1);

        CommonModel modelBefore = model(MODEL_ID_1);
        modelBefore.setPictures(Arrays.asList(modelPictureBefore1, modelPictureBefore2));
        CommonModel skuBefore = sku(SKU_ID_1, modelBefore);
        skuBefore.setPictures(Arrays.asList(skuPictureBefore1));

        // move picture2 from model to sku
        Picture modelPictureAfter1 = picture(URL_1, XL_PICTURE_1);
        Picture skuPictureAfter1 = picture(URL_3, XL_PICTURE_1);
        Picture skuPictureAfter2 = picture(URL_2, XL_PICTURE_2);
        CommonModel modelAfter = new CommonModel(modelBefore);
        modelAfter.setPictures(Arrays.asList(modelPictureAfter1));
        CommonModel skuAfter = new CommonModel(skuBefore);
        skuAfter.setPictures(Arrays.asList(skuPictureAfter1, skuPictureAfter2));

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(modelAfter, skuAfter),
            ImmutableList.of(modelBefore, skuBefore)
        );

        preprocessor.preprocess(modelSaveGroup, modelSaveContext);

        CommonModel modelAfterProcessed = modelSaveGroup.getById(modelAfter.getId());
        Assertions.assertThat(modelAfterProcessed.getPictures())
            .extracting(Picture::getModificationSource)
            .containsExactly(
                ModificationSource.OPERATOR_FILLED // picture1
            );

        CommonModel skuAfterProcessed = modelSaveGroup.getById(skuAfter.getId());
        Assertions.assertThat(skuAfterProcessed.getPictures())
            .extracting(Picture::getModificationSource)
            .containsExactly(
                ModificationSource.OPERATOR_FILLED, // picture3
                ModificationSource.OPERATOR_COPIED // picture2
            );
    }

    private Picture picture(String url, String xslName) {
        Picture picture = new Picture();
        picture.setUrl(url);
        picture.setXslName(xslName);
        picture.setModificationSource(ModificationSource.OPERATOR_FILLED);
        return picture;
    }
}
