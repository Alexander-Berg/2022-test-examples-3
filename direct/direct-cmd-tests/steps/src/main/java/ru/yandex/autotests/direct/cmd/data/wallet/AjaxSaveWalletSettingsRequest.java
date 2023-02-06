package ru.yandex.autotests.direct.cmd.data.wallet;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class AjaxSaveWalletSettingsRequest extends BasicDirectRequest {

    @SerializeKey("cid")
    private Long cid;

    @SerializeKey("email")
    private String email;

    @SerializeKey("money_warning_value")
    private Integer moneyWarningValue;

    @SerializeKey("sms_time_hour_from")
    private String smsTimeHourFrom;

    @SerializeKey("sms_time_hour_to")
    private String smsTimeHourTo;

    @SerializeKey("sms_time_min_from")
    private String smsTimeMinFrom;

    @SerializeKey("sms_time_min_to")
    private String smsTimeMinTo;

    @SerializeKey("paused_by_day_budget_sms")
    private Integer pausedByDayBudgetSms;

    @SerializeKey("email_notify_paused_by_day_budget")
    private Integer emailNotifyPausedByDayBudgetEmail;

    public Long getCid() {
        return cid;
    }

    public AjaxSaveWalletSettingsRequest withCid(Long cid) {
        this.cid = cid;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public AjaxSaveWalletSettingsRequest withEmail(String email) {
        this.email = email;
        return this;
    }

    public Integer getMoneyWarningValue() {
        return moneyWarningValue;
    }

    public AjaxSaveWalletSettingsRequest withMoneyWarningValue(Integer moneyWarningValue) {
        this.moneyWarningValue = moneyWarningValue;
        return this;
    }

    public String getSmsTimeHourFrom() {
        return smsTimeHourFrom;
    }

    public AjaxSaveWalletSettingsRequest withSmsTimeHourFrom(String smsTimeHourFrom) {
        this.smsTimeHourFrom = smsTimeHourFrom;
        return this;
    }

    public String getSmsTimeHourTo() {
        return smsTimeHourTo;
    }

    public AjaxSaveWalletSettingsRequest withSmsTimeHourTo(String smsTimeHourTo) {
        this.smsTimeHourTo = smsTimeHourTo;
        return this;
    }

    public String getSmsTimeMinFrom() {
        return smsTimeMinFrom;
    }

    public AjaxSaveWalletSettingsRequest withSmsTimeMinFrom(String smsTimeMinFrom) {
        this.smsTimeMinFrom = smsTimeMinFrom;
        return this;
    }

    public String getSmsTimeMinTo() {
        return smsTimeMinTo;
    }

    public AjaxSaveWalletSettingsRequest withSmsTimeMinTo(String smsTimeMinTo) {
        this.smsTimeMinTo = smsTimeMinTo;
        return this;
    }

    public Integer getPausedByDayBudgetSms() {
        return pausedByDayBudgetSms;
    }

    public AjaxSaveWalletSettingsRequest withPausedByDayBudgetSms(Integer pausedByDayBudgetSms) {
        this.pausedByDayBudgetSms = pausedByDayBudgetSms;
        return this;
    }

    public Integer getEmailNotifyPausedByDayBudgetEmail() {
        return emailNotifyPausedByDayBudgetEmail;
    }

    public AjaxSaveWalletSettingsRequest withEmailNotifyPausedByDayBudgetEmail(
            Integer emailNotifyPausedByDayBudgetEmail)
    {
        this.emailNotifyPausedByDayBudgetEmail = emailNotifyPausedByDayBudgetEmail;
        return this;
    }
}
