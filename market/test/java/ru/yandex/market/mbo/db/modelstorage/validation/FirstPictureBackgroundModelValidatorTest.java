package ru.yandex.market.mbo.db.modelstorage.validation;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.modelstorage.StatsModelStorageServiceStub;
import ru.yandex.market.mbo.db.modelstorage.image.ModelImageSyncService;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.client.CategoryParametersServiceClientStub;
import ru.yandex.market.mbo.gwt.models.ImageType;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(MockitoJUnitRunner.class)
public class FirstPictureBackgroundModelValidatorTest extends BaseValidatorTestClass {

    private static long paramIdSeq = 1L;

    private FirstPictureBackgroundModelValidator validator;
    private static final long GURU_ID = 1L;
    private static final long MODIFICATION_ID = 2L;
    private static final long SKU_ID = 3L;
    private static final long PSKU_ID = 4L;
    private static final long HID = 1050L;
    private static final long SEED = 6667L;

    private CommonModel guru;
    private CommonModel guruCopy;
    private CommonModel modification;
    private CommonModel modificationCopy;
    private CommonModel sku;
    private CommonModel psku;
    private CommonModel skuCopy;
    private CommonModel pskuCopy;
    private StatsModelStorageServiceStub storageService;
    private ModelImageSyncService imageSyncService;

    private Random random = new Random(SEED);

