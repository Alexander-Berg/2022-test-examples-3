package ru.yandex.market.mbo.db.modelstorage.image;


import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.ModelSaveContext;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.ModelPickerPreprocessor;
import ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper;
import ru.yandex.market.mbo.db.params.ParameterProtoConverter;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.client.CategoryParametersServiceClientStub;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersServiceClient;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.SubType;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;
import ru.yandex.market.mbo.gwt.utils.PickerImageUtils;
import ru.yandex.market.mbo.utils.MboAssertions;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Test of {@link ModelPickerPreprocessor}.
 *
 * @author danfertev
 * @since 25.08.2018
 */
@SuppressWarnings("checkstyle:magicNumber")
public class PickerImageCopyServiceTest {
    private static final int INT_NUMBER = 100;
    private static final double DOUBLE_NUMBER = 0.0;
    private static final String NO_PICKER = "noPicker";
    private static final String IMAGE_PICKER_MODEL = "imagePickerModel";
    private static final String IMAGE_PICKER_OFFER_1 = "imagePickerOffer1";
    private static final String IMAGE_PICKER_OFFER_2 = "imagePickerOffer2";
    private static final String IMAGE_PICKER_OFFER_3 = "imagePickerOffer3";

    private List<CategoryParam> parameters;

    private ModelPickerPreprocessor modelPickerPreprocessor;

    private ModelSaveContext context = new ModelSaveContext(0);

    @Before
    public void before() {
        parameters = ParametersBuilder.startParameters()
            .startParameter()
                .id(0L)
                .xslAndName(NO_PICKER)
                .type(Param.Type.ENUM)
                .subType(SubType.RANGE)
                .level(CategoryParam.Level.OFFER)
            .endParameter()
            .startParameter()
                .id(1L)
                .xslAndName(IMAGE_PICKER_MODEL)
                .type(Param.Type.ENUM)
                .subType(SubType.IMAGE_PICKER)
                .level(CategoryParam.Level.MODEL)
            .endParameter()
            .startParameter()
                .id(2L)
                .xslAndName(IMAGE_PICKER_OFFER_1)
                .type(Param.Type.ENUM)
                .subType(SubType.IMAGE_PICKER)
                .level(CategoryParam.Level.OFFER)
                .copyFirstSkuPictureToPicker(true)
            .endParameter()
            .startParameter()
                .id(3L)
                .xslAndName(IMAGE_PICKER_OFFER_2)
                .type(Param.Type.ENUM)
                .subType(SubType.IMAGE_PICKER)
                .level(CategoryParam.Level.OFFER)
                .copyFirstSkuPictureToPicker(true)
            .endParameter()
            .startParameter()
                .id(4L)
                .xslAndName(IMAGE_PICKER_OFFER_3)
                .type(Param.Type.ENUM)
                .subType(SubType.IMAGE_PICKER)
                .level(CategoryParam.Level.OFFER)
                .copyFirstSkuPictureToPicker(false)
            .endParameter()
            .getParameters();

        CategoryParametersServiceClient client = CategoryParametersServiceClientStub.ofCategory(
            SkuBuilderHelper.CATEGORY_ID,
            parameters.stream()
                .map(PickerImageCopyServiceTest::convert)
                .collect(Collectors.toList()));

        modelPickerPreprocessor = new ModelPickerPreprocessor(client);
    }

    @Test
    public void pictureChangedCopyEnabled() {
        CommonModel parent = SkuBuilderHelper.getGuruBuilder()
            .parameters(parameters)
            .id(1L)
            .endModel();
        CommonModel sku = SkuBuilderHelper.getSkuBuilder(1L)
            .parameters(parameters)
            .id(11L)
            .param(IMAGE_PICKER_OFFER_1).setOption(100L)
            .endModel();

        Picture firstPicture = picture("url");
        sku.addPicture(firstPicture);

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(parent, sku);
        modelPickerPreprocessor.preprocess(modelSaveGroup, context);

        CommonModel updatedParent = modelSaveGroup.getById(1L);

        assertPicker(updatedParent, IMAGE_PICKER_OFFER_1, 100L, firstPicture);
    }

    @Test
    public void pictureChangedCopyDisabled() {
        CommonModel parent = SkuBuilderHelper.getGuruBuilder()
            .parameters(parameters)
            .id(1L)
            .endModel();
        CommonModel sku = SkuBuilderHelper.getSkuBuilder(1L)
            .parameters(parameters)
            .id(11L)
            .param(IMAGE_PICKER_OFFER_3).setOption(1L)
            .endModel();

        Picture firstPicture = picture("url");
        sku.addPicture(firstPicture);

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(parent, sku);
        modelPickerPreprocessor.preprocess(modelSaveGroup, context);

        CommonModel updatedParent = modelSaveGroup.getById(1L);

        assertNoPicker(updatedParent, IMAGE_PICKER_OFFER_3);
    }

