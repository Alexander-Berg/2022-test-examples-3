package ru.yandex.market.mbo.db.modelstorage.validation;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.db.modelstorage.validation.context.CachingModelValidationContext;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(MockitoJUnitRunner.class)
public class ImageExistenceValidatorTest {

    private ModelValidationContext context;

    @Mock
    private RelatedModelsContainer modelsContainer;

    private ImageExistenceValidator validator = new ImageExistenceValidator();
    private static final long GURU_ID = 1L;
    private static final long MODIF_ID = 2L;
    private static final long SKU_ID = 3L;
    private static final long HID = 1050L;
    private static final long SEED = 6667L;

    private CommonModel guru;
    private CommonModel modif;
    private CommonModel sku;

    private Map<CommonModel, CommonModelBuilder> mockMapping;
    private Map<CommonModel, CommonModel> mockBases;
    private Random random = new Random(SEED);

    @Before
    public void setup() {
        context = Mockito.mock(CachingModelValidationContext.class);
        CommonModelBuilder guruBuilder = CommonModelBuilder.newBuilder()
            .id(GURU_ID)
            .category(HID)
            .currentType(CommonModel.Source.GURU)
            .published(true);

        CommonModelBuilder modifBuilder = CommonModelBuilder.newBuilder()
            .id(MODIF_ID)
            .category(HID)
            .currentType(CommonModel.Source.GURU)
            .startModelRelation()
            .id(SKU_ID)
            .type(ModelRelation.RelationType.SKU_MODEL)
            .categoryId(HID)
            .endModelRelation()
            .published(true)
            .parentModel(guruBuilder.getModel());

        CommonModelBuilder skuBuilder = CommonModelBuilder.newBuilder()
            .id(SKU_ID)
            .category(HID)
            .currentType(CommonModel.Source.SKU)
            .startModelRelation()
            .id(MODIF_ID)
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .categoryId(HID)
            .model(modifBuilder.getModel())
            .endModelRelation()
            .published(true);

        guru = guruBuilder.getModel();
        modif = modifBuilder.getModel();
        sku = skuBuilder.getModel();

        mockMapping = new HashMap<>();
        mockMapping.put(guru, guruBuilder);
        mockMapping.put(modif, modifBuilder);
        mockMapping.put(sku, skuBuilder);

        mockBases = new HashMap<>();
        mockBases.put(guru, guru);
        mockBases.put(modif, modif);
        mockBases.put(sku, modif);

        when(modelsContainer.getAllSkus()).thenReturn(Collections.singletonList(sku));

        when(context.getRootModel(any(CommonModel.class), any())).thenReturn(guru);
        when(context.getDumpModel(any(CommonModel.class), any())).thenAnswer(i -> {
            CommonModel input = i.getArgument(0);
            return mockMapping.get(input).getRawModel();
        });
        when(context.getDumpBeforeModel(any(CommonModel.class))).thenAnswer(i -> {
            CommonModel input = i.getArgument(0);
            return mockMapping.get(input).getRawModel();
        });
        when(context.getValidModifications(MODIF_ID, groupOf(modif)))
            .thenReturn(Collections.emptyList());
        when(context.getValidModifications(GURU_ID, groupOf(guru)))
            .thenReturn(Collections.singletonList(modif));
        when(context.getRelatedModelsForModels(anyLong(), anyList(), anyList(), anyList(), anyBoolean()))
            .thenReturn(modelsContainer);
        when(context.getBaseModel(any(CommonModel.class), any())).thenAnswer(i -> {
            CommonModel input = i.getArgument(0);
            return mockBases.get(input);
        });

        when(context.getParentChain(any(), anyList())).thenCallRealMethod();
        when(context.isModelHierarchyPublished(any(), anyList())).thenCallRealMethod();
        when(context.isModelBeingPublished(any(), any())).thenCallRealMethod();
    }

