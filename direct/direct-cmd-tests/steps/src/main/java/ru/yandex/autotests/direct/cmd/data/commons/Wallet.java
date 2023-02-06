package ru.yandex.autotests.direct.cmd.data.commons;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import ru.yandex.autotests.direct.cmd.data.autopayment.AutopaySettings;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.DayBudget;

public class Wallet {

    @SerializedName("autopay_settings")
    private AutopaySettings autopaySettings;

    @SerializedName("day_budget")
    private DayBudget dayBudget;

    @SerializedName("self")
    private Self self;

    public AutopaySettings getAutopaySettings() {
        return autopaySettings;
    }

    public Wallet withAutopaySettings(AutopaySettings autopaySettings) {
        this.autopaySettings = autopaySettings;
        return this;
    }

    public DayBudget getDayBudget() {
        return dayBudget;
    }

    public Wallet withDayBudget(DayBudget dayBudget) {
        this.dayBudget = dayBudget;
        return this;
    }

    public Self getSelf() {
        return self;
    }

    public Wallet withSelf(Self self) {
        this.self = self;
        return this;
    }

    public class Self {

        @SerializedName("camp_stop_daily_budget_stats")
        private List<String> campStopDailyBudgetStats;

        @SerializedName("autopay_settings")
        private AutopaySettings autopaySettings;

        @SerializedName("day_budget")
        private DayBudget dayBudget;

        @SerializedName("sms_flags")
        private SmsFlags smsFlags;

        @SerializedName("email_notifications")
        private EmailNotifications emailNotifications;

        @SerializedName("enabled")
        private Integer enabled;

        public List<String> getCampStopDailyBudgetStats() {
            return campStopDailyBudgetStats;
        }

        public Self withCampStopDailyBudgetStats(List<String> campStopDailyBudgetStats) {
            this.campStopDailyBudgetStats = campStopDailyBudgetStats;
            return this;
        }

        public AutopaySettings getAutopaySettings() {
            return autopaySettings;
        }

        public Self withAutopaySettings(AutopaySettings autopaySettings) {
            this.autopaySettings = autopaySettings;
            return this;
        }

        public DayBudget getDayBudget() {
            return dayBudget;
        }

        public Self withDayBudget(DayBudget dayBudget) {
            this.dayBudget = dayBudget;
            return this;
        }

        public SmsFlags getSmsFlags() {
            return smsFlags;
        }

        public Self withSmsFlags(SmsFlags smsFlags) {
            this.smsFlags = smsFlags;
            return this;
        }

        public EmailNotifications getEmailNotifications() {
            return emailNotifications;
        }

        public Self withEmailNotifications(EmailNotifications emailNotifications) {
            this.emailNotifications = emailNotifications;
            return this;
        }

        public Integer getEnabled() {
            return enabled;
        }

        public Self withEnabled(Integer enabled) {
            this.enabled = enabled;
            return this;
        }
    }
}
