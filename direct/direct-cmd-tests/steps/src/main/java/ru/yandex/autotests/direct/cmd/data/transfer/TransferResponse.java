package ru.yandex.autotests.direct.cmd.data.transfer;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.Campaign;

import java.util.List;
import java.util.stream.Collectors;

public class TransferResponse {

    @SerializedName("campaigns_from")
    private List<TransferResponseCampaign> campaignsFrom;

    @SerializedName("campaigns_to")
    private List<TransferResponseCampaign> campaignsTo;

    public List<TransferResponseCampaign> getCampaignsFrom() {
        return campaignsFrom;
    }

    public TransferResponse withCampaignsFrom(List<TransferResponseCampaign> campaignsFrom) {
        this.campaignsFrom = campaignsFrom;
        return this;
    }

    public List<TransferResponseCampaign> getCampaignsTo() {
        return campaignsTo;
    }

    public TransferResponse withCampaignsTo(List<TransferResponseCampaign> campaignsTo) {
        this.campaignsTo = campaignsTo;
        return this;
    }

    public List<String> getCampaignsFromIds() {
        return campaignsFrom.stream()
                .map(TransferResponseCampaign::getCid)
                .collect(Collectors.toList());
    }

    public List<String> getCampaignsToIds() {
        return campaignsTo.stream()
                .map(TransferResponseCampaign::getCid)
                .collect(Collectors.toList());
    }
}
