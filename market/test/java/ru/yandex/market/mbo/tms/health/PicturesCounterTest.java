package ru.yandex.market.mbo.tms.health;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.ImageType;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.tms.health.model.pictures.PicturesCounter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author s-ermakov
 */
@SuppressWarnings({"checkstyle:magicnumber", "checkstyle:linelength"})
public class PicturesCounterTest {
    private static Random random = new Random();

    private static final ModelStorage.Model.Builder WITHOUT_PICS = ModelPictureGenerator.generateModel(CommonModel.Source.GURU, 0);
    private static final ModelStorage.Model.Builder WITH_ONE_XL_PIC = ModelPictureGenerator.generateModel(CommonModel.Source.GURU, 1);
    private static final ModelStorage.Model.Builder WITH_SEVERAL_XL_PIC = ModelPictureGenerator.generateModel(CommonModel.Source.GURU, 13);

    @Test
    public void testOnePictureAndMaxPicturesCounter() throws Exception {
        List<ModelStorage.Model> list = Arrays.asList(WITHOUT_PICS.build(), WITH_ONE_XL_PIC.build(), WITH_SEVERAL_XL_PIC.build());

        PicturesCounter counter = new PicturesCounter();
        list.forEach(model -> counter.addModelToProcess(model, Collections.emptyList()));

        Assert.assertEquals(1, counter.getTotalModelsWithOnePicture());
        Assert.assertEquals(13, counter.getMaxPicturesOnModelCount());
        Assert.assertEquals(3, counter.getTotalModelsCount());
    }

    @Test
    public void testPicturesSplitsBySize() {
        int[] sizes = new int[]{
            // width | height
            2, 100,
            200, 299,
            200, 300,
            400, 300,
            499, 300,
            500, 300,
            999, 1,
            555, 555
        };

        ModelStorage.Model.Builder modelBuilder = ModelPictureGenerator.generateModel(CommonModel.Source.GURU,
            sizes.length / 2, sizes);
        PicturesCounter counter = new PicturesCounter();
        counter.addModelToProcess(modelBuilder.build(), Collections.emptyList());

        Assert.assertEquals(2, counter.getTotalXlPicturesInSplit(PicturesCounter.LESS_THAN_300_PIXS_SPLIT));
        Assert.assertEquals(3, counter.getTotalXlPicturesInSplit(PicturesCounter.LESS_THAN_500_PIXS_SPLIT));
        Assert.assertEquals(2, counter.getTotalXlPicturesInSplit(PicturesCounter.LESS_THAN_700_PIXS_SPLIT));
        Assert.assertEquals(1, counter.getTotalXlPicturesInSplit(PicturesCounter.MORE_THAN_700_PIXS_SPLIT));
        Assert.assertEquals(sizes.length / 2, counter.getTotalImagesCount());
    }

    @Test
    public void testPictureParamValueRelations() {
        // setup
        CategoryParam firstParam = CategoryParamBuilder.newBuilder(100, "first_param")
            .setUseForImages(true)
            .addOption(OptionBuilder.newBuilder(10))
            .addOption(OptionBuilder.newBuilder(11))
            .addOption(OptionBuilder.newBuilder(12))
            .build();
        CategoryParam secondParam = CategoryParamBuilder.newBuilder(200, "second_param")
            .setUseForImages(true)
            .addOption(OptionBuilder.newBuilder(20))
            .addOption(OptionBuilder.newBuilder(21))
            .build();
        CategoryParam uselessParam = CategoryParamBuilder.newBuilder(300, "useless_param")
            .addOption(OptionBuilder.newBuilder(30))
            .build();
        List<CategoryParam> categoryParams = Arrays.asList(firstParam, secondParam, uselessParam);

        // xs-picture   -> xsl-name: first_param, option id: 10
        // xs-picture_2 -> xsl-name: first_param, option id: 11
        ModelStorage.Model.Builder modelBuilder1 = ModelPictureGenerator.generateModel(CommonModel.Source.GURU,
            2, builder -> {
                ImageType imageType = ImageType.getImageType(builder.getXslName());
                int index = imageType.getIndex(builder.getXslName());
                if (index == 0) {
                    ModelStorage.ParameterValue parameterValue = ModelStorage.ParameterValue.newBuilder()
                        .setParamId(100)
                        .setXslName("first_param")
                        .setOptionId(10)
                        .build();
                    builder.addParameterValues(parameterValue);
                }
                if (index == 1) {
                    ModelStorage.ParameterValue parameterValue = ModelStorage.ParameterValue.newBuilder()
                        .setParamId(100)
                        .setXslName("first_param")
                        .setOptionId(11)
                        .build();
                    builder.addParameterValues(parameterValue);
                }
                return builder;
            });

        // xs-picture   -> xsl-name: first_param, option id: 10
        //              -> xsl-name: second_param, option id: 20
        //              -> xsl-name: invalid_param, option id: 666
        // xs-picture_2 -> xsl-name: second_param, option id: 20
        // xs-picture_3 ->
        ModelStorage.Model.Builder modelBuilder2 = ModelPictureGenerator.generateModel(CommonModel.Source.GURU,
            3, builder -> {
                ImageType imageType = ImageType.getImageType(builder.getXslName());
                int index = imageType.getIndex(builder.getXslName());
                if (index == 0) {
                    ModelStorage.ParameterValue parameterValue1 = ModelStorage.ParameterValue.newBuilder()
                        .setParamId(100)
                        .setXslName("first_param")
                        .setOptionId(10)
                        .build();
                    ModelStorage.ParameterValue parameterValue2 = ModelStorage.ParameterValue.newBuilder()
                        .setParamId(200)
                        .setXslName("second_param")
                        .setOptionId(20)
                        .build();
                    ModelStorage.ParameterValue parameterValue3 = ModelStorage.ParameterValue.newBuilder()
                        .setParamId(666)
                        .setXslName("invalid_param")
                        .setOptionId(666)
                        .build();
                    builder.addParameterValues(parameterValue1);
                    builder.addParameterValues(parameterValue2);
                    builder.addParameterValues(parameterValue3);
                }
                if (index == 1) {
                    ModelStorage.ParameterValue parameterValue = ModelStorage.ParameterValue.newBuilder()
                        .setParamId(200)
                        .setXslName("second_param")
                        .setOptionId(20)
                        .build();
                    builder.addParameterValues(parameterValue);
                }
                return builder;
            });

        // run
        PicturesCounter counter = new PicturesCounter();
        // первая модель имеет только один параметр: first_param
        counter.addModelToProcess(modelBuilder1.build(), categoryParams);
        counter.addModelToProcess(modelBuilder2.build(), categoryParams);

        // assert
        Assert.assertEquals(1, counter.getTotalImagesWithoutRelations());
        Assert.assertEquals(4, counter.getParamValueCountWithRelationedPicture());
        Assert.assertEquals(10, counter.getTotalParamValueCount());
    }

