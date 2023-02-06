package ru.yandex.autotests.direct.cmd.data.commons;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.KeyValueBean;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

@KeyValueBean()
public class Strategy {

    @SerializeKey("avg_bid")
    @SerializedName("avg_bid")
    @JsonPath(responsePath = "avg_bid")
    private String avgBid;

    @SerializeKey("avg_cpa")
    @SerializedName("avg_cpa")
    @JsonPath(responsePath = "avg_cpa")
    private String avgCpa;

    @SerializeKey("avg_cpi")
    @SerializedName("avg_cpi")
    @JsonPath(responsePath = "avg_cpi")
    private Double avgCpi;

    @SerializeKey("bid")
    @JsonPath(responsePath = "bid")
    private String bid;

    @SerializeKey("goal_id")
    @SerializedName("goal_id")
    @JsonPath(responsePath = "goal_id")
    private String goalId;

    @SerializeKey("limit_clicks")
    @SerializedName("limit_clicks")
    @JsonPath(responsePath = "limit_clicks")
    private String limitClicks;

    @SerializeKey("name")
    @JsonPath(responsePath = "name")
    private String name;

    @SerializeKey("place")
    @JsonPath(responsePath = "place")
    private String place;

    @SerializeKey("profitability")
    @JsonPath(responsePath = "profitability")
    private Double profitability;

    @SerializeKey("reserve_return")
    @SerializedName("reserve_return")
    @JsonPath(responsePath = "reserve_return")
    private Integer reserveReturn;

    @SerializeKey("roi_coef")
    @SerializedName("roi_coef")
    @JsonPath(responsePath = "roi_coef")
    private Double roiCoef;

    @SerializeKey("sum")
    @JsonPath(responsePath = "sum")
    private String sum;

    @SerializeKey("filter_avg_bid")
    @SerializedName("filter_avg_bid")
    @JsonPath(responsePath = "filter_avg_bid")
    private Double filterAvgBid;

    @SerializeKey("filter_avg_cpa")
    @SerializedName("filter_avg_cpa")
    @JsonPath(responsePath = "filter_avg_cpa")
    private Double filterAvgCpa;

    @SerializeKey("pay_for_conversion")
    @SerializedName("pay_for_conversion")
    @JsonPath(responsePath = "pay_for_conversion")
    private Integer payForConversion;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Strategy withName(String name) {
        this.name = name;
        return this;
    }

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }

    public String getSum() {
        return sum;
    }

    public void setSum(String sum) {
        this.sum = sum;
    }

    public String getAvgBid() {
        return avgBid;
    }

    public void setAvgBid(String avgBid) {
        this.avgBid = avgBid;
    }

    public String getGoalId() {
        return goalId;
    }

    public void setGoalId(String goalId) {
        this.goalId = goalId;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getAvgCpa() {
        return avgCpa;
    }

    public void setAvgCpa(String avgCpa) {
        this.avgCpa = avgCpa;
    }

    public void setAvgCpi(Double avgCpi) {
        this.avgCpi = avgCpi;
    }

    public Double getAvgCpi() {
        return avgCpi;
    }


    public Double getProfitability() {
        return profitability;
    }

    public void setProfitability(Double profitability) {
        this.profitability = profitability;
    }

    public Integer getReserveReturn() {
        return reserveReturn;
    }

    public void setReserveReturn(Integer reserveReturn) {
        this.reserveReturn = reserveReturn;
    }

    public Double getRoiCoef() {
        return roiCoef;
    }

    public void setRoiCoef(Double roiCoef) {
        this.roiCoef = roiCoef;
    }

    public String getLimitClicks() {
        return limitClicks;
    }

    public void setLimitClicks(String limitClicks) {
        this.limitClicks = limitClicks;
    }

    public Double getFilterAvgBid() {
        return filterAvgBid;
    }

    public void setFilterAvgBid(Double filterAvgBid) {
        this.filterAvgBid = filterAvgBid;
    }

    public Double getFilterAvgCpa() {
        return filterAvgCpa;
    }

    public void setFilterAvgCpa(Double filterAvgCpa) {
        this.filterAvgCpa = filterAvgCpa;
    }

    public Strategy withAvgBid(String avgBid) {
        this.avgBid = avgBid;
        return this;
    }

    public Strategy withAvgCpa(String avgCpa) {
        this.avgCpa = avgCpa;
        return this;
    }

    public Strategy withAvgCpi(Double avgCpi) {
        this.avgCpi = avgCpi;
        return this;
    }

    public Strategy withBid(String bid) {
        this.bid = bid;
        return this;
    }

    public Strategy withGoalId(String goalId) {
        this.goalId = goalId;
        return this;
    }

    public Strategy withLimitClicks(String limitClicks) {
        this.limitClicks = limitClicks;
        return this;
    }

    public Strategy withPlace(String place) {
        this.place = place;
        return this;
    }

    public Strategy withProfitability(Double profitability) {
        this.profitability = profitability;
        return this;
    }

    public Strategy withReserveReturn(Integer reserveReturn) {
        this.reserveReturn = reserveReturn;
        return this;
    }

    public Strategy withRoiCoef(Double roiCoef) {
        this.roiCoef = roiCoef;
        return this;
    }

    public Strategy withSum(String sum) {
        this.sum = sum;
        return this;
    }

    public Strategy withFilterAvgBid(Double filterAvgBid) {
        this.filterAvgBid = filterAvgBid;
        return this;
    }

    public Strategy withFilterAvgCpa(Double filterAvgCpa) {
        this.filterAvgCpa = filterAvgCpa;
        return this;
    }

    public Integer getPayForConversion() {
        return payForConversion;
    }

    public void setPayForConversion(Integer payForConversion) {
        this.payForConversion = payForConversion;
    }

    public Strategy withPayForConversion(Integer payForConversion) {
        this.payForConversion = payForConversion;
        return this;
    }
}
