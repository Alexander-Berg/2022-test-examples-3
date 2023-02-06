package ru.yandex.autotests.direct.cmd.data.interest;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.irt.testutils.json.JsonUtils;

public class RelevanceMatch {
    @SerializedName("bid_id")
    private Long bidId;

    @SerializedName("price")
    private Double price;

    @SerializedName("autobudgetPriority")
    private Integer autobudgetPriority;

    @SerializedName("is_suspended")
    private String isSuspended;


    public Long getBidId() {
        return bidId;
    }

    public RelevanceMatch withBidId(Long bidId) {
        this.bidId = bidId;
        return this;
    }

    public Double getPrice() {
        return price;
    }

    public RelevanceMatch withPrice(Double price) {
        this.price = price;
        return this;
    }

    public Integer getAutobudgetPriority() {
        return autobudgetPriority;
    }

    public RelevanceMatch withAutobudgetPriority(Integer autobudgetPriority) {
        this.autobudgetPriority = autobudgetPriority;
        return this;
    }

    public String getIsSuspended() {
        return isSuspended;
    }

    public RelevanceMatch withIsSuspended(String isSuspended) {
        this.isSuspended = isSuspended;
        return this;
    }


    @Override
    public String toString() {
        return JsonUtils.toString(this);
    }
}
