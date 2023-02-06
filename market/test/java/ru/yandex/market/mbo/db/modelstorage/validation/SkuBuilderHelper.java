package ru.yandex.market.mbo.db.modelstorage.validation;

import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;

import java.util.function.Function;

/**
 * @author danfertev
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SkuBuilderHelper {
    public static final long PARENT_MODEL_ID = 100500L;
    public static final long CATEGORY_ID = 100L;
    public static final long VENDOR_ID = 1000L;
    public static final long ANOTHER_CATEGORY_ID = 101L;
    public static final long ANOTHER_VENDOR_ID = 1001L;

    private SkuBuilderHelper() {
    }

    public static CommonModelBuilder<CommonModel> getGuruBuilder() {
        return ParametersBuilder
            .startParameters(p -> CommonModelBuilder.builder(Function.identity()).parameters(p))
                .startParameter()
                    .id(1L).xsl("param1").type(Param.Type.NUMERIC)
                .endParameter()
                .startParameter()
                    .id(2L).xsl("param2").type(Param.Type.STRING)
                .endParameter()
                .startParameter()
                    .id(3L).xsl("param3").type(Param.Type.ENUM)
                .endParameter()
                .startParameter()
                    .id(4L).xsl("param4").type(Param.Type.NUMERIC_ENUM)
                .endParameter()
                .startParameter()
                    .id(5L).xsl("IsSku").type(Param.Type.BOOLEAN)
                    .option(1L, "true")
                    .option(2L, "false")
                .endParameter()
            .endParameters()
            .id(PARENT_MODEL_ID).category(CATEGORY_ID).vendorId(VENDOR_ID)
            .currentType(CommonModel.Source.GURU).source(CommonModel.Source.GURU);
    }

    public static CommonModel getGuru() {
        return getGuruBuilder().getModel();
    }

    public static CommonModel getSku(Long parentId) {
        return getSkuBuilder(parentId).getModel();
    }

    public static CommonModelBuilder<CommonModel> getParentWithRelationBuilder(long... children) {
        CommonModelBuilder<CommonModel> builder = getGuruBuilder();

        for (long id : children) {
            builder
                .startModelRelation()
                .id(id).categoryId(CATEGORY_ID).type(ModelRelation.RelationType.SKU_MODEL)
                .endModelRelation();
        }

        return builder;
    }

    public static CommonModel getParentWithRelation(long... children) {
        return getParentWithRelationBuilder(children).endModel();
    }

    public static CommonModelBuilder<CommonModel> getModificationBuilder() {
        return getGuruBuilder().parentModelId(PARENT_MODEL_ID);
    }

    public static CommonModelBuilder<CommonModel> getSkuBuilder(long parentId) {
        return ParametersBuilder
            .startParameters(p -> CommonModelBuilder.builder(Function.identity()).parameters(p))
                .startParameter()
                    .id(1L).xsl("param1").type(Param.Type.NUMERIC).skuParameterMode(SkuParameterMode.SKU_DEFINING)
                .endParameter()
                .startParameter()
                    .id(2L).xsl("param2").type(Param.Type.STRING).skuParameterMode(SkuParameterMode.SKU_DEFINING)
                .endParameter()
                .startParameter()
                    .id(3L).xsl("param3").type(Param.Type.ENUM).skuParameterMode(SkuParameterMode.SKU_DEFINING)
                .endParameter()
                .startParameter()
                    .id(4L).xsl("param4").type(Param.Type.NUMERIC_ENUM).skuParameterMode(SkuParameterMode.SKU_DEFINING)
                .endParameter()
                .startParameter()
                    .id(5L).xsl("IsSku").type(Param.Type.BOOLEAN)
                    .option(1L, "true")
                    .option(2L, "false")
                .endParameter()
            .endParameters()
            .category(CATEGORY_ID).vendorId(VENDOR_ID).currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
                .id(parentId).categoryId(CATEGORY_ID).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation();
    }

    public static CommonModelBuilder<CommonModel> getDefaultSkuBuilder() {
        return getSkuBuilder(PARENT_MODEL_ID);
    }

    public static CommonModelBuilder<CommonModel> getSkuBuilderWithoutDefiningParams() {
        return ParametersBuilder
            .startParameters(p -> CommonModelBuilder.builder(Function.identity()).parameters(p))
                .startParameter()
                    .id(1L).xsl("param1").type(Param.Type.NUMERIC).skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL)
                .endParameter()
                .startParameter()
                    .id(2L).xsl("param2").type(Param.Type.NUMERIC).skuParameterMode(SkuParameterMode.SKU_NONE)
                .endParameter()
            .endParameters()
            .category(CATEGORY_ID).vendorId(VENDOR_ID).currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
                .id(PARENT_MODEL_ID).categoryId(CATEGORY_ID).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation();
    }

    public static CommonModelBuilder<CommonModel> getNoParamSkuBuilder() {
        return CommonModelBuilder.builder(Function.identity())
            .category(CATEGORY_ID).vendorId(VENDOR_ID).currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
                .id(PARENT_MODEL_ID).categoryId(CATEGORY_ID).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation();
    }

    public static ModelValidationError createSkuDuplicateParamError(long modelId) {
        return new ModelValidationError(
            modelId,
            ModelValidationError.ErrorType.INVALID_PARAMETER_VALUE,
            ModelValidationError.ErrorSubtype.DUPLICATE_SKU_PARAM_VALUE,
            true)
            .addLocalizedMessagePattern("Значения определяющих параметров дублируются среди SKU.");
    }
}
