package ru.yandex.autotests.direct.cmd.data.campaigns;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.Wallet;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.Campaign;

import java.util.List;

public class ShowCampsResponse {

    //TODO добавить список кампаний

    @SerializedName("campaigns")
    private List<Campaign> campaigns;

    @SerializedName("wallet")
    private Wallet wallet;

    public List<Campaign> getCampaigns() {
        return campaigns;
    }

    public ShowCampsResponse withCampaigns(List<Campaign> campaigns) {
        this.campaigns = campaigns;
        return this;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public ShowCampsResponse withWallet(Wallet wallet) {
        this.wallet = wallet;
        return this;
    }
}
