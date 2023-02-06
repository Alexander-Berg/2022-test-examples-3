package ru.yandex.market.mbo.db.modelstorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.core.modelstorage.util.ModelStorageExportUtil;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.export.modelstorage.pipe.ModelPipeContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.http.ModelStorage;

import static ru.yandex.market.mbo.core.modelstorage.util.ModelStorageExportUtil.SKU_BATCH_SIZE;

/**
 * @author dmserebr
 * @date 06.07.18
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ModelStorageExportUtilTest {
    private static final Long CATEGORY_ID = 10L;
    private static final Long VENDOR_ID = 20L;

    private ModelStorageServiceStub modelStorageService;

    @Before
    public void init() throws Exception {
        modelStorageService = Mockito.spy(new ModelStorageServiceStub());
        modelStorageService.setModelsMap(new HashMap<>());

        Mockito.doAnswer(i -> {
            long categoryId = i.getArgument(0);
            MboIndexesFilter filter = i.getArgument(1);
            List<Long> queriedModelIds = new ArrayList<>(filter.getModelIds());
            Assertions.assertThat(queriedModelIds.size()).isLessThanOrEqualTo(SKU_BATCH_SIZE);
            Consumer<ModelStorage.Model> consumer = i.getArgument(2);
            modelStorageService.getModels(categoryId, queriedModelIds).stream()
                .map(ModelProtoConverter::convert)
                .forEach(consumer);
            return null;
        }).when(modelStorageService).processQueryFullModels(
            Mockito.anyLong(), Mockito.any(MboIndexesFilter.class), ArgumentMatchers.any());

    }

    @Test
    public void testIdsSplit() {
        CommonModelBuilder builder = CommonModelBuilder.newBuilder(1L, CATEGORY_ID, VENDOR_ID)
            .currentType(CommonModel.Source.GURU);

        LongStream.range(10, 10000).forEach(i -> {
            CommonModel sku = CommonModelBuilder.newBuilder(i, CATEGORY_ID, VENDOR_ID)
                .currentType(CommonModel.Source.SKU)
                .withSkuParentRelation(CATEGORY_ID, 1L)
                .getModel();
            modelStorageService.saveModel(sku, 0L);
            builder.withSkuRelations(CATEGORY_ID, sku.getId());
        });

        CommonModel model = builder.getModel();

        Multimap<Long, ModelStorage.Model> result = ModelStorageExportUtil.fetchSkusForModelsAndModifs(
                Collections.singletonMap(model.getId(), ModelProtoConverter.convert(model)),
                Collections.emptyMap(),
                modelStorageService,
                model.getCategoryId(),
                false);

        Assertions.assertThat(result.values().size()).isEqualTo(10000 - 10);
    }

    @Test
    public void testProcessVendorAndGenerateModels() {
        //should be processed
        CommonModelBuilder vendorModelBuilder = CommonModelBuilder.newBuilder(1L, CATEGORY_ID, VENDOR_ID)
                .currentType(CommonModel.Source.VENDOR);
        CommonModel vendorModel = vendorModelBuilder.getModel();
        modelStorageService.saveModel(vendorModel, 0L);

        //should be processed
        CommonModelBuilder generatedModelBuilder = CommonModelBuilder.newBuilder(2L, CATEGORY_ID, VENDOR_ID)
                .currentType(CommonModel.Source.GENERATED);
        CommonModel generatedModel = generatedModelBuilder.getModel();
        modelStorageService.saveModel(generatedModel, 0L);

        //should NOT be processed
        CommonModelBuilder tolocaModelBuilder = CommonModelBuilder.newBuilder(3L, CATEGORY_ID, VENDOR_ID)
                .currentType(CommonModel.Source.TOLOKA);
        CommonModel tolokaModel = tolocaModelBuilder.getModel();
        modelStorageService.saveModel(tolokaModel, 0L);

        //should NOT be processed
        CommonModelBuilder bookModelBuilder = CommonModelBuilder.newBuilder(4L, CATEGORY_ID, VENDOR_ID)
                .currentType(CommonModel.Source.BOOK);
        CommonModel bookModel = bookModelBuilder.getModel();
        modelStorageService.saveModel(bookModel, 0L);

        Set<Long> ids = Stream.of(vendorModel, generatedModel, tolokaModel, bookModel)
                              .map(CommonModel::getId).collect(Collectors.toSet());

        TovarCategory node = new TovarCategory();
        node.setHid(CATEGORY_ID);
        node.setGuruCategoryId(CATEGORY_ID + 123);

        List<ModelStorage.Model> result = ModelStorageExportUtil.getSortedModelsWithModifsAndSkus(node, ids,
                                                                        modelStorageService, false, false);
        Assertions.assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void testProcessVendorModelWithGeneratedSkus() {
        CommonModel vendorModel = CommonModelBuilder.newBuilder(1L, CATEGORY_ID, VENDOR_ID)
            .currentType(CommonModel.Source.VENDOR)
            .withSkuRelations(CATEGORY_ID, 2L, 3L)
            .getModel();

        CommonModel generatedSku1 = CommonModelBuilder.newBuilder(2L, CATEGORY_ID, VENDOR_ID)
            .currentType(CommonModel.Source.GENERATED_SKU)
            .withSkuParentRelation(vendorModel)
            .getModel();

        CommonModel generatedSku2 = CommonModelBuilder.newBuilder(3L, CATEGORY_ID, VENDOR_ID)
            .currentType(CommonModel.Source.GENERATED_SKU)
            .withSkuParentRelation(vendorModel)
            .getModel();

        CommonModel brokenGeneratedSku = CommonModelBuilder.newBuilder(4L, CATEGORY_ID, VENDOR_ID)
            .currentType(CommonModel.Source.GENERATED_SKU)
            .withSkuParentRelation(100L, CATEGORY_ID)
            .getModel();

        modelStorageService.saveModel(vendorModel, 0L);
        modelStorageService.saveModel(generatedSku1, 0L);
        modelStorageService.saveModel(generatedSku2, 0L);
        modelStorageService.saveModel(brokenGeneratedSku, 0L);

        TovarCategory node = new TovarCategory();
        node.setHid(CATEGORY_ID);
        node.setGuruCategoryId(CATEGORY_ID + 123);

        List<ModelStorage.Model> result = ModelStorageExportUtil.getSortedModelsWithModifsAndSkus(node,
            Sets.newHashSet(1L, 2L, 3L, 4L), modelStorageService, false, false);
        Assertions.assertThat(result)
            .extracting(ModelStorage.Model::getId)
            .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    public void testCreatePipeContextsForParamInheritance() {
        modelStorageService.saveModels(Arrays.asList(
            // model with 2 skus
            CommonModelBuilder.newBuilder(1L, CATEGORY_ID, VENDOR_ID)
                .currentType(CommonModel.Source.GURU)
                .withSkuRelations(CATEGORY_ID, 2L, 3L)
                .getModel(),
            CommonModelBuilder.newBuilder(2L, CATEGORY_ID, VENDOR_ID)
                .currentType(CommonModel.Source.SKU)
                .withSkuParentRelation(CATEGORY_ID, 1L)
                .getModel(),
            CommonModelBuilder.newBuilder(3L, CATEGORY_ID, VENDOR_ID)
                .currentType(CommonModel.Source.SKU)
                .withSkuParentRelation(CATEGORY_ID, 1L)
                .getModel(),

            // model with 2 modifications, each of them with 1 or 2 skus
            CommonModelBuilder.newBuilder(4L, CATEGORY_ID, VENDOR_ID)
                .currentType(CommonModel.Source.GURU)
                .getModel(),
            CommonModelBuilder.newBuilder(5L, CATEGORY_ID, VENDOR_ID)
                .currentType(CommonModel.Source.GURU)
                .parentModelId(4L)
                .withSkuRelations(CATEGORY_ID, 6L, 7L)
                .getModel(),
            CommonModelBuilder.newBuilder(6L, CATEGORY_ID, VENDOR_ID)
                .currentType(CommonModel.Source.SKU)
                .withSkuParentRelation(CATEGORY_ID, 5L)
                .getModel(),
            CommonModelBuilder.newBuilder(7L, CATEGORY_ID, VENDOR_ID)
                .currentType(CommonModel.Source.SKU)
                .withSkuParentRelation(CATEGORY_ID, 5L)
                .getModel(),
            CommonModelBuilder.newBuilder(8L, CATEGORY_ID, VENDOR_ID)
                .currentType(CommonModel.Source.GURU)
                .parentModelId(4L)
                .withSkuRelations(CATEGORY_ID, 9L)
                .getModel(),
            CommonModelBuilder.newBuilder(9L, CATEGORY_ID, VENDOR_ID)
                .currentType(CommonModel.Source.SKU)
                .withSkuParentRelation(CATEGORY_ID, 8L)
                .getModel()
        ), new ModelSaveContext(100L));

        List<CommonModel> randomModels = modelStorageService.getModels(CATEGORY_ID, Collections.singletonList(1L));
        List<ModelPipeContext> context = ModelStorageExportUtil.createPipeContextsForParamInheritance(
            randomModels, CATEGORY_ID, modelStorageService);
        assertPipeContext(context.get(0), 1L, new Long[0], new Long[0]);

        randomModels = modelStorageService.getModels(CATEGORY_ID, Arrays.asList(1L, 2L));
        context = ModelStorageExportUtil.createPipeContextsForParamInheritance(
            randomModels, CATEGORY_ID, modelStorageService);
        assertPipeContext(context.get(0), 1L, new Long[0], new Long[] {2L});

        randomModels = modelStorageService.getModels(CATEGORY_ID, Collections.singletonList(3L));
        context = ModelStorageExportUtil.createPipeContextsForParamInheritance(
            randomModels, CATEGORY_ID, modelStorageService);
        assertPipeContext(context.get(0), 1L, new Long[0], new Long[] {3L});

        randomModels = modelStorageService.getModels(CATEGORY_ID, Collections.singletonList(4L));
        context = ModelStorageExportUtil.createPipeContextsForParamInheritance(
            randomModels, CATEGORY_ID, modelStorageService);
        assertPipeContext(context.get(0), 4L, new Long[0], new Long[0]);

        randomModels = modelStorageService.getModels(CATEGORY_ID, Collections.singletonList(5L));
        context = ModelStorageExportUtil.createPipeContextsForParamInheritance(
            randomModels, CATEGORY_ID, modelStorageService);
        assertPipeContext(context.get(0), 4L, new Long[] {5L}, new Long[0]);

        randomModels = modelStorageService.getModels(CATEGORY_ID, Collections.singletonList(6L));
        context = ModelStorageExportUtil.createPipeContextsForParamInheritance(
            randomModels, CATEGORY_ID, modelStorageService);
        assertPipeContext(context.get(0), 4L, new Long[] {5L}, new Long[] {6L});

        randomModels = modelStorageService.getModels(CATEGORY_ID, Arrays.asList(5L, 7L, 9L));
        context = ModelStorageExportUtil.createPipeContextsForParamInheritance(
            randomModels, CATEGORY_ID, modelStorageService);
        assertPipeContext(context.get(0), 4L, new Long[] {5L, 8L}, new Long[] {7L, 9L});

        randomModels = modelStorageService.getModels(CATEGORY_ID, Arrays.asList(2L, 5L));
        context = ModelStorageExportUtil.createPipeContextsForParamInheritance(
            randomModels, CATEGORY_ID, modelStorageService);
        assertPipeContext(context.get(0), 1L, new Long[0], new Long[] {2L});
        assertPipeContext(context.get(1), 4L, new Long[] {5L}, new Long[0]);

        randomModels = modelStorageService.getModels(CATEGORY_ID, Arrays.asList(1L, 3L, 9L, 5L));
        context = ModelStorageExportUtil.createPipeContextsForParamInheritance(
            randomModels, CATEGORY_ID, modelStorageService);
        assertPipeContext(context.get(0), 1L, new Long[0], new Long[] {3L});
        assertPipeContext(context.get(1), 4L, new Long[] {5L, 8L}, new Long[] {9L});
    }

    @Test
    public void testCreatePipeContextsForParamInheritanceForPartnerModels() {
        modelStorageService.saveModels(Arrays.asList(
            // partner model and partner sku
            CommonModelBuilder.newBuilder(10L, CATEGORY_ID, VENDOR_ID)
                .currentType(CommonModel.Source.PARTNER)
                .withSkuRelations(CATEGORY_ID, 11L)
                .getModel(),
            CommonModelBuilder.newBuilder(11L, CATEGORY_ID, VENDOR_ID)
                .currentType(CommonModel.Source.PARTNER_SKU)
                .withSkuParentRelation(CATEGORY_ID, 10L)
                .getModel()
        ), new ModelSaveContext(100L));

        List<CommonModel> randomModels = modelStorageService.getModels(CATEGORY_ID, Collections.singletonList(10L));
        List<ModelPipeContext> context = ModelStorageExportUtil.createPipeContextsForParamInheritance(
            randomModels, CATEGORY_ID, modelStorageService);
        assertPipeContext(context.get(0), 10L, new Long[0], new Long[0]);

        randomModels = modelStorageService.getModels(CATEGORY_ID, Collections.singletonList(11L));
        context = ModelStorageExportUtil.createPipeContextsForParamInheritance(
            randomModels, CATEGORY_ID, modelStorageService);
        assertPipeContext(context.get(0), 10L, new Long[0], new Long[] {11L});
    }

    @Test
    public void testCreatePipeContextsForTemplateRenderingUseObjectFromRelation() {
        CommonModel partnerModel = CommonModelBuilder.newBuilder(10L, CATEGORY_ID, VENDOR_ID)
            .currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(CATEGORY_ID, 11L)
            .getModel();
        CommonModel partnerSku = CommonModelBuilder.newBuilder(11L, CATEGORY_ID, VENDOR_ID)
            .currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(CATEGORY_ID, 10L)
            .getModel();
        partnerModel.getRelation(ModelRelation.RelationType.SKU_MODEL).ifPresent(r -> r.setModel(partnerSku));
        partnerSku.getRelation(ModelRelation.RelationType.SKU_PARENT_MODEL).ifPresent(r -> r.setModel(partnerModel));

        List<ModelPipeContext> context = ModelStorageExportUtil.createPipeContextsForTemplateRendering(
            Collections.singletonList(partnerModel), CATEGORY_ID, modelStorageService);
        assertPipeContext(context.get(0), 10L, new Long[0], new Long[] {11L});

        context = ModelStorageExportUtil.createPipeContextsForTemplateRendering(
            Collections.singletonList(partnerSku), CATEGORY_ID, modelStorageService);
        assertPipeContext(context.get(0), 10L, new Long[0], new Long[] {11L});
    }

    private static void assertPipeContext(ModelPipeContext context,
                                          long modelId, Long[] modificationIds, Long[] skuIds) {
        Assertions.assertThat(context.getModel().getId()).isEqualTo(modelId);
        Assertions.assertThat(context.getModifications().stream()
            .map(ModelStorage.Model.Builder::getId)
            .collect(Collectors.toList()))
            .containsExactly(modificationIds);
        Assertions.assertThat(context.getSkus().stream()
            .map(ModelStorage.Model.Builder::getId)
            .collect(Collectors.toList()))
            .containsExactly(skuIds);
    }

}
