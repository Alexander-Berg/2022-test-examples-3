package ru.yandex.market.mbo.db.modelstorage.guru;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.core.kdepot.api.KnownEntityTypes;
import ru.yandex.market.mbo.db.modelstorage.ModelEditService;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageService;
import ru.yandex.market.mbo.db.modelstorage.index.FilterHelper;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.params.guru.GuruService;
import ru.yandex.market.mbo.db.params.guru.GuruVendorsReader;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.modelstorage.models.MoveSkusGroup;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.ParameterValueBuilder;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MoveModelServiceTest {
    private static final long NAME_PARAM_ID = 333;
    private static final long USER_ID = 3;
    private static final int OPTION_ID11 = 11;
    private static final int OPTION_ID22 = 22;
    private static final int OPTION_ID33 = 33;
    private static final long PARENT_MODEL_ID = 123L;
    private static final long CATEGORY_ID = 1L;
    private static final long UID = 1L;
    private static final long MODEL_ID = 1L;
    private static final long NEW_PARENT_MODEL_ID = 2L;
    private static final CommonModel MODEL = model(MODEL_ID);
    private static final CommonModel NEW_PARENT_MODEL = model(NEW_PARENT_MODEL_ID);
    public static final long SKU_11_ID = 11L;
    public static final long SKU_12_ID = 12L;
    public static final long SKU_13_ID = 13L;
    public static final long SKU_21_ID = 21L;

    @Mock
    private ModelEditService modelEditService;

    @Mock
    private GuruService guruService;

    @Mock
    private GuruVendorsReader vendorsReader;

    @Mock
    private ModelStorageService modelStorageService;

    @Captor
    ArgumentCaptor<Collection<CommonModel>> captor;

    @InjectMocks
    private MoveModelService moveModelService;


    @Test(expected = IllegalStateException.class)
    public void changeVendorIfVendorNotExists() {
        doReturn(null).when(vendorsReader).getVendor(Mockito.anyLong());
        moveModelService.changeParent(KnownEntityTypes.MARKET_LOCAL_VENDOR, CATEGORY_ID, 1, 2, USER_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void changeVendorIfSameVendor() {
        doReturn(vendorOptionWithParentId(OPTION_ID11)).when(vendorsReader).getVendor(Mockito.anyLong());
        doReturn(modelWithVendorOptionId(OPTION_ID11)).when(modelEditService).findModel(eq(1L));

        moveModelService.changeParent(KnownEntityTypes.MARKET_LOCAL_VENDOR, CATEGORY_ID, 1, 2, USER_ID);
    }

    @Test
    public void moveModificationToVendor() {
        doReturn(modificationWithVendorOptionId(OPTION_ID11)).when(modelEditService).findModel(eq(1L));

        moveModelService.changeParent(KnownEntityTypes.MARKET_LOCAL_VENDOR, CATEGORY_ID, 1, 2, USER_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void testMoveInsideModelWithModifications() {
        reset(guruService);
        reset(modelEditService);
        reset(modelStorageService);
        doReturn(true).when(guruService).isGroupCategory(CATEGORY_ID);
        doReturn(2L).when(modelStorageService).count(any(MboIndexesFilter.class));
        doReturn(MODEL).when(modelEditService).findModel(eq(MODEL_ID));

        try {
            moveModelService.changeParent(KnownEntityTypes.MARKET_MODEL,
                                          CATEGORY_ID,
                                          MODEL.getId(),
                                          NEW_PARENT_MODEL.getId(),
                                          UID);
        } catch (Exception e) {
            ArgumentCaptor<MboIndexesFilter> queryCaptor = ArgumentCaptor.forClass(MboIndexesFilter.class);
            verify(modelStorageService, times(1)).count(queryCaptor.capture());
            Assert.assertEquals(FilterHelper.guruLiveModificationCriteria(MODEL.getId()), queryCaptor.getValue());
            throw e;
        }
    }

    @Test
    public void testMoveInsideModelWithoutModifications() {
        reset(guruService);
        reset(modelEditService);
        reset(modelStorageService);
        doReturn(true).when(guruService).isGroupCategory(CATEGORY_ID);
        doReturn(MODEL).when(modelEditService).findModel(eq(MODEL_ID));
        doReturn(NEW_PARENT_MODEL).when(modelEditService).findModel(eq(NEW_PARENT_MODEL_ID));
        doReturn(0L).when(modelStorageService).count(any(MboIndexesFilter.class));
        moveModelService.changeParent(KnownEntityTypes.MARKET_MODEL,
                                      CATEGORY_ID, MODEL.getId(),
                                      NEW_PARENT_MODEL.getId(), UID);
        Assert.assertEquals(MODEL.getParentModelId(), NEW_PARENT_MODEL.getId());
    }

    @Test
    public void changeVendor() {
        CommonModel modelWithVendor = modelWithVendorOptionId(OPTION_ID22);
        doReturn(vendorOptionWithParentId(OPTION_ID33)).when(vendorsReader).getVendor(Mockito.eq(2L));
        doReturn(modelWithVendor).when(modelEditService).findModel(eq(1L));

        moveModelService.changeParent(KnownEntityTypes.MARKET_LOCAL_VENDOR, CATEGORY_ID, 1, 2, USER_ID);

        verify(modelEditService).saveModelBilled(eq(modelWithVendor), eq(USER_ID));
        Assert.assertEquals(OPTION_ID33, modelWithVendor.getVendorId());
        Assert.assertNull(modelWithVendor.getParentModel());
        Assert.assertEquals(CommonModel.NO_ID, modelWithVendor.getParentModelId());
    }

    @Test
    public void changePModelVendorFromNotDefined() {
        CommonModel modelWithVendor = modelWithVendorOptionId(KnownIds.NOT_DEFINED_GLOBAL_VENDOR);
        modelWithVendor.setCurrentType(CommonModel.Source.PARTNER);
        modelWithVendor.addParameterValue(
            ParameterValueBuilder.newBuilder()
            .paramId(KnownIds.RAW_VENDOR_PARAM_ID)
            .xslName(XslNames.RAW_VENDOR)
            .type(Param.Type.STRING)
            .words("test")
            .build());

        doReturn(vendorOptionWithParentId(OPTION_ID33)).when(vendorsReader).getVendor(Mockito.eq(2L));
        doReturn(modelWithVendor).when(modelEditService).findModel(eq(1L));

        moveModelService.changeParent(KnownEntityTypes.MARKET_LOCAL_VENDOR, CATEGORY_ID, 1, 2, USER_ID);

        verify(modelEditService).saveModelBilled(eq(modelWithVendor), eq(USER_ID));
        assertThat(modelWithVendor.getVendorId()).isEqualTo(OPTION_ID33);
        assertThat(modelWithVendor.getParentModel()).isNull();
        assertThat(modelWithVendor.getSingleParameterValue(XslNames.RAW_VENDOR)).isNull();
        assertThat(modelWithVendor.getParentModelId()).isEqualTo(CommonModel.NO_ID);
    }

    @Test
    public void changePModelVendorToNotDefined() {
        CommonModel modelWithVendor = modelWithVendorOptionId(OPTION_ID22);
        modelWithVendor.setCurrentType(CommonModel.Source.PARTNER);

        doReturn(vendorOptionWithParentId(KnownIds.NOT_DEFINED_GLOBAL_VENDOR))
            .when(vendorsReader).getVendor(Mockito.eq(KnownIds.NOT_DEFINED_GLOBAL_VENDOR));
        doReturn(vendorOptionWithParentId(OPTION_ID22))
            .when(vendorsReader).getLocalVendor(anyLong(), Mockito.eq((long) OPTION_ID22));
        doReturn(modelWithVendor).when(modelEditService).findModel(eq(1L));

        moveModelService.changeParent(
            KnownEntityTypes.MARKET_LOCAL_VENDOR, CATEGORY_ID, 1, KnownIds.NOT_DEFINED_GLOBAL_VENDOR, USER_ID);

        verify(modelEditService).saveModelBilled(eq(modelWithVendor), eq(USER_ID));
        assertThat(modelWithVendor.getVendorId()).isEqualTo(KnownIds.NOT_DEFINED_GLOBAL_VENDOR);
        assertThat(modelWithVendor.getParentModel()).isNull();
        assertThat(modelWithVendor.getSingleParameterValue(XslNames.RAW_VENDOR)).isNotNull();
        assertThat(modelWithVendor.getSingleParameterValue(XslNames.RAW_VENDOR))
            .isNotNull()
            .extracting(ParameterValue::getModificationSource)
            .isEqualTo(ModificationSource.AUTO);
        assertThat(modelWithVendor.getParentModelId()).isEqualTo(CommonModel.NO_ID);
    }

    @Test
    public void testMoveModel() {
        reset(modelStorageService);

        CommonModel model1before = model(1L, CATEGORY_ID);
        CommonModel model2before = model(2L, CATEGORY_ID);

        CommonModel sku11before = createSku(SKU_11_ID, model1before);
        CommonModel sku12before = createSku(SKU_12_ID, model1before);
        CommonModel sku13before = createSku(SKU_13_ID, model1before);
        CommonModel sku21before = createSku(SKU_21_ID, model2before);

        List<CommonModel> allModelsBefore = new ArrayList<>();

        allModelsBefore.add(model1before);
        allModelsBefore.add(model2before);
        allModelsBefore.add(sku11before);
        allModelsBefore.add(sku12before);
        allModelsBefore.add(sku13before);
        allModelsBefore.add(sku21before);

        Consumer<CommonModel> setVendorFunct = m -> {
            long rootId = m.getRelation(ModelRelation.RelationType.SKU_PARENT_MODEL)
                .map(r -> r.getId())
                .orElse(m.getId());
            if (rootId == model1before.getId()) {
                setVendor(m, OPTION_ID11);
            } else {
                setVendor(m, OPTION_ID22);
            }
        };
        allModelsBefore.forEach(setVendorFunct);
        assertThat(allModelsBefore.stream().map(CommonModel::getVendorId).collect(Collectors.toSet()))
            .containsExactlyInAnyOrder((long) OPTION_ID11, (long) OPTION_ID22);

        doReturn(allModelsBefore).when(modelStorageService).getModelsWithParent(anyLong(), anyCollection());

        MoveSkusGroup group = new MoveSkusGroup()
            .withSkuIds(Arrays.asList(SKU_11_ID, SKU_12_ID, SKU_13_ID))
            .withSourceModelId(model1before.getId())
            .withTargetModelId(model2before.getId())
            .withForce(true)
            .withCategoryId(CATEGORY_ID)
            .withUid(1L);
        moveModelService.moveSkus(group);

        CommonModel model1after = model(1L, CATEGORY_ID);
        CommonModel model2after = model(2L, CATEGORY_ID);

        CommonModel sku11after = createSku(SKU_11_ID, model2after);
        CommonModel sku12after = createSku(SKU_12_ID, model2after);
        CommonModel sku13after = createSku(SKU_13_ID, model2after);
        CommonModel sku21after = createSku(SKU_21_ID, model2after);

        Set<CommonModel> allModelsAfter = new HashSet<>();

        allModelsAfter.add(model1after);
        allModelsAfter.add(model2after);
        allModelsAfter.add(sku11after);
        allModelsAfter.add(sku12after);
        allModelsAfter.add(sku13after);
        allModelsAfter.add(sku21after);
        allModelsAfter.forEach(setVendorFunct);

        verify(modelStorageService).saveModels(captor.capture(), any());
        Assert.assertTrue(haveSameRelationsCategoryVendor(allModelsAfter, captor.getValue()));
    }

    private boolean haveSameRelationsCategoryVendor(Collection<CommonModel> expected, Collection<CommonModel> actual) {
        Map<Long, CommonModel> actualMap = actual.stream()
            .collect(Collectors.toMap(CommonModel::getId, Function.identity()));

        if (expected.size() != actual.size()) {
            return false;
        }

        for (CommonModel expModel : expected) {
            CommonModel actModel = actualMap.get(expModel.getId());
            if (actModel == null) {
                return false;
            }

            Set<ModelRelation> actRelationsSet = new HashSet<>(actModel.getRelations());
            Set<ModelRelation> expRelationsSet = new HashSet<>(expModel.getRelations());
            if (!actRelationsSet.equals(expRelationsSet)) {
                return false;
            }

            if (expModel.getCategoryId() != actModel.getCategoryId()) {
                return false;
            }
            if (expModel.getVendorId() != actModel.getVendorId()) {
                return false;
            }
        }

        return true;
    }

    private OptionImpl vendorOptionWithParentId(long parentId) {
        OptionImpl vendor = new OptionImpl(Option.OptionType.VENDOR);
        vendor.setParent(new OptionImpl(parentId));
        return vendor;
    }

    private CommonModel modificationWithVendorOptionId(long optionId) {
        CommonModel parentModel = model(PARENT_MODEL_ID);
        CommonModel modification = modification(parentModel);
        return setVendor(modification, optionId);
    }

    private CommonModel modelWithVendorOptionId(long optionId) {
        CommonModel model = model(PARENT_MODEL_ID);
        return setVendor(model, optionId);
    }

    private CommonModel setVendor(CommonModel model, long vendorId) {
        model.putParameterValues(
            new ParameterValues(
                1,
                XslNames.VENDOR,
                Param.Type.ENUM,
                vendorId
            )
        );
        return model;
    }

    private CommonModel modification(CommonModel parentModel) {
        CommonModel modification = new CommonModel();
        modification.setParentModel(parentModel);
        modification.setParentModelId(parentModel.getId());

        return modification;
    }

    private static CommonModel model(long id) {
        CommonModel result = new CommonModel();
        result.setId(id);

        return result;
    }

    private static CommonModel model(long id, long categoryId) {
        CommonModel result = model(id);
        result.setCategoryId(categoryId);
        result.addParameterValue(
            new ParameterValue(NAME_PARAM_ID, XslNames.NAME, Param.Type.STRING,
                ParameterValue.ValueBuilder.newBuilder()
                    .setStringValue(new Word(Word.DEFAULT_LANG_ID, "model " + id))));

        return result;
    }

    private static CommonModel createSku(long id, CommonModel parent) {
        CommonModel sku = model(id, parent.getCategoryId());
        sku.setCurrentType(CommonModel.Source.SKU);
        sku.setCategoryId(parent.getCategoryId());
        sku.addParameterValue(
            new ParameterValue(NAME_PARAM_ID, XslNames.NAME, Param.Type.STRING,
                ParameterValue.ValueBuilder.newBuilder()
                    .setStringValue(new Word(Word.DEFAULT_LANG_ID, "sku " + id))));
        parent.addRelation(new ModelRelation(id, parent.getCategoryId(), ModelRelation.RelationType.SKU_MODEL));
        sku.addRelation(
            new ModelRelation(parent.getId(), parent.getCategoryId(), ModelRelation.RelationType.SKU_PARENT_MODEL));

        return sku;
    }
}
