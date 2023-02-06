package ru.yandex.market.mbo.tms.health;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.tms.health.model.pictures.ModelPicturesCounter;
import ru.yandex.market.mbo.tms.health.model.pictures.PicturesCounter;

import java.util.Collections;

/**
 * @author york@yandex-team.ru
 * @since 30.05.2017
 */
@SuppressWarnings({"checkstyle:magicnumber", "checkstyle:linelength"})
public class ModelsPictureCountersTest {

    @Test
    public void testPictureCounters() throws Exception {
        ModelStorage.Model.Builder withoutPics = ModelPictureGenerator.generateModel(CommonModel.Source.GURU, 0);

        ModelStorage.Model.Builder withBrokenPic = ModelPictureGenerator.generateModel(CommonModel.Source.GENERATED, 0)
            .addParameterValues(ModelPictureGenerator.generatePictureParam("XL-Picture"))
            .addParameterValues(ModelPictureGenerator.generatePictureSizeParam("XL-Picture_X"));

        ModelStorage.Model.Builder withPic = ModelPictureGenerator.generateModel(CommonModel.Source.GURU, 1);

        ModelStorage.Model.Builder generatedWithPics = ModelPictureGenerator.generateModel(CommonModel.Source.GENERATED, 2);

        ModelPicturesCounter counter = new ModelPicturesCounter();
        counter.processModel(withoutPics.build(), true, Collections.emptyList());
        counter.processModel(withBrokenPic.build(), true, Collections.emptyList());
        counter.processModel(withPic.build(), false, Collections.emptyList());
        counter.processModel(withPic.build(), true, Collections.emptyList());
        counter.processModel(generatedWithPics.build(), false, Collections.emptyList());

        PicturesCounter publishedGuruCounter = counter.getPublishedGuruCounter();
        PicturesCounter unpublishedGuruCounter = counter.getUnpublishedGuruCounter();
        PicturesCounter generatedCounter = counter.getGeneratedCounter();

        // assert with picture counter
        Assert.assertEquals(2, generatedCounter.getTotalImagesCount());
        Assert.assertEquals(2, generatedCounter.getTotalModelsCount());

        Assert.assertEquals(1, publishedGuruCounter.getTotalImagesCount());
        Assert.assertEquals(2, publishedGuruCounter.getTotalModelsCount());

        Assert.assertEquals(1, unpublishedGuruCounter.getTotalImagesCount());
        Assert.assertEquals(1, unpublishedGuruCounter.getTotalModelsCount());

        // assert without picture counter
        Assert.assertEquals(1, publishedGuruCounter.getTotalModelsWithoutPictures());
        Assert.assertEquals(0, unpublishedGuruCounter.getTotalModelsWithoutPictures());
        Assert.assertEquals(1, generatedCounter.getTotalModelsWithoutPictures());
    }
}
