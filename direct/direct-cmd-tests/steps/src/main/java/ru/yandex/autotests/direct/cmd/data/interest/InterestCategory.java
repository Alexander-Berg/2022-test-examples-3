package ru.yandex.autotests.direct.cmd.data.interest;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class InterestCategory {
    @SerializedName("target_category_id")
    private Long targetCategoryId;

    @SerializedName("name")
    private String name;

    @SerializedName("available")
    private Long available;

    @SerializedName("childs")
    private List<InterestCategory> childs;

    public Long getAvailable() {
        return available;
    }

    public InterestCategory withAvailable(Long available) {
        this.available = available;
        return this;
    }

    public Long getTargetCategoryId() {
        return targetCategoryId;
    }

    public InterestCategory withTargetCategoryId(Long targetCategoryId) {
        this.targetCategoryId = targetCategoryId;
        return this;
    }

    public String getName() {
        return name;
    }

    public InterestCategory withName(String name) {
        this.name = name;
        return this;
    }

    public List<InterestCategory> getChilds() {
        return childs;
    }

    public InterestCategory withChilds(List<InterestCategory> childs) {
        this.childs = childs;
        return this;
    }
}
