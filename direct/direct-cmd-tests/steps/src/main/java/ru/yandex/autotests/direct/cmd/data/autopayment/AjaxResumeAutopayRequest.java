package ru.yandex.autotests.direct.cmd.data.autopayment;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class AjaxResumeAutopayRequest extends BasicDirectRequest {

    @SerializeKey("wallet_cid")
    private String walletCid;

    public String getWalletCid() {
        return walletCid;
    }

    public AjaxResumeAutopayRequest withWalletCid(String walletCid) {
        this.walletCid = walletCid;
        return this;
    }
}
