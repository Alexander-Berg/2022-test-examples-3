package ru.yandex.autotests.direct.cmd.data.interest;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.irt.testutils.json.JsonUtils;

public class TargetInterests {
    @SerializeKey("cid")
    @SerializedName("cid")
    private Long cid;

    @SerializeKey("modtime")
    @SerializedName("modtime")
    private String modTime;

    @SerializeKey("statusBsSynced")
    @SerializedName("statusBsSynced")
    private String statusBsSynced;

    @SerializeKey("ret_id")
    @SerializedName("ret_id")
    private Integer retId;

    @SerializeKey("target_category_id")
    @SerializedName("target_category_id")
    private Long targetCategoryId;

    @SerializeKey("price_context")
    @SerializedName("price_context")
    private Double priceContext;

    @SerializeKey("autobudgetPriority")
    @SerializedName("autobudgetPriority")
    private Integer autobudgetPriority;

    @SerializeKey("is_suspended")
    @SerializedName("is_suspended")
    private Integer isSuspended;

    @SerializeKey("category_name")
    @SerializedName("category_name")
    private String categoryName;

    public String getStatusBsSynced() {
        return statusBsSynced;
    }

    public TargetInterests withStatusBsSynced(String statusBsSynced) {
        this.statusBsSynced = statusBsSynced;
        return this;
    }

    public Long getCid() {
        return cid;
    }

    public TargetInterests withCid(Long cid) {
        this.cid = cid;
        return this;
    }

    public String getModTime() {
        return modTime;
    }

    public TargetInterests withModTime(String modTime) {
        this.modTime = modTime;
        return this;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public TargetInterests withCategoryName(String categoryName) {
        this.categoryName = categoryName;
        return this;
    }

    public Integer getIsSuspended() {
        return isSuspended;
    }

    public TargetInterests withIsSuspended(Integer isSuspended) {
        this.isSuspended = isSuspended;
        return this;
    }

    public Integer getAutobudgetPriority() {
        return autobudgetPriority;
    }

    public TargetInterests withAutobudgetPriority(Integer autobudgetPriority) {
        this.autobudgetPriority = autobudgetPriority;
        return this;
    }

    public Double getPriceContext() {
        return priceContext;
    }

    public TargetInterests withPriceContext(Double priceContext) {
        this.priceContext = priceContext;
        return this;
    }

    public Long getTargetCategoryId() {
        return targetCategoryId;
    }

    public TargetInterests withTargetCategoryId(Long targetCategoryId) {
        this.targetCategoryId = targetCategoryId;
        return this;
    }

    public Integer getRetId() {
        return retId;
    }

    public TargetInterests withRetId(Integer retId) {
        this.retId = retId;
        return this;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(this);
    }
}