    @Test
    public void testUnpublishedPasses() {
        guru.setPublished(false).setBluePublished(false);
        modif.setPublished(false).setBluePublished(false);
        sku.setPublished(false).setBluePublished(false);

        List errors = validator.validate(context, changesOf(sku), groupOf(guru, modif, sku));
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testNoPicturesFail() {
        List<ModelValidationError> errors = validator.validate(context, changesOf(sku), groupOf(guru, modif, sku));
        assertThat(errors).containsExactlyInAnyOrder(error(sku));
    }

    @Test
    public void testBookIgnored() {
        guru.setSource(CommonModel.Source.BOOK);
        modif.setSource(CommonModel.Source.BOOK);
        sku.setSource(CommonModel.Source.BOOK);
        List errors = validator.validate(context, changesOf(sku), groupOf(guru, modif, sku));
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testWithPicturesPasses() {
        addPictures();
        List errors = validator.validate(context, changesOf(sku), groupOf(guru, modif, sku));
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testModelPublishingNonGrouped() {
        modif.setPublished(false).setBluePublished(false);
        sku.setBluePublished(true);

        //unpublished ignored
        List<ModelValidationError> errors;
        errors = validator.validate(context, changesOf(modif), groupOf(modif));
        assertTrue(errors.isEmpty());

        //becomes published
        CommonModel oldModif = new CommonModel(modif);
        modif.setPublished(true);
        errors = validator.validate(context, changesOf(oldModif, modif), groupOf(modif));
        assertThat(errors).containsExactlyInAnyOrder(error(sku));
    }

    @Test
    public void testModelPublishingGrouped() {
        guru.setPublished(false).setBluePublished(false);
        modif.setPublished(true);
        sku.setBluePublished(true);

        //unpublished ignored
        List<ModelValidationError> errors;
        errors = validator.validate(context, changesOf(guru), groupOf(guru));
        assertTrue(errors.isEmpty());

        //becomes published
        CommonModel oldRoot = new CommonModel(guru);
        guru.setPublished(true);
        errors = validator.validate(context, changesOf(oldRoot, guru), groupOf(guru));
        assertThat(errors).containsExactlyInAnyOrder(error(sku));
    }

    @Test
    public void testModelRemoveLastImage() {
        guru.setPublished(true);
        modif.setPublished(true);
        sku.setBluePublished(true);

        //becomes published
        CommonModel oldRoot = new CommonModel(guru);
        oldRoot.addPicture(new Picture());
        List<ModelValidationError> errors = validator.validate(context, changesOf(oldRoot, guru), groupOf(guru));
        assertThat(errors).containsExactlyInAnyOrder(error(sku));
    }

    @Test
    public void testPublishingSkuWithoutComputedPicturesFails() {
        CommonModel beforeSku = SerializationUtils.clone(sku);
        beforeSku.setPublished(false);
        beforeSku.setBluePublished(false);
        sku.setPublished(true);
        sku.setBluePublished(true);

        List<ModelValidationError> errors = validator.validate(context, changesOf(beforeSku, sku), groupOf(sku));
        assertThat(errors).containsExactlyInAnyOrder(error(sku));
    }

    @Test
    public void testSkuWithoutComputedPicturesFailsIfPreviouslyPicturesExisted() {
        CommonModel beforeSku = SerializationUtils.clone(sku);
        beforeSku.addPicture(generateNicePicture());
        when(context.getDumpBeforeModel(any(CommonModel.class))).thenReturn(ModelProtoConverter.convert(beforeSku));
        List<ModelValidationError> errors = validator.validate(context, changesOf(beforeSku, sku), groupOf(sku));
        assertThat(errors).containsExactlyInAnyOrder(error(sku));
    }

    @Test
    public void testSkuWithoutComputedPicturesIgnoredIfNoneEverExisted() {
        CommonModel beforeSku = SerializationUtils.clone(sku);
        // ...not adding pictures to before model...
        when(context.getDumpBeforeModel(any(CommonModel.class))).thenReturn(ModelProtoConverter.convert(beforeSku));
        List<ModelValidationError> errors = validator.validate(context, changesOf(beforeSku, sku), groupOf(sku));
        assertThat(errors).containsExactlyInAnyOrder(warn(sku));
    }

    @Test
    public void testPublicity() {
        Collection<CommonModel> group = groupOf(guru, modif, sku);

        guru.setPublished(false).setBluePublished(false);
        modif.setPublished(false).setBluePublished(false);
        sku.setPublished(false).setBluePublished(false);
        assertFalse(context.isModelHierarchyPublished(guru, group));
        assertFalse(context.isModelHierarchyPublished(modif, group));
        assertFalse(context.isModelHierarchyPublished(sku, group));


        guru.setPublished(false).setBluePublished(false);
        modif.setPublished(false).setBluePublished(false);
        sku.setPublished(true);
        assertFalse(context.isModelHierarchyPublished(guru, group));
        assertFalse(context.isModelHierarchyPublished(modif, group));
        assertFalse(context.isModelHierarchyPublished(sku, group));

        guru.setPublished(false).setBluePublished(false);
        modif.setPublished(true);
        sku.setPublished(false).setBluePublished(false);
        assertFalse(context.isModelHierarchyPublished(guru, group));
        assertFalse(context.isModelHierarchyPublished(modif, group));
        assertFalse(context.isModelHierarchyPublished(sku, group));

        guru.setPublished(false).setBluePublished(false);
        modif.setPublished(true);
        sku.setPublished(true);
        assertFalse(context.isModelHierarchyPublished(guru, group));
        assertFalse(context.isModelHierarchyPublished(modif, group));
        assertFalse(context.isModelHierarchyPublished(sku, group));

        guru.setPublished(true);
        modif.setPublished(false).setBluePublished(false);
        sku.setPublished(false).setBluePublished(false);
        assertTrue(context.isModelHierarchyPublished(guru, group));
        assertFalse(context.isModelHierarchyPublished(modif, group));
        assertFalse(context.isModelHierarchyPublished(sku, group));

        guru.setPublished(true);
        modif.setPublished(false).setBluePublished(false);
        sku.setPublished(true);
        assertTrue(context.isModelHierarchyPublished(guru, group));
        assertFalse(context.isModelHierarchyPublished(modif, group));
        assertFalse(context.isModelHierarchyPublished(sku, group));

        guru.setPublished(true);
        modif.setPublished(true);
        sku.setPublished(false).setBluePublished(false);
        assertTrue(context.isModelHierarchyPublished(guru, group));
        assertTrue(context.isModelHierarchyPublished(modif, group));
        assertFalse(context.isModelHierarchyPublished(sku, group));

        guru.setPublished(true);
        modif.setPublished(true);
        sku.setPublished(true);
        assertTrue(context.isModelHierarchyPublished(guru, group));
        assertTrue(context.isModelHierarchyPublished(modif, group));
        assertTrue(context.isModelHierarchyPublished(sku, group));

    }

    @Test
    public void testBluePublicity() {
        guru.setPublished(false).setBluePublished(false);
        modif.setPublished(false).setBluePublished(false);
        sku.setPublished(false).setBluePublished(false);
        Collection<CommonModel> group = groupOf(guru, modif, sku);

        guru.setBluePublished(false).setBluePublished(false);
        modif.setBluePublished(false).setBluePublished(false);
        sku.setBluePublished(false).setBluePublished(false);
        assertFalse(context.isModelHierarchyPublished(guru, group));
        assertFalse(context.isModelHierarchyPublished(modif, group));
        assertFalse(context.isModelHierarchyPublished(sku, group));


        guru.setBluePublished(false).setBluePublished(false);
        modif.setBluePublished(false).setBluePublished(false);
        sku.setBluePublished(true);
        assertFalse(context.isModelHierarchyPublished(guru, group));
        assertFalse(context.isModelHierarchyPublished(modif, group));
        assertFalse(context.isModelHierarchyPublished(sku, group));

        guru.setBluePublished(false).setBluePublished(false);
        modif.setBluePublished(true);
        sku.setBluePublished(false).setBluePublished(false);
        assertFalse(context.isModelHierarchyPublished(guru, group));
        assertFalse(context.isModelHierarchyPublished(modif, group));
        assertFalse(context.isModelHierarchyPublished(sku, group));

        guru.setBluePublished(false).setBluePublished(false);
        modif.setBluePublished(true);
        sku.setBluePublished(true);
        assertFalse(context.isModelHierarchyPublished(guru, group));
        assertFalse(context.isModelHierarchyPublished(modif, group));
        assertFalse(context.isModelHierarchyPublished(sku, group));

        guru.setBluePublished(true);
        modif.setBluePublished(false).setBluePublished(false);
        sku.setBluePublished(false).setBluePublished(false);
        assertTrue(context.isModelHierarchyPublished(guru, group));
        assertFalse(context.isModelHierarchyPublished(modif, group));
        assertFalse(context.isModelHierarchyPublished(sku, group));

        guru.setBluePublished(true);
        modif.setBluePublished(false).setBluePublished(false);
        sku.setBluePublished(true);
        assertTrue(context.isModelHierarchyPublished(guru, group));
        assertFalse(context.isModelHierarchyPublished(modif, group));
        assertFalse(context.isModelHierarchyPublished(sku, group));

        guru.setBluePublished(true);
        modif.setBluePublished(true);
        sku.setBluePublished(false).setBluePublished(false);
        assertTrue(context.isModelHierarchyPublished(guru, group));
        assertTrue(context.isModelHierarchyPublished(modif, group));
        assertFalse(context.isModelHierarchyPublished(sku, group));

        guru.setBluePublished(true);
        modif.setBluePublished(true);
        sku.setBluePublished(true);
        assertTrue(context.isModelHierarchyPublished(guru, group));
        assertTrue(context.isModelHierarchyPublished(modif, group));
        assertTrue(context.isModelHierarchyPublished(sku, group));
    }

    @Test
    public void allowForceByFlagError() {
        CommonModel model = mock(CommonModel.class);
        doReturn(CommonModel.Source.GURU).when(model).getCurrentType();
        doReturn(true).when(context).isForceSomeIgnoreValidatorsOnRemove();
        ModelValidationError error = ImageExistenceValidator.createError(model, true, context);
        assertFalse(error.isCritical());
        assertTrue(error.isAllowForce());
    }

    @Test
    public void allowForceByFlagFalseError() {
        CommonModel model = mock(CommonModel.class);
        doReturn(CommonModel.Source.GURU).when(model).getCurrentType();
        doReturn(false).when(context).isForceSomeIgnoreValidatorsOnRemove();
        ModelValidationError error = ImageExistenceValidator.createError(model, true, context);
        assertTrue(error.isCritical());
        assertFalse(error.isAllowForce());
    }

    private ModelChanges changesOf(CommonModel model) {
        return new ModelChanges(null, model);
    }

    private ModelChanges changesOf(CommonModel modelBefore, CommonModel modelAfter) {
        return new ModelChanges(modelBefore, modelAfter);
    }

    private List<CommonModel> groupOf(CommonModel... models) {
        return Arrays.asList(models);
    }

    private void addPictures() {
        //Эмулируем результат наследований пикч в PreprocessorPipePart
        mockMapping.forEach((model, rawBuilder) -> {
            model.addPicture(generateNicePicture());
            rawBuilder.picture(generateNicePicture());
        });
    }

    private Picture generateNicePicture() {
        Picture pic = new Picture();
        pic.setColorness(random.nextDouble());
        pic.setColornessAvg(random.nextDouble());
        pic.setHeight(random.nextInt(600));
        pic.setWidth(random.nextInt(600));
        pic.setLastModificationDate(new Date());
        pic.setLastModificationUid(random.nextLong());
        pic.setModificationSource(ModificationSource.AUTO);
        pic.setOrigMd5(DigestUtils.md5Hex(String.valueOf(random.nextLong())));
        pic.setIsWhiteBackground(random.nextBoolean());
        pic.setUploaded(random.nextBoolean());
        pic.setUrl("http://goatse.cx");
        pic.setUrlOrig("http://goatse.cx");
        pic.setUrlSource("http://goatse.cx");
        return pic;
    }

    private ModelValidationError error(CommonModel model) {
        return ImageExistenceValidator.createError(model, true, context);
    }

    private ModelValidationError warn(CommonModel model) {
        return ImageExistenceValidator.createError(model, false, context);
    }
}
