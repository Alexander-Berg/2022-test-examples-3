package ru.yandex.market.mbo.db.modelstorage.image;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.export.client.CategoryParametersServiceClientStub;
import ru.yandex.market.mbo.gwt.models.ImageType;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.utils.MboAssertions;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 14.02.2018
 */
public class ModelImageSyncServiceTest {

    private static final int WIDTH = 100;
    private static final int HEIGHT = 450;
    private static final double COLORNESS_AVG = 0.4;
    private static final double COLORNESS = 0.5;
    private static final int PICTURE_PARAMETERS_COUNT = 3;
    private static final long CATEGORY_ID = 666912;

    private final long userId = 28027378;
    private final ModificationSource modificationSource = ModificationSource.AUTO;

    private ModelImageSyncService imageSyncService;

    private List<CategoryParam> parameters;

    @Before
    public void before() {
        parameters = createPictureParameterView(PICTURE_PARAMETERS_COUNT);
        imageSyncService = new ModelImageSyncService(
            CategoryParametersServiceClientStub.ofCategoryParams(CATEGORY_ID, parameters));
    }

    @Test
    public void syncXLPictureAndFields() {
        Picture picture = createPicture("http://xl-picture");

        CommonModel model = CommonModelBuilder.newBuilder()
            .category(CATEGORY_ID)
            .picture(picture)
            .getModel();

        imageSyncService.syncPicturesToParameters(model, userId, modificationSource);

        Assertions.assertThat(model.getPictures()).hasSize(1);
        MboAssertions.assertThat(model, "XL-Picture").values("http://xl-picture");
        MboAssertions.assertThat(model, "XLPictureUrl").values("http://xl-picture/source");
        MboAssertions.assertThat(model, "XLPictureSizeX").values(WIDTH);
        MboAssertions.assertThat(model, "XLPictureSizeY").values(HEIGHT);
    }

    @Test
    public void xlPictureSizesHasNumericType() {
        CommonModel model = CommonModelBuilder.newBuilder()
            .category(CATEGORY_ID)
            .picture(createPicture("http://xl-picture"))
            .picture(createPicture("http://xl-picture2"))
            .getModel();

        imageSyncService.syncPicturesToParameters(model, userId, modificationSource);

        MboAssertions.assertThat(model, "XLPictureSizeX").type(Param.Type.NUMERIC);
        MboAssertions.assertThat(model, "XLPictureSizeY").type(Param.Type.NUMERIC);
        MboAssertions.assertThat(model, "XLPictureSizeX_2").type(Param.Type.NUMERIC);
        MboAssertions.assertThat(model, "XLPictureSizeY_2").type(Param.Type.NUMERIC);
    }

    @Test
    public void syncXLPicture() {
        Picture picture = createPicture("http://xl-picture");
        picture.setXslName(XslNames.XL_PICTURE);

        CommonModel model = commonModelBuilder()
            .picture(picture)
            .getModel();

        imageSyncService.syncPicturesToParameters(model, userId, modificationSource);

        Assertions.assertThat(model.getPictures()).hasSize(1);
        MboAssertions.assertThat(model, XslNames.XL_PICTURE).values("http://xl-picture");
    }

    @Test(expected = NoMorePictureParametersException.class)
    public void noSpaceLeft() {
        Picture picture = createPicture("http://xl-picture");
        Picture picture2 = createPicture("http://xl-picture_2");
        Picture picture3 = createPicture("http://xl-picture_3");
        Picture picture4 = createPicture("http://xl-picture_4");

        CommonModel model = commonModelBuilder()
            .param(XslNames.XL_PICTURE + "_3")
            .setString("http://xl-picture_2/exists")
            .picture(picture)
            .picture(picture2)
            .picture(picture3)
            .picture(picture4)
            .getModel();

        imageSyncService.syncPicturesToParameters(model, userId, modificationSource);
    }

