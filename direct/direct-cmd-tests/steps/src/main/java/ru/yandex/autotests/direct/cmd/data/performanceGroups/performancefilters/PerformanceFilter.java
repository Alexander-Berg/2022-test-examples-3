package ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.group.Condition;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.httpclientlite.core.support.gson.NullValueWrapper;

import java.util.List;

public class PerformanceFilter {

    @SerializedName("perf_filter_id")
    private String perfFilterId;

    @SerializedName("filter_name")
    private String filterName;

    @SerializedName("target_funnel")
    private String targetFunnel;

    @SerializedName("available")
    private String available;

    @SerializedName("from_tab")
    private String fromTab;

    @SerializedName("retargeting")
    private NullValueWrapper<RetargetingCondition> retargeting;

    @SerializedName("condition")
    private List<Condition> conditions;

    @SerializedName("is_suspended")
    private String isSuspended;

    @SerializedName("price_cpc")
    private String priceCpc;

    @SerializedName("price_cpa")
    private String priceCpa;

    @SerializedName("autobudgetPriority")
    private String autobudgetPriority;

    @SerializedName("status_bs_synced")
    private String statusBsSynced;

    public String getPerfFilterId() {
        return perfFilterId;
    }

    public void setPerfFilterId(String perfFilterId) {
        this.perfFilterId = perfFilterId;
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public String getTargetFunnel() {
        return targetFunnel;
    }

    public void setTargetFunnel(String targetFunnel) {
        this.targetFunnel = targetFunnel;
    }

    public PerformanceFilter withTargetFunnel(String targetFunnel) {
        this.targetFunnel = targetFunnel;
        return this;
    }

    public String getAvailable() {
        return available;
    }

    public void setAvailable(String available) {
        this.available = available;
    }

    public String getFromTab() {
        return fromTab;
    }

    public void setFromTab(String fromTab) {
        this.fromTab = fromTab;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public PerformanceFilter withPerfFilterId(String perfFilterId) {
        this.perfFilterId = perfFilterId;
        return this;
    }

    public NullValueWrapper<RetargetingCondition> getRetargeting() {
        return retargeting;
    }

    public void setRetargeting(NullValueWrapper<RetargetingCondition> retargeting) {
        this.retargeting = retargeting;
    }

    public PerformanceFilter withRetargeting(RetargetingCondition retargeting) {
        this.retargeting = NullValueWrapper.of(retargeting);
        return this;
    }

    public String getIsSuspended() {
        return isSuspended;
    }

    public PerformanceFilter withIsSuspended(String isSuspended) {
        this.isSuspended = isSuspended;
        return this;
    }

    public PerformanceFilter withFilterName(String filterName) {
        this.filterName = filterName;
        return this;
    }

    public PerformanceFilter withAvailable(String available) {
        this.available = available;
        return this;
    }

    public PerformanceFilter withFromTab(String fromTab) {
        this.fromTab = fromTab;
        return this;
    }

    public PerformanceFilter withRetargeting(NullValueWrapper<RetargetingCondition> retargeting) {
        this.retargeting = retargeting;
        return this;
    }

    public PerformanceFilter withConditions(List<Condition> conditions) {
        this.conditions = conditions;
        return this;
    }

    public String getPriceCpc() {
        return priceCpc;
    }

    public PerformanceFilter withPriceCpc(String priceCpc) {
        this.priceCpc = priceCpc;
        return this;
    }

    public String getPriceCpa() {
        return priceCpa;
    }

    public PerformanceFilter withPriceCpa(String priceCpa) {
        this.priceCpa = priceCpa;
        return this;
    }

    public String getAutobudgetPriority() {
        return autobudgetPriority;
    }

    public PerformanceFilter withAutobudgetPriority(String autobudgetPriority) {
        this.autobudgetPriority = autobudgetPriority;
        return this;
    }

    public String getStatusBsSynced() {
        return statusBsSynced;
    }

    public PerformanceFilter withStatusBsSynced(String statusBsSynced) {
        this.statusBsSynced = statusBsSynced;
        return this;
    }
}
