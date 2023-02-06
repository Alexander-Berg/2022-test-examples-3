package ru.yandex.market.mbo.db.modelstorage.validation;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.ModelSaveContext;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError.ErrorSubtype.IMAGE_MISSING_MANDATORY_PARAMS;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class PicturesModelValidatorTest extends BaseValidatorTestClass {

    private static final ModelSaveContext SAVE_CONTEXT = new ModelSaveContext(1);
    private static final String XL_PICTURE = "XL-Picture";
    private static final String XL_PICTURE_2 = "XL-Picture_2";
    private static final String LONG_STRING = StringUtils.repeat('a', 4001);
    private static final String NAMESPACE = "mpic-fake";
    private static final String AVATARS_HOST1 = "avatars.mdst-fake.yandex.net";
    private static final String AVATARS_HOST2 = "avatars.mdst-fake2.yandex.net";

    private PicturesModelValidator picturesModelValidator;

    @Before
    @Override
    public void before() {
        super.before();
        picturesModelValidator = new PicturesModelValidator(singletonList(AVATARS_HOST1), NAMESPACE);
    }

    @Test
    public void testEmptyModelWillPass() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1).getModel();
        List<ModelValidationError> errors = validate(model);
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCorrectModelWillPass() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .pictureParam(XL_PICTURE, makeUrl1("url1"), 300, 300, "source1", null)
            .pictureParam(XL_PICTURE_2, makeUrl1("url2"), 300, 300, null, "orig2")
            .picture(XL_PICTURE, makeUrl1("url1"), 300, 300, "source1", null)
            .picture(XL_PICTURE_2, makeUrl1("url2"), 300, 300, null, "orig2")
            .picture(makeUrl1("another/picture/url"))
            .getModel();
        List<ModelValidationError> errors = validate(model);
        assertThat(errors).isEmpty();
    }

    @Test
    public void testModelOnlyWithPictureWillPass() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .picture(makeUrl1("another/picture/url"))
            .picture(makeUrl1("another/picture/url2"))
            .picture(makeUrl1("another/picture/url3"))
            .getModel();
        List<ModelValidationError> errors = validate(model);
        assertThat(errors).isEmpty();
    }

    @Test
    public void testModelWithMissingPictureWillFail() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .pictureParam(XL_PICTURE, makeUrl1("url"), 300, 300, "source", null)
            .getModel();
        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            missingPictureError(XL_PICTURE, true)
        );
    }

    @Test
    public void testModelWithMissingPictureNotModified() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .pictureParam(XL_PICTURE, makeUrl1("url"), 300, 300, "source", null)
            .getModel();
        storage.saveModel(model, SAVE_CONTEXT);

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            missingPictureError(XL_PICTURE, false)
        );
    }

    @Test
    public void testModelWithMissingPictureModified() {
        CommonModel beforeModel = CommonModelBuilder.newBuilder(1, 1, 1)
            .pictureParam(XL_PICTURE, makeUrl1("url"), 300, 300, "source", null)
            .getModel();
        storage.saveModel(beforeModel, SAVE_CONTEXT);

        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .pictureParam(XL_PICTURE, makeUrl1("url"), 301, 300, "source", null)
            .getModel();

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            missingPictureError(XL_PICTURE, true)
        );
    }

    @Test
    public void testModelWithEmptyUrlWillFail() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .pictureParam(XL_PICTURE, "", 300, 300, null, null)
            .pictureParam(XL_PICTURE_2, null, 300, 300, null, null)
            .getModel();
        // для XL_PICTURE_2 специально помещаем в модель пустое значение
        model.putParameterValues(new ParameterValues(XL_PICTURE_2.hashCode(), XL_PICTURE_2, Param.Type.STRING));

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            missingPictureError(XL_PICTURE, true),
            missingPictureError(XL_PICTURE_2, true),
            mandatoryParamError(XL_PICTURE, true),
            mandatoryParamError(XL_PICTURE_2, true)
        );
    }

    @Test
    public void testModelWithEmptyUrlNotModified() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .pictureParam(XL_PICTURE, "", 300, 300, null, null)
            .pictureParam(XL_PICTURE_2, null, 300, 300, null, null)
            .getModel();
        // для XL_PICTURE_2 специально помещаем в модель пустое значение
        model.putParameterValues(new ParameterValues(XL_PICTURE_2.hashCode(), XL_PICTURE_2, Param.Type.STRING));
        storage.saveModel(model, SAVE_CONTEXT);

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            missingPictureError(XL_PICTURE, false),
            missingPictureError(XL_PICTURE_2, false),
            mandatoryParamError(XL_PICTURE, false),
            mandatoryParamError(XL_PICTURE_2, false)
        );
    }

    @Test
    public void testModelWithEmptyHeightWillFail() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .pictureParam(XL_PICTURE, makeUrl1("url1"), null, 300, null, null)
            .getModel();

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            missingPictureError(XL_PICTURE, true),
            mandatoryParamError("XLPictureSizeY", true)
        );
    }

    @Test
    public void testModelWithEmptyHeightNotModified() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .pictureParam(XL_PICTURE, makeUrl1("url1"), null, 300, null, null)
            .getModel();
        storage.saveModel(model, SAVE_CONTEXT);

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            missingPictureError(XL_PICTURE, false),
            mandatoryParamError("XLPictureSizeY", false)
        );
    }

    @Test
    public void testModelWithEmptyHeightPictureModified() {
        CommonModel modelBefore = CommonModelBuilder.newBuilder(1, 1, 1)
            .pictureParam(XL_PICTURE, makeUrl1("url1"), null, 300, null, null)
            .getModel();
        storage.saveModel(modelBefore, SAVE_CONTEXT);

        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .pictureParam(XL_PICTURE, makeUrl1("url1"), null, 300, null, null)
            .picture(XL_PICTURE, makeUrl1("url1"), null, 300, null, null)
            .getModel();

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            mandatoryParamError("XLPictureSizeY", true),
            mandatoryFieldError(XL_PICTURE, "height", true)
        );
    }

    @Test
    public void testModelWithEmptyWidthWillFail() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .pictureParam(XL_PICTURE, makeUrl1("url1"), 300, null, null, null)
            .getModel();

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            missingPictureError(XL_PICTURE, true),
            mandatoryParamError("XLPictureSizeX", true)
        );
    }

    @Test
    public void testModelWithEmptyWidthNotModified() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .pictureParam(XL_PICTURE, makeUrl1("url1"), 300, null, null, null)
            .getModel();
        storage.saveModel(model, SAVE_CONTEXT);

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            missingPictureError(XL_PICTURE, false),
            mandatoryParamError("XLPictureSizeX", false)
        );
    }

    @Test
    public void testModelWithEmptyPictureUrlWillFail() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .picture(null, 300, 300, null, null)
            .picture("", 300, 300, null, null)
            .getModel();

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            mandatoryFieldError("0", "url", true),
            mandatoryFieldError("1", "url", true)
        );
    }

    @Test
    public void testModelWithEmptyPictureUrlNotModified() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .picture(null, 300, 300, null, null)
            .picture("", 300, 300, null, null)
            .getModel();
        storage.saveModel(model, SAVE_CONTEXT);

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            mandatoryFieldError("0", "url", false),
            mandatoryFieldError("1", "url", false)
        );
    }

    @Test
    public void testModelWithEmptyPictureHeightWillFail() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .picture(makeUrl1("url"), null, 300, null, null)
            .getModel();

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            mandatoryFieldError("0", "height", true)
        );
    }

    @Test
    public void testModelWithEmptyPictureHeightNotModified() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .picture(makeUrl1("url"), null, 300, null, null)
            .getModel();
        storage.saveModel(model, SAVE_CONTEXT);

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            mandatoryFieldError("0", "height", false)
        );
    }

    @Test
    public void testModelWithEmptyPictureWidthWillFail() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .picture(makeUrl1("url"), 300, null, null, null)
            .getModel();

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            mandatoryFieldError("0", "width", true)
        );
    }

    @Test
    public void testModelWithEmptyPictureWidthNotModified() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .picture(makeUrl1("url"), 300, null, null, null)
            .getModel();
        storage.saveModel(model, SAVE_CONTEXT);

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            mandatoryFieldError("0", "width", false)
        );
    }

    @Test
    public void testModelWithIncorrectPictureXslNameWillFail() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .picture("qwerty", makeUrl1("url"), 300, 300, null, null)
            // с пустым xslName ничего падать не должно
            .picture(makeUrl1("url"), 300, 300, null, null)
            .getModel();

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            invalidParamForModelTypeError("qwerty", "xslName", true)
        );
    }

    @Test
    public void testModelWithIncorrectPictureXslNameNotModified() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .picture("qwerty", makeUrl1("url"), 300, 300, null, null)
            // с пустым xslName ничего падать не должно
            .picture(makeUrl1("url"), 300, 300, null, null)
            .getModel();

        storage.saveModel(model, SAVE_CONTEXT);
        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            invalidParamForModelTypeError("qwerty", "xslName", false)
        );
    }

    @Test
    public void testImageInconsistencyBetweenParamsAndPicture() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            // inconsistency pictures
            .pictureParam(XL_PICTURE, makeUrl1("url1"), 300, 300, "source1", null)
            .picture(XL_PICTURE, makeUrl1("url1"), 300, 300, null, "orig2")

            // inconsistency pictures
            .pictureParam(XL_PICTURE_2, makeUrl1("url2"), 300, 300, "source1", null)
            .picture(XL_PICTURE_2, makeUrl1("url2"), 300, 300, null, "orig2")

            .picture(makeUrl1("another/picture/url"))
            .getModel();
        List<ModelValidationError> errors = validate(model);
        assertThat(errors).containsExactlyInAnyOrder(
            imageInconsistencyError(XL_PICTURE, "urlSource", true, "source1", null),
            imageInconsistencyError(XL_PICTURE, "urlOrig", true, null, "orig2"),
            imageInconsistencyError(XL_PICTURE_2, "urlSource", true, "source1", null),
            imageInconsistencyError(XL_PICTURE_2, "urlOrig", true, null, "orig2")
        );
    }

    @Test
    public void testImageInconsistencyBetweenParamsAndPictureNotModified() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            // inconsistency pictures
            .pictureParam(XL_PICTURE, makeUrl1("url1"), 300, 300, "source1", null)
            .picture(XL_PICTURE, makeUrl1("url1"), 300, 300, null, "orig2")

            // inconsistency pictures
            .pictureParam(XL_PICTURE_2, makeUrl1("url2"), 300, 300, "source1", null)
            .picture(XL_PICTURE_2, makeUrl1("url2"), 300, 300, null, "orig2")

            .picture(makeUrl1("another/picture/url"))
            .getModel();
        storage.saveModel(model, SAVE_CONTEXT);

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            imageInconsistencyError(XL_PICTURE, "urlSource", false, "source1", null),
            imageInconsistencyError(XL_PICTURE, "urlOrig", false, null, "orig2"),
            imageInconsistencyError(XL_PICTURE_2, "urlSource", false, "source1", null),
            imageInconsistencyError(XL_PICTURE_2, "urlOrig", false, null, "orig2")
        );
    }

    @Test
    public void testLongUrls() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            // correct pictures
            .pictureParam(XL_PICTURE, makeUrl1(LONG_STRING), 300, 300, makeUrl1(LONG_STRING), makeUrl1(LONG_STRING))
            .picture(XL_PICTURE, makeUrl1(LONG_STRING), 300, 300, makeUrl1(LONG_STRING), makeUrl1(LONG_STRING))
            .getModel();

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            imageUrlTooLongError(XL_PICTURE, "url", true)
                .addParam(ModelStorage.ErrorParamName.MAX_LENGTH, 4000),
            imageUrlTooLongError(XL_PICTURE, "urlSource", true)
                .addParam(ModelStorage.ErrorParamName.MAX_LENGTH, 4000),
            imageUrlTooLongError(XL_PICTURE, "urlOrig", true)
                .addParam(ModelStorage.ErrorParamName.MAX_LENGTH, 4000)
        );
    }

    @Test
    public void testLongUrlsNotModified() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            // correct pictures
            .pictureParam(XL_PICTURE, makeUrl1(LONG_STRING), 300, 300, makeUrl1(LONG_STRING), makeUrl1(LONG_STRING))
            .picture(XL_PICTURE, makeUrl1(LONG_STRING), 300, 300, makeUrl1(LONG_STRING), makeUrl1(LONG_STRING))
            .getModel();
        storage.saveModel(model, SAVE_CONTEXT);

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            imageUrlTooLongError(XL_PICTURE, "url", false)
                .addParam(ModelStorage.ErrorParamName.MAX_LENGTH, 4000),
            imageUrlTooLongError(XL_PICTURE, "urlSource", false)
                .addParam(ModelStorage.ErrorParamName.MAX_LENGTH, 4000),
            imageUrlTooLongError(XL_PICTURE, "urlOrig", false)
                .addParam(ModelStorage.ErrorParamName.MAX_LENGTH, 4000)
        );
    }

    @Test
    public void testMdataParamsWontAffectValidator() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .parameterValues(1010101, "XL-Picture_4_mdata", makeUrl1("url1"))
            .getModel();

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).isEmpty();
    }

    @Test
    public void testModificationWithEmptyParamsWontFailIfParentModelContainsThem() {
        CommonModel baseModel = CommonModelBuilder.newBuilder(1, 1, 1)
            .pictureParam(XL_PICTURE, makeUrl1("url"), 300, 300, "source", null)
            .picture(XL_PICTURE, makeUrl1("url"), 300, 300, "source", null)
            .getModel();
        CommonModel modification = CommonModelBuilder.newBuilder(1, 1, 1)
            .parentModel(baseModel)
            .getModel();

        List<ModelValidationError> errors = validate(modification);

        assertThat(errors).isEmpty();
    }

    @Test
    public void testModificationWithOverriderParamsWillSucceed() {
        CommonModel baseModel = CommonModelBuilder.newBuilder(1, 1, 1)
            .pictureParam(XL_PICTURE, makeUrl1("url"), 300, 300, "source", null)
            .picture(XL_PICTURE, makeUrl1("url"), 300, 300, "source", null)
            .getModel();
        CommonModel modification = CommonModelBuilder.newBuilder(1, 1, 1)
            .parentModel(baseModel)
            .pictureParam(XL_PICTURE, makeUrl1("url2"), null, 600, null, null)
            .picture(XL_PICTURE, makeUrl1("url2"), 300, 600, "source", null)
            .getModel();

        List<ModelValidationError> errors = validate(modification);

        assertThat(errors).isEmpty();
    }

    @Test
    public void testSkuWithXslNamedPicturesFail() {
        CommonModel sku = CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.SKU)
            .picture(null, makeUrl1("url"), 300, 400, "url_source", null)
            .getModel();

        List<ModelValidationError> errors = validate(sku);
        assertThat(errors).isEmpty();

        sku.getPictures().get(0).setXslName(XL_PICTURE);
        errors = validate(sku);
        assertThat(errors).containsExactlyInAnyOrder(
            PicturesModelValidator.createXslNameInSkuError(sku.getId(), 1, true)
        );

        Picture allEmptyPicture = new Picture(sku.getPictures().get(0));
        allEmptyPicture.setUrl(null);
        allEmptyPicture.setUrlSource(null);
        allEmptyPicture.setXslName(XL_PICTURE);
        sku.getPictures().add(allEmptyPicture);
        errors = validate(sku);
        assertThat(errors).contains(
            PicturesModelValidator.createXslNameInSkuError(sku.getId(), 1, true),
            PicturesModelValidator.createXslNameInSkuError(sku.getId(), 2, true)
        );
    }

    @Test
    public void testXLPictureSize() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .pictureParam(XL_PICTURE, makeUrl1("url2"), 300, 701, "source", null)
            .picture(XL_PICTURE, makeUrl1("url2"), 300, 701, "source", null)
            .getModel();

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).isEmpty();
    }

    @Test
    public void testXLPictureLowSize() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .pictureParam(XL_PICTURE, makeUrl1("url2"), 100, 279, "source", null)
            .picture(XL_PICTURE, makeUrl1("url2"), 100, 279, "source", null)
            .getModel();

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            invalidResolutionError(XL_PICTURE, true)
        );
    }


    @Test
    public void testXLPictureLowSizeSku() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.SKU)
            .picture(null, makeUrl1("url2"), 100, 279, "source", null)
            .getModel();

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            invalidResolutionError("0", true)
        );
    }

    @Test
    public void testXLPictureNotValidatedOnGenerated() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.GENERATED)
            .pictureParam(XL_PICTURE, makeUrl1("url2"), 100, 299, "source", null)
            .picture(XL_PICTURE, makeUrl1("url2"), 100, 299, "source", null)
            .getModel();

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).isEmpty();
    }

    @Test
    public void testXLPicture2HighSize() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .pictureParam(XL_PICTURE_2, makeUrl1("url2"), 4002, 400, "source", null)
            .picture(XL_PICTURE_2, makeUrl1("url2"), 4002, 400, "source", null)
            .getModel();

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            invalidResolutionError(XL_PICTURE_2, true)
        );
    }

    @Test
    public void testWrongUrl() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.GURU)
            .pictureParam(XL_PICTURE_2, "url2", 700, 400, "source", null)
            .picture(XL_PICTURE_2, "url2", 700, 400, "source", null)
            .getModel();

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            invalidImageUrl(XL_PICTURE_2, "url", true)
        );
    }

    @Test
    public void testSeveralSupportedUrls() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.GURU)
            .pictureParam(XL_PICTURE, makeUrl1("url1"), 700, 400, "source", null)
            .picture(XL_PICTURE, makeUrl1("url1"), 700, 400, "source", null)
            .pictureParam(XL_PICTURE_2, makeUrl2("url2"), 700, 400, "source", null)
            .picture(XL_PICTURE_2, makeUrl2("url2"), 700, 400, "source", null)
            .getModel();

        List<ModelValidationError> errors = validate(model);
        assertThat(errors).containsExactlyInAnyOrder(
            invalidImageUrl(XL_PICTURE_2, "url", true)
        );

        picturesModelValidator = new PicturesModelValidator(asList(AVATARS_HOST1, AVATARS_HOST2), NAMESPACE);
        errors = validate(model);
        assertThat(errors).isEmpty();
    }

    @Test
    public void testColornessComparedWithEpsilon() {
        CommonModel goodModel = CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.GURU)
            .pictureParam(XL_PICTURE, makeUrl1("url1"), 700, 400, "source", null,
                1.00000000000001, null)
            .picture(XL_PICTURE, makeUrl1("url1"), 700, 400, "source", null, ModificationSource.OPERATOR_FILLED,
                1.00000000000002, null)
            .getModel();
        List<ModelValidationError> errors = validate(goodModel);
        assertThat(errors).isEmpty();

        CommonModel badModel = CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.GURU)
            .pictureParam(XL_PICTURE, makeUrl1("url1"), 700, 400, "source", null,
                1.00001, null)
            .picture(XL_PICTURE, makeUrl1("url1"), 700, 400, "source", null, ModificationSource.OPERATOR_FILLED,
                1.00002, null)
            .getModel();
        errors = validate(badModel);
        assertThat(errors).containsExactly(
            imageInconsistencyError(XL_PICTURE, "colorness", true, 1.00001, 1.00002));
    }

    @Test
    public void testModelWithWrongPictureUnmodifiedWillPass() {
        CommonModel beforeModel = CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.GURU)
            .pictureParam(XL_PICTURE, makeUrl1("url1"), 279, 29, "source", null)
            .picture(XL_PICTURE, makeUrl1("url1"), 279, 29, "source", null)
            .pictureParam(XL_PICTURE_2, makeUrl1("url2"), 280, 30, "source", null)
            .picture(XL_PICTURE_2, makeUrl1("url2"), 280, 30, "source", null)
            .getModel();
        storage.saveModel(beforeModel, SAVE_CONTEXT);

        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.GURU)
            .pictureParam(XL_PICTURE, makeUrl1("url1"), 279, 29, "source", null)
            .picture(XL_PICTURE, makeUrl1("url1"), 279, 29, "source", null)
            .pictureParam(XL_PICTURE_2, makeUrl1("url2"), 280, 30, "source", null)
            .picture(XL_PICTURE_2, makeUrl1("url2"), 280, 30, "source", null)
            .getModel();

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            invalidResolutionError(XL_PICTURE, false)
        );
    }

    @Test
    public void testModelWithWrongPictureChangeOrderWillPass() {
        CommonModel beforeModel = CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.GURU)
            .pictureParam(XL_PICTURE, makeUrl1("url1"), 279, 29, "source", null)
            .picture(XL_PICTURE, makeUrl1("url1"), 279, 29, "source", null)
            .pictureParam(XL_PICTURE_2, makeUrl1("url2"), 280, 30, "source", null)
            .picture(XL_PICTURE_2, makeUrl1("url2"), 280, 30, "source", null)
            .getModel();
        storage.saveModel(beforeModel, SAVE_CONTEXT);

        CommonModel model = CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.GURU)
            .pictureParam(XL_PICTURE, makeUrl1("url2"), 280, 30, "source", null)
            .picture(XL_PICTURE, makeUrl1("url2"), 280, 30, "source", null)
            .pictureParam(XL_PICTURE_2, makeUrl1("url1"), 279, 29, "source", null)
            .picture(XL_PICTURE_2, makeUrl1("url1"), 279, 29, "source", null)
            .getModel();

        List<ModelValidationError> errors = validate(model);

        assertThat(errors).containsExactlyInAnyOrder(
            invalidResolutionError(XL_PICTURE_2, false)
        );
    }

    private static String makeUrl1(String name) {
        return "//" + AVATARS_HOST1 + "/get-" + NAMESPACE + "/0/" + name + "/orig";
    }

    private static String makeUrl2(String name) {
        return "//" + AVATARS_HOST2 + "/get-" + NAMESPACE + "/0/" + name + "/orig";
    }

    private List<ModelValidationError> validate(CommonModel model) {
        return picturesModelValidator.validate(context, modelChanges(model), Collections.singleton(model));
    }

    private static ModelValidationError mandatoryParamError(String paramName, boolean critical) {
        return error(IMAGE_MISSING_MANDATORY_PARAMS, null, paramName, critical)
            .addLocalizedMessagePattern("Не заполнен обязательный параметр изображения '%{PARAM_XSL_NAME}'.");
    }

    private static ModelValidationError missingPictureError(String imageId, boolean critical) {
        return error(ModelValidationError.ErrorSubtype.MISSING_PICTURE, imageId, null, critical)
            .addLocalizedMessagePattern("Изображение %{IMAGE_ID} отсутствует в списке изображений.");
    }

    private static ModelValidationError invalidResolutionError(String imageId, boolean critical) {
        return error(ModelValidationError.ErrorSubtype.INVALID_IMAGE_RESOLUTION, imageId, null, critical)
            .addLocalizedMessagePattern("Изображение %{IMAGE_ID} имеет некорректный размер.");
    }

    private static ModelValidationError invalidImageUrl(String imageId, String paramName, boolean critical) {
        String pattern = "[//\\Qavatars.mdst-fake.yandex.net\\E/get-\\Qmpic-fake\\E/.*/.*/orig]";
        return PicturesModelValidator.createInvalidImageUrlError(1, imageId, paramName, pattern, critical);
    }

    private static ModelValidationError imageInconsistencyError(String imageId, String paramName, boolean critical,
                                                                Object paramValue, Object pictureValue) {
        ModelValidationError modelValidationError =
            error(ModelValidationError.ErrorSubtype.IMAGE_INCONSISTENCY, imageId, paramName, critical)
                .addLocalizedMessagePattern(
                    "Значение поля %{PARAM_XSL_NAME} картинки %{IMAGE_ID} не соответствует параметру. " +
                        "В параметре %{PARAM_VALUE}. В картинке: %{PICTURE_VALUE}");
        modelValidationError.addParam(ModelStorage.ErrorParamName.PARAM_VALUE,
            paramValue == null ? null : paramValue.toString());
        modelValidationError.addParam(ModelStorage.ErrorParamName.PICTURE_VALUE,
            pictureValue == null ? null : pictureValue.toString());

        return modelValidationError;
    }

    private static ModelValidationError error(ModelValidationError.ErrorSubtype subtype, String imageId,
                                              String paramName, boolean critical) {
        ModelValidationError error = new ModelValidationError(1L,
            ModelValidationError.ErrorType.INVALID_IMAGE, subtype, critical, false);
        if (imageId != null) {
            error.addParam(ModelStorage.ErrorParamName.IMAGE_ID, imageId);
        }
        if (paramName != null) {
            error.addParam(ModelStorage.ErrorParamName.PARAM_XSL_NAME, paramName);
        }
        return error;
    }

    private static ModelValidationError mandatoryFieldError(String imageId, String paramName, boolean critical) {
        return error(ModelValidationError.ErrorSubtype.IMAGE_MISSING_MANDATORY_FIELDS, imageId, paramName, critical)
            .addLocalizedMessagePattern(
                "Не заполнено обязательное поле '%{PARAM_XSL_NAME}' изображения %{IMAGE_ID}.");
    }


    private static ModelValidationError imageUrlTooLongError(String imageId, String paramName, boolean critical) {
        return error(ModelValidationError.ErrorSubtype.IMAGE_URL_TOO_LONG, imageId, paramName, critical)
            .addLocalizedMessagePattern(
                "Поле '%{PARAM_XSL_NAME}' изображения %{IMAGE_ID} слишком длинное. Максимальная длина %{MAX_LENGTH}");
    }

    private static ModelValidationError invalidParamForModelTypeError(
        String imageId, String paramName, boolean critical) {
        return error(ModelValidationError.ErrorSubtype.INVALID_PARAM_FOR_MODEL_TYPE, imageId, paramName, critical)
            .addLocalizedMessagePattern("%{PARAM_XSL_NAME} изображения %{IMAGE_ID} является некорректным.");
    }

    public static ListAssert<ModelValidationError> assertThat(List<ModelValidationError> errorList) {
        return Assertions.assertThat(wrap(errorList));
    }

    /**
     * Небольшая обертка, чтобы можно было сравнивать actual и expected ошибки между собой.
     */
    private static List<ModelValidationError> wrap(List<ModelValidationError> errorList) {
        return errorList.stream()
            // удлаяляем описание, так как его тяжело сфабриковать
            .peek(error -> error.removeParam(ModelStorage.ErrorParamName.DESCRIPTION))
            .collect(Collectors.toList());
    }
}
