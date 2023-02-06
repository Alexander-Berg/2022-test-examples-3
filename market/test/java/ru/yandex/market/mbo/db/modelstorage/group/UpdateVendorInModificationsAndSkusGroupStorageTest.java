package ru.yandex.market.mbo.db.modelstorage.group;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.group.engine.BaseGroupStorageUpdatesTest;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValueUtils;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParameterValueBuilder;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
@RunWith(MockitoJUnitRunner.class)
public class UpdateVendorInModificationsAndSkusGroupStorageTest extends BaseGroupStorageUpdatesTest {

    private CommonModel singleModel;
    private CommonModel modelWithModifications;
    private CommonModel modification1;
    private CommonModel modification2;
    private CommonModel modelWithSkus;
    private CommonModel sku1;
    private CommonModel sku2;
    private CommonModel modelWithModificationsWithSkus;
    private CommonModel modification3;
    private CommonModel modification4;
    private CommonModel sku3;
    private CommonModel sku4;
    private CommonModel sku5;

    @Before
    public void setUp() throws Exception {
        // 1. Model without modifications or SKUs
        singleModel = CommonModelBuilder.newBuilder(1, 1, 1).getModel();

        // 2. Model with 2 modifications
        modelWithModifications = CommonModelBuilder.newBuilder(2, 2, 1).getModel();
        modification1 = CommonModelBuilder.newBuilder(3, 2, 100)
            .parentModelId(2).getModel();
        modification2 = CommonModelBuilder.newBuilder(4, 2, 200)
            .parentModelId(2).getModel();

        // 3. Model with 2 SKUs
        modelWithSkus = CommonModelBuilder.newBuilder(10, 2, 1)
            .modelRelation(11, 2, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(12, 2, ModelRelation.RelationType.SKU_MODEL)
            .getModel();
        sku1 = CommonModelBuilder.newBuilder(11, 2, 300)
            .currentType(CommonModel.Source.SKU)
            .modelRelation(10, 2, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();
        sku2 = CommonModelBuilder.newBuilder(12, 2, 400)
            .currentType(CommonModel.Source.SKU)
            .modelRelation(10, 2, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        // 4. Model with 2 modifications that in turn have SKUs (3 in total)
        modelWithModificationsWithSkus = CommonModelBuilder.newBuilder(13, 2, 1).getModel();
        modification3 = CommonModelBuilder.newBuilder(14, 2, 1)
            .parentModelId(13)
            .modelRelation(16, 2, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(17, 2, ModelRelation.RelationType.SKU_MODEL)
            .getModel();
        modification4 = CommonModelBuilder.newBuilder(15, 2, 1)
            .parentModelId(13)
            .modelRelation(18, 2, ModelRelation.RelationType.SKU_MODEL)
            .getModel();
        sku3 = CommonModelBuilder.newBuilder(16, 2, 1)
            .modelRelation(14, 2, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .currentType(CommonModel.Source.SKU)
            .getModel();
        sku4 = CommonModelBuilder.newBuilder(17, 2, 1)
            .modelRelation(14, 2, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .currentType(CommonModel.Source.SKU)
            .getModel();
        sku5 = CommonModelBuilder.newBuilder(18, 2, 1)
            .modelRelation(15, 2, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .currentType(CommonModel.Source.SKU)
            .getModel();

        putToStorage(singleModel,
            modelWithModifications, modification1, modification2,
            modelWithSkus, sku1, sku2,
            modelWithModificationsWithSkus, modification3, modification4, sku3, sku4, sku5);
    }

    @Test
    public void testUpdateVendorInModifications() {
        // update vendor
        ParameterValue vendorValue = modelWithModifications.getSingleParameterValue(XslNames.VENDOR);
        vendorValue.setOptionId(2L);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(singleModel, modelWithModifications);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        Assert.assertEquals(4, groupOperationStatus.getAllModelStatuses().size());

        CommonModel modif1 = groupOperationStatus.getAdditionalModel(modification1.getId()).get();
        CommonModel modif2 = groupOperationStatus.getAdditionalModel(modification2.getId()).get();
        Assert.assertEquals(2, modif1.getVendorId());
        Assert.assertEquals(2, modif2.getVendorId());
    }

    @Test
    public void testUpdateVendorInNotUpdatedModifications() {
        // update vendor
        ParameterValue vendorValue = modelWithModifications.getSingleParameterValue(XslNames.VENDOR);
        vendorValue.setOptionId(2L);
        ParameterValue vendorValueInModification = modification1.getSingleParameterValue(XslNames.VENDOR);
        vendorValueInModification.setOptionId(2L);

        putToStorage(modification1);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(singleModel, modelWithModifications);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        Assert.assertEquals(3, groupOperationStatus.getAllModelStatuses().size());

        CommonModel modif2 = groupOperationStatus.getAdditionalModel(modification2.getId()).get();
        Assert.assertEquals(2, modif2.getVendorId());
    }

    @Test
    public void testUpdateVendorIModificationPassedToList() {
        // update vendor
        ParameterValue vendorValue = modelWithModifications.getSingleParameterValue(XslNames.VENDOR);
        vendorValue.setOptionId(2L);
        modification1.putParameterValues(new ParameterValues(11, "test-param",
            Param.Type.STRING, WordUtil.defaultWord("test-value")));

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(modelWithModifications, modification1);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        Assert.assertEquals(3, groupOperationStatus.getAllModelStatuses().size());

        CommonModel modif1 = groupOperationStatus.getRequestedModel(modification1.getId()).get();
        CommonModel modif2 = groupOperationStatus.getAdditionalModel(modification2.getId()).get();
        Assert.assertEquals(2, modif1.getVendorId());
        Assert.assertEquals(2, modif2.getVendorId());
        // проверяем, что модификация сохранилась та, которую передавали на сохранение,
        // а не так, которую взяли их хранилища
        assertThat(WordUtil.getDefaultWords(modif1.getParameterValues(11).getStringValues()))
            .containsExactly("test-value");
    }

    @Test
    public void testUpdateVendorInSkusOfModel() {
        ParameterValue vendorValue = modelWithSkus.getSingleParameterValue(XslNames.VENDOR);
        vendorValue.setOptionId(2L);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(singleModel, modelWithSkus);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        Assert.assertEquals(4, groupOperationStatus.getAllModelStatuses().size());

        CommonModel updatedSku1 = groupOperationStatus.getAdditionalModel(this.sku1.getId()).get();
        CommonModel updatedSku2 = groupOperationStatus.getAdditionalModel(this.sku2.getId()).get();
        Assert.assertEquals(2, updatedSku1.getVendorId());
        Assert.assertEquals(2, updatedSku2.getVendorId());
    }

    @Test
    public void testUpdateVendorInSkusOfModifications() {
        ParameterValue vendorValue = modelWithModificationsWithSkus.getSingleParameterValue(XslNames.VENDOR);
        vendorValue.setOptionId(2L);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(singleModel, modelWithModificationsWithSkus);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        Assert.assertEquals(7, groupOperationStatus.getAllModelStatuses().size());

        CommonModel updatedModif3 = groupOperationStatus.getAdditionalModel(this.modification3.getId()).get();
        CommonModel updatedModif4 = groupOperationStatus.getAdditionalModel(this.modification4.getId()).get();
        CommonModel updatedSku3 = groupOperationStatus.getAdditionalModel(this.sku3.getId()).get();
        CommonModel updatedSku4 = groupOperationStatus.getAdditionalModel(this.sku4.getId()).get();
        CommonModel updatedSku5 = groupOperationStatus.getAdditionalModel(this.sku5.getId()).get();
        Assert.assertEquals(2, updatedModif3.getVendorId());
        Assert.assertEquals(2, updatedModif4.getVendorId());
        Assert.assertEquals(2, updatedSku3.getVendorId());
        Assert.assertEquals(2, updatedSku4.getVendorId());
        Assert.assertEquals(2, updatedSku5.getVendorId());
    }

    @Test
    public void testUpdateVendorInSkusOfModificationsMovedWithoutModel() {
        ParameterValue vendorValue = modification3.getSingleParameterValue(XslNames.VENDOR);
        vendorValue.setOptionId(2L);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(singleModel, modification3);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        Assert.assertEquals(4, groupOperationStatus.getAllModelStatuses().size());

        CommonModel updatedModif3 = groupOperationStatus.getRequestedModel(this.modification3.getId()).get();
        CommonModel updatedSku3 = groupOperationStatus.getAdditionalModel(this.sku3.getId()).get();
        CommonModel updatedSku4 = groupOperationStatus.getAdditionalModel(this.sku4.getId()).get();
        Assert.assertEquals(2, updatedModif3.getVendorId());
        Assert.assertEquals(2, updatedSku3.getVendorId());
        Assert.assertEquals(2, updatedSku4.getVendorId());
    }

    @Test
    public void testUpdateVendorInNotUpdatedSkusAndModifications() {
        ParameterValue vendorValue = modelWithModificationsWithSkus.getSingleParameterValue(XslNames.VENDOR);
        vendorValue.setOptionId(2L);

        ParameterValue vendorValueInModification3 = modification3.getSingleParameterValue(XslNames.VENDOR);
        vendorValueInModification3.setOptionId(2L);
        putToStorage(modification3);
        ParameterValue vendorValueInSku4 = sku4.getSingleParameterValue(XslNames.VENDOR);
        vendorValueInSku4.setOptionId(2L);
        putToStorage(sku4);
        ParameterValue vendorValueInSku5 = sku5.getSingleParameterValue(XslNames.VENDOR);
        vendorValueInSku5.setOptionId(2L);
        putToStorage(sku5);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(singleModel, modelWithModificationsWithSkus);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        Assert.assertEquals(4, groupOperationStatus.getAllModelStatuses().size());

        CommonModel updatedModif4 = groupOperationStatus.getAdditionalModel(this.modification4.getId()).get();
        CommonModel updatedSku3 = groupOperationStatus.getAdditionalModel(this.sku3.getId()).get();
        Assert.assertEquals(2, updatedModif4.getVendorId());
        Assert.assertEquals(2, updatedSku3.getVendorId());
    }

    @Test
    public void testUpdateRawVendorInNotUpdatedSkusAndModifications() {
        ParameterValueBuilder<ParameterValueBuilder<?>> rawParamBuilder = ParameterValueBuilder.newBuilder()
            .paramId(KnownIds.RAW_VENDOR_PARAM_ID)
            .xslName(XslNames.RAW_VENDOR)
            .type(Param.Type.STRING)
            .words("test");

        ParameterValue vendorValue = modelWithModificationsWithSkus.getSingleParameterValue(XslNames.VENDOR);
        vendorValue.setOptionId(2L);
        modelWithModificationsWithSkus.addParameterValue(rawParamBuilder.build());

        ParameterValue vendorValueInModification3 = modification3.getSingleParameterValue(XslNames.VENDOR);
        vendorValueInModification3.setOptionId(2L);
        modification3.addParameterValue(rawParamBuilder.build());
        putToStorage(modification3);
        ParameterValue vendorValueInSku4 = sku4.getSingleParameterValue(XslNames.VENDOR);
        vendorValueInSku4.setOptionId(2L);
        sku4.addParameterValue(rawParamBuilder.build());
        putToStorage(sku4);
        ParameterValue vendorValueInSku5 = sku5.getSingleParameterValue(XslNames.VENDOR);
        vendorValueInSku5.setOptionId(2L);
        sku5.addParameterValue(rawParamBuilder.build());
        putToStorage(sku5);

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(singleModel, modelWithModificationsWithSkus);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        Assert.assertEquals(4, groupOperationStatus.getAllModelStatuses().size());

        CommonModel updatedModif4 = groupOperationStatus.getAdditionalModel(this.modification4.getId()).get();
        assertThat(updatedModif4.getVendorId()).isEqualTo(2);
        assertThat(updatedModif4.getSingleParameterValue(XslNames.RAW_VENDOR))
            .isNotNull()
            .extracting(ParameterValueUtils::getStringValues)
            .asList()
            .containsExactly("test");

        CommonModel updatedSku3 = groupOperationStatus.getAdditionalModel(this.sku3.getId()).get();
        assertThat(updatedSku3.getVendorId()).isEqualTo(2);
        assertThat(updatedSku3.getSingleParameterValue(XslNames.RAW_VENDOR))
            .isNotNull()
            .extracting(ParameterValueUtils::getStringValues)
            .asList()
            .containsExactly("test");
    }

    @Test
    public void testUpdateVendorIfModificationAndSkuPassedToList() {
        ParameterValue vendorValue = modelWithModificationsWithSkus.getSingleParameterValue(XslNames.VENDOR);
        vendorValue.setOptionId(2L);
        modification3.putParameterValues(new ParameterValues(11, "test-param",
            Param.Type.STRING, WordUtil.defaultWord("test-value")));
        sku5.putParameterValues(new ParameterValues(11, "test-param",
            Param.Type.STRING, WordUtil.defaultWord("test-value2")));

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(modelWithModificationsWithSkus, modification3, sku5);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        Assert.assertEquals(6, groupOperationStatus.getAllModelStatuses().size());

        CommonModel updatedModif3 = groupOperationStatus.getRequestedModel(modification3.getId()).get();
        CommonModel updatedModif4 = groupOperationStatus.getAdditionalModel(modification4.getId()).get();
        CommonModel updatedSku3 = groupOperationStatus.getAdditionalModel(sku3.getId()).get();
        CommonModel updatedSku4 = groupOperationStatus.getAdditionalModel(sku4.getId()).get();
        CommonModel updatedSku5 = groupOperationStatus.getRequestedModel(sku5.getId()).get();
        Assert.assertEquals(2, updatedModif3.getVendorId());
        Assert.assertEquals(2, updatedModif4.getVendorId());
        Assert.assertEquals(2, updatedSku3.getVendorId());
        Assert.assertEquals(2, updatedSku4.getVendorId());
        Assert.assertEquals(2, updatedSku5.getVendorId());
        // проверяем, что модификация и SKU сохранились те, которые передавали на сохранение,
        // а не те, которые взяли из хранилища
        assertThat(WordUtil.getDefaultWords(updatedModif3.getParameterValues(11).getStringValues()))
            .containsExactly("test-value");
        assertThat(WordUtil.getDefaultWords(updatedSku5.getParameterValues(11).getStringValues()))
            .containsExactly("test-value2");
    }
}
