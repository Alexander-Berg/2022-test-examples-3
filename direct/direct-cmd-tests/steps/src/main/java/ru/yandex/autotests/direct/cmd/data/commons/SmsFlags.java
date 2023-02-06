package ru.yandex.autotests.direct.cmd.data.commons;

import com.google.gson.annotations.SerializedName;

public class SmsFlags {

    @SerializedName("active_orders_money_out_sms")
    private String activeOrdersMoneyOutSms;

    @SerializedName("active_orders_money_warning_sms")
    private String activeOrdersMoneyWarningSms;

    @SerializedName("moderate_result_sms")
    private String moderateResultSms;

    @SerializedName("notify_order_money_in_sms")
    private String notifyOrderMoneyInSms;

    @SerializedName("paused_by_day_budget_sms")
    private Integer pausedByDayBudgetSms;

    public String getActiveOrdersMoneyOutSms() {
        return activeOrdersMoneyOutSms;
    }

    public void setActiveOrdersMoneyOutSms(String activeOrdersMoneyOutSms) {
        this.activeOrdersMoneyOutSms = activeOrdersMoneyOutSms;
    }

    public String getActiveOrdersMoneyWarningSms() {
        return activeOrdersMoneyWarningSms;
    }

    public void setActiveOrdersMoneyWarningSms(String activeOrdersMoneyWarningSms) {
        this.activeOrdersMoneyWarningSms = activeOrdersMoneyWarningSms;
    }

    public String getModerateResultSms() {
        return moderateResultSms;
    }

    public void setModerateResultSms(String moderateResultSms) {
        this.moderateResultSms = moderateResultSms;
    }

    public String getNotifyOrderMoneyInSms() {
        return notifyOrderMoneyInSms;
    }

    public void setNotifyOrderMoneyInSms(String notifyOrderMoneyInSms) {
        this.notifyOrderMoneyInSms = notifyOrderMoneyInSms;
    }

    public Integer getPausedByDayBudgetSms() {
        return pausedByDayBudgetSms;
    }

    public void setPausedByDayBudgetSms(Integer pausedByDayBudgetSms) {
        this.pausedByDayBudgetSms = pausedByDayBudgetSms;
    }

    public SmsFlags withPausedByDayBudgetSms(Integer pausedByDayBudgetSms) {
        this.pausedByDayBudgetSms = pausedByDayBudgetSms;
        return this;
    }
}
