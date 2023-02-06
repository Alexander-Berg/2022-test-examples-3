package ru.yandex.autotests.direct.cmd.data.commons.group;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Ставка на условие ретаргетинга в конкретной группе
 * В БД представлена в таблице bids_retargeting
 */
public class Retargeting {

    @SerializedName("ret_id")
    private Long retId;

    @SerializedName("ret_cond_id")
    private Long retCondId;

    @SerializedName("price_context")
    private String priceContext;

    @SerializedName("is_suspended")
    private String isSuspended;

    //Поля добавлены для поддержки CPM баннера
    @SerializedName("condition_name")
    private String conditionName;

    @SerializedName("description")
    private String description;

    @SerializedName("type")
    private String type;

    @SerializedName("groups")
    private List<CpmRetargetingGroup> groups;

    public Long getRetId() {
        return retId;
    }

    public Retargeting withRetId(Long retId) {
        this.retId = retId;
        return this;
    }

    public Long getRetCondId() {
        return retCondId;
    }

    public Retargeting withRetCondId(Long retCondId) {
        this.retCondId = retCondId;
        return this;
    }

    public String getPriceContext() {
        return priceContext;
    }

    public void setRetId(Long retId) {
        this.retId = retId;
    }

    public void setRetCondId(Long retCondId) {
        this.retCondId = retCondId;
    }

    public void setPriceContext(String priceContext) {
        this.priceContext = priceContext;
    }

    public void setIsSuspended(String isSuspended) {
        this.isSuspended = isSuspended;
    }

    public String getConditionName() {
        return conditionName;
    }

    public void setConditionName(String conditionName) {
        this.conditionName = conditionName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<CpmRetargetingGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<CpmRetargetingGroup> groups) {
        this.groups = groups;
    }

    public Retargeting withPriceContext(String priceContext) {
        this.priceContext = priceContext;
        return this;
    }

    public String getIsSuspended() {
        return isSuspended;
    }

    public Retargeting withIsSuspended(String isSuspended) {
        this.isSuspended = isSuspended;
        return this;
    }

    public Retargeting withConditionName(String conditionName) {
        this.conditionName = conditionName;
        return this;
    }

    public Retargeting withDescription(String description) {
        this.description = description;
        return this;
    }

    public Retargeting withType(String type) {
        this.type = type;
        return this;
    }

    public Retargeting withGroups(List<CpmRetargetingGroup> groups) {
        this.groups = groups;
        return this;
    }
}
