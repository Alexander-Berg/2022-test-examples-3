package ru.yandex.autotests.direct.cmd.data.commons.phrase;

import com.google.gson.annotations.SerializedName;

public class Bid {

    @SerializedName("bid_price")
    private Double bidPrice;
    @SerializedName("amnesty_price")
    private Double amnestyPrice;

    public Double getBidPrice() {
        return bidPrice;
    }

    public Bid withBidPrice(Double bidPrice) {
        this.bidPrice = bidPrice;
        return this;
    }

    public Double getAmnestyPrice() {
        return amnestyPrice;
    }

    public Bid withAmnestyPrice(Double amnestyPrice) {
        this.amnestyPrice = amnestyPrice;
        return this;
    }
}
