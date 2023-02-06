package ru.yandex.autotests.direct.cmd.data.banners;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.Campaign;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ListToCommaSeparatedStringSerializer;

import java.util.List;

public class SearchBannersResponse {

    @SerializedName("banners")
    @SerializeBy(ListToCommaSeparatedStringSerializer.class)
    private List<Banner> banners;

    @SerializedName("campaigns_list")
    @SerializeBy(ListToCommaSeparatedStringSerializer.class)
    private List<CampaignParams> campaignsList;

    public List<CampaignParams> getCampaignsList() {
        return campaignsList;
    }

    public SearchBannersResponse withCampaignsList(List<CampaignParams> campaignsList) {
        this.campaignsList = campaignsList;
        return this;
    }

    public List<Banner> getBanners() {
        return banners;
    }

    public void setBanners(List<Banner> banners) {
        this.banners = banners;
    }

    public SearchBannersResponse withBanners(List<Banner> banners) {
        this.banners = banners;
        return this;
    }
}