    @Test
    public void testDuplicateSourceImagesNoCheck() {
        Picture xlPicture = createPicture("http://xl-picture");
        Picture xlPicture2 = createPicture("http://xl-picture2")
            .setUrlSource(xlPicture.getUrlSource());
        List<Picture> pictures = Arrays.asList(xlPicture, xlPicture2);
        CommonModel model = commonModelBuilder()
            .getModel();

        imageSyncService.syncPicturesToParameters(model, pictures, userId, modificationSource, false);

        MboAssertions.assertThat(model, "XL-Picture").values("http://xl-picture");
        MboAssertions.assertThat(model, "XLPictureUrl").values("http://xl-picture/source");
        MboAssertions.assertThat(model, "XLPictureSizeX").values(WIDTH);
        MboAssertions.assertThat(model, "XLPictureSizeY").values(HEIGHT);

        MboAssertions.assertThat(model, "XL-Picture_2").values("http://xl-picture2");
        MboAssertions.assertThat(model, "XLPictureUrl_2").values("http://xl-picture/source");
        MboAssertions.assertThat(model, "XLPictureSizeX_2").values(WIDTH);
        MboAssertions.assertThat(model, "XLPictureSizeY_2").values(HEIGHT);
    }

    @Test
    public void testDuplicateSourceImagesCheck() {
        Picture xlPicture = createPicture("http://xl-picture");
        Picture xlPicture2 = createPicture("http://xl-picture2")
            .setUrlSource(xlPicture.getUrlSource());
        List<Picture> pictures = Arrays.asList(xlPicture, xlPicture2);
        CommonModel model1 = commonModelBuilder()
            .getModel();

        assertThatThrownBy(() -> imageSyncService
            .syncPicturesToParameters(model1, pictures, userId, modificationSource, true))
            .isInstanceOf(DuplicateImageUrlException.class);
    }


    @Test
    public void replaceXLPicture() {
        Picture picture = createPicture("http://new-xl-picture");
        picture.setXslName(XslNames.XL_PICTURE);

        CommonModel model = commonModelBuilder()
            .param(XslNames.XL_PICTURE)
            .setString("http://xl-picture")
            .picture(picture)
            .getModel();

        imageSyncService.syncPicturesToParameters(model, userId, modificationSource);

        Assertions.assertThat(model.getPictures()).hasSize(1);
        MboAssertions.assertThat(model, XslNames.XL_PICTURE).values("http://new-xl-picture");
    }

    @Test
    public void testUrlSourceFromParameterValuesWontBeLost() {
        Picture picture = createPicture("http://xl-picture");

        CommonModel model = commonModelBuilder()
            .pictureParam(XslNames.XL_PICTURE, null, null, null, "http://my-url", null)
            .picture(picture)
            .getModel();

        imageSyncService.syncPicturesToParameters(model, userId, modificationSource);

        Assertions.assertThat(model.getPictures()).hasSize(1);
        Assertions.assertThat(model.getPicture(XslNames.XL_PICTURE).getUrlSource())
            .isEqualTo("http://my-url");
        MboAssertions.assertThat(model, ImageType.getUrlSourceXslName(XslNames.XL_PICTURE))
            .values("http://my-url");
    }

    @Test
    public void concurrentModificationCheck() {
        Picture xlPicture = createPicture("http://xl-picture");
        xlPicture.setXslName(XslNames.XL_PICTURE);
        CommonModel model = commonModelBuilder()
            .picture(xlPicture)
            .getModel();

        imageSyncService.syncPicturesToParameters(model, userId, modificationSource);
    }

    @Test
    public void syncPicturesAlreadyInModel() {
        Picture xlPicture = createPicture("http://xl-picture");
        Picture xlPicture2 = createPicture("http://xl-picture2");

        CommonModel model = commonModelBuilder()
            .picture(xlPicture)
            .picture(xlPicture2)
            .getModel();

        imageSyncService.syncPicturesToParameters(model, userId, modificationSource);

        MboAssertions.assertThat(model, XslNames.XL_PICTURE).values("http://xl-picture");
        MboAssertions.assertThat(model, XslNames.XL_PICTURE_COLORNESS).values(COLORNESS);
        MboAssertions.assertThat(model, XslNames.XL_PICTURE_COLORNESS_AVG).values(COLORNESS_AVG);
        MboAssertions.assertThat(model, XslNames.XL_PICTURE + "_" + 2).values("http://xl-picture2");
    }

