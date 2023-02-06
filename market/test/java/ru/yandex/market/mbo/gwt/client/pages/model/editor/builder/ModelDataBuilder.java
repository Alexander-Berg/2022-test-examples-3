package ru.yandex.market.mbo.gwt.client.pages.model.editor.builder;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.ModelData;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.SizeMeasureScaleInfo;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.ThinCategoryParam;
import ru.yandex.market.mbo.gwt.models.rules.AbstractModelRuleBuilder;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleSet;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author gilmulla
 */
public class ModelDataBuilder {

    public class InternalRuleBuilder
        extends AbstractModelRuleBuilder<InternalRuleBuilder> {

        public InternalRuleBuilder() {
            setBuilder(this);
        }

        public ModelDataBuilder endRuleSet() {
            ModelRuleSet ruleSet = buildRuleSet();
            ModelDataBuilder modelDataBuilder = ModelDataBuilder.this;
            if (ruleSet != null) {
                modelDataBuilder.modelData.addRuleSet(ruleSet);
            }
            return modelDataBuilder;
        }
    }

    ParametersBuilder<ModelDataBuilder> paramBuilder;
    ModelData modelData = new ModelData();
    private ModelFormBuilder<ModelDataBuilder> modelFormBuilder;

    public static ModelDataBuilder modelData() {
        return new ModelDataBuilder();
    }

    public ParametersBuilder<ModelDataBuilder> startParameters() {
        this.paramBuilder = ParametersBuilder.builder(params -> {
            for (ThinCategoryParam param : params) {
                if (param instanceof CategoryParam) {
                    modelData.addParam((CategoryParam) param);
                }
            }
            return ModelDataBuilder.this;
        });
        return paramBuilder;
    }

    public ModelDataBuilder clearParameters() {
        modelData.clearParams();
        return this;
    }

    public CommonModelBuilder<ModelDataBuilder> startModel() {
        if (modelData == null) {
            throw new IllegalStateException("Missing parameters definition");
        }
        return CommonModelBuilder.builder(model -> {
            this.modelData.setModel(model);
            return ModelDataBuilder.this;
        }).parameters(modelData.getParams());
    }

    public InternalRuleBuilder startRuleSet() {
        if (modelData == null) {
            throw new IllegalStateException("Missing parameters definition");
        }

        Map<String, ThinCategoryParam> paramsByName = new HashMap<>();
        Map<Long, ThinCategoryParam> paramsById = new HashMap<>();
        for (CategoryParam param : modelData.getParams()) {
            paramsById.put(param.getId(), param);
            paramsByName.put(param.getXslName(), param);
        }

        return new InternalRuleBuilder()
            .parameters(paramsByName).parametersById(paramsById);
    }

    public ModelFormBuilder<ModelDataBuilder> startForm() {
        modelFormBuilder = new ModelFormBuilder<>(form -> {
            modelData.setModelForm(form);
            return ModelDataBuilder.this;
        });
        return modelFormBuilder;
    }

    public ModelData getModelData() {
        return this.modelData;
    }

    public CommonModel getModel() {
        return this.modelData.getModel();
    }

    public Map<Long, CategoryParam> getParamMap() {
        return this.modelData.getParamMap();
    }

    public ModelDataBuilder tovarCategory(int id, int guruId) {
        modelData.setTovarCategory(new TovarCategory(id));
        modelData.getTovarCategory().setGuruCategoryId(guruId);
        return this;
    }

    public ModelVendorBuilder startVendor() {
        modelData.setVendor(new GlobalVendor());
        return new ModelVendorBuilder(this);
    }

    public ModelDataBuilder endVendor() {
        modelData.setVendor(new GlobalVendor());
        return this;
    }

    public ModelDataBuilder sizeMeasureScaleInfos(List<SizeMeasureScaleInfo> sizeMeasureScaleInfos) {
        modelData.setSizeMeasureScaleInfos(sizeMeasureScaleInfos);
        return this;
    }
}
