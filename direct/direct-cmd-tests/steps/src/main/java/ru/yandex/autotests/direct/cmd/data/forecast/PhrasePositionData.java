package ru.yandex.autotests.direct.cmd.data.forecast;

import com.google.gson.annotations.SerializedName;

/**
 * Created by aleran on 29.09.2015.
 */
public class PhrasePositionData {

    @SerializedName("clicks")
    private Integer clicks;

    @SerializedName("bid_price")
    private Float bidPrice;

    @SerializedName("amnesty_price")
    private Float amnestyPrice;

    @SerializedName("sum")
    private String sum;

    @SerializedName("ctr")
    private String ctr;

    public Integer getClicks() {
        return clicks;
    }

    public void setClicks(Integer clicks) {
        this.clicks = clicks;
    }

    public Float getBidPrice() {
        return bidPrice;
    }

    public void setBidPrice(Float bidPrice) {
        this.bidPrice = bidPrice;
    }

    public Float getAmnestyPrice() {
        return amnestyPrice;
    }

    public void setAmnestyPrice(Float amnestyPrice) {
        this.amnestyPrice = amnestyPrice;
    }

    public String getSum() {
        return sum;
    }

    public void setSum(String sum) {
        this.sum = sum;
    }

    public String getCtr() {
        return ctr;
    }

    public void setCtr(String ctr) {
        this.ctr = ctr;
    }
}
