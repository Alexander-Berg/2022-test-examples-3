package ru.yandex.autotests.direct.cmd.data.creatives;

import com.google.gson.annotations.SerializedName;

public class ShortCreative {

    @SerializedName("id")
    private Long id;

    @SerializedName("business_type")
    private String businessType;

    @SerializedName("creative_group_id")
    private String creativeGroupId;

    public Long getId() {
        return id;
    }

    public ShortCreative withId(Long id) {
        this.id = id;
        return this;
    }

    public String getBusinessType() {
        return businessType;
    }

    public ShortCreative withBusinessType(String businessType) {
        this.businessType = businessType;
        return this;
    }

    public String getCreativeGroupId() {
        return creativeGroupId;
    }

    public ShortCreative withCreativeGroupId(String creativeGroupId) {
        this.creativeGroupId = creativeGroupId;
        return this;
    }
}