    @Test
    public void forceCreatePicker() {
        ModelSaveContext modelSaveContext = new ModelSaveContext(0L);
        modelSaveContext.setForcedCreatePicker(true);

        CommonModel parent = SkuBuilderHelper.getGuruBuilder()
                .parameters(parameters)
                .id(1L)
                .endModel();
        CommonModel sku = SkuBuilderHelper.getSkuBuilder(1L)
                .parameters(parameters)
                .id(11L)
                .param(IMAGE_PICKER_OFFER_3).setOption(1L)
                .endModel();

        Picture firstPicture = picture("url");
        sku.addPicture(firstPicture);

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(parent, sku);
        modelPickerPreprocessor.preprocess(modelSaveGroup, modelSaveContext);

        CommonModel updatedParent = modelSaveGroup.getById(1L);

        assertPicker(updatedParent, IMAGE_PICKER_OFFER_3, 1L, firstPicture);
    }

    @Test
    public void pictureChangedMultipleParamsCopied() {
        CommonModel parent = SkuBuilderHelper.getGuruBuilder()
            .parameters(parameters)
            .id(1L)
            .endModel();
        CommonModel sku = SkuBuilderHelper.getSkuBuilder(1L)
            .parameters(parameters)
            .id(11L)
            .param(IMAGE_PICKER_OFFER_1).setOption(100L)
            .param(IMAGE_PICKER_OFFER_2).setOption(200L)
            .endModel();

        Picture firstPicture = picture("url");
        sku.addPicture(firstPicture);

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(parent, sku);
        modelPickerPreprocessor.preprocess(modelSaveGroup, context);

        CommonModel updatedParent = modelSaveGroup.getById(1L);

        assertPicker(updatedParent, IMAGE_PICKER_OFFER_1, 100L, firstPicture);
        assertPicker(updatedParent, IMAGE_PICKER_OFFER_2, 200L, firstPicture);
    }

    @Test
    public void pictureChangedMultipleValuesCopied() {
        CommonModel parent = SkuBuilderHelper.getGuruBuilder()
            .parameters(parameters)
            .id(1L)
            .endModel();
        CommonModel sku = SkuBuilderHelper.getSkuBuilder(1L)
            .parameters(parameters)
            .id(11L)
            .param(IMAGE_PICKER_OFFER_1).setOption(100L)
            .param(IMAGE_PICKER_OFFER_1).setOption(101L)
            .endModel();

        Picture firstPicture = picture("url");
        sku.addPicture(firstPicture);

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(parent, sku);
        modelPickerPreprocessor.preprocess(modelSaveGroup, context);

        CommonModel updatedParent = modelSaveGroup.getById(1L);

        assertPicker(updatedParent, IMAGE_PICKER_OFFER_1, 100L, firstPicture);
        assertPicker(updatedParent, IMAGE_PICKER_OFFER_1, 101L, firstPicture);
    }

    @Test
    public void pictureChangedWrongParameters() {
        CommonModel parent = SkuBuilderHelper.getGuruBuilder()
            .parameters(parameters)
            .id(1L)
            .endModel();
        CommonModel sku = SkuBuilderHelper.getSkuBuilder(1L)
            .parameters(parameters)
            .id(11L)
            .param(NO_PICKER).setOption(100L)
            .param(IMAGE_PICKER_MODEL).setOption(200L)
            .endModel();

        Picture firstPicture = picture("url");
        sku.addPicture(firstPicture);

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(parent, sku);
        modelPickerPreprocessor.preprocess(modelSaveGroup, context);

        CommonModel updatedParent = modelSaveGroup.getById(1L);

        assertNoPicker(updatedParent, NO_PICKER);
        assertNoPicker(updatedParent, IMAGE_PICKER_MODEL);
    }

    @Test
    public void pictureNotChangedCopyEnabled() {
        Picture firstPicture = picture("url");
        CommonModel parent = SkuBuilderHelper.getGuruBuilder()
            .parameters(parameters)
            .id(1L)
            .endModel();
        CommonModel sku = SkuBuilderHelper.getSkuBuilder(1L)
            .parameters(parameters)
            .id(11L)
            .picture(firstPicture)
            .param(IMAGE_PICKER_OFFER_1).setOption(100L)
            .endModel();

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(parent, sku);
        modelPickerPreprocessor.preprocess(modelSaveGroup, context);

        CommonModel updatedParent = modelSaveGroup.getById(1L);

        assertNoPicker(updatedParent, IMAGE_PICKER_OFFER_1);
    }

