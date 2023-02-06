package ru.yandex.market.mbo.export.modelstorage.pipe;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.http.ModelStorage;

import java.math.BigDecimal;
import java.util.function.Supplier;

/**
 * @author york
 * @since 14.09.2018
 */
public class ModelPipeContextTestGenerator {

    private static long idx = 1;

    private Supplier<Long> idSupplier = () -> idx++;
    private boolean forBlue;

    public ModelPipeContextTestGenerator() {
        this(false);
    }

    public ModelPipeContextTestGenerator(boolean forBlue) {
        this(() -> idx++, forBlue);
    }

    public ModelPipeContextTestGenerator(Supplier<Long> idSupplier, boolean forBlue) {
        this.idSupplier = idSupplier;
        this.forBlue = forBlue;
    }

    public ModelStorage.Model createModel(CommonModel.Source currentType, boolean published) {
        return createModel(currentType, published, false, false);
    }

    public ModelStorage.Model createModel(
        CommonModel.Source currentType,
        boolean published,
        boolean broken,
        boolean strictChecksRequired
    ) {
        long id = idSupplier.get();
        ModelStorage.Model.Builder result = ModelStorage.Model.newBuilder()
            .setCurrentType(currentType.name())
            .setId(id)
            .setBroken(broken)
            .setStrictChecksRequired(strictChecksRequired)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("model" + id));
        if (forBlue) {
            result.setBluePublished(published);
        } else {
            result.setPublished(published);
        }
        return result.build();
    }

    public ModelStorage.Model createGuru(boolean published, boolean broken, boolean strictChecksRequired) {
        return createModel(CommonModel.Source.GURU, published, broken, strictChecksRequired);
    }

    public ModelStorage.Model createGuru(boolean published) {
        return createModel(CommonModel.Source.GURU, published);
    }

    public ModelStorage.Model createSkuFor(CommonModel.Source currentType,
                                           ModelStorage.Model model, boolean published) {
        long id = idSupplier.get();
        ModelStorage.Model.Builder result = ModelStorage.Model.newBuilder()
            .setCurrentType(currentType.name())
            .setId(id)
            .setPublished(forBlue ? false : published)
            .setBluePublished(forBlue ? published : false)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("sku" + id))
            .addRelations(
                ModelStorage.Relation.newBuilder()
                    .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                    .setId(model.getId()));
        if (forBlue) {
            result.setBluePublished(published);
        } else {
            result.setPublished(published);
        }
        return result.build();
    }

    public ModelStorage.Model createSkuFor(ModelStorage.Model model, boolean published) {
        return createSkuFor(CommonModel.Source.SKU, model, published);
    }

    public ModelStorage.Model createModificationFor(ModelStorage.Model model, boolean published) {
        long id = idSupplier.get();
        ModelStorage.Model.Builder result = ModelStorage.Model.newBuilder()
            .setCurrentType(CommonModel.Source.GURU.name())
            .setId(id)
            .setPublished(forBlue ? true : published)
            .setBluePublished(forBlue ? published : true)
            .addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ru")
                .setValue("modification" + id))
            .setParentId(model.getId());
        if (forBlue) {
            result.setBluePublished(published);
        } else {
            result.setPublished(published);
        }
        return result.build();
    }


    public ModelStorage.ParameterValue enumParam(String xsl, Long id, int optionId) {
        return ModelStorage.ParameterValue.newBuilder()
            .setParamId(id)
            .setXslName(xsl)
            .setValueType(MboParameters.ValueType.ENUM)
            .setOptionId(optionId)
            .build();
    }

    public ModelStorage.ParameterValue numericParam(String xsl, Long id, BigDecimal value) {
        return ModelStorage.ParameterValue.newBuilder()
            .setParamId(id)
            .setXslName(xsl)
            .setValueType(MboParameters.ValueType.NUMERIC)
            .setNumericValue(value.toPlainString())
            .build();
    }
}
