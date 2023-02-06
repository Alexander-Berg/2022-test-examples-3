package ru.yandex.market.mbo.db.modelstorage.validation;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.image.HttpImageDownloader;
import ru.yandex.market.mbo.db.modelstorage.image.ImageData;
import ru.yandex.market.mbo.db.modelstorage.validation.context.CachingModelValidationContext;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.modelstorage.validation.ImageUniquenessValidator.createError;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getParentWithRelation;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getSkuBuilder;

/**
 * @author commince
 * @date 05.06.2018
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ImageUniquenessValidatorTest {
    private static final Date NEW = new Date();
    private static final Date OLD = new Date(0);

    private static final String PICTURE1_URL = "http://ya.ru/1.jpg";
    private static final String PICTURE2_URL = "http://ya.ru/2.jpg";
    private static final String PICTURE2_URL1 = "http://ya.ru/2-1.jpg";
    private static final String PICTURE2_URL2 = "http://ya.ru/2-2.jpg";
    private static final String PICTURE2_URL3 = "http://ya.ru/2-3.jpg";
    private static final String PICTURE3_URL = "http://ya.ru/3.jpg";

    private ImageUniquenessValidator validator;
    private ModelValidationContext context;

    private CommonModel parentModel;
    private CommonModel sku1;
    private CommonModel sku2;

    private CommonModel parentModelWithModif;
    private CommonModel modification;
    private CommonModel sku3;

    @Before
    public void setup() throws IOException {

        context = mock(CachingModelValidationContext.class);
        HttpImageDownloader httpImageDownloader = mock(HttpImageDownloader.class);

        parentModel = getParentWithRelation(111L, 112L);
        parentModel.setId(1L);

        sku1 = getSkuBuilder(1L)
            .id(111L)
            .endModel();
        sku2 = getSkuBuilder(1L)
            .id(112L)
            .endModel();

        parentModelWithModif = SkuBuilderHelper.getGuru();
        parentModelWithModif.setId(2L);

        modification = getParentWithRelation(10L);
        modification.setId(21L);
        modification.setParentModelId(2L);
        modification.setParentModel(parentModelWithModif);

        sku3 = getSkuBuilder(21L)
            .id(113L)
            .endModel();

        validator = new ImageUniquenessValidator(httpImageDownloader);

        when(context.getRelatedModelsForModels(anyLong(), anyList(), anyCollection(),
            anyCollection(), anyBoolean())).thenCallRealMethod();

        when(context.getRelatedModelsForModels(anyLong(), anyList(), anyCollection(),
            eq(Collections.singletonList(parentModel)), anyBoolean()))
            .thenReturn(new RelatedModelsContainer(Collections.emptyList(), Arrays.asList(sku1, sku2),
                ModelRelation.RelationType.SKU_MODEL));

        when(context.getModels(anyLong(), anyList())).thenReturn(Collections.emptyList());
        when(httpImageDownloader.downloadImage(PICTURE1_URL))
            .thenReturn(new ImageData(new byte[]{1}, "jpg"));
        when(httpImageDownloader.downloadImage(PICTURE2_URL))
            .thenReturn(new ImageData(new byte[]{2}, "jpg"));
        when(httpImageDownloader.downloadImage(PICTURE2_URL1))
            .thenReturn(new ImageData(new byte[]{2}, "jpg"));
        when(httpImageDownloader.downloadImage(PICTURE2_URL2))
            .thenReturn(new ImageData(new byte[]{2}, "jpg"));
        when(httpImageDownloader.downloadImage(PICTURE2_URL3))
            .thenReturn(new ImageData(new byte[]{2}, "jpg"));
        when(httpImageDownloader.downloadImage(PICTURE3_URL))
            .thenReturn(new ImageData(new byte[]{3}, "jpg"));
    }

    @Test
    public void testValidationSkuChangeOk() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1, sku2);
        parentModel.addPicture(makePicture1("XLPicture1", OLD));
        sku1.addPicture(makePicture2("XLPicture2", OLD));
        saveGroup.addBeforeModels(getBeforeList(parentModel, sku1));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        sku2.addPicture(makePicture3HashSameAs2(NEW));

        List<ModelValidationError> errors = validate(saveGroup, parentModel);

        assertThat(errors).isEmpty();
    }

    @Test
    public void testValidationSkuChangeFail() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1, sku2);
        parentModel.addPicture(makePicture2("XLPicture1", OLD));
        saveGroup.addBeforeModels(getBeforeList(parentModel, sku1));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        sku1.addPicture(makePicture2Url1(NEW));

        List<ModelValidationError> errors = validate(saveGroup, parentModel);

        assertThat(errors).containsExactlyInAnyOrder(
            createError(sku1, "0", parentModel.getId(), "XLPicture1", false));
    }

    @Test
    public void testValidationNewSkuChangeFail() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1, sku2);
        parentModel.addPicture(makePicture2("XLPicture1", OLD));
        saveGroup.addBeforeModels(getBeforeList(parentModel));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        sku1.setId(CommonModel.NO_ID);
        sku1.addPicture(makePicture2Url1(NEW));

        List<ModelValidationError> errors = validate(saveGroup, parentModel);

        assertThat(errors).containsExactlyInAnyOrder(
            createError(sku1, "0", parentModel.getId(), "XLPicture1", false));
    }

    @Test
    public void testValidationRunOnModelChangeOk() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1, sku2);
        sku1.addPicture(makePicture2("XLPicture2", OLD));
        saveGroup.addBeforeModels(getBeforeList(parentModel, sku1));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        parentModel.addPicture(makePicture1("XLPicture1", NEW));

        List<ModelValidationError> errors = validate(saveGroup, parentModel);

        assertThat(errors).isEmpty();
    }

    @Test
    public void testValidationRunOnModelChangeFail() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1, sku2);
        sku1.addPicture(makePicture2Url1(OLD));
        saveGroup.addBeforeModels(getBeforeList(parentModel, sku1));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        parentModel.addPicture(makePicture2("XLPicture1", NEW));

        List<ModelValidationError> errors = validate(saveGroup, parentModel);

        assertThat(errors).containsExactlyInAnyOrder(
            createError(parentModel, "XLPicture1", sku1.getId(), "0", false));
    }

    @Test
    public void testTwoPicsInSameModelOnCreate() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1, sku2);
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        sku1.setId(CommonModel.NO_ID);
        sku1.addPicture(makePicture2(NEW));
        sku1.addPicture(makePicture2Url1(NEW));

        List<ModelValidationError> errors = validate(saveGroup, parentModel);

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).satisfiesAnyOf(
            e -> assertThat(e).isEqualTo(createError(sku1, "0", sku1.getId(), "1", false)),
            e -> assertThat(e).isEqualTo(createError(sku1, "1", sku1.getId(), "0", false))
        );
    }

    @Test
    public void testTwoPicsInSameModelOnCreateDifferentDates() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1, sku2);
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        parentModel.addPicture(makePicture2("XLPicture1", OLD));
        parentModel.addPicture(makePicture2Url1("XLPicture2", NEW));

        List<ModelValidationError> errors = validate(saveGroup, parentModel);

        assertThat(errors).containsExactlyInAnyOrder(
            createError(parentModel, "XLPicture2", parentModel.getId(), "XLPicture1", false));
    }

    @Test
    public void testTwoPicsInSameModelOnUpdate() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1, sku2);
        parentModel.addPicture(makePicture2("XLPicture1", OLD));
        saveGroup.addBeforeModels(getBeforeList(parentModel));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        parentModel.addPicture(makePicture2Url1("XLPicture2", NEW));

        List<ModelValidationError> errors = validate(saveGroup, parentModel);

        assertThat(errors).containsExactlyInAnyOrder(
            createError(parentModel, "XLPicture2", parentModel.getId(), "XLPicture1", false));
    }

    @Test
    public void testCheckOnModificationUpdateOk() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModelWithModif, modification, sku3);
        modification.addPicture(makePicture2("XLPicture2", OLD));
        sku3.addPicture(makePicture3HashSameAs2(OLD));
        saveGroup.addBeforeModels(getBeforeList(parentModelWithModif, modification, sku3));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);
        when(context.getValidModifications(eq(parentModelWithModif.getId()), anyCollection()))
            .thenReturn(Collections.singletonList(modification));

        parentModelWithModif.addPicture(makePicture1("XLPicture1", NEW));

        List<ModelValidationError> errors = validate(saveGroup, parentModelWithModif);

        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckOnModificationUpdateFail() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModelWithModif, modification, sku3);
        parentModelWithModif.addPicture(makePicture2("XLPicture1", OLD));
        sku3.addPicture(makePicture1(OLD));
        saveGroup.addBeforeModels(getBeforeList(parentModelWithModif, modification, sku3));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);
        when(context.getValidModifications(eq(parentModelWithModif.getId()), anyCollection()))
            .thenReturn(Collections.singletonList(modification));

        modification.addPicture(makePicture2Url1("XLPicture2", NEW));

        List<ModelValidationError> errors = validate(saveGroup, parentModelWithModif);

        assertThat(errors).containsExactlyInAnyOrder(
            createError(modification, "XLPicture2", parentModelWithModif.getId(),
                "XLPicture1", false));
    }

    @Test
    public void testSameHashCase() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1);
        sku1.addPicture(makePicture3HashSameAs2(OLD));
        saveGroup.addBeforeModels(getBeforeList(parentModel, sku1));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        parentModel.addPicture(makePicture2("XLPicture1", NEW));

        List<ModelValidationError> errors = validate(saveGroup, parentModel);

        assertThat(errors).isEmpty();
    }

    @Test
    public void testHaveNoHashPicsCase() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1, sku2);
        sku1.addPicture(makePicture2WOHash(OLD));
        saveGroup.addBeforeModels(getBeforeList(parentModel, sku1));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        parentModel.addPicture(makePicture2("XLPicture1", NEW));

        List<ModelValidationError> errors = validate(saveGroup, parentModel);

        assertThat(errors).isEmpty();
    }

    @Test
    public void testSingleNewMultipleOld() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1, sku2);
        parentModel.addPicture(makePicture2("XLPicture1", OLD));
        sku1.addPicture(makePicture2(OLD));
        saveGroup.addBeforeModels(getBeforeList(parentModel, sku1));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        parentModel.addPicture(makePicture2Url1("XLPicture2", NEW));

        List<ModelValidationError> errors = validate(saveGroup, parentModel);

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).satisfiesAnyOf(
            e -> assertThat(e).isEqualTo(
                createError(parentModel, "XLPicture2", parentModel.getId(), "XLPicture1", false)),
            e -> assertThat(e).isEqualTo(
                createError(parentModel, "XLPicture2", sku1.getId(), "0", false))
        );
    }

    @Test
    public void testMutualDuplicates() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1, sku2);
        sku2.addPicture(makePicture1(OLD));
        saveGroup.addBeforeModels(getBeforeList(parentModel));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);


        sku1.addPicture(makePicture2(NEW));
        sku2.addPicture(makePicture2Url1(NEW));

        List<ModelValidationError> errors = validate(saveGroup, parentModel);
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).satisfiesAnyOf(
            e -> assertThat(e).isEqualTo(createError(sku1, "0", sku2.getId(), "1", false)),
            e -> assertThat(e).isEqualTo(createError(sku2, "1", sku1.getId(), "0", false))
        );
    }

    @Test
    public void testMultipleNewSingleOld() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1, sku2);
        parentModel.addPicture(makePicture2("XLPicture1", OLD));
        saveGroup.addBeforeModels(getBeforeList(parentModel));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        parentModel.addPicture(makePicture2Url3("XLPicture2", NEW));
        sku1.addPicture(makePicture2Url1(NEW));
        sku2.addPicture(makePicture2Url2(NEW));

        List<ModelValidationError> errors = validate(saveGroup, parentModel);
        assertThat(errors).containsExactlyInAnyOrder(
            createError(parentModel, "XLPicture2", parentModel.getId(), "XLPicture1", false),
            createError(sku1, "0", parentModel.getId(), "XLPicture1", false),
            createError(sku2, "0", parentModel.getId(), "XLPicture1", false));
    }

    @Test
    public void testMultipleNew() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1, sku2);
        saveGroup.addBeforeModels(getBeforeList(parentModel));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        parentModel.addPicture(makePicture2Url3("XLPicture2", OLD));
        sku1.addPicture(makePicture2Url1(NEW));
        sku2.addPicture(makePicture2Url2(NEW));

        List<ModelValidationError> errors = validate(saveGroup, parentModel);
        assertThat(errors).containsExactlyInAnyOrder(
            createError(sku1, "0", parentModel.getId(), "XLPicture2", false),
            createError(sku2, "0", parentModel.getId(), "XLPicture2", false));
    }

    @Test
    public void testSameUrlOrigOperatorFilledCopiedWithOriginalOnGuru() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1, sku2);
        saveGroup.addBeforeModels(getBeforeList(parentModel));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        Picture operatorFilledSkuPic = makePicture2Url3("<sku pic>", NEW);
        operatorFilledSkuPic.setModificationSource(ModificationSource.OPERATOR_FILLED);

        Picture operatorCopiedSkuPic = makePicture2Url3("<sku pic>", NEW);
        operatorCopiedSkuPic.setModificationSource(ModificationSource.OPERATOR_COPIED);

        parentModel.addPicture(makePicture2Url3("XLPicture2", OLD)); // this one is older and treated as original
        sku1.addPicture(operatorFilledSkuPic); // new duplicate with FILLED source --> raises error
        sku2.addPicture(operatorCopiedSkuPic); // new duplicate with COPIED is ok

        List<ModelValidationError> errors = validate(saveGroup, parentModel);
        assertThat(errors).containsExactlyInAnyOrder(
            createError(sku1, "<sku pic>", parentModel.getId(), "XLPicture2", false));
    }

    @Test
    public void testSameUrlOrigOperatorFilledCopiedWithOriginalOnSku() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1, sku2);
        saveGroup.addBeforeModels(getBeforeList(parentModel));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        Picture operatorCopiedSkuPic = makePicture2Url3("<sku pic>", OLD);
        operatorCopiedSkuPic.setModificationSource(ModificationSource.OPERATOR_COPIED);

        Picture operatorFilledSkuPic = makePicture2Url3("<sku pic>", OLD);
        operatorFilledSkuPic.setModificationSource(ModificationSource.OPERATOR_FILLED);

        Picture operatorFilledGuruPic = makePicture2Url3("XLPicture2", NEW);
        operatorFilledGuruPic.setModificationSource(ModificationSource.OPERATOR_FILLED);

        parentModel.addPicture(operatorFilledGuruPic); // new duplicate with FILLED source --> raises error
        sku1.addPicture(operatorCopiedSkuPic); // old copy is ok
        sku2.addPicture(operatorFilledSkuPic); // this one is older and treated as original

        List<ModelValidationError> errors = validate(saveGroup, parentModel);
        assertThat(errors).containsExactlyInAnyOrder(
            createError(parentModel, "XLPicture2", sku2.getId(), "<sku pic>", false));
    }

    @Test
    public void testSameUrlOrigOperatorFilledPicsWithSameDateClash() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1, sku2);
        saveGroup.addBeforeModels(getBeforeList(parentModel));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        Picture operatorFilledSkuPic1 = makePicture2Url3("<sku pic1>", OLD);
        operatorFilledSkuPic1.setModificationSource(ModificationSource.OPERATOR_FILLED);

        Picture operatorFilledSkuPic2 = makePicture2Url3("<sku pic2>", OLD);
        operatorFilledSkuPic2.setModificationSource(ModificationSource.OPERATOR_FILLED);

        Picture operatorCopiedGuruPic = makePicture2Url3("XLPicture2", NEW);
        operatorCopiedGuruPic.setModificationSource(ModificationSource.OPERATOR_COPIED);

        parentModel.addPicture(operatorCopiedGuruPic); // new duplicate with COPIED is ok
        sku1.addPicture(operatorFilledSkuPic1); // this one is older and treated as original, it has FILLED source
        sku2.addPicture(operatorFilledSkuPic2); // another old pic with FILLED source --> raises error

        List<ModelValidationError> errors = validate(saveGroup, parentModel);
        assertThat(errors).containsExactlyInAnyOrder(
            createError(sku2, "<sku pic2>", sku1.getId(), "<sku pic1>", false));
    }

    @Test
    public void testSameUrlOrigPicturesWithUnusualModifSourcesDontTriggerError() {
        // Modif source checks should not affect tools, mdm, automatic and rule changes
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1);
        saveGroup.addBeforeModels(getBeforeList(parentModel));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        Picture operatorFilledGuruPic = makePicture2Url3("XLPicture2", OLD);
        operatorFilledGuruPic.setModificationSource(ModificationSource.OPERATOR_FILLED);
        parentModel.addPicture(operatorFilledGuruPic);

        // Add one picture of each possible modification source except FILLED:
        Arrays.stream(ModificationSource.values())
            .filter(s -> s != ModificationSource.OPERATOR_FILLED).forEach(possibleModifSource -> {
                Picture skuPicWithUnusualSource = makePicture2Url3(possibleModifSource.name(), NEW);
                skuPicWithUnusualSource.setModificationSource(possibleModifSource);
                sku1.addPicture(skuPicWithUnusualSource);
        });

        List<ModelValidationError> errors = validate(saveGroup, parentModel);
        assertThat(errors).isEmpty();
    }

    @Test
    public void testNewCopied() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1, sku2);
        sku1.addPicture(makePicture2(OLD));
        saveGroup.addBeforeModels(getBeforeList(parentModel));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        sku1.addPicture(makePicture2(NEW));
        sku1.addPicture(makePicture2(NEW));

        List<ModelValidationError> errors = validate(saveGroup, parentModel);
        assertThat(errors).isEmpty();
    }

    @Test
    public void testMultipleNewCopied() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1, sku2);
        parentModel.addPicture(makePicture3HashSameAs2("XLPicture1", OLD));
        saveGroup.addBeforeModels(getBeforeList(parentModel));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        sku1.addPicture(makePicture2Url1(NEW));
        sku1.addPicture(makePicture2Url1(OLD));

        sku2.addPicture(makePicture2Url2(NEW));
        sku2.addPicture(makePicture2Url2(NEW));
        sku2.addPicture(makePicture3HashSameAs2(NEW));

        List<ModelValidationError> errors = validate(saveGroup, parentModel);
        assertThat(errors).containsExactlyInAnyOrder(
            createError(sku2, "0", sku1.getId(), "1", false),
            createError(sku2, "1", sku1.getId(), "1", false));
    }

    @Test
    public void testNoUrlOrigIgnored() {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(parentModel, sku1, sku2);
        saveGroup.addBeforeModels(getBeforeList(parentModel));
        when(context.getModelSaveGroup()).thenReturn(saveGroup);

        parentModel.addPicture(makePicture2Url3("XLPicture2", OLD));
        sku1.addPicture(makePicture2Url1(NEW));
        sku2.addPicture(makePicture2Url2(NEW).setUrlOrig(null));

        List<ModelValidationError> errors = validate(saveGroup, parentModel);
        assertThat(errors).containsExactlyInAnyOrder(
            createError(sku1, "0", parentModel.getId(), "XLPicture2", false));
    }

    private ModelChanges getModelChanges(CommonModel before, CommonModel after) {
        return new ModelChanges(before, after);
    }

    private Picture makePicture1(String xslName, Date date) {
        return makePicture(xslName, "123", PICTURE1_URL, date);
    }

    private Picture makePicture1(Date date) {
        return makePicture1(null, date);
    }

    private Picture makePicture2(String xslName, Date date) {
        return makePicture(xslName, "234", PICTURE2_URL, date);
    }

    private Picture makePicture2(Date date) {
        return makePicture2(null, date);
    }

    private Picture makePicture2Url1(String xslName, Date date) {
        return makePicture(xslName, "234", PICTURE2_URL1, date);
    }

    private Picture makePicture2Url1(Date date) {
        return makePicture2Url1(null, date);
    }

    private Picture makePicture2Url2(Date date) {
        return makePicture(null, "234", PICTURE2_URL2, date);
    }

    private Picture makePicture2Url3(String xslName, Date date) {
        return makePicture(xslName, "234", PICTURE2_URL3, date);
    }

    private Picture makePicture3HashSameAs2(String xslName, Date date) {
        return makePicture(xslName, "234", PICTURE3_URL, date);
    }

    private Picture makePicture3HashSameAs2(Date date) {
        return makePicture3HashSameAs2(null, date);
    }

    private Picture makePicture2WOHash(Date date) {
        return makePicture(null, null, PICTURE2_URL, date);
    }

    private Picture makePicture(String xslName, String md5, String url, Date date) {
        Picture pic = new Picture();
        if (xslName != null) {
            pic.setXslName(xslName);
        }
        pic.setOrigMd5(md5);
        pic.setUrlOrig(url);
        pic.setLastModificationDate(date);

        return pic;
    }

    private List<ModelValidationError> validate(ModelSaveGroup saveGroup, CommonModel model) {
        return validator.validate(
            context,
            getModelChanges(saveGroup.getBeforeById(model.getId()), model),
            saveGroup.getModels()
        );
    }

    private static CommonModel getBefore(CommonModel model) {
        return new CommonModel(model);
    }

    private static List<CommonModel> getBeforeList(CommonModel... models) {
        return Arrays.stream(models).map(ImageUniquenessValidatorTest::getBefore).collect(Collectors.toList());
    }
}
