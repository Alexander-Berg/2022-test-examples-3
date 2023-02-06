package ru.yandex.autotests.direct.cmd.data.editcamp;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.Goal;
import ru.yandex.autotests.direct.cmd.data.commons.Wallet;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.CampaignV2;

import java.util.List;

public class EditCampResponse {

    @SerializedName("campaign")
    private CampaignV2 campaign;

    @SerializedName("wallet")
    private Wallet wallet;

    @SerializedName("goals_list")
    private List<Goal> goalsList;

    public CampaignV2 getCampaign() {
        return campaign;
    }

    public EditCampResponse withCampaign(CampaignV2 campaign) {
        this.campaign = campaign;
        return this;
    }

    public List<Goal> getGoalsList() {
        return goalsList;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public EditCampResponse withWallet(Wallet wallet) {
        this.wallet = wallet;
        return this;
    }
}
