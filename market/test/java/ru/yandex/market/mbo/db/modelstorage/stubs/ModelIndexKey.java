package ru.yandex.market.mbo.db.modelstorage.stubs;

import java.util.Objects;

/**
 * @author shadoff
 * created on 2019-12-12
 */
public class ModelIndexKey {
    private Long modelId;
    private Long categoryId;

    public ModelIndexKey(long modelId, long categoryId) {
        this.modelId = modelId;
        this.categoryId = categoryId;
    }

    public static ModelIndexKey from(long modelId, long categoryId) {
        return new ModelIndexKey(modelId, categoryId);
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ModelIndexKey that = (ModelIndexKey) o;
        return modelId.equals(that.modelId) &&
            categoryId.equals(that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelId, categoryId);
    }

    @Override
    public String toString() {
        return "ModelIndexKey{" +
            "modelId=" + modelId +
            ", categoryId=" + categoryId +
            '}';
    }
}