    @Before
    public void setup() {
        Collection<MboParameters.Parameter> imageParams = generateImageParams();
        imageSyncService = new ModelImageSyncService(CategoryParametersServiceClientStub
            .ofCategory(HID, imageParams));
        validator = new FirstPictureBackgroundModelValidator();
        storageService = new StatsModelStorageServiceStub();
        context.setStatsModelStorageService(storageService);
        guru = CommonModelBuilder.newBuilder()
            .id(GURU_ID)
            .category(HID)
            .currentType(CommonModel.Source.GURU)
            .modelRelation(SKU_ID, HID, ModelRelation.RelationType.SKU_MODEL)
            .getModel();
        modification = CommonModelBuilder.newBuilder()
            .id(MODIFICATION_ID)
            .parentModelId(GURU_ID)
            .category(HID)
            .currentType(CommonModel.Source.GURU)
            .getModel();
        sku = CommonModelBuilder.newBuilder()
            .id(SKU_ID)
            .category(HID)
            .currentType(CommonModel.Source.SKU)
            .modelRelation(GURU_ID, HID, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();
        psku = CommonModelBuilder.newBuilder()
            .id(PSKU_ID)
            .category(HID)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.PARTNER_SKU)
            .modelRelation(GURU_ID, HID, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();
        guruCopy = new CommonModel(guru);
        modificationCopy = new CommonModel(modification);
        skuCopy = new CommonModel(sku);
        pskuCopy = new CommonModel(psku);
    }

    @Test
    public void testNoPicturesOk() {
        List<ModelValidationError> errors = validator.validate(context, changesOf(guru), groupOf(guru));
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testGuruPasses() {
        setPictures(guru, Collections.singletonList(generateNicePicture("XL-Picture", true)));
        List<ModelValidationError> errors = validator.validate(context, changesOf(guru), groupOf(guru, sku));
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testSkipFirstPictureValidationWorks() {
        setPictures(sku, Arrays.asList(generateNicePicture(null, false),
            generateNicePicture(null, true)));
        setPictures(guru, Arrays.asList(generateNicePicture(null, false),
            generateNicePicture(null, true)));
        context.setSkipFirstPictureValidation(true);
        List<ModelValidationError> errors = validator.validate(context, changesOf(guru), groupOf(guru, sku));
        assertThat(errors).isEmpty();
    }

    @Test
    public void testGuruFails() {
        setPictures(guru, Arrays.asList(
            generateNicePicture("XL-Picture_2", true),
            generateNicePicture("XL-Picture", false)));

        List<ModelValidationError> errors = validator.validate(context, changesOf(guru), groupOf(guru, sku));
        assertThat(errors).containsExactly(error(guru.getId(), guru));
    }

    @Test
    public void testGuruNullBackgroundSkipped() {
        setPictures(guru, Arrays.asList(
            generateNicePicture("XL-Picture", null)));
        List<ModelValidationError> errors = validator.validate(context, changesOf(guru), groupOf(guru, sku));
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testSkuNullBackgroundPrev() {
        setPictures(sku, Arrays.asList(
            generateNicePicture(null, false)));
        setPictures(skuCopy, Arrays.asList(
            generateNicePicture(null, null)));

        List<ModelValidationError> errors = validate(Arrays.asList(guru, skuCopy), sku);

        assertThat(errors).containsExactly(error(sku.getId(), sku, false));
    }

    @Test
    public void testSkuNullPrevDiffUrl() {
        setPictures(sku, Arrays.asList(
            generateNicePicture(null, false)));
        Picture pic = generateNicePicture(null, null);
        pic.setUrl("diff url");
        setPictures(skuCopy, Arrays.asList(pic));

        List<ModelValidationError> errors = validate(Arrays.asList(guru, skuCopy), sku);
        assertThat(errors).containsExactly(error(sku.getId(), sku, true));
    }

    @Test
    public void testGuruFailsNonCritical() {
        setPictures(guru, Arrays.asList(
            generateNicePicture("XL-Picture_2", true),
            generateNicePicture("XL-Picture", false)));

        setPictures(guruCopy, Arrays.asList(
            generateNicePicture("XL-Picture_4", true),
            generateNicePicture("XL-Picture", false)));

        List<ModelValidationError> errors = validate(Arrays.asList(guruCopy, sku), guru);

        assertThat(errors).containsExactly(error(guru.getId(), guru, false));
    }

    @Test
    public void testGuruCritical() {
        setPictures(guru, Arrays.asList(
            generateNicePicture("XL-Picture_2", true),
            generateNicePicture("XL-Picture", false)));

        Picture prev = generateNicePicture("XL-Picture", false);
        prev.setUrl("other url");
        setPictures(guruCopy, Arrays.asList(prev));

        List<ModelValidationError> errors = validate(Arrays.asList(guruCopy, sku), guru);

        assertThat(errors).containsExactly(error(guru.getId(), guru, true));
    }

    @Test
    public void testSkuFails() {
        setPictures(sku, Arrays.asList(generateNicePicture(null, false), generateNicePicture(null, true)));

        List<ModelValidationError> errors = validator.validate(context, changesOf(sku), groupOf(guru, sku));

        assertThat(errors).containsExactlyInAnyOrder(error(sku.getId(), sku));
    }

    @Test
    public void testPSKUSkipped() {
        setPictures(psku, Arrays.asList(generateNicePicture(null, false), generateNicePicture(null, true)));
        List<ModelValidationError> errors = validator.validate(context, changesOf(psku), groupOf(guru, psku));
        assertThat(errors).isEmpty();
    }

    @Test
    public void testPSKUPrevSkipped() {
        setPictures(psku, Arrays.asList(generateNicePicture(null, false)));
        setPictures(pskuCopy, Arrays.asList(generateNicePicture(null, null)));
        List<ModelValidationError> errors = validate(Arrays.asList(guru, pskuCopy), psku);
        assertThat(errors).isEmpty();
    }

    private void setPictures(CommonModel model, List<Picture> pictures) {
        model.setPictures(pictures);
        imageSyncService.syncPicturesToParameters(model, 0L, ModificationSource.OPERATOR_FILLED);
    }

    private ModelChanges changesOf(CommonModel model) {
        return new ModelChanges(null, model);
    }

    private List<ModelValidationError> validate(Collection<CommonModel> prevModels, CommonModel changedModel) {
        storageService.initializeWithModels(prevModels);

        List<CommonModel> savingModels = new ArrayList<>(prevModels);
        CommonModel prevModel = prevModels.stream().filter(m -> m.getId() == changedModel.getId())
            .findFirst().orElse(null);
        savingModels.remove(prevModel);
        savingModels.add(changedModel);

        ModelChanges changes = new ModelChanges(prevModel, changedModel);

        context.getModelSaveGroup().addStorageModels(savingModels);
        context.getModelSaveGroup().addBeforeModels(prevModels);

        return validator.validate(context, changes, savingModels);
    }

    private List<CommonModel> groupOf(CommonModel... models) {
        return Arrays.asList(models);
    }

    private Picture generateNicePicture(String xslName, Boolean whiteBack) {
        Picture pic = new Picture();
        pic.setXslName(xslName);
        pic.setColorness(random.nextDouble());
        pic.setColornessAvg(random.nextDouble());
        pic.setHeight(random.nextInt(600));
        pic.setWidth(random.nextInt(600));
        pic.setLastModificationDate(new Date());
        pic.setLastModificationUid(random.nextLong());
        pic.setModificationSource(ModificationSource.AUTO);
        pic.setOrigMd5(DigestUtils.md5Hex(String.valueOf(random.nextLong())));
        pic.setIsWhiteBackground(whiteBack);
        pic.setUploaded(random.nextBoolean());
        pic.setUrl("http://goatse.cx" + Objects.hash(xslName));
        pic.setUrlOrig("http://goatse.cx");
        pic.setUrlSource("http://goatse.cx");
        return pic;
    }

    private Collection<MboParameters.Parameter> generateImageParams() {
        List<MboParameters.Parameter> result = new ArrayList<>();
        result.addAll(imageTypeParams("XL-Picture"));
        result.addAll(imageTypeParams("XL-Picture_2"));
        result.addAll(imageTypeParams("XL-Picture_4"));
        return result;
    }

    private List<MboParameters.Parameter> imageTypeParams(String xslName) {
        ImageType type = ImageType.XL_PICTURE;
        return Arrays.asList(
            param(xslName),
            param(type.getWidthParamName(xslName)),
            param(type.getHeightParamName(xslName)),
            param(type.getUrlParamName(xslName)),
            param(type.getRawUrlParamName(xslName)),
            param(type.getColornessParamName(xslName)),
            param(type.getColornessAvgParamName(xslName))
        );
    }

    private MboParameters.Parameter param(String xslName) {
        return MboParameters.Parameter.newBuilder().setXslName(xslName).setId(paramIdSeq++).build();
    }


    private ModelValidationError error(long changedModelId, CommonModel model) {
        return error(changedModelId, model, true);
    }

    private ModelValidationError error(long changedModelId, CommonModel model, boolean isCritical) {
        return FirstPictureBackgroundModelValidator.createError(changedModelId, model, isCritical);
    }

    private ModelValidationError warn(CommonModel model) {
        return ImageExistenceValidator.createError(model, false, context);
    }
}