    @Test
    public void pictureChangedModelDeleted() {
        CommonModel parent = SkuBuilderHelper.getGuruBuilder()
            .parameters(parameters)
            .id(1L)
            .endModel();
        CommonModel sku = SkuBuilderHelper.getSkuBuilder(1L)
            .parameters(parameters)
            .id(11L)
            .param(IMAGE_PICKER_OFFER_1).setOption(100L)
            .endModel();

        Picture firstPicture = picture("url");
        sku.addPicture(firstPicture);
        sku.setDeleted(true);

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(parent, sku);
        modelPickerPreprocessor.preprocess(modelSaveGroup, context);

        CommonModel updatedParent = modelSaveGroup.getById(1L);

        assertNoPicker(updatedParent, IMAGE_PICKER_OFFER_1);
    }

    @Test
    public void pictureChangedPickerUpdated() {
        Picture skuPicture = picture("skuPicture");
        Picture oldPicture = picture("oldPicture");
        Picture newPicture = picture("newPicture");

        CommonModel parentBefore = SkuBuilderHelper.getGuruBuilder()
            .parameters(parameters)
            .id(1L)
            .startParameterValueLink()
                .paramId(2L)
                    .xslName(IMAGE_PICKER_OFFER_1)
                    .optionId(100L)
                    .pickerImage(PickerImageUtils.convert(oldPicture.getUrl()))
                .modificationSource(ModificationSource.OPERATOR_COPIED)
                .pickerImageSource(ModificationSource.OPERATOR_COPIED)
            .endParameterValue()
            .endModel();

        CommonModel skuBefore = SkuBuilderHelper.getSkuBuilder(1L)
            .parameters(parameters)
            .id(11L)
            .param(IMAGE_PICKER_OFFER_1).setOption(100L)
            .endModel();

        CommonModel parentAfter = SkuBuilderHelper.getGuruBuilder()
            .parameters(parameters)
            .id(1L)
            .startParameterValueLink()
                .paramId(2L)
                    .xslName(IMAGE_PICKER_OFFER_1)
                    .optionId(100L)
                    .pickerImage(PickerImageUtils.convert(newPicture.getUrl()))
                .modificationSource(ModificationSource.OPERATOR_COPIED)
                .pickerImageSource(ModificationSource.OPERATOR_COPIED)
            .endParameterValue()
            .endModel();

        CommonModel skuAfter = SkuBuilderHelper.getSkuBuilder(1L)
            .parameters(parameters)
            .id(11L)
            .param(IMAGE_PICKER_OFFER_1).setOption(100L)
            .picture(skuPicture)
            .endModel();

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(parentAfter, skuAfter),
            ImmutableList.of(parentBefore, skuBefore));
        modelPickerPreprocessor.preprocess(modelSaveGroup, context);

        CommonModel updatedParent = modelSaveGroup.getById(1L);

