package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.sku;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.model_parameter_relation.ModelParameterRelationPickerImageChanged;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.model_parameter_relation.ModelParameterRelationRemove;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.save.PopulateModelSaveSyncEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.SkuRelationChangedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableModel;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.PickerImage;

/**
 * Тестируем, что обновление пикеров происходит корректно {@link SkuPickerUpdatesAddon}.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SkuPickerUpdatesAddonTest extends BaseSkuAddonTest {
    private CommonModel model;
    private CommonModel sku;

    @Override
    public void model() {
        data.startModel()
            .title("Test model")
            .id(1).category(666).vendorId(777).currentType(CommonModel.Source.GURU)
            .startParameterValueLink()
                .paramId(33).xslName("param1")
                .optionId(1)
                .pickerImage("pic1")
            .endParameterValue()
            .startParameterValueLink()
                .paramId(33).xslName("param1")
                .optionId(2)
                .pickerImage("pic2")
            .endParameterValue()
            .startModelRelation()
                .id(2).categoryId(666).type(ModelRelation.RelationType.SKU_MODEL)
                .startModel()
                     // sku model содержит определяющие параметры
                    .id(2).category(666).currentType(CommonModel.Source.SKU)
                    .startParameterValue()
                        .paramId(33).xslName("param1").optionId(1)
                        .modificationSource(ModificationSource.OPERATOR_FILLED)
                    .endParameterValue()
                    .startParameterValue()
                        .paramId(44).xslName("param2").optionId(4)
                        .modificationSource(ModificationSource.OPERATOR_FILLED)
                    .endParameterValue()
                    .startModelRelation()
                        .id(1).categoryId(666).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                    .endModelRelation()
                .endModel()
            .endModelRelation()
            .endModel();
    }

    @Override
    public void parameters() {
        data.startParameters()
            .startParameter()
                .id(11).xsl("param1").type(Param.Type.ENUM).name("Enum1")
                .level(CategoryParam.Level.OFFER)
                .option(1, "Option1")
                .option(2, "Option2")
                .option(3, "Option3")
            .endParameter()
            .startParameter()
                .id(22).xsl("param2").type(Param.Type.ENUM).name("Enum2")
                .mandatory(true)
                .option(4, "Option4")
                .option(5, "Option5")
                .option(6, "Option6")
            .endParameter()
            .startParameter()
                .id(33).xsl("param3").type(Param.Type.ENUM).name("Enum3")
                .option(7, "Option7")
                .option(8, "Option8")
                .option(9, "Option9")
            .endParameter()
            .startParameter()
                .xsl("num").type(Param.Type.NUMERIC).name("Num")
                .skuParameterMode(SkuParameterMode.SKU_DEFINING)
            .endParameter()
            .startParameter()
                .xsl("str").type(Param.Type.STRING).name("Str")
                .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL)
            .endParameter()
            .endParameters();
    }

    @Override
    protected void onModelLoaded(EditableModel editableModel) {
        super.onModelLoaded(editableModel);

        model = editableModel.getModel();
        sku = model.getRelation(2).get().getModel();
    }

    @Test
    public void testPickerImageSetToRelatedSkus() {
        ParameterValue paramValue = sku.getSingleParameterValue(33);
        assertPickerImage(paramValue.getPickerImage(), "pic1");
    }

    @Test
    public void testPickerImageDeleteDuringSave() {
        // помечаем, что модель поменялась, чтобы система решила отправить ее на бекенд
        bus.fireEvent(new SkuRelationChangedEvent(sku));

        CommonModel modelToSave = new CommonModel(model);
        bus.fireEvent(new PopulateModelSaveSyncEvent(modelToSave));

        CommonModel skuModel = modelToSave.getRelation(2).get().getModel();
        ParameterValue paramValue = skuModel.getSingleParameterValue(33);
        assertPickerImage(paramValue.getPickerImage(), null);
    }

    @Test
    public void testValueChanged() {
        ParameterValue value = sku.getSingleParameterValue(33);
        value.setOptionId(2L);

        bus.fireEvent(new SkuRelationChangedEvent(sku));

        CommonModel skuModel = model.getRelation(2).get().getModel();
        ParameterValue paramValue = skuModel.getSingleParameterValue(33);
        assertPickerImage(paramValue.getPickerImage(), "pic2");
    }

    @Test
    public void testPickerAddToNewValue() {
        PickerImage pic3 = new PickerImage("pic3", null);
        ParameterValue value = sku.getSingleParameterValue(44);

        bus.fireEvent(new ModelParameterRelationPickerImageChanged(value, pic3));

        ParameterValue skuParamValue = sku.getSingleParameterValue(44);
        assertPickerImage(skuParamValue.getPickerImage(), "pic3");

        ParameterValue modelParamValue = model.getParameterValueLink(skuParamValue).get();
        assertPickerImage(modelParamValue.getPickerImage(), "pic3");
    }

    @Test
    public void testPickerChangedInExistingValue() {
        PickerImage pic3 = new PickerImage("pic3", null);
        ParameterValue value = sku.getSingleParameterValue(33);

        bus.fireEvent(new ModelParameterRelationPickerImageChanged(value, pic3));

        ParameterValue skuParamValue = sku.getSingleParameterValue(33);
        assertPickerImage(skuParamValue.getPickerImage(), "pic3");

        ParameterValue modelParamValue = model.getParameterValueLink(skuParamValue).get();
        assertPickerImage(modelParamValue.getPickerImage(), "pic3");
    }

    @Test
    public void testPickerRemovedInExistingValue() {
        ParameterValue value = sku.getSingleParameterValue(33);

        bus.fireEvent(new ModelParameterRelationPickerImageChanged(value, null));

        ParameterValue skuParamValue = sku.getSingleParameterValue(33);
        assertPickerImage(skuParamValue.getPickerImage(), null);

        ParameterValue modelParamValue = model.getParameterValueLink(skuParamValue).get();
        assertPickerImage(modelParamValue.getPickerImage(),  null);
    }

    @Test // MBO-14904
    public void testPickerRemovedInExistingValueIfSkuDoesntContainParamValue() {
        ParameterValue value = sku.getSingleParameterValue(33);
        sku.removeAllParameterValues(33L);

        bus.fireEvent(new ModelParameterRelationPickerImageChanged(value, null));

        ParameterValue skuParamValue = sku.getSingleParameterValue(33);
        Assert.assertNull(skuParamValue);

        ParameterValue modelParamValue = model.getParameterValueLink(value).get();
        assertPickerImage(modelParamValue.getPickerImage(),  null);
    }

    @Test
    public void testParameterValueLinkWasDeleted() {
        ParameterValue parameterValueLink = model.getParameterValueLinks().get(0);
        Assert.assertEquals(33, parameterValueLink.getParamId());
        Assert.assertEquals(new Long(1L), parameterValueLink.getOptionId());

        bus.fireEvent(new ModelParameterRelationRemove(parameterValueLink));

        Assert.assertFalse(model.getParameterValueLink(parameterValueLink).isPresent());
        ParameterValue parameterValue = sku.getSingleParameterValue(parameterValueLink.getParamId());
        assertPickerImage(parameterValue.getPickerImage(), null);
    }

    private static void assertPickerImage(PickerImage actual, String urlExpected) {
        if (urlExpected == null) {
            Assert.assertNull(actual);
            return;
        }

        Assert.assertNotNull(actual);
        Assert.assertEquals(urlExpected, actual.getUrl());
    }
}
