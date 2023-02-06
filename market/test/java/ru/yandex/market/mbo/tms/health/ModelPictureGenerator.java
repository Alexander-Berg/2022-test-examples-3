package ru.yandex.market.mbo.tms.health;

import org.apache.commons.lang3.tuple.Pair;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageTestUtil;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.ImageType;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class ModelPictureGenerator {

    private static final Random RANDOM = new Random();
    private static final int PICTURE_SIZE_RANDOM_VALUE = 2000;
    private static final int USE_RANDOM_FLAG = -1;

    private ModelPictureGenerator() {
    }

    public static ModelStorage.Model.Builder generateModel(CommonModel.Source source,
                                                           int picturesCount) {
        return generateModel(source, picturesCount, new int[0]);
    }

    /**
     * Создает модель с заданными параметрами.
     *
     * @param source          тип модели
     * @param picturesCount   количество изображений
     * @param sizes           передавать в формате: width, height, width, height
     */
    public static ModelStorage.Model.Builder generateModel(CommonModel.Source source,
                                                           int picturesCount,
                                                           int... sizes) {
        return generateModel(source, picturesCount, Function.identity(), sizes);
    }

    /**
     * Создает модель с заданными параметрами.
     *
     * @param source          тип модели
     * @param picturesCount   количество изображений
     * @param pictureCreator  функция, для инициализации картинок
     * @param sizes           передавать в формате: width, height, width, height
     */
    @SuppressWarnings("checkstyle:LineLength")
    public static ModelStorage.Model.Builder generateModel(CommonModel.Source source,
                                                           int picturesCount,
                                                           Function<ModelStorage.Picture.Builder, ModelStorage.Picture.Builder> pictureCreator,
                                                           int... sizes) {

        ModelStorage.Model.Builder builder = ModelStorageTestUtil.generateModel().toBuilder()
            .setCurrentType(source.name());

        List<ModelStorage.ParameterValue> paramsWithoutPictures = builder.getParameterValuesList().stream()
            .filter(p -> ImageType.getImageType(p.getXslName()) == ImageType.UNKNOWN)
            .collect(Collectors.toList());

        builder.clearParameterValues()
            .addAllParameterValueLinks(paramsWithoutPictures);

        int[] sizeArray = sizes.length == 0 ? new int[]{USE_RANDOM_FLAG, USE_RANDOM_FLAG} : sizes;
        Iterator<Pair<Integer, Integer>> loopIterator = createSizeLoopIterator(sizeArray);

        for (int i = 0; i < picturesCount; i++) {
            String suffix = i == 0 ? "" : "_" + (i + 1);
            Pair<Integer, Integer> widthHeightPair = loopIterator.next();

            builder.addParameterValues(generatePictureParam("XL-Picture" + suffix))
                .addParameterValues(generatePictureSizeParam("XLPictureSizeX" + suffix, widthHeightPair.getLeft()))
                .addParameterValues(generatePictureSizeParam("XLPictureSizeY" + suffix, widthHeightPair.getRight()));

            ModelStorage.Picture.Builder picture = ModelStorage.Picture.newBuilder()
                .setXslName("XL-Picture" + suffix)
                .setWidth(widthHeightPair.getLeft())
                .setHeight(widthHeightPair.getRight());

            ModelStorage.Picture.Builder processedPictures = pictureCreator.apply(picture);
            builder.addPictures(processedPictures);
        }

        return builder;
    }

    public static ModelStorage.ParameterValue generatePictureParam(String xslName) {
        return ModelStorage.ParameterValue.newBuilder()
            .setXslName(xslName)
            .setValueType(MboParameters.ValueType.STRING)
            .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("http://avatar"))
            .build();
    }

    public static ModelStorage.ParameterValue generatePictureSizeParam(String xslName) {
        return generatePictureSizeParam(xslName, USE_RANDOM_FLAG);
    }

    public static ModelStorage.ParameterValue generatePictureSizeParam(String xslName, int size) {
        int pictureSize = size == -1 ? RANDOM.nextInt(PICTURE_SIZE_RANDOM_VALUE) : size;
        return ModelStorage.ParameterValue.newBuilder()
            .setXslName(xslName)
            .setValueType(MboParameters.ValueType.NUMERIC)
            .setNumericValue(String.valueOf(pictureSize))
            .build();
    }

    /**
     * Возвращает зацикленный итератор, который возвращает пары значений в формате (ширина, высота).
     */
    private static Iterator<Pair<Integer, Integer>> createSizeLoopIterator(int[] sizes) {
        if (sizes.length == 0) {
            throw new IllegalArgumentException("sizes are empty");
        }
        if (sizes.length % 2 != 0) {
            throw new IllegalArgumentException("size array ends with width. Add one more element.");
        }

        return new Iterator<Pair<Integer, Integer>>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Pair<Integer, Integer> next() {
                int width = sizes[index];
                int height = sizes[index + 1];
                index = (index + 2) % sizes.length;
                return Pair.of(width, height);
            }
        };
    }
}