    @Test
    public void testCopyMethod() throws Exception {
        PicturesCounter picturesCounter = generatePicturesCounter();
        PicturesCounter copyPicturesCounter = new PicturesCounter(picturesCounter);

        assertPicturesCounterEquals(picturesCounter, copyPicturesCounter);
    }

    private static void assertPicturesCounterEquals(PicturesCounter expected, PicturesCounter actual) {
        Assert.assertEquals(expected.getTotalImagesCount(), actual.getTotalImagesCount());
        Assert.assertEquals(expected.getTotalModelsCount(), actual.getTotalModelsCount());
        Assert.assertEquals(expected.getTotalModelsWithOnePicture(), actual.getTotalModelsWithOnePicture());
        Assert.assertEquals(expected.getTotalModelsWithoutPictures(), actual.getTotalModelsWithoutPictures());
        Assert.assertEquals(expected.getMaxPicturesOnModelCount(), actual.getMaxPicturesOnModelCount());
        Assert.assertEquals(expected.getTotalImagesWithoutRelations(), actual.getTotalImagesWithoutRelations());
        Assert.assertEquals(expected.getParamValueCountWithRelationedPicture(), actual.getParamValueCountWithRelationedPicture());
        Assert.assertEquals(expected.getTotalParamValueCount(), actual.getTotalParamValueCount());

        Assert.assertEquals(expected.getTotalXlPicturesInSplit(PicturesCounter.LESS_THAN_300_PIXS_SPLIT), actual.getTotalXlPicturesInSplit(PicturesCounter.LESS_THAN_300_PIXS_SPLIT));
        Assert.assertEquals(expected.getTotalXlPicturesInSplit(PicturesCounter.LESS_THAN_500_PIXS_SPLIT), actual.getTotalXlPicturesInSplit(PicturesCounter.LESS_THAN_500_PIXS_SPLIT));
        Assert.assertEquals(expected.getTotalXlPicturesInSplit(PicturesCounter.LESS_THAN_700_PIXS_SPLIT), actual.getTotalXlPicturesInSplit(PicturesCounter.LESS_THAN_700_PIXS_SPLIT));
        Assert.assertEquals(expected.getTotalXlPicturesInSplit(PicturesCounter.MORE_THAN_700_PIXS_SPLIT), actual.getTotalXlPicturesInSplit(PicturesCounter.MORE_THAN_700_PIXS_SPLIT));
    }

    private static PicturesCounter generatePicturesCounter() {
        CategoryParam param = CategoryParamBuilder.newBuilder(100, "param")
            .addOption(OptionBuilder.newBuilder(0))
            .addOption(OptionBuilder.newBuilder(1))
            .addOption(OptionBuilder.newBuilder(2))
            .build();

        ModelStorage.Model.Builder modelBuilder = ModelPictureGenerator.generateModel(CommonModel.Source.GURU,
            random.nextInt(10), builder -> {
                while (random.nextBoolean()) {
                    ModelStorage.ParameterValue parameterValue = ModelStorage.ParameterValue.newBuilder()
                        .setParamId(100)
                        .setXslName("param")
                        .setOptionId(random.nextInt(3))
                        .build();
                    builder.addParameterValues(parameterValue);
                }
                return builder;
            });

        PicturesCounter counter = new PicturesCounter();
        counter.addModelToProcess(modelBuilder.build(), Collections.singleton(param));
        return counter;
    }
}
