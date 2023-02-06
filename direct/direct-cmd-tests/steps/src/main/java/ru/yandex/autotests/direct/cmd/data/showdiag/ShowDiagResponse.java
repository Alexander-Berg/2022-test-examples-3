package ru.yandex.autotests.direct.cmd.data.showdiag;

import java.util.HashMap;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import ru.yandex.autotests.direct.cmd.data.commons.diags.BannerDiags;
import ru.yandex.autotests.direct.cmd.data.commons.diags.Diags;

public class ShowDiagResponse {

    @SerializedName("bad_phrases")
    private HashMap<String, String> badPhrases;

    @SerializedName("banner_diags")
    private BannerDiags bannerDiags;

    @SerializedName("callout_diags")
    private HashMap<String, List<Diags>> calloutDiags;

    @SerializedName("vcard_id")
    private String vcardId;

    @SerializedName("bid")
    private String bid;

    public HashMap<String, List<Diags>> getCalloutDiags() {
        return calloutDiags;
    }

    public void setCalloutDiags(
            HashMap<String, List<Diags>> calloutDiags)
    {
        this.calloutDiags = calloutDiags;
    }

    public ShowDiagResponse withCalloutDiags(
            HashMap<String, List<Diags>> calloutDiags)
    {
        setCalloutDiags(calloutDiags);
        return this;
    }

    public HashMap<String, String> getBadPhrases() {
        return badPhrases;
    }

    public ShowDiagResponse withBadPhrases(HashMap<String, String> badPhrases) {
        this.badPhrases = badPhrases;
        return this;
    }

    public BannerDiags getBannerDiags() {
        return bannerDiags;
    }

    public ShowDiagResponse withBannerDiags(BannerDiags bannerDiags) {
        this.bannerDiags = bannerDiags;
        return this;
    }

    public String getVcardId() {
        return vcardId;
    }

    public ShowDiagResponse withVcardId(String vcardId) {
        this.vcardId = vcardId;
        return this;
    }

    public String getBid() {
        return bid;
    }

    public ShowDiagResponse withBid(String bid) {
        this.bid = bid;
        return this;
    }
}
