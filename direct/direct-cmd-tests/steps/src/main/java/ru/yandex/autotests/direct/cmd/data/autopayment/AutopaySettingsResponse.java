package ru.yandex.autotests.direct.cmd.data.autopayment;

import com.google.gson.annotations.SerializedName;
import org.jmock.auto.Auto;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;

import java.util.List;

public class AutopaySettingsResponse extends ErrorResponse {

    public static AutopaySettingsResponse fromAutopaySettingsRequest(AjaxSaveAutopaySettingsRequest request) {
        return new AutopaySettingsResponse().withAutopaySettings(
                new AutopaySettings()
                        .withWalletCid(request.getCid())
                        .withPayMethodId(request.getJsonAutopay().getPaymethodId())
                        .withPayMethodType(request.getJsonAutopay().getPaymethodType())
                        .withPaymentSum(request.getJsonAutopay().getPaymentSum())
                        .withRemainingSum(request.getJsonAutopay().getRemainingSum()));
    }

    @SerializedName("autopay_settings")
    private AutopaySettings autopaySettings;

    public AutopaySettings getAutopaySettings() {
        return autopaySettings;
    }

    public AutopaySettingsResponse withAutopaySettings(AutopaySettings autopaySettings) {
        this.autopaySettings = autopaySettings;
        return this;
    }


}
