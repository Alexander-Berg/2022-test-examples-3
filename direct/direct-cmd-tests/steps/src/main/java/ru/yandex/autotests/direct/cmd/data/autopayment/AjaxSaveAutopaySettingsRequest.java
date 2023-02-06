package ru.yandex.autotests.direct.cmd.data.autopayment;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.WalletCampaignsAutopayMode;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToJsonSerializer;

public class AjaxSaveAutopaySettingsRequest extends BasicDirectRequest {

    public static AjaxSaveAutopaySettingsRequest getDisableAutopayRequest(String walletId) {
        return new AjaxSaveAutopaySettingsRequest()
                .withCid(String.valueOf(walletId))
                .withJsonAutopay(new AutoPayModel()
                        .withAutopayMode(WalletCampaignsAutopayMode.none.getLiteral()));
    }

    @SerializeKey("cid")
    @SerializedName("cid")
    private String cid;

    @SerializeKey("json_autopay")
    @SerializedName("json_autopay")
    @SerializeBy(ValueToJsonSerializer.class)
    private AutoPayModel jsonAutopay;

    public String getCid() {
        return cid;
    }

    public AjaxSaveAutopaySettingsRequest withCid(String cid) {
        this.cid = cid;
        return this;
    }

    public AutoPayModel getJsonAutopay() {
        return jsonAutopay;
    }

    public AjaxSaveAutopaySettingsRequest withJsonAutopay(AutoPayModel jsonAutopay) {
        this.jsonAutopay = jsonAutopay;
        return this;
    }

    public AjaxSaveAutopaySettingsRequest withPaymethodId(String paymethodId) {
        getJsonAutopay().withPaymethodId(paymethodId);
        return this;
    }
}
