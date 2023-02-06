package ru.yandex.autotests.direct.cmd.data.feeds;

import com.google.gson.annotations.SerializedName;

public class Category {

    @SerializedName("name")
    private String name;

    @SerializedName("parent_category_id")
    private String parentCategoryId;

    @SerializedName("category_id")
    private String categoryId;

    public String getName() {
        return name;
    }

    public Category withName(String name) {
        this.name = name;
        return this;
    }

    public String getParentCategoryId() {
        return parentCategoryId;
    }

    public Category withParentCategoryId(String parentCategoryId) {
        this.parentCategoryId = parentCategoryId;
        return this;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public Category withCategoryId(String categoryId) {
        this.categoryId = categoryId;
        return this;
    }
}
