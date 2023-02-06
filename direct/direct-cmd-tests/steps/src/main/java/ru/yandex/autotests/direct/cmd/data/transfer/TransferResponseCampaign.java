package ru.yandex.autotests.direct.cmd.data.transfer;

import com.google.gson.annotations.SerializedName;

public class TransferResponseCampaign {

    @SerializedName("cid")
    private String cid;

    public String getCid() {
        return cid;
    }

    public TransferResponseCampaign withCid(String cid) {
        this.cid = cid;
        return this;
    }
}
