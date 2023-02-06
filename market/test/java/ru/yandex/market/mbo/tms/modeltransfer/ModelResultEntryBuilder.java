package ru.yandex.market.mbo.tms.modeltransfer;

import ru.yandex.market.mbo.gwt.models.transfer.step.ModelResultEntry;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author dmserebr
 * @date 02.10.18
 */
public class ModelResultEntryBuilder {
    private ModelResultEntry result;

    public static ModelResultEntryBuilder newBuilder(long modelId, String modelName) {
        ModelResultEntryBuilder builder = new ModelResultEntryBuilder();
        builder.result = new ModelResultEntry();
        builder.result.setModelId(modelId);
        builder.result.setModelName(modelName);
        return builder;
    }

    public ModelResultEntryBuilder modelType(ModelResultEntry.ModelType modelType) {
        this.result.setModelType(modelType);
        return this;
    }

    public ModelResultEntryBuilder sourceCategory(long sourceCategoryId) {
        this.result.setSourceCategoryId(sourceCategoryId);
        return this;
    }

    public ModelResultEntryBuilder targetCategory(long targetCategoryId) {
        this.result.setTargetCategoryId(targetCategoryId);
        return this;
    }

    public ModelResultEntryBuilder status(ModelResultEntry.Status status) {
        this.result.setStatus(status);
        return this;
    }

    public ModelResultEntryBuilder statusMessage(String status) {
        this.result.setStatusMessage(status);
        return this;
    }

    public ModelResultEntryBuilder validationErrors(String... modelValidationErrors) {
        this.result.setModelValidationErrors(Arrays.stream(modelValidationErrors).collect(Collectors.toList()));
        return this;
    }

    public ModelResultEntryBuilder skuIds(Long... skuIds) {
        this.result.setSkuIds(Arrays.stream(skuIds).collect(Collectors.toList()));
        return this;
    }

    public ModelResultEntry build() {
        return result;
    }
}
