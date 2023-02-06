package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.sku;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableModel;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.AbstractModelTest;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.Param;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:MagicNumber")
public abstract class BaseSkuAddonTest extends AbstractModelTest {

    protected CommonModel sku;

    @Override
    public void parameters() {
        data.startParameters()
                .startParameter()
                    .id(1).xsl("param1").type(Param.Type.ENUM).name("Enum1")
                    .skuParameterMode(SkuParameterMode.SKU_DEFINING)
                    .option(11, "Option11")
                    .option(12, "Option12")
                    .option(13, "Option13")
                .endParameter()
                .startParameter()
                    .id(2).xsl("param2").type(Param.Type.ENUM).name("Enum2")
                    .mandatory(true)
                    .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL)
                    .option(21, "Option21")
                    .option(22, "Option22")
                    .option(23, "Option23")
                .endParameter()
                .startParameter()
                    .id(3).xsl("param3").type(Param.Type.ENUM).name("Enum3")
                    .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL)
                    .option(31, "Option31")
                    .option(32, "Option32")
                    .option(33, "Option33")
                    .option(34, "Option34")
                    .option(35, "Option35")
                .endParameter()
                .startParameter()
                    .id(4).xsl("param4").type(Param.Type.ENUM).name("Enum4")
                    .skuParameterMode(SkuParameterMode.SKU_DEFINING)
                    .multifield(true)
                    .option(41, "Option41")
                    .option(42, "Option42")
                    .option(43, "Option43")
                    .option(44, "Option44")
                    .option(45, "Option45")
                    .option(46, "Option46")
                .endParameter()
                .startParameter()
                    .xsl("num").type(Param.Type.NUMERIC).name("Num")
                    .skuParameterMode(SkuParameterMode.SKU_DEFINING)
                .endParameter()
                .startParameter()
                    .xsl("str").type(Param.Type.STRING).name("Str")
                    .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL)
                .endParameter()
                .startParameter()
                    .xsl("IsSku").type(Param.Type.BOOLEAN).name("IsSku")
                .endParameter()
                .startParameter()
                    .xsl("XL-Picture").type(Param.Type.STRING).name("XL-Picture")
                .endParameter()
                .startParameter()
                    .xsl("XL-Picture_2").type(Param.Type.STRING).name("XL-Picture_2")
                .endParameter()
            .endParameters();
    }

    @Override
    public void model() {
        // создаем модель и связанные с ней sku
        data.startModel()
            .title("Test model")
            .id(1).category(666).vendorId(777).currentType(CommonModel.Source.GURU)
            .picture("XL-Picture", "pic://1_2")
            .picture("XL-Picture_2", "pic://1_3")
            .param("XL-Picture").setString("pic://1_2")
            .param("XL-Picture_2").setString("pic://1_3")
            .startModelRelation()
                .id(2).categoryId(666).type(ModelRelation.RelationType.SKU_MODEL)
                .startModel()
                    // sku model содержит определяющие параметры
                    .id(2).category(666).currentType(CommonModel.Source.SKU)
                    .param("param1").setOption(1).modificationSource(ModificationSource.OPERATOR_FILLED)
                    .param("param2").setOption(4).modificationSource(ModificationSource.OPERATOR_FILLED)
                    .picture("pic://2_1")
                    .picture("pic://2_2")
                    .startModelRelation()
                        .id(1).categoryId(666).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                    .endModelRelation()
                .endModel()
            .endModelRelation()
            .startModelRelation()
                .id(3).categoryId(666).type(ModelRelation.RelationType.SKU_MODEL)
                .startModel()
                    // sku model содержит определяющие параметры
                    .id(3).category(666).currentType(CommonModel.Source.SKU)
                    .param("param1").setOption(1).modificationSource(ModificationSource.OPERATOR_FILLED)
                    .param("param2").setOption(4).modificationSource(ModificationSource.OPERATOR_FILLED)
                    .picture("pic://2_1")
                    .picture("pic://2_2")
                    .startModelRelation()
                        .id(1).categoryId(666).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                    .endModelRelation()
                .endModel()
            .endModelRelation()
            .startModelRelation()
                .id(4).categoryId(666).type(ModelRelation.RelationType.SKU_MODEL)
                .startModel()
                    // sku model содержит определяющие параметры
                    .id(4).category(666).currentType(CommonModel.Source.SKU)
                    .param("param1").setOption(1).modificationSource(ModificationSource.OPERATOR_FILLED)
                    .param("param2").setOption(4).modificationSource(ModificationSource.OPERATOR_FILLED)
                    .picture("pic://2_1")
                    .picture("pic://2_2")
                    .startModelRelation()
                        .id(1).categoryId(666).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                    .endModelRelation()
                .endModel()
            .endModelRelation()
            .startModelRelation()
                .id(5).categoryId(666).type(ModelRelation.RelationType.SKU_MODEL)
                .startModel()
                    // sku model содержит определяющие параметры
                    .id(5).category(666).currentType(CommonModel.Source.SKU)
                    .param("param1").setOption(1).modificationSource(ModificationSource.OPERATOR_FILLED)
                    .param("param2").setOption(4).modificationSource(ModificationSource.OPERATOR_FILLED)
                    .picture("pic://2_1")
                    .picture("pic://2_2")
                    .startModelRelation()
                        .id(1).categoryId(666).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                    .endModelRelation()
                .endModel()
            .endModelRelation()
            .endModel();
    }

    @Override
    public void form() {
    }

    @Override
    protected void onModelLoaded(EditableModel editableModel) {
        super.onModelLoaded(editableModel);

        sku = editableModel.getModel().getRelations().stream()
            .filter(rel -> rel.getType() == ModelRelation.RelationType.SKU_MODEL)
            .map(ModelRelation::getModel)
            .findFirst()
            .orElse(null);
    }
}
