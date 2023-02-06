package ru.yandex.autotests.direct.cmd.data.campaigns;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ListToCommaSeparatedStringSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Запрос для получения групп (страница редактирования групп)
 */
public class ShowCampMultiEditRequest extends BasicDirectRequest {

    public static ShowCampMultiEditRequest forCampaignAndGroups(String login, long campaignId, List<Long> groupIds) {
        return new ShowCampMultiEditRequest()
                .withCampaignId(campaignId)
                .withGroupIds(groupIds)
                .withUlogin(login);
    }

    public static ShowCampMultiEditRequest forSingleBanner(String login,
                                                           long campaignId, long groupId, long bannerId) {
        return new ShowCampMultiEditRequest().
                withCampaignId(campaignId).
                withGroupId(groupId).
                withBannerId(bannerId).
                withUlogin(login);
    }

    @SerializeKey("cid")
    private Long campaignId;

    @SerializeKey("adgroup_ids")
    @SerializeBy(ListToCommaSeparatedStringSerializer.class)
    private List<Long> groupIds;

    @SerializeKey("bid")
    @SerializeBy(ListToCommaSeparatedStringSerializer.class)
    private List<Long> bannerIds;

    @SerializeKey("banner_status")
    private String bannerStatus = "all";

    public Long getCampaignId() {
        return campaignId;
    }

    public ShowCampMultiEditRequest withCampaignId(Long campaignId) {
        this.campaignId = campaignId;
        return this;
    }

    public List<Long> getGroupIds() {
        return groupIds;
    }

    public ShowCampMultiEditRequest withGroupIds(List<Long> groupIds) {
        this.groupIds = groupIds;
        return this;
    }

    public ShowCampMultiEditRequest withGroupId(Long groupId) {
        this.groupIds = new ArrayList<>(Collections.singletonList(groupId));
        return this;
    }

    public List<Long> getBannerIds() {
        return bannerIds;
    }

    public ShowCampMultiEditRequest withBannerIds(List<Long> bannerIds) {
        this.bannerIds = bannerIds;
        return this;
    }

    public ShowCampMultiEditRequest withBannerId(Long bannerId) {
        this.bannerIds = new ArrayList<>(Collections.singletonList(bannerId));
        return this;
    }

    public String getBannerStatus() {
        return bannerStatus;
    }

    public ShowCampMultiEditRequest withBannerStatus(String bannerStatus) {
        this.bannerStatus = bannerStatus;
        return this;
    }
}