        assertPicker(
            updatedParent,
            IMAGE_PICKER_OFFER_1,
            100L,
            newPicture,
            ModificationSource.OPERATOR_COPIED,
            ModificationSource.OPERATOR_COPIED
        );
    }

    @Test
    public void skuCreatedDoNotOverwriteImagePicker() {
        Picture skuPicture = picture("skuPicture");
        Picture oldPicture = picture("oldPicture");

        CommonModel parentBefore = SkuBuilderHelper.getGuruBuilder()
            .parameters(parameters)
            .id(1L)
            .startParameterValueLink()
            .paramId(2L)
            .xslName(IMAGE_PICKER_OFFER_1)
            .optionId(100L)
            .pickerImage(PickerImageUtils.convert(oldPicture.getUrl()))
            .modificationSource(ModificationSource.OPERATOR_COPIED)
            .pickerImageSource(ModificationSource.OPERATOR_COPIED)
            .endParameterValue()
            .endModel();

        CommonModel skuAfter = SkuBuilderHelper.getSkuBuilder(1L)
            .parameters(parameters)
            .param(IMAGE_PICKER_OFFER_1).setOption(100L)
            .picture(skuPicture)
            .endModel();

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(parentBefore, skuAfter),
            ImmutableList.of(parentBefore));
        modelPickerPreprocessor.preprocess(modelSaveGroup, context);

        CommonModel updatedParent = modelSaveGroup.getById(1L);

        assertPicker(
            updatedParent,
            IMAGE_PICKER_OFFER_1,
            100L,
            oldPicture,
            ModificationSource.OPERATOR_COPIED,
            ModificationSource.OPERATOR_COPIED
        );
    }

    @Test
    public void skuCreatedCopyImagePickerToParent() {
        Picture skuPicture = picture("skuPicture");

        CommonModel parentBefore = SkuBuilderHelper.getGuruBuilder()
            .parameters(parameters)
            .id(1L)
            .endModel();

        CommonModel skuAfter = SkuBuilderHelper.getSkuBuilder(1L)
            .parameters(parameters)
            .param(IMAGE_PICKER_OFFER_1).setOption(100L)
            .picture(skuPicture)
            .endModel();

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(parentBefore, skuAfter),
            ImmutableList.of(parentBefore));
        modelPickerPreprocessor.preprocess(modelSaveGroup, context);

        CommonModel updatedParent = modelSaveGroup.getById(1L);

        assertPicker(
            updatedParent,
            IMAGE_PICKER_OFFER_1,
            100L,
            skuPicture,
            ModificationSource.AUTO,
            ModificationSource.AUTO
        );
    }

    @Test
    public void testGuruAndSkuIsBothNewModelsInModelSaveGroup() {
        // negative ids were introduced in MBO-17756
        CommonModel parent = SkuBuilderHelper.getGuruBuilder()
            .parameters(parameters)
            .id(-1L)
            .withSkuRelations(SkuBuilderHelper.CATEGORY_ID, -11L)
            .endModel();
        CommonModel sku = SkuBuilderHelper.getSkuBuilder(-1L)
            .parameters(parameters)
            .id(-11L)
            .param(IMAGE_PICKER_OFFER_1).setOption(100L)
            .endModel();

        Picture firstPicture = picture("url");
        sku.addPicture(firstPicture);

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(parent, sku),
            ImmutableList.of());
        modelPickerPreprocessor.preprocess(modelSaveGroup, context);

        CommonModel updatedParent = modelSaveGroup.getById(parent.getId());

        assertPicker(updatedParent, IMAGE_PICKER_OFFER_1, 100L, firstPicture);
    }

    private static Picture picture(String url) {
        Picture picture = new Picture();
        picture.setUrl("//host/get-mpic/1/" + url + ".jpeg/orig");
        picture.setHeight(INT_NUMBER);
        picture.setWidth(INT_NUMBER);
        picture.setUrlSource("urlSource");
        picture.setUrlOrig("urlOrig");
        picture.setModificationSource(ModificationSource.OPERATOR_FILLED);
        picture.setColorness(DOUBLE_NUMBER);
        picture.setColornessAvg(DOUBLE_NUMBER);
        return picture;
    }

    private static MboParameters.Parameter convert(CategoryParam param) {
        return MboParameters.Parameter.newBuilder()
            .setId(param.getId())
            .setXslName(param.getXslName())
            .setValueType(MboParameters.ValueType.valueOf(param.getType().name()))
            .setSubType(MboParameters.SubType.valueOf(param.getSubtype().name()))
            .setParamType(ParameterProtoConverter.convert(param.getLevel()))
            .setCopyFirstSkuPictureToPicker(param.isCopyFirstSkuPictureToPicker())
            .build();
    }

    private static void assertPicker(CommonModel model,
                                     String xslName,
                                     long optionId,
                                     Picture picture,
                                     ModificationSource linkSource,
                                     ModificationSource pickerSource
    ) {
        model.getParameterValueLinks().stream()
            .filter(pv -> xslName.equals(pv.getXslName()))
            .filter(pv -> pv.getOptionId() == optionId)
            .forEach(link -> {
                MboAssertions.assertThat(link).hasModificationSource(linkSource);
                Assertions.assertThat(link.getPickerModificationSource()).isEqualTo(pickerSource);
                Assertions.assertThat(link.getPickerImage()).isEqualTo(PickerImageUtils.convert(picture.getUrl()));
            });
    }

    private static void assertPicker(CommonModel model, String xslName, long optionId, Picture picture) {
        assertPicker(model, xslName, optionId, picture, ModificationSource.AUTO, ModificationSource.AUTO);
    }

    private static void assertNoPicker(CommonModel model, String xslName) {
        model.getParameterValueLinks().stream()
            .filter(pv -> xslName.equals(pv.getXslName()))
            .forEach(link -> Assertions.assertThat(link.getPickerImage()).isNull());
    }
}
