package ru.yandex.common.util.xml.parser.stackable;

import java.util.List;

public class Category {
    public int id;
    public String name;
    public String type;
    public List<Model> models;
    public List<Integer> modelIds;
    public int modelsCount;

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof Category)) return false;

        Category category = (Category) o;

        if(id != category.id) return false;
        if(modelsCount != category.modelsCount) return false;
        if(modelIds != null ? !modelIds.equals(category.modelIds) : category.modelIds != null) return false;
        if(models != null ? !models.equals(category.models) : category.models != null) return false;
        if(name != null ? !name.equals(category.name) : category.name != null) return false;
        if(type != null ? !type.equals(category.type) : category.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (models != null ? models.hashCode() : 0);
        result = 31 * result + (modelIds != null ? modelIds.hashCode() : 0);
        result = 31 * result + modelsCount;
        return result;
    }

    @Override
    public String toString() {
        return "Category{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", models=" + models +
            ", modelIds=" + modelIds +
            ", modelsCount=" + modelsCount +
            "} " + super.toString();
    }
}