    @Test
    public void testRemoveImageParamValues() {
        Picture xlPicture = createPicture("http://xl-picture");
        CommonModel model1 = commonModelBuilder()
            .picture(xlPicture)
            .getModel();
        CommonModel model2 = commonModelBuilder()
            .picture(xlPicture)
            .getModel();

        imageSyncService.syncPicturesToParameters(model1, userId, modificationSource);
        imageSyncService.syncPicturesToParameters(model2, userId, modificationSource);
        MboAssertions.assertThat(model1, XslNames.XL_PICTURE).exists();
        MboAssertions.assertThat(model1, XslNames.XL_PICTURE_URL).exists();
        MboAssertions.assertThat(model1, XslNames.XL_PICTURE_COLORNESS).exists();
        MboAssertions.assertThat(model2, XslNames.XL_PICTURE).exists();
        MboAssertions.assertThat(model2, XslNames.XL_PICTURE_URL).exists();
        MboAssertions.assertThat(model2, XslNames.XL_PICTURE_COLORNESS).exists();

        // first case - regular model
        imageSyncService.removeAllImageParamValues(model1);
        MboAssertions.assertThat(model1, XslNames.XL_PICTURE).notExists();
        MboAssertions.assertThat(model1, XslNames.XL_PICTURE_URL).notExists();
        MboAssertions.assertThat(model1, XslNames.XL_PICTURE_COLORNESS).notExists();

        // second case - model with empty pictures
        model2.clearPictures();
        imageSyncService.removeAllImageParamValues(model2);
        MboAssertions.assertThat(model2, XslNames.XL_PICTURE).notExists();
        MboAssertions.assertThat(model2, XslNames.XL_PICTURE_URL).notExists();
        MboAssertions.assertThat(model2, XslNames.XL_PICTURE_COLORNESS).notExists();
    }

    private CommonModelBuilder commonModelBuilder() {
        return CommonModelBuilder.newBuilder()
            .category(CATEGORY_ID)
            .parameters(parameters);
    }

    private Picture createPicture(String url) {
        Picture picture = new Picture();
        picture.setUrl(url);
        picture.setWidth(WIDTH);
        picture.setHeight(HEIGHT);
        picture.setUrlSource(url + "/source");
        picture.setColornessAvg(COLORNESS_AVG);
        picture.setColorness(COLORNESS);
        return picture;
    }

    private static List<CategoryParam> createPictureParameterView(int parametersCount) {
        ParametersBuilder<List<CategoryParam>> builder = ParametersBuilder.startParameters(Function.identity());

        List<String> pictureXslNames = Stream.concat(
            Stream.of(XslNames.XL_PICTURE),
            IntStream.iterate(2, i -> i + 1).mapToObj(n -> XslNames.XL_PICTURE + "_" + n)
        )
            .limit(parametersCount)
            .collect(Collectors.toList());

        long i = 0;

        for (String pictureXslName : pictureXslNames) {
            for (String xslName : ImageType.getAllImageParamNames(pictureXslName)) {

                Param.Type valueType;
                if (xslName.equals(ImageType.getWidthXslName(pictureXslName)) ||
                    xslName.equals(ImageType.getHeightXslName(pictureXslName)) ||
                    xslName.equals(ImageType.getColornessXslName(pictureXslName)) ||
                    xslName.equals(ImageType.getColornessAvgXslName(pictureXslName))) {
                    valueType = Param.Type.NUMERIC;
                } else {
                    valueType = Param.Type.STRING;
                }

                builder.startParameter()
                    .id(++i)
                    .xsl(xslName)
                    .type(valueType)
                    .endParameter();
            }
        }

        return builder.endParameters();
    }
}
