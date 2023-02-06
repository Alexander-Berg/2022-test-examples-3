package ru.yandex.autotests.direct.cmd.data.wallet;

import com.google.gson.annotations.SerializedName;

import ru.yandex.autotests.direct.cmd.data.autopayment.AutopaySettings;

public class AutoPay {

    @SerializedName("autopay_settings")
    private AutopaySettings autopaySettings;

    public AutopaySettings getAutopaySettings() {
        return autopaySettings;
    }

    public AutoPay withAutopaySettings(AutopaySettings autopaySettings) {
        this.autopaySettings = autopaySettings;
        return this;
    }
}
